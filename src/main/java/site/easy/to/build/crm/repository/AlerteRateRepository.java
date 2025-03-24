package site.easy.to.build.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.easy.to.build.crm.entity.AlerteRate;

@Repository
public interface AlerteRateRepository extends JpaRepository<AlerteRate, Integer> {

    AlerteRate findFirstByOrderByAlerteRateDateDesc();
    public AlerteRate findByAlerteRateId(Integer alerteId);
}