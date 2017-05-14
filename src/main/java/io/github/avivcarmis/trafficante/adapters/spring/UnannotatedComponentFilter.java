package io.github.avivcarmis.trafficante.adapters.spring;

import com.google.common.collect.ImmutableSet;
import io.github.avivcarmis.trafficante.core.BasicEndpoint;
import io.github.avivcarmis.trafficante.core.BasicErrorHandler;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.util.Set;

/**
 * A type filter to identify @Component classes that are not annotated.
 * Includes everything that inherits SUPPORTED_CLASSES constant.
 */
public class UnannotatedComponentFilter implements TypeFilter {

    // Constants

    private static final Set<Class<?>> SUPPORTED_CLASSES = ImmutableSet.of(
            BasicEndpoint.class,
            BasicErrorHandler.class
    );

    // Public

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
        Class<?> beanClass;
        try {
            beanClass = Class.forName(metadataReader.getClassMetadata().getClassName());
        } catch (ClassNotFoundException e) {
            return false;
        }
        for (Class<?> supportedClass : SUPPORTED_CLASSES) {
            if (supportedClass.isAssignableFrom(beanClass)) {
                return true;
            }
        }
        return false;
    }

}
