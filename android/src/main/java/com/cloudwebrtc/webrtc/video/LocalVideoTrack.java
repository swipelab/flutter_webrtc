package com.cloudwebrtc.webrtc.video;

import androidx.annotation.Nullable;

import com.cloudwebrtc.webrtc.LocalTrack;

import org.webrtc.VideoFrame;
import org.webrtc.VideoProcessor;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class LocalVideoTrack extends LocalTrack implements VideoProcessor {
    public interface ExternalVideoFrameProcessing {
        /**
         * Process a video frame.
         * @param frame
         * @return The processed video frame.
         */
        public abstract VideoFrame onFrame(VideoFrame frame);
    }

    public LocalVideoTrack(VideoTrack videoTrack) {
        super(videoTrack);
    }

    List<ExternalVideoFrameProcessing> processors = new ArrayList<>();

    public void addProcessor(ExternalVideoFrameProcessing processor) {
        synchronized (processors) {
            processors.add(processor);
        }
    }

    public void removeProcessor(ExternalVideoFrameProcessing processor) {
        synchronized (processors) {
            processors.remove(processor);
        }
    }

    private VideoSink sink = null;

    @Override
    public void setSink(@Nullable VideoSink videoSink) {
        sink = videoSink;
    }

    @Override
    public void onCapturerStarted(boolean b) {}

    @Override
    public void onCapturerStopped() {}

    /**
     * The capturer owns the frame it hands in and releases it once this
     * returns, so a frame a processor puts in its place carries a reference
     * only this method can drop.
     */
    @Override
    public void onFrameCaptured(VideoFrame videoFrame) {
        if (sink == null) {
            return;
        }
        VideoFrame current = videoFrame;
        synchronized (processors) {
            for (ExternalVideoFrameProcessing processor : processors) {
                VideoFrame processed = processor.onFrame(current);
                if (processed == current) {
                    continue;
                }
                if (current != videoFrame) {
                    current.release();
                }
                current = processed;
            }
        }
        sink.onFrame(current);
        if (current != videoFrame) {
            current.release();
        }
    }
}
