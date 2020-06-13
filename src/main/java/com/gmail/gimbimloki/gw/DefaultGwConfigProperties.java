package com.gmail.gimbimloki.gw;


import com.gmail.gimbimloki.gw.spring.mt.MultiTenants;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;

@Setter
@Getter
@ToString
@Component
@ConfigurationProperties(prefix = "gimbimloki.gw")
public class DefaultGwConfigProperties {
    @NotNull
    private Collection<RouteConfig> routeConfigs;
    @NotNull
    private Collection<ServiceConfig> serviceConfigs;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    @Builder
    public static class RouteConfig {
        private String id;
        private Collection<String> predicates;
        private Collection<String> filters;
        private String uri;
        private Map<String, Object> metadata;
        private int order;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    @Builder
    public static class ServiceConfig {
        private String instanceId;
        private String serviceId;
        private String host;
        private int port;
        private boolean isSecure;
        private String scheme;
        private String uri;
        private Map<String, String> metadata;
    }
}
