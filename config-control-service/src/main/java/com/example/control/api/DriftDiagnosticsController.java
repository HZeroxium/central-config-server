package com.example.control.api;
import com.example.control.config.ConfigServerProperties;
import com.example.control.configsnapshot.ConfigSnapshot;
import com.example.control.configsnapshot.ConfigSnapshotBuilderFromConfigServer;
import com.example.control.configsnapshot.Sha256Hasher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/drift-diagnostics")
@RequiredArgsConstructor
@Tag(name = "Drift Diagnostics", description = "Diagnostics for expected snapshot and hash")
public class DriftDiagnosticsController {

  private final ObjectMapper objectMapper;
  private final ConfigServerProperties configProps;
  private final RestClient rest = RestClient.create();
  private final ConfigSnapshotBuilderFromConfigServer builder = new ConfigSnapshotBuilderFromConfigServer();

  @GetMapping("/{service}/{profile}")
  @Operation(summary = "Get expected snapshot", description = "Return expected snapshot keys and hash from Config Server")
  public ResponseEntity<Map<String, Object>> getExpectedSnapshot(
      @PathVariable String service,
      @PathVariable String profile) {

    try {
      String url = normalize(configProps.getUrl()) + "/" + service + "/" + (profile == null || profile.isBlank() ? "default" : profile);
      String json = rest.get().uri(url).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
      JsonNode node = objectMapper.readTree(json);
      ConfigSnapshot snapshot = builder.build(service, profile, null, node);
      String hash = Sha256Hasher.hash(snapshot.toCanonicalString());
      var keys = snapshot.getProperties().keySet().stream().limit(50).toList();
      return ResponseEntity.ok(Map.of(
          "status", "ok",
          "application", service,
          "profile", profile,
          "keyCount", snapshot.getProperties().size(),
          "keysPreview", keys,
          "hash", hash,
          "properties", snapshot.getProperties()));
    } catch (Exception e) {
      log.error("Diagnostics failed", e);
      return ResponseEntity.status(500).body(Map.of(
          "status", "error",
          "message", e.getMessage()));
    }
  }

  private String normalize(String url) {
    if (url.endsWith("/")) return url.substring(0, url.length() - 1);
    return url;
  }
}


