package com.launchdarkly.developer.LDSampleApp;

import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Value;

import com.launchdarkly.openfeature.serverprovider.Provider;
import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.LDConfig;

import dev.openfeature.sdk.exceptions.OpenFeatureError;
import dev.openfeature.sdk.ImmutableStructure;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Configuration
public class OpenFeatureBeans {

    @org.springframework.beans.factory.annotation.Value("${ld.sdkkey}")
    private String sdkKey;

    @Bean()
    public OpenFeatureAPI OpenFeatureAPI() {
        final OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
        try {
            LDConfig config = new LDConfig.Builder()
                    .events(Components.sendEvents().capacity(5000).flushInterval(Duration.ofSeconds(2)))
                    .build();
            openFeatureAPI.setProviderAndWait(new Provider(sdkKey, config));
        } catch (OpenFeatureError e) {
            throw new RuntimeException("Failed to set OpenFeature provider", e);
        }
        return openFeatureAPI;
    }

    @Bean
    @Scope(scopeName = "request", proxyMode = ScopedProxyMode.INTERFACES)
    public Supplier<ImmutableStructure> serverUserContext(final HttpServletRequest request) {
        return () -> Optional.ofNullable(request.getParameter("userKey"))
                .map(userKey -> new ImmutableStructure(new HashMap<>() {
                    {
                        put("key", new Value(userKey));
                        put("email", new Value(request.getParameter("email")));
                        put("plan", new Value(request.getParameter("plan")));
                    }
                }))
                .orElseGet(() -> new ImmutableStructure(new HashMap<>() {
                    {
                        put("key", new Value(UUID.randomUUID().toString()));
                        put("anonymous", new Value(true));
                    }
                }));
    }
}