package com.bank.management.controller;

import com.bank.management.dto.*;
import com.bank.management.service.TransactionService;
import com.bank.management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        UserDto userDto = userService.getUserByUsername(getCurrentUsername());
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        // Build a UserDto with only the fields we allow to update
        UserDto userDto = new UserDto();
        userDto.setUsername(getCurrentUsername()); // for service lookup
        userDto.setFullName(request.getFullName());
        userDto.setEmail(request.getEmail());
        // password is intentionally left null

        UserDto updatedUser = userService.updateUserByUsername(getCurrentUsername(), userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(getCurrentUsername(), request);
        return ResponseEntity.ok().body("{\"message\":\"Password changed successfully\"}");
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getAccountBalance() {
        UserDto userDto = userService.getUserByUsername(getCurrentUsername());
        return ResponseEntity.ok().body("{\"balance\": " + userDto.getBalance() + "}");
    }

    @PostMapping("/transactions/deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createDeposit(getCurrentUsername(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/transactions/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createWithdrawal(getCurrentUsername(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/transactions/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransfer(getCurrentUsername(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions() {
        List<TransactionResponse> transactions = transactionService.getTransactionsForUser(getCurrentUsername());
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/pending")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransactionResponse>> getMyPendingTransactions() {
        List<TransactionResponse> pending = transactionService.getPendingTransactionsForUser(getCurrentUsername());
        return ResponseEntity.ok(pending);
    }
}