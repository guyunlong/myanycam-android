LOCAL_PATH := $(call my-dir)

$(warning ${LOCAL_PATH})
$(warning ${TARGET_ARCH_ABI})
$(warning ${LOCAL_PATH})

include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeg
LOCAL_SRC_FILES :=${TARGET_ARCH_ABI}/libffmpeg.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ffmpeg
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
SDL_PATH := ../SDL

LOCAL_MODULE    := ffmpegutils

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SDL_PATH)/include $(LOCAL_PATH)/ffmpeg 
LOCAL_SRC_FILES := jniMain.cpp shaderUtils.cpp ffmpeg_mp4.cpp\
jpeg.c \
libjpeg/jcapimin.c libjpeg/jcapistd.c libjpeg/jccoefct.c libjpeg/jccolor.c libjpeg/jcdctmgr.c libjpeg/jchuff.c \
libjpeg/jcinit.c libjpeg/jcmainct.c libjpeg/jcmarker.c libjpeg/jcmaster.c libjpeg/jcomapi.c libjpeg/jcparam.c \
libjpeg/jcphuff.c libjpeg/jcprepct.c libjpeg/jcsample.c libjpeg/jctrans.c libjpeg/jdapimin.c libjpeg/jdapistd.c \
libjpeg/jdatadst.c libjpeg/jdatasrc.c libjpeg/jdcoefct.c libjpeg/jdcolor.c libjpeg/jddctmgr.c libjpeg/jdhuff.c \
libjpeg/jdinput.c libjpeg/jdmainct.c libjpeg/jdmarker.c libjpeg/jdmaster.c libjpeg/jdmerge.c libjpeg/jdphuff.c \
libjpeg/jdpostct.c libjpeg/jdsample.c libjpeg/jdtrans.c libjpeg/jerror.c libjpeg/jfdctflt.c libjpeg/jfdctfst.c \
libjpeg/jfdctint.c libjpeg/jidctflt.c libjpeg/jidctfst.c libjpeg/jidctint.c libjpeg/jidctred.c libjpeg/jquant1.c \
libjpeg/jquant2.c libjpeg/jutils.c libjpeg/jmemmgr.c libjpeg/jmemansi.c

LOCAL_CFLAGS := -D__STDC_CONSTANT_MACROS -Wno-sign-compare -Wno-switch  -DHAVE_NEON=1 \
      -mfpu=neon -mfloat-abi=softfp -fPIC -DANDROID
LOCAL_LDLIBS := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib -L$(LOCAL_PATH) -llog -ljnigraphics -lz -ldl -lgcc -lGLESv1_CM -lEGL -lGLESv2  -landroid -fPIC
LOCAL_SHARED_LIBRARIES := ffmpeg
LOCAL_DISABLE_FATAL_LINKER_WARNINGS=true
LOCAL_CFLAGS += -D GL_GLEXT_PROTOTYPES
include $(BUILD_SHARED_LIBRARY)


