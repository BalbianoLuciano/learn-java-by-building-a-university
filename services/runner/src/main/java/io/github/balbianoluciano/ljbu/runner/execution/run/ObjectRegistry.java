package io.github.balbianoluciano.ljbu.runner.execution.run;

import com.sun.jdi.ObjectReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gives every object the trace mentions a stable id: o1, o2, … in order of appearance. */
final class ObjectRegistry {

  private final Map<Long, String> ids = new HashMap<>();
  private final List<ObjectReference> objects = new ArrayList<>();

  /** The id of the object, registering it the first time it is seen. */
  String idOf(ObjectReference object) {
    return ids.computeIfAbsent(
        object.uniqueID(),
        unused -> {
          // The final heap is read at the end: the object must still be there.
          object.disableCollection();
          objects.add(object);
          return "o" + objects.size();
        });
  }

  boolean knows(ObjectReference object) {
    return ids.containsKey(object.uniqueID());
  }

  int size() {
    return objects.size();
  }

  ObjectReference object(int index) {
    return objects.get(index);
  }

  String id(int index) {
    return "o" + (index + 1);
  }
}
