package site.easy.to.build.crm.service.expense;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import site.easy.to.build.crm.entity.Expense;
import site.easy.to.build.crm.repository.ExpenseRepository;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Autowired
    public ExpenseServiceImpl(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public Expense findById(int id) {
        return expenseRepository.findById(id).orElse(null);
    }

    @Override
    public Expense save(Expense expense) {
        return expenseRepository.save(expense);
    }

    @Override
    public void delete(Expense expense) {
        expenseRepository.delete(expense);
    }

    @Override
    public void deleteById(int id) {
        expenseRepository.deleteById(id);
    }

    @Override
    public List<Expense> findAll() {
        return expenseRepository.findAll();
    }    

    @Override
    public BigDecimal getTotalExpenses() {
        List<Expense> expenses = this.expenseRepository.findAll();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Expense expense : expenses) {
            totalExpenses = totalExpenses.add(new BigDecimal(expense.getAmount()));
        }

        return totalExpenses;
    }
}