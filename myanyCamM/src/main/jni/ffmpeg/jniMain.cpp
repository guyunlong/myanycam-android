#include "jniMain.h"
//#include "GLES/gl.h"
//#include "GLES/glext.h"
#include "shaderUtils.h"

#include <EGL/egl.h>
#include <android/native_window_jni.h>
MyVideo _video;
pthread_cond_t th_sync_cond;
pthread_mutex_t th_mutex_lock;
#define IS_TEST
#ifdef IS_TEST
int idec, isws, irender;
#endif

uint8_t*sps , *pps;
int   spsSize, ppsSize;
int   hasDecoderInit;

GLuint yTextureId;
GLuint uTextureId;
GLuint vTextureId;

static EGLConfig eglConf;
static EGLSurface eglWindow;
static EGLContext eglCtx;
static EGLDisplay eglDisp;



#define GET_STR(x) #x
const char *vertexShaderString = GET_STR(
                                         attribute vec4 aPosition;
                                         attribute vec2 aTexCoord;
                                         varying vec2 vTexCoord;
                                         void main() {
                                             vTexCoord=vec2(aTexCoord.x,1.0-aTexCoord.y);
                                             gl_Position = aPosition;
                                         }
                                         );
const char *fragmentShaderString = GET_STR(
                                           precision mediump float;
                                           varying vec2 vTexCoord;
                                           uniform sampler2D yTexture;
                                           uniform sampler2D uTexture;
                                           uniform sampler2D vTexture;
                                           void main() {
                                               vec3 yuv;
                                               vec3 rgb;
                                               yuv.r = texture2D(yTexture, vTexCoord).r;
                                               yuv.g = texture2D(uTexture, vTexCoord).r - 0.5;
                                               yuv.b = texture2D(vTexture, vTexCoord).r - 0.5;
                                               rgb = mat3(1.0,       1.0,         1.0,
                                                          0.0,       -0.39465,  2.03211,
                                                          1.13983, -0.58060,  0.0) * yuv;
                                               gl_FragColor = vec4(rgb, 1.0);
                                           }
                                           );

//-------------------------------------------------------------------------
int video_decode_Free()
{
  LOGE("%s(%d)\n", __FUNCTION__, __LINE__);
  if (_video.sws)
  {
    sws_freeContext(_video.sws);
    _video.sws = NULL;
  }
  if (_video.FrameRGB565)//tmpAV
  {
    av_free(_video.FrameRGB565);
    _video.FrameRGB565 = NULL;
  }
  if (_video.Frame422)
  {
    av_free(_video.Frame422);
    _video.Frame422 = NULL;
  }
  if (_video.ContextV)
  {
    avcodec_close(_video.ContextV);
    av_free(_video.ContextV);
    _video.ContextV = NULL;
  }
  _video.CodecV = NULL;
  return 1;
}
//-------------------------------------------------------------------------
int jvideo_decode_init(JNIEnv* env, jclass obj,int type,int width,int height)
{
    pthread_cond_init(&th_sync_cond, NULL);
    pthread_mutex_init(&th_mutex_lock, NULL);
    
    hasDecoderInit = 0;
    sps = pps = NULL;
    spsSize = ppsSize = 0;
  LOGE("type is %d,wid is %d,height is %d,%s(%d)\n",type,width,height, __FUNCTION__, __LINE__);
  if (_video.encWidth  == 0) _video.encWidth  = 1280;
  if (_video.encHeight == 0) _video.encHeight  = 720;
  av_register_all();
 // avcodec_register_all();

 _video.CodecV = avcodec_find_decoder(CODEC_ID_H264);
    if (_video.CodecV) {
         LOGE("%s(%d)\n", __FUNCTION__, __LINE__);
    }
  //_video.ContextV = avcodec_alloc_context();//old
  _video.ContextV = avcodec_alloc_context();//new
  _video.ContextV->time_base.num = 1; //’‚¡Ω––£∫“ª√Î÷”25÷°
  _video.ContextV->time_base.den = 25;
  _video.ContextV->bit_rate = 0; //≥ı ºªØŒ™0
  _video.ContextV->frame_number = 1; //√ø∞¸“ª∏ˆ ”∆µ÷°
  _video.ContextV->codec_type = AVMEDIA_TYPE_VIDEO;
  _video.ContextV->width = width; //’‚¡Ω––£∫ ”∆µµƒøÌ∂»∫Õ∏ﬂ∂»
  _video.ContextV->height = height;

  _video.ContextV->pix_fmt = PIX_FMT_YUV420P;
    if(avcodec_open(_video.ContextV, _video.CodecV)<0){
        LOGE("%s(%d)\n", __FUNCTION__, __LINE__);
    }
LOGE("%s(%d)\n", __FUNCTION__, __LINE__);
  //_video.Frame422 = avcodec_alloc_frame();//old
  //_video.FrameRGB565/*tmpAV*/ = avcodec_alloc_frame();//old
  _video.Frame422 = avcodec_alloc_frame();//new
  _video.FrameRGB565/*tmpAV*/ = avcodec_alloc_frame();//new
  avpicture_fill((AVPicture*)_video.FrameRGB565,
    (u8*)_video.bufferRGB565,
    PIX_FMT_RGB565,
    TEXTURE_WIDTH,
    TEXTURE_HEIGHT);
 
   _video.sws = sws_getContext(
    _video.ContextV->width,
    _video.ContextV->height,
    _video.ContextV->pix_fmt,
    TEXTURE_WIDTH,
    TEXTURE_HEIGHT,
    PIX_FMT_RGB565,
    SWS_POINT,
    NULL,
    NULL,
    NULL);
  return 1;
}


//-------------------------------------------------------------------------

int jvideo_decode_frame(JNIEnv* env, jclass obj, jbyteArray data, int Len)
{
    LOGE("%s(%d)\n", __FUNCTION__, __LINE__);
  AVPacket packet;
  jbyte * Buf = (jbyte*) env -> GetByteArrayElements( data, 0);
  packet.data = (uint8_t*)Buf;
  packet.size = Len;
     LOGE("packet buf is %02x,len is %d,%s(%d),\n", packet.data[Len-1],Len,__FUNCTION__, __LINE__);
  int gotPicture = 0;
  int size = 0;
#ifdef IS_TEST
  struct timeval tv1, tv2; gettimeofday(&tv1, NULL);
#endif
   
      // AVFrame *yuvFrame = avcodec_alloc_frame();
  while (packet.size > 0)
  {
     
    size = avcodec_decode_video2(_video.ContextV,_video.Frame422, &gotPicture, &packet);
  
      if (gotPicture == 0)
    {
       LOGE("%s(%d)\n", __FUNCTION__, __LINE__);
       
      size = avcodec_decode_video2(_video.ContextV, _video.Frame422, &gotPicture, &packet);
      break;
    }
    packet.size -= size;
    packet.data += size;
      LOGE("decode size is %d,gotpicture is %d ,%s(%d) \n",size,gotPicture, __FUNCTION__, __LINE__);
  }
  if (gotPicture <= 0) return 0;
   
    
    video_decode_SnapShot();
    video_decode_Record((u8*)Buf,Len,true);
    
 LOGE("gotPicture is %d,_video.Frame422->width is %d  _video.Frame422->height is %d,,%s(%d) \n",gotPicture,_video.Frame422->width,_video.Frame422->height, __FUNCTION__, __LINE__);
    
    
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, yTextureId);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, _video.Frame422->linesize[0], _video.Frame422->height,0, GL_LUMINANCE, GL_UNSIGNED_BYTE, _video.Frame422->data[0]);
    
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, uTextureId);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,  _video.Frame422->linesize[1], _video.Frame422->height/2,0, GL_LUMINANCE, GL_UNSIGNED_BYTE, _video.Frame422->data[1]);
    
    glActiveTexture(GL_TEXTURE2);
    glBindTexture(GL_TEXTURE_2D, vTextureId);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,  _video.Frame422->linesize[2], _video.Frame422->height/2,0, GL_LUMINANCE, GL_UNSIGNED_BYTE, _video.Frame422->data[2]);
    
    
    /***
     * 纹理更新完成后开始绘制
     ***/
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
    
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    
    eglSwapBuffers(eglDisp, eglWindow);
    
  //  av_free(&yuvFrame);
    
   
    
   
    
    //av_frame_free(&yuvFrame);
    
    
  
    
//        int slice =  sws_scale(_video.sws,
//                               (const uint8_t* const*)_video.Frame422->data,
//                               _video.Frame422->linesize,
//                               0,
//                               _video.ContextV->height,
//                               _video.FrameRGB565->data,
//                               _video.FrameRGB565->linesize);
   //  if (_video.IFrameFlag == 0) _video.IFrameFlag = 1;
 // pthread_cond_signal(&th_sync_cond);

  return (gotPicture > 0);
}

void video_decode_SnapShot()
{
    if (_video.IsSnapShot == false) return;
    _video.IsSnapShot = false;
    
    int w = _video.ContextV->width;
    int h = _video.ContextV->height;
    int linesize[4] = {w * 3, 0, 0, 0};
    //PIX_FMT_BGR24 && SaveToBmp
    //PIX_FMT_RGB24 && rgb24_jpg
    struct SwsContext* sws = sws_getContext(w, h, PIX_FMT_YUV420P, w, h,PIX_FMT_RGB24, SWS_POINT, NULL, NULL, NULL);
    if (sws)
    {
        char* rgbBuf = (char*)malloc(w * h * 3);
        sws_scale(sws,
                  (const u8* const*)_video.Frame422->data,
                  _video.Frame422->linesize,
                  0,
                  h,
                  (u8**)&rgbBuf,
                  linesize);
        sws_freeContext(sws);
        
        rgb24_jpg(_video.FileName_Jpg, rgbBuf, w, h);
        free(rgbBuf);
    }
}
int jlocal_SnapShot(JNIEnv* env, jclass obj, jstring nFileName)
{
    LOGE("%s(%d)\n", __FUNCTION__, __LINE__);
    strcpy(_video.FileName_Jpg, (char*)env->GetStringUTFChars( nFileName, NULL));
    _video.IsSnapShot = true;
    
    return 1;
}


//-----------------------------------------------------------------------------
//*****************************************************************************
//*****************************************************************************
//*****************************************************************************
//-----------------------------------------------------------------------------
JavaVM* sJavaVM  = 0;
jclass cls_Search  = 0;

//-----------------------------------------------------------------------------
static JNINativeMethod MethodLst[] = {
  //decode
    {"jvideo_decode_init", "(III)I", (void*)jvideo_decode_init},//type, width, height
    {"jvideo_decode_frame", "([BI)I", (void*)jvideo_decode_frame},
  //render
  {"jopengl_Resize", "(II)I", (void*)jopengl_Resize},
  {"jopenglInit", "(Landroid/view/Surface;II)I", (void*)jopenglInit},
  {"jopengl_Render", "()I", (void*)jopengl_Render},
  {"jlocal_SnapShot", "(Ljava/lang/String;)I", (void*)jlocal_SnapShot},
    {"jlocal_StartRec", "(Ljava/lang/String;)I", (void*)jlocal_StartRec},
    {"jlocal_StopRec", "()I", (void*)jlocal_StopRec},
    
  //local
 
};
//-----------------------------------------------------------------------------
int JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
     LOGI("jni onload\n");
#ifndef NELEM
#define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
#endif
  JNIEnv* env = NULL;
  jclass obj;
  sJavaVM = vm;
  if (vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) return  -1;
  assert(env != NULL);
  obj = env ->FindClass( CLASS_PATH_NAME);
  if (obj == NULL) return JNI_FALSE;
  if (env->RegisterNatives( obj, MethodLst, NELEM(MethodLst)) < 0) return JNI_FALSE;
  return JNI_VERSION_1_4;
}
//-----------------------------------------------------------------------------

//-------------------------------------------------------------------------
void check_gl_error(const char* op)
{
  GLint error;
  for (error = glGetError(); error; error = glGetError())
    LOGI("after %s() glError (0x%x)\n", op, error);
}
//-------------------------------------------------------------------------
int jopenglInit(JNIEnv* env, jclass obj, jobject surface,jint cnxwidth,jint cnxheight){
    /**
     *初始化egl
     **/
    LOGE("jopenglInit begin");
    int windowWidth;
    int windowHeight;
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
    
    EGLint configSpec[] = { EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT, EGL_NONE };
    
    eglDisp = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    EGLint eglMajVers, eglMinVers;
    EGLint numConfigs;
    eglInitialize(eglDisp, &eglMajVers, &eglMinVers);
    eglChooseConfig(eglDisp, configSpec, &eglConf, 1, &numConfigs);
    
    eglWindow = eglCreateWindowSurface(eglDisp, eglConf,nativeWindow, NULL);
    
    eglQuerySurface(eglDisp,eglWindow,EGL_WIDTH,&windowWidth);
    eglQuerySurface(eglDisp,eglWindow,EGL_HEIGHT,&windowHeight);
    const EGLint ctxAttr[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };
    eglCtx = eglCreateContext(eglDisp, eglConf,EGL_NO_CONTEXT, ctxAttr);
    
    eglMakeCurrent(eglDisp, eglWindow, eglWindow, eglCtx);
    
    LOGE("window width is %d, height is %d ,%s(%d) \n",windowWidth,windowHeight, __FUNCTION__, __LINE__);
    /**
     * 设置opengl 要在egl初始化后进行
     * **/
    float *vertexData = new float[12]{
        1.0f, -1.0f, 0.0f,
        -1.0f, -1.0f, 0.0f,
        1.0f, 1.0f, 0.0f,
        -1.0f, 1.0f, 0.0f
    };
    
    float *textureVertexData = new float[8]{
        1.0f, 0.0f,//右下
        0.0f, 0.0f,//左下
        1.0f, 1.0f,//右上
        0.0f, 1.0f//左上
    };
    ShaderUtils *shaderUtils = new ShaderUtils();
    
    GLuint programId = shaderUtils->createProgram(vertexShaderString,fragmentShaderString );
    delete shaderUtils;
    GLuint aPositionHandle = (GLuint) glGetAttribLocation(programId, "aPosition");
    GLuint aTextureCoordHandle = (GLuint) glGetAttribLocation(programId, "aTexCoord");
    
    GLuint textureSamplerHandleY = (GLuint) glGetUniformLocation(programId, "yTexture");
    GLuint textureSamplerHandleU = (GLuint) glGetUniformLocation(programId, "uTexture");
    GLuint textureSamplerHandleV = (GLuint) glGetUniformLocation(programId, "vTexture");
    
    
    
    //因为没有用矩阵所以就手动自适应
    int videoWidth = cnxwidth;
    int videoHeight = cnxheight;
    if (cnxwidth > 1280) {
        videoWidth = 1280;
        videoHeight = 720;
    }
    
    int left,top,viewWidth,viewHeight;
    if(windowHeight > windowWidth){
        left = 0;
        viewWidth = windowWidth;
        viewHeight = (int)(videoHeight*1.0f/videoWidth*viewWidth);
        top = (windowHeight - viewHeight)/2;
    }else{
        top = 0;
        viewHeight = windowHeight;
        viewWidth = (int)(videoWidth*1.0f/videoHeight*viewHeight);
        left = (windowWidth - viewWidth)/2;
    }
     LOGE("left is %d, top is %d ,viewWidth is %d,viewheight is %d,%s(%d) \n",left,top,viewWidth,viewHeight, __FUNCTION__, __LINE__);
    glViewport(left, top, viewWidth, viewHeight);
    
    glUseProgram(programId);
    glEnableVertexAttribArray(aPositionHandle);
    glVertexAttribPointer(aPositionHandle, 3, GL_FLOAT, GL_FALSE,
                          12, vertexData);
    glEnableVertexAttribArray(aTextureCoordHandle);
    glVertexAttribPointer(aTextureCoordHandle,2,GL_FLOAT,GL_FALSE,8,textureVertexData);
    /***
     * 初始化空的yuv纹理
     * **/
   
    glGenTextures(1,&yTextureId);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D,yTextureId);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,
                    GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    
    glUniform1i(textureSamplerHandleY,0);
    
    glGenTextures(1,&uTextureId);
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D,uTextureId);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,
                    GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    
    glUniform1i(textureSamplerHandleU,1);
    
    glGenTextures(1,&vTextureId);
    glActiveTexture(GL_TEXTURE2);
    glBindTexture(GL_TEXTURE_2D,vTextureId);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,
                    GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    
    glUniform1i(textureSamplerHandleV,2);

    return 1;
}
int jopengl_Resize(JNIEnv* env, jclass obj, int w, int h)
{
  int i;
  _video.ScreenWidth = w; //the device width and height
  _video.ScreenHeight = h;

//#ifdef IS_RENDER_YUV420
//  glDeleteTextures(3, _video.gl_texture);
//  glGenTextures(3, _video.gl_texture);
//#else
  static GLuint s_disable_caps[] = {GL_FOG, GL_LIGHTING, GL_CULL_FACE, GL_ALPHA_TEST, GL_BLEND, GL_COLOR_LOGIC_OP, GL_DITHER, GL_STENCIL_TEST, GL_DEPTH_TEST, GL_COLOR_MATERIAL, 0};

  glDeleteTextures(1, &(_video.gl_texture));
  GLuint* start = s_disable_caps;
  while (*start) glDisable(*start++);
  glEnable(GL_TEXTURE_2D);
  glGenTextures(1, &(_video.gl_texture));
  glBindTexture(GL_TEXTURE_2D, _video.gl_texture);
  glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
  glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
  glShadeModel(GL_FLAT);
  glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
                     check_gl_error("glColor4x");
  int rect[4] = {0, TEXTURE_HEIGHT, TEXTURE_WIDTH,  -TEXTURE_HEIGHT};
  glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, rect);
                     check_gl_error("glTexParameteriv");
//#endif
  LOGE("%s(%d) w:%d h:%d\n", __FUNCTION__, __LINE__, w, h);
  return 1;
}
//-------------------------------------------------------------------------
int jopengl_Render(JNIEnv* env, jclass obj)
{
  if (!_video.IFrameFlag) return 0;

  pthread_mutex_lock(&th_mutex_lock);
  pthread_cond_wait(&th_sync_cond, &th_mutex_lock);
  pthread_mutex_unlock(&th_mutex_lock);
  glClear(GL_COLOR_BUFFER_BIT);
  glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, TEXTURE_WIDTH, TEXTURE_HEIGHT, 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, _video.bufferRGB565);
//  glDrawTexiOES(0, 0, 0, _video.ScreenWidth, _video.ScreenHeight);
    
    int slice =  sws_scale(_video.sws,
                           (const uint8_t* const*)_video.Frame422->data,
                           _video.Frame422->linesize,
                           0,
                           _video.ContextV->height,
                           _video.FrameRGB565->data,
                           _video.FrameRGB565->linesize);
    
    LOGE("slice is %d,%s(%d) w:%d h:%d\n", slice,__FUNCTION__, __LINE__, _video.ScreenWidth, _video.ScreenHeight);
    
    glDrawTexiOES(0, 0, 0, _video.ScreenWidth,_video.ScreenWidth*_video.ContextV->height/_video.ContextV->width);
    check_gl_error("glTexImage2D");
  return 1;
}

void video_decode_Record(u8* Buf, int Len, bool IsIFrame)
{
     LOGE("video_decode_Record ,len is %d,isrec is %d,%s(%d)\n",Len ,_video.IsRec,__FUNCTION__, __LINE__);
    if (!_video.IsRec) return;
    
    if (_video.Flag_StartRec && IsIFrame)
    {
        _video.Flag_StartRec = false;
    }
    LOGE("video_decode_Record ,len is %d,%s(%d)\n",Len ,__FUNCTION__, __LINE__);
    if (_video.Flag_StartRec == false && Len > 0)
    {
        
        AVPacket pkt;
        av_init_packet(&pkt);
        pkt.stream_index = 0;//_video.avStreamV->index;
        pkt.data = Buf;
        pkt.size = Len;
        /*
         pkt.flags |= AV_PKT_FLAG_KEY;
         pts = pkt.pts;
         pkt.pts += last_pts;
         dts = pkt.dts;
         pkt.dts += last_dts;
         pkt.stream_index = 0;
         */
        static int num = 1;
        LOGE("frame %d,%s(%d)\n", num++,__FUNCTION__, __LINE__);
        av_interleaved_write_frame(_video.avContext, &pkt);
    }
}
int jlocal_StartRec(JNIEnv* env, jclass obj, jstring nFilename)
{
    LOGE("jlocal_StartRec ,%s(%d)\n", __FUNCTION__, __LINE__);
    strcpy(_video.FileName_Rec, (char*)env->GetStringUTFChars(nFilename, NULL));
    
   LOGE("jlocal_StartRec video.FileName_Rec %s,%s(%d)\n",_video.FileName_Rec, __FUNCTION__, __LINE__);
    avcodec_register_all();
    av_register_all();
    avformat_alloc_output_context2(&_video.avContext, NULL, NULL, _video.FileName_Rec);
    if (!_video.avContext)  {
        LOGE("jlocal_StartRec return %s(%d)\n", __FUNCTION__, __LINE__);
        return 0;
    }
    _video.avStreamV = avformat_new_stream(_video.avContext, NULL);
    _video.avStreamV->index = 0;
    _video.avStreamV->codec->frame_number = 1;
    _video.avStreamV->codec->bit_rate = 3000000;//25
    _video.avStreamV->codec->codec_id = CODEC_ID_H264;//i_video_stream->codec->codec_id;
    _video.avStreamV->codec->codec_type = AVMEDIA_TYPE_VIDEO;//i_video_stream->codec->codec_type;
    _video.avStreamV->codec->time_base.num = 1;//i_video_stream->time_base.num;
    //_video.avStreamV->codec->time_base.den = 25;//i_video_stream->time_base.den;
    _video.avStreamV->codec->time_base.den = _video.RecFrameRate;
    _video.avStreamV->codec->width = _video.encWidth;
    _video.avStreamV->codec->height = _video.encHeight;
    _video.avStreamV->codec->pix_fmt = PIX_FMT_YUV420P;//i_video_stream->codec->pix_fmt;
    
    _video.avContext->oformat->flags |= CODEC_FLAG_GLOBAL_HEADER;
    
    avio_open(&_video.avContext->pb, _video.FileName_Rec, AVIO_FLAG_WRITE);
    avformat_write_header(_video.avContext, NULL);
    
    LOGE("%s(%d)\n", __FUNCTION__, __LINE__);

    _video.IsRec = true;
    _video.Flag_StartRec = true;
    
    return 1;
}
//-------------------------------------------------------------------------
int jlocal_StopRec(JNIEnv* env, jclass obj)
{
    LOGE("jlocal_StopRec %s(%d)\n", __FUNCTION__, __LINE__);
    _video.IsRec = false;
    _video.Flag_StopRec = true;

    if (_video.avContext)
    {
        av_write_trailer(_video.avContext);
        
        avcodec_close(_video.avContext->streams[0]->codec);//  _video.avStreamV->codec
        av_freep(&_video.avContext->streams[0]->codec);
        av_freep(&_video.avContext->streams[0]);
        avio_close(_video.avContext->pb);
        av_free(_video.avContext);
    }
    LOGE("%s(%d)\n", __FUNCTION__, __LINE__);

    return 1;
}

