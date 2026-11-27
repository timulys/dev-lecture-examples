package dev.tim.strangler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
@RestController
@Profile("legacy")
@ConditionalOnProperty(name = "legacy.catalog-enabled", havingValue = "true", matchIfMissing = true)
public class LegacyCatalogController {
    private static final Logger log = LoggerFactory.getLogger(LegacyCatalogController.class);
    private final LegacyInventory inventory;
    public LegacyCatalogController(LegacyInventory inventory) { this.inventory = inventory; }
    @GetMapping("/products/{id}")
    public ProductView find(@PathVariable String id) {
        StockView stock = inventory.read(id);
        log.info("LEGACY catalog {} stock={}", id, stock.quantity());
        return new ProductView(id, "노트", stock.quantity());
    }
}
