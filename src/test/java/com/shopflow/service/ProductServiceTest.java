package com.shopflow.service;

import com.shopflow.dto.request.ProductRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.Product;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;

    @InjectMocks
    ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .name("USB-C Hub")
                .description("7-in-1 USB-C hub with HDMI")
                .price(new BigDecimal("1299.00"))
                .stockQty(25)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("addProduct should persist and return a summary with correct fields")
    void addProduct_validRequest_returnsSummary() {
        ProductRequest req = new ProductRequest();
        req.setName("USB-C Hub");
        req.setDescription("7-in-1 USB-C hub with HDMI");
        req.setPrice(new BigDecimal("1299.00"));
        req.setStockQty(25);

        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        Responses.ProductSummary result = productService.addProduct(req);

        assertThat(result.getName()).isEqualTo("USB-C Hub");
        assertThat(result.getPrice()).isEqualByComparingTo("1299.00");
        assertThat(result.getStockQty()).isEqualTo(25);
    }

    @Test
    @DisplayName("fetchOrThrow should throw ResourceNotFoundException for inactive products")
    void fetchOrThrow_inactiveProduct_throwsNotFound() {
        sampleProduct.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.fetchOrThrow(1L))
                .isInstanceOf(ShopExceptions.ResourceNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    @DisplayName("fetchOrThrow should throw ResourceNotFoundException when product does not exist")
    void fetchOrThrow_missingProduct_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.fetchOrThrow(99L))
                .isInstanceOf(ShopExceptions.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("removeProduct should soft-delete by setting active=false")
    void removeProduct_existingProduct_setsActiveToFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        productService.removeProduct(1L);

        assertThat(sampleProduct.isActive()).isFalse();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("updateProduct should change only the provided fields")
    void updateProduct_validRequest_updatesFields() {
        ProductRequest updateReq = new ProductRequest();
        updateReq.setName("USB-C Hub Pro");
        updateReq.setDescription("Updated description");
        updateReq.setPrice(new BigDecimal("1499.00"));
        updateReq.setStockQty(30);

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Responses.ProductSummary result = productService.updateProduct(1L, updateReq);

        assertThat(result.getName()).isEqualTo("USB-C Hub Pro");
        assertThat(result.getPrice()).isEqualByComparingTo("1499.00");
        assertThat(result.getStockQty()).isEqualTo(30);
    }
}
