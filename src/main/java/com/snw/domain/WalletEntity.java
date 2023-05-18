package com.snw.domain;

import com.snw.config.AppConstants;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id", doNotUseGetters = true, callSuper = false)
@Entity
@Table(name = AppConstants.TABLE_PREFIX + "wallet")
public class WalletEntity {

    @Id
    @Column(name = "user", unique = true, length = 50)
    private String user;

    @Column(precision = 10, scale = 2, name = "credit")
    private BigDecimal credit;

}
