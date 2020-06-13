package org.springframework.cloud.gateway.route;

import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.core.env.Environment;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class MultiTenancyRoutePredicateHandlerMapping extends RoutePredicateHandlerMapping {
    private MultiTenancyCachingRouteLocator multiTenancyCachingRouteLocator;

    public MultiTenancyRoutePredicateHandlerMapping(FilteringWebHandler webHandler, RouteLocator routeLocator, GlobalCorsProperties globalCorsProperties, Environment environment) {
        super(webHandler, routeLocator, globalCorsProperties, environment);

        if (routeLocator instanceof MultiTenancyCachingRouteLocator) {
            this.multiTenancyCachingRouteLocator = (MultiTenancyCachingRouteLocator) routeLocator;
        } else {
            this.multiTenancyCachingRouteLocator = null;
        }
    }

    @Override
    protected Mono<Route> lookupRoute(ServerWebExchange exchange) {
        final Mono<Route> routeMono = lookupRouteByTenantId(exchange);
        if (Objects.nonNull(routeMono)) {
            return routeMono;
        }

        return super.lookupRoute(exchange);
    }

    protected Mono<Route> lookupRouteByTenantId(ServerWebExchange exchange) {
        if (Objects.isNull(multiTenancyCachingRouteLocator)) {
            return null;
        }

        return multiTenancyCachingRouteLocator.getRoutes(exchange).next();
    }
}
