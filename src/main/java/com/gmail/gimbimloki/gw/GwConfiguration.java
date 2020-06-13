package com.gmail.gimbimloki.gw;

import com.gmail.gimbimloki.gw.spring.mt.converter.RouteDefinitionConverter;
import com.gmail.gimbimloki.gw.spring.mt.converter.ServiceInstanceConverter;
import org.springframework.cloud.gateway.route.MultiTenancyRouteDefinitionRepository;
import com.gmail.gimbimloki.gw.spring.mt.service.MultiTenancyDiscoveryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;

@Slf4j
@EnableConfigurationProperties
@EnableDiscoveryClient
@Configuration
public class GwConfiguration {
//    @Bean
//    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
//        return builder.routes()
////                .route(p -> p
////                        .path("/get")
////                        .filters(f -> f.addRequestHeader("Hello", "World"))
////                        .uri("http://httpbin.org:80"))
//                .route(p -> p
//                        .host("127.0.0.1")
//                        .filters(f -> f.addRequestHeader("localhost", "localhost"))
//                        .uri("http://httpbin.org:80"))
//                .route(p -> p
//                        .path("/get")
//                        .filters(f -> f.addRequestHeader("Second", "Route"))
//                        .uri("http://httpbin.org:80"))
//                .build();
//    }
    @Bean
    MultiTenancyDiscoveryClient discoveryClient(ServiceInstanceConverter serviceInstanceConverter) throws IOException {
        MultiTenancyDiscoveryClient discoveryClient = new MultiTenancyDiscoveryClient(serviceInstanceConverter, null);

//        discoveryClient.loadFromInputStream(new FileInputStream("src/main/resources/services.yml"));

        return discoveryClient;
    }

//    @Bean
//    DiscoveryClientRouteDefinitionLocator discoveryClientRouteDefinitionLocator(MultiTenancyDiscoveryClient discoveryClient) {
//        return new DiscoveryClientRouteDefinitionLocator(discoveryClient, new DiscoveryLocatorProperties());
//    }

    @Bean
    RouteDefinitionRepository routeDefinitionRepository(RouteDefinitionConverter routeDefinitionConverter, DefaultGwConfigProperties defaultGwConfigProperties) throws IOException {
        MultiTenancyRouteDefinitionRepository routeDefinitionRepository = new MultiTenancyRouteDefinitionRepository(routeDefinitionConverter, defaultGwConfigProperties);
        return routeDefinitionRepository;
    }

    @Bean
    @Order(Integer.MAX_VALUE)
    public GlobalFilter globalFilter() {
        return (exchange, chain) -> {
            log.info("path -> {}", exchange.getRequest().getPath().value());
            return chain.filter(exchange);
        };
    }
}
