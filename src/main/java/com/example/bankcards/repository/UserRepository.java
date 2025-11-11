package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.username LIKE %:username% AND r.name = :roleName")
    Page<User> findByUsernameContainingAndRoleName(@Param("username") String username,
                                                   @Param("roleName") String roleName,
                                                   Pageable pageable);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.user.id = :userId")
    Long countCardsByUserId(@Param("userId") Long userId);
}
