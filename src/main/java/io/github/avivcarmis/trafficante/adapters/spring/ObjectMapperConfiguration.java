package io.github.avivcarmis.trafficante.adapters.spring;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.avivcarmis.trafficante.core.Trafficante;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configures the server object mapper
 */
public class ObjectMapperConfiguration extends Jackson2ObjectMapperBuilder {

    @Override
    public void configure(ObjectMapper objectMapper) {
        super.configure(objectMapper);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setPropertyNamingStrategy(Trafficante.getSettings().getNamingStrategy());
    }

}
