package dev.jakubw.omnisentry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID


data class AccountDto(

    @JsonProperty("id")
    val saltEdgeAccountId: String,

    @JsonProperty("connection_id")
    val connectionId: String,

    val name: String,
    val balance: BigDecimal,
    val nature: String,

    @JsonProperty("currency_code")
    val currency: String,

    val extra: AccountExtraDto,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("updated_at")
    val updatedAt: String
)

data class AccountExtraDto(
    val iban: String? = null,
    val bban: String? = null,
    val status: String? = null,
    @JsonProperty("holder_name")
    val holderName: String? = null
)
