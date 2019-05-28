
//
//  IOSRenderAPI.m
//  MGLiveFramework
//
//  Created by YY on 2018/3/7.
//  Copyright © 2018年 YY. All rights reserved.
//

#include <GLES2/gl2.h>

#include "sticker_logger.h"
#include "AndroidRenderAPI.hpp"
#include "YY_png.h"

#define glError() { \
GLenum err = glGetError(); \
if (err != GL_NO_ERROR) { \
STICKER_ERROR("glError: %04x caught at %s:%u\n", err, __FILE__, __LINE__); \
} \
}

namespace gst
{
    AndroidRenderAPI::AndroidRenderAPI()
    {
        
    }
    
    AndroidRenderAPI::~AndroidRenderAPI()
    {
        
    }
    
    void AndroidRenderAPI::drawTexture(int textureId, int filterInputTextureUniform, int filterPositionAttribute, int filterTextureCoordinateAttribute, float * vArr)
    {
//        static const GLfloat textureCoordinates[] = {
//            0.0f, 0.0f,
//            1.0f, 0.0f,
//            0.0f, 1.0f,
//            1.0f, 1.0f,
//        };
        static const GLfloat textureCoordinates[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
        };

        GLfloat imageVertices[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
        };
        
        GLfloat imageVertices2[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
        };
        
        for( int i=0; i<8; i++ )
        {
            imageVertices[i] = (GLfloat)vArr[i];
            if( i%2 != 0 ) {
                imageVertices[i] = 0.0f - imageVertices[i];
            }
        }
        
        imageVertices2[0] = imageVertices[4];
        imageVertices2[1] = imageVertices[5];
        imageVertices2[2] = imageVertices[6];
        imageVertices2[3] = imageVertices[7];
        imageVertices2[4] = imageVertices[0];
        imageVertices2[5] = imageVertices[1];
        imageVertices2[6] = imageVertices[2];
        imageVertices2[7] = imageVertices[3];
        
//        STICKER_ERROR("imageVertices : %f,%f, %f,%f, %f,%f, %f,%f ", imageVertices[0], imageVertices[1], imageVertices[2], imageVertices[3], imageVertices[4], imageVertices[5], imageVertices[6], imageVertices[7] );
        
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glUniform1i(filterInputTextureUniform, 2);
        
        glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, 0, 0, imageVertices2);
        glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, 0, 0, textureCoordinates);
        
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        return;
    }
    
    void AndroidRenderAPI::deleteTexture(int textureId)
    {
        GLuint t = (GLuint)textureId;
        
        glDeleteTextures(1, &t);
    }
    
    int AndroidRenderAPI::loadPngToTexture(std::string imagefile)
    {
        // TODO:
        // 读取文件，路径固定
        int imageWidth = 480;
        int imageHeight = 640;
        uint8_t * imageData;// = (uint8_t*)malloc(imageWidth*imageHeight*4);
		
		YYPng	png;
		RGBQUAD* pImg = NULL;
		/// 图片解码
		png.getImageFromFile(&pImg, &imageWidth, &imageHeight, imagefile.c_str());
		imageData = (uint8_t *)pImg;
        // int * r = (int*)imageData;
        // for( int i=0; i<480*640; i++ ) {
            // if( i%2 == 0 )
                // r[i] = 0x00ff00ff;
            // else
                // r[i] = 0xff00ff00;
        // }
        glError();
		
        GLuint _texture;
        
        glActiveTexture(GL_TEXTURE1);
        glGenTextures(1, &_texture);
        
        glBindTexture(GL_TEXTURE_2D, _texture);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // This is necessary for non-power-of-two textures
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, (int)imageWidth, (int)imageHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        
        glBindTexture(GL_TEXTURE_2D, 0);
        glError();
        free(imageData);
        
        return (int)_texture;
    }
    
}
