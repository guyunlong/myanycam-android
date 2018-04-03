#include "com_greatcompany_myteam_ref_JNITest.h"
#undef LOG
#include <android/log.h>
#include "SDL_video.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#define BENCHMARK_SDL

#define NOTICE(X)	printf("%s", X);

#define WINDOW_WIDTH  176
#define WINDOW_HEIGHT 144
//#define WINDOW_WIDTH  176
//#define WINDOW_HEIGHT 144

#include "SDL.h"
#include "SDL_config.h"
/* Include the SDL main definition header */
#include "SDL_main.h"
/*******************************************************************************
 Functions called by JNI
 *******************************************************************************/
#include <jni.h>
#include <android/log.h>
#define LOG_TAG "JNITest_native"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

// Called before SDL_main() to initialize JNI bindings in SDL library
void SDL_Android_Init(JNIEnv* env, jclass cls);

// Library init
jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	return JNI_VERSION_1_4;
}



SDL_Surface *screen;
SDL_Overlay *overlay;
int w, h;

static void quit(int rc) {
	SDL_Quit();
	exit(rc);
}

int desired_bpp;
Uint32 video_flags, overlay_format;
FILE* fp;
unsigned char* pY;
unsigned char* pU;
unsigned char* pV;
int iRet;
SDL_Rect rect;

#ifdef BENCHMARK_SDL
Uint32 then, now;
#endif

void Java_com_greatcompany_myteam_ref_JNITest_onNativeResize(
                                    JNIEnv* env, jclass jcls,
                                    jint width, jint height, jint format)
{
	LOGE("DLActivity_onNativeResize..");

    Android_SetScreenResolution(width, height, format);
}

JNIEXPORT jstring JNICALL Java_com_greatcompany_myteam_ref_JNITest_initSDL(
		JNIEnv * env, jclass cls) {

	SDL_Android_Init(env, cls);

		/* Set default options and check command-line */
		overlay_format = SDL_YV12_OVERLAY; //yes
//		overlay_format = SDL_IYUV_OVERLAY; //yes
	//	overlay_format = SDL_YUY2_OVERLAY; //no
		//overlay_format = SDL_UYVY_OVERLAY; //no
		//overlay_format = SDL_YVYU_OVERLAY; //no

		w = WINDOW_WIDTH;
		h = WINDOW_HEIGHT;

		if (SDL_Init(SDL_INIT_VIDEO) < 0) {
			quit(1);
		}

	    atexit(SDL_Quit);

		/* Initialize the display */
		LOGE( "SDL_SetVideoMode start!");
		//	screen = SDL_SetVideoMode(w, h, desired_bpp, video_flags);
		screen = SDL_SetVideoMode(w, h, 0, 0);
		if (screen == NULL) {
			quit(1);
		}

		/* Create the overlay */
		LOGE("SDL_CreateYUVOverlay start!\n");
		overlay = SDL_CreateYUVOverlay(w, h, overlay_format, screen);
		if (overlay == NULL) {
			quit(1);
		}

		SDL_WM_SetCaption("Play YUV", "yang test");
		LOGE("SDL_CreateYUVOverlay end!\n");

	return (*env)->NewStringUTF(env, (char *) "initSDL success");
}
//
//JNIEXPORT jstring JNICALL Java_com_greatcompany_myteam_ref_JNITest_convertBytesToVideo(
//		JNIEnv * env, jclass cls, jbyteArray PY, jbyteArray PV, jbyteArray PU) {
//	pY = PY;
//	pV = PV;
//	pU = PU;
//	LOGE("while loop start!\n");
//
//	iRet = SDL_LockSurface(screen);
//	iRet = SDL_LockYUVOverlay(overlay);
//	memcpy(overlay->pixels[0], pY, w * h);
//	LOGE("vvv");
//	memcpy(overlay->pixels[1], pV, w * h / 4);
//	memcpy(overlay->pixels[2], pU, w * h / 4);
//	SDL_UnlockYUVOverlay(overlay);
//	SDL_UnlockSurface(screen);
//
//	rect.w = w;
//	rect.h = h;
//	rect.x = rect.y = 0;
//	iRet = SDL_DisplayYUVOverlay(overlay, &rect);
//	SDL_Delay(40);
//	LOGE("native 转换完成...");
//
//	return (*env)->NewStringUTF(env, (char *) "playe video finish");
//}

JNIEXPORT jstring JNICALL Java_com_greatcompany_myteam_ref_JNITest_convertBytesToVideo(
		JNIEnv * env, jclass cls, jbyteArray PY, jbyteArray PV, jbyteArray PU) {
	pY = PY;
	pV = PV;
	pU = PU;

    int i = 1;
    int x, y;
    int w = 176;
    int h = 144;
    char c = 'n';
	FILE * fp;
	char filename[64];
	unsigned char* pY;
	unsigned char* pU;
	unsigned char* pV;
	LOGE("while loop start!\n");

//	memcpy(overlay->pixels[0], pY, w * h);
//	LOGE("vvv");
//	memcpy(overlay->pixels[1], pV, w * h / 4);
//	memcpy(overlay->pixels[2], pU, w * h / 4);
    pY = (unsigned char*)malloc(w*h);
    pU = (unsigned char*)malloc(w*h/4);
    pV = (unsigned char*)malloc(w*h/4);
    while (i<=96)
    {
    	iRet = SDL_LockSurface(screen);
    	iRet = SDL_LockYUVOverlay(overlay);
        sprintf(filename, "/sdcard/a.yuv", i);
        fp = fopen(filename, "rb");
          if (fp == NULL)
          {
              fprintf(stderr, "open file error!\n");
              exit(1);
          }

          fread(pY, 1, w*h, fp);
          fread(pU, 1, w*h/4, fp);
          fread(pV, 1, w*h/4, fp);
          memcpy(overlay->pixels[0], pY, w*h);
          memcpy(overlay->pixels[1], pV, w*h/4);
          memcpy(overlay->pixels[2], pU, w*h/4);
    	SDL_UnlockYUVOverlay(overlay);
    	SDL_UnlockSurface(screen);
    	 fclose(fp);
    	rect.w = w;
    	rect.h = h;
    	rect.x = rect.y = 0;
    	iRet = SDL_DisplayYUVOverlay(overlay, &rect);
    	SDL_Delay(40);
    	  i += 1;

    	LOGE("native 转换完成...");
}


	return (*env)->NewStringUTF(env, (char *) "playe video finish");
}

JNIEXPORT jstring JNICALL Java_com_greatcompany_myteam_ref_JNITest_destroySDL(
		JNIEnv * env, jclass cls) {
//		fclose(fp);
	free(pY);
	free(pU);
	free(pV);
	SDL_FreeYUVOverlay(overlay);
	SDL_FreeSurface(screen);
	SDL_Quit();
	return (*env)->NewStringUTF(env, (char *) "destroySDL success");
}

//JNIEXPORT jstring JNICALL Java_com_greatcompany_myteam_ref_JNITest_convertBytesToVideo(
//		JNIEnv * env, jclass cls) {
//	pY = (unsigned char*) malloc(w * h);
//	pU = (unsigned char*) malloc(w * h / 4);
//	pV = (unsigned char*) malloc(w * h / 4);
//
//	fp = fopen("/sdcard/DCIM/akiyo_qcif.yuv", "rb");
////		fp = fopen("/sdcard/DCIM/show.yuv", "rb");
//	if (fp == NULL) {
//		quit(1);
//	}
//
//	fseek(fp, 0, 2);
//	int yuvlen = ftell(fp);
//	fseek(fp, 0, 0);
//	int frame_num = yuvlen / (w * h * 3 / 2);
//	__android_log_print(ANDROID_LOG_INFO, "jni", "while loop start!\n");
//	while (frame_num--) {
//		iRet = SDL_LockSurface(screen);
//		iRet = SDL_LockYUVOverlay(overlay);
//
//		fread(pY, 1, w * h, fp);
//		fread(pU, 1, w * h / 4, fp);
//		fread(pV, 1, w * h / 4, fp);
//		memcpy(overlay->pixels[0], pY, w * h);
//		memcpy(overlay->pixels[1], pV, w * h / 4);
//		memcpy(overlay->pixels[2], pU, w * h / 4);
//		SDL_UnlockYUVOverlay(overlay);
//		SDL_UnlockSurface(screen);
//
//		rect.w = w;
//		rect.h = h;
//		rect.x = rect.y = 0;
//		iRet = SDL_DisplayYUVOverlay(overlay, &rect);
//		SDL_Delay(40);
//	}
//	return (*env)->NewStringUTF(env, (char *) "playe video finish");
//}

