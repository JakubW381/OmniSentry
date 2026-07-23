package dev.jakubw.omnisentry.services.grpc;

import dev.jakubw.omnisentry.dto.CustomerDto;
import dev.jakubw.omnisentry.grpc.UserRegistrationRequest;
import dev.jakubw.omnisentry.grpc.UserRegistrationResponse;
import dev.jakubw.omnisentry.grpc.UserRegistrationServiceGrpc.UserRegistrationServiceImplBase;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.UserRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@GrpcService
@Service
@RequiredArgsConstructor
public class UserRegistrationGrpcService extends UserRegistrationServiceImplBase {

    private final UserRepository userRepository;
    private final SaltEdgeService saltEdgeService;

    @Override
    public void registerUser(UserRegistrationRequest request, StreamObserver<UserRegistrationResponse> responseObserver) {

        if (userRepository.existsByEmailOrUsername(request.getEmail(), request.getUsername())) {
            UserRegistrationResponse response = UserRegistrationResponse.newBuilder()
                    .setMessage("User with this Email or Username already exists")
                    .setResult(false)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        Optional<CustomerDto> customerDto = saltEdgeService.createCustomer(request.getEmail());

        if (customerDto.isEmpty()) {

            UserRegistrationResponse response = UserRegistrationResponse.newBuilder()
                    .setMessage("Failed to create customer account in SaltEdge")
                    .setResult(false)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        CustomerDto customer = customerDto.get();

        UserEntity userEntity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .name(request.getName())
                .surname(request.getSurname())
                .dateOfBirth(Instant.ofEpochSecond(
                        request.getDateOfBirth().getSeconds(),
                        request.getDateOfBirth().getNanos()
                ))
                .saltEdgeCustomerId(customer.customerId())
                .build();

        userRepository.save(userEntity);

        UserRegistrationResponse response = UserRegistrationResponse.newBuilder()
                .setMessage("User registered successfully")
                .setCustomerId(customer.customerId())
                .setResult(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}