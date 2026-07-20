package dev.jakubw.omnisentry
package util

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.semconv.ServiceAttributes

object OpenTelemetryInit {

  def getOpenTelemetry(serviceName: String): OpenTelemetry = {
    AutoConfiguredOpenTelemetrySdk.builder()
      .addResourceCustomizer { (oldResource, _) =>
        oldResource.toBuilder
          .put(ServiceAttributes.SERVICE_NAME, serviceName)
          .build()
      }
      .build()
      .getOpenTelemetrySdk
  }

}
