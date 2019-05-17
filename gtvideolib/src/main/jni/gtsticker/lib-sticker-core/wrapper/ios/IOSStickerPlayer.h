//
//  IOSStickerPlayer.h
//  gtv
//
//  Created by gtv on 2018/3/7.
//  Copyright © 2018年 gtv. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface IOSStickerPlayer : NSObject

// must be called on gles thread
- (void) startPlaySticker:(NSString*)folder;
- (void) startPlaySticker:(NSString*)folder withLoopCount:(int)loops;

// must be called on gles thread
- (void) stopPlaySticker;

- (void) prepareTexture;
- (void) clearTexture;

//int uniform, int posAttri, int coorAttri
- (void) drawWithUniform:(int)unif andPosition:(int)pos andTextureCoor:(int)coor;

- (void) udpateCanvasWidth:(int)width andHeight:(int)height;
- (void) updateFacePoint:(float*)arr withSize:(int)siz;

@end
