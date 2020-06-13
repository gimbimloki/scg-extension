package com.gmail.gimbimloki.gw.spring.mt.converter;

import com.gmail.gimbimloki.gw.DefaultGwConfigProperties;
import com.gmail.gimbimloki.gw.MapStructConfiguration;
import org.mapstruct.Mapper;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(config = MapStructConfiguration.class)
public interface RouteDefinitionConverter {
    Collection<RouteDefinition> toRouteDefinitions(Collection<DefaultGwConfigProperties.RouteConfig> routeConfigs);

    default RouteDefinition toRouteDefinition(DefaultGwConfigProperties.RouteConfig routeConfig) {
        final RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(routeConfig.getId());
        try {
            routeDefinition.setUri(new URI(routeConfig.getUri()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        routeDefinition.setFilters(createFilterDefinitions(routeConfig.getFilters()));
        routeDefinition.setPredicates(createPredicateDefinition(routeConfig.getPredicates()));
        if (Objects.nonNull(routeConfig.getMetadata())) {
            routeDefinition.setMetadata(routeConfig.getMetadata());
        }
        routeDefinition.setOrder(routeConfig.getOrder());

        return routeDefinition;
    }

    default List<FilterDefinition> createFilterDefinitions(Collection<String> filters) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }

        return filters
                .stream()
                .map(it -> new FilterDefinition(it))
                .collect(Collectors.toList());
    }

    default List<PredicateDefinition> createPredicateDefinition(Collection<String> predicates) {
        if (CollectionUtils.isEmpty(predicates)) {
            return Collections.emptyList();
        }

        return predicates
                .stream()
                .map(it -> new PredicateDefinition(it))
                .collect(Collectors.toList());
    }
}
