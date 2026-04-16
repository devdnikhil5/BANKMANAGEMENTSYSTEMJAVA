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

import com.bank.management.dto.ManagerDto;
import com.bank.management.dto.UserDto;
import com.bank.management.service.AdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // Manager Management

    @PostMapping("/managers")
    public ResponseEntity<ManagerDto> createManager(@Valid @RequestBody ManagerDto managerDto) {
        ManagerDto createdManager = adminService.createManager(managerDto);
        return new ResponseEntity<>(createdManager, HttpStatus.CREATED);
    }

    @GetMapping("/managers")
    public ResponseEntity<List<ManagerDto>> getAllManagers() {
        List<ManagerDto> managers = adminService.getAllManagers();
        return ResponseEntity.ok(managers);
    }

    @GetMapping("/managers/{id}")
    public ResponseEntity<ManagerDto> getManagerById(@PathVariable Long id) {
        ManagerDto manager = adminService.getManagerById(id);
        return ResponseEntity.ok(manager);
    }

    @PutMapping("/managers/{id}")
    public ResponseEntity<ManagerDto> updateManager(@PathVariable Long id, @Valid @RequestBody ManagerDto managerDto) {
        ManagerDto updatedManager = adminService.updateManager(id, managerDto);
        return ResponseEntity.ok(updatedManager);
    }

    @DeleteMapping("/managers/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable Long id) {
        adminService.deleteManager(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/managers/{id}/toggle-status")
    public ResponseEntity<ManagerDto> toggleManagerStatus(@PathVariable Long id) {
        ManagerDto manager = adminService.toggleManagerStatus(id);
        return ResponseEntity.ok(manager);
    }

    // User Overview (All Users) 

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = adminService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // System Statistics 

    @GetMapping("/statistics")
    public ResponseEntity<?> getSystemStatistics() {
        return ResponseEntity.ok(adminService.getSystemStatistics());
    }
}