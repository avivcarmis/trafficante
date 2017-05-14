package io.github.avivcarmis.trafficante.adapters.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.util.LinkedList;
import java.util.List;

/**
 * Registers {@link QueryStringResolver} to process instead of
 * {@link RequestResponseBodyMethodProcessor}
 */
@Component
@DependsOn("QueryStringResolver")
public class QueryStringResolverPostProcessor implements BeanPostProcessor {

    // Fields

    private final QueryStringResolver _resolver;

    // Constructors

    @Autowired
    public QueryStringResolverPostProcessor(QueryStringResolver resolver) {
        _resolver = resolver;
    }

    // Public

    @Override
    public Object postProcessAfterInitialization(Object bean, String arg1) throws BeansException {
        if (bean instanceof RequestMappingHandlerAdapter) {
            RequestMappingHandlerAdapter adapter = (RequestMappingHandlerAdapter) bean;
            List<HandlerMethodArgumentResolver> resolvers = new LinkedList<>(adapter.getArgumentResolvers());
            int index = findBodyResolver(resolvers);
            HandlerMethodArgumentResolver bodyResolver = resolvers.remove(index);
            _resolver.setOriginalProcessor((RequestResponseBodyMethodProcessor) bodyResolver);
            resolvers.add(0, _resolver);
            adapter.setArgumentResolvers(resolvers);
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String arg1) throws BeansException {
        return bean;
    }

    // Private

    private int findBodyResolver(List<HandlerMethodArgumentResolver> resolvers) {
        for (int i = 0; i < resolvers.size(); i++) {
            if (resolvers.get(i) instanceof RequestResponseBodyMethodProcessor) {
                return i;
            }
        }
        throw new RuntimeException("not found RequestResponseBodyMethodProcessor");
    }

}