package com.shopflow.controller;

import com.shopflow.dto.request.ProductRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<Responses.ProductSummary>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Responses.ProductSummary> products = productService.listAll(page, size);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Responses.ProductSummary> getProduct(@PathVariable Long id) {
        Responses.ProductSummary product = productService.getById(id);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Responses.ProductSummary>> search(@RequestParam String q) {
        List<Responses.ProductSummary> results = productService.search(q);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Responses.ProductSummary> createProduct(
            @Valid @RequestBody ProductRequest request) {
        Responses.ProductSummary product = productService.addProduct(request);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Responses.ProductSummary> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        Responses.ProductSummary product = productService.updateProduct(id, request);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Responses.ApiMessage> deleteProduct(@PathVariable Long id) {
        productService.removeProduct(id);
        Responses.ApiMessage msg = new Responses.ApiMessage("Product removed successfully");
        return new ResponseEntity<>(msg, HttpStatus.OK);
    }
}
