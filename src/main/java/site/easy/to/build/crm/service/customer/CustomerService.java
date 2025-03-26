package site.easy.to.build.crm.service.customer;

import java.math.BigDecimal;
import java.util.List;

import site.easy.to.build.crm.entity.Customer;

public interface CustomerService {

    public Customer findByCustomerId(int customerId);

    public List<Customer> findByUserId(int userId);

    public Customer findByEmail(String email);

    public List<Customer> findAll();

    public Customer save(Customer customer);

    public void delete(Customer customer);

    public List<Customer> getRecentCustomers(int userId, int limit);

    long countByUserId(int userId);

    public BigDecimal getTotalBudget(Customer customer);

    public BigDecimal calculateTotalExpenses(Customer customer);

    public BigDecimal getTotalAllocatedBudget(Customer customer);
}