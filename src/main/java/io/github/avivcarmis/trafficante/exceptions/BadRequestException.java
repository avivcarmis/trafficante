package io.github.avivcarmis.trafficante.exceptions;

import org.springframework.http.HttpStatus;

/**
 * An exception to be thrown when a request is invalid
 */
public class BadRequestException extends APIException {

    // Constructors
    
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

}
