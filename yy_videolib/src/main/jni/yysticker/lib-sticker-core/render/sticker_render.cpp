//
//  sticker_render.cpp
//  YY
//
//  Created by YY on 2018/3/7.
//  Copyright © 2018年 YY. All rights reserved.
//

#include "sticker_render.hpp"
#include "sticker_logger.h"
#if defined(TARGET_OS_IPHONE)
#include "IOSRenderAPI.h"
#endif
#if defined(TARGET_OS_ANDROID)
#include "AndroidRenderAPI.hpp"
#endif
#include <math.h>

#define POSITION_INDEX_FOR_SCREEN           (9999)      // 用于固定屏幕区域，该模式下positionX/positionY/width/height，设置的是canvas宽高的万分比

namespace gst
{
    SingleFrame::SingleFrame()
    {
        
    }
    
    SingleFrame::~SingleFrame()
    {
        
    }
    
    GLESStickerRender::GLESStickerRender()
    {
        this->canvasWidth = 720;
        this->canvasHeight = 1280;
        
        this->facePointCount = 0;
        memset(this->facePointList, 0x00, sizeof(this->facePointList));
        
        this->a1.create(3, 3);
        this->a2.create(3, 3);
        this->a3.create(3, 3);
        
        this->m1.create(3, 1);
        this->m2.create(3, 1);
        this->m3.create(3, 1);
        this->m4.create(3, 1);
    }
    
    GLESStickerRender::~GLESStickerRender()
    {
    }
    
    void GLESStickerRender::clearTextureCache()
    {
        std::map<std::string,int>::iterator it;
        
        it = this->glesTextureMap.begin();
        
        while(it != this->glesTextureMap.end())
        {
            int textureId = it->second;
            this->realDeleteTexture(textureId);
            it ++;
        }
        
        this->glesTextureMap.clear();
        
        return;
    }
    
    void GLESStickerRender::updateCanvasSize(int width, int height)
    {
        this->canvasWidth = (float)width;
        this->canvasHeight = (float)height;
    }
    
    void GLESStickerRender::updateFacePoint(float * a, int s)
    {
        this->facePointCount = s;
        memcpy(this->facePointList, a, sizeof(float)*s);
        
        this->prepareMatrix();
    }
    
    void GLESStickerRender::prepareMatrix()
    {
        // 根据 36,39,42,45 计算出旋转的角度，以及缩放的系数
        // cv2::Matrix2d<float> a;
        int c = this->facePointCount;
        
        float center_x = (this->facePointList[36] + this->facePointList[39] + this->facePointList[42] + this->facePointList[45])/4;
        float center_y = (this->facePointList[36+c/2] + this->facePointList[39+c/2] + this->facePointList[42+c/2] + this->facePointList[45+c/2])/4;
        
        float p_x = this->facePointList[45];
        float p_y = this->facePointList[45+c/2];
//        float p_x = this->facePointList[16];
//        float p_y = this->facePointList[16+c/2];

        float radius = sqrt( (center_x - p_x)*(center_x - p_x) + (center_y - p_y)*(center_y - p_y) );
        
        float rotateSin = (p_y - center_y) / radius;
        float rotateCos = (p_x - center_x) / radius;
        
        this->faceRotateCos = rotateCos;
        this->faceRotateSin = rotateSin;
        
        this->faceScaleFactor = radius*2.0f*1.5f / (720.0f/2.0f);// 假设两个眼角的距离乘以1.5就是脸部的宽度
        
        
        return;
    }
    
    void GLESStickerRender::unloadTexture(std::vector<SingleFrame> v)
    {
        std::map<std::string, int>::iterator iter;
        int i=0;
        int textureId;
        
        for( i=0; i<v.size(); i++ )
        {
            std::string key = v[i].imageFile;
            iter = this->glesTextureMap.find(key);
            
            if(iter == this->glesTextureMap.end())
            {
                continue;
            }
            
            textureId = iter->second;
            this->realDeleteTexture(textureId);
            // TODO:加锁？
            this->glesTextureMap.erase(iter);
        }
        
        return;
    }
    
    void GLESStickerRender::preloadTexture(std::vector<SingleFrame> v)
    {
        std::map<std::string, int>::iterator iter;
        int i=0;
        int textureId;
        
        for( i=0; i<v.size(); i++ )
        {
            std::string key = v[i].imageFile;
            iter = this->glesTextureMap.find(key);
            
            if(iter != this->glesTextureMap.end())
            {
                continue;
            }
            
            // need preload
            textureId = this->realLoadImageToTexture(key);
            if( textureId >= 0 )
            {
                this->glesTextureMap.insert(std::pair<std::string, int>(key, textureId));
            }
        }
        
        // check if map is too big
        if( this->glesTextureMap.size() )
        {
            // TODO:
        }
        
        return;
    }
    
    void GLESStickerRender::prepareProgramInfo(int uniform, int posAttri, int coorAttri)
    {
        this->filterInputTextureUniform = uniform;
        this->filterPositionAttribute = posAttri;
        this->filterTextureCoordinateAttribute = coorAttri;
        
        return;
    }
    
    void GLESStickerRender::drawTexture(std::vector<SingleFrame> v)
    {
        std::map<std::string, int>::iterator iter;
        int i=0;
        int textureId;
        
        for( i=0; i<v.size(); i++ )
        {
            std::string key = v[i].imageFile;
            iter = this->glesTextureMap.find(key);
            
            if(iter != this->glesTextureMap.end())
            {
                textureId = iter->second;
                
                if( textureId >= 0 && this->filterInputTextureUniform >= 0 ) {
                    
                    float imageVertices[] = {
                        -1.0f, -1.0f,
                        1.0f, -1.0f,
                        -1.0f,  1.0f,
                        1.0f,  1.0f,
                    };
                    
                    // 先平移，再旋转，再缩放，再回去
                    SingleFrame sf = v[i];
                    if( sf.positionIndex < 68 )
                    {
                        float center_x = this->facePointList[sf.positionIndex];
                        float center_y = this->facePointList[sf.positionIndex+this->facePointCount/2];
                        
                        a1(0,0) = 1;
                        a1(0,1) = 0;
                        a1(0,2) = center_x;
                        a1(1,0) = 0;
                        a1(1,1) = 1;
                        a1(1,2) = center_y;
                        a1(2,0) = 0;
                        a1(2,1) = 0;
                        a1(2,2) = 1;
                        
                        a2(0,0) = this->faceRotateCos;
                        a2(0,1) = this->faceRotateSin;
                        a2(0,2) = 0;
                        a2(1,0) = this->faceRotateSin;
                        a2(1,1) = -this->faceRotateCos;
                        a2(1,2) = 0;
                        a2(2,0) = 0;
                        a2(2,1) = 0;
                        a2(2,2) = 1;
                        
                        a3(0,0) = 1;
                        a3(0,1) = 0;
                        a3(0,2) = -center_x;
                        a3(1,0) = 0;
                        a3(1,1) = 1;
                        a3(1,2) = -center_y;
                        a3(2,0) = 0;
                        a3(2,1) = 0;
                        a3(2,2) = 1;
                        
                        // 计算图片的四个顶点位置 (图片原点和特征点原点在左上角，texture原点在左下角)
                        // 左上
                        m1(0,0) = (center_x - (float)sf.positionX * this->faceScaleFactor);
                        m1(1,0) = (center_y + (float)sf.positionY * this->faceScaleFactor);
                        m1(2,0) = 1;
                        // 右上
                        m2(0,0) = (center_x - (float)sf.positionX * this->faceScaleFactor) + (float)sf.width * this->faceScaleFactor;
                        m2(1,0) = (center_y + (float)sf.positionY * this->faceScaleFactor);
                        m2(2,0) = 1;
                        // 左下
                        m3(0,0) = (center_x - (float)sf.positionX * this->faceScaleFactor);
                        m3(1,0) = (center_y + (float)sf.positionY * this->faceScaleFactor) - (float)sf.height * this->faceScaleFactor;
                        m3(2,0) = 1;
                        // 右下
                        m4(0,0) = (center_x - (float)sf.positionX * this->faceScaleFactor) + (float)sf.width * this->faceScaleFactor;
                        m4(1,0) = (center_y + (float)sf.positionY * this->faceScaleFactor) - (float)sf.height * this->faceScaleFactor;
                        m4(2,0) = 1;
                        
                        cv2::Matrix2d<float> merge = a1 * a2;
                        merge = merge * a3;
                        
                        ////////////////////////////////////
                        cv2::Matrix2d<float> result = merge * m1;
                        
                        float tx = result(0, 0);
                        float ty = result(0, 1);
                        
                        // 左上
                        imageVertices[0] = tx*2.0f / this->canvasWidth - 1.0f;
                        imageVertices[1] = ty*2.0f / this->canvasHeight - 1.0f;
                        
                        ////////////////////////////////////
                        result = merge * m2;
                        
                        tx = result(0, 0);
                        ty = result(0, 1);
                        
                        // 右上
                        imageVertices[2] = tx*2.0f / this->canvasWidth - 1.0f;
                        imageVertices[3] = ty*2.0f / this->canvasHeight - 1.0f;
                        
                        ////////////////////////////////////
                        result = merge * m3;
                        
                        tx = result(0, 0);
                        ty = result(0, 1);
                        
                        // 左下
                        imageVertices[4] = tx*2.0f / this->canvasWidth - 1.0f;
                        imageVertices[5] = ty*2.0f / this->canvasHeight - 1.0f;
                        
                        ////////////////////////////////////
                        result = merge * m4;
                        
                        tx = result(0, 0);
                        ty = result(0, 1);
                        
                        // 右下
                        imageVertices[6] = tx*2.0f / this->canvasWidth - 1.0f;
                        imageVertices[7] = ty*2.0f / this->canvasHeight - 1.0f;
                    }
                    // 全屏动画
                    else if( sf.positionIndex == POSITION_INDEX_FOR_SCREEN ) {
                        
                        int realX = sf.positionX * this->canvasWidth / 10000;
                        int realY = sf.positionY * this->canvasHeight / 10000;
                        int realWidth = sf.width * this->canvasWidth / 10000;
                        int realHeight = sf.height * this->canvasHeight / 10000;
                        
                        imageVertices[0] = realX * 2.0f / this->canvasWidth - 1.0f;
                        imageVertices[1] = realY * 2.0f / this->canvasHeight - 1.0f;
                        
                        imageVertices[2] = (realX+realWidth) * 2.0f / this->canvasWidth - 1.0f;
                        imageVertices[3] = realY * 2.0f / this->canvasHeight - 1.0f;
                        
                        imageVertices[4] = realX * 2.0f / this->canvasWidth - 1.0f;
                        imageVertices[5] = (realY+realHeight) * 2.0f / this->canvasHeight - 1.0f;
                        
                        imageVertices[6] = (realX+realWidth) * 2.0f / this->canvasWidth - 1.0f;
                        imageVertices[7] = (realY+realHeight) * 2.0f / this->canvasHeight - 1.0f;
                    }
                    
                    this->realDrawTexture(textureId, this->filterInputTextureUniform, this->filterPositionAttribute, this->filterTextureCoordinateAttribute, imageVertices);
                }
                else
                {
                    STICKER_ERROR("GLESStickerRender::drawTexture invalid textureId (%d,%d)", textureId, this->filterInputTextureUniform);
                }
            }
            else
            {
                // warning texture not ready
                STICKER_ERROR("GLESStickerRender::drawTexture not ready (%s)", (char*)key.c_str());
            }
        }
        
        return;
    }
    
    void GLESStickerRender::realDeleteTexture(int textureId)
    {
#if defined(TARGET_OS_IPHONE)
        IOSRenderAPI::deleteTexture(textureId);
#endif
#if defined(TARGET_OS_ANDROID)
        AndroidRenderAPI::deleteTexture(textureId);
#endif
    }
    
    void GLESStickerRender::realDrawTexture(int textureId, int filterInputTextureUniform, int filterPositionAttribute, int filterTextureCoordinateAttribute, float * vArr)
    {
#if defined(TARGET_OS_IPHONE)
        IOSRenderAPI::drawTexture(textureId, filterInputTextureUniform, filterPositionAttribute, filterTextureCoordinateAttribute, vArr);
#endif
#if defined(TARGET_OS_ANDROID)
        AndroidRenderAPI::drawTexture(textureId, filterInputTextureUniform, filterPositionAttribute, filterTextureCoordinateAttribute, vArr);
#endif
    }
    
    int GLESStickerRender::realLoadImageToTexture(std::string imagefile)
    {
#if defined(TARGET_OS_IPHONE)
        return IOSRenderAPI::loadPngToTexture3(imagefile);
#endif
#if defined(TARGET_OS_ANDROID)
        return AndroidRenderAPI::loadPngToTexture(imagefile);
#endif
    }
}
