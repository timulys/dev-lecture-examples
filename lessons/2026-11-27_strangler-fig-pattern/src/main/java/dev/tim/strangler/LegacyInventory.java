package dev.tim.strangler;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
@Profile("legacy")
public class LegacyInventory {
    private final AtomicInteger quantity = new AtomicInteger(10);
    public void requireProduct(String id) {
        if (!"P100".equals(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    public StockView read(String id) {
        requireProduct(id);
        return new StockView(id, quantity.get());
    }
    public StockView change(String id, StockChange change) {
        requireProduct(id);
        if (change.quantity() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        quantity.set(change.quantity());
        return new StockView(id, quantity.get());
    }
}
