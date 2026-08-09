package com.cloudwebrtc.webrtc.audio;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;

public class LocalAudioTrackTest {
  private static class CountingAudioTrack extends AudioTrack {
    int disposed;

    CountingAudioTrack() {
      super(1L);
    }

    @Override
    public void dispose() {
      disposed += 1;
    }
  }

  private static class CountingAudioSource extends AudioSource {
    int disposed;

    CountingAudioSource() {
      super(1L);
    }

    @Override
    public void dispose() {
      disposed += 1;
    }
  }

  @Test
  public void releasesTheCaptureSourceItWasOpenedOn() {
    CountingAudioTrack track = new CountingAudioTrack();
    CountingAudioSource source = new CountingAudioSource();

    new LocalAudioTrack(track, source).dispose();

    assertEquals(1, track.disposed);
    assertEquals(1, source.disposed);
  }

  @Test
  public void releasesOnlyTheTrackWhenItOwnsNoSource() {
    CountingAudioTrack track = new CountingAudioTrack();

    new LocalAudioTrack(track, null).dispose();

    assertEquals(1, track.disposed);
  }
}
