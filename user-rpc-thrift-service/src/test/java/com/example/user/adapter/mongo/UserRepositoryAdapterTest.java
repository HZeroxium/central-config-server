package com.example.user.adapter.mongo;

import com.example.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

 

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for UserRepositoryAdapter (MongoDB).
 * Tests all repository operations, data mapping, error handling, and metrics collection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryAdapter (MongoDB) Tests")
class UserRepositoryAdapterTest {

    @Mock
    private UserMongoRepository repository;


    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserRepositoryAdapter(repository);
    }

    @Nested
    @DisplayName("Save User Tests")
    class SaveUserTests {

        @Test
        @DisplayName("Should save user successfully")
        void shouldSaveUserSuccessfully() {
            // Given
            User inputUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            UserDocument savedDocument = UserDocument.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(repository.save(any(UserDocument.class))).thenReturn(savedDocument);

            // When
            User result = adapter.save(inputUser);

            // Then
            assertThat(result.getId()).isEqualTo("user-123");
            assertThat(result.getName()).isEqualTo("John Doe");
            assertThat(result.getPhone()).isEqualTo("+1-555-123-4567");
            assertThat(result.getAddress()).isEqualTo("123 Main St");
            
            verify(repository).save(any(UserDocument.class));
        }

        @Test
        @DisplayName("Should save user with null ID")
        void shouldSaveUserWithNullId() {
            // Given
            User inputUser = User.builder()
                    .id(null)
                    .name("Jane Doe")
                    .phone("+1-555-987-6543")
                    .address("456 Oak Ave")
                    .build();

            UserDocument savedDocument = UserDocument.builder()
                    .id("generated-id")
                    .name("Jane Doe")
                    .phone("+1-555-987-6543")
                    .address("456 Oak Ave")
                    .build();

            when(repository.save(any(UserDocument.class))).thenReturn(savedDocument);
            

            // When
            User result = adapter.save(inputUser);

            // Then
            assertThat(result.getId()).isEqualTo("generated-id");
            assertThat(result.getName()).isEqualTo("Jane Doe");
            assertThat(result.getPhone()).isEqualTo("+1-555-987-6543");
            assertThat(result.getAddress()).isEqualTo("456 Oak Ave");
            
            verify(repository).save(any(UserDocument.class));
        }

        @Test
        @DisplayName("Should handle database error during save")
        void shouldHandleDatabaseErrorDuringSave() {
            // Given
            User inputUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            RuntimeException dbException = new RuntimeException("MongoDB connection failed");
            when(repository.save(any(UserDocument.class))).thenThrow(dbException);
            

            // When & Then
            assertThatThrownBy(() -> adapter.save(inputUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("MongoDB connection failed");
            
        }

        @Test
        @DisplayName("Should handle null user input")
        void shouldHandleNullUserInput() {
            // Given
            User inputUser = null;
            

            // When & Then
            assertThatThrownBy(() -> adapter.save(inputUser))
                    .isInstanceOf(Exception.class);
            
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find user by ID when exists")
        void shouldFindUserByIdWhenExists() {
            // Given
            String userId = "user-123";
            UserDocument document = UserDocument.builder()
                    .id(userId)
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(repository.findById(userId)).thenReturn(Optional.of(document));
            

            // When
            Optional<User> result = adapter.findById(userId);

            // Then
            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getId()).isEqualTo(userId);
            assertThat(user.getName()).isEqualTo("John Doe");
            assertThat(user.getPhone()).isEqualTo("+1-555-123-4567");
            assertThat(user.getAddress()).isEqualTo("123 Main St");
            
            verify(repository).findById(userId);
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            String userId = "non-existent-user";
            when(repository.findById(userId)).thenReturn(Optional.empty());
            

            // When
            Optional<User> result = adapter.findById(userId);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findById(userId);
        }

        @Test
        @DisplayName("Should handle database error during find by ID")
        void shouldHandleDatabaseErrorDuringFindById() {
            // Given
            String userId = "user-123";
            RuntimeException dbException = new RuntimeException("MongoDB query failed");
            when(repository.findById(userId)).thenThrow(dbException);
            

            // When & Then
            assertThatThrownBy(() -> adapter.findById(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("MongoDB query failed");
            
        }

        @Test
        @DisplayName("Should handle null user ID")
        void shouldHandleNullUserId() {
            // Given
            String userId = null;
            when(repository.findById(userId)).thenReturn(Optional.empty());
            

            // When
            Optional<User> result = adapter.findById(userId);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findById(userId);
        }
    }

    @Nested
    @DisplayName("Find Page Tests")
    class FindPageTests {

        @Test
        @DisplayName("Should find users with pagination successfully")
        void shouldFindUsersWithPaginationSuccessfully() {
            // Given
            int page = 0;
            int size = 10;
            List<UserDocument> documents = List.of(
                    UserDocument.builder()
                            .id("user-1")
                            .name("User 1")
                            .phone("+1-555-111-1111")
                            .address("Address 1")
                            .build(),
                    UserDocument.builder()
                            .id("user-2")
                            .name("User 2")
                            .phone("+1-555-222-2222")
                            .address("Address 2")
                            .build()
            );

            Page<UserDocument> documentPage = new PageImpl<>(documents, PageRequest.of(page, size), 2L);
            when(repository.findAll(any(PageRequest.class))).thenReturn(documentPage);
            

            // When
            List<User> result = adapter.findPage(page, size);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo("user-1");
            assertThat(result.get(0).getName()).isEqualTo("User 1");
            assertThat(result.get(1).getId()).isEqualTo("user-2");
            assertThat(result.get(1).getName()).isEqualTo("User 2");
            
            verify(repository).findAll(any(PageRequest.class));
        }

        @Test
        @DisplayName("Should return empty list when no users found")
        void shouldReturnEmptyListWhenNoUsersFound() {
            // Given
            int page = 0;
            int size = 10;
            Page<UserDocument> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0L);
            when(repository.findAll(any(PageRequest.class))).thenReturn(emptyPage);
            

            // When
            List<User> result = adapter.findPage(page, size);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findAll(any(PageRequest.class));
        }

        @Test
        @DisplayName("Should handle database error during find page")
        void shouldHandleDatabaseErrorDuringFindPage() {
            // Given
            int page = 0;
            int size = 10;
            RuntimeException dbException = new RuntimeException("MongoDB aggregation failed");
            when(repository.findAll(any(PageRequest.class))).thenThrow(dbException);
            

            // When & Then
            assertThatThrownBy(() -> adapter.findPage(page, size))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("MongoDB aggregation failed");
            
        }

        @Test
        @DisplayName("Should handle large page sizes")
        void shouldHandleLargePageSizes() {
            // Given
            int page = 0;
            int size = 1000;
            Page<UserDocument> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0L);
            when(repository.findAll(any(PageRequest.class))).thenReturn(emptyPage);
            

            // When
            List<User> result = adapter.findPage(page, size);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findAll(PageRequest.of(page, size));
        }

        @Test
        @DisplayName("Should handle negative page numbers")
        void shouldHandleNegativePageNumbers() {
            // Given
            int page = -1;
            int size = 10;
            Page<UserDocument> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0L);
            when(repository.findAll(any(PageRequest.class))).thenReturn(emptyPage);
            

            // When
            List<User> result = adapter.findPage(page, size);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findAll(PageRequest.of(page, size));
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should count users successfully")
        void shouldCountUsersSuccessfully() {
            // Given
            long expectedCount = 42L;
            when(repository.count()).thenReturn(expectedCount);

            // When
            long result = adapter.count();

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(repository).count();
        }

        @Test
        @DisplayName("Should return zero count when no users")
        void shouldReturnZeroCountWhenNoUsers() {
            // Given
            when(repository.count()).thenReturn(0L);

            // When
            long result = adapter.count();

            // Then
            assertThat(result).isZero();
            verify(repository).count();
        }

        @Test
        @DisplayName("Should handle database error during count")
        void shouldHandleDatabaseErrorDuringCount() {
            // Given
            RuntimeException dbException = new RuntimeException("MongoDB count failed");
            when(repository.count()).thenThrow(dbException);

            // When & Then
            assertThatThrownBy(() -> adapter.count())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("MongoDB count failed");
            
        }

        @Test
        @DisplayName("Should handle large count values")
        void shouldHandleLargeCountValues() {
            // Given
            long largeCount = Long.MAX_VALUE;
            when(repository.count()).thenReturn(largeCount);

            // When
            long result = adapter.count();

            // Then
            assertThat(result).isEqualTo(largeCount);
            verify(repository).count();
        }
    }

    @Nested
    @DisplayName("Delete By ID Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should delete user by ID successfully")
        void shouldDeleteUserByIdSuccessfully() {
            // Given
            String userId = "user-123";

            // When
            adapter.deleteById(userId);

            // Then
            verify(repository).deleteById(userId);
        }

        @Test
        @DisplayName("Should handle database error during delete")
        void shouldHandleDatabaseErrorDuringDelete() {
            // Given
            String userId = "user-123";
            RuntimeException dbException = new RuntimeException("MongoDB delete failed");
            doThrow(dbException).when(repository).deleteById(userId);

            // When & Then
            assertThatThrownBy(() -> adapter.deleteById(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("MongoDB delete failed");
            
        }

        @Test
        @DisplayName("Should handle null user ID for delete")
        void shouldHandleNullUserIdForDelete() {
            // Given
            String userId = null;

            // When
            adapter.deleteById(userId);

            // Then
            verify(repository).deleteById(userId);
        }

        @Test
        @DisplayName("Should handle empty user ID for delete")
        void shouldHandleEmptyUserIdForDelete() {
            // Given
            String userId = "";

            // When
            adapter.deleteById(userId);

            // Then
            verify(repository).deleteById(userId);
        }
    }

    @Nested
    @DisplayName("Data Mapping Tests")
    class DataMappingTests {

        @Test
        @DisplayName("Should map domain user to document correctly")
        void shouldMapDomainUserToDocumentCorrectly() {
            // Given
            User domainUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            UserDocument savedDocument = UserDocument.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(repository.save(any(UserDocument.class))).thenReturn(savedDocument);

            // When
            User result = adapter.save(domainUser);

            // Then
            assertThat(result.getId()).isEqualTo(domainUser.getId());
            assertThat(result.getName()).isEqualTo(domainUser.getName());
            assertThat(result.getPhone()).isEqualTo(domainUser.getPhone());
            assertThat(result.getAddress()).isEqualTo(domainUser.getAddress());
        }

        @Test
        @DisplayName("Should map document to domain user correctly")
        void shouldMapDocumentToDomainUserCorrectly() {
            // Given
            UserDocument document = UserDocument.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("123 Main St")
                    .build();

            when(repository.findById("user-123")).thenReturn(Optional.of(document));

            // When
            Optional<User> result = adapter.findById("user-123");

            // Then
            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getId()).isEqualTo(document.getId());
            assertThat(user.getName()).isEqualTo(document.getName());
            assertThat(user.getPhone()).isEqualTo(document.getPhone());
            assertThat(user.getAddress()).isEqualTo(document.getAddress());
        }

        @Test
        @DisplayName("Should handle null fields in mapping")
        void shouldHandleNullFieldsInMapping() {
            // Given
            User domainUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address(null) // null address
                    .build();

            UserDocument savedDocument = UserDocument.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address(null)
                    .build();

            when(repository.save(any(UserDocument.class))).thenReturn(savedDocument);

            // When
            User result = adapter.save(domainUser);

            // Then
            assertThat(result.getId()).isEqualTo(domainUser.getId());
            assertThat(result.getName()).isEqualTo(domainUser.getName());
            assertThat(result.getPhone()).isEqualTo(domainUser.getPhone());
            assertThat(result.getAddress()).isNull();
        }

        @Test
        @DisplayName("Should handle empty string fields in mapping")
        void shouldHandleEmptyStringFieldsInMapping() {
            // Given
            User domainUser = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("") // empty address
                    .build();

            UserDocument savedDocument = UserDocument.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .address("")
                    .build();

            when(repository.save(any(UserDocument.class))).thenReturn(savedDocument);

            // When
            User result = adapter.save(domainUser);

            // Then
            assertThat(result.getId()).isEqualTo(domainUser.getId());
            assertThat(result.getName()).isEqualTo(domainUser.getName());
            assertThat(result.getPhone()).isEqualTo(domainUser.getPhone());
            assertThat(result.getAddress()).isEmpty();
        }

        @Test
        @DisplayName("Should handle special characters in mapping")
        void shouldHandleSpecialCharactersInMapping() {
            // Given
            User domainUser = User.builder()
                    .id("user-123")
                    .name("José María")
                    .phone("+1-555-123-4567")
                    .address("123 Main St, Apt #4B")
                    .build();

            UserDocument savedDocument = UserDocument.builder()
                    .id("user-123")
                    .name("José María")
                    .phone("+1-555-123-4567")
                    .address("123 Main St, Apt #4B")
                    .build();

            when(repository.save(any(UserDocument.class))).thenReturn(savedDocument);

            // When
            User result = adapter.save(domainUser);

            // Then
            assertThat(result.getId()).isEqualTo(domainUser.getId());
            assertThat(result.getName()).isEqualTo(domainUser.getName());
            assertThat(result.getPhone()).isEqualTo(domainUser.getPhone());
            assertThat(result.getAddress()).isEqualTo(domainUser.getAddress());
        }
    }

    @Nested
    @DisplayName("Metrics Integration Tests")
    class MetricsIntegrationTests {

        @Test
        @DisplayName("Should record metrics for all operations")
        void shouldRecordMetricsForAllOperations() {
            // Given
            User user = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            when(repository.save(any(UserDocument.class))).thenReturn(UserDocument.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build());
            when(repository.findById(anyString())).thenReturn(Optional.empty());
            when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
            when(repository.count()).thenReturn(0L);


            // When
            adapter.save(user);
            adapter.findById("user-123");
            adapter.findPage(0, 10);
            adapter.count();
            adapter.deleteById("user-123");

            // Then

        }

        @Test
        @DisplayName("Should record error metrics for failed operations")
        void shouldRecordErrorMetricsForFailedOperations() {
            // Given
            User user = User.builder()
                    .id("user-123")
                    .name("John Doe")
                    .phone("+1-555-123-4567")
                    .build();

            RuntimeException dbException = new RuntimeException("MongoDB error");
            when(repository.save(any(UserDocument.class))).thenThrow(dbException);
            when(repository.findById(anyString())).thenThrow(dbException);
            when(repository.findAll(any(PageRequest.class))).thenThrow(dbException);
            when(repository.count()).thenThrow(dbException);
            doThrow(dbException).when(repository).deleteById(anyString());

            

            // When & Then
            assertThatThrownBy(() -> adapter.save(user))
                    .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> adapter.findById("user-123"))
                    .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> adapter.findPage(0, 10))
                    .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> adapter.count())
                    .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> adapter.deleteById("user-123"))
                    .isInstanceOf(RuntimeException.class);

            // Then

        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle very large page sizes")
        void shouldHandleVeryLargePageSizes() {
            // Given
            int page = 0;
            int size = Integer.MAX_VALUE;
            Page<UserDocument> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0L);
            when(repository.findAll(any(PageRequest.class))).thenReturn(emptyPage);
            

            // When
            List<User> result = adapter.findPage(page, size);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findAll(PageRequest.of(page, size));
        }

        @Test
        @DisplayName("Should handle zero page size")
        void shouldHandleZeroPageSize() {
            // Given
            int page = 0;
            int size = 0;
            Page<UserDocument> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0L);
            when(repository.findAll(any(PageRequest.class))).thenReturn(emptyPage);
            

            // When
            List<User> result = adapter.findPage(page, size);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findAll(PageRequest.of(page, size));
        }

        @Test
        @DisplayName("Should handle very large page numbers")
        void shouldHandleVeryLargePageNumbers() {
            // Given
            int page = Integer.MAX_VALUE;
            int size = 10;
            Page<UserDocument> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0L);
            when(repository.findAll(any(PageRequest.class))).thenReturn(emptyPage);
                

            // When
            List<User> result = adapter.findPage(page, size);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findAll(PageRequest.of(page, size));
        }
    }
}
