package com.bank.management.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ManagerService {

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

    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }
        if (userDto.getPassword() == null || userDto.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setFullName(userDto.getFullName());
        user.setActive(userDto.isActive());

        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: User role not found."));
        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setUser(savedUser);
        account.setBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        return mapToUserDto(savedUser);
    }

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
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFullName(userDto.getFullName());
        user.setEmail(userDto.getEmail());
        user.setActive(userDto.isActive());

        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            if (userDto.getPassword().length() < 6) {
                throw new RuntimeException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return mapToUserDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        // 1. Clear approved_by references
        transactionRepository.clearApprovedByReferences(id);
        // 2. Delete all transactions involving accounts owned by this user
        transactionRepository.deleteAllByUserId(id);
        // 3. Delete the account
        accountRepository.deleteByUserId(id);
        // 4. Delete user roles
        userRepository.deleteUserRoles(id);
        // 5. Delete user
        userRepository.deleteById(id);
    }

    @Transactional
    public UserDto toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(!user.isActive());
        User updatedUser = userRepository.save(user);
        return mapToUserDto(updatedUser);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            int random = (int) (Math.random() * 100000000);
            accountNumber = String.format("ACC%08d", random);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
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