package io.github.avivcarmis.trafficante.exceptions;

import org.springframework.http.HttpStatus;

/**
 * An exception to be thrown while handling a server request.
 * Indicates the response status code and an error message.
 */
public class APIException extends Exception {

    // Fields

    /**
     * The status code to respond with
     */
    private final HttpStatus _statusCode;

    // Constructors

    public APIException(String message, HttpStatus status) {
        super(message);
        _statusCode = status;
    }

    public APIException(String message) {
        this(message, HttpStatus.OK);
    }

    // Etc

    public HttpStatus getStatusCode() {
        return _statusCode;
    }

}
