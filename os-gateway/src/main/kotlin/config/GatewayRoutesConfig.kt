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
import java.util.function.Function

@Configuration
class GatewayRoutesConfig {

    @Bean
    fun gatewayRouting(): RouterFunction<ServerResponse> {
        return route("main-backend")
            .route(path("/api/backend/**"), http())
            .before(stripPrefix(2) as Function<ServerRequest, ServerRequest>)
            .before(uri("http://user-service")) // Adres ustawiony jako filtr before
            .filter(userHeaderFilter())
            .build()
            .and(
                route("authenticator")
                    .route(path("/api/auth/**"), http())
                    .before(stripPrefix(1) as Function<ServerRequest, ServerRequest>)
                    .before(uri("http://authenticator"))
                    .build()
            )
            .and(
                route("ai-agent")
                    .route(path("/api/ai/**"), http())
                    .before(stripPrefix(1) as Function<ServerRequest, ServerRequest>)
                    .before(uri("http://ai-agent"))
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