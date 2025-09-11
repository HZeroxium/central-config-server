package com.example.rest.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.rest.user.domain.User;
import com.example.rest.user.service.UserService;

class UserControllerTest {
  private MockMvc mockMvc;
  private UserService userService;

  @BeforeEach
  void setup() {
    userService = Mockito.mock(UserService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
  }

  @Test
  void pingEndpoint() throws Exception {
    when(userService.ping()).thenReturn("pong");
    mockMvc
        .perform(get("/users/ping"))
        .andExpect(status().isOk())
        .andExpect(content().string("pong"));
  }

  @Test
  void getUser() throws Exception {
    when(userService.getById("1"))
        .thenReturn(Optional.of(User.builder().id("1").name("A").build()));
    mockMvc
        .perform(get("/users/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("1"));
  }
}
