package site.easy.to.build.crm.service.budget;

import java.util.List;

import site.easy.to.build.crm.entity.CustomerBudget;

public interface CustomerBudgetService {
    
    CustomerBudget findByBudgetId(int id);
    
    List<CustomerBudget> findAll();
    
    List<CustomerBudget> findByCustomerId(int customerId);
    
    List<CustomerBudget> findByUserId(int userId);
    
    CustomerBudget save(CustomerBudget customerBudget);
    
    void delete(CustomerBudget customerBudget);
    
    long countByCustomerId(int customerId);
    
    long countByUserId(int userId);
}