LOCAL_PATH:= $(call my-dir)

ifneq ($(PRODUCT_PREBUILT_LIB_JNI_MOSAIC),yes)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/feature_stab/db_vlvm \
        $(LOCAL_PATH)/feature_stab/src \
        $(LOCAL_PATH)/feature_stab/src/dbreg \
        $(LOCAL_PATH)/feature_mos/src \
        $(LOCAL_PATH)/feature_mos/src/mosaic

LOCAL_CFLAGS := -O3 -DNDEBUG -fstrict-aliasing

LOCAL_SRC_FILES := \
        feature_mos_jni.cpp \
        mosaic_renderer_jni.cpp \
        feature_mos/src/mosaic/trsMatrix.cpp \
        feature_mos/src/mosaic/AlignFeatures.cpp \
        feature_mos/src/mosaic/Blend.cpp \
        feature_mos/src/mosaic/Delaunay.cpp \
        feature_mos/src/mosaic/ImageUtils.cpp \
        feature_mos/src/mosaic/Mosaic.cpp \
        feature_mos/src/mosaic/Pyramid.cpp \
        feature_mos/src/mosaic_renderer/Renderer.cpp \
        feature_mos/src/mosaic_renderer/WarpRenderer.cpp \
        feature_mos/src/mosaic_renderer/SurfaceTextureRenderer.cpp \
        feature_mos/src/mosaic_renderer/YVURenderer.cpp \
        feature_mos/src/mosaic_renderer/FrameBuffer.cpp \
        feature_stab/db_vlvm/db_feature_detection.cpp \
        feature_stab/db_vlvm/db_feature_matching.cpp \
        feature_stab/db_vlvm/db_framestitching.cpp \
        feature_stab/db_vlvm/db_image_homography.cpp \
        feature_stab/db_vlvm/db_rob_image_homography.cpp \
        feature_stab/db_vlvm/db_utilities.cpp \
        feature_stab/db_vlvm/db_utilities_camera.cpp \
        feature_stab/db_vlvm/db_utilities_indexing.cpp \
        feature_stab/db_vlvm/db_utilities_linalg.cpp \
        feature_stab/db_vlvm/db_utilities_poly.cpp \
        feature_stab/src/dbreg/dbreg.cpp \
        feature_stab/src/dbreg/dbstabsmooth.cpp \
        feature_stab/src/dbreg/vp_motionmodel.c

ifeq ($(TARGET_ARCH), arm)
        LOCAL_SDK_VERSION := 9
endif

ifeq ($(TARGET_ARCH), x86)
        LOCAL_SDK_VERSION := 9
endif

ifeq ($(TARGET_ARCH), mips)
        LOCAL_SDK_VERSION := 9
endif

LOCAL_LDFLAGS := -llog -lGLESv2

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE    := libjni_mosaic
include $(BUILD_SHARED_LIBRARY)
endif

# TinyPlanet
ifneq ($(PRODUCT_PREBUILT_LIB_JNI_TINYPLANET),yes)
include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc
LOCAL_LDFLAGS   := -llog -ljnigraphics
LOCAL_SDK_VERSION := 9
LOCAL_MODULE    := libjni_tinyplanet
LOCAL_SRC_FILES := tinyplanet.cc

LOCAL_CFLAGS    += -ffast-math -O3 -funroll-loops
LOCAL_ARM_MODE := arm

include $(BUILD_SHARED_LIBRARY)

#awrecorder begin
include $(CLEAR_VARS)
LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libnativehelper \
    libutils \
    libbinder \
    libmedia \
    libskia \
    libui \
    liblog \
    libcutils \
    libgui \
    libstagefright \
    libstagefright_foundation \
    libcamera_client \
    libmtp \
    libusbhost \
    libexif \
    libstagefright_amrnb_common \
    libAWCarletRecorder

LOCAL_REQUIRED_MODULES := \
    libexif_jni

LOCAL_STATIC_LIBRARIES := \
    libstagefright_amrnbenc

LOCAL_C_INCLUDES += \
    external/jhead \
    external/tremor/Tremor \
    frameworks/base/core/jni \
    frameworks/av/media/libmedia \
    frameworks/av/media/libstagefright \
    frameworks/av/media/libstagefright/codecs/amrnb/enc/src \
    frameworks/av/media/libstagefright/codecs/amrnb/common \
    frameworks/av/media/libstagefright/codecs/amrnb/common/include \
    frameworks/av/media/mtp \
    frameworks/native/include/media/openmax \
    $(call include-path-for, libhardware)/hardware \
    system/media/camera/include \
    $(PV_INCLUDES) \
    $(JNI_H_INCLUDE) \
    $(call include-path-for, corecg graphics) \
    $(LOCAL_PATH)/jni/libAWCarletRecorder/include \

LOCAL_CFLAGS +=

LOCAL_LDLIBS := -lpthread
LOCAL_LDFLAGS   := -llog 
LOCAL_MODULE    := libAWRecorder_jni
LOCAL_SRC_FILES := ./awrecorder/AWRecorder.cpp ./awrecorder/AWRecorder_jni.cpp 
#LOCAL_SHARED_LIBRARIES := libGsensor

include $(BUILD_SHARED_LIBRARY)
include $(call all-makefiles-under,$(LOCAL_PATH)/awrecorder/)
#awrecorder end

endif

