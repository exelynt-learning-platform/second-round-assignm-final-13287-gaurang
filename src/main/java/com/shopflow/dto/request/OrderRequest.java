package com.shopflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "\\d{6}", message = "Enter a valid 6-digit pincode")
    private String pincode;
}
