package com.example.watcher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Data
@Validated
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsProperties {

    @NotBlank
    private String pingRequest = "ping.request";

    @NotBlank
    private String pingResponse = "ping.response";

    @NotBlank
    private String userCreateRequest = "user.create.request";

    @NotBlank
    private String userCreateResponse = "user.create.response";

    @NotBlank
    private String userGetRequest = "user.get.request";

    @NotBlank
    private String userGetResponse = "user.get.response";

    @NotBlank
    private String userUpdateRequest = "user.update.request";

    @NotBlank
    private String userUpdateResponse = "user.update.response";

    @NotBlank
    private String userDeleteRequest = "user.delete.request";

    @NotBlank
    private String userDeleteResponse = "user.delete.response";

    @NotBlank
    private String userListRequest = "user.list.request";

    @NotBlank
    private String userListResponse = "user.list.response";

    // V2 Async topics
    @NotBlank
    private String userCommands = "user.commands";

    @NotBlank
    private String userEvents = "user.events";

    @NotBlank
    private String userOperations = "user.operations";
}
