//
//  gtv_demuxer.h
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_live_demuxer_h
#define gtv_live_demuxer_h

#include <stdio.h>
#include "gtv_com_def.h"
#include "gtv_raw_frame.h"
#include "gtv_data_queue.h"
#include "gtv_data_buffer.h"

#ifdef __cplusplus
extern "C" {
#endif
    
typedef struct ST_GTV_LIVE_DEMUXER_T * HANDLE_GTV_DEMUXER;

typedef struct ST_GTV_LIVE_DEMUXER_T {
    
    ST_GTV_DATA_QUEUE           audio_pkt_queue;
    ST_GTV_DATA_QUEUE           video_pkt_queue;
    
    ST_GTV_DATA_BUFFER          audio_buffer;   // sound card cache
    
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
    
} ST_GTV_LIVE_DEMUXER;

HANDLE_GTV_DEMUXER gtv_demuxer_open(char * url);

void gtv_demuxer_close(HANDLE_GTV_DEMUXER handle);

int gtv_demuxer_set_range(HANDLE_GTV_DEMUXER handle, int64_t start_milli, int64_t end_milli);
    
int gtv_demuxer_pull_audio(HANDLE_GTV_DEMUXER handle, uint8_t * buff, int len);

int gtv_demuxer_peek_next_video(HANDLE_GTV_DEMUXER handle, ST_GTV_RAW_FRAME_REF ref);
    
int gtv_demuxer_remove_next_video(HANDLE_GTV_DEMUXER handle);

int gtv_demuxer_check_eof(HANDLE_GTV_DEMUXER handle);

int gtv_demuxer_seekto(HANDLE_GTV_DEMUXER handle, int milli);
    
#ifdef __cplusplus
}
#endif

#endif /* gtv_live_demuxer_h */
