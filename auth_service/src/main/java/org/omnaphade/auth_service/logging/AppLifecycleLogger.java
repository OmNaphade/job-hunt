package org.omnaphade.auth_service.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppLifecycleLogger {

    private static final Logger log = LoggerFactory.getLogger(AppLifecycleLogger.class);

    @EventListener
    public void onStart(ApplicationReadyEvent event) {
        log.info("✅ Application started successfully");
    }

    @EventListener
    public void onShutdown(ContextClosedEvent event) {
        log.info("🛑 Application is shutting down");
    }

    @EventListener
    public void onFailure(ApplicationFailedEvent event) {
        log.error("❌ Application failed to start", event.getException());
    }
}
