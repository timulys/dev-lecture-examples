package dev.tim.strangler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
@RestController
@Profile("modern")
public class ModernCatalogController {
    private static final Logger log = LoggerFactory.getLogger(ModernCatalogController.class);
    private final RestClient legacy;
    public ModernCatalogController(@Qualifier("legacyClient") RestClient legacy) { this.legacy = legacy; }
    @GetMapping("/products/{id}")
    public ProductView find(@PathVariable String id) {
        StockView stock = legacy.get().uri("/inventory/{id}", id)
                .retrieve().body(StockView.class);
        log.info("MODERN catalog {} stock={}", id, stock.quantity());
        return new ProductView(id, "노트", stock.quantity());
    }
}
