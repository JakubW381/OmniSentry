package dev.jakubw.omnisentry.config

import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri
import org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.stripPrefix
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.servlet.function.*

@Configuration
open class GatewayRoutesConfig {

    @Bean
    open fun gatewayRouting(): RouterFunction<ServerResponse> {
        return route("dev.jakubw.omnisentry.main-backend")
            .route(path("/api/backend/**"), http())
            .filter(stripPrefix(2))
            .before(uri("http://os-backend"))
            .filter(userHeaderFilter())
            .build()
            .and(
                route("authenticator")
                    .route(path("/api/auth/**"), http())
                    .filter(stripPrefix(1))
                    .before(uri("http://os-authenticator"))
                    .build()
            )
            .and(
                route("ai-agent")
                    .route(path("/api/ai/**"), http())
                    .filter(stripPrefix(1))
                    .before(uri("http://os-agent"))
                    .filter(userHeaderFilter())
                    .build()
            )
    }

    private fun userHeaderFilter(): HandlerFilterFunction<ServerResponse, ServerResponse> {
        return HandlerFilterFunction { request, next ->
            val auth = SecurityContextHolder.getContext().authentication

            if (auth != null && auth.principal is Jwt) {
                val username = auth.name
                val modifiedRequest = ServerRequest.from(request)
                    .header("X-User-Username", username)
                    .build()
                next.handle(modifiedRequest)
            } else {
                next.handle(request)
            }
        }
    }
}