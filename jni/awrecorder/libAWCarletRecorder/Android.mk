LOCAL_PATH := $(call my-dir)

#######################

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := eng optional
LOCAL_PREBUILT_LIBS := libAWCarletRecorder.so
include $(BUILD_MULTI_PREBUILT)

#######################