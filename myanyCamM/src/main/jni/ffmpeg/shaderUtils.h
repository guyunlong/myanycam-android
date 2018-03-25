//
//  shaderUtils.hpp
//  myanycam
//
//  Created by guyunlong on 3/25/18.
//  Copyright © 2018 jdf. All rights reserved.
//

#ifndef shaderUtils_hpp
#define shaderUtils_hpp
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

#include "jni.h"
#include "GLES/gl.h"
#include "GLES/glext.h"
#include <SLES/OpenSLES.h>
#include "SLES/OpenSLES_Android.h"

class ShaderUtils {
public:
    GLuint createProgram(const char *vertexSource, const char *fragmentSource);
    
    GLuint loadShader(GLenum shaderType, const char *source);
};
#endif /* shaderUtils_hpp */
