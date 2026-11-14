package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    // Dashboard: statistics, forms, customers table, latest transactions
    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        loadDashboard(model, keyword);
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        return "bank";
    }

    // Add a new customer
    @PostMapping("/customers")
    public String addCustomer(@RequestParam String accountNumber,
                              @RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam BigDecimal initialBalance,
                              RedirectAttributes redirectAttributes) {
        try {
            bankService.createCustomer(accountNumber.trim(), fullName.trim(), email.trim(), initialBalance);
            redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully.");
        } catch (BankException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/";
    }

    // Transfer money between accounts
    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadDashboard(model, null);
            return "bank";
        }

        try {
            bankService.transfer(
                    request.getFromAccount().trim(),
                    request.getToAccount().trim(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully.");
        } catch (BankException ex) {
            // Runs AFTER transfer() rolls back; records FAILED log in a separate REQUIRES_NEW transaction
            auditService.recordFailure(
                    request.getFromAccount().trim(),
                    request.getToAccount().trim(),
                    request.getAmount(),
                    ex.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage() + " - All changes were rolled back.");
        }
        return "redirect:/";
    }

    // Account transaction history page
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        Customer customer = bankService.getCustomer(accountNumber);
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", bankService.getAccountHistory(accountNumber));
        return "history";
    }

    private void loadDashboard(Model model, String keyword) {
        model.addAttribute("customers", bankService.getAllCustomers(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("customerCount", bankService.getCustomerCount());
        model.addAttribute("successCount", bankService.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countByStatus(TransactionStatus.FAILED));
        model.addAttribute("transactions", bankService.getLatestTransactions());
    }
}