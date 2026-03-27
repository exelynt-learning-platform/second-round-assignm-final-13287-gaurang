package com.shopflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class Register {
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 80)
        private String fullName;

        @Email(message = "Enter a valid email")
        @NotBlank
        private String email;

        @NotBlank
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        private String password;
    }

    @Data
    public static class Login {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;
    }
}
