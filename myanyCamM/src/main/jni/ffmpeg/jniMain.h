#ifndef jniMain_h
#define jniMain_h

//#define IS_TEST


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <pthread.h>

#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <EGL/egl.h>



extern "C"{
#include "jni.h"
#include "jpeg.h"
#include "GLES/gl.h"
#include "GLES/glext.h"
#include <SLES/OpenSLES.h>
#include "SLES/OpenSLES_Android.h"
#include "ffmpeg/libavcodec/avcodec.h"
#include "ffmpeg/libavformat/avformat.h"
#include "ffmpeg/libswscale/swscale.h"
#include "ffmpeg/libavutil/log.h"
}


#define CLASS_PATH_NAME "com/thSDK/lib"
#define LOG_TAG    "jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef  uint8_t u8  ;
typedef char char1024[1024];


typedef struct videoCfg{
#ifdef IS_RENDER_YUV420
        GLuint gl_texture[3];
        GLint _uniformSamplers[3];
#else
        GLuint gl_texture;
#endif
        int ScreenWidth;
        int ScreenHeight;
#define TEXTURE_WIDTH 512
#define TEXTURE_HEIGHT 256
        u8 bufferRGB565[sizeof(uint16_t)*TEXTURE_WIDTH*TEXTURE_HEIGHT];
        //#define MAX_DECODE_WIDTH  1920
        //#define MAX_DECODE_HEIGHT 1080
        //u8 bufferRGB565[MAX_DECODE_WIDTH * MAX_DECODE_HEIGHT * 3];
        
        char1024 FileName_Jpg;
        char1024 FileName_Rec;
        
        struct SwsContext* sws;
        AVCodec* CodecV;
        AVCodecContext* ContextV;
        AVFrame* Frame422;
        AVFrame* FrameRGB565;//tmpAV
        //rec
        AVFormatContext* avContext;
        AVStream* avStreamV;
        bool isEncoding;
        
        u8* bufferRecYUV420;
        u8 avEncBuf[1000*200];
        AVFrame* avFrameV;
        AVOutputFormat* avfmt;
    
        
        char DevIP[100];
        int DevPort;
        char UserName[100];
        char Password[100];
        char UID[100];
        char UIDPsd[100];
        char RemoteFileName[100];
        
        bool IsSnapShot;
        bool IsRec; // 是否要
        bool Flag_StartRec; // 如果第一次录制，置为true ，然后置为false
        bool Flag_StopRec;
        int RecFrameRate;
        
        int encWidth;
        int encHeight;
        
        int VideoType;
        int IFrameFlag;
    
    uint8_t *sps;
    int spsSize;
    uint8_t *pps;
    int ppsSize;
    }MyVideo;


int jvideo_decode_init(JNIEnv* env, jclass obj,int type,int width,int height);

int jvideo_decode_frame(JNIEnv* env, jclass obj, jbyteArray data, int Len);
int video_decode_Free();
int jlocal_StopRec(JNIEnv* env, jclass obj);
int jlocal_StartRec(JNIEnv* env, jclass obj, jstring nFilename);



//render
int jopengl_Resize(JNIEnv* env, jclass obj, int w, int h);
int jopengl_Render(JNIEnv* env, jclass obj);
int jopenglInit(JNIEnv* env, jclass obj, jobject h,jint width,jint height);
int jlocal_SnapShot(JNIEnv* env, jclass obj, jstring nFileName);
void video_decode_SnapShot();
bool isIdrFrame(uint8_t* buf, int len);
void parseFrame(uint8_t*buf, int len);
void getSpsAndPpsInfo(uint8_t*buf ,int size);
void video_decode_Record(AVFrame * pFrame);
void video_decode_Record1(u8* Buf, int Len);
void video_decode_Record2(u8* Buf, int Len);
long long current_timestamp();
#endif


