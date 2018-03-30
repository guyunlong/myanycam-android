//
//  shaderUtils.hpp
//  myanycam
//
//  Created by guyunlong on 3/25/18.
//  Copyright © 2018 jdf. All rights reserved.
//

#ifndef shaderUtils_hpp
#define shaderUtils_hpp
#include <GLES2/gl2.h>

class ShaderUtils {
public:
    GLuint createProgram(const char *vertexSource, const char *fragmentSource);
    
    GLuint loadShader(GLenum shaderType, const char *source);
};
#endif /* shaderUtils_hpp */
