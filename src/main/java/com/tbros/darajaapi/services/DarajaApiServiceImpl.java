package com.tbros.darajaapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tbros.darajaapi.config.MpesaConfiguration;
import com.tbros.darajaapi.dtos.*;
import com.tbros.darajaapi.repository.B2CC2BEntriesRepository;
import com.tbros.darajaapi.utils.Constants;
import com.tbros.darajaapi.utils.HelperUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tbros.documents.B2C_C2B_Entries;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static com.tbros.darajaapi.utils.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DarajaApiServiceImpl implements DarajaApiService {

    private final MpesaConfiguration mpesaConfiguration;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final B2CC2BEntriesRepository b2CC2BEntriesRepository;
    private final AcknowledgeResponse acknowledgeResponse;

    /**
     * @return Returns Daraja API Access Token Response
     */
    @Override
    public AccessTokenResponse getAccessToken() {

        String encodedCredentials = HelperUtility.toBase64String(String.format("%s:%s", mpesaConfiguration.getConsumerKey(),
                mpesaConfiguration.getConsumerSecret()));

        Request request = new Request.Builder()
                .url(String.format("%s?grant_type=%s", mpesaConfiguration.getOauthEndpoint(), mpesaConfiguration.getGrantType()))
                .get()
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BASIC_AUTH_STRING, encodedCredentials))
                .addHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_HEADER_VALUE)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            assert response.body() != null;

            String responseBodyString = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBodyString, AccessTokenResponse.class);
        } catch (IOException e) {
            return null;
        }

    }

    @Override
    public RegisterUrlResponse registerUrl() {
        AccessTokenResponse accessTokenResponse = getAccessToken();

        RegisterUrlRequest registerUrlRequest = new RegisterUrlRequest();
        registerUrlRequest.setConfirmationURL(mpesaConfiguration.getConfirmationURL());
        registerUrlRequest.setResponseType(mpesaConfiguration.getResponseType());
        registerUrlRequest.setShortCode(mpesaConfiguration.getShortCode());
        registerUrlRequest.setValidationURL(mpesaConfiguration.getValidationURL());


        RequestBody body = RequestBody.create(Objects.requireNonNull(HelperUtility.toJson(registerUrlRequest)),
                JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(mpesaConfiguration.getRegisterUrlEndpoint())
                .post(body)
                .addHeader("Authorization", String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            assert response.body() != null;

            String responseBodyString = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBodyString, RegisterUrlResponse.class);
        } catch (IOException e) {
            return null;
        }

    }

    @Override
    public AcknowledgeResponse handleMpesaValidation(MpesaValidationResponse mpesaValidationResponse) {
        Optional<B2C_C2B_Entries> optionalB2CC2BEntry = b2CC2BEntriesRepository.findByBillRefNumber(mpesaValidationResponse.getBillRefNumber());

        if (optionalB2CC2BEntry.isPresent()) {
            B2C_C2B_Entries b2CC2BEntry = optionalB2CC2BEntry.get();
            b2CC2BEntry.setRawCallbackPayloadResponse(mpesaValidationResponse);
            b2CC2BEntry.setResultCode("0");
            b2CC2BEntry.setTransactionId(mpesaValidationResponse.getTransID());

            b2CC2BEntriesRepository.save(b2CC2BEntry);
        } else {
            log.error("B2C_C2B_Entry not found for BillRefNumber: {}", mpesaValidationResponse.getBillRefNumber());
        }
        return acknowledgeResponse;
    }

    @Override
    public AcknowledgeResponse handleB2CTransactionAsyncResults(B2CTransactionAsyncResponse b2CTransactionAsyncResponse) {
        try {
            log.info(objectMapper.writeValueAsString(b2CTransactionAsyncResponse));
        } catch (JsonProcessingException e) {
            log.error("Error logging B2CTransactionAsyncResponse: {}", e.getMessage());
        }

        Result b2cResult = b2CTransactionAsyncResponse.getResult();

        Optional<B2C_C2B_Entries> optionalB2cInternalRecord = b2CC2BEntriesRepository.findByConversationIdOrOriginatorConversationId(
                b2cResult.getConversationID(),
                b2cResult.getOriginatorConversationID());

        if (optionalB2cInternalRecord.isPresent()) {
            B2C_C2B_Entries b2cInternalRecord = optionalB2cInternalRecord.get();
            b2cInternalRecord.setRawCallbackPayloadResponse(b2CTransactionAsyncResponse);
            b2cInternalRecord.setResultCode(String.valueOf(b2cResult.getResultCode()));
            b2cInternalRecord.setTransactionId(b2cResult.getTransactionID());

            b2CC2BEntriesRepository.save(b2cInternalRecord);
        } else {
            log.error("B2C_C2B_Entry not found for ConversationID: {} or OriginatorConversationID: {}", b2cResult.getConversationID(), b2cResult.getOriginatorConversationID());
        }
        return acknowledgeResponse;
    }

    @Override
    public SimulateTransactionResponse simulateC2BTransaction(SimulateTransactionRequest simulateTransactionRequest) {
        AccessTokenResponse accessTokenResponse = getAccessToken();
        log.info("Access Token: {}", accessTokenResponse.getAccessToken());
        RequestBody body = RequestBody.create(Objects.requireNonNull(HelperUtility.toJson(simulateTransactionRequest)),
                JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(mpesaConfiguration.getSimulateTransactionEndpoint())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            assert response.body() != null;

            String responseBodyString = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBodyString, SimulateTransactionResponse.class);
        } catch (IOException e) {
            return null;
        }

    }

    public SimulateTransactionResponse simulateC2BAndPersist(SimulateTransactionRequest simulateTransactionRequest) {
        SimulateTransactionResponse simulateTransactionResponse = simulateC2BTransaction(simulateTransactionRequest);

        if (simulateTransactionResponse != null) {
            B2C_C2B_Entries b2C_c2BEntry = new B2C_C2B_Entries();
            b2C_c2BEntry.setTransactionType("C2B");
            b2C_c2BEntry.setBillRefNumber(simulateTransactionRequest.getBillRefNumber());
            b2C_c2BEntry.setAmount(simulateTransactionRequest.getAmount());
            b2C_c2BEntry.setEntryDate(new Date());
            b2C_c2BEntry.setOriginatorConversationId(simulateTransactionResponse.getOriginatorCoversationID());
            b2C_c2BEntry.setConversationId(simulateTransactionResponse.getConversationID());
            b2C_c2BEntry.setMsisdn(simulateTransactionRequest.getMsisdn());

            b2CC2BEntriesRepository.save(b2C_c2BEntry);
        }
        return simulateTransactionResponse;
    }

    @Override
    public CommonSyncResponse performB2CTransaction(InternalB2CTransactionRequest internalB2CTransactionRequest) {
        AccessTokenResponse accessTokenResponse = getAccessToken();

        B2CTransactionRequest b2CTransactionRequest = new B2CTransactionRequest();

        b2CTransactionRequest.setCommandID(internalB2CTransactionRequest.getCommandID());
        b2CTransactionRequest.setAmount(internalB2CTransactionRequest.getAmount());
        b2CTransactionRequest.setPartyB(internalB2CTransactionRequest.getPartyB());
        b2CTransactionRequest.setRemarks(internalB2CTransactionRequest.getRemarks());
        b2CTransactionRequest.setOccassion(internalB2CTransactionRequest.getOccassion());

        b2CTransactionRequest.setSecurityCredential(HelperUtility.getSecurityCredentials(mpesaConfiguration.getB2cInitiatorPassword()));

        b2CTransactionRequest.setResultURL(mpesaConfiguration.getB2cResultUrl());
        b2CTransactionRequest.setQueueTimeOutURL(mpesaConfiguration.getB2cQueueTimeoutUrl());
        b2CTransactionRequest.setInitiatorName(mpesaConfiguration.getB2cInitiatorName());
        b2CTransactionRequest.setPartyA(mpesaConfiguration.getShortCode());

        RequestBody body = RequestBody.create(Objects.requireNonNull(HelperUtility.toJson(b2CTransactionRequest)),
                JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(mpesaConfiguration.getB2cTransactionEndpoint())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            assert response.body() != null;

            String responseBodyString = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBodyString, CommonSyncResponse.class);
        } catch (IOException e) {
            return null;
        }

    }

    public CommonSyncResponse performB2CTransactionAndPersist(InternalB2CTransactionRequest internalB2CTransactionRequest) {
        CommonSyncResponse commonSyncResponse = performB2CTransaction(internalB2CTransactionRequest);

        if (commonSyncResponse != null) {
            B2C_C2B_Entries b2C_c2BEntry = new B2C_C2B_Entries();
            b2C_c2BEntry.setTransactionType("B2C");
            b2C_c2BEntry.setAmount(internalB2CTransactionRequest.getAmount());
            b2C_c2BEntry.setEntryDate(new Date());
            b2C_c2BEntry.setOriginatorConversationId(commonSyncResponse.getOriginatorConversationID());
            b2C_c2BEntry.setConversationId(commonSyncResponse.getConversationID());
            b2C_c2BEntry.setMsisdn(internalB2CTransactionRequest.getPartyB());

            b2CC2BEntriesRepository.save(b2C_c2BEntry);
        }
        return commonSyncResponse;
    }

    @Override
    public TransactionStatusSyncResponse getTransactionResult(InternalTransactionStatusRequest internalTransactionStatusRequest) {

        TransactionStatusRequest transactionStatusRequest = new TransactionStatusRequest();
        transactionStatusRequest.setTransactionID(internalTransactionStatusRequest.getTransactionID());

        transactionStatusRequest.setInitiator(mpesaConfiguration.getB2cInitiatorName());
        transactionStatusRequest.setSecurityCredential(HelperUtility.getSecurityCredentials(mpesaConfiguration.getB2cInitiatorPassword()));
        transactionStatusRequest.setCommandID(TRANSACTION_STATUS_QUERY_COMMAND);
        transactionStatusRequest.setPartyA(mpesaConfiguration.getShortCode());
        transactionStatusRequest.setIdentifierType(SHORT_CODE_IDENTIFIER);
        transactionStatusRequest.setResultURL(mpesaConfiguration.getB2cResultUrl());
        transactionStatusRequest.setQueueTimeOutURL(mpesaConfiguration.getB2cQueueTimeoutUrl());
        transactionStatusRequest.setRemarks(TRANSACTION_STATUS_VALUE);
        transactionStatusRequest.setOccasion(TRANSACTION_STATUS_VALUE);

        AccessTokenResponse accessTokenResponse = getAccessToken();

        RequestBody body = RequestBody.create(Objects.requireNonNull(HelperUtility.toJson(transactionStatusRequest)),
                JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(mpesaConfiguration.getTransactionResultUrl())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            assert response.body() != null;

            String responseBodyString = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBodyString, TransactionStatusSyncResponse.class);
        } catch (IOException e) {
            return null;
        }

    }

    @Override
    public CommonSyncResponse checkAccountBalance() {

        CheckAccountBalanceRequest checkAccountBalanceRequest = new CheckAccountBalanceRequest();
        checkAccountBalanceRequest.setInitiator(mpesaConfiguration.getB2cInitiatorName());
        checkAccountBalanceRequest.setSecurityCredential(HelperUtility.getSecurityCredentials(mpesaConfiguration.getB2cInitiatorPassword()));
        checkAccountBalanceRequest.setCommandID(Constants.ACCOUNT_BALANCE_COMMAND);
        checkAccountBalanceRequest.setPartyA(mpesaConfiguration.getShortCode());
        checkAccountBalanceRequest.setIdentifierType(Constants.SHORT_CODE_IDENTIFIER);
        checkAccountBalanceRequest.setRemarks("Check Account Balance.");
        checkAccountBalanceRequest.setQueueTimeOutURL(mpesaConfiguration.getB2cQueueTimeoutUrl());
        checkAccountBalanceRequest.setResultURL(mpesaConfiguration.getB2cResultUrl());

        AccessTokenResponse accessTokenResponse = getAccessToken();

        RequestBody body = RequestBody.create(Objects.requireNonNull(HelperUtility.toJson(checkAccountBalanceRequest)),
                JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(mpesaConfiguration.getCheckAccountBalanceUrl())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            assert response.body() != null;

            String responseBodyString = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBodyString, CommonSyncResponse.class);
        } catch (IOException e) {
            return null;
        }

    }

    @Override
    public StkPushSyncResponse performStkPushTransaction(InternalStkPushRequest internalStkPushRequest) {

        ExternalStkPushRequest externalStkPushRequest = new ExternalStkPushRequest();
        externalStkPushRequest.setBusinessShortCode(mpesaConfiguration.getStkPushShortCode());

        String transactionTimestamp = HelperUtility.getTransactionTimestamp();
        String stkPushPassword = HelperUtility.getStkPushPassword(mpesaConfiguration.getStkPushShortCode(),
                mpesaConfiguration.getStkPassKey(), transactionTimestamp);

        externalStkPushRequest.setPassword(stkPushPassword);
        externalStkPushRequest.setTimestamp(transactionTimestamp);
        externalStkPushRequest.setTransactionType(Constants.CUSTOMER_PAYBILL_ONLINE);
        externalStkPushRequest.setAmount(internalStkPushRequest.getAmount());
        externalStkPushRequest.setPartyA(internalStkPushRequest.getPhoneNumber());
        externalStkPushRequest.setPartyB(mpesaConfiguration.getStkPushShortCode());
        externalStkPushRequest.setPhoneNumber(internalStkPushRequest.getPhoneNumber());
        externalStkPushRequest.setCallBackURL(mpesaConfiguration.getStkPushRequestCallbackUrl());
        externalStkPushRequest.setAccountReference(HelperUtility.getTransactionUniqueNumber());
        externalStkPushRequest.setTransactionDesc(String.format("%s Transaction", internalStkPushRequest.getPhoneNumber()));

        AccessTokenResponse accessTokenResponse = getAccessToken();

        RequestBody body = RequestBody.create(Objects.requireNonNull(HelperUtility.toJson(externalStkPushRequest)),
                JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(mpesaConfiguration.getStkPushRequestUrl())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            assert response.body() != null;

            String responseBodyString = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBodyString, StkPushSyncResponse.class);
        } catch (IOException e) {
            return null;
        }

    }

    @Override
    public LNMQueryResponse getTransactionStatus(InternalLNMRequest internalLNMRequest) {

        ExternalLNMQueryRequest externalLNMQueryRequest = new ExternalLNMQueryRequest();
        externalLNMQueryRequest.setBusinessShortCode(mpesaConfiguration.getStkPushShortCode());

        String requestTimestamp = HelperUtility.getTransactionTimestamp();
        String stkPushPassword = HelperUtility.getStkPushPassword(mpesaConfiguration.getStkPushShortCode(),
                mpesaConfiguration.getStkPassKey(), requestTimestamp);

        externalLNMQueryRequest.setPassword(stkPushPassword);
        externalLNMQueryRequest.setTimestamp(requestTimestamp);
        externalLNMQueryRequest.setCheckoutRequestID(internalLNMRequest.getCheckoutRequestID());

        AccessTokenResponse accessTokenResponse = getAccessToken();

        RequestBody body = RequestBody.create( Objects.requireNonNull(HelperUtility.toJson(externalLNMQueryRequest)),
                JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(mpesaConfiguration.getLnmQueryRequestUrl())
                .post(body)
                .addHeader(AUTHORIZATION_HEADER_STRING, String.format("%s %s", BEARER_AUTH_STRING, accessTokenResponse.getAccessToken()))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            assert response.body() != null;
            // Use Jackson to Deserialize the ResponseBody ...
            String responseBodyString = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBodyString, LNMQueryResponse.class);
        } catch (IOException e) {
            return null;
        }

    }
}
