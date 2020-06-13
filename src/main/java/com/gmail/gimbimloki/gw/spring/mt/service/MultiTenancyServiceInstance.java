package com.gmail.gimbimloki.gw.spring.mt.service;

import lombok.*;
import org.springframework.cloud.client.serviceregistry.Registration;

import java.net.URI;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class MultiTenancyServiceInstance implements Registration {
    private String instanceId;
    private String serviceId;
    private String host;
    private int port;
    private boolean isSecure;
    private String scheme;
    private URI uri;
    private Map<String, String> metadata;

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String getScheme() {
        return scheme;
    }
}
