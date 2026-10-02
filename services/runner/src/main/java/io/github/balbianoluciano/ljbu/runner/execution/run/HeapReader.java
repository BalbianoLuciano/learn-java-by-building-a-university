package io.github.balbianoluciano.ljbu.runner.execution.run;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import io.github.balbianoluciano.ljbu.runner.execution.structure.TypeNames;
import io.github.balbianoluciano.ljbu.runner.execution.trace.HeapObject;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Value;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the final state of the objects of the trace while the child JVM is suspended. Learner
 * objects are read field by field; arrays and the usual collections, element by element; any other
 * object is opaque.
 */
final class HeapReader {

  static final int MAX_ELEMENTS = 200;

  /** Objects found while reading (elements of elements…) are read too, up to this many. */
  private static final int MAX_OBJECTS_READ = 2000;

  private final ObjectRegistry registry;
  private final ValueMapper mapper;
  private final Set<String> learnerClasses;

  HeapReader(ObjectRegistry registry, ValueMapper mapper, Set<String> learnerClasses) {
    this.registry = registry;
    this.mapper = mapper;
    this.learnerClasses = learnerClasses;
  }

  Map<String, HeapObject> readHeap() {
    Map<String, HeapObject> heap = new LinkedHashMap<>();
    // The registry grows while reading: a list brings in its elements.
    for (int i = 0; i < registry.size(); i++) {
      ObjectReference object = registry.object(i);
      heap.put(registry.id(i), i < MAX_OBJECTS_READ ? read(object) : opaque(object));
    }
    return heap;
  }

  Map<String, Map<String, Value>> readStatics(List<ReferenceType> learnerTypes) {
    Map<String, Map<String, Value>> statics = new LinkedHashMap<>();
    for (ReferenceType type : learnerTypes) {
      try {
        List<Field> fields =
            type.fields().stream()
                .filter(field -> field.isStatic() && !field.isSynthetic())
                .toList();
        if (!fields.isEmpty()) {
          statics.put(type.name(), values(type.getValues(fields)));
        }
      } catch (RuntimeException e) {
        // The class was not initialized or the JVM is gone: it has no statics to show.
      }
    }
    return statics;
  }

  private HeapObject read(ObjectReference object) {
    try {
      if (object instanceof ArrayReference array) {
        return new HeapObject.Sequence(
            typeName(object), array.length(), elements(array, array.length()));
      }
      String className = object.referenceType().name();
      if (learnerClasses.contains(className)) {
        return new HeapObject.Instance(className, learnerFields(object));
      }
      return switch (className) {
        case "java.util.ArrayList" -> {
          int size = intField(object, "size");
          yield new HeapObject.Sequence(
              typeName(object), size, elements(arrayField(object, "elementData"), size));
        }
        case "java.util.Arrays$ArrayList" -> arrayBacked(object, "a");
        case "java.util.ImmutableCollections$ListN" -> arrayBacked(object, "elements");
        case "java.util.HashMap", "java.util.LinkedHashMap" -> {
          List<HeapObject.Entry> entries = entries(object);
          yield new HeapObject.MapObject(typeName(object), intField(object, "size"), entries);
        }
        case "java.util.HashSet", "java.util.LinkedHashSet" -> {
          ObjectReference map = (ObjectReference) field(object, "map");
          List<Value> keys = entries(map).stream().map(HeapObject.Entry::key).toList();
          yield new HeapObject.Sequence(typeName(object), intField(map, "size"), keys);
        }
        default -> opaque(object);
      };
    } catch (RuntimeException e) {
      return opaque(object);
    }
  }

  private static HeapObject opaque(ObjectReference object) {
    String type;
    try {
      type = typeName(object);
    } catch (RuntimeException e) {
      type = "Object";
    }
    return new HeapObject.Instance(type, Map.of());
  }

  /** The fields the learner declared, superclass first. */
  private Map<String, Value> learnerFields(ObjectReference object) {
    List<Field> fields = new ArrayList<>();
    for (ClassType type = (ClassType) object.referenceType();
        type != null;
        type = type.superclass()) {
      if (learnerClasses.contains(type.name())) {
        fields.addAll(
            0,
            type.fields().stream()
                .filter(field -> !field.isStatic() && !field.isSynthetic())
                .toList());
      }
    }
    return values(object.getValues(fields));
  }

  private Map<String, Value> values(Map<Field, com.sun.jdi.Value> mirrors) {
    Map<String, Value> values = new LinkedHashMap<>();
    mirrors.forEach((field, value) -> values.put(field.name(), mapper.toValue(value)));
    return values;
  }

  private HeapObject arrayBacked(ObjectReference list, String fieldName) {
    ArrayReference array = arrayField(list, fieldName);
    return new HeapObject.Sequence(typeName(list), array.length(), elements(array, array.length()));
  }

  private List<Value> elements(ArrayReference array, int size) {
    int count = Math.min(size, MAX_ELEMENTS);
    if (array == null || count == 0) {
      return List.of();
    }
    return array.getValues(0, count).stream().map(mapper::toValue).toList();
  }

  /** Walks the buckets of a HashMap; every node links to the next one of its bucket. */
  private List<HeapObject.Entry> entries(ObjectReference map) {
    List<HeapObject.Entry> entries = new ArrayList<>();
    ArrayReference table = arrayField(map, "table");
    if (table == null) {
      return entries;
    }
    for (com.sun.jdi.Value bucket : table.getValues()) {
      ObjectReference node = (ObjectReference) bucket;
      while (node != null && entries.size() < MAX_ELEMENTS) {
        entries.add(
            new HeapObject.Entry(
                mapper.toValue(field(node, "key")), mapper.toValue(field(node, "value"))));
        node = (ObjectReference) field(node, "next");
      }
    }
    return entries;
  }

  private static com.sun.jdi.Value field(ObjectReference object, String name) {
    return object.getValue(object.referenceType().fieldByName(name));
  }

  private static ArrayReference arrayField(ObjectReference object, String name) {
    return (ArrayReference) field(object, name);
  }

  private static int intField(ObjectReference object, String name) {
    return ((IntegerValue) field(object, name)).value();
  }

  private static String typeName(ObjectReference object) {
    return TypeNames.simple(object.referenceType().name());
  }
}
