LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

VIDEO_ROOT=$(LOCAL_PATH)/
VIDEO_WEBP_ROOT=$(LOCAL_PATH)/../webp
VIDEO_FFMPEG_ROOT=$(VIDEO_ROOT)/ffmpeg/android
VIDEO_LIBRARY_ROOT=$(VIDEO_ROOT)/VideoPlayerLib

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(VIDEO_FFMPEG_ROOT)/ffmpeg-armv7a/include/
LOCAL_C_INCLUDES += $(VIDEO_WEBP_ROOT)/include/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/logger/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/logger/android/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/object/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/player/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/receiver/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/segments/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/remuxer/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/utils/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/wrapper/android/

LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/player/yy_player.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/player/yy_demuxer.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/receiver/yy_ffmpeg_reader.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/segments/yy_segments.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/remuxer/yy_remuxer.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/remuxer/yy_transcode.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/remuxer/yy_mp4Remux.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/object/yy_data_buffer.c	
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/object/yy_data_queue.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/object/yy_raw_frame.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/utils/yy_com_utils.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/logger/yy_logger.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/wrapper/android/videosegments_jni.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/wrapper/android/mp4remuxer_jni.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/wrapper/android/videodemuxer_jni.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/wrapper/android/videoplayer_jni.cpp
			

LOCAL_LDLIBS += -lz -llog -landroid -lGLESv1_CM
LOCAL_LDFLAGS += -L$(VIDEO_FFMPEG_ROOT)/ffmpeg-armv7a/lib/ -lavformat -lavfilter -lswresample -lswscale -lavcodec -lavutil 

LOCAL_CFLAGS += -DTARGET_OS_ANDROID
LOCAL_CFLAGS += -std=c99

LOCAL_SHARED_LIBRARIES := libimgtowebp libx264

# -std=c99

LOCAL_MODULE := yyvideo

include $(BUILD_SHARED_LIBRARY)

