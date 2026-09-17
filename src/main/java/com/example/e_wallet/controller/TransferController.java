package com.example.e_wallet.controller;

import com.example.e_wallet.dto.TransferRequest;
import com.example.e_wallet.service.TransferService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PreAuthorize("@accountSecurity.canWrite(#request.sourceAccountId(), authentication)")
    @PostMapping
    public void transfer(@Valid @RequestBody TransferRequest request) {
        transferService.transfer(request.sourceAccountId(), request.destinationAccountId(), request.amount());
    }
}