package com.cloudwebrtc.webrtc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.cloudwebrtc.webrtc.utils.ConstraintsMap;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class GetUserMediaImplTest {
  private static ConstraintsMap constraints(String key, Object value) {
    Map<String, Object> outer = new HashMap<>();
    outer.put(key, value);
    return new ConstraintsMap(outer);
  }

  private static Map<String, Object> ideal(Object value) {
    Map<String, Object> inner = new HashMap<>();
    inner.put("ideal", value);
    return inner;
  }

  @Test
  public void readsAnIdealCarriedAsANumber() {
    assertEquals(
        Integer.valueOf(1280),
        GetUserMediaImpl.getConstrainInt(constraints("width", ideal(1280)), "width"));
  }

  @Test
  public void readsAnIdealCarriedAsAString() {
    assertEquals(
        Integer.valueOf(720),
        GetUserMediaImpl.getConstrainInt(constraints("height", ideal("720")), "height"));
  }

  @Test
  public void readsAPlainNumber() {
    assertEquals(
        Integer.valueOf(24),
        GetUserMediaImpl.getConstrainInt(constraints("frameRate", 24), "frameRate"));
  }

  @Test
  public void answersNullForAKeyNoConstraintCarries() {
    assertNull(GetUserMediaImpl.getConstrainInt(constraints("width", ideal(1280)), "height"));
  }

  @Test
  public void answersNullForAnAbsentConstraintsMap() {
    assertNull(GetUserMediaImpl.getConstrainInt(null, "width"));
  }
}
