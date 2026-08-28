package com.example.expensetracker.controller;

import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.User;
import com.example.expensetracker.repository.ExpenseRepository;
import com.example.expensetracker.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        List<Expense> expenses = expenseRepository.findByUser(user);
        model.addAttribute("expenses", expenses);
        return "dashboard";
    }

    @GetMapping("/add-expense")
    public String addExpenseForm(Model model) {
        model.addAttribute("expense", new Expense());
        return "add-expense";
    }

    @PostMapping("/add-expense")
    public String saveExpense(@ModelAttribute Expense expense, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        expense.setUser(user);
        expenseRepository.save(expense);
        return "redirect:/dashboard";
    }

    @GetMapping("/filter")
    public String filterByCategory(@RequestParam String category, Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        List<Expense> expenses = expenseRepository.findByUserAndCategory(user, category);
        model.addAttribute("expenses", expenses);
        return "dashboard";
    }
}
