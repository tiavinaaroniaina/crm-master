package site.easy.to.build.crm.service.budget;

import java.util.List;

import org.springframework.stereotype.Service;

import site.easy.to.build.crm.entity.CustomerBudget;
import site.easy.to.build.crm.repository.CustomerBudgetRepository;

@Service
public class CustomerBudgetServiceImpl implements CustomerBudgetService {

    private final CustomerBudgetRepository customerBudgetRepository;

    public CustomerBudgetServiceImpl(CustomerBudgetRepository customerBudgetRepository) {
        this.customerBudgetRepository = customerBudgetRepository;
    }

    @Override
    public CustomerBudget findByBudgetId(int id) {
        return customerBudgetRepository.findByBudgetId(id);
    }

    @Override
    public List<CustomerBudget> findAll() {
        return customerBudgetRepository.findAll();
    }

    @Override
    public List<CustomerBudget> findByCustomerId(int customerId) {
        return customerBudgetRepository.findByCustomerCustomerId(customerId);
    }

    @Override
    public List<CustomerBudget> findByUserId(int userId) {
        return customerBudgetRepository.findByUserId(userId);
    }

    @Override
    public CustomerBudget save(CustomerBudget customerBudget) {
        return customerBudgetRepository.save(customerBudget);
    }

    @Override
    public void delete(CustomerBudget customerBudget) {
        customerBudgetRepository.delete(customerBudget);
    }

    @Override
    public long countByCustomerId(int customerId) {
        return customerBudgetRepository.countByCustomerCustomerId(customerId);
    }

    @Override
    public long countByUserId(int userId) {
        return customerBudgetRepository.countByUserId(userId);
    }
}