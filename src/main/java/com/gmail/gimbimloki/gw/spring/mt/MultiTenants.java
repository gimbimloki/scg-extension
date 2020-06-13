package com.gmail.gimbimloki.gw.spring.mt;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.util.StringUtils;

public final class MultiTenants {
    public static final String TENANT_ID_ATTR = "com.gmail.gimbimloki.gw.spring.mt.MultiTenants.TENANT-ID";

    private MultiTenants() {
        // Do Nothing
    }

    public static String getTenantId(RouteDefinition routeDefinition) {
        return getTenantId(routeDefinition.getId());
    }

    public static String getTenantId(Route route) {
        return getTenantId(route.getId());
    }

    public static String getTenantId(ServiceInstance serviceInstance) {
        return getTenantId(serviceInstance.getServiceId());
    }

    /**
     *
     * @param id route-id or service-id
     */
    public static void validateId(String id) {
//        if (StringUtils.isEmpty(id)) {
//            throw new IllegalArgumentException("id is empty");
//        }
//        if (id.indexOf('#') < 1) {
//            throw new IllegalArgumentException("id is invalid, " + id +
//                    " -> TENANT_ID#ROUTE_ID or TENANT_ID#SERVICE_ID");
//        }
    }

    /**
     *
     * @param id route-id or service-id
     * @return
     */
    public static String getTenantId(String id) {
        validateId(id);

        return id.split("#")[0];
    }
}
