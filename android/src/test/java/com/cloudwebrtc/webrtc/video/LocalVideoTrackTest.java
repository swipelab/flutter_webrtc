package com.cloudwebrtc.webrtc.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public class LocalVideoTrackTest {
  private static class CountingBuffer implements VideoFrame.Buffer {
    int released;

    @Override
    public int getWidth() {
      return 2;
    }

    @Override
    public int getHeight() {
      return 2;
    }

    @Override
    public VideoFrame.I420Buffer toI420() {
      return null;
    }

    @Override
    public void retain() {}

    @Override
    public void release() {
      released += 1;
    }

    @Override
    public VideoFrame.Buffer cropAndScale(int cropX, int cropY, int cropWidth, int cropHeight,
        int scaleWidth, int scaleHeight) {
      return this;
    }
  }

  private static class LastFrameSink implements VideoSink {
    VideoFrame last;

    @Override
    public void onFrame(VideoFrame frame) {
      last = frame;
    }
  }

  private static VideoFrame frame(CountingBuffer buffer) {
    return new VideoFrame(buffer, 0, 0L);
  }

  @Test
  public void releasesTheFrameAProcessorPutInPlace() {
    LocalVideoTrack track = new LocalVideoTrack(null);
    LastFrameSink sink = new LastFrameSink();
    track.setSink(sink);

    CountingBuffer capturedBuffer = new CountingBuffer();
    CountingBuffer replacementBuffer = new CountingBuffer();
    VideoFrame captured = frame(capturedBuffer);
    VideoFrame replacement = frame(replacementBuffer);
    track.addProcessor(incoming -> replacement);

    track.onFrameCaptured(captured);

    assertSame(replacement, sink.last);
    assertEquals(1, replacementBuffer.released);
    assertEquals(0, capturedBuffer.released);
  }

  @Test
  public void keepsTheCapturedFrameAProcessorPassedThrough() {
    LocalVideoTrack track = new LocalVideoTrack(null);
    LastFrameSink sink = new LastFrameSink();
    track.setSink(sink);

    CountingBuffer capturedBuffer = new CountingBuffer();
    VideoFrame captured = frame(capturedBuffer);
    track.addProcessor(incoming -> incoming);

    track.onFrameCaptured(captured);

    assertSame(captured, sink.last);
    assertEquals(0, capturedBuffer.released);
  }

  @Test
  public void releasesEveryReplacementAlongAProcessorChain() {
    LocalVideoTrack track = new LocalVideoTrack(null);
    LastFrameSink sink = new LastFrameSink();
    track.setSink(sink);

    CountingBuffer capturedBuffer = new CountingBuffer();
    CountingBuffer middleBuffer = new CountingBuffer();
    CountingBuffer lastBuffer = new CountingBuffer();
    VideoFrame captured = frame(capturedBuffer);
    VideoFrame middle = frame(middleBuffer);
    VideoFrame last = frame(lastBuffer);
    track.addProcessor(incoming -> middle);
    track.addProcessor(incoming -> last);

    track.onFrameCaptured(captured);

    assertSame(last, sink.last);
    assertEquals(1, middleBuffer.released);
    assertEquals(1, lastBuffer.released);
    assertEquals(0, capturedBuffer.released);
  }

  @Test
  public void releasesTheReplacementWhenALaterProcessorThrows() {
    LocalVideoTrack track = new LocalVideoTrack(null);
    LastFrameSink sink = new LastFrameSink();
    track.setSink(sink);

    CountingBuffer capturedBuffer = new CountingBuffer();
    CountingBuffer replacementBuffer = new CountingBuffer();
    VideoFrame captured = frame(capturedBuffer);
    VideoFrame replacement = frame(replacementBuffer);
    RuntimeException failure = new RuntimeException("processor failed");
    track.addProcessor(incoming -> replacement);
    track.addProcessor(incoming -> {
      throw failure;
    });

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> track.onFrameCaptured(captured));

    assertSame(failure, thrown);
    assertEquals(1, replacementBuffer.released);
    assertEquals(0, capturedBuffer.released);
  }

  @Test
  public void releasesTheReplacementWhenTheSinkThrows() {
    LocalVideoTrack track = new LocalVideoTrack(null);
    RuntimeException failure = new RuntimeException("sink failed");
    track.setSink(frame -> {
      throw failure;
    });

    CountingBuffer capturedBuffer = new CountingBuffer();
    CountingBuffer replacementBuffer = new CountingBuffer();
    VideoFrame captured = frame(capturedBuffer);
    VideoFrame replacement = frame(replacementBuffer);
    track.addProcessor(incoming -> replacement);

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> track.onFrameCaptured(captured));

    assertSame(failure, thrown);
    assertEquals(1, replacementBuffer.released);
    assertEquals(0, capturedBuffer.released);
  }

  @Test
  public void leavesTheCapturedFrameUntouchedWithoutASink() {
    LocalVideoTrack track = new LocalVideoTrack(null);
    CountingBuffer capturedBuffer = new CountingBuffer();
    VideoFrame captured = frame(capturedBuffer);
    int[] processed = {0};
    track.addProcessor(incoming -> {
      processed[0] += 1;
      return incoming;
    });

    track.onFrameCaptured(captured);

    assertEquals(0, processed[0]);
    assertEquals(0, capturedBuffer.released);
  }
}
