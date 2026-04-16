package com.bank.management.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.management.dto.ChangePasswordRequest;
import com.bank.management.dto.UserDto;
import com.bank.management.exception.ResourceNotFoundException;
import com.bank.management.exception.UnauthorizedException;
import com.bank.management.model.User;
import com.bank.management.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return mapToUserDto(user);
    }

    @Transactional
    public UserDto updateUserByUsername(String username, UserDto userDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (userDto.getFullName() != null && !userDto.getFullName().isEmpty()) {
            user.setFullName(userDto.getFullName());
        }
        if (userDto.getEmail() != null && !userDto.getEmail().isEmpty()) {
            if (userRepository.existsByEmail(userDto.getEmail()) && !user.getEmail().equals(userDto.getEmail())) {
                throw new RuntimeException("Error: Email is already in use!");
            }
            user.setEmail(userDto.getEmail());
        }
        // Password is never updated here – use changePassword endpoint
        User updatedUser = userRepository.save(user);
        return mapToUserDto(updatedUser);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
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