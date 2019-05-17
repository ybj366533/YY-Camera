//
//  gtv_live_player.h
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_live_player_h
#define gtv_live_player_h

#include <stdio.h>
#include "gtv_com_def.h"
#include "gtv_raw_frame.h"
#include "gtv_data_queue.h"
#include "gtv_data_buffer.h"

#ifdef __cplusplus
extern "C" {
#endif
    
typedef enum {
    
    GTV_PLAYER_EVT_INITED           = 0x9000,
    GTV_PLAYER_EVT_PREPARED         = 0x9001,
    GTV_PLAYER_EVT_FINISHED         = 0x9002
    
} GTV_PLAYER_EVENT;

typedef enum {
    
    GTV_PLAYER_NO_ERROR             = 0x0000,
    GTV_PLAYER_TIMEOUT_ERROR        = 0x1001,
    GTV_PLAYER_STREAM_ERROR         = 0x1002
    
} GTV_PLAYER_ERROR;

typedef enum {
    
    GTV_PLAYER_STREAM_OPENED        = 0x5000,
    GTV_PLAYER_STREAM_STREAMING     = 0x5001,
    GTV_PLAYER_STREAM_PAUSED        = 0x5002,
    GTV_PLAYER_STREAM_EOF           = 0x5003,
    GTV_PLAYER_STREAM_UNKNOWN       = 0x5099
    
} GTV_PLAYER_STREAM_STS;

typedef struct ST_GTV_LIVE_PLAYER_T * HANDLE_GTV_PLAYER;

typedef void (*FUNC_PLAYER_DATA_NOTIFY)(void * target, ST_GTV_RAW_FRAME_REF ref);
typedef void (*FUNC_PLAYER_EVENT_NOTIFY)(void * target, HANDLE_GTV_PLAYER handle, int event, int param1, int param2);

typedef struct ST_GTV_LIVE_PLAYER_T {
    
    char                        server_url[GTV_COM_URL_LIMIT_LEN];
    
    FUNC_PLAYER_EVENT_NOTIFY    event_callback;
    FUNC_PLAYER_DATA_NOTIFY     data_callback;
    void *                      target;
    
    pthread_t                   thread_id;
    uint8_t                     thread_running_flag;

    uint8_t                     quit_flag;
    
    ST_GTV_DATA_QUEUE           audio_pkt_queue;
    ST_GTV_DATA_QUEUE           video_pkt_queue;
    ST_GTV_DATA_QUEUE           redirect_pkt_queue;
    
	uint8_t						pause_flag;
	uint8_t						start_play_flag;

    int                         stream_sts;
    
    ST_GTV_DATA_BUFFER          audio_buffer;   // sound card cache
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
    
} ST_GTV_LIVE_PLAYER;

HANDLE_GTV_PLAYER gtv_player_open(char * url, FUNC_PLAYER_EVENT_NOTIFY evt_func, FUNC_PLAYER_DATA_NOTIFY data_func, void * target);

void gtv_player_close(HANDLE_GTV_PLAYER handle);

int gtv_player_set_range(HANDLE_GTV_PLAYER handle, int64_t start_milli, int64_t end_milli);
    
void gtv_player_set_pause_mode(HANDLE_GTV_PLAYER handle, uint8_t mode);
    
int gtv_player_get_duration(HANDLE_GTV_PLAYER handle);
    
int gtv_player_current_timestamp(HANDLE_GTV_PLAYER handle);
    
int gtv_player_seekto(HANDLE_GTV_PLAYER handle, int milli);
    
int gtv_player_pull_audio(HANDLE_GTV_PLAYER handle, uint8_t*buf, int len);

int gtv_player_peek_next_video(HANDLE_GTV_PLAYER handle, ST_GTV_RAW_FRAME_REF ref);
    
int gtv_player_remove_next_video(HANDLE_GTV_PLAYER handle);

int gtv_player_check_status(HANDLE_GTV_PLAYER handle);

#ifdef __cplusplus
}
#endif

#endif /* gtv_live_player_h */
