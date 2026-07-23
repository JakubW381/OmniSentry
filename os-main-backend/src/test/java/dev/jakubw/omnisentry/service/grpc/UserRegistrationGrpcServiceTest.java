package dev.jakubw.omnisentry.service.grpc;

import com.google.protobuf.Timestamp;
import dev.jakubw.omnisentry.dto.CustomerDto;
import dev.jakubw.omnisentry.grpc.UserRegistrationRequest;
import dev.jakubw.omnisentry.grpc.UserRegistrationResponse;
import dev.jakubw.omnisentry.models.UserEntity;
import dev.jakubw.omnisentry.repos.UserRepository;
import dev.jakubw.omnisentry.services.SaltEdgeService;
import dev.jakubw.omnisentry.services.grpc.UserRegistrationGrpcService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationGrpcServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SaltEdgeService saltEdgeService;

    @Mock
    private StreamObserver<UserRegistrationResponse> responseObserver;

    @InjectMocks
    private UserRegistrationGrpcService userRegistrationGrpcService;

    @Captor
    private ArgumentCaptor<UserRegistrationResponse> responseCaptor;

    @Captor
    private ArgumentCaptor<UserEntity> userEntityCaptor;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        String email = "test@example.com";
        String username = "tester";
        String customerId = "cust_abc123";

        Timestamp dateOfBirth = Timestamp.newBuilder().setSeconds(946684800).setNanos(0).build();
        UserRegistrationRequest request = UserRegistrationRequest.newBuilder()
                .setEmail(email)
                .setUsername(username)
                .setName("John")
                .setSurname("Doe")
                .setDateOfBirth(dateOfBirth)
                .build();

        CustomerDto customerDto = new CustomerDto(customerId,email);
        when(saltEdgeService.createCustomer(email)).thenReturn(Optional.of(customerDto));
        when(userRepository.existsByEmailOrUsername(email, username)).thenReturn(false);

        // When
        userRegistrationGrpcService.registerUser(request, responseObserver);

        // Then
        verify(userRepository).save(userEntityCaptor.capture());
        UserEntity savedUser = userEntityCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getUsername()).isEqualTo(username);
        assertThat(savedUser.getSaltEdgeCustomerId()).isEqualTo(customerId);
        assertThat(savedUser.getDateOfBirth()).isEqualTo(Instant.ofEpochSecond(946684800, 0));

        verify(responseObserver).onNext(responseCaptor.capture());
        UserRegistrationResponse response = responseCaptor.getValue();
        assertThat(response.getResult()).isTrue();
        assertThat(response.getMessage()).isEqualTo("User registered successfully");
        verify(responseObserver).onCompleted();
    }

    @Test
    void shouldReturnErrorResponseWhenUserAlreadyExists() {
        // Given
        String email = "existing@example.com";
        String username = "existingUser";

        UserRegistrationRequest request = UserRegistrationRequest.newBuilder()
                .setEmail(email)
                .setUsername(username)
                .build();

        when(userRepository.existsByEmailOrUsername(email, username)).thenReturn(true);

        // When
        userRegistrationGrpcService.registerUser(request, responseObserver);

        // Then
        verify(userRepository, never()).save(any(UserEntity.class));

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        UserRegistrationResponse response = responseCaptor.getValue();

        assertThat(response.getResult()).isFalse();
        assertThat(response.getMessage()).isEqualTo("User with this Email or Username already exists");

        verify(responseObserver, times(1)).onCompleted();
        verify(responseObserver, never()).onError(any());
    }
}