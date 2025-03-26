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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.CustomerLoginInfo;
import site.easy.to.build.crm.entity.EmailTemplate;
import site.easy.to.build.crm.entity.Expense;
import site.easy.to.build.crm.entity.File;
import site.easy.to.build.crm.entity.GoogleDriveFile;
import site.easy.to.build.crm.entity.Lead;
import site.easy.to.build.crm.entity.LeadAction;
import site.easy.to.build.crm.entity.OAuthUser;
import site.easy.to.build.crm.entity.User;
import site.easy.to.build.crm.entity.settings.LeadEmailSettings;
import site.easy.to.build.crm.google.model.calendar.EventDisplay;
import site.easy.to.build.crm.google.model.drive.GoogleDriveFolder;
import site.easy.to.build.crm.google.model.gmail.Attachment;
import site.easy.to.build.crm.google.service.acess.GoogleAccessService;
import site.easy.to.build.crm.google.service.calendar.GoogleCalendarApiService;
import site.easy.to.build.crm.google.service.drive.GoogleDriveApiService;
import site.easy.to.build.crm.google.service.gmail.GoogleGmailApiService;
import site.easy.to.build.crm.repository.ExpenseRepository;
import site.easy.to.build.crm.service.alert.AlerteRateService;
import site.easy.to.build.crm.service.customer.CustomerService;
import site.easy.to.build.crm.service.drive.GoogleDriveFileService;
import site.easy.to.build.crm.service.file.FileService;
import site.easy.to.build.crm.service.lead.LeadActionService;
import site.easy.to.build.crm.service.lead.LeadService;
import site.easy.to.build.crm.service.settings.LeadEmailSettingsService;
import site.easy.to.build.crm.service.user.UserService;
import site.easy.to.build.crm.util.AuthenticationUtils;
import site.easy.to.build.crm.util.AuthorizationUtil;
import site.easy.to.build.crm.util.DatabaseUtil;
import site.easy.to.build.crm.util.FileUtil;
import site.easy.to.build.crm.util.LogEntityChanges;
import site.easy.to.build.crm.util.StringUtils;

@Controller
@RequestMapping("/employee/lead")
public class LeadController {

    private final LeadService leadService;
    private final AuthenticationUtils authenticationUtils;
    private final UserService userService;
    private final CustomerService customerService;
    private final LeadActionService leadActionService;
    private final GoogleCalendarApiService googleCalendarApiService;
    private final FileService fileService;
    private final GoogleDriveApiService googleDriveApiService;
    private final GoogleDriveFileService googleDriveFileService;
    private final FileUtil fileUtil;
    private final LeadEmailSettingsService leadEmailSettingsService;
    private final GoogleGmailApiService googleGmailApiService;
    private final EntityManager entityManager;
    private final ExpenseRepository expenseRepository;
    private final AlerteRateService alerteRateService;


    @Autowired
    public LeadController(LeadService leadService, AuthenticationUtils authenticationUtils, UserService userService, CustomerService customerService,
                          LeadActionService leadActionService, GoogleCalendarApiService googleCalendarApiService, FileService fileService,
                          GoogleDriveApiService googleDriveApiService, GoogleDriveFileService googleDriveFileService, FileUtil fileUtil,
                          LeadEmailSettingsService leadEmailSettingsService, GoogleGmailApiService googleGmailApiService, EntityManager entityManager,
                          ExpenseRepository expenseRepository,
                          AlerteRateService alerteRateService
                          ) {
        this.leadService = leadService;
        this.authenticationUtils = authenticationUtils;
        this.userService = userService;
        this.customerService = customerService;
        this.leadActionService = leadActionService;
        this.googleCalendarApiService = googleCalendarApiService;
        this.fileService = fileService;
        this.googleDriveApiService = googleDriveApiService;
        this.googleDriveFileService = googleDriveFileService;
        this.fileUtil = fileUtil;
        this.leadEmailSettingsService = leadEmailSettingsService;
        this.googleGmailApiService = googleGmailApiService;
        this.entityManager = entityManager;
        this.expenseRepository = expenseRepository;
        this.alerteRateService = alerteRateService;
    }

    @GetMapping("/show/{id}")
    public String showDetails(@PathVariable("id") int id, Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if(loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Lead lead = leadService.findByLeadId(id);

        if(lead == null) {
            return  "error/not-found";
        }
        User employee = lead.getEmployee();
        if(!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && !AuthorizationUtil.checkIfUserAuthorized(employee,loggedInUser)) {
            return "error/access-denied";
        }

        EventDisplay eventDisplay = null;
        String eventId = lead.getMeetingId();
        List<File> files = fileService.findByLeadId(id);
        List<Attachment> attachments = new ArrayList<>();
        for (File file : files) {
            String base64Data = Base64.getEncoder().encodeToString(file.getFileData());
            Attachment attachment = new Attachment(file.getFileName(), base64Data, file.getFileType());
            attachments.add(attachment);
        }
        if (!(authentication instanceof UsernamePasswordAuthenticationToken) && eventId != null && !eventId.isEmpty() && googleCalendarApiService != null) {
            OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
            try {
                eventDisplay = googleCalendarApiService.getEvent("primary", oAuthUser, eventId);
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        model.addAttribute("lead", lead);
        model.addAttribute("event", eventDisplay);
        model.addAttribute("attachments", attachments);
        return "lead/show-details";
    }

    @GetMapping("/assigned-leads")
    public String showAssignedEmployeeLeads(Authentication authentication, Model model) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        List<Lead> leads = leadService.findAssignedLeads(userId);
        model.addAttribute("leads", leads);
        return "lead/show-my-leads";
    }

    @GetMapping("/created-leads")
    public String showCreatedEmployeeLeads(Authentication authentication, Model model) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        List<Lead> leads = leadService.findCreatedLeads(userId);
        model.addAttribute("leads", leads);
        return "lead/show-my-leads";
    }

    @GetMapping("/manager/all-leads")
    public String showAllLeads(Model model) {
        List<Lead> leads = leadService.findAll();
        model.addAttribute("leads", leads);
        return "lead/show-my-leads";
    }

    @PostMapping("/upload")
    public ResponseEntity<Void> uploadAttachment() {
        // Simulate a successful file upload by returning a 200 OK response
        return ResponseEntity.ok().build();
    }

    @GetMapping("/create")
    public String showCreatingForm(Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User user = userService.findById(userId);
        if(user.isInactiveUser()) {
            return "error/account-inactive";
        }
        populateModelAttributes(model, authentication, user);
        model.addAttribute("lead", new Lead());
        return "lead/create-lead";
    }

    @PostMapping("/create")
    public String createLead(@ModelAttribute("lead") @Validated Lead lead, BindingResult bindingResult,
                             @RequestParam("customerId") int customerId, @RequestParam("employeeId") int employeeId,
                             Authentication authentication, @RequestParam("allFiles")@Nullable String files,
                             @RequestParam("folderId") @Nullable String folderId, Model model) throws JsonProcessingException {

        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User manager = userService.findById(userId);
        if(manager.isInactiveUser()) {
            return "error/account-inactive";
        }

        if(bindingResult.hasErrors()) {
            User user = userService.findById(userId);
            populateModelAttributes(model, authentication, user);
            return "lead/create-lead";
        }

        User employee = userService.findById(employeeId);
        Customer customer = customerService.findByCustomerId(customerId);
        if(AuthorizationUtil.hasRole(authentication, "ROLE_EMPLOYEE") && (employee.getId() != userId)) {
            return "error/500";
        }
        lead.setCustomer(customer);
        lead.setEmployee(employee);
        lead.setManager(manager);
        lead.setGoogleDriveFolderId(folderId);
        lead.setCreatedAt(LocalDateTime.now());

        ObjectMapper objectMapper = new ObjectMapper();
        List<Attachment> allFiles = objectMapper.readValue(files, new TypeReference<List<Attachment>>() {
        });

        if (!(authentication instanceof UsernamePasswordAuthenticationToken) && googleDriveApiService != null) {
            OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
            try {
                if (folderId != null && !folderId.isEmpty()) {
                    googleDriveApiService.checkFolderExists(oAuthUser, folderId);
                }
            } catch (IOException | GeneralSecurityException e) {
                return "error/500";
            }
        }

        Lead createdLead = leadService.save(lead);
        fileUtil.saveFiles(allFiles, createdLead);

        if (lead.getGoogleDrive() != null) {
            fileUtil.saveGoogleDriveFiles(authentication, allFiles, folderId, createdLead);
        }

        if (lead.getStatus().equals("meeting-to-schedule")) {
            return "redirect:/employee/calendar/create-event?leadId=" + lead.getLeadId();
        }
        if(AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            return "redirect:/employee/lead/created-leads";
        }
        return "redirect:/employee/lead/assigned-leads";
    }

    @GetMapping("/update/{id}")
    public String showUpdatingForm(Model model, @PathVariable("id") int id, Authentication authentication) {

        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if(loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Lead lead = leadService.findByLeadId(id);

        if(lead == null) {
            return "error/not-found";
        }

        User employee = lead.getEmployee();
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
            if(!Objects.equals(employee.getId(), lead.getManager().getId())) {
                customers.add(lead.getCustomer());
            } else {
                customers = customerService.findByUserId(loggedInUser.getId());
            }
        }


        List<File> files = lead.getFiles();

        List<Attachment> attachments = new ArrayList<>();
        for (File file : files) {
            String base64Data = Base64.getEncoder().encodeToString(file.getFileData());
            Attachment attachment = new Attachment(file.getFileName(), base64Data, file.getFileType());
            attachments.add(attachment);
        }

        List<GoogleDriveFolder> folders = null;

        boolean hasGoogleDriveAccess = false;
        if (!(authentication instanceof UsernamePasswordAuthenticationToken) && googleDriveApiService != null) {
            OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
            List<GoogleDriveFile> googleDriveFiles = lead.getGoogleDriveFiles();
            try {
                hasGoogleDriveAccess = authenticationUtils.checkIfAppHasAccess(GoogleAccessService.SCOPE_DRIVE, oAuthUser);
                if (hasGoogleDriveAccess) {
                    folders = googleDriveApiService.listFolders(oAuthUser);
                }

                // Check if the file got deleted using his Google Drive
                fileUtil.updateFilesBasedOnGoogleDriveFiles(oAuthUser,googleDriveFiles,lead);

            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        model.addAttribute("lead", lead);
        model.addAttribute("employees", employees);
        model.addAttribute("customers", customers);
        model.addAttribute("attachments", attachments);
        model.addAttribute("folders", folders);
        model.addAttribute("hasGoogleDriveAccess",hasGoogleDriveAccess);
        return "lead/update-lead";
    }

    @PostMapping("/update")
    public String updateLead(@ModelAttribute("lead") @Validated Lead lead, BindingResult bindingResult, @RequestParam("customerId") int customerId,
                             @RequestParam("employeeId") int employeeId, Authentication authentication, Model model,
                             @RequestParam("allFiles") @Nullable String files, @RequestParam("folderId") @Nullable String folderId) throws JsonProcessingException {

        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if(loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }
        Lead currLead = leadService.findByLeadId(lead.getLeadId());
        if(currLead == null) {
            return "error/500";
        }

        User manager = currLead.getManager();
        User employee = userService.findById(employeeId);
        Customer customer = customerService.findByCustomerId(customerId);
        if(employee == null || manager == null || customer == null) {
            return "error/500";
        }

        //check in case the employee created a lead for him/her self,
        // they won't be able to assign lead for customer that isn't created themselves
        if(manager.getId() == employeeId) {
            if (!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && customer.getUser().getId() != userId) {
                return "error/500";
            }
        } else {
            if(!AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER") && currLead.getCustomer().getCustomerId() != customerId) {
                return "error/500";
            }
        }

        if(AuthorizationUtil.hasRole(authentication, "ROLE_EMPLOYEE") && employee.getId() != userId) {
            return "error/500";
        }

        if(bindingResult.hasErrors()) {
            List<User> employees = new ArrayList<>();
            List<Customer> customers = new ArrayList<>();

            if(AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
                employees = userService.findAll();
                customers = customerService.findAll();
            } else {
                employees.add(loggedInUser);
                //In case Employee's manager assign lead for the employee with a customer that's not created by this employee
                //As a result of that the employee mustn't change the customer
                if(!Objects.equals(employee.getId(), lead.getManager().getId())) {
                    customers.add(lead.getCustomer());
                } else {
                    customers = customerService.findByUserId(loggedInUser.getId());
                }
            }
            Lead tempLead = leadService.findByLeadId(lead.getLeadId());
            lead.setEmployee(tempLead.getEmployee());
            lead.setManager(tempLead.getManager());
            lead.setLeadActions(tempLead.getLeadActions());
            lead.setCustomer(tempLead.getCustomer());
            List<File> filesArray = tempLead.getFiles();

            List<Attachment> attachments = new ArrayList<>();
            for (File file : filesArray) {
                String base64Data = Base64.getEncoder().encodeToString(file.getFileData());
                Attachment attachment = new Attachment(file.getFileName(), base64Data, file.getFileType());
                attachments.add(attachment);
            }

            List<GoogleDriveFolder> folders = null;

            boolean hasGoogleDriveAccess = false;
            if (!(authentication instanceof UsernamePasswordAuthenticationToken) && googleDriveApiService != null) {
                OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
                List<GoogleDriveFile> googleDriveFiles = tempLead.getGoogleDriveFiles();
                try {
                    hasGoogleDriveAccess = authenticationUtils.checkIfAppHasAccess(GoogleAccessService.SCOPE_DRIVE, oAuthUser);
                    if (hasGoogleDriveAccess) {
                        folders = googleDriveApiService.listFolders(oAuthUser);
                    }

                    // Check if the file got deleted using his Google Drive
                    fileUtil.updateFilesBasedOnGoogleDriveFiles(oAuthUser,googleDriveFiles,tempLead);

                } catch (IOException | GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
            }
            model.addAttribute("employees", employees);
            model.addAttribute("customers", customers);
            model.addAttribute("attachments", attachments);
            model.addAttribute("folders", folders);
            model.addAttribute("hasGoogleDriveAccess",hasGoogleDriveAccess);
            return "lead/update-lead";
        }

        Lead prevLead = leadService.findByLeadId(lead.getLeadId());
        Lead originalLead = new Lead();
        BeanUtils.copyProperties(prevLead,originalLead);
        List<File> oldFiles = fileService.findByLeadId(lead.getLeadId());
        List<GoogleDriveFile> oldGoogleDriveFiles = new ArrayList<>();
        if(googleDriveFileService != null) {
            oldGoogleDriveFiles = googleDriveFileService.getAllDriveFileByLeadId(lead.getLeadId());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<Attachment> allFiles = objectMapper.readValue(files, new TypeReference<List<Attachment>>() {
        });

        lead.setCustomer(customer);
        lead.setEmployee(employee);
        lead.setManager(manager);
        lead.setGoogleDriveFolderId(folderId);
        lead.setCreatedAt(originalLead.getCreatedAt());
        fileUtil.deleteOldFiles(oldFiles, lead);
        if (!(authentication instanceof UsernamePasswordAuthenticationToken) && googleDriveFileService != null) {
            fileUtil.deleteGoogleDriveFiles(oldGoogleDriveFiles, authentication);
        }
        fileUtil.saveFiles(allFiles, lead);
        if(!(authentication instanceof UsernamePasswordAuthenticationToken) && lead.getGoogleDrive() && googleDriveApiService != null) {
            OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
            try {
                if (folderId != null && !folderId.isEmpty()) {
                    googleDriveApiService.checkFolderExists(oAuthUser, folderId);
                }
            } catch (IOException | GeneralSecurityException e) {
                return "error/500";
            }
            fileUtil.saveGoogleDriveFiles(authentication,allFiles,folderId,lead);
        }
        Lead CurrentLead = leadService.save(lead);
        saveLeadActions(lead, prevLead);
        List<String> properties = DatabaseUtil.getColumnNames(entityManager, Lead.class);
        Map<String, Pair<String ,String>> changes = LogEntityChanges.trackChanges(originalLead,CurrentLead, properties);

        boolean isGoogleUser = !(authentication instanceof UsernamePasswordAuthenticationToken);

        if(isGoogleUser && googleGmailApiService != null) {
            OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
            if(oAuthUser.getGrantedScopes().contains(GoogleAccessService.SCOPE_GMAIL)) {
                try {
                    processEmailSettingsChanges(changes, userId, oAuthUser, customer);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return "redirect:/employee/lead/assigned-leads";
    }

    @PostMapping("/delete/{id}")
    public String deleteLead(@PathVariable("id") int id, Authentication authentication) {
        Lead lead = leadService.findByLeadId(id);

        User employee = lead.getEmployee();
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if(loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }
        if(!AuthorizationUtil.checkIfUserAuthorized(employee,loggedInUser)) {
            return "error/access-denied";
        }

        leadService.delete(lead);
        return "redirect:/employee/lead/created-leads";
    }

    @PostMapping("/save-attachment/ajax")
    @ResponseBody
    public ResponseEntity<String> saveAttachmentAjax(Authentication authentication) {
        return ResponseEntity.ok("success");
    }

    @PostMapping("/drive/ajax-create")
    @ResponseBody
    public ResponseEntity<Map<String, String>> createGoogleDriveFolder(Authentication authentication, @RequestParam("folderName") String folderName) {
        OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
        String folderId = null;
        if(googleDriveApiService != null) {
            try {
                folderId = googleDriveApiService.createFolder(oAuthUser, folderName);
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
        Map<String, String> response = new HashMap<>();
        response.put("folderId", folderId);
        response.put("folderName", folderName);
        return ResponseEntity.ok(response);
    }

    private StringBuilder getChanges(Lead prevLead, Lead lead) {
        StringBuilder changes = new StringBuilder();
        if (!prevLead.getName().equals(lead.getName())) {
            changes.append("The lead's name changes from ").append(prevLead.getName()).append(" To ").append(lead.getName()).append('.');
        }
        if (!prevLead.getPhone().equals(lead.getPhone())) {
            changes.append("The lead's phone changes from ").append(prevLead.getPhone()).append(" To ").append(lead.getPhone()).append('.');
        }
        if (!prevLead.getEmployee().equals(lead.getEmployee())) {
            changes.append("The lead's employee changes from ").append(prevLead.getEmployee().getUsername()).append(" To ").append(lead.getEmployee().getUsername()).append('.');
        }
        if (!prevLead.getCustomer().equals(lead.getCustomer())) {
            changes.append("The lead's customer changes from ").append(prevLead.getCustomer().getName()).append(" To ").append(lead.getCustomer().getName()).append('.');
        }
        return changes;
    }

    private void saveLeadActions(Lead lead, Lead prevLead) {
        StringBuilder changes = getChanges(prevLead, lead);
        List<LeadAction> leadActions = new ArrayList<>();
        if (!changes.isEmpty()) {
            LeadAction leadAction = new LeadAction();
            leadAction.setLead(lead);
            leadAction.setTimestamp(LocalDateTime.now());
            leadAction.setAction(changes.toString());
            leadActions.add(leadAction);
            leadActionService.save(leadAction);
        }
        lead.setLeadActions(leadActions);
    }

    public void processEmailSettingsChanges(Map<String, Pair<String, String>> changes, int userId, OAuthUser oAuthUser,
                                            Customer customer) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        for (Map.Entry<String, Pair<String, String>> entry : changes.entrySet()) {
            String property = entry.getKey();
            String propertyName = StringUtils.replaceCharToCamelCase(property, '_');
            propertyName = StringUtils.replaceCharToCamelCase(propertyName, ' ');

            String prevState = entry.getValue().getFirst();
            String nextState = entry.getValue().getSecond();

            LeadEmailSettings leadEmailSettings = leadEmailSettingsService.findByUserId(userId);

            CustomerLoginInfo customerLoginInfo = customer.getCustomerLoginInfo();
            LeadEmailSettings customerLeadEmailSettings = leadEmailSettingsService.findByCustomerId(customerLoginInfo.getId());

            if (leadEmailSettings != null) {
                String getterMethodName = "get" + StringUtils.capitalizeFirstLetter(propertyName);
                Method getterMethod = LeadEmailSettings.class.getMethod(getterMethodName);
                Boolean propertyValue = (Boolean) getterMethod.invoke(leadEmailSettings);

                Boolean isCustomerLikeToGetNotified = true;
                if(customerLeadEmailSettings != null) {
                    isCustomerLikeToGetNotified = (Boolean) getterMethod.invoke(customerLeadEmailSettings);
                }

                if (isCustomerLikeToGetNotified != null && propertyValue != null && propertyValue && isCustomerLikeToGetNotified) {
                    String emailTemplateGetterMethodName = "get" + StringUtils.capitalizeFirstLetter(propertyName) + "EmailTemplate";
                    Method emailTemplateGetterMethod = LeadEmailSettings.class.getMethod(emailTemplateGetterMethodName);
                    EmailTemplate emailTemplate = (EmailTemplate) emailTemplateGetterMethod.invoke(leadEmailSettings);
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

    private void populateModelAttributes(Model model, Authentication authentication, User loggedInUser) {
        List<User> employees = new ArrayList<>();
        List<Customer> customers;

        List<Attachment> attachments = new ArrayList<>();

        if(AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            employees = userService.findAll();
            customers = customerService.findAll();
        } else {
            employees.add(loggedInUser);
            customers = customerService.findByUserId(loggedInUser.getId());
        }

        List<GoogleDriveFolder> folders = null;
        boolean hasGoogleDriveAccess = false;
        if (!(authentication instanceof UsernamePasswordAuthenticationToken) && googleDriveApiService != null) {
            OAuthUser oAuthUser = authenticationUtils.getOAuthUserFromAuthentication(authentication);
            try {
                hasGoogleDriveAccess = authenticationUtils.checkIfAppHasAccess(GoogleAccessService.SCOPE_DRIVE, oAuthUser);
                if (hasGoogleDriveAccess) {
                    folders = googleDriveApiService.listFolders(oAuthUser);
                }
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        model.addAttribute("employees", employees);
        model.addAttribute("customers", customers);
        model.addAttribute("attachments", attachments);
        model.addAttribute("folders", folders);
        model.addAttribute("hasGoogleDriveAccess", hasGoogleDriveAccess);
    }

    @GetMapping("/add-expense/{leadId}")
    public String showAddExpenseForm(@PathVariable("leadId") int leadId, Model model, Authentication authentication) {
        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Lead lead = leadService.findByLeadId(leadId);
        if (lead == null) {
            return "error/not-found";
        }

        if (!AuthorizationUtil.checkIfUserAuthorized(lead.getEmployee(), loggedInUser) && !AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            return "error/access-denied";
        }

        model.addAttribute("leadId", leadId);
        return "lead/add-expense";
    }

    @PostMapping("/add-expense")
    public String addExpense(@RequestParam("leadId") int leadId,
                            @RequestParam("amount") double amount,
                            @RequestParam("expenseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {

        int userId = authenticationUtils.getLoggedInUserId(authentication);
        User loggedInUser = userService.findById(userId);
        if (loggedInUser.isInactiveUser()) {
            return "error/account-inactive";
        }

        Lead lead = leadService.findByLeadId(leadId);
        if (lead == null) {
            return "error/not-found";
        }

        // Check if the user is authorized to add an expense for this lead
        if (!AuthorizationUtil.checkIfUserAuthorized(lead.getEmployee(), loggedInUser) && !AuthorizationUtil.hasRole(authentication, "ROLE_MANAGER")) {
            return "error/access-denied";
        }

        Expense expense = new Expense(amount, expenseDate);
        BigDecimal newExpenseAmount = new BigDecimal(amount);
        
        Customer customer = lead.getCustomer();
        
        // Calculate current used budget
        BigDecimal currentUsedBudget = customerService.calculateTotalExpenses(customer);
        
        // Calculate the total budget allocation (not the remaining)
        BigDecimal totalAllocatedBudget = customerService.getTotalAllocatedBudget(customer);
        
        // Calculate what the total used budget would be after this expense
        BigDecimal projectedTotalUsed = currentUsedBudget.add(newExpenseAmount);
        
        // Calculate remaining budget
        BigDecimal remainingBudget = totalAllocatedBudget.subtract(currentUsedBudget);
        
        System.out.println("Debug: Total allocated budget: " + totalAllocatedBudget);
        System.out.println("Debug: Current used budget: " + currentUsedBudget);
        System.out.println("Debug: New expense amount: " + newExpenseAmount);
        System.out.println("Debug: Remaining budget: " + remainingBudget);
        System.out.println("Debug: Projected total used: " + projectedTotalUsed);

        // Check if adding this expense would exceed the total budget
        if (projectedTotalUsed.compareTo(totalAllocatedBudget) > 0) {
            redirectAttributes.addFlashAttribute("pendingLead", lead);
            redirectAttributes.addFlashAttribute("pendingExpense", expense);
            redirectAttributes.addFlashAttribute("exceedsBudget", true);
            redirectAttributes.addFlashAttribute("currentAmount", newExpenseAmount);
            redirectAttributes.addFlashAttribute("budget", remainingBudget);
            redirectAttributes.addFlashAttribute("totalBudget", totalAllocatedBudget);

            System.out.println("Debug: Require confirmation - budget would be exceeded");
            return "redirect:/employee/lead/confirm";
        } else {
            // Save the expense since it doesn't exceed the budget
            expenseRepository.save(expense);
            lead.setExpense(expense);
            leadService.save(lead);

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

            return "redirect:/employee/lead/assigned-leads";
        }
    }

    @GetMapping("/confirm")
    public String showConfirmationPage() {
        return "lead/confirm-expense";
    }

    @PostMapping("/confirm")
    public String confirmExpense(@RequestParam("leadId") int leadId,
                                @RequestParam("amount") double amount,
                                @RequestParam("expenseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
                                RedirectAttributes redirectAttributes) {
        Lead lead = leadService.findByLeadId(leadId);
        if (lead == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid lead data.");
            return "redirect:/employee/lead/assigned-leads";
        }

        Expense expense = new Expense(amount, expenseDate);
        expenseRepository.save(expense);
        lead.setExpense(expense);
        leadService.save(lead);

        redirectAttributes.addFlashAttribute("confirmationMessage", 
            "Expense saved despite exceeding the budget.");
        return "redirect:/employee/lead/assigned-leads";
    }
}