package io.github.avivcarmis.trafficante.adapters.swagger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import springfox.documentation.service.Operation;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;
import springfox.documentation.spring.web.readers.operation.OperationParameterReader;

import java.util.List;

/**
 * Skips {@link OperationParameterReader} class to allow
 * {@link QueryStringOperationParameterReader} to process instead.
 */
@Component
@Primary
public class QueryStringDocumentationPluginsManager extends DocumentationPluginsManager {

    // Fields

    private final PluginRegistry<OperationBuilderPlugin, DocumentationType> operationBuilderPlugins;

    // Constructors

    public QueryStringDocumentationPluginsManager(@Autowired
                                                  @Qualifier("operationBuilderPluginRegistry")
                                                          PluginRegistry<OperationBuilderPlugin, DocumentationType>
                                                          operationBuilderPlugins) {
        this.operationBuilderPlugins = operationBuilderPlugins;
    }

    // Public

    @Override
    public Operation operation(OperationContext operationContext) {
        List<OperationBuilderPlugin> allPlugins = operationBuilderPlugins
                .getPluginsFor(operationContext.getDocumentationType());
        for (OperationBuilderPlugin each : allPlugins) {
            if (each.getClass() == OperationParameterReader.class) {
                continue;
            }
            each.apply(operationContext);
        }
        return operationContext.operationBuilder().build();
    }
}
