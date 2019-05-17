//
//  GTVideoPlayer.m
//  gtv
//
//  Created by gtv on 2017/5/1.
//  Copyright © 2017年 gtv. All rights reserved.
//

#import "GTVideoPlayer.h"

#import "GTVAudioUnitPlayer.h"

#include "gtv_player.h"
#include "gtv_logger.h"
#include "gtv_data_buffer.h"

static void gtv_player_event_callback(void * target, HANDLE_GTV_PLAYER handle, int event, int param1, int param2);
static void gtv_player_data_callback(void * target, ST_GTV_RAW_FRAME_REF ref);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
static GTVideoPlayerPool * _player_pool_instance = nil;

@interface GTVideoPlayerPool()

@property (nonatomic, strong) NSMutableArray * playerList;

@end

@implementation GTVideoPlayerPool

- (id) init
{
    self = [super init];
    if(self) {
        self.playerList = [[NSMutableArray alloc] init];
    }
    
    return self;
}

- (void) addPlayer:(GTVideoPlayer*)p
{
    @synchronized (self.playerList) {
        if( [self.playerList containsObject:p] == false ) {
            [self.playerList addObject:p];
        }
    }
}

- (void) removePlayer:(GTVideoPlayer*)p
{
    @synchronized (self.playerList) {
        [self.playerList removeObject:p];
    }
}

- (BOOL) constainsPlayer:(GTVideoPlayer*)p
{
    @synchronized (self.playerList) {
        return [self.playerList containsObject:p];
    }
    
    return false;
}

+ (GTVideoPlayerPool*)getInstance
{
    if( _player_pool_instance == nil ) {
        @synchronized ([GTVideoPlayerPool class]) {
            if( _player_pool_instance == nil ) {
                _player_pool_instance = [[GTVideoPlayerPool alloc] init];
            }
        }
    }
    
    return _player_pool_instance;
}

+ (void) addLivePlayer:(GTVideoPlayer*)p
{
    [[GTVideoPlayerPool getInstance] addPlayer:p];
}

+ (void) removeLivePlayer:(GTVideoPlayer*)p
{
    [[GTVideoPlayerPool getInstance] removePlayer:p];
}

+ (BOOL) constainsPlayer:(GTVideoPlayer*)p
{
    return [[GTVideoPlayerPool getInstance] constainsPlayer:p];
}

@end

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@interface GTVideoPlayer() <GTVAudioUnitPlayerDelegate> {
    
    HANDLE_GTV_PLAYER _player_hander;
    ST_GTV_DATA_BUFFER _audio_buffer;
    BOOL _outsideAudioSyncFlg;
    BOOL _withoutAudioPlayerFlg;
    NSRange _range;
}

@property (nonatomic, strong) NSString * serverUrl;
@property (nonatomic, strong) GTVAudioUnitPlayer * audioPlayer;

- (void) onPlayerEventPosted:(HANDLE_GTV_PLAYER)handle eventNumber:(int)event eventParamFirst:(int)param1 eventParamSecond:(int)param2;

@end

@implementation GTVideoPlayer

- (void) dealloc
{
    if( _player_hander != NULL ) {
        gtv_player_close(_player_hander);
        _player_hander = NULL;
    }
    
    gtv_databuffer_destroy(&_audio_buffer);
}

- (void) useOutsideAudioSync:(BOOL)flag
{
    _outsideAudioSyncFlg = flag;
}

// only used with playWithoutDataCallback
- (int) peekNextVideo:(ST_GTV_YUV_FRAME*)yuv
{
    ST_GTV_RAW_FRAME raw_frame;
    int ret = 0;
    
    if( yuv == NULL )
        return 0;
    
    if( _player_hander == NULL )
        return 0;
    
    ret = gtv_player_peek_next_video(_player_hander, &raw_frame);
    if( ret > 0 ) {
        
        yuv->pixel_width = raw_frame.pixel_width;
        yuv->pixel_height = raw_frame.pixel_height;
        
        yuv->plane_data[0] = raw_frame.plane_data[0];
        yuv->plane_data[1] = raw_frame.plane_data[1];
        yuv->plane_data[2] = raw_frame.plane_data[2];
        
        yuv->plane_size[0] = raw_frame.plane_size[0];
        yuv->plane_size[1] = raw_frame.plane_size[1];
        yuv->plane_size[2] = raw_frame.plane_size[2];
        
        yuv->stride_size[0] = raw_frame.stride_size[0];
        yuv->stride_size[1] = raw_frame.stride_size[1];
        yuv->stride_size[2] = raw_frame.stride_size[2];
    }
    
    return ret;
}

- (int) removeNextVideo
{
    if( _player_hander == NULL )
        return 0;
    
    return gtv_player_remove_next_video(_player_hander);
}

- (int) pullAudio:(uint8_t*)data withLen:(int)len
{
    int ret = 0;
    
    if( _withoutAudioPlayerFlg == true ) {
        return gtv_player_pull_audio(_player_hander, data, len);
    }
    
    if( _outsideAudioSyncFlg == false ) {
        GTV_ERROR("_outsideAudioSyncFlg is false.");
        return 0;
    }
    
    if( _player_hander != NULL ) {
        ret = gtv_player_pull_audio(_player_hander, data, len);
        if( ret > 0 ) {
            // only put what we get
            gtv_databuffer_put(&_audio_buffer, data, ret, 0);
        }
        return ret;
    }
    
    return 0;
}

- (id) initWithURLString:(NSString*)url withoutAudioPlayer:(BOOL)flag
{
    self = [super init];
    if(self) {
        self.serverUrl = url;
    }
    [GTVideoPlayerPool addLivePlayer:self];
    
    _outsideAudioSyncFlg = false;
    _withoutAudioPlayerFlg = flag;
    gtv_databuffer_init(&_audio_buffer, 8192);
    
    return self;
}

- (id) initWithURLString:(NSString*)url
{
    self = [super init];
    if(self) {
        self.serverUrl = url;
    }
    [GTVideoPlayerPool addLivePlayer:self];
    
    _outsideAudioSyncFlg = false;
    _withoutAudioPlayerFlg = false;
    gtv_databuffer_init(&_audio_buffer, 8192);
    
    return self;
}

- (void) playWithoutDataCallback
{
    if( _player_hander != NULL ) {
        GTV_WARN("player already started.");
        return;
    }
    const char * server_url = [self.serverUrl cStringUsingEncoding:NSUTF8StringEncoding];
    
    _player_hander = gtv_player_open((char*)server_url, gtv_player_event_callback, NULL, (__bridge void*)self);
    
    if( _range.length > 0 )
        gtv_player_set_range(_player_hander, (int64_t)_range.location, (int64_t)(_range.location+_range.length));
    
    if( _withoutAudioPlayerFlg == false ) {
        self.audioPlayer = [[GTVAudioUnitPlayer alloc] init];
        self.audioPlayer.delegate = self;
        [self.audioPlayer play];
    }
    
    return;
}

- (void) play
{
    if( _player_hander != NULL ) {
        GTV_WARN("player already started.");
        return;
    }
    const char * server_url = [self.serverUrl cStringUsingEncoding:NSUTF8StringEncoding];
    
    _player_hander = gtv_player_open((char*)server_url, gtv_player_event_callback, gtv_player_data_callback, (__bridge void*)self);
    
    if( _range.length > 0 )
        gtv_player_set_range(_player_hander, (int64_t)_range.location, (int64_t)(_range.location+_range.length));
    
    if( _withoutAudioPlayerFlg == false ) {
        self.audioPlayer = [[GTVAudioUnitPlayer alloc] init];
        self.audioPlayer.delegate = self;
        [self.audioPlayer play];
    }
    
    return;
}

- (void) close
{
    [self.audioPlayer close];
    self.audioPlayer.delegate = nil;
    self.audioPlayer = nil;
    
    if( _player_hander != NULL ) {
        
        void* t = (void*)_player_hander;
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
            gtv_player_close(t);
        });
        _player_hander = NULL;
    }
    
    // 3秒后移除对象
    [self performSelector:@selector(innerRemoveObject) withObject:nil afterDelay:3.0f];
}

- (void) setPause:(BOOL)flag
{
    if( _player_hander != NULL ) {
        gtv_player_set_pause_mode(_player_hander, flag?1:0);
    }
    
    return;
}

- (int) currentTimestamp
{
    if( _player_hander != NULL ) {
        return gtv_player_current_timestamp(_player_hander);
    }
    
    return -1;
}

- (int) getDuration
{
    if( _player_hander != NULL ) {
        return gtv_player_get_duration(_player_hander);
    }
    
    return -1;
}

- (void) seekTo:(int)milli
{
    if( _player_hander != NULL ) {
        gtv_player_seekto(_player_hander, milli);
    }
}

- (void) setValidRange:(NSRange)r
{
    _range = r;
    
    if( _player_hander != NULL ) {
        if( _range.length > 0 )
            gtv_player_set_range(_player_hander, (int64_t)_range.location, (int64_t)(_range.location+_range.length));
    }
}

- (void) innerRemoveObject
{
    [GTVideoPlayerPool removeLivePlayer:self];
}

- (int) getStreamStatus
{
    if( _player_hander != NULL ) {
        return gtv_player_check_status(_player_hander);
    }
    
    return k_GTVPLAYER_STREAM_UNKNOWN;
}

- (int) onQueryAudioData:(NSObject*)p withBuffer:(uint8_t*)buf andSize:(int)len
{
    if( self.audioPlayer != p ) {
        return 0;
    }
    
    if( _outsideAudioSyncFlg == false ) {
     
        if( _player_hander != NULL ) {
            return gtv_player_pull_audio(_player_hander, buf, len);
        }
        
        return 0;
    }
    else {
        
        return gtv_databuffer_get(&_audio_buffer, buf, len);
    }
    
    return 0;
}

- (void) onPlayerEventPosted:(HANDLE_GTV_PLAYER)handle eventNumber:(int)event eventParamFirst:(int)param1 eventParamSecond:(int)param2
{
    if( _player_hander != handle )
        return;
    
    if( self.delegate != nil && [self.delegate respondsToSelector:@selector(onPlayer:processEventdata:eventParamFirst:eventParamSecond:)] ) {
        [self.delegate onPlayer:self processEventdata:event eventParamFirst:param1 eventParamSecond:param2];
    }
    
    return;
}

+ (void) setLogLevel:(int)level
{
    gtv_logger_set_level(level);
}

+ (void) setLogCallback:(gtv_player_logcallback)cb
{
    gtv_logger_set_callback(cb);
}

@end

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

static void gtv_player_event_callback(void * target, HANDLE_GTV_PLAYER handle, int event, int param1, int param2)
{
    if( target == NULL || handle == NULL ) {
        return;
    }
    
    GTVideoPlayer * player = (__bridge GTVideoPlayer*)target;
    
    if( [GTVideoPlayerPool constainsPlayer:player] == false ) {
        return;
    }
    
    [player onPlayerEventPosted:handle eventNumber:event eventParamFirst:param1 eventParamSecond:param2];
    
    return;
}

static void gtv_player_data_callback(void * target, ST_GTV_RAW_FRAME_REF ref)
{
    if( target == NULL || ref == NULL ) {
        return;
    }
    
    GTVideoPlayer * player = (__bridge GTVideoPlayer*)target;
    
    if( [GTVideoPlayerPool constainsPlayer:player] == false ) {
        return;
    }
    
    ST_GTV_YUV_FRAME yuv;
    
    yuv.pixel_width = ref->pixel_width;
    yuv.pixel_height = ref->pixel_height;
    
    yuv.plane_data[0] = ref->plane_data[0];
    yuv.plane_data[1] = ref->plane_data[1];
    yuv.plane_data[2] = ref->plane_data[2];
    
    yuv.plane_size[0] = ref->plane_size[0];
    yuv.plane_size[1] = ref->plane_size[1];
    yuv.plane_size[2] = ref->plane_size[2];
    
    yuv.stride_size[0] = ref->stride_size[0];
    yuv.stride_size[1] = ref->stride_size[1];
    yuv.stride_size[2] = ref->stride_size[2];
    
    if( player.delegate != nil && [player.delegate respondsToSelector:@selector(onPlayer:processImagebuffer:)] ) {
        [player.delegate onPlayer:player processImagebuffer:&yuv];
    }
    
    return;
}
