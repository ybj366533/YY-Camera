//
//  Created by YY on 2018/1/19.
//  Copyright © 2018年 YY. All rights reserved.
//

#ifndef yy_ffmpeg_reader_h
#define yy_ffmpeg_reader_h

#include "yy_com_def.h"
#include "yy_data_queue.h"

#include <libavutil/imgutils.h>
#include <libavutil/samplefmt.h>
#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>

typedef struct ST_YY_FFMPEG_READER_T * HANDLE_YY_FFREADER;

typedef struct ST_YY_FFMPEG_READER_T {
    
    char            server_url[YY_COM_URL_LIMIT_LEN];
    
    pthread_t       read_thread_id;
    uint8_t         read_thread_running_flag;
    int             read_eof;    // read to end
    int             read_error;
    
    pthread_t       audio_decode_thread_id;
    uint8_t         audio_decode_thread_running_flag;
    int             audio_decode_error;
    
    pthread_t       video_decode_thread_id;
    uint8_t         video_decode_thread_running_flag;
    int             video_decode_error;
    
    int64_t         arrived_last_packet_milli;
    uint8_t         quit_flag;
    
    ST_YY_DATA_QUEUE*              weak_audio_frame_queue;
    ST_YY_DATA_QUEUE*              weak_video_frame_queue;
    
    ST_YY_DATA_QUEUE               audio_packet_decode_queue;                // for decode
    ST_YY_DATA_QUEUE               video_packet_decode_queue;                // for decode
    
    // for ffmpeg
    AVFormatContext *fmt_ctx;
    AVCodecContext *video_dec_ctx;
    AVCodecContext *audio_dec_ctx;
    
    int file_open_flag;
    int width;
    int height;
    enum AVPixelFormat pix_fmt;
    AVStream *video_stream;
    AVStream *audio_stream;
    
    int video_stream_idx;
    int audio_stream_idx;
    
    int order_seek_flag;        // order flag
    int order_seek_timestamp;   // ordered seek to some time
    int seek_index;             // every time we seek , cout up this index
    
    // player start-end milli
    int64_t                     valid_start_milli;
    int64_t                     valid_end_milli;
    
    // last seek timestamp
    int64_t                     last_order_seek_ts;
    int64_t                     last_seek_milli;
    
    // video rotate degree
    float                       video_rotate_degree;
    // demux mode
    int                         demux_mode_flag;
    
} ST_YY_FFMPEG_READER;

HANDLE_YY_FFREADER YY_ffreader_open(char * url, ST_YY_DATA_QUEUE* audio_queue, ST_YY_DATA_QUEUE* video_queue);
void YY_ffreader_close(HANDLE_YY_FFREADER handle);
int YY_ffreader_check_opened(HANDLE_YY_FFREADER handle);

// use this func for seek
void YY_ffreader_seekto(HANDLE_YY_FFREADER handle, int milli);
int YY_ffreader_inseeking(HANDLE_YY_FFREADER handle);

// use this func for clip play
void YY_ffreader_valid_range(HANDLE_YY_FFREADER handle, int64_t start_milli, int64_t end_milli);

// get duration
int YY_ffreader_get_duration(HANDLE_YY_FFREADER handle);

// get current seek index
int YY_ffreader_get_seekindex(HANDLE_YY_FFREADER handle);

// get video size info
int YY_ffreader_video_size(HANDLE_YY_FFREADER handle, int * w, int * h);

// check if stream audio/video status
int YY_ffreader_audio_existed(HANDLE_YY_FFREADER handle);
int YY_ffreader_video_existed(HANDLE_YY_FFREADER handle);

// check if all data is empty
int YY_ffreader_check_eof(HANDLE_YY_FFREADER handle);

// get video rotate
float YY_ffreader_video_rotate_degree(HANDLE_YY_FFREADER handle);

#endif /* YY_ffmpeg_reader_h */
