package site.easy.to.build.crm.service.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import site.easy.to.build.crm.entity.Customer;
import site.easy.to.build.crm.entity.CustomerBudget;
import site.easy.to.build.crm.entity.Expense;
import site.easy.to.build.crm.entity.Lead;
import site.easy.to.build.crm.entity.Ticket;
import site.easy.to.build.crm.entity.User;
import site.easy.to.build.crm.repository.CustomerBudgetRepository;
import site.easy.to.build.crm.repository.CustomerRepository;
import site.easy.to.build.crm.repository.ExpenseRepository;
import site.easy.to.build.crm.repository.LeadRepository;
import site.easy.to.build.crm.repository.TicketRepository;
import site.easy.to.build.crm.repository.UserRepository;

@Service
@SuppressWarnings("deprecation")
public class CsvImportService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerBudgetRepository customerBudgetRepository; 
    
    private final int USER_ID = 52; // fix value because `users` table can't be deleted

    public static class ImportError {

        private String fileName;
        private int lineNumber;
        private String errorMessage;

        public ImportError(String fileName, int lineNumber, String errorMessage) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.errorMessage = errorMessage;
        }

        public String getFileName() {
            return fileName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    @Transactional
    public List<ImportError> importMultipleCsvFiles(Map<String, MultipartFile> fileMap) {
        List<ImportError> errors = new ArrayList<>();

        // Validate all files first (Tout ou rien principle)
        for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
            String fileName = entry.getKey();
            MultipartFile file = entry.getValue();

            if (fileName.equals("customersFile")) {
                validateCustomersCsvFile(file, errors);
            } else if (fileName.equals("budgetsFile")) {
                validateBudgetsCsvFile(file, errors); 
            } else if (fileName.equals("itemsFile")) {
                validateItemsCsvFile(file, errors);
            }
        }

        // If there are any errors, return them without inserting data
        if (!errors.isEmpty()) {
            return errors;
        }

        // Process files in order: Customers → Budgets → Items
        Map<String, Customer> customerEmailMap = new HashMap<>();
        
        try {
            // 1. Process customers file
            if (fileMap.containsKey("customersFile")) {
                MultipartFile customersFile = fileMap.get("customersFile");
                customerEmailMap = processCustomersFile(customersFile);
            }
    
            // 2. Process budgets file
            if (fileMap.containsKey("budgetsFile")) {
                MultipartFile budgetsFile = fileMap.get("budgetsFile");
                processBudgetsFile(budgetsFile, customerEmailMap); 
            }
    
            // 3. Process items file (tickets, leads, expenses)
            if (fileMap.containsKey("itemsFile")) {
                MultipartFile itemsFile = fileMap.get("itemsFile");
                processItemsFile(itemsFile, customerEmailMap);
            }
        } catch (Exception e) {
            errors.add(new ImportError("General Error", 0, "Unexpected error: " + e.getMessage()));
        }

        return errors;
    }

    private void validateBudgetsCsvFile(MultipartFile file, List<ImportError> errors) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
                     .withIgnoreHeaderCase().withTrim())) {

            int lineNumber = 2;
            for (CSVRecord csvRecord : csvParser) {
                if (!csvRecord.isSet("customer_email") || csvRecord.get("customer_email").isEmpty()) {
                    errors.add(new ImportError("budgetsFile", lineNumber, "Missing customer email"));
                } else if (!isValidEmail(csvRecord.get("customer_email"))) {
                    errors.add(new ImportError("budgetsFile", lineNumber, "Invalid email format"));
                }

                if (!csvRecord.isSet("Budget") || csvRecord.get("Budget").isEmpty()) {
                    errors.add(new ImportError("budgetsFile", lineNumber, "Missing Budget"));
                } else {
                    try {
                        String budgetStr = csvRecord.get("Budget").replace(",", ".");
                        BigDecimal budget = new BigDecimal(budgetStr);
                        if (budget.compareTo(BigDecimal.ZERO) <= 0) {
                            errors.add(new ImportError("budgetsFile", lineNumber, "Budget cannot be negative or equals to 0"));
                        }
                    } catch (NumberFormatException e) {
                        errors.add(new ImportError("budgetsFile", lineNumber, "Invalid Budget format"));
                    }
                }

                lineNumber++;
            }
        } catch (IOException e) {
            errors.add(new ImportError("budgetsFile", 0, "Error reading CSV file: " + e.getMessage()));
        }
    }

    private void processBudgetsFile(MultipartFile file, Map<String, Customer> customerEmailMap) 
        throws IOException 
    {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
                     .withIgnoreHeaderCase().withTrim())) {

            User user = userRepository.findById(USER_ID);
            if (user == null) {
                throw new IllegalStateException("Fixed user with ID " + USER_ID + " not found");
            }

            int processedRecords = 0;
            for (CSVRecord csvRecord : csvParser) {
                String customerEmail = csvRecord.get("customer_email");
                String budgetStr = csvRecord.get("Budget").replace(",", "."); 
                BigDecimal budget = new BigDecimal(budgetStr);

                Customer customer = customerEmailMap.get(customerEmail);
                if (customer == null) {
                    System.out.println("Customer not found for email: " + customerEmail); // Debugging
                    continue; 
                }

                CustomerBudget customerBudget = new CustomerBudget();
                customerBudget.setCustomer(customer);
                customerBudget.setLabel(generateRandomLabel());
                customerBudget.setAmount(budget); 
                customerBudget.setTransactionDate(LocalDate.now());
                customerBudget.setCreatedAt(LocalDateTime.now());
                customerBudget.setUser(user);

                customerBudgetRepository.save(customerBudget);
                processedRecords++;
            }
            System.out.println("Processed " + processedRecords + " budget records"); // Debugging
        } catch (Exception e) {
            System.err.println("Error processing budgets file: " + e.getMessage()); // Debugging
            throw e; // Re-throw to ensure transaction rollback
        }
    }

    private String generateRandomLabel() {
        String[] labels = {
            "Annual Budget", "Project Funding", "Marketing Allocation", 
            "Operational Costs", "Client Investment", "R&D Budget"
        };
        return labels[new Random().nextInt(labels.length)];
    }

    private void validateCustomersCsvFile(MultipartFile file, List<ImportError> errors) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
                     .withIgnoreHeaderCase().withTrim())) {

            int lineNumber = 2; // Start from line 2 as line 1 is header
            for (CSVRecord csvRecord : csvParser) {
                // Check for required fields
                if (!csvRecord.isSet("customer_email") || csvRecord.get("customer_email").isEmpty()) {
                    errors.add(new ImportError("customersFile", lineNumber, "Missing customer email"));
                } else if (!isValidEmail(csvRecord.get("customer_email"))) {
                    errors.add(new ImportError("customersFile", lineNumber, "Invalid email format"));
                }

                if (!csvRecord.isSet("customer_name") || csvRecord.get("customer_name").isEmpty()) {
                    errors.add(new ImportError("customersFile", lineNumber, "Missing customer name"));
                }
                
                lineNumber++; // pass to the next line
            }
        } catch (IOException e) {
            errors.add(new ImportError("customersFile", 0, "Error reading CSV file: " + e.getMessage()));
        }
    }

    private void validateItemsCsvFile(MultipartFile file, List<ImportError> errors) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             @SuppressWarnings("deprecation")
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
                     .withIgnoreHeaderCase().withTrim())) {

            int lineNumber = 2; // Start from line 2 as line 1 is header
            for (CSVRecord csvRecord : csvParser) {
                // Check for required fields
                if (!csvRecord.isSet("customer_email") || csvRecord.get("customer_email").isEmpty()) {
                    errors.add(new ImportError("itemsFile", lineNumber, "Missing customer email"));
                } else if (!isValidEmail(csvRecord.get("customer_email"))) {
                    errors.add(new ImportError("itemsFile", lineNumber, "Invalid email format"));
                }

                if (!csvRecord.isSet("type") || csvRecord.get("type").isEmpty()) {
                    errors.add(new ImportError("itemsFile", lineNumber, "Missing type"));
                } else {
                    String type = csvRecord.get("type");

                    // managed type, only lead and ticket for now
                    if (!type.equals("lead") && !type.equals("ticket")) {
                        errors.add(new ImportError("itemsFile", lineNumber, "Invalid type: must be 'lead' or 'ticket'"));
                    }
                }

                if (!csvRecord.isSet("status") || csvRecord.get("status").isEmpty()) {
                    errors.add(new ImportError("itemsFile", lineNumber, "Missing status"));
                } else {
                    String status = csvRecord.get("status");
                    String type = csvRecord.get("type");
                    
                    if (type.equals("ticket") && !isValidTicketStatus(status)) {
                        errors.add(new ImportError("itemsFile", lineNumber, "Invalid ticket status"));
                    } else if (type.equals("lead") && !isValidLeadStatus(status)) {
                        errors.add(new ImportError("itemsFile", lineNumber, "Invalid lead status"));
                    }
                }

                if (!csvRecord.isSet("subject_or_name") || csvRecord.get("subject_or_name").isEmpty()) {
                    errors.add(new ImportError("itemsFile", lineNumber, "Missing subject/name"));
                }
                
                if (!csvRecord.isSet("expense") || csvRecord.get("expense").isEmpty()) {
                    errors.add(new ImportError("itemsFile", lineNumber, "Missing expense amount"));
                } else {
                    try {
                        String expenseStr = csvRecord.get("expense").replace(",", ".");
                        double expense = Double.parseDouble(expenseStr);
                        if (expense < 0) {
                            errors.add(new ImportError("itemsFile", lineNumber, "Expense amount cannot be negative"));
                        }
                    } catch (NumberFormatException e) {
                        errors.add(new ImportError("itemsFile", lineNumber, "Invalid expense amount format"));
                    }
                }

                lineNumber++;
            }
        } catch (IOException e) {
            errors.add(new ImportError("itemsFile", 0, "Error reading CSV file: " + e.getMessage()));
        }
    }

    private Map<String, Customer> processCustomersFile(MultipartFile file) throws IOException {
        Map<String, Customer> customerEmailMap = new HashMap<>();
        
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
                     .withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord csvRecord : csvParser) {
                String email = csvRecord.get("customer_email");
                String name = csvRecord.get("customer_name");
                
                // Check if customer already exists
                Customer existingCustomer = customerRepository.findByEmail(email);
                
                if (existingCustomer == null) {
                    // Create new customer
                    Customer customer = new Customer();
                    customer.setEmail(email);
                    customer.setName(name);
                    
                    // Set default values for required fields
                    customer.setCountry(generateRandomCountry());
                    customer.setCreatedAt(LocalDateTime.now());
                    
                    // Get fixed user
                    User user = userRepository.findById(USER_ID);
                    customer.setUser(user);
                    
                    // Generate random values for additional fields
                    customer.setPhone(generateRandomPhone());
                    customer.setAddress(generateRandomAddress());
                    customer.setCity(generateRandomCity());
                    customer.setState(generateRandomState());
                    customer.setDescription(generateRandomDescription());
                    customer.setPosition(generateRandomPosition());
                    customer.setTwitter(generateRandomSocialMedia("twitter"));
                    customer.setFacebook(generateRandomSocialMedia("facebook"));
                    customer.setYoutube(generateRandomSocialMedia("youtube"));
                    
                    Customer savedCustomer = customerRepository.save(customer);
                    customerEmailMap.put(email, savedCustomer);
                } else {
                    customerEmailMap.put(email, existingCustomer);
                }
            }
        }
        
        return customerEmailMap;
    }

    private void processItemsFile(MultipartFile file, Map<String, Customer> customerEmailMap) 
        throws IOException 
    {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
                     .withIgnoreHeaderCase().withTrim())) {

            // Get the fixed user
            User user = userRepository.findById(USER_ID);

            for (CSVRecord csvRecord : csvParser) {
                String customerEmail = csvRecord.get("customer_email");
                String type = csvRecord.get("type");
                String status = csvRecord.get("status");
                String subjectOrName = csvRecord.get("subject_or_name");
                String expenseStr = csvRecord.get("expense").replace(",", "."); 
                double expenseAmount = Double.parseDouble(expenseStr);
                
                // Get the customer from the map
                Customer customer = customerEmailMap.get(customerEmail);
                
                // If customer not in map (shouldn't happen after validation)
                if (customer == null) {
                    continue;
                }
                
                // Create expense
                Expense expense = new Expense();

                expense.setAmount(expenseAmount);
                expense.setExpenseDate(LocalDate.now());

                Expense savedExpense = expenseRepository.save(expense);
                
                if (type.equals("ticket")) {
                    // Create ticket
                    Ticket ticket = new Ticket();

                    ticket.setSubject(subjectOrName);
                    ticket.setDescription(generateRandomDescription());
                    ticket.setStatus(status);
                    ticket.setCustomer(customer);
                    ticket.setExpense(savedExpense);
                    ticket.setCreatedAt(LocalDateTime.now());
                    
                    // Set defaults for required fields
                    ticket.setPriority("medium");
                    
                    // Set the manager using the fixed USER_ID
                    ticket.setManager(user);
                    ticketRepository.save(ticket);
                } else if (type.equals("lead")) {
                    // Create lead
                    Lead lead = new Lead();

                    lead.setName(subjectOrName);
                    lead.setStatus(status);
                    lead.setCustomer(customer);
                    lead.setExpense(savedExpense);
                    lead.setCreatedAt(LocalDateTime.now());
                    lead.setPhone(generateRandomPhone());
                    
                    // Set the manager using the fixed USER_ID
                    lead.setManager(user);
                    
                    leadRepository.save(lead);
                }
            }
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidTicketStatus(String status) {
        return status.matches("^(open|assigned|on-hold|in-progress|resolved|closed|reopened|pending-customer-response|escalated|archived)$");
    }

    private boolean isValidLeadStatus(String status) {
        return status.matches("^(meeting-to-schedule|scheduled|archived|success|assign-to-sales)$");
    }

    private String generateRandomCountry() {
        String[] countries = {"USA", "France", "Germany", "UK", "Canada", "Australia", "Japan"};
        return countries[new Random().nextInt(countries.length)];
    }
    
    private String generateRandomPhone() {
        Random random = new Random();
        StringBuilder phoneNumber = new StringBuilder("+33 ");
        
        // Generate first digit (usually 6 or 7 for mobile in France)
        phoneNumber.append(random.nextInt(2) + 6);
        
        // Generate remaining 8 digits
        for (int i = 0; i < 8; i++) {
            if (i % 2 == 0) {
                phoneNumber.append(" ");
            }
            phoneNumber.append(random.nextInt(10));
        }
        
        return phoneNumber.toString();
    }
    
    private String generateRandomAddress() {
        String[] streetNumbers = {"12", "45", "78", "123", "256", "8", "42"};
        String[] streetNames = {"Main Street", "Park Avenue", "Boulevard Saint-Michel", "Rue de la Paix", 
                               "Broadway", "Baker Street", "Via Roma"};
        
        Random random = new Random();
        return streetNumbers[random.nextInt(streetNumbers.length)] + " " + 
               streetNames[random.nextInt(streetNames.length)];
    }
    
    private String generateRandomCity() {
        String[] cities = {"Paris", "New York", "London", "Tokyo", "Berlin", "Rome", "Sydney", "Madrid"};
        return cities[new Random().nextInt(cities.length)];
    }
    
    private String generateRandomState() {
        String[] states = {"Île-de-France", "New York", "California", "Texas", "Bavaria", "Lazio", "New South Wales"};
        return states[new Random().nextInt(states.length)];
    }
    
    private String generateRandomDescription() {
        String[] descriptions = {
            "Customer interested in our premium services.",
            "New lead from the marketing campaign.",
            "Long-term client with multiple projects.",
            "Potential partnership opportunity.",
            "First-time customer referred by an existing client.",
            "International client requiring special attention.",
            "Client with high-value potential projects."
        };
        return descriptions[new Random().nextInt(descriptions.length)];
    }
    
    private String generateRandomPosition() {
        String[] positions = {"CEO", "CTO", "Marketing Director", "Project Manager", 
                             "Sales Representative", "HR Manager", "Operations Director"};
        return positions[new Random().nextInt(positions.length)];
    }
    
    private String generateRandomSocialMedia(String platform) {
        if (platform.equals("twitter")) {
            return "https://twitter.com/user" + new Random().nextInt(10000);
        } else if (platform.equals("facebook")) {
            return "https://facebook.com/profile" + new Random().nextInt(10000);
        } else if (platform.equals("youtube")) {
            return "https://youtube.com/channel" + new Random().nextInt(10000);
        }
        return "";
    }
}