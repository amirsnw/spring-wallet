package com.snw.controller.model;

import com.snw.domain.enumeration.AccountingStatus;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class FinancialModel {

    private String id;

    private AccountingStatus status;

    private String user;

    private BigDecimal amount;

}
