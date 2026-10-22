package com.luairan.control;

import com.luairan.business.*;
import com.luairan.service.context.LeaseUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/business/entries")
@ConditionalOnProperty(name = "paxos.business.enabled", havingValue = "true")
public final class BusinessController {
    private final FencedWriteService service;
    public BusinessController(FencedWriteService service) { this.service = service; }
    public record WriteBody(String value) { }
    @PutMapping("/{key}") public ResponseEntity<Void> write(@PathVariable String key, @RequestBody WriteBody body) {
        service.write(key, body.value()); return ResponseEntity.noContent().build();
    }
    @GetMapping("/{key}") public ResponseEntity<Map<String, String>> read(@PathVariable String key) {
        return service.read(key).map(value -> ResponseEntity.ok(Map.of("value", value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @ExceptionHandler({FencedOutException.class, LeaseUnavailableException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> conflict() { return Map.of("error", "No current fenced write generation; retry after election"); }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalid() { return Map.of("error", "Invalid entry"); }
    @ExceptionHandler({DataAccessException.class, org.springframework.transaction.TransactionException.class}) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    Map<String, String> unavailable() { return Map.of("error", "Business storage unavailable"); }
}
