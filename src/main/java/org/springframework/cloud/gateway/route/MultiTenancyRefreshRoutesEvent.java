package org.springframework.cloud.gateway.route;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;

public class MultiTenancyRefreshRoutesEvent extends RefreshRoutesEvent {
    private String routeId;

    public MultiTenancyRefreshRoutesEvent(Object source, String routeId) {
        super(source);

        this.routeId = routeId;
    }

    public String getRouteId() {
        return routeId;
    }
}
