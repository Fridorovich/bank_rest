package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserId(Long userId);

    List<Card> findByUserIdAndStatus(Long userId, CardStatus status);

    Optional<Card> findByIdAndUserId(Long id, Long userId);

    List<Card> findByStatus(CardStatus status);

    boolean existsByNumber(String number);

    Optional<Card> findByNumber(String number);

    Page<Card> findByUserId(Long userId, Pageable pageable);
    Page<Card> findByUserIdAndStatus(Long userId, CardStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.number LIKE %:searchTerm%")
    Page<Card> findByUserIdAndNumberContaining(@Param("userId") Long userId,
                                               @Param("searchTerm") String searchTerm,
                                               Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.status = :status AND c.number LIKE %:searchTerm%")
    Page<Card> findByUserIdAndStatusAndNumberContaining(@Param("userId") Long userId,
                                                        @Param("status") CardStatus status,
                                                        @Param("searchTerm") String searchTerm,
                                                        Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.expiryDate < CURRENT_DATE AND c.status = 'ACTIVE'")
    List<Card> findExpiredActiveCards();
}
