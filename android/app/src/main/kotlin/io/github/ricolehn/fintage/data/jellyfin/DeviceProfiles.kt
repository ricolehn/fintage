package io.github.ricolehn.fintage.data.jellyfin

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.Display

/**
 * Describes what this phone can play, from the decoders it actually has. The server then plays files
 * directly when it can, and converts only the parts the phone cannot decode.
 */
object DeviceProfiles {
    private val codecs: List<MediaCodecInfo> by lazy {
        runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { !it.isEncoder } }
            .getOrDefault(emptyList())
    }

    private fun hasDecoder(mime: String): Boolean =
        codecs.any { info -> info.supportedTypes.any { it.equals(mime, ignoreCase = true) } }

    private fun supportsHevcMain10(): Boolean = codecs.any { info ->
        info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, true) } && runCatching {
            info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_HEVC).profileLevels.any {
                it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                    it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
            }
        }.getOrDefault(false)
    }

    private fun hdrTypes(context: Context): Set<Int> = runCatching {
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        if (Build.VERSION.SDK_INT >= 34) {
            display?.mode?.supportedHdrTypes?.toSet().orEmpty()
        } else {
            @Suppress("DEPRECATION")
            display?.hdrCapabilities?.supportedHdrTypes?.toSet().orEmpty()
        }
    }.getOrDefault(emptySet())

    fun build(context: Context, maxBitrate: Long): DeviceProfile {
        val video = buildList {
            add("h264")
            if (hasDecoder(MediaFormat.MIMETYPE_VIDEO_HEVC)) add("hevc")
            if (hasDecoder(MediaFormat.MIMETYPE_VIDEO_VP9)) add("vp9")
            if (hasDecoder(MediaFormat.MIMETYPE_VIDEO_VP8)) add("vp8")
            if (hasDecoder(MediaFormat.MIMETYPE_VIDEO_AV1)) add("av1")
            if (hasDecoder(MediaFormat.MIMETYPE_VIDEO_MPEG4)) add("mpeg4")
            if (hasDecoder(MediaFormat.MIMETYPE_VIDEO_MPEG2)) add("mpeg2video")
        }
        val audio = buildList {
            addAll(listOf("aac", "mp3", "mp2", "opus", "flac", "vorbis", "pcm_s16le", "pcm_s24le"))
            if (hasDecoder(MediaFormat.MIMETYPE_AUDIO_AC3)) add("ac3")
            if (hasDecoder(MediaFormat.MIMETYPE_AUDIO_EAC3)) add("eac3")
            if (Build.VERSION.SDK_INT >= 29 && hasDecoder(MediaFormat.MIMETYPE_AUDIO_AC4)) add("ac4")
            if (hasDecoder("audio/vnd.dts")) add("dts")
            if (hasDecoder("audio/vnd.dts.hd")) add("dca")
            if (hasDecoder("audio/true-hd")) add("truehd")
            if (hasDecoder("audio/alac")) add("alac")
        }
        val transcodeAudio = buildList {
            add("aac")
            add("mp3")
            if ("ac3" in audio) add("ac3")
            if ("eac3" in audio) add("eac3")
        }

        val hdr = hdrTypes(context)
        val hdr10 = hdr.contains(Display.HdrCapabilities.HDR_TYPE_HDR10)
        val hlg = hdr.contains(Display.HdrCapabilities.HDR_TYPE_HLG)
        val hdr10Plus = hdr.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS)
        val dolbyVision = hasDecoder(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)
        val ranges = buildList {
            add("SDR")
            if (hdr10) addAll(listOf("HDR10", "DOVIWithHDR10"))
            if (hdr10Plus) addAll(listOf("HDR10Plus", "DOVIWithHDR10Plus"))
            if (hlg) addAll(listOf("HLG", "DOVIWithHLG"))
            add("DOVIWithSDR")
            if (dolbyVision) addAll(listOf("DOVI", "DOVIWithEL", "DOVIWithELHDR10Plus"))
        }.joinToString("|")

        val codecProfiles = buildList {
            // most phones decode H.264 only with 8 bits
            add(
                CodecProfile(
                    type = "Video",
                    codec = "h264",
                    conditions = listOf(ProfileCondition("LessThanEqual", "VideoBitDepth", "8")),
                ),
            )
            if ("hevc" in video) {
                val conditions = buildList {
                    add(ProfileCondition("EqualsAny", "VideoRangeType", ranges))
                    if (!supportsHevcMain10()) add(ProfileCondition("LessThanEqual", "VideoBitDepth", "8"))
                }
                add(CodecProfile(type = "Video", codec = "hevc", conditions = conditions))
            }
            if ("av1" in video || "vp9" in video) {
                add(
                    CodecProfile(
                        type = "Video",
                        codec = listOf("av1", "vp9").filter { it in video }.joinToString(","),
                        conditions = listOf(ProfileCondition("EqualsAny", "VideoRangeType", ranges)),
                    ),
                )
            }
        }

        return DeviceProfile(
            name = "Fintage for Android",
            maxStreamingBitrate = maxBitrate,
            maxStaticBitrate = maxOf(maxBitrate, 200_000_000L),
            directPlayProfiles = listOf(
                DirectPlayProfile(
                    container = "mp4,m4v,mkv,webm,mov,ts,mpegts,m2ts,3gp",
                    type = "Video",
                    videoCodec = video.joinToString(","),
                    audioCodec = audio.joinToString(","),
                ),
                DirectPlayProfile(container = "mp3,aac,m4a,flac,ogg,opus,wav,webma", type = "Audio"),
            ),
            transcodingProfiles = listOf(
                TranscodingProfile(
                    container = "ts",
                    type = "Video",
                    videoCodec = if ("hevc" in video) "h264,hevc" else "h264",
                    audioCodec = transcodeAudio.joinToString(","),
                    protocol = "hls",
                    maxAudioChannels = "6",
                    minSegments = 1,
                    breakOnNonKeyFrames = true,
                ),
                TranscodingProfile(
                    container = "mp3",
                    type = "Audio",
                    videoCodec = "",
                    audioCodec = "mp3",
                    protocol = "http",
                    maxAudioChannels = "2",
                ),
            ),
            codecProfiles = codecProfiles,
            subtitleProfiles = listOf(
                SubtitleProfile("srt", "External"),
                SubtitleProfile("subrip", "External"),
                SubtitleProfile("ass", "External"),
                SubtitleProfile("ssa", "External"),
                SubtitleProfile("vtt", "External"),
                SubtitleProfile("webvtt", "External"),
                SubtitleProfile("ttml", "External"),
                SubtitleProfile("pgs", "Encode"),
                SubtitleProfile("pgssub", "Encode"),
                SubtitleProfile("dvdsub", "Encode"),
                SubtitleProfile("dvbsub", "Encode"),
            ),
        )
    }
}
