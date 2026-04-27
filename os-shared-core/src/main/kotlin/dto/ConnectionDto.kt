package dev.jakubw.omnisentry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class ConnectionDto(
    val internalId: UUID?,

    @JsonProperty("id")
    val connectionId: String,

    @JsonProperty("customer_id")
    val customerId: String,

    @JsonProperty("provider_name")
    val providerName: String,

    @JsonProperty("provider_code")
    val providerCode: String,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("last_attempt")
    val lastAttempt: LastAttemptDto,

    val status: String
)

data class LastAttemptDto(
    @JsonProperty("device_type")
    val deviceType: String?,
    @JsonProperty("remote_ip")
    val remoteIp: String?
)