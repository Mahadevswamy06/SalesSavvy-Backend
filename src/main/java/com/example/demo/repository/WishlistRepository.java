package com.example.demo.repository;

import com.example.demo.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Integer> {
    List<WishlistItem> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
    Optional<WishlistItem> findByUser_UserIdAndProduct_ProductId(Integer userId, Integer productId);
    void deleteByUser_UserIdAndProduct_ProductId(Integer userId, Integer productId);
    void deleteByUser_UserId(Integer userId);
}
