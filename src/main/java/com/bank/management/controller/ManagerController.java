package com.bank.management.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.management.dto.ApproveTransactionRequest;
import com.bank.management.dto.TransactionResponse;
import com.bank.management.dto.UserDto;
import com.bank.management.service.ManagerService;
import com.bank.management.service.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    @Autowired
    private ManagerService managerService;

    @Autowired
    private TransactionService transactionService;

    // User Management

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        UserDto createdUser = managerService.createUser(userDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = managerService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = managerService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        UserDto updatedUser = managerService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        managerService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<UserDto> toggleUserStatus(@PathVariable Long id) {
        UserDto user = managerService.toggleUserStatus(id);
        return ResponseEntity.ok(user);
    }

    // Transaction Approval

    @GetMapping("/transactions/pending")
    public ResponseEntity<List<TransactionResponse>> getPendingTransactions() {
        List<TransactionResponse> pendingTransactions = transactionService.getPendingTransactions();
        return ResponseEntity.ok(pendingTransactions);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        List<TransactionResponse> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @PatchMapping("/transactions/{id}/approve")
    public ResponseEntity<TransactionResponse> approveTransaction(@PathVariable Long id,
                                                                  @RequestBody ApproveTransactionRequest request) {
        TransactionResponse transaction = transactionService.approveTransaction(id, request.getManagerComment());
        return ResponseEntity.ok(transaction);
    }

    @PatchMapping("/transactions/{id}/reject")
    public ResponseEntity<TransactionResponse> rejectTransaction(@PathVariable Long id,
                                                                 @RequestBody ApproveTransactionRequest request) {
        TransactionResponse transaction = transactionService.rejectTransaction(id, request.getManagerComment());
        return ResponseEntity.ok(transaction);
    }
}