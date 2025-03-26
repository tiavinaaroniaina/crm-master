package site.easy.to.build.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import site.easy.to.build.crm.entity.Expense;

public class LeadDTO {
    private int leadId;
    private String name;
    private String status;
    private String phone;
    private Integer managerId;
    private Integer employeeId;
    private Integer customerId;
    private Expense expense;
    private LocalDateTime createdAt;
    private BigDecimal totalAllocatedBudget;
    private BigDecimal currentUsedBudget;
    private BigDecimal alertRatePercentage;

    public LeadDTO(int leadId, String name, String status, String phone,
                   Integer managerId, Integer employeeId, Integer customerId,
                   Expense expense, LocalDateTime createdAt,
                   BigDecimal totalAllocatedBudget, BigDecimal currentUsedBudget,
                   BigDecimal alertRatePercentage) {
        this.leadId = leadId;
        this.name = name;
        this.status = status;
        this.phone = phone;
        this.managerId = managerId;
        this.employeeId = employeeId;
        this.customerId = customerId;
        this.expense = expense;
        this.createdAt = createdAt;
        this.totalAllocatedBudget = totalAllocatedBudget;
        this.currentUsedBudget = currentUsedBudget;
        this.alertRatePercentage = alertRatePercentage;
    }

    // Getters and Setters
    public int getLeadId() { return leadId; }
    public void setLeadId(int leadId) { this.leadId = leadId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getManagerId() { return managerId; }
    public void setManagerId(Integer managerId) { this.managerId = managerId; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public BigDecimal getTotalAllocatedBudget() { return totalAllocatedBudget; }
    public void setTotalAllocatedBudget(BigDecimal totalAllocatedBudget) { this.totalAllocatedBudget = totalAllocatedBudget; }
    public BigDecimal getCurrentUsedBudget() { return currentUsedBudget; }
    public void setCurrentUsedBudget(BigDecimal currentUsedBudget) { this.currentUsedBudget = currentUsedBudget; }
    public BigDecimal getAlertRatePercentage() { return alertRatePercentage; }
    public void setAlertRatePercentage(BigDecimal alertRatePercentage) { this.alertRatePercentage = alertRatePercentage; }
}