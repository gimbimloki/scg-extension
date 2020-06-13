package org.springframework.cloud.gateway.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public final class RouteDefinitionLoader {
    private static final String ROUTES = "routes";
    private static final String ID = "id";

    private RouteDefinitionLoader() {
        // Do Nothing
    }

    public static Collection<RouteDefinition> loadRouteDefinitions(InputStream inputStream) {
        final Routes routes = loadRoutes(inputStream);
        if (Objects.nonNull(routes) && !CollectionUtils.isEmpty(routes.getRoutes())) {
            return routes
                    .getRoutes()
                    .stream()
                    .map(it -> {
                        final RouteDefinition routeDefinition = new RouteDefinition();
                        routeDefinition.setId(it.getId());
                        try {
                            routeDefinition.setUri(new URI(it.getUri()));
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                        routeDefinition.setFilters(createFilterDefinitions(it.getFilters()));
                        routeDefinition.setPredicates(createPredicateDefinition(it.getPredicates()));
                        if (Objects.nonNull(it.getMetadata())) {
                            routeDefinition.setMetadata(it.getMetadata());
                        }
                        routeDefinition.setOrder(it.getOrder());

                        return routeDefinition;
                    })
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    protected static  List<FilterDefinition> createFilterDefinitions(Collection<String> filters) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }

        return filters
                .stream()
                .map(it -> new FilterDefinition(it))
                .collect(Collectors.toList());
    }

    protected static List<PredicateDefinition> createPredicateDefinition(Collection<String> predicates) {
        if (CollectionUtils.isEmpty(predicates)) {
            return Collections.emptyList();
        }

        return predicates
                .stream()
                .map(it -> new PredicateDefinition(it))
                .collect(Collectors.toList());
    }

    protected static Routes loadRoutes(InputStream inputStream) {
        final Yaml yaml = new Yaml(new Constructor(Routes.class));
        final Routes routes  = yaml.load(inputStream);

        log.info("routes: {}", routes);

        return routes;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    @Builder
    public static class Routes {
        private Collection<Route> routes;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    @Builder
    public static class Route {
        private String id;
        private String uri;
        private Collection<String> filters;
        private Collection<String> predicates;
        private Map<String, Object> metadata;
        private int order;
    }
}
