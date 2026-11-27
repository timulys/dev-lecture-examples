package dev.tim.strangler;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component
@Profile("gateway")
public class CatalogRoutes {
    public enum Mode { LEGACY, MODERN }
    private final AtomicReference<Mode> mode;
    public CatalogRoutes(@Value("${catalog.route}") Mode initial) { mode = new AtomicReference<>(initial); }
    public Mode current() { return mode.get(); }
    public void use(Mode next) { mode.set(next); }
}
