package io.github.avivcarmis.trafficante.adapters.swagger;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.types.ResolvedArrayType;
import com.fasterxml.classmate.types.ResolvedPrimitiveType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.schema.Collections;
import springfox.documentation.schema.Maps;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.schema.Types;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.TypeNameProviderPlugin;
import springfox.documentation.spi.schema.contexts.ModelContext;

/**
 * Implements a TypeNameExtractor in which model names are taken from
 * {@link CautiousApiModelReader} and not directly from field classes.
 */
@Component
@Primary
public class CautiousTypeNameExtractor extends TypeNameExtractor {

    // Fields

    private final TypeResolver typeResolver;

    // Constructors

    @Autowired
    public CautiousTypeNameExtractor(TypeResolver typeResolver,
                                     @Qualifier("typeNameProviderPluginRegistry")
                                     PluginRegistry<TypeNameProviderPlugin, DocumentationType> typeNameProviders) {
        super(typeResolver, typeNameProviders);
        this.typeResolver = typeResolver;
    }

    // Public

    @Override
    public String typeName(ModelContext context) {
        ResolvedType type = typeResolver.resolve(context.getType());
        return shouldIgnore(type) ? super.typeName(context) : CautiousApiModelReader.getModelName(type);
    }

    // Private

    /**
     * Ignores all types that should be implemented by the super class,
     * for example primitive types that should be named differently than
     * `java.lang.Integer`.
     * @param type type to test
     * @return true if should be handled by the super class, false otherwise.
     */
    private boolean shouldIgnore(ResolvedType type) {
        return Types.typeNameFor(type.getErasedType()) != null ||
                Maps.isMapType(type) ||
                Collections.isContainerType(type) ||
                Void.class.equals(type.getErasedType()) ||
                Void.TYPE.equals(type.getErasedType()) ||
                MultipartFile.class.isAssignableFrom(type.getErasedType()) ||
                type instanceof ResolvedPrimitiveType ||
                type.getErasedType().isEnum() ||
                type instanceof ResolvedArrayType;
    }

}
