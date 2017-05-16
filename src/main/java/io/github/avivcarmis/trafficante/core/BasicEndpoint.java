package io.github.avivcarmis.trafficante.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.github.avivcarmis.trafficante.exceptions.APIException;
import io.github.avivcarmis.trafficante.exceptions.BadRequestException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A basic endpoint for the API server.
 * Extend this class to create a handler for requests to a certain
 * path and http method.
 *
 * Usually this class should be abstractly extended once to implement {@link #wrapResponse(Object)}
 * and {@link #wrapFailure(Throwable)} methods, and allow a unified wrapping for your entire API.
 *
 * Methods named `defaultX` may also be overridden to alter behaviour.
 *
 * @param <REQ>         the type of the endpoint request entity
 * @param <RES>         the type of the endpoint response entity
 * @param <RES_WRAPPER> supplies a standard wrapping for the endpoint response,
 *                     to allow unified API across the entire server
 */
abstract public class BasicEndpoint<REQ, RES, RES_WRAPPER> {

    // Constants

    private static final Log LOG = LogFactory.getLog(BasicEndpoint.class);

    private static final ThreadLocal<HttpServletRequest> SERVLET_REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    private static final ThreadLocal<MultiValueMap<String, String>> RESPONSE_HEADERS_THREAD_LOCAL = new ThreadLocal<>();

    private static final ThreadLocal<HttpStatus> RESPONSE_STATUS_THREAD_LOCAL = new ThreadLocal<>();

    // Fields

    private final String _apiPath;

    private final RequestMethod _httpMethod;

    private final boolean _enableFlowLogging;

    @Autowired
    private ObjectMapper _objectMapper;

    // Constructors

    public BasicEndpoint(RequestMethod httpMethod, boolean enableFlowLogging) {
        if (httpMethod == null) {
            throw new IllegalArgumentException("endpoint http method cannot be null");
        }
        _httpMethod = httpMethod;
        _enableFlowLogging = enableFlowLogging;
        _apiPath = httpMethod.name() + " " + defaultPathProvider();
    }

    // Public

    @SuppressWarnings("unused")
    public final @ResponseBody ResponseEntity<RES_WRAPPER> doAPICall(@RequestBody REQ request) {
        RES_WRAPPER response;
        try {
            logEnter(request);
            responseStatusCode(HttpStatus.OK);
            RESPONSE_HEADERS_THREAD_LOCAL.set(new LinkedMultiValueMap<>());
            SERVLET_REQUEST_THREAD_LOCAL.set(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            validateObject(_objectMapper.getPropertyNamingStrategy(), request);
            if (request instanceof Validatable) {
                ((Validatable) request).validate();
            }
            response = wrapResponse(defaultInvocationWrapper(request));
        } catch (APIException e) {
            response = wrapFailure(e);
            responseStatusCode(e.getStatusCode());
        } catch (Throwable t) {
            response = wrapFailure(new RuntimeException("internal server error occurred"));
            responseStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        ResponseEntity<RES_WRAPPER> entity = new ResponseEntity<>(response, RESPONSE_HEADERS_THREAD_LOCAL.get(), RESPONSE_STATUS_THREAD_LOCAL.get());
        logExit(entity);
        return entity;
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(Throwable.class)
    public final @ResponseBody ResponseEntity<RES_WRAPPER> errorHandler(Throwable t) {
        RES_WRAPPER response;
        HttpStatus status;
        if (t instanceof APIException) {
            response = wrapFailure(t);
            status = ((APIException) t).getStatusCode();
        }
        else {
            response = wrapFailure(new RuntimeException("internal server error occurred"));
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(response, null, status);
    }

    public final RequestMethod getHttpMethod() {
        return _httpMethod;
    }

    /**
     * To be overridden in case some operations should be performed before and/or
     * after handling the request. For example, measuring execution time, extra logging, etc...
     * @param request the parsed client request
     * @return the produced endpoint response
     * @throws APIException in case execution has failed
     */
    public RES defaultInvocationWrapper(REQ request) throws APIException {
        return handle(request);
    }

    /**
     * To be overridden in case path strategy should be changed.
     * By default, the path of an endpoint is plus the name of the
     * endpoint class using the naming convention of the server.
     * @return the path of the endpoint
     */
    public String defaultPathProvider() {
        return "/" + Trafficante.getSettings().getNamingStrategy()
                .nameForField(null, null, getClass().getSimpleName());
    }

    public ParamsRequestCondition defaultParamsRequestConditionProvider() {
        return null;
    }

    public HeadersRequestCondition defaultHeadersRequestConditionProvider() {
        return null;
    }

    public ConsumesRequestCondition defaultConsumesRequestConditionProvider() {
        return null;
    }

    public ProducesRequestCondition defaultProducesRequestConditionProvider() {
        return null;
    }

    // Private

    private void logEnter(REQ request) {
        logMessage("Entering " + _apiPath + " with " + _objectMapper.valueToTree(request));
    }

    private void logExit(ResponseEntity<RES_WRAPPER> response) {
        logMessage("Exiting " + _apiPath + " with status " + response.getStatusCode() + " and body " +
                _objectMapper.valueToTree(response.getBody()).toString());
    }

    private void logMessage(String message) {
        if (_enableFlowLogging) {
            LOG.info(message);
        }
    }

    private void validateObject(PropertyNamingStrategy strategy, Object toValidate) throws BadRequestException {
        for (Field field : listClassFields(toValidate.getClass())) {
            validateField(strategy, toValidate, field);
        }
    }

    private void validateField(PropertyNamingStrategy strategy, Object ownerObject, Field field)
            throws BadRequestException {
        Required[] annotations = field.getAnnotationsByType(Required.class);
        if (annotations == null || annotations.length == 0) {
            return;
        }
        try {
            field.setAccessible(true);
            Object value = field.get(ownerObject);
            if (value == null) {
                String name = field.getName();
                if (strategy != null) {
                    name = strategy.nameForField(null, null, field.getName());
                }
                throw new BadRequestException("field `" + name + "` is required");
            }
            else {
                validateObject(strategy, value);
            }
        } catch (IllegalAccessException ignored) {}
    }

    private List<Field> listClassFields(Class<?> startClass) {
        List<Field> result = new ArrayList<>();
        Class<?> currentClass = startClass;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            Field[] currentClassFields = currentClass.getDeclaredFields();
            for (Field field : currentClassFields) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    result.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return result;
    }

    /**
     * Wraps a successful response
     * @param response the response instance
     * @return a wrapped response
     */
    abstract protected RES_WRAPPER wrapResponse(RES response);

    /**
     * Wraps a failure response
     * @param t the throwable causing the failure
     * @return a wrapped response
     */
    abstract protected RES_WRAPPER wrapFailure(Throwable t);

    /**
     * The execution of the endpoint
     * @param request the parsed client request
     * @return the produces endpoint response
     * @throws APIException in case execution has failed
     */
    abstract protected RES handle(REQ request) throws APIException;

    // Static

    /**
     * Returns the value of the given request header name
     * @param key name of the header to read
     * @return the value of the header, null if not found
     */
    protected static String requestHeader(String key) {
        return SERVLET_REQUEST_THREAD_LOCAL.get().getHeader(key);
    }

    /**
     * Writes an HTTP header to the response
     * @param key   key of the header
     * @param value value of the header
     */
    protected static void responseHeader(String key, String value) {
        RESPONSE_HEADERS_THREAD_LOCAL.get().add(key, value);
    }

    protected static void responseStatusCode(HttpStatus status) {
        RESPONSE_STATUS_THREAD_LOCAL.set(status);
    }

}
