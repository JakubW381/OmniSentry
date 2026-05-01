package dev.jakubw.omnisentry.config


import dev.jakubw.omnisentry.grpc.UserRegistrationServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory

@Configuration
open class GrpcClientConfig {
    @Bean
    open fun userRegistrationStub(factory: GrpcChannelFactory): UserRegistrationServiceGrpc.UserRegistrationServiceBlockingStub {
        return UserRegistrationServiceGrpc.newBlockingStub(factory.createChannel("user-service"))
    }
}