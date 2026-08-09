package com.cloudwebrtc.webrtc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.cloudwebrtc.webrtc.audio.LocalAudioTrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;

public class MethodCallHandlerImplTest {
  private final List<String> calls = new ArrayList<>();

  private class RecordingAudioTrack extends AudioTrack {
    RecordingAudioTrack() {
      super(1L);
    }

    @Override
    public boolean setEnabled(boolean enable) {
      return true;
    }

    @Override
    public void dispose() {
      calls.add("dispose");
    }
  }

  private class RecordingMediaStream extends MediaStream {
    RecordingMediaStream() {
      super(1L);
    }

    @Override
    public boolean removeTrack(AudioTrack track) {
      calls.add("removeTrack");
      return true;
    }
  }

  @Test
  public void disposesTheTrackItDropsFromTheRegistry() {
    MethodCallHandlerImpl handler = new MethodCallHandlerImpl(null, null, null);
    handler.putLocalTrack("track", new LocalAudioTrack(new RecordingAudioTrack(), null));

    handler.trackDispose("track");

    assertEquals(Collections.singletonList("dispose"), calls);
    assertNull(handler.getLocalTrack("track"));
  }

  @Test
  public void dropsTheTrackFromEveryLocalStreamBeforeDisposingIt() {
    MethodCallHandlerImpl handler = new MethodCallHandlerImpl(null, null, null);
    handler.putLocalTrack("track", new LocalAudioTrack(new RecordingAudioTrack(), null));
    handler.putLocalStream("first", new RecordingMediaStream());
    handler.putLocalStream("second", new RecordingMediaStream());

    handler.trackDispose("track");

    assertEquals(Arrays.asList("removeTrack", "removeTrack", "dispose"), calls);
  }
}
