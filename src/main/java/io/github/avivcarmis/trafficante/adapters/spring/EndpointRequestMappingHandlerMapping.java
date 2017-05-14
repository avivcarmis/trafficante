package io.github.avivcarmis.trafficante.adapters.spring;

import com.google.common.collect.ImmutableSet;
import io.github.avivcarmis.trafficante.core.BasicEndpoint;
import io.github.avivcarmis.trafficante.core.Trafficante;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import springfox.documentation.swagger.web.ApiResourceController;
import springfox.documentation.swagger2.web.Swagger2Controller;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Identifies and maps endpoint classes to a {@link RequestMappingInfo}.
 */
public class EndpointRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    // Constants

    private static final Set<Class> SWAGGER_CLASSES = ImmutableSet.of(
            ApiResourceController.class,
            Swagger2Controller.class
    );

    // Fields

    private final ApplicationContext _applicationContext;

    // Constructors

    EndpointRequestMappingHandlerMapping(ApplicationContext applicationContext) {
        _applicationContext = applicationContext;
    }

    // Private

    @Override
    protected boolean isHandler(Class<?> beanType) {
        if (super.isHandler(beanType)) {
            if (Trafficante.getSettings().isSwaggerEnabled()) {
                return true;
            }
            if (SWAGGER_CLASSES.contains(beanType)) {
                return false;
            }
        }
        return BasicEndpoint.class.isAssignableFrom(beanType);
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        if (!BasicEndpoint.class.isAssignableFrom(handlerType)) {
            return super.getMappingForMethod(method, handlerType);
        }
        if (!method.getName().equals("doAPICall")) {
            return null;
        }
        //noinspection unchecked
        BasicEndpoint instance = _applicationContext.getAutowireCapableBeanFactory().createBean((Class<BasicEndpoint>) handlerType);
        String path = instance.defaultPathProvider();
        return new RequestMappingInfo(
                null,
                new PatternsRequestCondition(path.charAt(0) == '/' ? path : '/' + path),
                new RequestMethodsRequestCondition(instance.getHttpMethod()),
                instance.defaultParamsRequestConditionProvider(),
                instance.defaultHeadersRequestConditionProvider(),
                instance.defaultConsumesRequestConditionProvider(),
                instance.defaultProducesRequestConditionProvider(),
                instance.defaultConsumesRequestConditionProvider()
        );
    }

}