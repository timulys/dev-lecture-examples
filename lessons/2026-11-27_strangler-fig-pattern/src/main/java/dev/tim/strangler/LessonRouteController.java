package dev.tim.strangler;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
@RestController
@Profile("gateway")
public class LessonRouteController {
    private final CatalogRoutes routes;
    public LessonRouteController(CatalogRoutes routes) { this.routes = routes; }
    @PutMapping("/lesson/catalog-route/{mode}")
    public String select(@PathVariable CatalogRoutes.Mode mode) {
        routes.use(mode);
        return mode.name();
    }
}
