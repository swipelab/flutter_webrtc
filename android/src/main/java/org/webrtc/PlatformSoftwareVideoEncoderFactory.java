package org.webrtc;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MediaCodec video encoders the platform implements in software.
 * HardwareVideoEncoderFactory takes only vendor-prefixed codecs and
 * SoftwareVideoEncoderFactory carries no H.264, so on a device with no vendor
 * H.264 encoder this factory is the only source of one.
 */
public class PlatformSoftwareVideoEncoderFactory implements VideoEncoderFactory {
    private static final int PERIODIC_KEY_FRAME_INTERVAL_S = 3600;

    private static final VideoCodecMimeType[] SUPPORTED_TYPES = {VideoCodecMimeType.VP8,
            VideoCodecMimeType.VP9, VideoCodecMimeType.H264, VideoCodecMimeType.AV1,
            VideoCodecMimeType.H265};

    @Nullable
    @Override
    public VideoEncoder createEncoder(VideoCodecInfo input) {
        VideoCodecMimeType type;
        try {
            type = VideoCodecMimeType.valueOf(input.name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        MediaCodecInfo codec = findCodecForType(type);
        if (codec == null) {
            return null;
        }
        // Only the baseline profile is advertised, so a high-profile request has
        // no encoder here.
        if (type == VideoCodecMimeType.H264
                && !H264Utils.isSameH264Profile(
                        input.params, MediaCodecUtils.getCodecProperties(type, false))) {
            return null;
        }
        Integer colorFormat = MediaCodecUtils.selectColorFormat(
                MediaCodecUtils.ENCODER_COLOR_FORMATS, codec.getCapabilitiesForType(type.mimeType()));
        if (colorFormat == null) {
            return null;
        }
        // A null shared context takes the encoder's YUV input path.
        return new HardwareVideoEncoder(new MediaCodecWrapperFactoryImpl(), codec.getName(), type,
                /* surfaceColorFormat= */ null, colorFormat, input.params,
                PERIODIC_KEY_FRAME_INTERVAL_S, /* forcedKeyFrameIntervalMs= */ 0,
                new BaseBitrateAdjuster(), /* sharedContext= */ null);
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        List<VideoCodecInfo> codecs = new ArrayList<>();
        for (VideoCodecMimeType type : SUPPORTED_TYPES) {
            if (findCodecForType(type) != null) {
                codecs.add(new VideoCodecInfo(type.name(),
                        MediaCodecUtils.getCodecProperties(type, false), new ArrayList<>()));
            }
        }
        return codecs.toArray(new VideoCodecInfo[0]);
    }

    @Nullable
    private MediaCodecInfo findCodecForType(VideoCodecMimeType type) {
        for (int i = 0; i < MediaCodecList.getCodecCount(); ++i) {
            MediaCodecInfo info;
            try {
                info = MediaCodecList.getCodecInfoAt(i);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (info == null || !info.isEncoder() || !MediaCodecUtils.isSoftwareOnly(info)) {
                continue;
            }
            if (MediaCodecUtils.codecSupportsType(info, type)
                    && MediaCodecUtils.selectColorFormat(MediaCodecUtils.ENCODER_COLOR_FORMATS,
                               info.getCapabilitiesForType(type.mimeType()))
                            != null) {
                return info;
            }
        }
        return null;
    }
}
