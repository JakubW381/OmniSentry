package dev.jakubw.omnisentry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*

data class TransactionDto(
    val internalId: UUID? = null,

    @JsonProperty("id")
    val transasctionId: String,

    @JsonProperty("account_id")
    val accountId: String,

    val amount: BigDecimal,

    @JsonProperty("currency_code")
    val currency: String,

    val description: String,

    val category: String? = "uncategorized",

    @JsonProperty("made_on")
    val madeOn: String,

    val status: String,

    val extra: Map<String, Any>? = emptyMap()
)