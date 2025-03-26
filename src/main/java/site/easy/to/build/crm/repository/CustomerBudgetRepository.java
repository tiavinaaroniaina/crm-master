package site.easy.to.build.crm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import site.easy.to.build.crm.entity.CustomerBudget;

@Repository
public interface CustomerBudgetRepository extends JpaRepository<CustomerBudget, Integer> {
    
    CustomerBudget findByBudgetId(int id);
    
    List<CustomerBudget> findByCustomerCustomerId(int customerId);
    
    List<CustomerBudget> findByUserId(int userId);
    
    long countByCustomerCustomerId(int customerId);
    
    long countByUserId(int userId);
}