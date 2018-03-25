#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>

#define  LOG_TAG    "myanycam_native"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

/* Cheat to keep things simple and just use some globals. */
AVFormatContext *pFormatCtx;
AVCodecContext *pCodecCtx;
AVFrame *pFrame;
AVFrame *pFrameRGB;
int videoStream;
struct SwsContext *img_convert_ctx;

/*
 * Write a frame worth of video (in pFrame) into the Android bitmap
 * described by info using the raw pixel buffer.  It's a very inefficient
 * draw routine, but it's easy to read. Relies on the format of the
 * bitmap being 8bits per color component plus an 8bit alpha channel.
 */

static void fill_bitmap(AndroidBitmapInfo* info, void *pixels, AVFrame *pFrame) {
	uint8_t *frameLine;
	int yy;

	for (yy = 0; yy < info->height; yy++) {
		uint8_t* line = (uint8_t*) pixels;
		frameLine = (uint8_t *) pFrame->data[0] + (yy * pFrame->linesize[0]);

		int xx;
		for (xx = 0; xx < info->width; xx++) {
			int out_offset = xx * 4;
			int in_offset = xx * 3;

			line[out_offset] = frameLine[in_offset];
			line[out_offset + 1] = frameLine[in_offset + 1];
			line[out_offset + 2] = frameLine[in_offset + 2];
			line[out_offset + 3] = 0XFF;
		}
		pixels = (char*) pixels + info->stride;
	}
}

void Java_gyl_cam_recThread_init(JNIEnv * env, jobject this, jint type,
		jint width, jint height) {
	uint8_t *buffer;
	int numBytes;
	av_register_all();
//	avcodec_init();
	AVCodec * pCodec;
	if (0 == type) {

		LOGE("mjpeg codec");
		pCodec = avcodec_find_decoder(CODEC_ID_MJPEG);
	}
	if (1 == type) {
		LOGE("h264 codec");
		pCodec = avcodec_find_decoder(CODEC_ID_H264);
	}

	pCodecCtx = avcodec_alloc_context();

	pCodecCtx->time_base.num = 1; //这两行：一秒钟25帧
	pCodecCtx->time_base.den = 25;
	pCodecCtx->bit_rate = 0; //初始化为0
	pCodecCtx->frame_number = 1; //每包一个视频帧
	pCodecCtx->codec_type = AVMEDIA_TYPE_VIDEO;
	pCodecCtx->width = width; //这两行：视频的宽度和高度
	pCodecCtx->height = height;
	pCodecCtx->pix_fmt = PIX_FMT_YUV420P;
	if (avcodec_open(pCodecCtx, pCodec) < 0) {

		LOGE("Unable to open codec");
		return;
	}

	pFrame = avcodec_alloc_frame();
	pFrameRGB = avcodec_alloc_frame();
	if (pFrameRGB == NULL) {
		return;
	}

	numBytes = avpicture_get_size(PIX_FMT_RGB24, pCodecCtx->width,
			pCodecCtx->height);
	buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));

	avpicture_fill((AVPicture *) pFrameRGB, buffer, PIX_FMT_RGB24,
			pCodecCtx->width, pCodecCtx->height);

	img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height,
			pCodecCtx->pix_fmt, width, height, PIX_FMT_RGB24, SWS_BICUBIC, NULL,
			NULL, NULL);
	if (img_convert_ctx == NULL) {
		LOGE("could not initialize conversion context\n");
		return;
	}
}

jint Java_gyl_cam_recThread_DecodeFrame(JNIEnv * env, jobject this,
		jstring bitmap, jbyteArray in, jint nallen) {
//	LOGE("^^^^^^^^^^^^^^^^^^1");
	AndroidBitmapInfo info;
	void* pixels;
	int ret;

	int err;
	int i;

	AVPacket packet;

	static int bytesRemaining;
	static uint8_t *rawData;
	int bytesDecoded;
	int frameFinished = 0;
	int bOk = 0;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return -1;
	}
	LOGE("Checked on the bitmap");

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
	}
	LOGE("Grabbed the pixels");

	jbyte * Buf = (jbyte*) (*env)->GetByteArrayElements(env, in, 0);
	bytesRemaining = nallen;


	rawData = (uint8_t*) Buf;
	packet.data = rawData;
	packet.size = nallen;
//	LOGE("^^^^^^^^^^^^^^^^^^2");

	bytesDecoded = avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished,
			&packet);
	if (bytesDecoded < 0)
		return -1;

	LOGE("^^^^^^^^^^^^^^^^^^^3");
	if (frameFinished) {


		// This is much different than the tutorial, sws_scale
		// replaces img_convert, but it's not a complete drop in.
		// This version keeps the image the same size but swaps to
		// RGB24 format, which works perfect for PPM output.

		sws_scale(img_convert_ctx, (const uint8_t* const *) pFrame->data,
				pFrame->linesize, 0, pCodecCtx->height, pFrameRGB->data,
				pFrameRGB->linesize);
		fill_bitmap(&info, pixels, pFrameRGB);
	}
	//  av_free_packet(&packet);
	AndroidBitmap_unlockPixels(env, bitmap);
	(*env)->ReleaseByteArrayElements(env, in, Buf, JNI_FALSE);
	LOGE("^^^^^^^^^^^^^^^^^^4");
	return bytesDecoded;
}

