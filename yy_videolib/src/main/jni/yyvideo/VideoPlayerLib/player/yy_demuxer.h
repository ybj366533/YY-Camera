//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef yy_live_demuxer_h
#define yy_live_demuxer_h

#include <stdio.h>
#include "yy_com_def.h"
#include "yy_raw_frame.h"
#include "yy_data_queue.h"
#include "yy_data_buffer.h"

#ifdef __cplusplus
extern "C" {
#endif
    
typedef struct ST_YY_LIVE_DEMUXER_T * HANDLE_YY_DEMUXER;

typedef struct ST_YY_LIVE_DEMUXER_T {
    
    ST_YY_DATA_QUEUE           audio_pkt_queue;
    ST_YY_DATA_QUEUE           video_pkt_queue;
    
    ST_YY_DATA_BUFFER          audio_buffer;   // sound card cache
    
    //struct AudioParams audio_tgt;
    //struct SwrContext *swr_ctx;
    void *                      swr_ctx_addr;
    uint8_t                     audio_swr_cache[4096*4];
    int                         audio_swr_len;
    
    // reader addr
    void *                      reader_addr;
    
    // demux start-end milli
    int64_t                     valid_start_milli;
    int64_t                     valid_end_milli;
    
} ST_YY_LIVE_DEMUXER;

HANDLE_YY_DEMUXER YY_demuxer_open(char * url);

void YY_demuxer_close(HANDLE_YY_DEMUXER handle);

int YY_demuxer_set_range(HANDLE_YY_DEMUXER handle, int64_t start_milli, int64_t end_milli);
    
int YY_demuxer_pull_audio(HANDLE_YY_DEMUXER handle, uint8_t * buff, int len);

int YY_demuxer_peek_next_video(HANDLE_YY_DEMUXER handle, ST_YY_RAW_FRAME_REF ref);
    
int YY_demuxer_remove_next_video(HANDLE_YY_DEMUXER handle);

int YY_demuxer_check_eof(HANDLE_YY_DEMUXER handle);

int YY_demuxer_seekto(HANDLE_YY_DEMUXER handle, int milli);
    
#ifdef __cplusplus
}
#endif

#endif /* YY_live_demuxer_h */
