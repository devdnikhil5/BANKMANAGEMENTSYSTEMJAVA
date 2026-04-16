package com.bank.management.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.management.dto.TransactionRequest;
import com.bank.management.dto.TransactionResponse;
import com.bank.management.exception.ResourceNotFoundException;
import com.bank.management.exception.UnauthorizedException;
import com.bank.management.model.Account;
import com.bank.management.model.ApprovalStatus;
import com.bank.management.model.Transaction;
import com.bank.management.model.Transaction.TransactionType;
import com.bank.management.model.User;
import com.bank.management.repository.AccountRepository;
import com.bank.management.repository.TransactionRepository;
import com.bank.management.repository.UserRepository;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    // Helper: get current authenticated username
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    // Helper: get current authenticated user
    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Helper: convert Transaction entity to TransactionResponse DTO
    private TransactionResponse mapToResponse(Transaction tx) {
        TransactionResponse resp = new TransactionResponse();
        resp.setId(tx.getId());
        resp.setTransactionType(tx.getTransactionType().name());
        resp.setAmount(tx.getAmount());
        resp.setDescription(tx.getDescription());
        resp.setStatus(tx.getStatus());
        resp.setFromAccountNumber(tx.getFromAccount() != null ? tx.getFromAccount().getAccountNumber() : null);
        resp.setToAccountNumber(tx.getToAccount() != null ? tx.getToAccount().getAccountNumber() : null);
        resp.setCreatedAt(tx.getCreatedAt());
        resp.setApprovedAt(tx.getApprovedAt());
        resp.setApprovedBy(tx.getApprovedBy() != null ? tx.getApprovedBy().getUsername() : null);
        resp.setManagerComment(tx.getManagerComment());
        return resp;
    }

    // Transaction Creation (by User)

    @Transactional
    public TransactionResponse createDeposit(String username, TransactionRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Account account = user.getAccount();
        if (account == null) {
            throw new ResourceNotFoundException("User account not found");
        }

        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription());
        tx.setToAccount(account);
        tx.setStatus(ApprovalStatus.PENDING);

        Transaction saved = transactionRepository.save(tx);
        return mapToResponse(saved);
    }

    @Transactional
    public TransactionResponse createWithdrawal(String username, TransactionRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Account account = user.getAccount();
        if (account == null) {
            throw new ResourceNotFoundException("User account not found");
        }

        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.WITHDRAWAL);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription());
        tx.setFromAccount(account);
        tx.setStatus(ApprovalStatus.PENDING);

        Transaction saved = transactionRepository.save(tx);
        return mapToResponse(saved);
    }

    @Transactional
    public TransactionResponse createTransfer(String username, TransactionRequest request) {
        User fromUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found"));
        Account fromAccount = fromUser.getAccount();
        if (fromAccount == null) {
            throw new ResourceNotFoundException("Sender account not found");
        }

        Account toAccount = accountRepository.findByAccountNumber(request.getRecipientAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient account not found"));

        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription());
        tx.setFromAccount(fromAccount);
        tx.setToAccount(toAccount);
        tx.setStatus(ApprovalStatus.PENDING);

        Transaction saved = transactionRepository.save(tx);
        return mapToResponse(saved);
    }

    // Approval / Rejection (by Manager)

    @Transactional
    public TransactionResponse approveTransaction(Long transactionId, String managerComment) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getStatus() != ApprovalStatus.PENDING) {
            throw new UnauthorizedException("Only pending transactions can be approved");
        }

        // Perform balance checks and update accounts
        validateAndProcessTransaction(tx);

        User approver = getCurrentUser();
        tx.setStatus(ApprovalStatus.APPROVED);
        tx.setApprovedAt(LocalDateTime.now());
        tx.setApprovedBy(approver);
        tx.setManagerComment(managerComment);

        Transaction saved = transactionRepository.save(tx);
        return mapToResponse(saved);
    }

    @Transactional
    public TransactionResponse rejectTransaction(Long transactionId, String managerComment) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getStatus() != ApprovalStatus.PENDING) {
            throw new UnauthorizedException("Only pending transactions can be rejected");
        }

        User approver = getCurrentUser();
        tx.setStatus(ApprovalStatus.REJECTED);
        tx.setApprovedAt(LocalDateTime.now());
        tx.setApprovedBy(approver);
        tx.setManagerComment(managerComment);

        Transaction saved = transactionRepository.save(tx);
        return mapToResponse(saved);
    }

    private void validateAndProcessTransaction(Transaction tx) {
        switch (tx.getTransactionType()) {
            case DEPOSIT:
                Account toAcc = tx.getToAccount();
                toAcc.setBalance(toAcc.getBalance().add(tx.getAmount()));
                accountRepository.save(toAcc);
                break;
            case WITHDRAWAL:
                Account fromAcc = tx.getFromAccount();
                if (fromAcc.getBalance().compareTo(tx.getAmount()) < 0) {
                    throw new UnauthorizedException("Insufficient balance for withdrawal");
                }
                fromAcc.setBalance(fromAcc.getBalance().subtract(tx.getAmount()));
                accountRepository.save(fromAcc);
                break;
            case TRANSFER:
                Account source = tx.getFromAccount();
                Account target = tx.getToAccount();
                if (source.getBalance().compareTo(tx.getAmount()) < 0) {
                    throw new UnauthorizedException("Insufficient balance for transfer");
                }
                source.setBalance(source.getBalance().subtract(tx.getAmount()));
                target.setBalance(target.getBalance().add(tx.getAmount()));
                accountRepository.save(source);
                accountRepository.save(target);
                break;
            default:
                throw new RuntimeException("Invalid transaction type");
        }
    }

    // Queries

    public List<TransactionResponse> getPendingTransactions() {
        return transactionRepository.findAllPendingTransactions().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Transaction> txs = transactionRepository.findAllByUserId(user.getId());
        return txs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getPendingTransactionsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Account account = user.getAccount();
        if (account == null) return List.of();
        List<Transaction> txs = transactionRepository.findByAccountAndStatus(account, ApprovalStatus.PENDING);
        return txs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
}