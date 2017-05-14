package io.github.avivcarmis.trafficante.adapters.swagger;

import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.avivcarmis.trafficante.core.HttpMethodContentClass;
import io.github.avivcarmis.trafficante.core.Required;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.schema.Types;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.readers.operation.OperationParameterReader;
import springfox.documentation.spring.web.readers.parameter.ModelAttributeParameterExpander;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Handles parameter reading in case of query string endpoints,
 * to force property naming to go through the server ObjectMapper.
 */
@Component
public class QueryStringOperationParameterReader extends OperationParameterReader {

    // Fields

    private final ObjectMapper objectMapper;

    private final TypeResolver typeResolver;

    // Constructors

    @Autowired
    public QueryStringOperationParameterReader(ModelAttributeParameterExpander expander, ObjectMapper objectMapper) {
        super(expander);
        this.objectMapper = objectMapper;
        this.typeResolver = new TypeResolver();
    }

    // Public

    @Override
    public void apply(OperationContext context) {
        if (HttpMethodContentClass.classify(context.httpMethod()) != HttpMethodContentClass.QUERY_STRING) {
            super.apply(context);
        }
        else {
            context.operationBuilder().parameters(context.getGlobalOperationParameters());
            context.operationBuilder().parameters(readParameters(context));
        }
    }

    // Private

    private List<Parameter> readParameters(OperationContext context) {
        List<ResolvedMethodParameter> methodParameters = context.getParameters();
        List<Parameter> parameters = newArrayList();
        ResolvedMethodParameter methodParameter = methodParameters.get(0);
        Class<?> type = methodParameter.getParameterType().getErasedType();
        Map<Field, String> fields = mapFields(type);
        for (Map.Entry<Field, String> entry : fields.entrySet()) {
            Field field = entry.getKey();
            Parameter param = new ParameterBuilder()
                    .name(entry.getValue())
                    .required(field.getAnnotationsByType(Required.class).length > 0)
                    .allowMultiple(false)
                    .modelRef(
                            new ModelRef(
                                    Types.typeNameFor(field.getType()),
                                    null,
                                    Map.class.isAssignableFrom(field.getType())
                            )
                    )
                    .type(typeResolver.resolve(field.getType()))
                    .parameterType("query")
                    .hidden(false)
                    .build();
            parameters.add(param);
        }
        return parameters;
    }

    private Map<Field, String> mapFields(Class<?> aClass) {
        Map<Field, String> result = new HashMap<>();
        JavaType type = TypeFactory.defaultInstance().constructType(aClass);
        DeserializationConfig config = objectMapper.getDeserializationConfig();
        BeanDescription beanDescription = config.introspect(type);
        for (BeanPropertyDefinition definition : beanDescription.findProperties()) {
            result.put(definition.getField().getAnnotated(), definition.getName());
        }
        return result;
    }

}
