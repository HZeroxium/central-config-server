// package com.example.control.web;

// import com.example.control.consulclient.client.ConsulClient;
// import com.example.control.consulclient.core.QueryOptions;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import java.util.Map;

// /**
//  * Test controller for the new Consul SDK.
//  */
// @Slf4j
// @RestController
// @RequestMapping("/api/consul-sdk")
// @RequiredArgsConstructor
// public class ConsulSdkTestController {
    
//     private final ConsulClient consulClient;
    
//     /**
//      * Test the new Consul SDK by listing all nodes.
//      */
//     @GetMapping("/nodes")
//     public ResponseEntity<Map<String, Object>> testNodes() {
//         try {
//             log.info("Testing new Consul SDK - listing nodes");
            
//             var response = consulClient.catalogNodes(QueryOptions.builder().build());
            
//             Map<String, Object> result = Map.of(
//                 "success", true,
//                 "message", "Successfully retrieved nodes using new Consul SDK",
//                 "data", response.getBody(),
//                 "consulIndex", response.getConsulIndex(),
//                 "knownLeader", response.getKnownLeader(),
//                 "lastContact", response.getLastContact() != null ? response.getLastContact().toString() : "N/A"
//             );
            
//             return ResponseEntity.ok(result);
            
//         } catch (Exception e) {
//             log.error("Failed to test new Consul SDK", e);
            
//             Map<String, Object> result = Map.of(
//                 "success", false,
//                 "message", "Failed to test new Consul SDK: " + e.getMessage(),
//                 "error", e.getClass().getSimpleName()
//             );
            
//             return ResponseEntity.internalServerError().body(result);
//         }
//     }
    
//     /**
//      * Test the new Consul SDK by listing all services.
//      */
//     @GetMapping("/services")
//     public ResponseEntity<Map<String, Object>> testServices() {
//         try {
//             log.info("Testing new Consul SDK - listing services");
            
//             var response = consulClient.catalogServices(QueryOptions.builder().build());
            
//             Map<String, Object> result = Map.of(
//                 "success", true,
//                 "message", "Successfully retrieved services using new Consul SDK",
//                 "data", response.getBody(),
//                 "consulIndex", response.getConsulIndex(),
//                 "knownLeader", response.getKnownLeader(),
//                 "lastContact", response.getLastContact() != null ? response.getLastContact().toString() : "N/A"
//             );
            
//             return ResponseEntity.ok(result);
            
//         } catch (Exception e) {
//             log.error("Failed to test new Consul SDK", e);
            
//             Map<String, Object> result = Map.of(
//                 "success", false,
//                 "message", "Failed to test new Consul SDK: " + e.getMessage(),
//                 "error", e.getClass().getSimpleName()
//             );
            
//             return ResponseEntity.internalServerError().body(result);
//         }
//     }
    
//     /**
//      * Test the new Consul SDK by getting cluster leader.
//      */
//     @GetMapping("/leader")
//     public ResponseEntity<Map<String, Object>> testLeader() {
//         try {
//             log.info("Testing new Consul SDK - getting cluster leader");
            
//             var response = consulClient.statusLeader(QueryOptions.builder().build());
            
//             Map<String, Object> result = Map.of(
//                 "success", true,
//                 "message", "Successfully retrieved cluster leader using new Consul SDK",
//                 "data", response.getBody(),
//                 "consulIndex", response.getConsulIndex(),
//                 "knownLeader", response.getKnownLeader(),
//                 "lastContact", response.getLastContact() != null ? response.getLastContact().toString() : "N/A"
//             );
            
//             return ResponseEntity.ok(result);
            
//         } catch (Exception e) {
//             log.error("Failed to test new Consul SDK", e);
            
//             Map<String, Object> result = Map.of(
//                 "success", false,
//                 "message", "Failed to test new Consul SDK: " + e.getMessage(),
//                 "error", e.getClass().getSimpleName()
//             );
            
//             return ResponseEntity.internalServerError().body(result);
//         }
//     }
// }
