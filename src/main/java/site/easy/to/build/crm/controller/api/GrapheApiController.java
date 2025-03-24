package site.easy.to.build.crm.controller.api;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.easy.to.build.crm.entity.CustomerBudget;
import site.easy.to.build.crm.entity.Expense;
import site.easy.to.build.crm.entity.Ticket;
import site.easy.to.build.crm.service.budget.CustomerBudgetService;
import site.easy.to.build.crm.service.customer.CustomerService;
import site.easy.to.build.crm.service.expense.ExpenseService;
import site.easy.to.build.crm.service.ticket.TicketService;

@RestController
@RequestMapping("/api/dashboard")
public class GrapheApiController {

    private final TicketService ticketService;
    private final ExpenseService expenseService;
    private final CustomerService customerService;
    private final CustomerBudgetService customerBudgetService;

    @Autowired
    public GrapheApiController(TicketService ticketService,
                              ExpenseService expenseService,
                              CustomerService customerService,
                              CustomerBudgetService customerBudgetService) {
        this.ticketService = ticketService;
        this.expenseService = expenseService;
        this.customerService = customerService;
        this.customerBudgetService = customerBudgetService;
    }

    // DTO classes and endpoint methods remain the same
    public static class TicketStatusDTO {
        private String status;
        private long count;

        public TicketStatusDTO(String status, long count) {
            this.status = status;
            this.count = count;
        }
        public String getStatus() { return status; }
        public long getCount() { return count; }
    }

    public static class MonthlyExpenseDTO {
        private String month;
        private double totalAmount;

        public MonthlyExpenseDTO(String month, double totalAmount) {
            this.month = month;
            this.totalAmount = totalAmount;
        }
        public String getMonth() { return month; }
        public double getTotalAmount() { return totalAmount; }
    }

    public static class BudgetEvolutionDTO {
        private String date;
        private BigDecimal totalBudget;

        public BudgetEvolutionDTO(String date, BigDecimal totalBudget) {
            this.date = date;
            this.totalBudget = totalBudget;
        }
        public String getDate() { return date; }
        public BigDecimal getTotalBudget() { return totalBudget; }
    }

    @GetMapping("/ticket-status")
    public ResponseEntity<?> getTicketStatusDistribution() {
        try {
            List<Ticket> tickets = ticketService.findAll();
            Map<String, Long> statusCount = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getStatus, Collectors.counting()));

            List<TicketStatusDTO> statusDistribution = statusCount.entrySet().stream()
                .map(entry -> new TicketStatusDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

            if (statusDistribution.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No ticket status data found");
            }
            return ResponseEntity.ok(statusDistribution);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving ticket status distribution: " + e.getMessage());
        }
    }

    @GetMapping("/monthly-expenses")
    public ResponseEntity<?> getMonthlyExpenses() {
        try {
            List<Expense> expenses = expenseService.findAll();
            Map<YearMonth, Double> monthlyTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                    expense -> YearMonth.from(expense.getExpenseDate()),
                    Collectors.summingDouble(Expense::getAmount)
                ));

            List<MonthlyExpenseDTO> monthlyExpenses = monthlyTotals.entrySet().stream()
                .map(entry -> new MonthlyExpenseDTO(entry.getKey().toString(), entry.getValue()))
                .sorted((a, b) -> a.getMonth().compareTo(b.getMonth()))
                .collect(Collectors.toList());

            if (monthlyExpenses.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No expense data found");
            }
            return ResponseEntity.ok(monthlyExpenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving monthly expenses: " + e.getMessage());
        }
    }

    @GetMapping("/budget-evolution")
    public ResponseEntity<?> getBudgetEvolution() {
        try {
            List<CustomerBudget> budgets = this.customerBudgetService.findAll();
            Map<YearMonth, BigDecimal> monthlyBudgets = budgets.stream()
                .filter(budget -> budget.getTransactionDate() != null && budget.getAmount() != null)
                .collect(Collectors.groupingBy(
                    budget -> YearMonth.from(budget.getTransactionDate()),
                    Collectors.mapping(
                        CustomerBudget::getAmount,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                    )
                ));

            List<BudgetEvolutionDTO> budgetEvolution = monthlyBudgets.entrySet().stream()
                .map(entry -> new BudgetEvolutionDTO(entry.getKey().toString(), entry.getValue()))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());

            if (budgetEvolution.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No budget data found");
            }
            return ResponseEntity.ok(budgetEvolution);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving budget evolution");
        }
    }
}