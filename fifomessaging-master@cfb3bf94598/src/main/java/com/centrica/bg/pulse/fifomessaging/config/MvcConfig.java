package com.centrica.bg.pulse.fifomessaging.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * This class is needed to turn on default servlet handling for Swagger content
 * which cannot be served using Jersey Servlet.
 */
@Configuration
public class MvcConfig extends WebMvcConfigurerAdapter {
    private final static Log log = LogFactory.getLog(MvcConfig.class);

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
        log.debug("Enable default servlet handler");
    }
}