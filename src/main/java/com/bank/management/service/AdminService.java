package com.bank.management.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.management.dto.ManagerDto;
import com.bank.management.dto.UserDto;
import com.bank.management.exception.ResourceNotFoundException;
import com.bank.management.model.Account;
import com.bank.management.model.Role;
import com.bank.management.model.Role.ERole;
import com.bank.management.model.User;
import com.bank.management.repository.AccountRepository;
import com.bank.management.repository.RoleRepository;
import com.bank.management.repository.TransactionRepository;
import com.bank.management.repository.UserRepository;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Manager Management

    @Transactional
    public ManagerDto createManager(ManagerDto managerDto) {
        if (userRepository.existsByUsername(managerDto.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(managerDto.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User manager = new User();
        manager.setUsername(managerDto.getUsername());
        manager.setEmail(managerDto.getEmail());
        manager.setPassword(passwordEncoder.encode(managerDto.getPassword()));
        manager.setFullName(managerDto.getFullName());
        manager.setActive(managerDto.isActive());

        Role managerRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                .orElseThrow(() -> new RuntimeException("Error: Manager role not found."));
        manager.setRoles(Set.of(managerRole));

        User savedManager = userRepository.save(manager);
        return mapToManagerDto(savedManager);
    }

    public List<ManagerDto> getAllManagers() {
        List<User> managers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_MANAGER))
                .collect(Collectors.toList());
        return managers.stream().map(this::mapToManagerDto).collect(Collectors.toList());
    }

    public ManagerDto getManagerById(Long id) {
        User manager = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + id));
        if (manager.getRoles().stream().noneMatch(r -> r.getName() == ERole.ROLE_MANAGER)) {
            throw new ResourceNotFoundException("User is not a manager");
        }
        return mapToManagerDto(manager);
    }

    @Transactional
    public ManagerDto updateManager(Long id, ManagerDto managerDto) {
        User manager = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + id));

        manager.setFullName(managerDto.getFullName());
        manager.setEmail(managerDto.getEmail());
        manager.setActive(managerDto.isActive());

        if (managerDto.getPassword() != null && !managerDto.getPassword().isEmpty()) {
            if (managerDto.getPassword().length() < 6) {
                throw new RuntimeException("Password must be at least 6 characters");
            }
            manager.setPassword(passwordEncoder.encode(managerDto.getPassword()));
        }

        User updatedManager = userRepository.save(manager);
        return mapToManagerDto(updatedManager);
    }

    @Transactional
    public void deleteManager(Long id) {
        // 1. Clear approved_by references (managers may have approved transactions)
        transactionRepository.clearApprovedByReferences(id);
        // 2. Delete all transactions involving accounts owned by this user
        transactionRepository.deleteAllByUserId(id);
        // 3. Delete the account (if any)
        accountRepository.deleteByUserId(id);
        // 4. Delete user roles
        userRepository.deleteUserRoles(id);
        // 5. Finally delete the user
        userRepository.deleteById(id);
    }

    @Transactional
    public ManagerDto toggleManagerStatus(Long id) {
        User manager = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + id));
        manager.setActive(!manager.isActive());
        User updatedManager = userRepository.save(manager);
        return mapToManagerDto(updatedManager);
    }

    // User Overview

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_USER))
                .collect(Collectors.toList());
        return users.stream().map(this::mapToUserDto).collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserDto(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        // 1. Clear approved_by references (users are unlikely to have approved, but safe)
        transactionRepository.clearApprovedByReferences(id);
        // 2. Delete all transactions involving this user's account
        transactionRepository.deleteAllByUserId(id);
        // 3. Delete the account
        accountRepository.deleteByUserId(id);
        // 4. Delete user roles
        userRepository.deleteUserRoles(id);
        // 5. Delete user
        userRepository.deleteById(id);
    }

    // Statistics

    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<User> allUsers = userRepository.findAll();

        long totalUsers = allUsers.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_USER))
                .count();
        long totalManagers = allUsers.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_MANAGER))
                .count();
        long activeUsers = allUsers.stream()
                .filter(u -> u.isActive() && u.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_USER))
                .count();

        BigDecimal totalDeposits = accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalUsers", totalUsers);
        stats.put("totalManagers", totalManagers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalDeposits", totalDeposits);
        return stats;
    }

    // Helper Methods 

    private ManagerDto mapToManagerDto(User user) {
        ManagerDto dto = new ManagerDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : null);
        return dto;
    }

    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : null);

        if (user.getAccount() != null) {
            dto.setAccountNumber(user.getAccount().getAccountNumber());
            dto.setBalance(user.getAccount().getBalance());
        } else {
            dto.setBalance(BigDecimal.ZERO);
        }
        return dto;
    }
}