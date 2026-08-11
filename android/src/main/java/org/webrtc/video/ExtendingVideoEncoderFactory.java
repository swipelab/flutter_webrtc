package org.webrtc.video;

import androidx.annotation.Nullable;

import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The preferred factory's capability set, extended with the codecs only the
 * extension carries. Preferred entries keep their order and stay ahead of the
 * extension's, and every codec the preferred factory names is encoded by it —
 * the extension answers for nothing else.
 */
public class ExtendingVideoEncoderFactory implements VideoEncoderFactory {
    private final VideoEncoderFactory preferred;
    private final VideoEncoderFactory extension;

    public ExtendingVideoEncoderFactory(
            VideoEncoderFactory preferred, VideoEncoderFactory extension) {
        this.preferred = preferred;
        this.extension = extension;
    }

    @Nullable
    @Override
    public VideoEncoder createEncoder(VideoCodecInfo info) {
        return named(preferred.getSupportedCodecs(), info.name) ? preferred.createEncoder(info)
                                                                : extension.createEncoder(info);
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        VideoCodecInfo[] preferredCodecs = preferred.getSupportedCodecs();
        List<VideoCodecInfo> codecs = new ArrayList<>(Arrays.asList(preferredCodecs));
        for (VideoCodecInfo info : extension.getSupportedCodecs()) {
            if (!named(preferredCodecs, info.name)) {
                codecs.add(info);
            }
        }
        return codecs.toArray(new VideoCodecInfo[0]);
    }

    private static boolean named(VideoCodecInfo[] codecs, String name) {
        for (VideoCodecInfo info : codecs) {
            if (info.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
