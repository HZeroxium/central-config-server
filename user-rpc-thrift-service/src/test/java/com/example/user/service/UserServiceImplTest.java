package com.example.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.user.domain.User;
import com.example.user.service.port.UserRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
public class UserServiceImplTest {

    private static final String VALID_USER_PHONE = "+84-123-456-789";
    private static final String VALID_USER_NAME = "Nguyen Gia Huy";
    private static final String VALID_USER_ADDRESS = "123 Street, District 10";
    private static final String VALID_USER_ID = "validUserId";

    @Mock
    private UserRepositoryPort userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            // ARRANGE
            User inputUser = User.builder()
                    .name(VALID_USER_NAME)
                    .phone(VALID_USER_PHONE)
                    .address(VALID_USER_ADDRESS)
                    .build();

            User createdUser = User.builder().id(VALID_USER_ID).name(VALID_USER_NAME).phone(VALID_USER_PHONE)
                    .address(VALID_USER_ADDRESS).createdAt(LocalDateTime.now()).createdBy("admin").version(1)
                    .deleted(false).build();

            when(userRepository.save(any(User.class))).thenReturn(createdUser);

            // ACT
            User result = userService.create(inputUser);
            
            // ASSERT
            assertThat(result).isEqualTo(createdUser);
            assertThat(result.getId()).isEqualTo(VALID_USER_ID);
            assertThat(result.getCreatedBy()).isEqualTo("admin");
            assertThat(result.getDeleted()).isFalse();

            // VERIFY - To validate that service call the right method of repository
            verify(userRepository).save(any(User.class));
        }
    }
}
