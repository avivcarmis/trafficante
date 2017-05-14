package io.github.avivcarmis.trafficante.core;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Classifies {@link RequestMethod} and {@link HttpMethod} by the
 * method of delivering request body.
 */
public enum HttpMethodContentClass {

    // Values

    /**
     * Http methods which deliver request body by using query strings.
     */
    QUERY_STRING(
            RequestMethod.GET,
            RequestMethod.HEAD,
            RequestMethod.PATCH,
            RequestMethod.DELETE,
            RequestMethod.OPTIONS,
            RequestMethod.TRACE
    ),

    /**
     * Http methods which deliver request body by using body payload.
     */
    PAYLOAD(
            RequestMethod.POST,
            RequestMethod.PUT
    );

    // Fields

    private final Set<RequestMethod> _supportedMethods;

    // Constructors

    HttpMethodContentClass(RequestMethod... supportedMethods) {
        _supportedMethods = new HashSet<>(Arrays.asList(supportedMethods));
    }

    // Static

    private static final Map<HttpMethod, RequestMethod> METHOD_MAPPER = ImmutableMap.<HttpMethod, RequestMethod>builder()
            .put(HttpMethod.GET, RequestMethod.GET)
            .put(HttpMethod.HEAD, RequestMethod.HEAD)
            .put(HttpMethod.POST, RequestMethod.POST)
            .put(HttpMethod.PUT, RequestMethod.PUT)
            .put(HttpMethod.PATCH, RequestMethod.PATCH)
            .put(HttpMethod.DELETE, RequestMethod.DELETE)
            .put(HttpMethod.OPTIONS, RequestMethod.OPTIONS)
            .put(HttpMethod.TRACE, RequestMethod.TRACE)
            .build();

    public static HttpMethodContentClass classify(RequestMethod requestMethod) {
        for (HttpMethodContentClass contentClass : values()) {
            if (contentClass._supportedMethods.contains(requestMethod)) {
                return contentClass;
            }
        }
        return QUERY_STRING;
    }

    public static HttpMethodContentClass classify(HttpMethod httpMethod) {
        return classify(METHOD_MAPPER.get(httpMethod));
    }

}
