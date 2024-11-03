package com.trade_ham.domain.product.domain;

import com.trade_ham.domain.auth.entity.UserEntity;
import com.trade_ham.domain.locker.domain.LockerEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tradeId;
    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private UserEntity buyer;
    @ManyToOne
    @JoinColumn(name = "seller_id")
    private UserEntity seller;
    @ManyToOne
    @JoinColumn(name = "locker_id")
    private LockerEntity lockerEntity;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}