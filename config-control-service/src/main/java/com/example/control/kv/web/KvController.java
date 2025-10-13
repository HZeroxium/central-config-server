package com.example.control.kv.web;

import com.example.control.kv.KvStore;
import com.example.control.kv.model.KvDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/kv")
@RequiredArgsConstructor
public class KvController {

  private final KvStore kvStore;

  @PutMapping("/{key:.*}")
  public ResponseEntity<PutResponse> put(
      @PathVariable String key,
      @Valid @RequestBody PutRequest request) {
    try {
      log.debug("Put request for key: {}", key);
      
      kvStore.put(key, request.valueBytes(), request.expectedVersion(), request.ttl());
      
      // Get the updated entry to return version
      Optional<KvStore.Entry> entry = kvStore.get(key);
      String version = entry.map(KvStore.Entry::version).orElse("0");
      
      return ResponseEntity.ok(new PutResponse(true, version));
    } catch (Exception e) {
      log.error("Failed to put key: {}", key, e);
      return ResponseEntity.internalServerError()
          .body(new PutResponse(false, null));
    }
  }

  @GetMapping("/{key:.*}")
  public ResponseEntity<Entry> get(@PathVariable String key) {
    try {
      log.debug("Get request for key: {}", key);
      
      Optional<KvStore.Entry> entry = kvStore.get(key);
      if (entry.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      
      KvStore.Entry kvEntry = entry.get();
      String base64Value = Base64.getEncoder().encodeToString(kvEntry.value());
      
      Entry response = new Entry(
          kvEntry.key(),
          base64Value,
          kvEntry.version(),
          kvEntry.createIndex(),
          kvEntry.modifyIndex()
      );
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to get key: {}", key, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @DeleteMapping("/{key:.*}")
  public ResponseEntity<DeleteResponse> delete(
      @PathVariable String key,
      @RequestParam(required = false) String expectedVersion) {
    try {
      log.debug("Delete request for key: {}", key);
      
      boolean deleted = kvStore.delete(key, expectedVersion);
      return ResponseEntity.ok(new DeleteResponse(deleted));
    } catch (Exception e) {
      log.error("Failed to delete key: {}", key, e);
      return ResponseEntity.internalServerError()
          .body(new DeleteResponse(false));
    }
  }

  @GetMapping
  public ResponseEntity<ListResponse> list(
      @RequestParam(required = false) String prefix,
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(required = false) String fromKey) {
    try {
      log.debug("List request with prefix: {}, limit: {}, fromKey: {}", prefix, limit, fromKey);
      
      List<KvStore.Entry> entries = kvStore.list(
          prefix != null ? prefix : "",
          limit,
          fromKey
      );
      
      List<Entry> responseEntries = entries.stream()
          .map(entry -> new Entry(
              entry.key(),
              Base64.getEncoder().encodeToString(entry.value()),
              entry.version(),
              entry.createIndex(),
              entry.modifyIndex()
          ))
          .toList();
      
      return ResponseEntity.ok(new ListResponse(responseEntries));
    } catch (Exception e) {
      log.error("Failed to list keys with prefix: {}", prefix, e);
      return ResponseEntity.internalServerError()
          .body(new ListResponse(List.of()));
    }
  }

  @PostMapping("/txn")
  public ResponseEntity<TxnResponse> transaction(@Valid @RequestBody TxnRequest request) {
    try {
      log.debug("Transaction request with {} operations", request.ops().size());
      
      List<KvStore.TxnOp> txnOps = request.ops().stream()
          .map(op -> new KvStore.TxnOp(
              KvStore.TxnOp.Type.valueOf(op.type().toUpperCase()),
              op.key(),
              Base64.getDecoder().decode(op.base64Value() != null ? op.base64Value() : ""),
              op.expectedVersion(),
              op.ttlSeconds() != null ? Duration.ofSeconds(op.ttlSeconds()) : null
          ))
          .toList();
      
      List<Boolean> results = kvStore.txn(txnOps);
      return ResponseEntity.ok(new TxnResponse(results));
    } catch (Exception e) {
      log.error("Failed to execute transaction", e);
      return ResponseEntity.internalServerError()
          .body(new TxnResponse(List.of()));
    }
  }

  @GetMapping(value = "/watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter watch(@RequestParam String prefix) {
    log.debug("Watch request for prefix: {}", prefix);
    
    SseEmitter emitter = new SseEmitter(0L); // No timeout
    
    KvStore.WatchHandler handler = new KvStore.WatchHandler() {
      @Override
      public void onPut(KvStore.Entry entry) {
        try {
          Entry responseEntry = new Entry(
              entry.key(),
              Base64.getEncoder().encodeToString(entry.value()),
              entry.version(),
              entry.createIndex(),
              entry.modifyIndex()
          );
          emitter.send(SseEmitter.event()
              .name("put")
              .data(responseEntry));
        } catch (Exception e) {
          log.error("Failed to send put event", e);
          emitter.completeWithError(e);
        }
      }
      
      @Override
      public void onDelete(String key, String version) {
        try {
          emitter.send(SseEmitter.event()
              .name("delete")
              .data(new DeleteEvent(key, version)));
        } catch (Exception e) {
          log.error("Failed to send delete event", e);
          emitter.completeWithError(e);
        }
      }
      
      @Override
      public void onError(Throwable throwable) {
        log.error("Watch error for prefix: {}", prefix, throwable);
        emitter.completeWithError(throwable);
      }
    };
    
    // Start watching in a separate thread
    new Thread(() -> {
      try {
        kvStore.watchPrefix(prefix, handler);
      } catch (Exception e) {
        log.error("Failed to start watch for prefix: {}", prefix, e);
        emitter.completeWithError(e);
      }
    }).start();
    
    return emitter;
  }

  @PostMapping("/lock")
  public ResponseEntity<LockResponse> acquireLock(@Valid @RequestBody LockRequest request) {
    try {
      log.debug("Acquire lock request for key: {}", request.lockKey());
      
      Duration ttl = request.ttlSeconds() != null ? 
          Duration.ofSeconds(request.ttlSeconds()) : 
          Duration.ofMinutes(5);
      
      String lockId = kvStore.acquireLock(request.lockKey(), ttl);
      if (lockId != null) {
        return ResponseEntity.ok(new LockResponse(lockId));
      } else {
        return ResponseEntity.status(409)
            .body(new LockResponse(null));
      }
    } catch (Exception e) {
      log.error("Failed to acquire lock for key: {}", request.lockKey(), e);
      return ResponseEntity.internalServerError()
          .body(new LockResponse(null));
    }
  }

  @DeleteMapping("/lock/{lockKey}")
  public ResponseEntity<Void> releaseLock(
      @PathVariable String lockKey,
      @RequestParam String lockId) {
    try {
      log.debug("Release lock request for key: {}", lockKey);
      
      boolean released = kvStore.releaseLock(lockKey, lockId);
      if (released) {
        return ResponseEntity.noContent().build();
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      log.error("Failed to release lock for key: {}", lockKey, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PutMapping("/ephemeral/{key:.*}")
  public ResponseEntity<EphemeralResponse> putEphemeral(
      @PathVariable String key,
      @Valid @RequestBody EphemeralRequest request) {
    try {
      log.debug("Put ephemeral request for key: {}", key);
      
      Duration ttl = request.ttlSeconds() != null ? 
          Duration.ofSeconds(request.ttlSeconds()) : 
          Duration.ofMinutes(5);
      
      String ephemeralId = kvStore.putEphemeral(key, request.valueBytes(), ttl);
      return ResponseEntity.ok(new EphemeralResponse(ephemeralId));
    } catch (Exception e) {
      log.error("Failed to put ephemeral key: {}", key, e);
      return ResponseEntity.internalServerError()
          .body(new EphemeralResponse(null));
    }
  }

  // Helper record for delete events in SSE
  private record DeleteEvent(String key, String version) {}
  
  // Helper record for ephemeral requests/responses
  private record EphemeralRequest(String base64Value, Long ttlSeconds) {
    public byte[] valueBytes() {
      return (base64Value == null) ? new byte[0] : Base64.getDecoder().decode(base64Value);
    }
  }
  
  private record EphemeralResponse(String ephemeralId) {}
}
