package com.example.sample.web;

import com.vng.zing.zcm.client.ClientApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sdk")
@RequiredArgsConstructor
public class SdkTestController {

  private final ClientApi client;

  @GetMapping("/snapshot")
  public ResponseEntity<Map<String, Object>> getSnapshot() {
    String hash = client.configHash();
    Map<String, Object> snap = client.configSnapshotMap();

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "ok");
    body.put("hash", hash);
    body.putAll(snap);
    body.put("keyCount", ((Map<?, ?>) snap.get("properties")).size());
    return ResponseEntity.ok(body);
  }
}


