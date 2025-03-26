package site.easy.to.build.crm.service.ticket;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.Ticket;
import site.easy.to.build.crm.repository.CustomerRepository;
import site.easy.to.build.crm.repository.TicketRepository;
import site.easy.to.build.crm.repository.UserRepository;
import site.easy.to.build.crm.util.data.RandomTicketGenerator;

@Service
public class TicketServiceImpl implements TicketService{

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public TicketServiceImpl(TicketRepository ticketRepository, UserRepository userRepository, CustomerRepository customerRepository) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Ticket findByTicketId(int id) {
        return ticketRepository.findByTicketId(id);
    }

    @Override
    public Ticket save(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    @Override
    public void delete(Ticket ticket) {
        ticketRepository.delete(ticket);
    }

    @Override
    public List<Ticket> findManagerTickets(int id) {
        return ticketRepository.findByManagerId(id);
    }

    @Override
    public List<Ticket> findEmployeeTickets(int id) {
        return ticketRepository.findByEmployeeId(id);
    }

    @Override
    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    @Override
    public List<Ticket> findCustomerTickets(int id) {
        return ticketRepository.findByCustomerCustomerId(id);
    }

    @Override
    public List<Ticket> getRecentTickets(int managerId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return ticketRepository.findByManagerIdOrderByCreatedAtDesc(managerId, pageable);
    }

    @Override
    public List<Ticket> getRecentEmployeeTickets(int employeeId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return ticketRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId, pageable);
    }

    @Override
    public List<Ticket> getRecentCustomerTickets(int customerId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return ticketRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    @Override
    public long countByEmployeeId(int employeeId) {
        return ticketRepository.countByEmployeeId(employeeId);
    }

    @Override
    public long countByManagerId(int managerId) {
        return ticketRepository.countByManagerId(managerId);
    }

    @Override
    public long countByCustomerCustomerId(int customerId) {
        return ticketRepository.countByCustomerCustomerId(customerId);
    }

    @Override
    public void deleteAllByCustomer(Customer customer) {
        ticketRepository.deleteAllByCustomer(customer);
    }

    @Override
    public Ticket createRandomTicket() {
        return new RandomTicketGenerator(this.userRepository,this.customerRepository).generateRandomTicket();
    }

    @Override
    public void generateRandomTicket(int number) {
        for (int i = 0; i < number; i++) {
            Ticket ticket = createRandomTicket();
            save(ticket);
        }
    }

    @Override
    public List<Ticket> findByCustomer(Customer customer) {
        return this.ticketRepository.findByCustomerCustomerId(customer.getCustomerId());
    }

    @Override
    public BigDecimal getTicketExpenseAmount(Ticket ticket) {
        if (ticket.getExpense() != null) {
            double amount = ticket.getExpense().getAmount();
            return new BigDecimal(amount);
        }
        
        return BigDecimal.ZERO;
    }
}