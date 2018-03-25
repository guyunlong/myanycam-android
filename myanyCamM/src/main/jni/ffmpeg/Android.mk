LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
SDL_PATH := ../SDL
LOCAL_MODULE    := ffmpegutils

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SDL_PATH)/include $(LOCAL_PATH)/ffmpeg 
LOCAL_SRC_FILES := jniMain.cpp shaderUtils.cpp
LOCAL_CFLAGS := -D__STDC_CONSTANT_MACROS -Wno-sign-compare -Wno-switch  -DHAVE_NEON=1 \
      -mfpu=neon -mfloat-abi=softfp -fPIC -DANDROID
LOCAL_LDLIBS := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib -L$(LOCAL_PATH) -lavformat -lavcodec -lavdevice -lavfilter -lavutil -lswscale -llog -ljnigraphics -lz -ldl -lgcc -lGLESv1_CM -lEGL -lGLESv2  -landroid
LOCAL_DISABLE_FATAL_LINKER_WARNINGS=true
LOCAL_CFLAGS += -D GL_GLEXT_PROTOTYPES
include $(BUILD_SHARED_LIBRARY)


