package dev.jakubw.omnisentry.util

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.semconv.ServiceAttributes

fun getOpenTelemetry(serviceName: String): OpenTelemetry {

    return AutoConfiguredOpenTelemetrySdk.builder().addResourceCustomizer { resource,_ ->
        resource.toBuilder()
            .putAll(resource.attributes)
            .put(ServiceAttributes.SERVICE_NAME,serviceName)
            .build()
    }.build().openTelemetrySdk
}