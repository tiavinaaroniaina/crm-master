package site.easy.to.build.crm.controller.api;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.easy.to.build.crm.dto.BudgetDTO;
import site.easy.to.build.crm.entity.CustomerBudget;
import site.easy.to.build.crm.entity.Expense;
import site.easy.to.build.crm.service.budget.CustomerBudgetService;
import site.easy.to.build.crm.service.expense.ExpenseService;
import site.easy.to.build.crm.service.lead.LeadService;
import site.easy.to.build.crm.service.ticket.TicketService;
import site.easy.to.build.crm.util.AuthenticationUtils;
import site.easy.to.build.crm.util.AuthorizationUtil;
import site.easy.to.build.crm.util.number.NumberFormatUtil;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final TicketService ticketService;
    private final LeadService leadService;
    private final AuthenticationUtils authenticationUtils;
    private final CustomerBudgetService customerBudgetService;
    private final ExpenseService expenseService;

    @Autowired
    public DashboardApiController(TicketService ticketService, LeadService leadService,
                                 AuthenticationUtils authenticationUtils,
                                 CustomerBudgetService customerBudgetService,
                                 ExpenseService expenseService) {
        this.ticketService = ticketService;
        this.leadService = leadService;
        this.authenticationUtils = authenticationUtils;
        this.customerBudgetService = customerBudgetService;
        this.expenseService = expenseService;
    }

    @GetMapping("/counts")
    public ResponseEntity<?> getDashboardCounts(Authentication authentication) {
        try {
            int userId = authenticationUtils.getLoggedInUserId(authentication);
            Map<String, Long> response = new HashMap<>();
            
            long countTickets;
            long countLeads;

            if (AuthorizationUtil.hasRole(authentication, "ROLE_CUSTOMER")) {
                countTickets = ticketService.findAll().size();
                countLeads = leadService.findAll().size();
            } else {
                countTickets = ticketService.findAll().size();
                countLeads = leadService.findAll().size();
            }

            response.put("totalTickets", countTickets);
            response.put("totalLeads", countLeads);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Error retrieving counts: " + e.getMessage());
        }
    }

    @GetMapping("/total_budget")
    public ResponseEntity<?> getTotalBudget(Authentication authentication) {
        BigDecimal totalBudget = this.customerBudgetService.getTotalBudget();
        Map<String, BigDecimal> response = new HashMap<String, BigDecimal>();

        response.put("totalBudget", NumberFormatUtil.roundToDecimals(totalBudget, 2));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/budgets")
    public ResponseEntity<?> getBudgets(Authentication authentication) {
        try {
            List<CustomerBudget> budgets = this.customerBudgetService.findAll();
            List<BudgetDTO> budgetDTOs = budgets.stream()
                .map(budget -> new BudgetDTO(
                    budget.getBudgetId(),
                    budget.getCustomer().getCustomerId(),
                    budget.getCustomer().getName(),
                    budget.getLabel(),
                    budget.getAmount(),
                    budget.getTransactionDate()
                ))
                .collect(Collectors.toList());
            
            Map<String, List<BudgetDTO>> response = new HashMap<String, List<BudgetDTO>>();
    
            response.put("budgets", budgetDTOs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body("Error retrieving budgets: " + e.getMessage());
        }
    }

    @GetMapping("/total_expenses")
    public ResponseEntity<?> getTotalExpenses(Authentication authentication) {
        try {
            BigDecimal totalExpenses = this.expenseService.getTotalExpenses();
            Map<String, BigDecimal> response = new HashMap<String, BigDecimal>();

            response.put("expenses", NumberFormatUtil.roundToDecimals(totalExpenses, 2));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body("Error retrieving total expense: " + e.getMessage());
        }
    }

    @GetMapping("/expenses")
    public ResponseEntity<?> getExpenses(Authentication authentication) {
        try {
            List<Expense> expenses = this.expenseService.findAll();
            Map<String, List<Expense>> response = new HashMap<String, List<Expense>>();

            response.put("expenses", expenses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body("Error retrieving expenses: " + e.getMessage());
        }
    }
}