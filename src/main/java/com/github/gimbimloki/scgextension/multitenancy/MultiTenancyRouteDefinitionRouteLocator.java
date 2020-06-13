package com.github.gimbimloki.scgextension.multitenancy;

import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Multi Tenancy version of 'org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator'
 *
 * @see org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator
 */
public class MultiTenancyRouteDefinitionRouteLocator extends RouteDefinitionRouteLocator {
    public MultiTenancyRouteDefinitionRouteLocator(RouteDefinitionLocator routeDefinitionLocator, List<RoutePredicateFactory> predicates, List<GatewayFilterFactory> gatewayFilterFactories, GatewayProperties gatewayProperties, ConversionService conversionService) {
        super(routeDefinitionLocator, predicates, gatewayFilterFactories, gatewayProperties, conversionService);
    }

    public MultiTenancyRouteDefinitionRouteLocator(RouteDefinitionLocator routeDefinitionLocator, List<RoutePredicateFactory> predicates, List<GatewayFilterFactory> gatewayFilterFactories, GatewayProperties gatewayProperties, ConfigurationService configurationService) {
        super(routeDefinitionLocator, predicates, gatewayFilterFactories, gatewayProperties, configurationService);

    }

    protected Mono<Route> lookupRoute(ServerWebExchange serverWebExchange) {
        Flux<Route> routes = null;
        routes
                .concatMap(route -> Mono.just(route).filterWhen(r -> r.getPredicate().apply(serverWebExchange)));
        return null;
    }
}
