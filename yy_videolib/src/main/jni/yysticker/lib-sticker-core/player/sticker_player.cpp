//
//  sticker_player.cpp
//  YY
//
//  Created by YY on 2018/3/6.
//  Copyright © 2018年 YY. All rights reserved.
//
#include <sys/time.h>

#include "sticker_player.hpp"

#include "sticker_logger.h"

#include "cJSON.h"

#define _STICKER_JSON_VER_ "0.1"

static int64_t sticker_system_current_milli()
{
    int64_t milli = 0;

    struct timeval    tp;

    gettimeofday(&tp, NULL);

    //milli = tp.tv_sec * 1000 + tp.tv_usec/1000;
    milli = tp.tv_sec * (int64_t)1000 + tp.tv_usec/1000;

    return milli;
}

namespace gst {
    
    PartSticker::PartSticker()
    {
        
    }
    
    PartSticker::~PartSticker()
    {
        
    }
    
    int PartSticker::getDisplayIndex(uint64_t timestamp)
    {
        uint64_t d = (timestamp / (uint64_t)this->interval);
        int index = d % this->frameCount;
        
        return index;
    }
    
    std::string PartSticker::fileNameToDisplay(uint64_t timestamp)
    {
        int index = 0;
        char buffer[128];
        
        memset(buffer, 0x00, sizeof(buffer));
        
        index = this->getDisplayIndex(timestamp);
        
        snprintf(buffer, 127, "%s/%s_%03d.png", this->partName.c_str(), this->partName.c_str(), index);
        
        std::string s(buffer);
        
        return s;
    }
    
    int PartSticker::frameToDisplay(uint64_t timestamp, SingleFrame &sf)
    {
        int index = 0;
        char buffer[2048];
        
        memset(buffer, 0x00, sizeof(buffer));
        
        index = this->getDisplayIndex(timestamp);
        
        snprintf(buffer, sizeof(buffer)-1, "%s/%s/%s_%03d.png", this->rootFolder.c_str(), this->partName.c_str(), this->partName.c_str(), index);
        
        std::string s(buffer);
        
        sf.imageFile = s;
        sf.width = this->width;
        sf.height = this->height;
        sf.positionX = this->positionX;
        sf.positionY = this->positionY;
        sf.positionIndex = this->positionIndex;
        sf.zPosition = this->zPosition;
        
        return 1;
    }
    
    int PartSticker::lastFrameToDisplay(SingleFrame &sf)
    {
        int index = 0;
        char buffer[2048];
        
        memset(buffer, 0x00, sizeof(buffer));
        
        index = this->frameCount - 1;
        
        snprintf(buffer, sizeof(buffer)-1, "%s/%s/%s_%03d.png", this->rootFolder.c_str(), this->partName.c_str(), this->partName.c_str(), index);
        
        std::string s(buffer);
        
        sf.imageFile = s;
        sf.width = this->width;
        sf.height = this->height;
        sf.positionX = this->positionX;
        sf.positionY = this->positionY;
        sf.positionIndex = this->positionIndex;
        sf.zPosition = this->zPosition;
        
        return 1;
    }
    
    int PartSticker::preloadFrameList(uint64_t timestamp, std::vector<SingleFrame> &v, int limit)
    {
        int i = 0;
        int index = 0;
        int fidx = 0;
        char buffer[2048];
        
        memset(buffer, 0x00, sizeof(buffer));
        index = this->getDisplayIndex(timestamp);
        
        for( i=0; i<limit; i++ )
        {
            fidx = (index+i)%this->frameCount;
//            if( index+i >= this->frameCount )
//                continue;
            
            memset(buffer, 0x00, sizeof(buffer));
            snprintf(buffer, sizeof(buffer)-1, "%s/%s/%s_%03d.png", this->rootFolder.c_str(), this->partName.c_str(), this->partName.c_str(), fidx);
            
            std::string s(buffer);
            
            SingleFrame sf;
            
            sf.imageFile = s;
            sf.width = this->width;
            sf.height = this->height;
            sf.positionX = this->positionX;
            sf.positionY = this->positionY;
            sf.positionIndex = this->positionIndex;
            sf.zPosition = this->zPosition;
            
            v.push_back(sf);
        }
        
        return limit;
    }
    
    int PartSticker::unloadFrameList(uint64_t timestamp, std::vector<SingleFrame> &v, int limit)
    {
        int i = 0;
        int index = 0;
        int fidx = 0;
        char buffer[2048];
        
        if( this->frameCount <= 20 )
            return 0;
        
        memset(buffer, 0x00, sizeof(buffer));
        index = this->getDisplayIndex(timestamp);
        
        for( i=1; i<=limit; i++ )
        {
            fidx = index-i;
            if( fidx < 0 )
                fidx = this->frameCount - 1;
            
            memset(buffer, 0x00, sizeof(buffer));
            snprintf(buffer, sizeof(buffer)-1, "%s/%s/%s_%03d.png", this->rootFolder.c_str(), this->partName.c_str(), this->partName.c_str(), fidx);
            
            std::string s(buffer);
            
            SingleFrame sf;
            
            sf.imageFile = s;
            sf.width = this->width;
            sf.height = this->height;
            sf.positionX = this->positionX;
            sf.positionY = this->positionY;
            sf.positionIndex = this->positionIndex;
            sf.zPosition = this->zPosition;
            
            v.push_back(sf);
        }
        
        return limit;
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    
    int ContentSticker::jsonToObject(const char * file_content)
    {
        cJSON * property = NULL;
        cJSON * elem = NULL;
        cJSON * partObj = NULL;
        cJSON * partValue = NULL;
        int ret, i, siz;
        
        ret = i = siz = 0;
        
        // check json
        cJSON *monitor_json = cJSON_Parse(file_content);
        
        if( monitor_json == NULL ) {
            STICKER_ERROR("invalid json file (%s)", file_content);
            ret = -21;
            goto INIT_JSON_END;
        }
        
        // get version
        property = cJSON_GetObjectItemCaseSensitive(monitor_json, "version");
        if( property != NULL && cJSON_IsString(property) ) {
            STICKER_INFO("sticker version %s == %s ? ", property->valuestring, _STICKER_JSON_VER_);
            if( strcmp(property->valuestring, _STICKER_JSON_VER_) != 0 ) {
                ret = -22;
                goto INIT_JSON_END;
            }
        }
        else {
            STICKER_ERROR("invalid json file version not found (%s)", file_content);
            ret = -23;
            goto INIT_JSON_END;
        }
        
        // get head list
        property = cJSON_GetObjectItemCaseSensitive(monitor_json, "head");
        if( property != NULL && cJSON_IsArray(property) ) {
            
            siz = cJSON_GetArraySize(property);
            
            for( i=0; i<siz; i++ ) {
                
                elem = cJSON_GetArrayItem(property, i);
                if( elem != NULL && cJSON_IsString(elem) ) {
                    STICKER_INFO("sticker key %s ", elem->valuestring);
                    std::string s = elem->valuestring;
                    this->keys.push_back(s);
                }
                else {
                    continue;
                }
            }
        }
        else {
            STICKER_ERROR("invalid json file head not found (%s)", file_content);
            ret = -24;
            goto INIT_JSON_END;
        }
        
        // get body part
        property = cJSON_GetObjectItemCaseSensitive(monitor_json, "body");
        if( property != NULL && cJSON_IsObject(property) ) {
            
            for( i=0; i<this->keys.size(); i++ ) {
                
                std::string t = this->keys[i];
                partObj = cJSON_GetObjectItemCaseSensitive(property, (char*)(t.c_str()));
                
                if( partObj != NULL && cJSON_IsObject(partObj) ) {
                    
                    PartSticker ps;
                    
                    ps.partName = t;
                    ps.rootFolder = this->folder;
                    
                    partValue = cJSON_GetObjectItemCaseSensitive(partObj, "interval");
                    if( partValue != NULL && cJSON_IsNumber(partValue) ) {
                        ps.interval = partValue->valueint;
                        if( ps.interval <= 0 ) {
                            STICKER_ERROR("invalid interval value (%s:%d)", (char*)(t.c_str()), ps.interval);
                            ps.interval = 50;
                        }
                    }
                    else {
                        STICKER_ERROR("invalid partValue (%s:%s)", file_content, (char*)(t.c_str()));
                        ret = -25;
                        goto INIT_JSON_END;
                    }
                    
                    partValue = cJSON_GetObjectItemCaseSensitive(partObj, "frameCount");
                    if( partValue != NULL && cJSON_IsNumber(partValue) ) {
                        ps.frameCount = partValue->valueint;
                        if( ps.frameCount <= 0 ) {
                            STICKER_ERROR("invalid frameCount value (%s:%d)", (char*)(t.c_str()), ps.frameCount);
                            ps.frameCount = 50;
                        }
                    }
                    else {
                        STICKER_ERROR("invalid partValue (%s:%s)", file_content, (char*)(t.c_str()));
                        ret = -25;
                        goto INIT_JSON_END;
                    }
                    
                    partValue = cJSON_GetObjectItemCaseSensitive(partObj, "width");
                    if( partValue != NULL && cJSON_IsNumber(partValue) ) {
                        ps.width = partValue->valueint;
                    }
                    else {
                        STICKER_ERROR("invalid partValue (%s:%s)", file_content, (char*)(t.c_str()));
                        ret = -25;
                        goto INIT_JSON_END;
                    }
                    
                    partValue = cJSON_GetObjectItemCaseSensitive(partObj, "height");
                    if( partValue != NULL && cJSON_IsNumber(partValue) ) {
                        ps.height = partValue->valueint;
                    }
                    else {
                        STICKER_ERROR("invalid partValue (%s:%s)", file_content, (char*)(t.c_str()));
                        ret = -25;
                        goto INIT_JSON_END;
                    }
                    
                    partValue = cJSON_GetObjectItemCaseSensitive(partObj, "positionIndex");
                    if( partValue != NULL && cJSON_IsNumber(partValue) ) {
                        ps.positionIndex = partValue->valueint;
                    }
                    else {
                        STICKER_ERROR("invalid partValue (%s:%s)", file_content, (char*)(t.c_str()));
                        ret = -25;
                        goto INIT_JSON_END;
                    }
                    
                    partValue = cJSON_GetObjectItemCaseSensitive(partObj, "positionX");
                    if( partValue != NULL && cJSON_IsNumber(partValue) ) {
                        ps.positionX = partValue->valueint;
                    }
                    else {
                        STICKER_ERROR("invalid partValue (%s:%s)", file_content, (char*)(t.c_str()));
                        ret = -25;
                        goto INIT_JSON_END;
                    }
                    
                    partValue = cJSON_GetObjectItemCaseSensitive(partObj, "positionY");
                    if( partValue != NULL && cJSON_IsNumber(partValue) ) {
                        ps.positionY = partValue->valueint;
                    }
                    else {
                        STICKER_ERROR("invalid partValue (%s:%s)", file_content, (char*)(t.c_str()));
                        ret = -25;
                        goto INIT_JSON_END;
                    }
                    
                    partValue = cJSON_GetObjectItemCaseSensitive(partObj, "zPosition");
                    if( partValue != NULL && cJSON_IsNumber(partValue) ) {
                        ps.zPosition = partValue->valueint;
                    }
                    else {
                        STICKER_ERROR("invalid partValue (%s:%s)", file_content, (char*)(t.c_str()));
                        ret = -25;
                        goto INIT_JSON_END;
                    }
                    
                    this->parts.push_back(ps);
                }
                else {
                    STICKER_ERROR("invalid part (%s:%s)", file_content, (char*)(t.c_str()));
                    ret = -25;
                    goto INIT_JSON_END;
                }
            }
        }
        else {
            STICKER_ERROR("invalid json file body not found (%s)", file_content);
            ret = -25;
            goto INIT_JSON_END;
        }
        
    INIT_JSON_END:
        
        if( monitor_json != NULL )
            cJSON_Delete(monitor_json);
        
        return ret;
    }
    
    int ContentSticker::loadJsonFile(const char * c)
    {
        if( c == NULL )
            return -1;
        
        std::string f = c;
        
        return this->loadJsonFile(f);
    }
    
    int ContentSticker::loadJsonFile(std::string f)
    {
        char path[2048];
        
        int ret = 0;
        int size = 0;
        char * file_content = NULL;
        
        this->folder = f;
        
        this->clear();
        
        memset(path, 0x00, sizeof(path));
        snprintf(path, sizeof(path)-1, "%s/default.json", (char*)this->folder.c_str());
        
        // open json file
        FILE * fp = fopen(path, "r");
        
        if( fp == NULL ) {
            STICKER_ERROR("folder data not found (%s)", path);
            return -3;
        }
        
        fseek(fp,0L,SEEK_END);
        size = (int)ftell(fp);
        
        // json file size must be smaller
        if( size > 2*1024*1024 ) {
            STICKER_ERROR("json file too big (%s:%d)", path, size);
            ret = -4;
            goto STICKER_PLAY_END;
        }
        
        file_content = (char*)malloc(size+32);
        
        if( file_content == NULL ) {
            STICKER_ERROR("can not alloc memory (%s:%d)", path, size);
            ret = -5;
            goto STICKER_PLAY_END;
        }
        
        memset(file_content, 0x00, size+32);
        fseek(fp,0L,SEEK_SET);
        fread(file_content, 1, size, fp);
        //printf("ret = %d %c %c %c ", ret, file_content[0],file_content[1],file_content[2]);
        
        ret = jsonToObject(file_content);
        
        if( ret == 0 ) {
            this->isReady = 1;
        }
        
    STICKER_PLAY_END:
        
        if( file_content != NULL )
            free(file_content);
        if( fp != NULL )
            fclose(fp);
        
        return ret;
    }
    
    void ContentSticker::clear()
    {
        this->isReady = 0;
        this->keys.clear();
        this->parts.clear();
    }
    
    int ContentSticker::lastFrameListToDisplay(std::vector<SingleFrame> &v)
    {
        int i = 0;
        int count = 0;
        
        for( i=0; i<this->parts.size(); i++ ) {
            
            SingleFrame sf;
            
            if( this->parts[i].lastFrameToDisplay(sf) > 0 ) {
                // insert at zPosition order
                int pos = (int)v.size();
                for( int j=0; j<v.size(); j++ ) {
                    SingleFrame t = v[j];
                    if( t.zPosition >= sf.zPosition ) {
                        pos = j;
                        break;
                    }
                }
                v.insert(v.begin()+pos, sf);
                //v.push_back(sf);
                count ++;
            }
        }
        
        return count;
    }
    
    int ContentSticker::frameListToDisplay(uint64_t timestamp, std::vector<SingleFrame> &v)
    {
        int i = 0;
        int count = 0;
        
        for( i=0; i<this->parts.size(); i++ ) {
            
            SingleFrame sf;
            
            if( this->parts[i].frameToDisplay(timestamp, sf) > 0 ) {
                // insert at zPosition order
                int pos = (int)v.size();
                for( int j=0; j<v.size(); j++ ) {
                    SingleFrame t = v[j];
                    if( t.zPosition >= sf.zPosition ) {
                        pos = j;
                        break;
                    }
                }
                v.insert(v.begin()+pos, sf);
                //v.push_back(sf);
                count ++;
            }
        }
        
        return count;
    }
    
    int ContentSticker::frameListToPreload(uint64_t timestamp, std::vector<SingleFrame> &v, int limit)
    {
        int i = 0;
        
        for( i=0; i<this->parts.size(); i++ ) {
            
            this->parts[i].preloadFrameList(timestamp, v, limit);
        }
        
        return (int)v.size();
    }
    
    int ContentSticker::frameListToUnload(uint64_t timestamp, std::vector<SingleFrame> &v, int limit)
    {
        int i = 0;
        
        for( i=0; i<this->parts.size(); i++ ) {
            
            this->parts[i].unloadFrameList(timestamp, v, limit);
        }
        
        return (int)v.size();
    }
    
    int ContentSticker::getDuration()
    {
        int duration = 0;
        int i = 0;
        int tmp = 0;
        
        for( i=0; i<this->parts.size(); i++ ) {
            
            tmp = (this->parts[i].frameCount * this->parts[i].interval);
            if( duration < tmp )
                duration = tmp;
        }
        
        return duration;
    }
    
    void ContentSticker::test(uint64_t timestamp)
    {
//        int i=0;
//        std::vector<std::string> v;
//
//        for( i=0; i<this->parts.size(); i++ ) {
//
//            PartSticker ps = this->parts[i];
//
//            std::string s = ps.fileNameToDisplay(timestamp);
//            v.clear();
//            ps.preloadFileList(timestamp, v, 5);
//            
//            STICKER_DEBUG("draw:%s", s.c_str());
//            for( int j=0; j<v.size(); j++ ) {
//                STICKER_DEBUG("preload:%s", v[j].c_str());
//            }
//        }
        
        return;
    }
    
    ContentSticker::ContentSticker()
    {
        this->isReady = 0;
    }
    
    ContentSticker::~ContentSticker()
    {
        
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    
    FramePlayer::FramePlayer()
    {
    }
    
    FramePlayer::~FramePlayer()
    {
        
    }

    int FramePlayer::playWithLoopCount(const char * path, int loops)
    {
        int ret = 0;
        
        std::string f = path;
        
        ret = current.loadJsonFile(f);
        if( ret < 0 )
            return ret;
        
        //this->timestamp = (uint64_t)sticker_system_current_milli();
        this->timestamp = 0;
        
        this->isAssetsReady = 0;
        this->loopCount = loops;// 一直循环
        
        return ret;
    }
    
    int FramePlayer::play(const char * path)
    {
        // -1 代表一直循环
        return this->playWithLoopCount(path, -1);
    }
    
    void FramePlayer::stop()
    {
        this->current.clear();
        this->timestamp = 0;
        this->isAssetsReady = 0;
        
        return;
    }
    
    void FramePlayer::unload()
    {
        this->isAssetsReady = 0;
        render.clearTextureCache();
    }
    
    void FramePlayer::setUniformInfo(int uniform, int posAttri, int coorAttri)
    {
        render.prepareProgramInfo(uniform, posAttri, coorAttri);
    }
    
    void FramePlayer::draw()
    {
        if( this->isAssetsReady == 0 )
            return;
        
        // 确保是发生一次load之后的draw，防止一次播放的时候，时间都用于load了
        if( this->timestamp == 0 ) {
            this->timestamp = (uint64_t)sticker_system_current_milli();
        }
        
        std::vector<SingleFrame> v;
        uint64_t now = (uint64_t)sticker_system_current_milli();
        uint64_t passed = now-this->timestamp;
        
        // 不循环，并且超时了
        if( this->loopCount == 0 ) {
            if( passed > current.getDuration() ) {
                return;
            }
        }
        
        // 不循环，保留最后一帧
        if( this->loopCount == -9999 ) {
            if( passed > current.getDuration() ) {
                // draw last frame
                passed = current.getDuration();
                current.lastFrameListToDisplay(v);
                render.drawTexture(v);
                return;
            }
        }
        
        current.frameListToDisplay(passed, v);
        
        render.drawTexture(v);
        
        return;
    }
    
    int FramePlayer::preloadFrames(int count, int ready)
    {
        int i = 1;
        std::vector<SingleFrame> v;
        uint64_t now = (uint64_t)sticker_system_current_milli();
        
        int c = (int)(now-this->timestamp);
        if( this->timestamp == 0 ) {
            c = 0;
        }
        if( c < 0 ) c = 0;
        
        // TODO:逻辑不好，重复添加
        current.frameListToPreload(c, v, count);
        render.preloadTexture(v);
        
        this->isAssetsReady = ready;
        
        return i;
    }
    
    void FramePlayer::preload()
    {
        int i = 1;
        std::vector<SingleFrame> v;
        uint64_t now = (uint64_t)sticker_system_current_milli();
        
        int c = (int)(now-this->timestamp);
        if( this->timestamp == 0 ) {
            c = 0;
        }
        if( c < 0 ) c = 0;
        
        while(i<10)
        {
            // TODO:逻辑不好，重复添加
            current.frameListToPreload(c, v, i);
            render.preloadTexture(v);
            i ++;
        }
        
        this->isAssetsReady = 1;
        
        return;
    }
    
    void FramePlayer::updateCanvasSize(int width, int height)
    {
        render.updateCanvasSize(width, height);
    }
    
    void FramePlayer::updateFacePoint(float * a, int s)
    {
        render.updateFacePoint(a, s);
    }
}

