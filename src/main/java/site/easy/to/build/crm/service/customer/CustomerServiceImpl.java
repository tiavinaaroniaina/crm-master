package site.easy.to.build.crm.service.customer;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.CustomerBudget;
import site.easy.to.build.crm.entity.Lead;
import site.easy.to.build.crm.entity.Ticket;
import site.easy.to.build.crm.repository.CustomerBudgetRepository;
import site.easy.to.build.crm.repository.CustomerRepository;
import site.easy.to.build.crm.service.lead.LeadService;
import site.easy.to.build.crm.service.ticket.TicketService;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerBudgetRepository customerBudgetRepository;
    private final LeadService leadService;
    private final TicketService ticketService;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerBudgetRepository customerBudgetRepository, LeadService leadService, TicketService ticketService) {
        this.customerRepository = customerRepository;
        this.customerBudgetRepository = customerBudgetRepository;
        this.leadService = leadService;
        this.ticketService = ticketService;
    }

    @Override
    public Customer findByCustomerId(int customerId) {
        return customerRepository.findByCustomerId(customerId);
    }

    @Override
    public Customer findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    @Override
    public List<Customer> findByUserId(int userId) {
        return customerRepository.findByUserId(userId);
    }

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    public void delete(Customer customer) {
        customerRepository.delete(customer);
    }

    @Override
    public List<Customer> getRecentCustomers(int userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return customerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public long countByUserId(int userId) {
        return customerRepository.countByUserId(userId);
    }

    @Override
    public BigDecimal getTotalBudget(Customer customer) {
        BigDecimal allocatedBudget = BigDecimal.ZERO;
        List<CustomerBudget> budgets = this.customerBudgetRepository.findByCustomerCustomerId(customer.getCustomerId());

        for (CustomerBudget customerBudget : budgets) {
            allocatedBudget = allocatedBudget.add(customerBudget.getAmount());
        }
        
        BigDecimal usedBudget = calculateTotalExpenses(customer);
        return allocatedBudget.subtract(usedBudget);
    }

    public BigDecimal calculateTotalExpenses(Customer customer) {
        BigDecimal totalExpenses = BigDecimal.ZERO;
        
        List<Lead> customerLeads = this.leadService.findByCustomer(customer);
        for (Lead lead : customerLeads) 
        { 
            totalExpenses = totalExpenses.add(leadService.getLeadExpenseAmount(lead)); 
        }
        
        List<Ticket> customerTickets = this.ticketService.findByCustomer(customer);
        for (Ticket ticket : customerTickets) 
        { 
            totalExpenses = totalExpenses.add(ticketService.getTicketExpenseAmount(ticket)); 
        }        
        return totalExpenses;
    }

    public BigDecimal getTotalAllocatedBudget(Customer customer) {
        BigDecimal allocatedBudget = BigDecimal.ZERO;
        List<CustomerBudget> budgets = this.customerBudgetRepository.findByCustomerCustomerId(customer.getCustomerId());
    
        for (CustomerBudget customerBudget : budgets) {
            allocatedBudget = allocatedBudget.add(customerBudget.getAmount());
        }
        
        return allocatedBudget;
    }
}