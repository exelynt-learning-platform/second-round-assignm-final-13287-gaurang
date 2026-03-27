package com.shopflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Min(0)
    @Column(nullable = false)
    private Integer stockQty;

    @Column(length = 500)
    private String imageUrl;

    @Column(updatable = false)
    private LocalDateTime listedAt;

    private boolean active = true;

    @PrePersist
    private void setListingTime() {
        listedAt = LocalDateTime.now();
    }

    public boolean isInStock(int requiredQty) {
        return this.stockQty >= requiredQty;
    }

    public void decreaseStock(int qty) {
        if (!isInStock(qty)) {
            throw new IllegalStateException("Not enough stock for product: " + name);
        }
        this.stockQty -= qty;
    }
}
