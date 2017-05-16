package io.github.avivcarmis.trafficante.adapters.swagger;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.Model;
import springfox.documentation.schema.ModelProvider;
import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.spi.service.contexts.RequestMappingContext;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;
import springfox.documentation.spring.web.scanners.ApiModelReader;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * By default, Springfox swagger does not account for different classes
 * with the same name.
 * This model reader overrides the default one and uses a cautious naming
 * in which duplicate class names gets numbered.
 */
@Component
@Primary
public class CautiousApiModelReader extends ApiModelReader {

    // Constants

    private static final Set<String> USED_MODEL_NAMES = new HashSet<>();

    private static final ConcurrentHashMap<String, String> MODEL_NAME_MAPPER = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(ApiModelReader.class);

    // Fields

    private final ModelProvider _modelProvider;

    private final TypeResolver _typeResolver;

    private final DocumentationPluginsManager _pluginsManager;

    // Constructors

    @Autowired
    public CautiousApiModelReader(@Qualifier("cachedModels") ModelProvider modelProvider,
                                  TypeResolver typeResolver,
                                  DocumentationPluginsManager pluginsManager) {
        super(modelProvider, typeResolver, pluginsManager);
        this._modelProvider = modelProvider;
        this._typeResolver = typeResolver;
        this._pluginsManager = pluginsManager;
    }

    // Public

    @Override
    public Map<String, Model> read(RequestMappingContext context) {
        Set<Class> ignorableTypes = newHashSet(context.getIgnorableParameterTypes());
        Set<ModelContext> modelContexts = _pluginsManager.modelContexts(context);
        Map<String, Model> modelMap = newHashMap(context.getModelMap());
        for (ModelContext each : modelContexts) {
            markIgnorablesAsHasSeen(_typeResolver, ignorableTypes, each);
            Optional<Model> pModel = _modelProvider.modelFor(each);
            if (pModel.isPresent()) {
                LOG.debug("Generated parameter model id: {}, name: {}, schema: {} models",
                        pModel.get().getId(),
                        pModel.get().getName());
                mergeModelMap(modelMap, pModel.get(), (ResolvedType) each.getType());
            } else {
                LOG.debug("Did not find any parameter models for {}", each.getType());
            }
            populateDependencies(each, modelMap);
        }
        return modelMap;
    }

    // Private

    private void mergeModelMap(Map<String, Model> target, Model source, ResolvedType type) {
        String name = getModelName(type);
        target.put(name,cloneModel(source, name));
    }

    private void markIgnorablesAsHasSeen(TypeResolver typeResolver,
                                         Set<Class> ignorableParameterTypes,
                                         ModelContext modelContext) {
        for (Class ignorableParameterType : ignorableParameterTypes) {
            modelContext.seen(typeResolver.resolve(ignorableParameterType));
        }
    }

    private void populateDependencies(ModelContext modelContext, Map<String, Model> modelMap) {
        Map<String, Model> dependencies = _modelProvider.dependencies(modelContext);
        for (Model each : dependencies.values()) {
            mergeModelMap(modelMap, each, each.getType());
        }
    }

    private Model cloneModel(Model model, String withName) {
        return new Model(
                withName,
                withName,
                model.getType(),
                model.getQualifiedType(),
                model.getProperties(),
                model.getDescription(),
                model.getBaseModel(),
                model.getDiscriminator(),
                model.getSubTypes(),
                model.getExample()
        );
    }

    // Static

    static String getModelName(ResolvedType type) {
        return MODEL_NAME_MAPPER.computeIfAbsent(type.getTypeName(), t -> {
            synchronized (USED_MODEL_NAMES) {
                String baseName = type.getErasedType().getSimpleName();
                String name = baseName;
                int nextNumber = 1;
                while (USED_MODEL_NAMES.contains(name)) {
                    name = baseName + nextNumber;
                    nextNumber++;
                }
                USED_MODEL_NAMES.add(name);
                return name;
            }
        });
    }

}
