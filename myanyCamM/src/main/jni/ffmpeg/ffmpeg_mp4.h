#include <stdio.h>
extern "C"
{
#include <libavformat/avformat.h>
};
int getVopType( const void *p, int len );
AVStream *add_stream(AVFormatContext *oc, AVCodec **codec, enum AVCodecID codec_id);
void open_video(AVFormatContext *oc, AVCodec *codec, AVStream *st);
int CreateMp4(const char* filename);
void WriteVideo(void* data, int nLen);
void CloseMp4();

