package org.springframework.cloud.gateway.route;

import com.gmail.gimbimloki.gw.spring.mt.MultiTenants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MultiTenancyCachingRouteLocator implements Ordered, RouteLocator, ApplicationListener<RefreshRoutesEvent> {
    private final MultiTenancyCompositeRouteLocator delegate;

    /**
     * for Multi Tenancy
     *
     * RouteID(TenantID#RouteID), Route
     */
    private final Map<String, Flux<Route>> routesCache;

    public MultiTenancyCachingRouteLocator(RouteLocator delegate) {
        if (!(delegate instanceof MultiTenancyCompositeRouteLocator)) {
            throw new IllegalArgumentException("delegate is not MultiTenancyCompositeRouteLocator");
        }

        this.delegate = (MultiTenancyCompositeRouteLocator) delegate;

        this.routesCache = new HashMap<>();
    }

    private Flux<Route> fetch() {
        if (routesCache.isEmpty()) {
            delegate
                .getRoutes()
                .doOnNext(route -> routesCache.put(route.getId(), Flux.just(route)));
        }


        return Flux.fromStream(
                    routesCache.values()
                        .stream()
                        .flatMap(it -> it.toStream()))
                .sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    private Flux<Route> fetch(String routeId) {
        routesCache.computeIfAbsent(routeId, delegate::getRoutes);

        return routesCache.get(routeId)
                .sort(AnnotationAwareOrderComparator.INSTANCE);
    }


    @Override
    public Flux<Route> getRoutes() {
        return Flux.just(routesCache)
                .flatMap(it -> Flux.concat(it.values()));
    }

    /**
     * for Multi Tenancy
     *
     * @param exchange
     * @return
     */
    public Flux<Route> getRoutes(ServerWebExchange exchange) {
        final String tenantId = exchange.getAttribute(MultiTenants.TENANT_ID_ATTR);
        if (StringUtils.isEmpty(tenantId)) {
            log.warn("Tenant ID is not found in ServerWebExchange Attributes");

            return Flux.empty();
        }

        return Flux.just(tenantId)
                .flatMap(it -> routesCache.getOrDefault(it, Flux.empty()));
    }

    @Override
    public void onApplicationEvent(RefreshRoutesEvent event) {
        if (event instanceof MultiTenancyRefreshRoutesEvent) {
            final String routeId = ((MultiTenancyRefreshRoutesEvent) event).getRouteId();

            fetch(routeId);
        } else {
            routesCache.clear();
            fetch();
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
