package com.github.gimbimloki.scgextension.multitenancy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Multi Tenancy version of 'org.springframework.cloud.gateway.route.CachingRouteLocator'
 */
public class MultiTenancyCachingRouteLocator implements Ordered, RouteLocator, ApplicationListener<RefreshRoutesEvent> {
    private static final String CACHE_KEY = "routes";

    private final RouteLocator delegate;

    private final Flux<Route> routes;

    private final ConcurrentHashMap<String, List> cache = new ConcurrentHashMap<>();

    private final Cache<String, Flux<Route>> routesCache;

    public MultiTenancyCachingRouteLocator(RouteLocator delegate) {
        this.delegate = delegate;
        routes = CacheFlux.lookup(cache, CACHE_KEY, Route.class)
                .onCacheMissResume(this::fetch);

        routesCache = CacheBuilder
                .newBuilder()
                .build(new CacheLoader<String, Flux<Route>>() {
                    @Override
                    public Flux<Route> load(String multiTenantId) throws Exception {
                        return fetch()
                                .filter(route -> route
                                        .getMetadata()
                                        .get(MultiTenancyAttributes.ROUTE_META_DATA_MULTI_TENANT_ID)
                                        .equals(multiTenantId));
                    }
                });
    }

    private Flux<Route> fetch() {
        return this.delegate.getRoutes().sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    @Override
    public Flux<Route> getRoutes() {
        return this.routes;
    }

    /**
     * called by MultiTenancyRouteDefinitionRouteLocator
     * @param exchange
     * @return
     */
    public Flux<Route> getRoutes(ServerWebExchange exchange) {
        final List<String> hosts = exchange.getRequest().getHeaders().get("host");
        if (CollectionUtils.isEmpty(hosts)) {
            return Flux.empty();
        }

        return Flux.fromIterable(hosts)
                .flatMap(host -> routesCache.getIfPresent(host));
    }

    /**
     * Clears the routes cache.
     * @return routes flux
     */
    public Flux<Route> refresh() {
        this.cache.clear();
        return this.routes;
    }

    @Override
    public void onApplicationEvent(RefreshRoutesEvent event) {
        fetch().materialize().collect(Collectors.toList())
                .doOnNext(routes -> cache.put(CACHE_KEY, routes)).subscribe();
    }

    @Deprecated
        /* for testing */ void handleRefresh() {
        refresh();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
