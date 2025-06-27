package com.tbros.darajaapi.services;

import com.tbros.darajaapi.dtos.*;

public interface DarajaApiService {

    /**
     * @return Returns Daraja API Access Token Response
     */
    AccessTokenResponse getAccessToken();

    RegisterUrlResponse registerUrl();

    AcknowledgeResponse handleMpesaValidation(MpesaValidationResponse mpesaValidationResponse);

    // New method to handle B2C Transaction Async Results and persistence
    AcknowledgeResponse handleB2CTransactionAsyncResults(B2CTransactionAsyncResponse b2CTransactionAsyncResponse);

    SimulateTransactionResponse simulateC2BTransaction(SimulateTransactionRequest simulateTransactionRequest);

    SimulateTransactionResponse simulateC2BAndPersist(SimulateTransactionRequest simulateTransactionRequest);

    CommonSyncResponse performB2CTransaction(InternalB2CTransactionRequest internalB2CTransactionRequest);

    CommonSyncResponse performB2CTransactionAndPersist(InternalB2CTransactionRequest internalB2CTransactionRequest);

    TransactionStatusSyncResponse getTransactionResult(InternalTransactionStatusRequest internalTransactionStatusRequest);

    CommonSyncResponse checkAccountBalance();

    StkPushSyncResponse performStkPushTransaction(InternalStkPushRequest internalStkPushRequest);

    LNMQueryResponse getTransactionStatus(InternalLNMRequest internalLNMRequest);
}
