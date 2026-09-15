package comp3011.assignment1.config;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configures the HTTP client used to communicate with OpenAI.
 */
@Configuration
public class OpenAiClientConfig {

    /**
     * Creates a reusable RestClient containing the OpenAI authorization
     *
     * @param apiKey OpenAI API key from the env
     * @return configured REST client
     */
    @Bean
    public RestClient openAiRestClient(
    		
            @Value("${openai.api.key}") String apiKey) {

        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

        
        ProxySelector proxySelector = ProxySelector.getDefault();
        
        //If proxy exists, configure HTTP client to use it
        if (proxySelector != null) {
            httpClientBuilder.proxy(proxySelector);
        }

        //build the actual java HttpClient using the setting configured 
        HttpClient httpClient = httpClientBuilder.build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }
}