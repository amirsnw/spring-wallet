package com.snw.service;

import com.snw.domain.FinancialEntity;
import com.snw.exception.NoRecordFoundException;
import com.snw.repository.FinancialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FinancialService {

    private final FinancialRepository repository;

    public FinancialService(FinancialRepository repository) {
        this.repository = repository;
    }

    public FinancialEntity save(FinancialEntity entity) {
        return repository.save(entity);
    }

    public List<FinancialEntity> findAll() {
        return repository.findAll();
    }

    public FinancialEntity updateById(String id, FinancialEntity entity) {
        FinancialEntity domain = getById(id);

        // Lock the entity for update
        repository.lock(id);

        domain.setAmount(entity.getAmount());
        domain.setUser(entity.getUser());
        // May add more setters (But I think this entity should be immutable - readonly - for security reasons)

        repository.saveAndFlush(domain);
        return domain;
    }

    public FinancialEntity update(FinancialEntity entity) {
        FinancialEntity domain = getById(entity.getId());

        // Lock the entity for update
        repository.lock(entity.getId());

        domain.setAmount(entity.getAmount());
        domain.setUser(entity.getUser());
        // May add more setters (But I think this entity should be immutable - readonly - for security reasons)

        repository.saveAndFlush(domain);
        return domain;
    }

    public FinancialEntity partialUpdate(String id, Map<String, Object> changes) {
        FinancialEntity domain = getById(id);

        // Lock the entity for update
        repository.lock(id);

        // Apply the updates to the entity
        changes.forEach((key, value) -> {
            switch (key) {
                case "user":
                    domain.setUser((String) value);
                    break;
                case "amount":
                    domain.setAmount(new BigDecimal((String) value));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid field: " + key);
            }
        });

        repository.saveAndFlush(domain);
        return domain;
    }

    public void delete(String id) {
        FinancialEntity domain = getById(id);

        // Lock the entity for update
        repository.lock(id);

        repository.delete(domain);
    }

    public FinancialEntity getById(String id) {
        return repository
                .findById(id).orElseThrow(() -> new NoRecordFoundException("Financial entity not found"));
    }

    public FinancialEntity getByIdAndLock(String id) {
        FinancialEntity entity = repository.lock(id);
        if (entity == null) {
            new NoRecordFoundException("Financial entity not found");
        }
        try {
            Thread.sleep(5000);
        } catch (Exception e){
            e.printStackTrace();
        }
        return entity;
    }

    public List<FinancialEntity> getByUserId(String user) {
        return repository.findByUser(user);
    }
}
