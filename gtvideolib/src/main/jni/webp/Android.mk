LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PREBUILT_DIR := prebuilt
LOCAL_SRC_FILES := $(LOCAL_PREBUILT_DIR)/$(TARGET_ARCH_ABI)/libimgtowebp.so
LOCAL_MODULE := libimgtowebp
include $(PREBUILT_SHARED_LIBRARY)


