package site.easy.to.build.crm.controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityManager;
import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.CustomerLoginInfo;
import site.easy.to.build.crm.entity.EmailTemplate;
import site.easy.to.build.crm.entity.Expense;
import site.easy.to.build.crm.entity.OAuthUser;
import site.easy.to.build.crm.entity.Ticket;
import site.easy.to.build.crm.entity.User;
import site.easy.to.build.crm.entity.settings.TicketEmailSettings;
import site.easy.to.build.crm.google.service.acess.GoogleAccessService;
import site.easy.to.build.crm.google.service.gmail.GoogleGmailApiService;
import site.easy.to.build.crm.repository.ExpenseRepository;
import site.easy.to.build.crm.service.alert.AlerteRateService;
import site.easy.to.build.crm.service.customer.CustomerService;
import site.easy.to.build.crm.service.settings.TicketEmailSettingsService;
import site.easy.to.build.crm.service.ticket.TicketService;
import site.easy.to.build.crm.service.user.UserService;
import site.easy.to.build.crm.util.AuthenticationUtils;
import site.easy.to.build.crm.util.AuthorizationUtil;
import site.easy.to.build.crm.util.DatabaseUtil;
import site.easy.to.build.crm.util.LogEntityChanges;
import site.easy.to.build.crm.util.StringUtils;

@Controller
@RequestMapping("/employee/ticket")
public class TicketController {

    private final TicketService ticketService;
    private final AuthenticationUtils authenticationUtils;
    private final UserService userService;
    private final CustomerService customerService;
    private final TicketEmailSettingsService ticketEmailSettingsService;
    private final GoogleGmailApiService googleGmailApiService;
    private final EntityManager entityManager;
    private final ExpenseRepository expenseRepository;
    private final AlerteRateService alerteRateService;


    @Autowired
    public TicketController(TicketService ticketService, AuthenticationUtils authenticationUtils, UserService userService, CustomerService customerService,
                            TicketEmailSettingsService ticketEmailSettingsService, GoogleGmailApiService googleGmailApiService, EntityManager entityManager,
                            ExpenseRepository expenseRepository,
                            AlerteRateService alerteRateService) {
        this.ticketService = ticketService;
        this.authenticationUtils = authenticationUtils;
        this.userService = userService;
        this.customerService = customerService;
        this.ticketEmailSettingsService = ticketEmailSettingsService;
        this.googleGmailApiService = googleGmailApiService;
        this.entityManager = entityManager;
        this.expenseRepository = expenseRepository;
        this.alerteRateService = alerteRateService;
    }

    @GetMapping("/show-ticket/{id}")
    public String showTicketDetails(@PathVariable("id") int id, Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if(loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Ticket ticket = ticketService.findByTicketId(id);
        if(ticket == null) {
            return "error/not-found";
        }
        User employee = ticket.getEmployee();
        if(!AuthorizationUtil.checkIfUserAuthorized(employee,loggedInUser) && !AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            return "error/access-denied";
        }

        model.addAttribute("ticket",ticket);
        return "ticket/show-ticket";
    }

    @GetMapping("/manager/all-tickets")
    public String showAllTickets(Model model) {
        List<Ticket> tickets = ticketService.findAll();
        model.addAttribute("tickets",tickets);
        return "ticket/my-tickets";
    }

    @GetMapping("/created-tickets")
    public String showCreatedTicket(Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        List<Ticket> tickets = ticketService.findManagerTickets(userId);
        model.addAttribute("tickets",tickets);
        return "ticket/my-tickets";
    }

    @GetMapping("/assigned-tickets")
    public String showEmployeeTicket(Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        List<Ticket> tickets = ticketService.findEmployeeTickets(userId);
        model.addAttribute("tickets",tickets);
        return "ticket/my-tickets";
    }
    
    @GetMapping("/create-ticket")
    public String showTicketCreationForm(Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User user = userService.findById(userId);
        if(user.isInactiveUser()) {
            return "error/account-inactive";
        }
        List<User> employees = new ArrayList<>();
        List<Customer> customers;

        if(AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            employees = userService.findAll();
            customers = customerService.findAll();
        } else {
            employees.add(user);
            customers = customerService.findByUserId(user.getId());
        }

        model.addAttribute("employees",employees);
        model.addAttribute("customers",customers);
        model.addAttribute("ticket", new Ticket());
        return "ticket/create-ticket";
    }

    @PostMapping("/create-ticket")
    public String createTicket(@ModelAttribute("ticket") @Validated Ticket ticket, BindingResult bindingResult, @RequestParam("customerId") int customerId,
                               @RequestParam Map<String, String> formParams, Model model,
                               @RequestParam("employeeId") int employeeId, Authentication authentication) {

        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User manager = userService.findById(userId);
        if(manager == null) {
            return "error/500";
        }
        if(manager.isInactiveUser()) {
            return "error/account-inactive";
        }
        if(bindingResult.hasErrors()) {
            List<User> employees = new ArrayList<>();
            List<Customer> customers;

            if(AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
                employees = userService.findAll();
                customers = customerService.findAll();
            } else {
                employees.add(manager);
                customers = customerService.findByUserId(manager.getId());
            }

            model.addAttribute("employees",employees);
            model.addAttribute("customers",customers);
            return "ticket/create-ticket";
        }

        User employee = userService.findById(employeeId);
        Customer customer = customerService.findByCustomerId(customerId);

        if(employee == null || customer == null) {
            return "error/500";
        }
        if(AuthorizationUtil.hasRole(authentication, "ROLE_EMPLOYEE")) {
            if(userId != employeeId || customer.getUser().getId() != userId) {
                return "error/500";
            }
        }

        ticket.setCustomer(customer);
        ticket.setManager(manager);
        ticket.setEmployee(employee);
        ticket.setCreatedAt(LocalDateTime.now());

        ticketService.save(ticket);

        return "redirect:/employee/ticket/assigned-tickets";
    }

    @GetMapping("/update-ticket/{id}")
    public String showTicketUpdatingForm(Model model, @PathVariable("id") int id, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if(loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Ticket ticket = ticketService.findByTicketId(id);
        if(ticket == null) {
            return "error/not-found";
        }

        User employee = ticket.getEmployee();
        if(!AuthorizationUtil.checkIfUserAuthorized(employee,loggedInUser) && !AuthorizationUtil.hasRole(authentication,"ROLE_MANAGER")) {
            return "error/access-denied";
        }

        List<User> employees = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();

        if(AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            employees = userService.findAll();
            customers = customerService.findAll();
        } else {
            employees.add(loggedInUser);
            //In case Employee's manager assign lead for the employee with a customer that's not created by this employee
            //As a result of that the employee mustn't change the customer
            if(!Objects.equals(employee.getId(), ticket.getManager().getId())) {
                customers.add(ticket.getCustomer());
            } else {
                customers = customerService.findByUserId(loggedInUser.getId());
            }
        }

        model.addAttribute("employees",employees);
        model.addAttribute("customers",customers);
        model.addAttribute("ticket", ticket);
        return "ticket/update-ticket";
    }

    @PostMapping("/update-ticket")
    public String updateTicket(@ModelAttribute("ticket") @Validated Ticket ticket, BindingResult bindingResult,
                               @RequestParam("customerId") int customerId, @RequestParam("employeeId") int employeeId,
                               Authentication authentication, Model model) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if(loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Ticket previousTicket = ticketService.findByTicketId(ticket.getTicketId());
        if(previousTicket == null) {
            return "error/not-found";
        }
        Ticket originalTicket = new Ticket();
        BeanUtils.copyProperties(previousTicket, originalTicket);

        User manager = originalTicket.getManager();
        User employee = userService.findById(employeeId);
        Customer customer = customerService.findByCustomerId(customerId);

        if(manager == null || employee ==null || customer == null) {
            return "error/500";
        }

        if(bindingResult.hasErrors()) {
            ticket.setEmployee(employee);
            ticket.setManager(manager);
            ticket.setCustomer(customer);

            List<User> employees = new ArrayList<>();
            List<Customer> customers = new ArrayList<>();

            if(AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
                employees = userService.findAll();
                customers = customerService.findAll();
            } else {
                employees.add(loggedInUser);
                //In case Employee's manager assign lead for the employee with a customer that's not created by this employee
                //As a result of that the employee mustn't change the customer
                if(!Objects.equals(employee.getId(), ticket.getManager().getId())) {
                    customers.add(ticket.getCustomer());
                } else {
                    customers = customerService.findByUserId(loggedInUser.getId());
                }
            }

            model.addAttribute("employees",employees);
            model.addAttribute("customers",customers);
            return "ticket/update-ticket";
        }
        if(manager.getId() == employeeId) {
            if (!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && customer.getUser().getId() != userId) {
                return "error/500";
            }
        } else {
            if(!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && originalTicket.getCustomer().getCustomerId() != customerId) {
                return "error/500";
            }
        }

        if(AuthorizationUtil.hasRole(authentication, "ROLE_EMPLOYEE") && employee.getId() != userId) {
            return "error/500";
        }

        ticket.setCustomer(customer);
        ticket.setManager(manager);
        ticket.setEmployee(employee);
        Ticket currentTicket = ticketService.save(ticket);

        List<String> properties = DatabaseUtil.getColumnNames(entityManager, Ticket.class);
        Map<String, Pair<String,String>> changes = LogEntityChanges.trackChanges(originalTicket,currentTicket,properties);
        boolean isGoogleUser = !(authentication instanceof UsernamePasswordAuthenticationToken);

        if(isGoogleUser && googleGmailApiService != null) {
            OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
            if(oAuthUser.getGrantedScopes().contains(GoogleAccessService.SCOPE_GMAIL)) {
                processEmailSettingsChanges(changes, userId, oAuthUser, customer);
            }
        }

        return "redirect:/employee/ticket/assigned-tickets";
    }

    @PostMapping("/delete-ticket/{id}")
    public String deleteTicket(@PathVariable("id") int id, Authentication authentication){
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if(loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Ticket ticket = ticketService.findByTicketId(id);

        User employee = ticket.getEmployee();
        if(!AuthorizationUtil.checkIfUserAuthorized(employee,loggedInUser)) {
            return "error/access-denied";
        }

        ticketService.delete(ticket);
        return "redirect:/employee/ticket/assigned-tickets";
    }

    private void processEmailSettingsChanges(Map<String, Pair<String, String>> changes, int userId, OAuthUser oAuthUser,
                                             Customer customer) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        for (Map.Entry<String, Pair<String, String>> entry : changes.entrySet()) {
            String property = entry.getKey();
            String propertyName = StringUtils.replaceCharToCamelCase(property, '_');
            propertyName = StringUtils.replaceCharToCamelCase(propertyName, ' ');

            String prevState = entry.getValue().getFirst();
            String nextState = entry.getValue().getSecond();

            TicketEmailSettings ticketEmailSettings = ticketEmailSettingsService.findByUserId(userId);

            CustomerLoginInfo customerLoginInfo = customer.getCustomerLoginInfo();
            TicketEmailSettings customerTicketEmailSettings = ticketEmailSettingsService.findByCustomerId(customerLoginInfo.getId());

            if (ticketEmailSettings != null) {
                String getterMethodName = "get" + StringUtils.capitalizeFirstLetter(propertyName);
                Method getterMethod = TicketEmailSettings.class.getMethod(getterMethodName);
                Boolean propertyValue = (Boolean) getterMethod.invoke(ticketEmailSettings);

                Boolean isCustomerLikeToGetNotified = true;
                if(customerTicketEmailSettings != null) {
                    isCustomerLikeToGetNotified = (Boolean) getterMethod.invoke(customerTicketEmailSettings);
                }

                if (isCustomerLikeToGetNotified != null && propertyValue != null && propertyValue && isCustomerLikeToGetNotified) {
                    String emailTemplateGetterMethodName = "get" + StringUtils.capitalizeFirstLetter(propertyName) + "EmailTemplate";
                    Method emailTemplateGetterMethod = TicketEmailSettings.class.getMethod(emailTemplateGetterMethodName);
                    EmailTemplate emailTemplate = (EmailTemplate) emailTemplateGetterMethod.invoke(ticketEmailSettings);
                    String body = emailTemplate.getContent();

                    property = property.replace(' ', '_');
                    String regex = "\\{\\{(.*?)\\}\\}";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(body);

                    while (matcher.find()) {
                        String placeholder = matcher.group(1);
                        if (placeholder.contains("previous") && placeholder.contains(property)) {
                            body = body.replace("{{" + placeholder + "}}", prevState);
                        } else if (placeholder.contains("next") && placeholder.contains(property)) {
                            body = body.replace("{{" + placeholder + "}}", nextState);
                        }
                    }

                    try {
                        googleGmailApiService.sendEmail(oAuthUser, customer.getEmail(), emailTemplate.getName(), body);
                    } catch (IOException | GeneralSecurityException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @PostMapping("/add-expense")
    public String addExpense(@RequestParam("ticketId") int ticketId,
                            @RequestParam("amount") double amount,
                            @RequestParam("expenseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Ticket ticket = ticketService.findByTicketId(ticketId);
        if (ticket == null) {
            return "error/not-found";
        }

        if (!AuthorizationUtil.checkIfUserAuthorized(ticket.getEmployee(), loggedInUser) && !AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            return "error/access-denied";
        }

        Expense expense = new Expense(amount, expenseDate);
        BigDecimal newExpenseAmount = new BigDecimal(amount);
        
        Customer customer = ticket.getCustomer();
        
        BigDecimal currentUsedBudget = customerService.calculateTotalExpenses(customer);
        BigDecimal totalAllocatedBudget = customerService.getTotalAllocatedBudget(customer);
        BigDecimal projectedTotalUsed = currentUsedBudget.add(newExpenseAmount);
        BigDecimal remainingBudget = totalAllocatedBudget.subtract(currentUsedBudget);
        
        System.out.println("Debug: Total allocated budget: " + totalAllocatedBudget);
        System.out.println("Debug: Current used budget: " + currentUsedBudget);
        System.out.println("Debug: New expense amount: " + newExpenseAmount);
        System.out.println("Debug: Remaining budget: " + remainingBudget);
        System.out.println("Debug: Projected total used: " + projectedTotalUsed);

        if (projectedTotalUsed.compareTo(totalAllocatedBudget) > 0) {
            redirectAttributes.addFlashAttribute("pendingTicket", ticket);
            redirectAttributes.addFlashAttribute("pendingExpense", expense);
            redirectAttributes.addFlashAttribute("exceedsBudget", true);
            redirectAttributes.addFlashAttribute("currentAmount", newExpenseAmount);
            redirectAttributes.addFlashAttribute("budget", remainingBudget);
            redirectAttributes.addFlashAttribute("totalBudget", totalAllocatedBudget);

            System.out.println("Debug: Require confirmation - budget would be exceeded");
            return "redirect:/employee/ticket/confirm";
        } else {
            // Save the expense since it doesn't exceed the budget
            expenseRepository.save(expense);
            ticket.setExpense(expense);
            ticketService.save(ticket);

            // Check if the alert threshold is reached
            BigDecimal alertPercentage = alerteRateService.getLatestAlerteRatePercentage();
            BigDecimal alertThreshold = totalAllocatedBudget.multiply(alertPercentage)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            if (projectedTotalUsed.compareTo(alertThreshold) >= 0) {
                BigDecimal usedPercentage = projectedTotalUsed.multiply(BigDecimal.valueOf(100))
                                        .divide(totalAllocatedBudget, 2, RoundingMode.HALF_UP);
                
                redirectAttributes.addFlashAttribute("alertMessage", "Alerte: Vous avez atteint " + 
                    usedPercentage.setScale(1, RoundingMode.HALF_UP) + "% du budget total.");

                System.out.println("Debug: Alert percentage reached: " + usedPercentage + "%");
            }

            return "redirect:/employee/ticket/assigned-tickets";
        }
    }

    @GetMapping("/confirm")
    public String showConfirmationPage(Model model, @ModelAttribute("pendingTicket") Ticket pendingTicket) {
        if (pendingTicket != null) {
            model.addAttribute("ticketId", pendingTicket.getTicketId());
        }
        return "ticket/confirm-expense";
    }

    @PostMapping("/confirm")
    public String confirmExpense(@RequestParam("ticketId") int ticketId,
                                @RequestParam("amount") double amount,
                                @RequestParam("expenseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
                                RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.findByTicketId(ticketId);
        if (ticket == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid ticket data.");
            return "redirect:/employee/ticket/assigned-tickets";
        }

        Expense expense = new Expense(amount, expenseDate);
        expenseRepository.save(expense);
        ticket.setExpense(expense);
        ticketService.save(ticket);

        redirectAttributes.addFlashAttribute("confirmationMessage", 
            "Expense saved despite exceeding the budget.");
        return "redirect:/employee/ticket/assigned-tickets";
    }

    @GetMapping("/add-expense/{ticketId}")
    public String showAddExpenseForm(@PathVariable("ticketId") int ticketId, Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Ticket ticket = ticketService.findByTicketId(ticketId);
        if (ticket == null) {
            return "error/not-found";
        }

        if (!AuthorizationUtil.checkIfUserAuthorized(ticket.getEmployee(), loggedInUser) && !AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            return "error/access-denied";
        }

        model.addAttribute("ticketId", ticketId);
        return "ticket/add-expense";
    }
}
