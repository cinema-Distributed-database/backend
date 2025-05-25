package com.cinema.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LookupBookingRequest {
    @NotEmpty(message = "Số điện thoại không được để trống")
    private String phone;

    // @NotEmpty(message = "Email không được để trống")
    private String email;
}