//
//  GTVideoDemuxer.h
//  gtv
//
//  Created by gtv on 2017/5/1.
//  Copyright © 2017年 gtv. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

typedef struct {
    
    int64_t     timestamp;
    
    int         pixel_width;
    int         pixel_height;
    
    uint8_t *   plane_data[3];
    uint32_t    plane_size[3];
    uint32_t    stride_size[3];
    
} ST_GTV_YUV_VIDEO_FRAME;

@interface GTVideoDemuxer : NSObject

- (id) initWithURLString:(NSString*)url;

- (int) open;
- (void) close;

- (void) seekTo:(int)milli;
- (void) setValidRange:(NSRange)r;

- (int) pullAudio:(uint8_t*)data withLen:(int)len;

- (int) peekNextVideo:(ST_GTV_YUV_VIDEO_FRAME*)yuv;
- (int) removeNextVideo;

- (BOOL) checkStreamEOF;

@end

