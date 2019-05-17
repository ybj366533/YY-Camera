
//
//  IOSRenderAPI.m
//  gtv
//
//  Created by gtv on 2018/3/7.
//  Copyright © 2018年 gtv. All rights reserved.
//
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

#if TARGET_IPHONE_SIMULATOR || TARGET_OS_IPHONE
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#else
#import <OpenGL/OpenGL.h>
#import <OpenGL/gl.h>
#endif

#include "IOSRenderAPI.h"

#define glError() { \
GLenum err = glGetError(); \
if (err != GL_NO_ERROR) { \
printf("glError: %04x caught at %s:%u\n", err, __FILE__, __LINE__); \
} \
}
#include "gtv_png.h"

namespace gst
{
    IOSRenderAPI::IOSRenderAPI()
    {
        
    }
    
    IOSRenderAPI::~IOSRenderAPI()
    {
        
    }
    
    void IOSRenderAPI::drawTexture(int textureId, int filterInputTextureUniform, int filterPositionAttribute, int filterTextureCoordinateAttribute, float * vArr)
    {
        static const GLfloat textureCoordinates[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
        };
        
        GLfloat imageVertices[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
        };
        
        for( int i=0; i<8; i++ )
        {
            imageVertices[i] = (GLfloat)vArr[i];
        }
        
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glUniform1i(filterInputTextureUniform, 2);
        
        glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, 0, 0, imageVertices);
        glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, 0, 0, textureCoordinates);
        
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        return;
    }
    
    void IOSRenderAPI::deleteTexture(int textureId)
    {
        GLuint t = (GLuint)textureId;
        
        glDeleteTextures(1, &t);
    }
    
    int IOSRenderAPI::loadPngToTexture(std::string imagefile)
    {
        // Id for texture
        GLuint texture;
        
        // Generate textures
        glGenTextures(1, &texture);
        // Bind it
        glBindTexture(GL_TEXTURE_2D, texture);
        
        // Set a few parameters to handle drawing the image at lower and higher sizes than original
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        //    glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, GL_TRUE);
        
        NSString *path = [[NSString alloc] initWithUTF8String:imagefile.c_str()];
        NSData *texData = [[NSData alloc] initWithContentsOfFile:path];
        UIImage *image = [[UIImage alloc] initWithData:texData];
        if (image == nil) {
            return -1;
        }
        
        // Get Image size
        GLuint width = (GLuint)CGImageGetWidth(image.CGImage);
        GLuint height = (GLuint)CGImageGetHeight(image.CGImage);
        
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        // Allocate memory for image
        void *imageData = malloc( height * width * 4 );
        CGContextRef imgcontext = CGBitmapContextCreate( imageData, width, height, 8, 4 * width, colorSpace, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big );
        CGColorSpaceRelease( colorSpace );
        CGContextClearRect( imgcontext, CGRectMake( 0, 0, width, height ) );
        CGContextTranslateCTM( imgcontext, 0, height - height );
        CGContextDrawImage( imgcontext, CGRectMake( 0, 0, width, height ), image.CGImage );
        
        // Generate texture in opengl
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        // Release context
        CGContextRelease(imgcontext);
        // Free Stuff
        free(imageData);
        //    [image release];
        //    [texData release];
        
        return texture;
    }
    
    int IOSRenderAPI::loadPngToTexture2(std::string imagefile)
    {
        NSString *path = [[NSString alloc] initWithUTF8String:imagefile.c_str()];
        NSData *texData = [[NSData alloc] initWithContentsOfFile:path];
        UIImage *image = [[UIImage alloc] initWithData:texData];
        if (image == nil) {
            return -1;
        }
        
        CGImageRef newImageSource = [image CGImage];
        
        // TODO: Dispatch this whole thing asynchronously to move image loading off main thread
        CGFloat widthOfImage = CGImageGetWidth(newImageSource);
        CGFloat heightOfImage = CGImageGetHeight(newImageSource);
        
        if( widthOfImage <= 0 || heightOfImage <= 0 ) {
            return -1;
        }
        
        if( widthOfImage > 1920 || heightOfImage > 1080 ) {
            return -1;
        }
        
        CGSize pixelSizeOfImage = CGSizeMake(widthOfImage, heightOfImage);
        CGSize pixelSizeToUseForTexture = pixelSizeOfImage;
        
        BOOL shouldRedrawUsingCoreGraphics = NO;
        
        GLubyte *imageData = NULL;
        CFDataRef dataFromImageDataProvider = NULL;
        GLenum format = GL_BGRA;
        BOOL isLitteEndian = YES;
        BOOL alphaFirst = NO;
        BOOL premultiplied = NO;
        
        /* Check that the memory layout is compatible with GL, as we cannot use glPixelStore to
         * tell GL about the memory layout with GLES.
         */
        if (CGImageGetBytesPerRow(newImageSource) != CGImageGetWidth(newImageSource) * 4 ||
            CGImageGetBitsPerPixel(newImageSource) != 32 ||
            CGImageGetBitsPerComponent(newImageSource) != 8)
        {
            shouldRedrawUsingCoreGraphics = YES;
        } else {
            /* Check that the bitmap pixel format is compatible with GL */
            CGBitmapInfo bitmapInfo = CGImageGetBitmapInfo(newImageSource);
            if ((bitmapInfo & kCGBitmapFloatComponents) != 0) {
                /* We don't support float components for use directly in GL */
                shouldRedrawUsingCoreGraphics = YES;
            } else {
                CGBitmapInfo byteOrderInfo = bitmapInfo & kCGBitmapByteOrderMask;
                if (byteOrderInfo == kCGBitmapByteOrder32Little) {
                    /* Little endian, for alpha-first we can use this bitmap directly in GL */
                    CGImageAlphaInfo alphaInfo = (CGImageAlphaInfo)(bitmapInfo & kCGBitmapAlphaInfoMask);
                    if (alphaInfo != kCGImageAlphaPremultipliedFirst && alphaInfo != kCGImageAlphaFirst &&
                        alphaInfo != kCGImageAlphaNoneSkipFirst) {
                        shouldRedrawUsingCoreGraphics = YES;
                    }
                } else if (byteOrderInfo == kCGBitmapByteOrderDefault || byteOrderInfo == kCGBitmapByteOrder32Big) {
                    isLitteEndian = NO;
                    /* Big endian, for alpha-last we can use this bitmap directly in GL */
                    CGImageAlphaInfo alphaInfo = (CGImageAlphaInfo)(bitmapInfo & kCGBitmapAlphaInfoMask);
                    if (alphaInfo != kCGImageAlphaPremultipliedLast && alphaInfo != kCGImageAlphaLast &&
                        alphaInfo != kCGImageAlphaNoneSkipLast) {
                        shouldRedrawUsingCoreGraphics = YES;
                    } else {
                        /* Can access directly using GL_RGBA pixel format */
                        premultiplied = alphaInfo == kCGImageAlphaPremultipliedLast || alphaInfo == kCGImageAlphaPremultipliedLast;
                        alphaFirst = alphaInfo == kCGImageAlphaFirst || alphaInfo == kCGImageAlphaPremultipliedFirst;
                        format = GL_RGBA;
                    }
                }
            }
        }
        
        //    CFAbsoluteTime elapsedTime, startTime = CFAbsoluteTimeGetCurrent();
        
        if (shouldRedrawUsingCoreGraphics)
        {
            // For resized or incompatible image: redraw
            imageData = (GLubyte *) calloc(1, (int)pixelSizeToUseForTexture.width * (int)pixelSizeToUseForTexture.height * 4);
            
            CGColorSpaceRef genericRGBColorspace = CGColorSpaceCreateDeviceRGB();
            
            CGContextRef imageContext = CGBitmapContextCreate(imageData, (size_t)pixelSizeToUseForTexture.width, (size_t)pixelSizeToUseForTexture.height, 8, (size_t)pixelSizeToUseForTexture.width * 4, genericRGBColorspace,  kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
            //        CGContextSetBlendMode(imageContext, kCGBlendModeCopy); // From Technical Q&A QA1708: http://developer.apple.com/library/ios/#qa/qa1708/_index.html
            CGContextDrawImage(imageContext, CGRectMake(0.0, 0.0, pixelSizeToUseForTexture.width, pixelSizeToUseForTexture.height), newImageSource);
            CGContextRelease(imageContext);
            CGColorSpaceRelease(genericRGBColorspace);
            isLitteEndian = YES;
            alphaFirst = YES;
            premultiplied = YES;
        }
        else
        {
            // Access the raw image bytes directly
            dataFromImageDataProvider = CGDataProviderCopyData(CGImageGetDataProvider(newImageSource));
            imageData = (GLubyte *)CFDataGetBytePtr(dataFromImageDataProvider);
        }
        
        if (premultiplied) {
            NSUInteger    totalNumberOfPixels = round(pixelSizeToUseForTexture.width * pixelSizeToUseForTexture.height);
            uint32_t    *pixelP = (uint32_t *)imageData;
            uint32_t    pixel;
            CGFloat        srcR, srcG, srcB, srcA;
            
            for (NSUInteger idx=0; idx<totalNumberOfPixels; idx++, pixelP++) {
                pixel = isLitteEndian ? CFSwapInt32LittleToHost(*pixelP) : CFSwapInt32BigToHost(*pixelP);
                
                if (alphaFirst) {
                    srcA = (CGFloat)((pixel & 0xff000000) >> 24) / 255.0f;
                }
                else {
                    srcA = (CGFloat)(pixel & 0x000000ff) / 255.0f;
                    pixel >>= 8;
                }
                
                srcR = (CGFloat)((pixel & 0x00ff0000) >> 16) / 255.0f;
                srcG = (CGFloat)((pixel & 0x0000ff00) >> 8) / 255.0f;
                srcB = (CGFloat)(pixel & 0x000000ff) / 255.0f;
                
                srcR /= srcA; srcG /= srcA; srcB /= srcA;
                
                pixel = (uint32_t)(srcR * 255.0) << 16;
                pixel |= (uint32_t)(srcG * 255.0) << 8;
                pixel |= (uint32_t)(srcB * 255.0);
                
                if (alphaFirst) {
                    pixel |= (uint32_t)(srcA * 255.0) << 24;
                }
                else {
                    pixel <<= 8;
                    pixel |= (uint32_t)(srcA * 255.0);
                }
                *pixelP = isLitteEndian ? CFSwapInt32HostToLittle(pixel) : CFSwapInt32HostToBig(pixel);
            }
        }
        
        GLuint _texture;
        
        glActiveTexture(GL_TEXTURE1);
        glGenTextures(1, &_texture);
        
        glBindTexture(GL_TEXTURE_2D, _texture);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // This is necessary for non-power-of-two textures
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, (int)pixelSizeToUseForTexture.width, (int)pixelSizeToUseForTexture.height, 0, format, GL_UNSIGNED_BYTE, imageData);
        
        glBindTexture(GL_TEXTURE_2D, 0);
        
        if (shouldRedrawUsingCoreGraphics)
        {
            free(imageData);
        }
        else
        {
            if (dataFromImageDataProvider)
            {
                CFRelease(dataFromImageDataProvider);
            }
        }
        
        return (int)_texture;//[outputFramebuffer texture];
    }
    
    int IOSRenderAPI::loadPngToTexture3(std::string imagefile)
    {
        // TODO:
        // 读取文件，路径固定
        int imageWidth = 480;
        int imageHeight = 640;
        uint8_t * imageData;// = (uint8_t*)malloc(imageWidth*imageHeight*4);
        
        gtvPng    png;
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
