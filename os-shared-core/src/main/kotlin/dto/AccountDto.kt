package dev.jakubw.omnisentry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID


data class AccountDto(

    @JsonProperty("id")
    val saltEdgeAccountId: String = "",

    @JsonProperty("connection_id")
    val connectionId: String = "",

    @JsonProperty("name")
    val name: String = "",
    @JsonProperty("balance")
    val balance: BigDecimal = BigDecimal.ZERO,
    @JsonProperty("nature")
    val nature: String = "",

    @JsonProperty("currency_code")
    val currency: String = "",

    @JsonProperty("extra")
    val extra: AccountExtraDto = AccountExtraDto(),

    @JsonProperty("created_at")
    val createdAt: String = "",

    @JsonProperty("updated_at")
    val updatedAt: String = ""
)

data class AccountExtraDto(
    @JsonProperty("iban")
    val iban: String? = "",
    @JsonProperty("bban")
    val bban: String? = "",
    @JsonProperty("status")
    val status: String? = "",
    @JsonProperty("holder_name")
    val holderName: String? = ""
)
