package com.snw.repository;


import com.snw.domain.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Set;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)

    @Query("SELECT e FROM WalletEntity e WHERE e.user  IN :users")
    List<WalletEntity> lockAllIn(@Param("users") Set<String> users);

    @Query("SELECT e FROM WalletEntity e WHERE e.user = :user")
    WalletEntity getWithLock(@Param("user") String user);

    @Query(value =
            "SELECT " +
                    " new com.snw.domain.WalletEntity(fe.user, " +
                    "SUM(CASE WHEN fe.status = 'CREDITOR' THEN fe.amount ELSE -fe.amount END))" +
                    "FROM FinancialEntity fe " +
                    "GROUP BY fe.user")
    List<WalletEntity> groupByUser();

}
