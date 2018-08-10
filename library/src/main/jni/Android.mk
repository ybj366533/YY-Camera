LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := gpuimage

LOCAL_SRC_FILES := yuv-decoder.c

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)