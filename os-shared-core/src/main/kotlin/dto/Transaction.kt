package dev.jakubw.omnisentry.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Transaction(
    val id: UUID,                // System ID
    val externalId: String?,     // ID from Salt Edge
    val accountId: UUID,         // Account ID

    val amount: BigDecimal,
    val currency: String,

    val description: String,     // Raw bank description
    val category: String?,

    val madeOn: Instant,         // Transaction Date
    val status: TransactionStatus,

    val metadata: TransactionMetadata
)

data class TransactionMetadata(
    val merchantId: String? = null,
    val time: String? = null,
    val type: String? = null,
    val closingBalance: BigDecimal? = null,
    val city: String? = null
)

enum class TransactionStatus {
    POSTED, PENDING, REJECTED
}