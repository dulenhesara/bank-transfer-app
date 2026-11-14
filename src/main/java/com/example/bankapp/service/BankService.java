package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public List<Customer> getAllCustomers(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return customerRepository.searchByName(keyword.trim());
        }
        return customerRepository.findAllOrdered();
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    public long getCustomerCount() {
        return customerRepository.countCustomersNative();
    }

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public long countByStatus(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    @Transactional
    public Customer createCustomer(String accountNumber, String fullName, String email, BigDecimal initialBalance) {
        if (customerRepository.countAccounts(accountNumber) > 0) {
            throw new BankException("Account number already exists: " + accountNumber);
        }
        if (customerRepository.countEmails(email) > 0) {
            throw new BankException("Email already exists: " + email);
        }
        Customer customer = new Customer(accountNumber, fullName, email, initialBalance);
        return customerRepository.save(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        // Step 1: Validate accounts are different and both exist
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver must be different accounts");
        }
        Customer sender = customerRepository.getByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found: " + fromAccount));
        Customer receiver = customerRepository.getByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found: " + toAccount));

        // Step 2: Atomic debit
        int updated = customerRepository.debit(sender.getAccountNumber(), amount);
        if (updated == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + fromAccount);
        }

        // Step 3: Simulated failure experiment
        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal");
        }

        // Step 4: Credit receiver
        customerRepository.credit(receiver.getAccountNumber(), amount);

        // Step 5: Save SUCCESS record
        BankTransaction tx = new BankTransaction(
                fromAccount,
                toAccount,
                amount,
                TransactionStatus.SUCCESS,
                "Transfer completed"
        );
        return transactionRepository.save(tx);
    }
}