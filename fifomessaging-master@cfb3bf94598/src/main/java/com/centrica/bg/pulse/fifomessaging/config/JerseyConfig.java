package com.centrica.bg.pulse.fifomessaging.config;

import com.centrica.bg.pulse.fifomessaging.constant.Constants;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.jersey.listing.ApiListingResourceJSON;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.server.wadl.internal.WadlResource;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;

/**
 * The application path is required so Jersey Servlet handles requests only for the given context
 */
@Configuration
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    private final static Log log = LogFactory.getLog(JerseyConfig.class);


    public JerseyConfig() {
        registerEndpoints();
        configureSwaggerJaxrs();
    }

    private void registerEndpoints() {
        register(RequestContextFilter.class);
        register(WadlResource.class);
        // Package scanner for endpoints
        packages(true, Constants.REST_PACKAGE);
        log.debug("Registered REST endpoints");
    }

    private void configureSwaggerJaxrs() {
        register(ApiListingResourceJSON.class);
        register(SwaggerSerializers.class);
        log.debug("Configure Swagger.io");
    }


}