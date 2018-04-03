#include "SDL.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include<android/log.h>
#define LOG_TAG "debug"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

void test(){
	LOGI("test");
}

int main(int argc , char* argv[])
{

    int i = 1;
    int x, y;
    int w = 176;
    int h = 144;
    char c = 'n';

    FILE* fp;
    char filename[64];
    unsigned char* pY;
    unsigned char* pU;
    unsigned char* pV;
    SDL_Rect rect;

    if (SDL_Init(SDL_INIT_VIDEO) < 0)
    {
        fprintf(stderr, "can not initialize SDL:%s\n", SDL_GetError());
        exit(1);
    }
    atexit(SDL_Quit);

    SDL_Surface* screen = SDL_SetVideoMode(w, h, 0, 0);
    if (screen == NULL)
    {
        fprintf(stderr, "create surface error!\n");
        exit(1);
    }

    SDL_Overlay* overlay = SDL_CreateYUVOverlay(w, h, SDL_YV12_OVERLAY, screen);
    if (overlay == NULL)
    {
        fprintf(stderr, "create overlay error!\n");
        exit(1);
    }

    printf("w:%d, h:%d, planes:%d\n", overlay->w, overlay->h, overlay->planes);
    printf("pitches:%d, %d, %d\n", overlay->pitches[0], overlay->pitches[1], overlay->pitches[2]);

    pY = (unsigned char*)malloc(w*h);
    pU = (unsigned char*)malloc(w*h/4);
    pV = (unsigned char*)malloc(w*h/4);


    while (i<=96)
    {
        SDL_LockSurface(screen);
        SDL_LockYUVOverlay(overlay);

        sprintf(filename, "/sdcard/a.yuv", i);
        printf("%s\n", filename);

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

        fclose(fp);

        SDL_UnlockYUVOverlay(overlay);
        SDL_UnlockSurface(screen);

        rect.w = w;
        rect.h = h;
        rect.x = rect.y = 0;
        SDL_DisplayYUVOverlay(overlay, &rect);

        SDL_Delay(40);

        i += 1;
    }

    free(pY);
    free(pU);
    free(pV);

    while (c != 'q')
        scanf("%c", &c);

    SDL_FreeYUVOverlay(overlay);
    SDL_FreeSurface(screen);

    return 0;
}
