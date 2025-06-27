package com.tbros.darajaapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tbros.darajaapi.dtos.*;
import com.tbros.darajaapi.repository.StkPushEntriesRepository;
import com.tbros.darajaapi.services.DarajaApiService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mobile-money")
@Slf4j
@RequiredArgsConstructor
public class MpesaController {

    private final DarajaApiService darajaApiService;
    private final AcknowledgeResponse acknowledgeResponse;
    private final ObjectMapper objectMapper;
    private final StkPushEntriesRepository stkPushEntriesRepository;

    @GetMapping(path = "/token", produces = "application/json")
    public ResponseEntity<AccessTokenResponse> getAccessToken() {
        return ResponseEntity.ok(darajaApiService.getAccessToken());
    }

    @GetMapping(path = "/register-url", produces = "application/json")
    public ResponseEntity<RegisterUrlResponse> registerUrl() {
        return ResponseEntity.ok(darajaApiService.registerUrl());
    }

    @PostMapping(path = "/validation", produces = "application/json")
    public ResponseEntity<AcknowledgeResponse> mpesaValidation(@RequestBody MpesaValidationResponse mpesaValidationResponse) {
        AcknowledgeResponse response = darajaApiService.handleMpesaValidation(mpesaValidationResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/simulate-c2b", produces = "application/json")
    public ResponseEntity<SimulateTransactionResponse> simulateC2BTransaction(@RequestBody SimulateTransactionRequest simulateTransactionRequest) {
        SimulateTransactionResponse simulateTransactionResponse = darajaApiService.simulateC2BAndPersist(simulateTransactionRequest);

        if (simulateTransactionResponse == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(simulateTransactionResponse);
    }

    @PostMapping(path = "/transaction-result", produces = "application/json")
    public ResponseEntity<AcknowledgeResponse> b2cTransactionAsyncResults(@RequestBody B2CTransactionAsyncResponse b2CTransactionAsyncResponse) {
        AcknowledgeResponse response = darajaApiService.handleB2CTransactionAsyncResults(b2CTransactionAsyncResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/b2c-queue-timeout", produces = "application/json")
    public ResponseEntity<AcknowledgeResponse> queueTimeout(@RequestBody Object object) {
        return ResponseEntity.ok(acknowledgeResponse);
    }

    @PostMapping(path = "/b2c-transaction", produces = "application/json")
    public ResponseEntity<CommonSyncResponse> performB2CTransaction(@RequestBody InternalB2CTransactionRequest internalB2CTransactionRequest) {
        CommonSyncResponse commonSyncResponse = darajaApiService.performB2CTransactionAndPersist(internalB2CTransactionRequest);

        if (commonSyncResponse == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(commonSyncResponse);
    }

    @PostMapping(path = "/simulate-transaction-result", produces = "application/json")
    public ResponseEntity<TransactionStatusSyncResponse> getTransactionStatusResult(@RequestBody InternalTransactionStatusRequest internalTransactionStatusRequest) {
        return ResponseEntity.ok(darajaApiService.getTransactionResult(internalTransactionStatusRequest));
    }

    @GetMapping(path = "/check-account-balance", produces = "application/json")
    public ResponseEntity<CommonSyncResponse> checkAccountBalance() {
        return ResponseEntity.ok(darajaApiService.checkAccountBalance());
    }

    @PostMapping(path = "/stk-transaction-request", produces = "application/json")
    public ResponseEntity<StkPushSyncResponse> performStkPushTransaction(@RequestBody InternalStkPushRequest internalStkPushRequest) {
        return ResponseEntity.ok(darajaApiService.performStkPushTransaction(internalStkPushRequest));
    }

    @SneakyThrows
    @PostMapping(path = "/stk-transaction-result", produces = "application/json")
    public ResponseEntity<AcknowledgeResponse> acknowledgeStkPushResponse(@RequestBody StkPushAsyncResponse stkPushAsyncResponse) {
        return ResponseEntity.ok(acknowledgeResponse);
    }

    @PostMapping(path = "/query-lnm-request", produces = "application/json")
    public ResponseEntity<LNMQueryResponse> getTransactionStatus(@RequestBody InternalLNMRequest internalLNMRequest) {
        return ResponseEntity.ok(darajaApiService.getTransactionStatus(internalLNMRequest));
    }
}
