package io.github.avivcarmis.trafficante.adapters.swagger;

import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.property.BeanPropertyNamingStrategy;

/**
 * Overrides an issues with springfox swagger,
 * which extracts field names from the bean, rather than calling the naming
 * strategy again, like {@link springfox.documentation.schema.property.ObjectMapperBeanPropertyNamingStrategy}.
 */
@Component
@Primary
public class FixedBeanPropertyNamingStrategy implements BeanPropertyNamingStrategy {

    @Override
    public String nameForSerialization(BeanPropertyDefinition beanProperty) {
        return beanProperty.getName();
    }

    @Override
    public String nameForDeserialization(BeanPropertyDefinition beanProperty) {
        return beanProperty.getName();
    }

}
