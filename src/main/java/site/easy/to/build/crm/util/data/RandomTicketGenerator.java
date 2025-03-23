package site.easy.to.build.crm.util.data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.Ticket;
import site.easy.to.build.crm.entity.User;
import site.easy.to.build.crm.repository.CustomerRepository;
import site.easy.to.build.crm.repository.UserRepository;

public class RandomTicketGenerator {

    private static final Random random = new Random();

    private static final String[] STATUSES = {
        "open", "assigned", "on-hold", "in-progress", "resolved", "closed", 
        "reopened", "pending-customer-response", "escalated", "archived"
    };
    
    private static final String[] PRIORITIES = {
        "low", "medium", "high", "closed", "urgent", "critical"
    };
    
    private static final String[] SUBJECTS = {
        "Login Issue", "Payment Failure", "Feature Request", "Bug Report",
        "Account Suspension", "Performance Problem", "Security Concern"
    };
    
    private static final String[] DESCRIPTION_TEMPLATES = {
        "User reported an issue with %s.",
        "Customer experienced %s during their session.",
        "Request for %s submitted by customer.",
        "System encountered %s error on last login attempt.",
        "Urgent: %s affecting multiple users."
    };

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public RandomTicketGenerator(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public Ticket generateRandomTicket() throws IllegalStateException {
        List<User> users = userRepository.findAll();
        List<Customer> customers = customerRepository.findAll();

        if (users.isEmpty() || customers.isEmpty()) {
            throw new IllegalStateException("Cannot generate ticket: No users or customers found in the database.");
        }

        User manager = getRandomUser(users);
        User employee = getRandomUser(users);
        Customer customer = getRandomCustomer(customers);

        return createTicket(manager, employee, customer);
    }

    private Ticket createTicket(User manager, User employee, Customer customer) {
        Ticket ticket = new Ticket();
        
        ticket.setSubject(generateRandomSubject());
        ticket.setDescription(generateRandomDescription());
        ticket.setStatus(generateRandomStatus());
        ticket.setPriority(generateRandomPriority());
        ticket.setManager(manager);
        ticket.setEmployee(employee);
        ticket.setCustomer(customer);
        ticket.setCreatedAt(generateRandomCreatedAt());
        
        return ticket;
    }

    private String generateRandomSubject() {
        return SUBJECTS[random.nextInt(SUBJECTS.length)];
    }

    private String generateRandomDescription() {
        String template = DESCRIPTION_TEMPLATES[random.nextInt(DESCRIPTION_TEMPLATES.length)];
        String detail = generateRandomDetail();
        return String.format(template, detail);
    }

    private String generateRandomDetail() {
        String[] details = {
            "authentication", "payment processing", "new dashboard feature",
            "unexpected crash", "slow response times", "unauthorized access"
        };
        return details[random.nextInt(details.length)];
    }

    private String generateRandomStatus() {
        return STATUSES[random.nextInt(STATUSES.length)];
    }

    private String generateRandomPriority() {
        return PRIORITIES[random.nextInt(PRIORITIES.length)];
    }

    private LocalDateTime generateRandomCreatedAt() {
        LocalDateTime now = LocalDateTime.now();
        int daysBack = random.nextInt(30); 
        int hours = random.nextInt(24);
        int minutes = random.nextInt(60);
        return now.minusDays(daysBack).minusHours(hours).minusMinutes(minutes);
    }

    private User getRandomUser(List<User> users) {
        return users.get(random.nextInt(users.size()));
    }

    private Customer getRandomCustomer(List<Customer> customers) {
        return customers.get(random.nextInt(customers.size()));
    }
}