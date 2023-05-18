package com.snw.service;

import com.snw.domain.FinancialEntity;
import com.snw.domain.WalletEntity;
import com.snw.exception.FinancialBoundaryException;
import com.snw.exception.NoRecordFoundException;
import com.snw.repository.FinancialRepository;
import com.snw.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional
public class WalletService {

    private final FinancialRepository financialRepository;

    private final WalletRepository walletRepository;

    public WalletService(FinancialRepository financialRepository,
                         WalletRepository walletRepository) {
        this.financialRepository = financialRepository;
        this.walletRepository = walletRepository;
    }

    public WalletEntity getByUser(String user) {
        return walletRepository
                .findById(user).orElseThrow(() -> new NoRecordFoundException("Wallet entity not found"));
    }

    public List<WalletEntity> updateCredit(List<FinancialEntity> inputRecords) {

        List<WalletEntity> entities = new ArrayList<>();

        // Validate (amount < 1000) for all records
        checkForMoreThanThousand(inputRecords);

        // Extracting input/actual balance for each user
        Map<String, BigDecimal> inputUsersBalance = extractInputRecordsToSumByUser(inputRecords);

        // Since we are fetching/validating/updating in bulk, so we should lock all target records
        walletRepository.lockAllIn(inputUsersBalance.keySet());

        // get summery using "group by user"
        Map<String, BigDecimal> actualUsersBalance =
                convertUserBalanceListToMap(walletRepository.groupByUser());

        // Validate boundary constraints in lock mode
        checkForConstraints(inputUsersBalance, actualUsersBalance);

        inputUsersBalance.forEach((user, amount) -> {
            WalletEntity walletEntity = walletRepository.getWithLock(user);
            BigDecimal actualAmount = actualUsersBalance.get(user) == null ?
                    new BigDecimal(0) : actualUsersBalance.get(user);

            if (walletEntity == null) {
                walletEntity = new WalletEntity();
                walletEntity.setUser(user);
            }

            walletEntity.setCredit(amount.add(actualAmount));

            // save or update and flush(release) immediately
            entities.add(walletRepository.saveAndFlush(walletEntity));
        });

        financialRepository.saveAll(inputRecords);

        // Save the records
        return entities;
    }

    private Map<String, BigDecimal> convertUserBalanceListToMap(List<WalletEntity> records) {
        return records.stream().collect(Collectors.toMap(key -> key.getUser(), val -> val.getCredit()));
    }

    private Map<String, BigDecimal> extractInputRecordsToSumByUser(List<FinancialEntity> inputRecords) {
        Map<String, BigDecimal> inputUsersBalance = new HashMap<>();
        Map<String, List<FinancialEntity>> inputRecordsByUsers = inputRecords.stream()
                .collect(groupingBy(FinancialEntity::getUser));

        inputRecordsByUsers.forEach((user, records) -> {
            BigDecimal amount = new BigDecimal(0);
            for (FinancialEntity item : records) {
                switch (item.getStatus()) {
                    case CREDITOR:
                        amount = amount.add(item.getAmount());
                        break;
                    case DEBTOR:
                        amount = amount.subtract(item.getAmount());
                        break;
                    default:
                        continue;
                }
            }
            inputUsersBalance.put(user, amount);
        });
        return inputUsersBalance;
    }

    private void checkForMoreThanThousand(List<FinancialEntity> records) {
        for (FinancialEntity record : records) {
            if (record.getAmount().compareTo(new BigDecimal("1000")) > 0) {
                throw new IllegalArgumentException("Record amount cannot be greater than 1000");
            }
        }
    }

    private void checkForConstraints(Map<String, BigDecimal> input, Map<String, BigDecimal> actual) {

        Map<String, List<String>> constraintViolationMap = new HashMap<>();

        input.forEach((user, amount) -> {
            List<String> userConstraintResult = new ArrayList<>();

            if (amount.signum() == -1) {
                userConstraintResult.add("Record amount constraints minimum boundary (0)");
            }

            if (amount.compareTo(new BigDecimal(1_000_000L)) > 0) {
                userConstraintResult.add("Record amount constraints maximum boundary (1,000,000)");
            }
            if (actual.get(user) != null) {
                // Check if sum is negative
                if (actual.get(user).compareTo(amount) < 0) {
                    userConstraintResult.add("Record amount constraints minimum boundary (0)");
                }

                // BigDecimal is an immutable class, so actual amount stays the same
                if (actual.get(user).add(amount).compareTo(new BigDecimal(1_000_000L)) > 0) {
                    userConstraintResult.add("Record amount constraints maximum boundary (1,000,000)");
                }
            }
            if (userConstraintResult.size() > 0) {
                constraintViolationMap.put(user, userConstraintResult);
            }
        });

        if (!constraintViolationMap.isEmpty()) {
            throw new FinancialBoundaryException(constraintViolationMap);
        }
    }

}
