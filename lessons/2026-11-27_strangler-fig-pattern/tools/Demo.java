import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class Demo {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String base;
    public static void main(String[] args) throws Exception {
        String step = args.length == 0 ? "baseline" : args[0];
        base = args.length > 1 ? args[1] : "http://127.0.0.1:19080";
        switch (step) {
            case "baseline" -> { route("LEGACY"); getProduct(200); }
            case "migrate" -> { route("MODERN"); getProduct(200); }
            case "update" -> {
                route("MODERN");
                call("PUT", "/inventory/P100", "{\"quantity\":7}", 200);
                getProduct(200);
            }
            case "rollback" -> { route("LEGACY"); getProduct(200); }
            case "missing" -> {
                route("LEGACY"); call("GET", "/products/UNKNOWN", null, 404);
                route("MODERN"); call("GET", "/products/UNKNOWN", null, 404);
            }
            case "outage" -> {
                route("MODERN"); getProduct(502);
                route("LEGACY"); getProduct(200);
            }
            case "retired" -> {
                route("MODERN"); getProduct(200);
                route("LEGACY"); getProduct(404);
                route("MODERN");
            }
            default -> throw new IllegalArgumentException("Unknown step: " + step);
        }
    }
    private static void route(String mode) throws Exception { call("PUT", "/lesson/catalog-route/" + mode, null, 200); }
    private static void getProduct(int expected) throws Exception { call("GET", "/products/P100", null, expected); }
    private static void call(String method, String path, String body, int expected) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(method + " " + path + " -> " + response.statusCode());
        response.headers().firstValue("X-Lesson-Catalog").ifPresent(source -> System.out.println("catalog: " + source));
        System.out.println(response.body());
        if (response.statusCode() != expected) throw new AssertionError("Expected " + expected + ", got " + response.statusCode());
    }
}
