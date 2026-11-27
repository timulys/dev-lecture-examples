package dev.tim.strangler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
@RestController
@Profile("legacy")
public class LegacyStockController {
    private static final Logger log = LoggerFactory.getLogger(LegacyStockController.class);
    private final LegacyInventory inventory;
    public LegacyStockController(LegacyInventory inventory) { this.inventory = inventory; }
    @GetMapping("/inventory/{id}")
    public StockView find(@PathVariable String id) {
        StockView stock = inventory.read(id);
        log.info("LEGACY inventory read {} quantity={}", id, stock.quantity());
        return stock;
    }
    @PutMapping("/inventory/{id}")
    public StockView change(@PathVariable String id, @RequestBody StockChange change) {
        StockView stock = inventory.change(id, change);
        log.info("LEGACY inventory write {} quantity={}", id, stock.quantity());
        return stock;
    }
}
