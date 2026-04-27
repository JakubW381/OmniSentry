package dev.jakubw.omnisentry.controllers;

import dev.jakubw.omnisentry.dto.SaltEdgeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/callbacks/saltedge")
public class SaltEdgeWebhook {

    @PostMapping("/success")
    public ResponseEntity<Void> handleSuccess(@RequestBody SaltEdgeResponse<Map<String,Object>> response) {
        var data = response.data();
        log.info("Received SaltEdge success callback: {}" , data);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fail")
    public ResponseEntity<Void> handleFailure(@RequestBody SaltEdgeResponse<Map<String,Object>> response) {
        var error = response.data();
        log.info("Received SaltEdge error callback: {}" , error);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/destroy")
    public ResponseEntity<Void> handleDestroy(@RequestBody SaltEdgeResponse<Map<String,Object>> response) {
        var destroy = response.data();
        log.info("Received SaltEdge destroy callback: {}" , destroy);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/notify")
    public ResponseEntity<Void> handleNotify(@RequestBody SaltEdgeResponse<Map<String,Object>> response) {
        var notify = response.data();
        log.info("Received SaltEdge notify callback: {}" , notify);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/changes/provider")
    public ResponseEntity<Void> handleProviderChanges(@RequestBody SaltEdgeResponse<Map<String,Object>> response) {
        var changes = response.data();
        log.info("Received SaltEdge provider changes callback: {}" , changes);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/changes/consent")
    public ResponseEntity<Void> handleConsentChanges(@RequestBody SaltEdgeResponse<Map<String,Object>> response) {
        var changes = response.data();
        log.info("Received SaltEdge consent changes callback: {}" , changes);
        return ResponseEntity.ok().build();
    }
}
