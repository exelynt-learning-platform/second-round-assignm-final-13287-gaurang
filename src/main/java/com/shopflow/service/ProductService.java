package com.shopflow.service;

import com.shopflow.dto.request.ProductRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.Product;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Responses.ProductSummary addProduct(ProductRequest req) {
        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stockQty(req.getStockQty())
                .imageUrl(req.getImageUrl())
                .active(true)
                .build();

        return toSummary(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public Page<Responses.ProductSummary> listAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("listedAt").descending());
        return productRepository.findByActiveTrue(pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Responses.ProductSummary getById(Long productId) {
        return toSummary(fetchOrThrow(productId));
    }

    @Transactional(readOnly = true)
    public List<Responses.ProductSummary> search(String keyword) {
        List<Product> products = productRepository.searchByKeyword(keyword);
        List<Responses.ProductSummary> result = new ArrayList<>();
        for (Product product : products) {
            result.add(toSummary(product));
        }
        return result;
    }

    @Transactional
    public Responses.ProductSummary updateProduct(Long productId, ProductRequest req) {
        Product existing = fetchOrThrow(productId);
        existing.setName(req.getName());
        existing.setDescription(req.getDescription());
        existing.setPrice(req.getPrice());
        existing.setStockQty(req.getStockQty());
        existing.setImageUrl(req.getImageUrl());
        return toSummary(productRepository.save(existing));
    }

    @Transactional
    public void removeProduct(Long productId) {
        Product existing = fetchOrThrow(productId);
        existing.setActive(false);
        productRepository.save(existing);
    }

    public Product fetchOrThrow(Long productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException(
                        "Product not found with id: " + productId));
    }

    private Responses.ProductSummary toSummary(Product product) {
        return Responses.ProductSummary.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQty(product.getStockQty())
                .imageUrl(product.getImageUrl())
                .listedAt(product.getListedAt())
                .build();
    }
}
