package org.springframework.cloud.gateway.route;

import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.stream.Collectors;

public class MultiTenancyCompositeRouteLocator extends CompositeRouteLocator {
    private MultiTenancyRouteDefinitionRouteLocator routeDefinitionRouteLocator;

    public MultiTenancyCompositeRouteLocator(Flux<RouteLocator> delegates) {
        super(delegates);

        routeDefinitionRouteLocator = (MultiTenancyRouteDefinitionRouteLocator) delegates.collect(Collectors.toList())
                .block()
                .stream()
                .filter(it -> it instanceof MultiTenancyRouteDefinitionRouteLocator)
                .findFirst()
                .orElseGet(null);
    }

    public Flux<Route> getRoutes(String routeId) {
        if (Objects.nonNull(routeDefinitionRouteLocator)) {
//            routeDefinitionRouteLocator.lookupRoute(routeId);
        }

        return getRoutes();
    }
}
