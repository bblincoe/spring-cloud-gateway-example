package com.bblincoe.examples.gatewayapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableConfigurationProperties(UriConfiguration.class)
@RestController
public class GatewayAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayAppApplication.class, args);
    }

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder,
                                 UriConfiguration uriConfiguration) {

        String httpUri = uriConfiguration.getHttpbin();
        return builder.routes()
            // Create a custom route from /get to httpbin.org
            // To test run: curl http://localhost:8080/get'
            .route(p -> p
                .path("/get")
                .filters(f -> f.addRequestHeader("Hello", "World"))
                .uri(httpUri))
            // Introduce hystrix circuit breaker pattern for fault tolerance (fallback)
            // You might want to use this pattern to retrieve "stale" data vs. showing timeouts
            // To test run: curl --dump-header - --header 'Host: www.hystrix.com' http://localhost:8080/delay/3
            .route(p -> p
                .host("*.hystrix.com")
                .filters(f -> f.hystrix(config -> config
                    .setName("mycmd")
                    .setFallbackUri("forward:/fallback")))
                .uri(httpUri))
                .build();
    }

    @RequestMapping("/fallback")
    public Mono<String> fallback() {
        return Mono.just("fallback");
    }

}

@ConfigurationProperties
class UriConfiguration {

    private String httpbin = "http://httpbin.org:80";

    public String getHttpbin() {
        return httpbin;
    }

    public void setHttpbin(String httpbin) {
        this.httpbin = httpbin;
    }

}
