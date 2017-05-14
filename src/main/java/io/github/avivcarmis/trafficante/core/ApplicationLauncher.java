package io.github.avivcarmis.trafficante.core;

import io.github.avivcarmis.trafficante.adapters.spring.ObjectMapperConfiguration;
import io.github.avivcarmis.trafficante.adapters.spring.UnannotatedComponentFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Launches a Spring Boot application
 */
@Configuration
@SpringBootApplication
@ComponentScan(
        basePackages = {"io.github.avivcarmis.trafficante", "${" + ApplicationLauncher.BASE_PACKAGE_INDICATOR + "}"},
        includeFilters = {@ComponentScan.Filter(type = FilterType.CUSTOM, value = UnannotatedComponentFilter.class)}
)
public class ApplicationLauncher {

    // Constants

    static final String BASE_PACKAGE_INDICATOR = "base_package_name";

    // Public

    @Bean
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {
        return new ObjectMapperConfiguration();
    }

    // Static

    public static void launch(String[] args) {
        SpringApplication.run(ApplicationLauncher.class, args);
    }

}
