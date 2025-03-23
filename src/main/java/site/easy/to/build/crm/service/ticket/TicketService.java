package site.easy.to.build.crm.service.ticket;

import java.util.List;

import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.Ticket;

public interface TicketService {
    public Ticket findByTicketId(int id);

    public Ticket save(Ticket ticket);

    public void delete(Ticket ticket);

    public List<Ticket> findManagerTickets(int id);

    public List<Ticket> findEmployeeTickets(int id);

    public List<Ticket> findAll();

    public List<Ticket> findCustomerTickets(int id);

    List<Ticket> getRecentTickets(int managerId, int limit);

    List<Ticket> getRecentEmployeeTickets(int employeeId, int limit);

    List<Ticket> getRecentCustomerTickets(int customerId, int limit);

    long countByEmployeeId(int employeeId);

    long countByManagerId(int managerId);

    long countByCustomerCustomerId(int customerId);

    void deleteAllByCustomer(Customer customer);

    Ticket createRandomTicket();

    void generateRandomTicket(int number);

    public List<Ticket> findByCustomer(Customer customer);
}