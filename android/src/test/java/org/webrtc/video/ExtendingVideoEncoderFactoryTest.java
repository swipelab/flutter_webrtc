package org.webrtc.video;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoFrame;

import java.util.ArrayList;
import java.util.HashMap;

public class ExtendingVideoEncoderFactoryTest {
  private static class NamedEncoder implements VideoEncoder {
    final String name;

    NamedEncoder(String name) {
      this.name = name;
    }

    @Override
    public VideoCodecStatus initEncode(VideoEncoder.Settings settings, VideoEncoder.Callback cb) {
      return VideoCodecStatus.OK;
    }

    @Override
    public VideoCodecStatus release() {
      return VideoCodecStatus.OK;
    }

    @Override
    public VideoCodecStatus encode(VideoFrame frame, VideoEncoder.EncodeInfo info) {
      return VideoCodecStatus.OK;
    }

    @Override
    public VideoCodecStatus setRateAllocation(VideoEncoder.BitrateAllocation allocation, int fps) {
      return VideoCodecStatus.OK;
    }

    @Override
    public VideoEncoder.ScalingSettings getScalingSettings() {
      return VideoEncoder.ScalingSettings.OFF;
    }

    @Override
    public String getImplementationName() {
      return name;
    }
  }

  private static class ListedFactory implements VideoEncoderFactory {
    final String implementationName;
    final VideoCodecInfo[] codecs;

    ListedFactory(String implementationName, String... names) {
      this.implementationName = implementationName;
      this.codecs = new VideoCodecInfo[names.length];
      for (int i = 0; i < names.length; i++) {
        this.codecs[i] = codec(names[i]);
      }
    }

    @Override
    public VideoEncoder createEncoder(VideoCodecInfo info) {
      for (VideoCodecInfo codec : codecs) {
        if (codec.name.equals(info.name)) {
          return new NamedEncoder(implementationName);
        }
      }
      return null;
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
      return codecs;
    }
  }

  private static VideoCodecInfo codec(String name) {
    return new VideoCodecInfo(name, new HashMap<>(), new ArrayList<>());
  }

  private static String[] names(VideoCodecInfo[] codecs) {
    String[] names = new String[codecs.length];
    for (int i = 0; i < codecs.length; i++) {
      names[i] = codecs[i].name;
    }
    return names;
  }

  @Test
  public void appendsTheCodecsOnlySoftwareCarriesBehindTheHardwareOnes() {
    ExtendingVideoEncoderFactory factory = new ExtendingVideoEncoderFactory(
        new ListedFactory("hardware", "VP8", "VP9"),
        new ListedFactory("software", "VP8", "H264", "H265"));

    assertArrayEquals(
        new String[] {"VP8", "VP9", "H264", "H265"}, names(factory.getSupportedCodecs()));
  }

  @Test
  public void encodesACodecTheHardwareCarriesInHardware() {
    ExtendingVideoEncoderFactory factory = new ExtendingVideoEncoderFactory(
        new ListedFactory("hardware", "VP8", "H264"),
        new ListedFactory("software", "VP8", "H264"));

    assertEquals("hardware", factory.createEncoder(codec("H264")).getImplementationName());
  }

  @Test
  public void encodesACodecOnlySoftwareCarriesInSoftware() {
    ExtendingVideoEncoderFactory factory = new ExtendingVideoEncoderFactory(
        new ListedFactory("hardware", "VP8"), new ListedFactory("software", "VP8", "H264"));

    assertEquals("software", factory.createEncoder(codec("H264")).getImplementationName());
  }

  @Test
  public void carriesNoEncoderForACodecNeitherFactoryNames() {
    ExtendingVideoEncoderFactory factory = new ExtendingVideoEncoderFactory(
        new ListedFactory("hardware", "VP8"), new ListedFactory("software", "VP8"));

    assertNull(factory.createEncoder(codec("AV1")));
  }
}
