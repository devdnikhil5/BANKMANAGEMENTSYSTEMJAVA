package com.bank.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bank.management.model.Account;
import com.bank.management.model.ApprovalStatus;
import com.bank.management.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByStatus(ApprovalStatus status);

    List<Transaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(Account fromAccount, Account toAccount);

    List<Transaction> findByFromAccountOrderByCreatedAtDesc(Account account);

    List<Transaction> findByToAccountOrderByCreatedAtDesc(Account account);

    @Query("SELECT t FROM Transaction t WHERE (t.fromAccount = :account OR t.toAccount = :account) AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountAndStatus(@Param("account") Account account, @Param("status") ApprovalStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' ORDER BY t.createdAt DESC")
    List<Transaction> findAllPendingTransactions();

    @Query("SELECT t FROM Transaction t " +
    "LEFT JOIN t.fromAccount fa " +
    "LEFT JOIN t.toAccount ta " +
    "WHERE fa.user.id = :userId OR ta.user.id = :userId " +
    "ORDER BY t.createdAt DESC")
    List<Transaction> findAllByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM transactions WHERE from_account_id IN (SELECT id FROM accounts WHERE user_id = :userId) OR to_account_id IN (SELECT id FROM accounts WHERE user_id = :userId)", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE transactions SET approved_by = NULL WHERE approved_by = :userId", nativeQuery = true)
    void clearApprovedByReferences(@Param("userId") Long userId);
}