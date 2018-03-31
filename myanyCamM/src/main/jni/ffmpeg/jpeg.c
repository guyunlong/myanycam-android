#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <linux/videodev.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/timeb.h>
//-----------------------------------------------------------------------------
#include "libjpeg/jpeglib.h"

#include <android/log.h>
#define LOG_TAG    "SouthTech"
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,__VA_ARGS__)
//-----------------------------------------------------------------------------
void JpegInitDestination(j_compress_ptr cinfo){}
int JpegEmptyOutputBuffer(j_compress_ptr cinfo){}
void JpegTermDestination(j_compress_ptr cinfo){}

int JpegCompress(int w,int h, const char* rgb_data, int rgb_size, char* jpeg_data, int* jpeg_size)
{

  struct jpeg_compress_struct cinfo;
  struct jpeg_error_mgr jerr;
  struct jpeg_destination_mgr jpegDstManager;
  int ret;
  unsigned char* srcBuf = (unsigned char*)malloc(w * 3);
  JSAMPROW rowPointer[1];
  rowPointer[0] = (JSAMPROW)srcBuf;
  int left_size;
  left_size = *jpeg_size;
  cinfo.err = jpeg_std_error(&jerr);
  jpeg_create_compress(&cinfo);
  cinfo.image_width = w;
  cinfo.image_height = h;
  cinfo.input_components = 3;
  cinfo.in_color_space = JCS_RGB;
  cinfo.raw_data_in = 1;
  jpeg_set_defaults(&cinfo);
  cinfo.dest = &jpegDstManager;

  jpegDstManager.next_output_byte = (unsigned char*)jpeg_data;
  jpegDstManager.free_in_buffer = left_size;
  jpegDstManager.init_destination = JpegInitDestination;
  jpegDstManager.empty_output_buffer = JpegEmptyOutputBuffer;
  jpegDstManager.term_destination = JpegTermDestination;
  //jpeg_set_quality(&cinfo, 20, 1);
  jpeg_start_compress(&cinfo, 1);    
  int y;
  for(y=0;y< h;y++)
  {
    rowPointer[0] = (unsigned char*)(rgb_data + y*w*3);
    ret = jpeg_write_scanlines(&cinfo, rowPointer, 1);
  }
  jpeg_finish_compress(&cinfo);    
  jpeg_destroy_compress(&cinfo);    
  *jpeg_size = left_size - jpegDstManager.free_in_buffer;
  return 1;
}
//-----------------------------------------------------------------------------
void JpegInitSource(j_decompress_ptr cinfo){}
int JpegFillInputBuffer(j_decompress_ptr cinfo){}
void JpegSkipInputData(j_decompress_ptr cinfo, long num_bytes){}
void JpegTermSource(j_decompress_ptr cinfo){}

int JpegUnCompress(const char* jpeg_data, int jpeg_size, char* rgb_data, int rgb_size, int w, int h)
{
  struct jpeg_decompress_struct cinfo;
  struct jpeg_error_mgr jerr;
  struct jpeg_source_mgr jpegSrcManager;
  int ret;
  JSAMPROW rowPointer[1];
  cinfo.err = jpeg_std_error(&jerr);
  jpeg_create_decompress(&cinfo);

  jpegSrcManager.init_source = JpegInitSource;
  jpegSrcManager.fill_input_buffer = JpegFillInputBuffer;
  jpegSrcManager.skip_input_data = JpegSkipInputData;
  jpegSrcManager.resync_to_restart = jpeg_resync_to_restart;
  jpegSrcManager.term_source = JpegTermSource;
  jpegSrcManager.next_input_byte = (unsigned char*)jpeg_data;
  jpegSrcManager.bytes_in_buffer = jpeg_size;
  cinfo.src = &jpegSrcManager;

  jpeg_read_header(&cinfo, 1);
  cinfo.out_color_space = JCS_RGB;
  jpeg_start_decompress(&cinfo);
  if( cinfo.output_width != (unsigned int)w && cinfo.output_height != (unsigned int)h)
  {
    jpeg_destroy_decompress(&cinfo);
    return 0;
  }
  int dy;
  for (dy = 0; cinfo.output_scanline < cinfo.output_height; dy++)
  {
    rowPointer[0] = (unsigned char *)(rgb_data + w*dy*3);
    ret = jpeg_read_scanlines(&cinfo, rowPointer, 1);
  }
  jpeg_finish_decompress(&cinfo);        
  jpeg_destroy_decompress(&cinfo);        
  return 1;
}
//-----------------------------------------------------------------------------
int rgb24_jpg(char* dstFile, char* rgbBuf, int w, int h)
{
  char* dstBuf;
  int dstLen;
  dstBuf = malloc(w * h * 2);
  JpegCompress(w, h, rgbBuf, w * h * 3, dstBuf, &dstLen);
  FILE* f = fopen(dstFile, "wb");
  fwrite(dstBuf, dstLen, 1, f); 
  fclose(f);
  free(dstBuf);
  return 1;
}
//-----------------------------------------------------------------------------
int bmp_jpg(char* srcFile, char* dstFile)
{
#pragma pack(2)        //两字节对齐，否则TBmpFileHead会占16Byte
  typedef struct TBmpFileHead {//
    unsigned short bfType;
    unsigned int bfSize;
    unsigned short bfReserved1;
    unsigned short bfReserved2;
    unsigned int bfOffBits;
  }TBmpFileHead;

  typedef struct TBmpInfoHead {
    unsigned int biSize;
    unsigned int biWidth;
    unsigned int biHeight;
    unsigned short biPlanes;
    unsigned short biBitCount;
    unsigned int biCompression;
    unsigned int biSizeImage;
    unsigned int biXPelsPerMeter;
    unsigned int biYPelsPerMeter;
    unsigned int biClrUsed;
    unsigned int biClrImportant;
  }TBmpInfoHead;

  FILE* f = fopen(srcFile, "rb");
  struct TBmpFileHead FileHead;
  struct TBmpInfoHead InfoHead;
  fread(&FileHead, sizeof(TBmpFileHead), 1, f);
  fread(&InfoHead, sizeof(TBmpInfoHead), 1, f);
  int w  = InfoHead.biWidth;
  int h = InfoHead.biHeight;
  int depth = InfoHead.biBitCount / 8; //24 /8 = 3
  //printf("size %d w:%d h:%d bit:%d\n", FileHead.bfSize, w, h, InfoHead.biBitCount);
  if (w % 4 != 0 || InfoHead.biBitCount != 24)
  {
    fclose(f);
    return 0;
  }

  fseek(f, FileHead.bfOffBits, 0);
  char* srcBuf = malloc(w * h * depth);
  char* rgbBuf = malloc(w * h * depth);
  fread(srcBuf, w * h * depth, 1, f);
  fclose(f);

  int i, j;
  char* psrc = srcBuf;
  char* prgb = rgbBuf + w * depth *(h - 1);
  for (i = 0; i < h; i++)
  {
    for (j = 0; j < w * depth; j += depth)
    {
      prgb[j + 0] = psrc[j + 2];
      prgb[j + 1] = psrc[j + 1];
      prgb[j + 2] = psrc[j + 0];
    }
    psrc = psrc + w * depth;
    prgb = prgb - w * depth;
  }
  free(srcBuf);
  int ret = rgb24_jpg(dstFile, rgbBuf, w, h);
  free(rgbBuf);
  return ret;
}
//-----------------------------------------------------------------------------
/*
int main(int argc, char** argv, char** argp)
{
  char* srcFile;
  char dstFile[100];
  if (argc == 2)
  {
    srcFile = argv[1];
  }
  printf("srcFile:%s\n", srcFile);
  if (!srcFile)
  {
    printf("%s xxx.bmp\n", argv[0]);
    return 0;
  }
  sprintf(dstFile, "%s.jpg", srcFile);

  return bmp_jpg(srcFile, dstFile);
#warning aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
}
*/