#ifndef jpeg_h
#define jpeg_h

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
#include "libjpeg/jpeglib.h"

//-----------------------------------------------------------------------------
int rgb24_jpg(char* dstFile, char* rgbBuf, int w, int h);
int bmp_jpg(char* srcFile, char* dstFile);

#endif
