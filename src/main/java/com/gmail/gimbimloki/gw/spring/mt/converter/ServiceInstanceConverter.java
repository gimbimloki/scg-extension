package com.gmail.gimbimloki.gw.spring.mt.converter;

import com.gmail.gimbimloki.gw.DefaultGwConfigProperties;
import com.gmail.gimbimloki.gw.MapStructConfiguration;
import com.gmail.gimbimloki.gw.spring.mt.service.MultiTenancyServiceInstance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.URI;
import java.net.URISyntaxException;

@Mapper(config = MapStructConfiguration.class)
public interface ServiceInstanceConverter {
    default URI toURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    MultiTenancyServiceInstance toMultiTenancyServiceInstance(DefaultGwConfigProperties.ServiceConfig serviceConfig);
}
