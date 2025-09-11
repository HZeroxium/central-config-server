package com.example.user.adapter.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 * Only used when app.persistence.type=h2 (or any JPA-backed profile).
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, String>, PagingAndSortingRepository<UserEntity, String> {}


