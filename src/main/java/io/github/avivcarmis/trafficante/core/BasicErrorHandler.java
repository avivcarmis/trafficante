package io.github.avivcarmis.trafficante.core;

import org.springframework.boot.autoconfigure.web.AbstractErrorController;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A basic handler for errors that cannot be handled directly by and endpoint.
 * Extend this class to create a handler which can deliver errors using your custom API.
 */
abstract public class BasicErrorHandler<RES_WRAPPER> extends AbstractErrorController {

    // Constants

    private static final String ERROR_PATH = "/error";

    // Fields

    private final ConcurrentHashMap<HttpStatus, RES_WRAPPER> _responseCache;

    // Constructors

    public BasicErrorHandler() {
        super(new DefaultErrorAttributes());
        _responseCache = new ConcurrentHashMap<>();
    }

    // Public

    @SuppressWarnings("unused")
    @RequestMapping(value = ERROR_PATH)
    public ResponseEntity<RES_WRAPPER> handleError(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        return new ResponseEntity<>(getResponse(status), status);
    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }

    // Private

    private RES_WRAPPER getResponse(HttpStatus status) {
        return _responseCache.computeIfAbsent(status, s -> {
            String description = status.name().toLowerCase().replace("_", " ");
            return wrapFailure(new RuntimeException(description));
        });
    }

    abstract protected RES_WRAPPER wrapFailure(Throwable t);

}