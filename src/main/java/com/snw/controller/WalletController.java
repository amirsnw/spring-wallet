package com.snw.controller;

import com.snw.controller.dto.FinancialDto;
import com.snw.controller.mapper.FinancialMapper;
import com.snw.controller.mapper.WalletMapper;
import com.snw.controller.model.WalletModel;
import com.snw.domain.FinancialEntity;
import com.snw.domain.WalletEntity;
import com.snw.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final FinancialMapper financialMapper;

    private final WalletMapper walletMapper;

    private final WalletService service;

    @Autowired
    public WalletController(FinancialMapper financialMapper,
                            WalletMapper walletMapper,
                            WalletService service) {
        this.financialMapper = financialMapper;
        this.walletMapper = walletMapper;
        this.service = service;
    }

    @PostMapping()
    public ResponseEntity<List<WalletModel>> addCredit(@RequestBody List<FinancialDto> records) {
        List<FinancialEntity> financialEntities = financialMapper.toEntity(records);

        List<WalletEntity> walletEntities = service.updateCredit(financialEntities);

        return new ResponseEntity<>(walletMapper.toModel(walletEntities), HttpStatus.CREATED);
    }

    @GetMapping("/{user}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String user) {
        WalletEntity entity = service.getByUser(user);

        return new ResponseEntity<>(walletMapper.toModel(entity).getCredit(), HttpStatus.OK);
    }
}
