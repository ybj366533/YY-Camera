//
//  GTVideoPlayer.h
//  gtv
//
//  Created by gtv on 2017/5/1.
//  Copyright © 2017年 gtv. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <VideoToolbox/VideoToolbox.h>

typedef enum
{
    k_GTVPLAYER_LOGDEBUG = 0,
    k_GTVPLAYER_LOGWARNING,
    k_GTVPLAYER_LOGINFO,
    k_GTVPLAYER_LOGERROR,
    k_GTVPLAYER_LOGCRIT
    
} k_GTVPLAYER_LOGLEVEL;

typedef enum {
    
    k_GTVPLAYER_EVT_INITED           = 0x9000,
    k_GTVPLAYER_EVT_PREPARED         = 0x9001,
    k_GTVPLAYER_EVT_FINISHED         = 0x9002

} k_GTVPLAYER_EVENT;

typedef enum {
    
    k_GTVPLAYER_NO_ERROR             = 0x0000,
    k_GTVPLAYER_TIMEOUT_ERROR        = 0x1001,
    k_GTVPLAYER_STREAM_ERROR         = 0x1002
    
} k_GTVPLAYER_ERROR;

typedef enum {
    
    k_GTVPLAYER_STREAM_OPENED        = 0x5000,
    k_GTVPLAYER_STREAM_STREAMING     = 0x5001,
    k_GTVPLAYER_STREAM_PAUSED        = 0x5002,
    k_GTVPLAYER_STREAM_EOF           = 0x5003,
    k_GTVPLAYER_STREAM_UNKNOWN       = 0x5099
    
} k_GTVPLAYER_STREAM_STS;

typedef struct {
    
    int         pixel_width;
    int         pixel_height;
    
    uint8_t *   plane_data[3];
    uint32_t    plane_size[3];
    uint32_t    stride_size[3];
    
} ST_GTV_YUV_FRAME;

typedef void (*gtv_player_logcallback)(const char *data);

@class GTVideoPlayer;

@protocol GTVideoPlayerDelegate <NSObject>

- (void) onPlayer:(GTVideoPlayer*)p processImagebuffer:(ST_GTV_YUV_FRAME*)yuv;
- (void) onPlayer:(GTVideoPlayer*)p processEventdata:(int)event eventParamFirst:(int)param1 eventParamSecond:(int)param2;

@end

@interface GTVideoPlayer : NSObject

@property (nonatomic, weak) id<GTVideoPlayerDelegate> delegate;

- (id) initWithURLString:(NSString*)url withoutAudioPlayer:(BOOL)flag;
- (id) initWithURLString:(NSString*)url;
- (void) play;
- (void) playWithoutDataCallback;
- (void) close;

- (void) setPause:(BOOL)flag;
- (int) getDuration;
- (void) seekTo:(int)milli;
- (int) currentTimestamp;
- (void) setValidRange:(NSRange)r;

- (void) useOutsideAudioSync:(BOOL)flag;
- (int) pullAudio:(uint8_t*)data withLen:(int)len;

// only used with playWithoutDataCallback
- (int) peekNextVideo:(ST_GTV_YUV_FRAME*)yuv;
- (int) removeNextVideo;

- (int) getStreamStatus;

+ (void) setLogLevel:(int)level;
+ (void) setLogCallback:(gtv_player_logcallback)cb;

@end

@interface GTVideoPlayerPool : NSObject

@end
