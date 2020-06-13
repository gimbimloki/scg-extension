package org.springframework.cloud.gateway.route;

import com.gmail.gimbimloki.gw.DefaultGwConfigProperties;
import com.gmail.gimbimloki.gw.spring.mt.MultiTenants;
import com.gmail.gimbimloki.gw.spring.mt.converter.RouteDefinitionConverter;
import com.gmail.gimbimloki.gw.spring.mt.util.StackTraces;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MultiTenancyRouteDefinitionRepository implements RouteDefinitionRepository {
    /**
     * Tenant ID and Route Definitions
     */
    private MultiValueMap<String, RouteDefinition> routeDefinitions;

    @Autowired
    public MultiTenancyRouteDefinitionRepository(RouteDefinitionConverter routeDefinitionConverter,
                                                 DefaultGwConfigProperties defaultGwConfigProperties) {
        routeDefinitions = new LinkedMultiValueMap<>();
        if (!CollectionUtils.isEmpty(defaultGwConfigProperties.getRouteConfigs())) {
            for (DefaultGwConfigProperties.RouteConfig routeConfig : defaultGwConfigProperties.getRouteConfigs()) {
                final RouteDefinition routeDefinition = routeDefinitionConverter.toRouteDefinition(routeConfig);
                routeDefinitions.add(MultiTenants.getTenantId(routeDefinition), routeDefinition);
            }
        }
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        log.warn("{}", StackTraces.getStackTrancesString());

        return Flux.fromIterable(routeDefinitions
                .values()
                .stream()
                .flatMap(it -> it.stream())
                .collect(Collectors.toList()));
    }

    public Flux<RouteDefinition> getRouteDefinitions(String tenantId) {
        log.debug("{}", StackTraces.getStackTrancesString());

        if (!routeDefinitions.containsKey(tenantId)) {
            throw new NoSuchElementException(tenantId);
        }

        return Flux.fromStream(routeDefinitions.get(tenantId).stream());
    }

    public Mono<RouteDefinition> getRouteDefinition(String routeId) {
        log.debug("{}", StackTraces.getStackTrancesString());

        return Mono.just(routeId)
                .map(it -> {
                    final String tenantId = MultiTenants.getTenantId(routeId);
                    if (!routeDefinitions.containsKey(tenantId)) {
                        throw new NoSuchElementException(tenantId);
                    }

                    final RouteDefinition savedRouteDefinition =  routeDefinitions.get(tenantId)
                        .stream()
                        .filter(routeDefinition -> routeDefinition.getId().equals(routeId))
                        .findFirst()
                        .get();
                    if (Objects.isNull(savedRouteDefinition)) {
                        throw new NoSuchElementException(tenantId);
                    }

                    return savedRouteDefinition;
                });
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route
                .doOnNext(newRouteDefinition -> {
                    final String tenantId = MultiTenants.getTenantId(newRouteDefinition);

                    RouteDefinition oldRouteDefinition = null;
                    if (routeDefinitions.containsKey(tenantId)) {
                        oldRouteDefinition = routeDefinitions.get(tenantId)
                            .stream()
                            .filter(routeDefinition -> routeDefinition.getId().equals(newRouteDefinition.getId()))
                            .findFirst()
                            .get();

                        int index = 0;
                        for (RouteDefinition routeDefinition : routeDefinitions.get(tenantId)) {
                            if (routeDefinition.getId().equals(newRouteDefinition.getId())) {
                                break;
                            }

                            index++;
                        }

                        if (index < routeDefinitions.size()) {
                            routeDefinitions.get(tenantId).remove(index);
                        }
                    }

                    routeDefinitions.add(tenantId, newRouteDefinition);

                    log.debug("tenantId: {}, old: {}, new: {}, is added", tenantId, oldRouteDefinition, newRouteDefinition);
                })
                .then();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeIdMono) {
        return routeIdMono
                .doOnNext(routeId -> {
                    final String tenantId = MultiTenants.getTenantId(routeId);
                    int index = 0;
                    for (RouteDefinition routeDefinition : routeDefinitions.get(tenantId)) {
                        if (routeDefinition.getId().equals(routeId)) {
                            break;
                        }

                        index++;
                    }

                    if (index < routeDefinitions.size()) {
                        routeDefinitions.get(tenantId).remove(index);

                        log.debug("routeId: {}, index: {}, is deleted", routeId, index);
                    }
                })
                .then();
    }
}
