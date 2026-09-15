package com.example.e_wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long sourceAccountId,
        @NotNull Long destinationAccountId,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount
) {}