//
//  Created by YY on 2018/1/19.
//  Copyright © 2018年 YY. All rights reserved.
//

#include "yy_ffmpeg_reader.h"

#include "stdio.h"
#include "stdlib.h"
#include "string.h"
#include "pthread.h"
#include "unistd.h"

#include "yy_logger.h"
#include "yy_com_utils.h"

#include "yy_raw_frame.h"

#if defined(TARGET_OS_ANDROID)
#include "videoplayer_jni.h"
#endif

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// private function
////////////////////////////////////////////////////////////////////////////////////////////////////////////
static void frame_rotate_90( AVFrame *src,AVFrame*des)
{
    int m_srcW = src->width;
    int m_srcH = src->height;
    
    for (int i = 0; i < m_srcW; i++)
    {
        for (int j = 0; j < m_srcH; j++)
        {
            des->data[0][i * des->linesize[0] + j] =
            src->data[0][(m_srcH - j - 1) * src->linesize[0] + i];
        }
    }

    for (int i = 0; i < m_srcW / 2; i++)
    {
        for (int j = 0; j < m_srcH / 2; j++)
        {
            des->data[1][i * des->linesize[1] + j] =
            src->data[1][(m_srcH / 2 - j - 1) * src->linesize[1] + i];
            
            des->data[2][i * des->linesize[2] + j] =
            src->data[2][(m_srcH / 2 - j - 1) * src->linesize[2] + i];
        }
    }
    
    des->width = src->height;
    des->height = src->width;
    
    return;
}

static void frame_rotate_270(AVFrame *src,AVFrame*des)
{
    int m_srcW = src->width;
    int m_srcH = src->height;
    
    for (int i = 0; i < m_srcW; i++)
    {
        for (int j = 0; j < m_srcH; j++)
        {
            des->data[0][i * des->linesize[0] + j] =
            src->data[0][j * src->linesize[0] + (m_srcW - i - 1)];
        }
    }
    
    for (int i = 0; i < m_srcW / 2; i++)
    {
        for (int j = 0; j < m_srcH / 2; j++)
        {
            des->data[1][i * des->linesize[1] + j] =
            src->data[1][j * src->linesize[1] + (m_srcW/2 - i - 1)];
            
            des->data[2][i * des->linesize[2] + j] =
            src->data[2][j * src->linesize[2] + (m_srcW/2 - i - 1)];
        }
    }
    
    des->width = src->height;
    des->height = src->width;
    
    return;
}

static void frame_rotate_180(AVFrame *src,AVFrame*des)
{
    int m_srcW = src->width;
    int m_srcH = src->height;
    
    for (int i = 0; i < m_srcH; i++)
    {
        for (int j = 0; j < m_srcW; j++)
        {
            des->data[0][i*des->linesize[0] + j] =
            src->data[0][(m_srcH-i-1)*src->linesize[0] + j];
        }
    }
    
    for (int i = 0; i < m_srcH / 2; i++)
    {
        for (int j = 0; j < m_srcW / 2; j++)
        {
            des->data[1][i*des->linesize[1] + j] =
            src->data[1][(m_srcH/2-i-1)*src->linesize[1] + j];
            
            des->data[2][i*des->linesize[2] + j] =
            src->data[2][(m_srcH/2-i-1)*src->linesize[2] + j];
        }
    }
    
    des->width = src->width;
    des->height = src->height;
    
    return;
}

static double YY_ffreader_get_rotation(AVStream *st)
{
    AVDictionaryEntry *rotate_tag = av_dict_get(st->metadata, "rotate", NULL, 0);
    double theta = 0;
    
    if (rotate_tag && *rotate_tag->value && strcmp(rotate_tag->value, "0")) {
        theta = atof(rotate_tag->value);
    }
    
    theta -= 360*floor(theta/360 + 0.9/360);
    
    if (fabs(theta - 90*round(theta/90)) > 2)
        YY_ERROR("Odd rotation angle");
    
    YY_ERROR("get_rotation %f",theta);
    
    return theta;
}

static int open_codec_context(HANDLE_YY_FFREADER reader, int *stream_idx, AVCodecContext **dec_ctx, AVFormatContext *fmt_ctx, enum AVMediaType type)
{
    int ret, stream_index;
    AVStream *st;
    AVCodec *dec = NULL;
    AVDictionary *opts = NULL;
    
    ret = av_find_best_stream(fmt_ctx, type, -1, -1, NULL, 0);
    if (ret < 0) {
        fprintf(stderr, "Could not find %s stream in input file '%s'\n",
                av_get_media_type_string(type), reader->server_url);
        return ret;
    } else {
        stream_index = ret;
        st = reader->fmt_ctx->streams[stream_index];
        
        dec = avcodec_find_decoder(st->codecpar->codec_id);
        if (!dec) {
            fprintf(stderr, "Failed to find %s codec\n",
                    av_get_media_type_string(type));
            return AVERROR(EINVAL);
        }
        
        // Allocate a codec context for the decoder
        *dec_ctx = avcodec_alloc_context3(dec);
        if (!*dec_ctx) {
            fprintf(stderr, "Failed to allocate the %s codec context\n",
                    av_get_media_type_string(type));
            return AVERROR(ENOMEM);
        }
        
        // Copy codec parameters from input stream to output codec context
        if ((ret = avcodec_parameters_to_context(*dec_ctx, st->codecpar)) < 0) {
            fprintf(stderr, "Failed to copy %s codec parameters to decoder context\n",
                    av_get_media_type_string(type));
            return ret;
        }
        
        // Init the decoders, without reference counting ?? todo
        av_dict_set(&opts, "refcounted_frames", "0", 0);
        if ((ret = avcodec_open2(*dec_ctx, dec, &opts)) < 0) {
            fprintf(stderr, "Failed to open %s codec\n",
                    av_get_media_type_string(type));
            return ret;
        }
        *stream_idx = stream_index;
    }
    
    return 0;
}

// This does not quite work like avcodec_decode_audio4/avcodec_decode_video2.
// There is the following difference: if you got a frame, you must call
// it again with pkt=NULL. pkt==NULL is treated differently from pkt.size==0
// (pkt==NULL means get more output, pkt.size==0 is a flush/drain packet)
static int decode(AVCodecContext *avctx, AVFrame *frame, int *got_frame, AVPacket *pkt)
{
    int ret;
    
    *got_frame = 0;
    
    if (pkt) {
        ret = avcodec_send_packet(avctx, pkt);
        // In particular, we don't expect AVERROR(EAGAIN), because we read all
        // decoded frames with avcodec_receive_frame() until done.
        if (ret < 0 && ret != AVERROR_EOF)
            return ret;
    }
    
    ret = avcodec_receive_frame(avctx, frame);
    if (ret < 0 && ret != AVERROR(EAGAIN))
        return ret;
    if (ret >= 0)
        *got_frame = 1;
    
    return 0;
}

static void * ffmpeg_audio_decode_thread(void * thread_data)
{
    HANDLE_YY_FFREADER reader = (HANDLE_YY_FFREADER)thread_data;
    
    int ret = 0;
    int extra = 0;
    
    AVFrame *frame = NULL;
    int got_frame = 0;
    
    if( reader == NULL ) {
        YY_ERROR("ffmpeg_decode_thread param null");
        return NULL;
    }
    
#if defined(TARGET_OS_ANDROID)
    JNI_AttachThread();
#endif
    
    while(reader->quit_flag == 0) {
        
        if( reader->weak_audio_frame_queue != NULL && reader->weak_audio_frame_queue->count > 10 ) {
            YY_milliseconds_sleep(20);
            continue;
        }
        
        AVPacket * incoming_pkt = YY_dataqueue_get(&(reader->audio_packet_decode_queue), &extra);
        
        if( incoming_pkt == NULL && extra == -9999 ) {
            break;
        }
        
        if( incoming_pkt == NULL ) {
            YY_dataqueue_wait(&(reader->audio_packet_decode_queue), 1000);
            continue;
        }
        
        // check if this is a valid pkt
        if( extra != YY_ffreader_get_seekindex(reader) || incoming_pkt->stream_index != reader->audio_stream_idx ) {
               av_packet_free(&incoming_pkt);
               continue;
        }
        
        // oh, not in player valid range
        int pkt_timestamp = av_q2d(reader->audio_stream->time_base) * incoming_pkt->pts * 1000;

        if( (reader->valid_start_milli >= 0 && pkt_timestamp < reader->valid_start_milli) ||
           (reader->valid_end_milli >= 0 && pkt_timestamp > reader->valid_end_milli + 100) )
        {
            av_packet_free(&incoming_pkt);
            continue;
        }
        
        do {
            
            frame = av_frame_alloc();
            
            ret = decode(reader->audio_dec_ctx, frame, &got_frame, incoming_pkt);
            if (ret < 0) {
                YY_ERROR("Error decoding audio frame (%s)\n", av_err2str(ret));
                reader->audio_decode_error |= (0x01 << 4);
                av_frame_free(&frame);
                frame = NULL;
            }
            
            // send to outside
            if (got_frame > 0 && frame != NULL && reader->weak_audio_frame_queue != NULL ) {
                // change pts
                frame->pts = av_q2d(reader->audio_stream->time_base) * frame->pts * 1000;
                YY_dataqueue_put(reader->weak_audio_frame_queue, frame, extra);
//                YY_DEBUG("#### YY_dataqueue_put audio:%d\n", reader->weak_audio_frame_queue->count);
            }
            else if( frame != NULL ) {
                av_frame_free(&frame);
                frame = NULL;
            }
            
            // free packet
            if( incoming_pkt != NULL ) {
                av_packet_free(&incoming_pkt);
                incoming_pkt = NULL;
            }
            
            // try to get more frame
            
        } while(got_frame);
        
    }
    
    // we die
    reader->audio_decode_thread_running_flag = D_YY_COM_FLAG_OFF;
    
#if defined(TARGET_OS_ANDROID)
    JNI_detachThread();
#endif
    
    return NULL;
}

static void * ffmpeg_video_decode_thread(void * thread_data)
{
    HANDLE_YY_FFREADER reader = (HANDLE_YY_FFREADER)thread_data;
    
    int ret = 0;
    int last_seek_index = 0;
    int extra = 0;
    int got_key = 0;
    
    AVFrame *frame = NULL;
    int got_frame = 0;
    
    if( reader == NULL ) {
        YY_ERROR("ffmpeg_decode_thread param null");
        return NULL;
    }
    
#if defined(TARGET_OS_ANDROID)
    JNI_AttachThread();
#endif
    
    while(reader->quit_flag == 0) {
        
        // check if output queue is full
        if( reader->weak_video_frame_queue != NULL && reader->weak_video_frame_queue->count > 3 && YY_ffreader_inseeking(reader) == 0 ) {
            YY_milliseconds_sleep(20);
            continue;
        }
        AVPacket * incoming_pkt = YY_dataqueue_get(&(reader->video_packet_decode_queue), &extra);
        
        if( incoming_pkt == NULL && extra == -9999 ) {
            break;
        }
        
        if( incoming_pkt == NULL ) {
            YY_dataqueue_wait(&(reader->video_packet_decode_queue), 1000);
            continue;
        }
        
        // check if in seeking mode
        int seek_disp = 0;
        if( YY_ffreader_inseeking(reader) > 0 ) {
            int64_t pkt_timestamp = av_q2d(reader->video_stream->time_base) * incoming_pkt->pts * 1000;
            int diff = (int)(pkt_timestamp - reader->last_order_seek_ts);
//            YY_DEBUG("#### YY_ffreader_inseeking diff:%d, %lld\n", diff, pkt_timestamp);
            if( diff > -1000 && diff < 1000 ) {
                seek_disp = 1;
            }
        }
        
        // check if this is a valid pkt
        if( (seek_disp == 0 && extra != YY_ffreader_get_seekindex(reader)) || incoming_pkt->stream_index != reader->video_stream_idx ) {
            av_packet_free(&incoming_pkt);
            continue;
        }
        
        // check if we got a new stream, if so flush dec
        if( last_seek_index != extra ) {
            avcodec_flush_buffers(reader->video_dec_ctx);
            last_seek_index = extra;
            // when seek happen decode from key-frame
            got_key = 0;
        }
        
        // oh, not in player valid range
        int pkt_timestamp = av_q2d(reader->video_stream->time_base) * incoming_pkt->pts * 1000;
        // in demux mode , decode all packet
        if( reader->demux_mode_flag == 0 )
        {
            if( (reader->valid_start_milli >= 0 && pkt_timestamp < reader->valid_start_milli) ||
               (reader->valid_end_milli >= 0 && pkt_timestamp > reader->valid_end_milli) )
            {
                av_packet_free(&incoming_pkt);
                continue;
            }
        }
        
        // check if we got a key_frame
        if( got_key == 0 ) {
            if( (incoming_pkt->flags & AV_PKT_FLAG_KEY) > 0 ) {
                got_key = 1;
            }
            // this is not a key frame drop it
            else {
                av_packet_free(&incoming_pkt);
                continue;
            }
        }
        
        // now we decode
        do {
            frame = av_frame_alloc();
            
            ret = decode(reader->video_dec_ctx, frame, &got_frame, incoming_pkt);
            if (ret < 0) {
                YY_ERROR("Error decoding video frame (%s)\n", av_err2str(ret));
                reader->video_decode_error |= (0x01 << 0);
                av_frame_free(&frame);
                frame = NULL;
            }
            
            // send to outside
            if (got_frame > 0 && frame != NULL && reader->weak_video_frame_queue != NULL ) {
                // change pts
                frame->pts = av_q2d(reader->video_stream->time_base) * frame->pts * 1000;
                
                // check if need rotate
                int rotate_degree = (int)YY_ffreader_video_rotate_degree(reader);
                if( rotate_degree == 90 ) {
                    AVFrame * rotate_frame = av_frame_alloc();
                    av_frame_copy_props(rotate_frame, frame);
                    rotate_frame->width = frame->height;
                    rotate_frame->height = frame->width;
                    rotate_frame->format = frame->format;
                    av_frame_get_buffer(rotate_frame, 16);
                    frame_rotate_90(frame, rotate_frame);
                    AVFrame * del_frame = frame;
                    frame = rotate_frame;
                    av_frame_free(&del_frame);
                }
                else if( rotate_degree == 270 || rotate_degree == -90 ) {
                    AVFrame * rotate_frame = av_frame_alloc();
                    av_frame_copy_props(rotate_frame, frame);
                    rotate_frame->width = frame->height;
                    rotate_frame->height = frame->width;
                    rotate_frame->format = frame->format;
                    av_frame_get_buffer(rotate_frame, 16);
                    frame_rotate_270(frame, rotate_frame);
                    AVFrame * del_frame = frame;
                    frame = rotate_frame;
                    av_frame_free(&del_frame);
                }
                else if( rotate_degree == 180 ) {
                    AVFrame * rotate_frame = av_frame_alloc();
                    av_frame_copy_props(rotate_frame, frame);
                    rotate_frame->width = frame->width;
                    rotate_frame->height = frame->height;
                    rotate_frame->format = frame->format;
                    av_frame_get_buffer(rotate_frame, 16);
                    frame_rotate_180(frame, rotate_frame);
                    AVFrame * del_frame = frame;
                    frame = rotate_frame;
                    av_frame_free(&del_frame);
                }
                
                YY_dataqueue_put(reader->weak_video_frame_queue, frame, extra);
//                YY_DEBUG("#### YY_dataqueue_put video:%d\n", reader->weak_video_frame_queue->count);
            }
            else if( frame != NULL ) {
                av_frame_free(&frame);
                frame = NULL;
            }
            
            // free packet
            if( incoming_pkt != NULL ) {
                av_packet_free(&incoming_pkt);
                incoming_pkt = NULL;
            }
            
            // try to get more frame
            
        } while(got_frame);
    }
    
    // we die
    reader->video_decode_thread_running_flag = D_YY_COM_FLAG_OFF;
    
#if defined(TARGET_OS_ANDROID)
    JNI_detachThread();
#endif
    
    return NULL;
}

static void * ffmpeg_read_thread(void * thread_data)
{
    HANDLE_YY_FFREADER reader = (HANDLE_YY_FFREADER)thread_data;
    
    if( reader == NULL ) {
        YY_ERROR("ffmpeg_read_thread param null");
        return NULL;
    }
    
#if defined(TARGET_OS_ANDROID)
    JNI_AttachThread();
#endif
    
    // init decode queue
    YY_dataqueue_init(&(reader->video_packet_decode_queue));
    YY_dataqueue_init(&(reader->audio_packet_decode_queue));
    
    // register all formats and codecs
    avformat_network_init();
    av_register_all();
    
    if (avformat_open_input(&(reader->fmt_ctx), reader->server_url, NULL, NULL) < 0) {
        YY_ERROR("Could not open source file %s\n", reader->server_url);
        reader->read_error = -1;
        goto read_end;
    }
    
    if (avformat_find_stream_info((reader->fmt_ctx), NULL) < 0) {
        YY_ERROR("Could not find stream information\n");
        reader->read_error = -2;
        goto read_end;
    }
    
    // try to get video stream
    if (open_codec_context(reader, &(reader->video_stream_idx), &(reader->video_dec_ctx), reader->fmt_ctx, AVMEDIA_TYPE_VIDEO) >= 0) {
        
        reader->video_stream = reader->fmt_ctx->streams[reader->video_stream_idx];
        
        reader->width = reader->video_dec_ctx->width;
        reader->height = reader->video_dec_ctx->height;
        reader->pix_fmt = reader->video_dec_ctx->pix_fmt;
        
        reader->video_rotate_degree = YY_ffreader_get_rotation(reader->video_stream);
        
        if( (int)reader->video_rotate_degree == 90 || (int)reader->video_rotate_degree == -90 || (int)reader->video_rotate_degree == 270 ) {
            reader->width = reader->video_dec_ctx->height;
            reader->height = reader->video_dec_ctx->width;
        }
    }
    else {
        reader->video_stream = NULL;
        
        reader->width = 0;
        reader->height = 0;
        reader->pix_fmt = 0;
        
        reader->video_stream_idx = -1;
        reader->video_dec_ctx = NULL;
        
        reader->video_rotate_degree = 0.0f;
    }
    
    if (open_codec_context(reader, &(reader->audio_stream_idx), &(reader->audio_dec_ctx), reader->fmt_ctx, AVMEDIA_TYPE_AUDIO) >= 0) {
        
        reader->audio_stream = reader->fmt_ctx->streams[reader->audio_stream_idx];
    }
    else {
        
        reader->audio_stream = NULL;
        
        reader->audio_dec_ctx = NULL;
        reader->audio_stream_idx = -1;
    }
    
    // dump input information to stderr
    av_dump_format(reader->fmt_ctx, 0, reader->server_url, 0);
    
    if (!reader->audio_stream && !reader->video_stream) {
        YY_ERROR("Could not find audio or video stream in the input, aborting\n");
        reader->read_error = -3;
        goto read_end;
    }
    
    YY_INFO("YY-ffreader open succeed (%s) %d %d %f \n", reader->server_url, reader->width, reader->height, reader->video_rotate_degree);
    
    // launch decode thread
    if( reader->audio_stream_idx >= 0 ) {
        
        reader->audio_decode_thread_running_flag = D_YY_COM_FLAG_ON;
        
        // start thread
        if( pthread_create(&(reader->audio_decode_thread_id), NULL, ffmpeg_audio_decode_thread, reader) < 0 )
        {
            YY_ERROR("launch ffmpeg_audio_decode_thread failed");
            reader->audio_decode_thread_running_flag = D_YY_COM_FLAG_OFF;
            reader->read_error = -4;
            goto read_end;
        }
    }
    else {
        
        reader->audio_decode_thread_running_flag = D_YY_COM_FLAG_OFF;
    }
    
    if( reader->video_stream_idx >= 0 ) {
        
        reader->video_decode_thread_running_flag = D_YY_COM_FLAG_ON;
        
        // start thread
        if( pthread_create(&(reader->video_decode_thread_id), NULL, ffmpeg_video_decode_thread, reader) < 0 )
        {
            YY_ERROR("launch ffmpeg_video_decode_thread failed");
            reader->video_decode_thread_running_flag = D_YY_COM_FLAG_OFF;
            reader->read_error = -5;
            goto read_end;
        }
    }
    else {
        
        reader->video_decode_thread_running_flag = D_YY_COM_FLAG_OFF;
    }
    
    reader->file_open_flag = 1;
    
    while(reader->quit_flag == 0) {
        
        // tell me to seek
        if( reader->order_seek_flag > 0 && reader->order_seek_timestamp >= 0 ) {
            
            // seek only to video stream ??
            if( reader->video_stream != NULL ) {
                
                int64_t k = 0;
                
                k = (int64_t) (reader->order_seek_timestamp / av_q2d(reader->video_stream->time_base));
                k = k / 1000;
                YY_DEBUG("seeking to %lld (%d) %f %d %d ", k, reader->order_seek_timestamp, av_q2d(reader->video_stream->time_base), reader->video_stream->time_base.den, reader->video_stream->time_base.num);
                
                av_seek_frame(reader->fmt_ctx, reader->video_stream_idx, k, AVSEEK_FLAG_ANY);
            }
            else if( reader->audio_stream != NULL ) {
                
                int64_t k = 0;
                
                k = (int64_t) (reader->order_seek_timestamp / av_q2d(reader->audio_stream->time_base));
                k = k / 1000;
                YY_DEBUG("seeking to %lld (%d) %f %d %d ", k, reader->order_seek_timestamp, av_q2d(reader->audio_stream->time_base), reader->audio_stream->time_base.den, reader->audio_stream->time_base.num);
                
                av_seek_frame(reader->fmt_ctx, reader->audio_stream_idx, k, AVSEEK_FLAG_ANY);
            }
            
            reader->order_seek_flag = 0;
            reader->order_seek_timestamp = -1;
            
            reader->seek_index ++;
            
            // seek 后强制读取一帧视频
            if(reader->video_stream){
                AVPacket * pkt = av_packet_alloc();
                pkt->data = NULL;
                pkt->size = 0;
                
                while( av_read_frame(reader->fmt_ctx, pkt) >= 0 ) {
                    
                    if( pkt->stream_index == reader->video_stream_idx ) {
                        if( (pkt->flags & AV_PKT_FLAG_KEY) > 0 ) {
//                            YY_DEBUG("video pkt %lld --> %f \n", pkt->pts, pkt->pts * av_q2d(reader->video_stream->time_base));
                            YY_dataqueue_put(&(reader->video_packet_decode_queue), pkt, reader->seek_index);
                            break;
                        }
                        else {
                            continue;
                        }
                    }
                    else {
                        YY_ERROR("drop audio packet %d ", pkt->stream_index);
                        av_packet_free(&pkt);
                        
                        pkt = av_packet_alloc();
                        pkt->data = NULL;
                        pkt->size = 0;
                    }
                }
            }
        }
        
        if( YY_ffreader_inseeking(reader) > 0 ) {
            YY_milliseconds_sleep(10);
            continue;
        }
        
        // check if output queue is full
        if( reader->video_packet_decode_queue.count > 80 || reader->audio_packet_decode_queue.count > 100 ) {
            YY_milliseconds_sleep(20);
            continue;
        }
        
        // now start to read packet
        AVPacket * pkt = av_packet_alloc();
        pkt->data = NULL;
        pkt->size = 0;
        
        if( av_read_frame(reader->fmt_ctx, pkt) < 0 ) {
            
            // no data or some error occurs
            reader->read_eof = 1;
            av_packet_free(&pkt);
            
            YY_milliseconds_sleep(30);
            
            continue;
        }
        
        // is not end, put to decode queue
        reader->read_eof = 0;
        
        // oh, in seeking
        if( reader->order_seek_flag > 0 ) {
            av_packet_free(&pkt);
            continue;
        }
        
        // put packet into decode queue
        if( pkt->stream_index == reader->audio_stream_idx ) {
            YY_dataqueue_put(&(reader->audio_packet_decode_queue), pkt, reader->seek_index);
        }
        else if( pkt->stream_index == reader->video_stream_idx ) {
            YY_dataqueue_put(&(reader->video_packet_decode_queue), pkt, reader->seek_index);
//            YY_DEBUG("video pkt %lld --> %f \n", pkt->pts, pkt->pts * av_q2d(reader->video_stream->time_base));
        }
        else {
            YY_ERROR("unknow packet %d ", pkt->stream_index);
            av_packet_free(&pkt);
        }
    }
    
read_end:
    
    // wait for decode thread
    reader->seek_index = -1;
    YY_dataqueue_put(&(reader->audio_packet_decode_queue), NULL, -9999);
    YY_dataqueue_put(&(reader->video_packet_decode_queue), NULL, -9999);
    
    while (reader->audio_decode_thread_running_flag != D_YY_COM_FLAG_OFF) {
        usleep(30*1000);
    }
    YY_dataqueue_destroy(&(reader->audio_packet_decode_queue), 0);
    
    while (reader->video_decode_thread_running_flag != D_YY_COM_FLAG_OFF) {
        usleep(30*1000);
    }
    YY_dataqueue_destroy(&(reader->video_packet_decode_queue), 0);
    
    // must free aft decode down
    avcodec_free_context(&reader->video_dec_ctx);
    avcodec_free_context(&reader->audio_dec_ctx);
    avformat_close_input(&reader->fmt_ctx);
    
    // we die
    reader->read_thread_running_flag = D_YY_COM_FLAG_OFF;
    
#if defined(TARGET_OS_ANDROID)
    JNI_detachThread();
#endif
    
    return NULL;
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// public function
////////////////////////////////////////////////////////////////////////////////////////////////////////////

HANDLE_YY_FFREADER YY_ffreader_open(char * url, ST_YY_DATA_QUEUE* audio_queue, ST_YY_DATA_QUEUE* video_queue)
{
    HANDLE_YY_FFREADER r;
    
    if( url == NULL || strlen(url) >= YY_COM_URL_LIMIT_LEN-1 ) {
        return NULL;
    }
    
    r = (HANDLE_YY_FFREADER)malloc(sizeof(ST_YY_FFMPEG_READER));
    
    memset(r, 0x00, sizeof(ST_YY_FFMPEG_READER));
    
    strncpy(r->server_url, url, YY_COM_URL_LIMIT_LEN-1);
    
    r->weak_audio_frame_queue = audio_queue;
    r->weak_video_frame_queue = video_queue;
    
    r->quit_flag = D_YY_COM_FLAG_OFF;
    r->read_thread_running_flag = D_YY_COM_FLAG_ON;// 默认修改成ON，防止open完立刻调用close
    r->arrived_last_packet_milli = 0;
    r->last_seek_milli = r->last_order_seek_ts = 0;
    
    r->valid_start_milli = r->valid_end_milli = -1;
    r->file_open_flag = 0;
    r->demux_mode_flag = 0;
    
    // start thread
    if( pthread_create(&(r->read_thread_id), NULL, ffmpeg_read_thread, r) < 0 )
    {
        YY_ERROR("launch ffmpeg_read_thread failed");
        free(r);
        return NULL;
    }
    
    return (HANDLE_YY_FFREADER)r;
}

int YY_ffreader_check_opened(HANDLE_YY_FFREADER handle)
{
    if( handle == NULL ) {
        return -1;
    }
    
    return handle->file_open_flag;
}

void YY_ffreader_close(HANDLE_YY_FFREADER handle)
{
    if( handle == NULL ) {
        return;
    }
    
    handle->quit_flag = D_YY_COM_FLAG_ON;
    
    while (handle->read_thread_running_flag != D_YY_COM_FLAG_OFF) {
        usleep(30*1000);
    }
    
    free(handle);
    
    return;
}

// get duration
int YY_ffreader_get_duration(HANDLE_YY_FFREADER handle)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_get_duration null");
        return -1;
    }
    
    if( handle->fmt_ctx == NULL ) {
        return -1;
    }
    
    int ret = (int)av_rescale(handle->fmt_ctx->duration, 1000, AV_TIME_BASE);
    
    return ret;
}

// use this func for clip play
void YY_ffreader_valid_range(HANDLE_YY_FFREADER handle, int64_t start_milli, int64_t end_milli)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_valid_range null");
        return;
    }
    
    handle->valid_start_milli = start_milli;
    handle->valid_end_milli = end_milli;
    
    return;
}

// use this func for seek
void YY_ffreader_seekto(HANDLE_YY_FFREADER handle, int milli)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_seekto null");
        return;
    }
    
    int64_t now = YY_system_current_milli();
    if( now - handle->last_seek_milli < 50 )
        return;
    
    handle->read_eof = 0; // once to reading status
    handle->order_seek_flag = 1;
    handle->order_seek_timestamp = milli;
    
    handle->last_seek_milli = YY_system_current_milli();
    handle->last_order_seek_ts = milli;
    
    return;
}

int YY_ffreader_inseeking(HANDLE_YY_FFREADER handle)
{
    if( YY_system_current_milli() - handle->last_seek_milli < 300 ) {
        return 1;
    }
    
    return 0;
}

// get current seek index
int YY_ffreader_get_seekindex(HANDLE_YY_FFREADER handle)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_get_seekindex null");
        return -1;
    }
    
    if( handle->order_seek_flag > 0 ) {
        return -1;
    }
    
    return handle->seek_index;
}

int YY_ffreader_video_size(HANDLE_YY_FFREADER handle, int * w, int * h)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_video_size null");
        return -1;
    }
    
    *w = handle->width;
    *h = handle->height;
    
    return 0;
}

int YY_ffreader_audio_existed(HANDLE_YY_FFREADER handle)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_audio_existed null");
        return -1;
    }
    
    if( handle->audio_stream != NULL ) {
        return 1;
    }
    
    return 0;
}
int YY_ffreader_video_existed(HANDLE_YY_FFREADER handle)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_video_existed null");
        return -1;
    }
    
    if( handle->video_stream != NULL ) {
        return 1;
    }
    
    return 0;
}

int YY_ffreader_check_eof(HANDLE_YY_FFREADER handle)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_check_eof null");
        return -1;
    }
    
    if( handle->read_eof == 1 && handle->audio_packet_decode_queue.count == 0 && handle->video_packet_decode_queue.count == 0 ) {
        return 1;
    }
    
    return 0;
}

float YY_ffreader_video_rotate_degree(HANDLE_YY_FFREADER handle)
{
    if( handle == NULL ) {
        YY_ERROR("YY_ffreader_video_rotate_degree null");
        return 0.0f;
    }
    
    return handle->video_rotate_degree;
}
