package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.countCustomersNative() == 0) {
            customerRepository.save(new Customer("100001", "Kamal Silva", "kamal@example.com", new BigDecimal("50000.00")));
            customerRepository.save(new Customer("100002", "Nimali Perera", "nimali@example.com", new BigDecimal("25000.00")));
            customerRepository.save(new Customer("100003", "Saman Fernando", "saman@example.com", new BigDecimal("10000.00")));
        }
    }
}