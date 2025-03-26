package site.easy.to.build.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BudgetDTO {
    
    private Integer budgetId;
    private Integer customerId;
    private String customerName;
    private String label;  
    private BigDecimal amount;
    private LocalDate transactionDate;

    public BudgetDTO(Integer budgetId, Integer customerId, String customerName, String label, BigDecimal amount,
            LocalDate transactionDate) {
        this.budgetId = budgetId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.label = label;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    public Integer getBudgetId() {
        return budgetId;
    }
    public void setBudgetId(Integer budgetId) {
        this.budgetId = budgetId;
    }
    public Integer getCustomerId() {
        return customerId;
    }
    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }
    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public LocalDate getTransactionDate() {
        return transactionDate;
    }
    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    
}
