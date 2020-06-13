package com.gmail.gimbimloki.gw.spring.mt.service;

import com.gmail.gimbimloki.gw.DefaultGwConfigProperties;
import com.gmail.gimbimloki.gw.spring.mt.MultiTenants;
import com.gmail.gimbimloki.gw.spring.mt.converter.ServiceInstanceConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class MultiTenancyDiscoveryClient implements ReactiveDiscoveryClient, ServiceRegistry<MultiTenancyServiceInstance> {
    /**
     * Key: TenantID
     * Value: Collection<ServiceInstance>
     */
    private Map<String, MultiValueMap<String, ServiceInstance>> services = new ConcurrentHashMap<>();

    private Collection<ServiceInstance> defaultServiceInstances;

    @Autowired
    public MultiTenancyDiscoveryClient(ServiceInstanceConverter serviceInstanceConverter, Collection<DefaultGwConfigProperties.ServiceConfig> serviceConfigs) {
        if (CollectionUtils.isEmpty(serviceConfigs)) {
            defaultServiceInstances = Collections.emptyList();
        } else {
            defaultServiceInstances = serviceConfigs
                    .stream()
                    .map(it -> serviceInstanceConverter.toMultiTenancyServiceInstance(it))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public String description() {
        return "for Multi Tenancy";
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {
        log.info("serviceId: {}", serviceId);

        return Mono.just(serviceId)
                .flatMap(it -> {
                    final String tenantId = MultiTenants.getTenantId(serviceId);
                    if (services.containsKey(tenantId) && services.get(tenantId).containsKey(serviceId)) {
                        log.debug("tenantId: {}, serviceId: {}, is found", tenantId, serviceId);

                        return Mono.just(services.get(tenantId).get(serviceId));
                    }

                    log.debug("tenantId: {}, serviceId: {}, is NOT found", tenantId, serviceId);

                    return Mono.empty();
                })
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<String> getServices() {
        log.info("called");

        return Flux.fromIterable(services
                .values()
                .stream()
                .flatMap(it -> it.keySet().stream())
                .collect(Collectors.toList()));
    }

    @Override
    public void register(MultiTenancyServiceInstance registration) {
        final String tenantId = MultiTenants.getTenantId(registration.getServiceId());
        final MultiValueMap<String, ServiceInstance> old =  services.putIfAbsent(tenantId, new LinkedMultiValueMap<>());
        if (!CollectionUtils.isEmpty(old)) {
            services.get(tenantId).addAll(old);
        }
        services.get(tenantId).add(registration.getServiceId(), registration);
    }

    @Override
    public void deregister(MultiTenancyServiceInstance registration) {
        final String tenantId = MultiTenants.getTenantId(registration.getServiceId());
        if (services.containsKey(tenantId) && services.get(tenantId).containsKey(registration.getServiceId())) {
            final Collection<ServiceInstance> serviceInstances = services.get(tenantId).get(registration.getServiceId());
            int index = 0;
            for (ServiceInstance serviceInstance : serviceInstances) {
                if (serviceInstance.getInstanceId().equals(registration.getInstanceId())) {
                    break;
                }

                index++;
            }

            if (index < serviceInstances.size()) {
                final boolean isDeleted = serviceInstances.remove(index);

                log.debug("tenantId: {}, serviceId: {}, instanceId: {}, is deleted({})",
                        tenantId, registration.getServiceId(), registration.getInstanceId(), isDeleted);
            } else {
                log.debug("tenantId: {}, serviceId: {}, instanceId: {}, is NOT deleted",
                        tenantId, registration.getServiceId(), registration.getInstanceId());
            }
        }
    }

    @Override
    public void close() {
        log.info("close services: {}", services);
    }

    @Override
    public void setStatus(MultiTenancyServiceInstance registration, String status) {
        // TODO
    }

    @Override
    public <T> T getStatus(MultiTenancyServiceInstance registration) {
        // TODO

        return null;
    }
}
