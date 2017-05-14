package io.github.avivcarmis.trafficante.core;

import io.github.avivcarmis.trafficante.exceptions.BadRequestException;

/**
 * An interface to implement for an endpoint request entity
 * in case that custom validation is required.
 */
public interface Validatable {

    /**
     * Validates the current object
     * @throws BadRequestException in case the object is not valid
     */
    void validate() throws BadRequestException;

}
