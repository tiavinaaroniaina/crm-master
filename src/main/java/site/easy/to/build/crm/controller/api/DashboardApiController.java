package site.easy.to.build.crm.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.easy.to.build.crm.service.lead.LeadService;
import site.easy.to.build.crm.service.ticket.TicketService;
import site.easy.to.build.crm.util.AuthenticationUtils;
import site.easy.to.build.crm.util.AuthorizationUtil;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final TicketService ticketService;
    private final LeadService leadService;
    private final AuthenticationUtils authenticationUtils;

    @Autowired
    public DashboardApiController(TicketService ticketService, LeadService leadService,
                                 AuthenticationUtils authenticationUtils) {
        this.ticketService = ticketService;
        this.leadService = leadService;
        this.authenticationUtils = authenticationUtils;
    }

    @GetMapping("/counts")
    public ResponseEntity<?> getDashboardCounts(Authentication authentication) {
        try {
            int userId = authenticationUtils.getLoggedInUserId(authentication);
            Map<String, Long> response = new HashMap<>();
            
            long countTickets;
            long countLeads;

            // Handle counts based on user role (customer or employee)
            if (AuthorizationUtil.hasRole(authentication, "ROLE_CUSTOMER")) {
                String email = authenticationUtils.getOAuthUserFromAuthentication(authentication).getEmail();
                
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
}