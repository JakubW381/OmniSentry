package dev.jakubw.omnisentry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*

data class TransactionDto(

    @JsonProperty("id")
    val transactionId: String,

    @JsonProperty("account_id")
    val accountId: String,

    @JsonProperty("amount")
    val amount: BigDecimal,

    @JsonProperty("currency_code")
    val currency: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("category")
    val category: String? = "uncategorized",

    @JsonProperty("made_on")
    val madeOn: String,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("extra")
    val extra: Map<String, Any>? = emptyMap()
)