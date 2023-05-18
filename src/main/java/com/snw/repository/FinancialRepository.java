package com.snw.repository;


import com.snw.domain.FinancialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;

@Repository
public interface FinancialRepository extends JpaRepository<FinancialEntity, String> {

    // @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM FinancialEntity e WHERE e.id = :id")
    // @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value="1000")})
    // @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value="1000")})
    FinancialEntity lock(@Param("id") String id);

    List<FinancialEntity> findByUser(String user);
}
