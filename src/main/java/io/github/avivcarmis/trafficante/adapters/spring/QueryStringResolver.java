package io.github.avivcarmis.trafficante.adapters.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.avivcarmis.trafficante.core.BasicEndpoint;
import io.github.avivcarmis.trafficante.core.HttpMethodContentClass;
import io.github.avivcarmis.trafficante.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles parsing of query string endpoint requests.
 * Overrides {@link RequestResponseBodyMethodProcessor} to check if request
 * is of type query string - if so, parses the query string,
 * if not, let {@link RequestResponseBodyMethodProcessor} handle.
 */
@Component("QueryStringResolver")
public class QueryStringResolver implements HandlerMethodArgumentResolver {

    // Fields

    private final ApplicationContext _applicationContext;

    private final ObjectMapper _objectMapper;

    private RequestResponseBodyMethodProcessor _originalProcessor;

    // Constructors

    @Autowired
    public QueryStringResolver(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        _applicationContext = applicationContext;
        _objectMapper = objectMapper;
    }

    // Public

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return BasicEndpoint.class.isAssignableFrom(parameter.getMethod().getDeclaringClass()) &&
                parameter.getMethod().getName().equals("doAPICall");
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        Class<?> aClass = parameter.getContainingClass();
        //noinspection unchecked
        BasicEndpoint endpoint = _applicationContext.getBean((Class<? extends BasicEndpoint>) aClass);
        if (HttpMethodContentClass.classify(endpoint.getHttpMethod()) != HttpMethodContentClass.QUERY_STRING) {
            return _originalProcessor.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        }
        Class<?> reqClass = parameter.getParameterType();
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : webRequest.getParameterMap().entrySet()) {
            String[] value = entry.getValue();
            if (value != null && value.length > 0) {
                params.put(entry.getKey(), value[0]);
            }
        }
        String mapAsJson;
        try {
            mapAsJson = _objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("could not map object to string", e);
        }
        try {
            return _objectMapper.readValue(mapAsJson, reqClass);
        } catch (InvalidFormatException e) {
            String message = e.getMessage();
            if (e.getPath().size() > 0) {
                String fieldName = _objectMapper.getPropertyNamingStrategy().nameForField(null, null,
                        e.getPath().get(0).getFieldName());
                message = "field `" + fieldName + "` must be of type " + e.getTargetType().getSimpleName();
            }
            throw new BadRequestException(message);
        } catch (IOException e) {
            if (e.getMessage().contains("Can not construct")) {
                throw new RuntimeException("can not construct an instance of " + reqClass.getName());
            }
            throw new BadRequestException(e.getMessage());
        }
    }

    // Private

    void setOriginalProcessor(RequestResponseBodyMethodProcessor originalProcessor) {
        this._originalProcessor = originalProcessor;
    }

}
