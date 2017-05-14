package io.github.avivcarmis.trafficante.adapters.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Registers {@link EndpointRequestMappingHandlerMapping} to map
 * {@link io.github.avivcarmis.trafficante.core.BasicEndpoint} endpoints.
 */
@Configuration
public class RequestMappingRegistration extends DelegatingWebMvcConfiguration {

    // Fields

    private final ApplicationContext _applicationContext;

    // Constructors

    @Autowired
    public RequestMappingRegistration(ApplicationContext applicationContext) {
        _applicationContext = applicationContext;
    }

    // Private

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        return new EndpointRequestMappingHandlerMapping(_applicationContext);
    }

}
