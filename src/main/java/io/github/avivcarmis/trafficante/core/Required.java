package io.github.avivcarmis.trafficante.core;

import java.lang.annotation.*;

/**
 * Field annotation to signal request fields that may not ne null
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.FIELD)
public @interface Required {}