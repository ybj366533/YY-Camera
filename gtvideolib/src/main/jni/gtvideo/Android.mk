LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

GTVIDEO_ROOT=$(LOCAL_PATH)/../../../../../../../
GTVIDEO_WEBP_ROOT=$(LOCAL_PATH)/../webp
GTVIDEO_FFMPEG_ROOT=$(GTVIDEO_ROOT)/ffmpeg/android
GTVIDEO_LIBRARY_ROOT=$(GTVIDEO_ROOT)/GTVideoPlayerLib

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(GTVIDEO_FFMPEG_ROOT)/ffmpeg-armv7a/include/
LOCAL_C_INCLUDES += $(GTVIDEO_WEBP_ROOT)/include/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/logger/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/logger/android/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/object/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/player/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/receiver/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/segments/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/remuxer/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/utils/
LOCAL_C_INCLUDES += $(GTVIDEO_LIBRARY_ROOT)/wrapper/android/

LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/player/gtv_player.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/player/gtv_demuxer.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/receiver/gtv_ffmpeg_reader.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/segments/gtv_segments.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/remuxer/gtv_remuxer.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/remuxer/gtv_transcode.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/remuxer/gtv_mp4Remux.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/object/gtv_data_buffer.c	
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/object/gtv_data_queue.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/object/gtv_raw_frame.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/utils/gtv_com_utils.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/logger/gtv_logger.c
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/wrapper/android/gtvvideosegments_jni.cpp
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/wrapper/android/gtvmp4remuxer_jni.cpp
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/wrapper/android/gtvideodemuxer_jni.cpp
LOCAL_SRC_FILES += $(GTVIDEO_LIBRARY_ROOT)/wrapper/android/gtvideoplayer_jni.cpp
			

LOCAL_LDLIBS += -lz -llog -landroid -lGLESv1_CM
LOCAL_LDFLAGS += -L$(GTVIDEO_FFMPEG_ROOT)/ffmpeg-armv7a/lib/ -lavformat -lavfilter -lswresample -lswscale -lavcodec -lavutil 

LOCAL_CFLAGS += -DTARGET_OS_ANDROID
LOCAL_CFLAGS += -std=c99

LOCAL_SHARED_LIBRARIES := libimgtowebp libx264

# -std=c99

LOCAL_MODULE := gtvideo

include $(BUILD_SHARED_LIBRARY)

