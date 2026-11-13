package com.iitsm.common;

import java.util.*;

/** Explicit projection allow-lists keep JPA objects and confidential fields off the wire. */
public final class Views {
  private Views() {}

  public static Map<String, Object> of(Object o, String fields) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (String f : fields.split(",")) {
      try {
        m.put(f, o.getClass().getField(f).get(o));
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(e);
      }
    }
    return m;
  }
}
