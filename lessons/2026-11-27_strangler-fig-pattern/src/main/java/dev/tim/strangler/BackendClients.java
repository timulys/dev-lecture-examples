package dev.tim.strangler;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
@Configuration
public class BackendClients {
    private RestClient client(String url) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(2));
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    @Bean("legacyClient")
    RestClient legacy(@Value("${legacy.url}") String url) { return client(url); }
    @Bean("modernClient")
    RestClient modern(@Value("${modern.url}") String url) { return client(url); }
}
