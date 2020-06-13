package org.springframework.cloud.gateway.route;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.event.FilterArgsEvent;
import org.springframework.cloud.gateway.event.PredicateArgsEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.cloud.gateway.support.HasRouteId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.Validator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Copy 'org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator'.
 * And Extends it to support Multi Tenancy.
 */
public class MultiTenancyRouteDefinitionRouteLocator implements RouteLocator {

    /**
     * Default filters name.
     */
    public static final String DEFAULT_FILTERS = "defaultFilters";

    protected final Log logger = LogFactory.getLog(getClass());

    private final MultiTenancyRouteDefinitionRepository routeDefinitionLocator;

    private final ConfigurationService configurationService;

    private final Map<String, RoutePredicateFactory> predicates = new LinkedHashMap<>();

    private final Map<String, GatewayFilterFactory> gatewayFilterFactories = new HashMap<>();

    private final GatewayProperties gatewayProperties;

    public MultiTenancyRouteDefinitionRouteLocator(RouteDefinitionLocator routeDefinitionLocator,
                                       List<RoutePredicateFactory> predicates,
                                       List<GatewayFilterFactory> gatewayFilterFactories,
                                       GatewayProperties gatewayProperties,
                                       ConfigurationService configurationService) {
        if (!(routeDefinitionLocator instanceof MultiTenancyRouteDefinitionRepository)) {
            throw new IllegalArgumentException("routeDefinitionLocator is not MultiTenancyRouteDefinitionRouteLocator");
        }
        this.routeDefinitionLocator = (MultiTenancyRouteDefinitionRepository) routeDefinitionLocator;
        this.configurationService = configurationService;
        initFactories(predicates);
        gatewayFilterFactories.forEach(
                factory -> this.gatewayFilterFactories.put(factory.name(), factory));
        this.gatewayProperties = gatewayProperties;
    }

    private void initFactories(List<RoutePredicateFactory> predicates) {
        predicates.forEach(factory -> {
            String key = factory.name();
            if (this.predicates.containsKey(key)) {
                this.logger.warn("A RoutePredicateFactory named " + key
                        + " already exists, class: " + this.predicates.get(key)
                        + ". It will be overwritten.");
            }
            this.predicates.put(key, factory);
            if (logger.isInfoEnabled()) {
                logger.info("Loaded RoutePredicateFactory [" + key + "]");
            }
        });
    }

    @Override
    public Flux<Route> getRoutes() {
        return getRoutes(this.routeDefinitionLocator.getRouteDefinitions());
    }

    /**
     * for Multi Tenancy
     *
     * @param tenantId
     * @return
     */
    public Flux<Route> getRoutes(String tenantId) {
        return getRoutes(this.routeDefinitionLocator.getRouteDefinitions(tenantId));
    }

    /**
     * for Multi Tenancy
     *
     * @param routeId
     * @return
     */
    public Mono<Route> getRoute(String routeId) {
        return getRoutes(this.routeDefinitionLocator.getRouteDefinition(routeId).flux()).next();
    }

    private Flux<Route> getRoutes(Flux<RouteDefinition> routeDefinitionFlux) {
        return routeDefinitionFlux
                .map(this::convertToRoute)
                .map(route -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("RouteDefinition matched: " + route.getId());
                    }
                    return route;
                });
    }

    private Route convertToRoute(RouteDefinition routeDefinition) {
        AsyncPredicate<ServerWebExchange> predicate = combinePredicates(routeDefinition);
        List<GatewayFilter> gatewayFilters = getFilters(routeDefinition);

        return Route.async(routeDefinition).asyncPredicate(predicate)
                .replaceFilters(gatewayFilters).build();
    }

    @SuppressWarnings("unchecked")
    List<GatewayFilter> loadGatewayFilters(String id,
                                           List<FilterDefinition> filterDefinitions) {
        ArrayList<GatewayFilter> ordered = new ArrayList<>(filterDefinitions.size());
        for (int i = 0; i < filterDefinitions.size(); i++) {
            FilterDefinition definition = filterDefinitions.get(i);
            GatewayFilterFactory factory = this.gatewayFilterFactories
                    .get(definition.getName());
            if (factory == null) {
                throw new IllegalArgumentException(
                        "Unable to find GatewayFilterFactory with name "
                                + definition.getName());
            }
            if (logger.isDebugEnabled()) {
                logger.debug("RouteDefinition " + id + " applying filter "
                        + definition.getArgs() + " to " + definition.getName());
            }

            // @formatter:off
            Object configuration = this.configurationService.with(factory)
                    .name(definition.getName())
                    .properties(definition.getArgs())
                    .eventFunction((bound, properties) -> new FilterArgsEvent(
                            // TODO: why explicit cast needed or java compile fails
                            MultiTenancyRouteDefinitionRouteLocator.this, id, (Map<String, Object>) properties))
                    .bind();
            // @formatter:on

            // some filters require routeId
            // TODO: is there a better place to apply this?
            if (configuration instanceof HasRouteId) {
                HasRouteId hasRouteId = (HasRouteId) configuration;
                hasRouteId.setRouteId(id);
            }

            GatewayFilter gatewayFilter = factory.apply(configuration);
            if (gatewayFilter instanceof Ordered) {
                ordered.add(gatewayFilter);
            }
            else {
                ordered.add(new OrderedGatewayFilter(gatewayFilter, i + 1));
            }
        }

        return ordered;
    }

    private List<GatewayFilter> getFilters(RouteDefinition routeDefinition) {
        List<GatewayFilter> filters = new ArrayList<>();

        // TODO: support option to apply defaults after route specific filters?
        if (!this.gatewayProperties.getDefaultFilters().isEmpty()) {
            filters.addAll(loadGatewayFilters(DEFAULT_FILTERS,
                    this.gatewayProperties.getDefaultFilters()));
        }

        if (!routeDefinition.getFilters().isEmpty()) {
            filters.addAll(loadGatewayFilters(routeDefinition.getId(),
                    routeDefinition.getFilters()));
        }

        AnnotationAwareOrderComparator.sort(filters);
        return filters;
    }

    private AsyncPredicate<ServerWebExchange> combinePredicates(
            RouteDefinition routeDefinition) {
        List<PredicateDefinition> predicates = routeDefinition.getPredicates();
        AsyncPredicate<ServerWebExchange> predicate = lookup(routeDefinition,
                predicates.get(0));

        for (PredicateDefinition andPredicate : predicates.subList(1,
                predicates.size())) {
            AsyncPredicate<ServerWebExchange> found = lookup(routeDefinition,
                    andPredicate);
            predicate = predicate.and(found);
        }

        return predicate;
    }

    @SuppressWarnings("unchecked")
    private AsyncPredicate<ServerWebExchange> lookup(RouteDefinition route,
                                                     PredicateDefinition predicate) {
        RoutePredicateFactory<Object> factory = this.predicates.get(predicate.getName());
        if (factory == null) {
            throw new IllegalArgumentException(
                    "Unable to find RoutePredicateFactory with name "
                            + predicate.getName());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("RouteDefinition " + route.getId() + " applying "
                    + predicate.getArgs() + " to " + predicate.getName());
        }

        // @formatter:off
        Object config = this.configurationService.with(factory)
                .name(predicate.getName())
                .properties(predicate.getArgs())
                .eventFunction((bound, properties) -> new PredicateArgsEvent(
                        MultiTenancyRouteDefinitionRouteLocator.this, route.getId(), properties))
                .bind();
        // @formatter:on

        return factory.applyAsync(config);
    }
}
