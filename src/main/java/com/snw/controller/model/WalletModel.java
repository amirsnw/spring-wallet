package com.snw.controller.model;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class WalletModel {

    private String id;
    private String user;
    private BigDecimal credit;

}
