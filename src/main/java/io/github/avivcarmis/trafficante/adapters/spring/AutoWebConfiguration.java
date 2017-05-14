package io.github.avivcarmis.trafficante.adapters.spring;

import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Inherits {@link WebMvcAutoConfiguration} class to bypass
 * ConditionalOnMissingBean(WebMvcConfigurationSupport.class) expression that
 * prevents it from automatically configuring the server due to
 * {@link RequestMappingRegistration} class.
 */
@Configuration
public class AutoWebConfiguration extends WebMvcAutoConfiguration {}
