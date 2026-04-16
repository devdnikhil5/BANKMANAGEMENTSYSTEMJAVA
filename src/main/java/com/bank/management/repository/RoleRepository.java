package com.bank.management.repository;

import com.bank.management.model.Role;
import com.bank.management.model.Role.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    
    Optional<Role> findByName(ERole name);
    
    boolean existsByName(ERole name);
}