package dev.jakubw.omnisentry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerDto(
        @JsonProperty("customer_id")
        String customerId,
        String identifier) {}
