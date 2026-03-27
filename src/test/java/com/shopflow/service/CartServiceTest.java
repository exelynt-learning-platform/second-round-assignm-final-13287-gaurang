package com.shopflow.service;

import com.shopflow.dto.request.CartItemRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.*;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock UserRepository userRepository;
    @Mock ProductService productService;

    @InjectMocks
    CartService cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void buildFixtures() {
        testUser = User.builder()
                .id(1L).email("test@shopflow.com")
                .fullName("Test User")
                .role(User.Role.CUSTOMER)
                .build();

        testProduct = Product.builder()
                .id(10L).name("Wireless Mouse")
                .price(new BigDecimal("799.00"))
                .stockQty(50)
                .active(true)
                .build();

        testCart = Cart.builder()
                .id(1L)
                .owner(testUser)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Adding a new product to cart creates a CartItem")
    void addItem_newProduct_createsCartItem() {
        CartItemRequest req = new CartItemRequest();
        req.setProductId(testProduct.getId());
        req.setQuantity(2);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByOwner(testUser)).thenReturn(Optional.of(testCart));
        when(productService.fetchOrThrow(testProduct.getId())).thenReturn(testProduct);
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.empty());
        when(cartRepository.save(testCart)).thenReturn(testCart);

        Responses.CartView result = cartService.addItem(testUser.getEmail(), req);

        assertThat(result).isNotNull();
        assertThat(result.getCartId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Adding the same product again merges quantities")
    void addItem_existingProduct_incrementsQuantity() {
        CartItem existing = CartItem.builder()
                .id(5L).cart(testCart).product(testProduct).quantity(1).build();

        CartItemRequest req = new CartItemRequest();
        req.setProductId(testProduct.getId());
        req.setQuantity(3);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByOwner(testUser)).thenReturn(Optional.of(testCart));
        when(productService.fetchOrThrow(testProduct.getId())).thenReturn(testProduct);
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(existing));
        when(cartRepository.save(testCart)).thenReturn(testCart);

        cartService.addItem(testUser.getEmail(), req);

        // quantity should be bumped from 1 → 4
        assertThat(existing.getQuantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("Adding more than available stock throws OutOfStockException")
    void addItem_insufficientStock_throwsOutOfStock() {
        testProduct.setStockQty(1); // only 1 in stock

        CartItemRequest req = new CartItemRequest();
        req.setProductId(testProduct.getId());
        req.setQuantity(5);    // requesting more than available

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByOwner(testUser)).thenReturn(Optional.of(testCart));
        when(productService.fetchOrThrow(testProduct.getId())).thenReturn(testProduct);

        assertThatThrownBy(() -> cartService.addItem(testUser.getEmail(), req))
                .isInstanceOf(ShopExceptions.OutOfStockException.class)
                .hasMessageContaining("Wireless Mouse");
    }

    @Test
    @DisplayName("Removing an item that belongs to a different user's cart throws AccessDeniedException")
    void removeItem_wrongUser_throwsForbidden() {
        Cart anotherCart = Cart.builder().id(99L).owner(testUser).items(new ArrayList<>()).build();
        CartItem foreignItem = CartItem.builder().id(77L).cart(anotherCart).product(testProduct).quantity(1).build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByOwner(testUser)).thenReturn(Optional.of(testCart)); // user's cart id=1
        when(cartItemRepository.findById(77L)).thenReturn(Optional.of(foreignItem)); // item belongs to cart id=99

        assertThatThrownBy(() -> cartService.removeItem(testUser.getEmail(), 77L))
                .isInstanceOf(ShopExceptions.AccessDeniedException.class);
    }
}
