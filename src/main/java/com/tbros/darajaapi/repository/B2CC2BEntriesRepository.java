package com.tbros.darajaapi.repository;

import com.tbros.documents.B2C_C2B_Entries;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface B2CC2BEntriesRepository extends MongoRepository<B2C_C2B_Entries, String> {

    // Find Record By ConversationID or OriginatorConversationID ...
    Optional<B2C_C2B_Entries> findByConversationIdOrOriginatorConversationId(String conversationId, String originatorConversationId);

    // Find Transaction By TransactionId ....
    B2C_C2B_Entries findByTransactionId(String transactionId);

    Optional<B2C_C2B_Entries> findByBillRefNumber(String billRefNumber);

}
