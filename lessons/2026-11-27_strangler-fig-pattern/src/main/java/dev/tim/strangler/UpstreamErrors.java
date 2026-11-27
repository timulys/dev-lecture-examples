package dev.tim.strangler;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
@RestControllerAdvice
public class UpstreamErrors {
    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<Map<String, String>> status(RestClientResponseException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("error", "upstream_response"));
    }
    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Map<String, String>> unavailable(ResourceAccessException error) {
        return ResponseEntity.status(502).body(Map.of("error", "upstream_unavailable"));
    }
}
