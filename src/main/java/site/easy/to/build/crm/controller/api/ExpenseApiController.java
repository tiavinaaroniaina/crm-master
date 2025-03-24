package site.easy.to.build.crm.controller.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.easy.to.build.crm.dto.LeadDTO;
import site.easy.to.build.crm.dto.TicketDTO;
import site.easy.to.build.crm.entity.Expense;
import site.easy.to.build.crm.entity.Lead;
import site.easy.to.build.crm.entity.Ticket;
import site.easy.to.build.crm.service.alert.AlerteRateService;
import site.easy.to.build.crm.service.customer.CustomerService;
import site.easy.to.build.crm.service.expense.ExpenseService;
import site.easy.to.build.crm.service.lead.LeadService;
import site.easy.to.build.crm.service.ticket.TicketService;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseApiController {

    private final TicketService ticketService;
    private final LeadService leadService;
    private final ExpenseService expenseService;
    private final CustomerService customerService;
    private final AlerteRateService alerteRateService;

    @Autowired
    public ExpenseApiController(TicketService ticketService, 
                                LeadService leadService, 
                                ExpenseService expenseService,
                                CustomerService customerService,
                                AlerteRateService alerteRateService) {
        this.ticketService = ticketService;
        this.leadService = leadService;
        this.expenseService = expenseService;
        this.customerService = customerService;
        this.alerteRateService = alerteRateService;
    }

    @PutMapping("/ticket/{ticketId}")
    public ResponseEntity<?> updateTicketExpense(@PathVariable int ticketId, 
                                               @RequestBody Expense updatedExpense) {
        try {
            Ticket ticket = ticketService.findByTicketId(ticketId);
            if (ticket == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ticket not found with ID: " + ticketId);
            }

            Expense existingExpense = ticket.getExpense();
            if (existingExpense == null) {
                updatedExpense.setExpenseId(0); 
                expenseService.save(updatedExpense);
                ticket.setExpense(updatedExpense);
            } else {
                existingExpense.setAmount(updatedExpense.getAmount());
                existingExpense.setExpenseDate(updatedExpense.getExpenseDate());
                expenseService.save(existingExpense);
            }
            
            ticketService.save(ticket);
            return ResponseEntity.ok(ticket.getExpense());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating ticket expense: " + e.getMessage());
        }
    }

    @DeleteMapping("/ticket/{ticketId}")
    public ResponseEntity<?> deleteTicketExpense(@PathVariable int ticketId) {
        try {
            Ticket ticket = ticketService.findByTicketId(ticketId);
            if (ticket == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ticket not found with ID: " + ticketId);
            }

            Expense expense = ticket.getExpense();
            if (expense == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No expense found for ticket ID: " + ticketId);
            }

            ticket.setExpense(null);
            ticketService.save(ticket);
            expenseService.delete(expense);
            
            return ResponseEntity.ok("Expense deleted successfully from ticket");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting ticket expense: " + e.getMessage());
        }
    }

    @PutMapping("/lead/{leadId}")
    public ResponseEntity<?> updateLeadExpense(@PathVariable int leadId, 
                                             @RequestBody Expense updatedExpense) {
        try {
            Lead lead = leadService.findByLeadId(leadId);
            if (lead == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lead not found with ID: " + leadId);
            }

            Expense existingExpense = lead.getExpense();
            if (existingExpense == null) {
                updatedExpense.setExpenseId(0); // Ensure new expense gets generated ID
                expenseService.save(updatedExpense);
                lead.setExpense(updatedExpense);
            } else {
                existingExpense.setAmount(updatedExpense.getAmount());
                existingExpense.setExpenseDate(updatedExpense.getExpenseDate());
                expenseService.save(existingExpense);
            }
            
            leadService.save(lead);
            return ResponseEntity.ok(lead.getExpense());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating lead expense: " + e.getMessage());
        }
    }

    @DeleteMapping("/lead/{leadId}")
    public ResponseEntity<?> deleteLeadExpense(@PathVariable int leadId) {
        try {
            Lead lead = leadService.findByLeadId(leadId);
            if (lead == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lead not found with ID: " + leadId);
            }

            Expense expense = lead.getExpense();
            if (expense == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No expense found for lead ID: " + leadId);
            }

            lead.setExpense(null);
            leadService.save(lead);
            expenseService.delete(expense);
            
            return ResponseEntity.ok("Expense deleted successfully from lead");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting lead expense: " + e.getMessage());
        }
    }

    @GetMapping("/tickets")
    public ResponseEntity<?> getAllTicketsWithExpenses() {
        try {
            List<Ticket> allTickets = ticketService.findAll();
            List<TicketDTO> ticketsWithExpenses = allTickets.stream()
                .map(ticket -> new TicketDTO(
                    ticket.getTicketId(),
                    ticket.getSubject(),
                    ticket.getStatus(),
                    ticket.getPriority(),
                    ticket.getManager() != null ? ticket.getManager().getId() : null,
                    ticket.getEmployee() != null ? ticket.getEmployee().getId() : null,
                    ticket.getCustomer() != null ? ticket.getCustomer().getCustomerId() : null,
                    ticket.getExpense(),
                    ticket.getCreatedAt(),
                    ticket.getCustomer() != null ? customerService.getTotalAllocatedBudget(ticket.getCustomer()) : BigDecimal.ZERO,
                    ticket.getCustomer() != null ? customerService.calculateTotalExpenses(ticket.getCustomer()) : BigDecimal.ZERO,
                    alerteRateService.getLatestAlerteRatePercentage()
                ))
                .collect(Collectors.toList());
            
            if (ticketsWithExpenses.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No tickets with expenses found");
            }
            
            return ResponseEntity.ok(ticketsWithExpenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving tickets with expenses: " + e.getMessage());
        }
    }

    @GetMapping("/leads")
    public ResponseEntity<?> getAllLeadsWithExpenses() {
        try {
            List<Lead> allLeads = leadService.findAll();
            List<LeadDTO> leadsWithExpenses = allLeads.stream()
                .map(lead -> new LeadDTO(
                    lead.getLeadId(),
                    lead.getName(),
                    lead.getStatus(),
                    lead.getPhone(),
                    lead.getManager() != null ? lead.getManager().getId() : null,
                    lead.getEmployee() != null ? lead.getEmployee().getId() : null,
                    lead.getCustomer() != null ? lead.getCustomer().getCustomerId() : null,
                    lead.getExpense(),
                    lead.getCreatedAt(),
                    lead.getCustomer() != null ? customerService.getTotalAllocatedBudget(lead.getCustomer()) : BigDecimal.ZERO,
                    lead.getCustomer() != null ? customerService.calculateTotalExpenses(lead.getCustomer()) : BigDecimal.ZERO,
                    alerteRateService.getLatestAlerteRatePercentage()
                ))
                .collect(Collectors.toList());
            
            if (leadsWithExpenses.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No leads with expenses found");
            }
            
            return ResponseEntity.ok(leadsWithExpenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving leads with expenses: " + e.getMessage());
        }
    }
}