package com.cinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequestDto {
    @NotBlank(message = "Booking ID không được để trống")
    private String bookingId;

    private String returnUrl; // URL frontend để VNPay redirect về sau thanh toán
}