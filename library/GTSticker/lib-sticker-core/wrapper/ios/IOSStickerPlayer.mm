//
//  IOSStickerPlayer.m
//  gtv
//
//  Created by gtv on 2018/3/7.
//  Copyright © 2018年 gtv. All rights reserved.
//

#import "IOSStickerPlayer.h"

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <Foundation/Foundation.h>

#if TARGET_IPHONE_SIMULATOR || TARGET_OS_IPHONE
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#else
#import <OpenGL/OpenGL.h>
#import <OpenGL/gl.h>
#endif

#include "sticker_player.hpp"

#define glError() { \
GLenum err = glGetError(); \
if (err != GL_NO_ERROR) { \
printf("glError: %04x caught at %s:%u\n", err, __FILE__, __LINE__); \
} \
}
@interface IOSStickerPlayer() {
    
    gst::FramePlayer _corePlayer;
}

//@property (nonatomic, strong)  NSTimer * timer;

@end

@implementation IOSStickerPlayer

- (void) startPlaySticker:(NSString*)folder withLoopCount:(int)loops
{
    const char * p = [folder cStringUsingEncoding:NSUTF8StringEncoding];
    
    _corePlayer.playWithLoopCount(p, loops);
    
    return;
}

- (void) startPlaySticker:(NSString*)folder
{
    const char * p = [folder cStringUsingEncoding:NSUTF8StringEncoding];

    _corePlayer.play(p);
    
    return;
}

- (void) stopPlaySticker
{
    _corePlayer.stop();
    
    return;
}

- (void) prepareTexture
{
    _corePlayer.preload();
}

- (void) clearTexture
{
    _corePlayer.unload();
}

- (void) drawWithUniform:(int)unif andPosition:(int)pos andTextureCoor:(int)coor
{
    _corePlayer.setUniformInfo(unif, pos, coor);
    _corePlayer.draw();
    
    return;
}

- (void) updateFacePoint:(float*)arr withSize:(int)siz
{
    _corePlayer.updateFacePoint(arr, siz);
    
    return;
}

- (void) udpateCanvasWidth:(int)width andHeight:(int)height
{
    _corePlayer.updateCanvasSize(width, height);
}

@end
