//
//  sticker_player.hpp
//  YY
//
//  Created by YY on 2018/3/6.
//  Copyright © 2018年 YY. All rights reserved.
//

#ifndef sticker_player_hpp
#define sticker_player_hpp

#include <iostream>
#include <vector>
#include <string>

#include "sticker_render.hpp"

namespace gst
{
    class PartSticker {
        
    private:
        
        int                 getDisplayIndex(uint64_t timestamp);
        
    public:
        
        PartSticker();
        ~PartSticker();
    
        std::string         rootFolder;
        std::string         partName;
        
        int                 interval;
        int                 frameCount;
        int                 width;
        int                 height;
        
        int                 positionIndex;
        int                 positionX;
        int                 positionY;
        
        int                 zPosition;
        
        std::string         fileNameToDisplay(uint64_t timestamp);
        int                 preloadFrameList(uint64_t timestamp, std::vector<SingleFrame> &v, int limit);
        int                 unloadFrameList(uint64_t timestamp, std::vector<SingleFrame> &v, int limit);
        int                 frameToDisplay(uint64_t timestamp, SingleFrame &sf);
        int                 lastFrameToDisplay(SingleFrame &sf);
    };
    
    class ContentSticker {
        
    public:
        ContentSticker();
        ~ContentSticker();
        
        int loadJsonFile(const char * c);
        int loadJsonFile(std::string f);
        
        void clear();
        
        void test(uint64_t timestamp);
        
        int lastFrameListToDisplay(std::vector<SingleFrame> &v);
        int frameListToDisplay(uint64_t timestamp, std::vector<SingleFrame> &v);
        int frameListToPreload(uint64_t timestamp, std::vector<SingleFrame> &v, int limit);
        int frameListToUnload(uint64_t timestamp, std::vector<SingleFrame> &v, int limit);
        
        int getDuration();
        
    private:
        
        int jsonToObject(const char * c);
        
        int isReady;
        std::string         folder;
        
        std::vector<std::string> keys;
        std::vector<PartSticker> parts;
    };
    
    class FramePlayer {
        
    public:
        FramePlayer();
        ~FramePlayer();
        
        int playWithLoopCount(const char * path, int loops);
        int play(const char * path);
        void stop();
        
        void setUniformInfo(int uniform, int posAttri, int coorAttri);
        void draw();
        
        int preloadFrames(int count, int ready);
        void preload();
        void unload();
        
        void updateCanvasSize(int width, int height);
        void updateFacePoint(float * a, int s);
        
    private:
        
        int                 isAssetsReady;
        
        std::string         stickerFolder;
        
        uint64_t            timestamp;
        int                 loopCount;
        
        ContentSticker      current;
        GLESStickerRender   render;
    };
}

#endif /* sticker_player_hpp */
