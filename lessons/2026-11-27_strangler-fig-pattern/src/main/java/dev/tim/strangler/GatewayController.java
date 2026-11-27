package dev.tim.strangler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
@RestController
@Profile("gateway")
public class GatewayController {
    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);
    private final RestClient legacy;
    private final RestClient modern;
    private final CatalogRoutes routes;
    public GatewayController(@Qualifier("legacyClient") RestClient legacy,
            @Qualifier("modernClient") RestClient modern, CatalogRoutes routes) {
        this.legacy = legacy; this.modern = modern; this.routes = routes;
    }
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductView> find(@PathVariable String id) {
        CatalogRoutes.Mode mode = routes.current();
        RestClient target = mode == CatalogRoutes.Mode.MODERN ? modern : legacy;
        ProductView product = target.get().uri("/products/{id}", id)
                .retrieve().body(ProductView.class);
        log.info("GATEWAY catalog {} -> {}", id, mode);
        return ResponseEntity.ok().header("X-Lesson-Catalog", mode.name()).body(product);
    }
    @PutMapping("/inventory/{id}")
    public StockView change(@PathVariable String id, @RequestBody StockChange change) {
        return legacy.put().uri("/inventory/{id}", id).body(change)
                .retrieve().body(StockView.class);
    }
}
