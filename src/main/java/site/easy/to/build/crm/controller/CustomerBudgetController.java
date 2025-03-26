package site.easy.to.build.crm.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.CustomerBudget;
import site.easy.to.build.crm.entity.User;
import site.easy.to.build.crm.service.budget.CustomerBudgetService;
import site.easy.to.build.crm.service.customer.CustomerService;
import site.easy.to.build.crm.service.user.UserService;
import site.easy.to.build.crm.util.AuthenticationUtils;
import site.easy.to.build.crm.util.AuthorizationUtil;

@Controller
@RequestMapping("/employee/budget")
public class CustomerBudgetController {

    private final CustomerBudgetService customerBudgetService;
    private final AuthenticationUtils authenticationUtils;
    private final UserService userService;
    private final CustomerService customerService;

    @Autowired
    public CustomerBudgetController(CustomerBudgetService customerBudgetService, 
                                  AuthenticationUtils authenticationUtils,
                                  UserService userService,
                                  CustomerService customerService) {
        this.customerBudgetService = customerBudgetService;
        this.authenticationUtils = authenticationUtils;
        this.userService = userService;
        this.customerService = customerService;
    }

    @GetMapping("/show/{id}")
    public String showDetails(@PathVariable("id") int id, Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        CustomerBudget budget = customerBudgetService.findByBudgetId(id);
        if (budget == null) {
            return "error/not-found";
        }

        if (!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && 
            budget.getUser() != null && 
            budget.getUser().getId() != userId) {
            return "error/access-denied";
        }

        model.addAttribute("budget", budget);
        return "budget/show-details";
    }

    @GetMapping("/all")
    public String showAllBudgets(Model model, Authentication authentication) {
        if (!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            return "error/access-denied";
        }
        List<CustomerBudget> budgets = customerBudgetService.findAll();
        model.addAttribute("budgets", budgets);
        return "budget/show-budgets";
    }

    @GetMapping("/user-budgets")
    public String showUserBudgets(Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        List<CustomerBudget> budgets = customerBudgetService.findByUserId(userId);
        model.addAttribute("budgets", budgets);
        return "budget/show-budgets";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        populateModelAttributes(model, authentication, loggedInUser);
        model.addAttribute("budget", new CustomerBudget());
        return "budget/create-budget";
    }

    @PostMapping("/create")
    public String createBudget(
            @ModelAttribute("budget") CustomerBudget budget,
            BindingResult bindingResult,
            @RequestParam("customerId") int customerId,
            Authentication authentication,
            Model model) {
        
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Customer customer = customerService.findByCustomerId(customerId);
        if (customer == null) {
            return "error/500";
        }

        // Set customer before validation
        budget.setCustomer(customer);
        budget.setUser(loggedInUser);
        budget.setCreatedAt(LocalDateTime.now());
        budget.setUpdatedAt(LocalDateTime.now());
        
        // Manual validation for required fields
        if (budget.getLabel() == null || budget.getLabel().trim().isEmpty()) {
            bindingResult.rejectValue("label", "NotBlank", "Label is required");
        }
        
        if (budget.getAmount() == null) {
            bindingResult.rejectValue("amount", "NotNull", "Amount is required");
        }
        
        if (budget.getTransactionDate() == null) {
            bindingResult.rejectValue("transactionDate", "NotNull", "Transaction date is required");
        }
        
        if (bindingResult.hasErrors()) {
            populateModelAttributes(model, authentication, loggedInUser);
            return "budget/create-budget";
        }

        customerBudgetService.save(budget);
        return "redirect:/employee/budget/user-budgets";
    }

    @GetMapping("/update/{id}")
    public String showUpdateForm(@PathVariable("id") int id, Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        CustomerBudget budget = customerBudgetService.findByBudgetId(id);
        if (budget == null) {
            return "error/not-found";
        }

        if (!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && 
            budget.getUser() != null && 
            budget.getUser().getId() != userId) {
            return "error/access-denied";
        }

        populateModelAttributes(model, authentication, loggedInUser);
        model.addAttribute("budget", budget);
        return "budget/create-budget"; // Use the same template for update
    }

    @PostMapping("/update")
    public String updateBudget(
            @ModelAttribute("budget") CustomerBudget budget, // Remove @Validated here
            BindingResult bindingResult,
            @RequestParam("customerId") int customerId,
            Authentication authentication,
            Model model) {
        
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        CustomerBudget existingBudget = customerBudgetService.findByBudgetId(budget.getBudgetId());
        if (existingBudget == null) {
            return "error/not-found";
        }

        if (!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && 
            existingBudget.getUser() != null && 
            existingBudget.getUser().getId() != userId) {
            return "error/access-denied";
        }

        Customer customer = customerService.findByCustomerId(customerId);
        if (customer == null) {
            return "error/500";
        }

        // Set all fields before validation
        budget.setCustomer(customer);
        budget.setUser(existingBudget.getUser());
        budget.setCreatedAt(existingBudget.getCreatedAt());
        budget.setUpdatedAt(LocalDateTime.now());
        
        // Manual validation for required fields
        if (budget.getLabel() == null || budget.getLabel().trim().isEmpty()) {
            bindingResult.rejectValue("label", "NotBlank", "Label is required");
        }
        
        if (budget.getAmount() == null) {
            bindingResult.rejectValue("amount", "NotNull", "Amount is required");
        }
        
        if (budget.getTransactionDate() == null) {
            bindingResult.rejectValue("transactionDate", "NotNull", "Transaction date is required");
        }
        
        if (bindingResult.hasErrors()) {
            populateModelAttributes(model, authentication, loggedInUser);
            return "budget/create-budget";
        }

        customerBudgetService.save(budget);
        return "redirect:/employee/budget/user-budgets";
    }
    @PostMapping("/delete/{id}")
    public String deleteBudget(@PathVariable("id") int id, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        CustomerBudget budget = customerBudgetService.findByBudgetId(id);
        if (budget == null) {
            return "error/not-found";
        }

        if (!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && 
            budget.getUser() != null && 
            budget.getUser().getId() != userId) {
            return "error/access-denied";
        }

        customerBudgetService.delete(budget);
        return "redirect:/employee/budget/user-budgets";
    }

    private void populateModelAttributes(Model model, Authentication authentication, User loggedInUser) {
        List<Customer> customers;

        if (AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            customers = customerService.findAll();
        } else {
            customers = customerService.findByUserId(loggedInUser.getId());
        }

        model.addAttribute("customers", customers);
    }
}