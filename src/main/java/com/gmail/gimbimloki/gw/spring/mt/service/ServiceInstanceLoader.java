package com.gmail.gimbimloki.gw.spring.mt.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

@Slf4j
public final class ServiceInstanceLoader {
    private ServiceInstanceLoader() {
        // Do Nothing
    }

    public static MultiValueMap<String, ServiceInstance> loadServices(InputStream inputStream) {
        final MultiValueMap<String, ServiceInstance> services = new LinkedMultiValueMap<>();

        loadServicesFromInputStream(inputStream)
                .getServices()
                .stream()
                .forEach(service -> {
                    try {
                        services.add(service.getServiceId(), MultiTenancyServiceInstance
                                .builder()
                                .serviceId(service.getServiceId())
                                .host(service.getHost())
                                .port(service.getPort())
                                .isSecure(service.getIsSecure())
                                .scheme(service.getScheme())
                                .metadata(service.getMetadata())
                                .uri(StringUtils.isEmpty(service.getUri()) ? null : new URI(service.getUri()))
                                .build());
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });


        return services;
    }

    protected static Services loadServicesFromInputStream(InputStream inputStream) {
        final Yaml yaml = new Yaml(new Constructor(Services.class));
        final Services services = yaml.load(inputStream);

        log.info("services: {}", services);

        return  services;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    @Builder
    public static class Services {
        private Collection<Service> services;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    @Builder
    public static class Service {
        private String serviceId;
        private String instanceId;
        private String host;
        @Builder.Default
        private Integer port = 80;
        @Builder.Default
        private Boolean isSecure = false;
        @Builder.Default
        private String scheme = "http";
        private String uri;
        private Map<String, String> metadata;

    }
}
