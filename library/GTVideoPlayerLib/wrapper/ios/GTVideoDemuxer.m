//
//  GTVideoDemuxer.m
//  gtv
//
//  Created by gtv on 2017/5/1.
//  Copyright © 2017年 gtv. All rights reserved.
//

#import "GTVideoDemuxer.h"

#include "gtv_demuxer.h"
#include "gtv_logger.h"

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@interface GTVideoDemuxer() {
    
    HANDLE_GTV_DEMUXER _demuxer_hander;
    NSRange _range;
}

@property (nonatomic, strong) NSString * serverUrl;

@end

@implementation GTVideoDemuxer

- (void) dealloc
{
    if( _demuxer_hander != NULL ) {
        gtv_demuxer_close(_demuxer_hander);
        _demuxer_hander = NULL;
    }
}

// only used with playWithoutDataCallback
- (int) peekNextVideo:(ST_GTV_YUV_VIDEO_FRAME*)yuv
{
    ST_GTV_RAW_FRAME raw_frame;
    int ret = 0;
    
    if( yuv == NULL )
        return 0;
    
    if( _demuxer_hander == NULL )
        return 0;
    
    ret = gtv_demuxer_peek_next_video(_demuxer_hander, &raw_frame);
    if( ret > 0 ) {
        
        yuv->timestamp = raw_frame.timestamp;
        
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
    if( _demuxer_hander == NULL )
        return 0;
    
    return gtv_demuxer_remove_next_video(_demuxer_hander);
}

- (int) pullAudio:(uint8_t*)data withLen:(int)len
{
    return gtv_demuxer_pull_audio(_demuxer_hander, data, len);
}

- (id) initWithURLString:(NSString*)url
{
    self = [super init];
    if(self) {
        self.serverUrl = url;
    }
    
    return self;
}

- (int) open
{
    if( _demuxer_hander != NULL ) {
        GTV_WARN("demuxer already started.");
        return -1;
    }
    const char * server_url = [self.serverUrl cStringUsingEncoding:NSUTF8StringEncoding];
    
    _demuxer_hander = gtv_demuxer_open((char*)server_url);
    
    if( _demuxer_hander == NULL ) {
        return -2;
    }
    
    if( _range.length > 0 ) {
        gtv_demuxer_set_range(_demuxer_hander, (int64_t)_range.location, (int64_t)(_range.location+_range.length));
        gtv_demuxer_seekto(_demuxer_hander, (int)_range.location);
    }
    
    return 0;
}

- (void) close
{
    if( _demuxer_hander != NULL ) {
        
        gtv_demuxer_close(_demuxer_hander);
        _demuxer_hander = NULL;
    }
}

- (void) setValidRange:(NSRange)r
{
    _range = r;
    
    if( _demuxer_hander != NULL ) {
        if( _range.length > 0 )
            gtv_demuxer_set_range(_demuxer_hander, (int64_t)_range.location, (int64_t)(_range.location+_range.length));
    }
}

- (BOOL) checkStreamEOF;
{
    if( _demuxer_hander != NULL ) {
        if( gtv_demuxer_check_eof(_demuxer_hander) == 1 )
        return true;
    }
    
    return false;
}

- (void) seekTo:(int)milli
{
    if( _demuxer_hander != NULL ) {
        gtv_demuxer_seekto(_demuxer_hander, milli);
    }
}

@end
