LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

VIDEO_ROOT=$(LOCAL_PATH)/
VIDEO_LIBRARY_ROOT=$(VIDEO_ROOT)/
GTPNG_ROOT=$(LOCAL_PATH)/../png

LOCAL_CPPFLAGS += -fexceptions -mfloat-abi=softfp -mfpu=neon
LOCAL_CPPFLAGS += -frtti -std=c++11 
LOCAL_CPPFLAGS += -fsigned-char -Wno-error=format-security

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/json/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/logger/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/logger/android/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/wrapper/android/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/render/android/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/player/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/render/
LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/png/

LOCAL_C_INCLUDES += $(VIDEO_LIBRARY_ROOT)/lib-sdm-core/
LOCAL_C_INCLUDES += $(GTPNG_ROOT)/

LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/player/sticker_player.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/render/sticker_render.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/render/android/AndroidRenderAPI.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/png/YY_png.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/json/cJSON.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/json/cJSON_Utils.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sticker-core/logger/sticker_logger.c

LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sdm-core/feature_descriptor.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sdm-core/hog.c
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sdm-core/ldmarkmodel.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sdm-core/matrix.cpp
LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sdm-core/SdmTracker.cpp

LOCAL_SRC_FILES += $(VIDEO_LIBRARY_ROOT)/lib-sdm-core/libsdm_jni.cpp

LOCAL_LDLIBS += -lGLESv2 -lz -llog -landroid -lm

LOCAL_CFLAGS += -DTARGET_OS_ANDROID
#LOCAL_CFLAGS += -std=c99

# -std=c99

LOCAL_STATIC_LIBRARIES += png

LOCAL_MODULE := yysticker

include $(BUILD_SHARED_LIBRARY)

