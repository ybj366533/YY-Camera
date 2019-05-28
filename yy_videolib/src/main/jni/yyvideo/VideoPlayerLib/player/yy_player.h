//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef yy_live_player_h
#define yy_live_player_h

#include <stdio.h>
#include "yy_com_def.h"
#include "yy_raw_frame.h"
#include "yy_data_queue.h"
#include "yy_data_buffer.h"

#ifdef __cplusplus
extern "C" {
#endif
    
typedef enum {
    
    YY_PLAYER_EVT_INITED           = 0x9000,
    YY_PLAYER_EVT_PREPARED         = 0x9001,
    YY_PLAYER_EVT_FINISHED         = 0x9002
    
} YY_PLAYER_EVENT;

typedef enum {
    
    YY_PLAYER_NO_ERROR             = 0x0000,
    YY_PLAYER_TIMEOUT_ERROR        = 0x1001,
    YY_PLAYER_STREAM_ERROR         = 0x1002
    
} YY_PLAYER_ERROR;

typedef enum {
    
    YY_PLAYER_STREAM_OPENED        = 0x5000,
    YY_PLAYER_STREAM_STREAMING     = 0x5001,
    YY_PLAYER_STREAM_PAUSED        = 0x5002,
    YY_PLAYER_STREAM_EOF           = 0x5003,
    YY_PLAYER_STREAM_UNKNOWN       = 0x5099
    
} YY_PLAYER_STREAM_STS;

typedef struct ST_YY_LIVE_PLAYER_T * HANDLE_YY_PLAYER;

typedef void (*FUNC_PLAYER_DATA_NOTIFY)(void * target, ST_YY_RAW_FRAME_REF ref);
typedef void (*FUNC_PLAYER_EVENT_NOTIFY)(void * target, HANDLE_YY_PLAYER handle, int event, int param1, int param2);

typedef struct ST_YY_LIVE_PLAYER_T {
    
    char                        server_url[YY_COM_URL_LIMIT_LEN];
    
    FUNC_PLAYER_EVENT_NOTIFY    event_callback;
    FUNC_PLAYER_DATA_NOTIFY     data_callback;
    void *                      target;
    
    pthread_t                   thread_id;
    uint8_t                     thread_running_flag;

    uint8_t                     quit_flag;
    
    ST_YY_DATA_QUEUE           audio_pkt_queue;
    ST_YY_DATA_QUEUE           video_pkt_queue;
    ST_YY_DATA_QUEUE           redirect_pkt_queue;
    
	uint8_t						pause_flag;
	uint8_t						start_play_flag;

    int                         stream_sts;
    
    ST_YY_DATA_BUFFER          audio_buffer;   // sound card cache
    int64_t                     current_timestamp;
    
    //struct AudioParams audio_tgt;
    //struct SwrContext *swr_ctx;
    void *                      swr_ctx_addr;
    uint8_t                     audio_swr_cache[4096*4];
    int                         audio_swr_len;
    
    // reader addr
    void *                      reader_addr;
    
    // player start-end milli
    int64_t                     valid_start_milli;
    int64_t                     valid_end_milli;
    
    int                         first_rend;
    
} ST_YY_LIVE_PLAYER;

HANDLE_YY_PLAYER YY_player_open(char * url, FUNC_PLAYER_EVENT_NOTIFY evt_func, FUNC_PLAYER_DATA_NOTIFY data_func, void * target);

void YY_player_close(HANDLE_YY_PLAYER handle);

int YY_player_set_range(HANDLE_YY_PLAYER handle, int64_t start_milli, int64_t end_milli);
    
void YY_player_set_pause_mode(HANDLE_YY_PLAYER handle, uint8_t mode);
    
int YY_player_get_duration(HANDLE_YY_PLAYER handle);
    
int YY_player_current_timestamp(HANDLE_YY_PLAYER handle);
    
int YY_player_seekto(HANDLE_YY_PLAYER handle, int milli);
    
int YY_player_pull_audio(HANDLE_YY_PLAYER handle, uint8_t*buf, int len);

int YY_player_peek_next_video(HANDLE_YY_PLAYER handle, ST_YY_RAW_FRAME_REF ref);
    
int YY_player_remove_next_video(HANDLE_YY_PLAYER handle);

int YY_player_check_status(HANDLE_YY_PLAYER handle);

#ifdef __cplusplus
}
#endif

#endif /* YY_live_player_h */
