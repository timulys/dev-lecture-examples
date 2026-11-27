package dev.tim.strangler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class MigrationIT {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void migrateChangeStockRollBackAndHandleFailuresOverRealHttp() throws Exception {
        try (Cluster c = new Cluster(false)) {
            String base = c.gatewayUrl();
            HttpResponse<String> before = call("GET", base + "/products/P100", null);
            assertProduct(before, 10, "LEGACY");
            step("baseline", before);
            assertEquals("MODERN", call("PUT", base + "/lesson/catalog-route/MODERN", null).body());
            HttpResponse<String> after = call("GET", base + "/products/P100", null);
            assertProduct(after, 10, "MODERN");
            assertEquals(json.readTree(before.body()), json.readTree(after.body()));
            step("migrate: identical response body", after);
            HttpResponse<String> changed = call("PUT", base + "/inventory/P100", "{\"quantity\":7}");
            assertEquals(200, changed.statusCode());
            assertEquals(7, json.readTree(changed.body()).get("quantity").asInt());
            HttpResponse<String> modern = call("GET", base + "/products/P100", null);
            assertProduct(modern, 7, "MODERN");
            step("stock owner remains legacy", modern);
            assertEquals(400, call("PUT", base + "/inventory/P100", "{\"quantity\":-1}").statusCode());
            assertProduct(call("GET", base + "/products/P100", null), 7, "MODERN");
            assertEquals(404, call("GET", base + "/products/UNKNOWN", null).statusCode());
            assertEquals(404, call("PUT", base + "/inventory/UNKNOWN", "{\"quantity\":3}").statusCode());
            call("PUT", base + "/lesson/catalog-route/LEGACY", null);
            HttpResponse<String> rollback = call("GET", base + "/products/P100", null);
            assertProduct(rollback, 7, "LEGACY");
            step("rollback changes route, stock remains 7", rollback);
            assertEquals(404, call("GET", base + "/products/UNKNOWN", null).statusCode());
            assertEquals(400, call("PUT", base + "/lesson/catalog-route/INVALID", null).statusCode());
            c.stop(c.modern);
            call("PUT", base + "/lesson/catalog-route/MODERN", null);
            HttpResponse<String> unavailable = call("GET", base + "/products/P100", null);
            assertEquals(502, unavailable.statusCode());
            assertEquals("upstream_unavailable", json.readTree(unavailable.body()).get("error").asText());
            step("modern stopped: no automatic route fallback", unavailable);
            assertEquals(200, call("PUT", base + "/inventory/P100", "{\"quantity\":6}").statusCode());
            call("PUT", base + "/lesson/catalog-route/LEGACY", null);
            assertProduct(call("GET", base + "/products/P100", null), 6, "LEGACY");
            String legacyLog = Files.readString(c.logs.resolve("legacy.log"));
            String modernLog = Files.readString(c.logs.resolve("modern.log"));
            assertTrue(legacyLog.contains("LEGACY catalog P100 stock=10"));
            assertTrue(legacyLog.contains("LEGACY inventory write P100 quantity=7"));
            assertTrue(modernLog.contains("MODERN catalog P100 stock=7"));
        }
    }

    @Test
    void retireOnlyOldCatalogAndProveRemainingInventoryDependency() throws Exception {
        try (Cluster c = new Cluster(true)) {
            assertEquals(404, call("GET", c.legacyUrl() + "/products/P100", null).statusCode());
            assertEquals(200, call("GET", c.legacyUrl() + "/inventory/P100", null).statusCode());
            call("PUT", c.gatewayUrl() + "/lesson/catalog-route/MODERN", null);
            HttpResponse<String> migrated = call("GET", c.gatewayUrl() + "/products/P100", null);
            assertProduct(migrated, 10, "MODERN");
            step("old catalog disabled; new catalog and old inventory work", migrated);
            call("PUT", c.gatewayUrl() + "/lesson/catalog-route/LEGACY", null);
            assertEquals(404, call("GET", c.gatewayUrl() + "/products/P100", null).statusCode());
            call("PUT", c.gatewayUrl() + "/lesson/catalog-route/MODERN", null);
            c.stop(c.legacy);
            HttpResponse<String> dependent = call("GET", c.gatewayUrl() + "/products/P100", null);
            assertEquals(502, dependent.statusCode());
            step("stopping legacy inventory also breaks new catalog", dependent);
        }
    }

    private void assertProduct(HttpResponse<String> response, int stock, String source) throws Exception {
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = json.readTree(response.body());
        assertEquals(Set.of("id", "name", "stock"), fieldNames(body));
        assertEquals("P100", body.get("id").asText());
        assertEquals("노트", body.get("name").asText());
        assertEquals(stock, body.get("stock").asInt());
        assertEquals(source, response.headers().firstValue("X-Lesson-Catalog").orElseThrow());
    }
    private Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>(); node.fieldNames().forEachRemaining(names::add); return names;
    }
    private HttpResponse<String> call(String method, String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
    private void step(String name, HttpResponse<String> response) {
        System.out.println("SCENARIO " + name + " | HTTP " + response.statusCode() + " | " + response.body());
    }
    private int freePort() throws Exception { try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); } }

    private final class Cluster implements AutoCloseable {
        final int legacyPort = freePort(), modernPort = freePort(), gatewayPort = freePort();
        final Path logs;
        final List<Process> processes = new ArrayList<>();
        Process legacy, modern;
        Cluster(boolean retired) throws Exception {
            logs = Path.of("target", "scenario-logs", retired ? "retired" : "migration");
            Files.createDirectories(logs);
            try {
                legacy = start("legacy", legacyPort, "--legacy.catalog-enabled=" + !retired);
                modern = start("modern", modernPort);
                start("gateway", gatewayPort);
            } catch (Exception | Error failure) { close(); throw failure; }
        }
        String legacyUrl() { return "http://127.0.0.1:" + legacyPort; }
        String gatewayUrl() { return "http://127.0.0.1:" + gatewayPort; }
        Process start(String role, int port, String... extra) throws Exception {
            List<String> args = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-jar", Path.of("target/strangler-fig-demo.jar").toAbsolutePath().toString(),
                    "--spring.profiles.active=" + role, "--server.port=" + port,
                    "--legacy.url=" + legacyUrl(), "--modern.url=http://127.0.0.1:" + modernPort));
            args.addAll(List.of(extra));
            Process p = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(logs.resolve(role + ".log").toFile()).start();
            processes.add(p);
            long deadline = System.nanoTime() + Duration.ofSeconds(35).toNanos();
            while (System.nanoTime() < deadline) {
                if (!p.isAlive()) throw new AssertionError(role + " stopped: " + Files.readString(logs.resolve(role + ".log")));
                try { if (call("GET", "http://127.0.0.1:" + port + "/ready", null).statusCode() == 200) return p; }
                catch (java.io.IOException expectedDuringStartup) { }
                Thread.sleep(100);
            }
            throw new AssertionError("Startup deadline exceeded: " + role);
        }
        void stop(Process p) throws Exception {
            p.destroy();
            if (!p.waitFor(5, TimeUnit.SECONDS)) { p.destroyForcibly(); assertTrue(p.waitFor(5, TimeUnit.SECONDS)); }
        }
        public void close() throws Exception {
            for (int i = processes.size() - 1; i >= 0; i--) stop(processes.get(i));
        }
    }
}
