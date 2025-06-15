package iam.userservice.mapper;

import org.mapstruct.BeforeMapping;
import org.mapstruct.TargetType;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A context class designed to handle cyclic references during object mapping operations.
 * This class prevents infinite recursion that can occur when mapping objects with
 * bidirectional relationships or circular dependencies.
 *
 * <p>The context maintains a cache of already mapped objects using {@link IdentityHashMap},
 * which uses reference equality instead of {@code equals()} for comparing keys. This makes
 * it suitable for tracking object instances during the mapping process.</p>
 *
 * <p>Usage example with MapStruct:</p>
 * <pre>
 * {@code
 * @Mapper
 * public interface MyMapper {
 *     @Mapping(target = "someField", source = "sourceField")
 *     TargetDTO toDto(SourceEntity source, @Context CyclicReferenceMappingContext context);
 * }
 * }
 */
public class CircularReferenceMappingContext {
    private final Map<Object, Object> mappedObjects = new IdentityHashMap<>();


    @BeforeMapping
    @SuppressWarnings("unchecked")
    public <T> T findAlreadyMappedObject(Object sourceObject, @TargetType Class<T> targetType) {
        return (T) mappedObjects.get(sourceObject);
    }


    @BeforeMapping
    public void saveMapping(Object sourceObject, @TargetType Object targetObject) {
        mappedObjects.put(sourceObject, targetObject);
    }
}
