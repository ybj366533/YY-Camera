//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#include "stdio.h"
#include "stdlib.h"
#include "string.h"
#include "pthread.h"
#include "unistd.h"

#include "libavutil/samplefmt.h"
#include "libavutil/avassert.h"
#include "libavutil/time.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavutil/opt.h"
#include "libavcodec/avfft.h"
#include "libswresample/swresample.h"

#include "yy_demuxer.h"
#include "yy_logger.h"
#include "yy_com_utils.h"

#include "yy_ffmpeg_reader.h"

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// private function
////////////////////////////////////////////////////////////////////////////////////////////////////////////

static int muxer_convert_and_put_audio(HANDLE_YY_DEMUXER handle, AVFrame * f)
{
    // AVSampleFormat sf;
    // AV_CH_LAYOUT_5POINT1_BACK AV_CH_LAYOUT_STEREO
    if (f->format != AV_SAMPLE_FMT_S16 ||
        f->channel_layout != AV_CH_LAYOUT_STEREO ||
        f->sample_rate != 44100 ) {
        
        // at the very first, check if any old swr cache exist
        if( handle->audio_swr_len > 0 ) {
            
            int empty = YY_databuffer_space(&(handle->audio_buffer));
            
            if( empty > handle->audio_swr_len ) {
                YY_databuffer_put(&(handle->audio_buffer), handle->audio_swr_cache, handle->audio_swr_len, 1);
                handle->audio_swr_len = 0;
            }
            else {
                // old data can not be handled
                return -99;
            }
        }
        
        // convert audio
        if( handle->swr_ctx_addr == NULL ) {
            
            handle->swr_ctx_addr = swr_alloc_set_opts(NULL,
                                                      AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, 44100,
                                                      f->channel_layout, f->format, f->sample_rate,
                                                      0, NULL);
            if (!handle->swr_ctx_addr) {
                
                YY_ERROR("Cannot create sample rate converter for conversion of %d Hz %s %d channels to %d Hz %s %d channels!\n",
                          f->sample_rate, av_get_sample_fmt_name(f->format), av_frame_get_channels(f),
                          44100, av_get_sample_fmt_name(AV_SAMPLE_FMT_S16), 2);
                return -1;
            }
            
            // TODO:not support AV_CH_LAYOUT_5POINT1_BACK
            struct SwrContext * swr_ctx = (struct SwrContext*)handle->swr_ctx_addr;
            if (swr_init(swr_ctx) < 0) {
                
                YY_ERROR("Cannot create sample rate converter for conversion of %d Hz %s %d channels to %d Hz %s %d channels!\n",
                          f->sample_rate, av_get_sample_fmt_name(f->format), av_frame_get_channels(f),
                          44100, av_get_sample_fmt_name(AV_SAMPLE_FMT_S16), 2);
                swr_free(&swr_ctx);
                handle->swr_ctx_addr = NULL;
                return -2;
            }
        }
        
        // do convert
        if( handle->swr_ctx_addr != NULL ) {
            
            struct SwrContext * swr_ctx = (struct SwrContext*)handle->swr_ctx_addr;
            
            const uint8_t **in = (const uint8_t **)f->extended_data;
            uint8_t * plane[1];
            plane[0] = &(handle->audio_swr_cache[0]);
            uint8_t **out = plane;
            int out_count = (int)((int64_t)f->nb_samples * 44100 / f->sample_rate + 256);
            int out_size  = av_samples_get_buffer_size(NULL, 2, out_count, AV_SAMPLE_FMT_S16, 0);
            int len2;
            if (out_size < 0) {
                YY_ERROR("av_samples_get_buffer_size() failed %d %d \n", out_size, out_count);
                return -3;
            }
            
            len2 = swr_convert(swr_ctx, out, out_count, in, f->nb_samples);
            if (len2 < 0) {
                YY_ERROR("swr_convert() failed\n");
                return -4;
            }
            if (len2 == out_count) {
                YY_ERROR("audio buffer is probably too small\n");
            }
            handle->audio_swr_len = len2 * 2 * av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
            
            // put it to audio_buffer if possibleint empty = YY_databuffer_space(&(handle->audio_buffer));
            int empty = YY_databuffer_space(&(handle->audio_buffer));
            
            if( empty > handle->audio_swr_len ) {
                YY_databuffer_put(&(handle->audio_buffer), handle->audio_swr_cache, handle->audio_swr_len, 1);
                handle->audio_swr_len = 0;
            }
            
            return len2;
        }
        
        return -88;
    }
    // no need convert
    else {
        
        int empty = YY_databuffer_space(&(handle->audio_buffer));
        if( empty > f->linesize[0] ) {
            YY_databuffer_put(&(handle->audio_buffer), f->data[0], f->linesize[0], 1);
            return f->linesize[0];
        }
        else {
            return -77;
        }
    }
}

static HANDLE_YY_FFREADER open_file(char * url, ST_YY_DATA_QUEUE* audio_queue, ST_YY_DATA_QUEUE* video_queue)
{
    int limit = 100;
    HANDLE_YY_FFREADER reader = YY_ffreader_open(url, audio_queue, video_queue);
    if( reader == NULL ) {
        return NULL;
    }
    reader->demux_mode_flag = 1;
    // 先开始缓冲，缓冲到一定程度在开始，如果在1秒内还没能完成缓冲，结束，抛错
    while(limit >= 0) {
        
        YY_milliseconds_sleep(30);
        
        if( audio_queue->count >= 3 || video_queue->count >= 3 ) {
            break;
        }
        
        if( YY_ffreader_check_opened(reader) > 0 ) {
            
            // 缓冲一定程度再开始 TODO:
            int is_video_ready = 1;
            int is_audio_ready = 1;
            if( YY_ffreader_audio_existed(reader) && audio_queue->count < 3 ) {
                is_audio_ready = 0;
            }
            if( YY_ffreader_video_existed(reader) && video_queue->count < 3 ) {
                is_video_ready = 0;
            }
            if( is_audio_ready > 0 && is_video_ready > 0 ) {
                break;
            }
        }
        
        limit --;
    }
    
    if( limit < 0 && reader != NULL ) {
        YY_ERROR("YY_ffreader_open %s failed .", url);
        YY_ffreader_close(reader);
        return NULL;
    }
    
    return reader;
}

static void close_file(HANDLE_YY_FFREADER reader)
{
    if( reader != NULL ) {
        YY_ffreader_close(reader);
    }
    
    return;
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// public function
////////////////////////////////////////////////////////////////////////////////////////////////////////////

HANDLE_YY_DEMUXER YY_demuxer_open(char * url)
{
    HANDLE_YY_DEMUXER r;
    
    if( url == NULL ) {
        YY_ERROR("YY_demuxer_open ng url \n");
        return NULL;
    }
    
    r = (HANDLE_YY_DEMUXER)malloc(sizeof(ST_YY_LIVE_DEMUXER));
    
    memset(r, 0x00, sizeof(ST_YY_LIVE_DEMUXER));
    
    r->valid_start_milli = r->valid_end_milli = -1;
    
    YY_dataqueue_init(&(r->video_pkt_queue));
    YY_dataqueue_init(&(r->audio_pkt_queue));
    
    HANDLE_YY_FFREADER reader = open_file(url, &(r->audio_pkt_queue), &(r->video_pkt_queue));
    if( reader == NULL ) {
        free(r);
        return NULL;
    }
    
    YY_databuffer_init(&(r->audio_buffer), 4096*3);
    
    // open reader
    r->reader_addr = (void*)reader;
    
    return (HANDLE_YY_DEMUXER)r;
}

void YY_demuxer_close(HANDLE_YY_DEMUXER handle)
{
    if( handle == NULL ) {
        return;
    }
    
    // 销毁每一个packet和frame，不能依靠dataqueue自带的方法
    AVFrame * frame = NULL;
    int extra = 0;
    while( (frame = (AVFrame*)YY_dataqueue_get(&(handle->video_pkt_queue), &extra)) != NULL ) {
        av_frame_free(&frame);
    }
    YY_dataqueue_destroy(&(handle->video_pkt_queue), 0);
    
    while( (frame = (AVFrame*)YY_dataqueue_get(&(handle->audio_pkt_queue), &extra)) != NULL ) {
        av_frame_free(&frame);
    }
    YY_dataqueue_destroy(&(handle->audio_pkt_queue), 0);
    
    // destroy buffer
    YY_databuffer_destroy(&(handle->audio_buffer));
    
    // destroy reader
    close_file((HANDLE_YY_FFREADER)handle->reader_addr);
    
    // real free
    free(handle);
}

int YY_demuxer_set_range(HANDLE_YY_DEMUXER handle, int64_t start_milli, int64_t end_milli)
{
    HANDLE_YY_FFREADER reader = NULL;
    
    if( handle == NULL ) {
        return -1;
    }
    
    handle->valid_start_milli = start_milli;
    handle->valid_end_milli = end_milli;
    
    reader = (handle->reader_addr);
    
    if( reader != NULL )
        YY_ffreader_valid_range(reader, handle->valid_start_milli, handle->valid_end_milli);
    
    return 0;
}

int YY_demuxer_peek_next_video(HANDLE_YY_DEMUXER handle, ST_YY_RAW_FRAME_REF ref)
{
    int extra = 0;
    
    if( handle == NULL ) {
        return 0;
    }
    
    AVFrame * vframe = NULL;
    
    while( handle->video_pkt_queue.count > 0 )
    {
        vframe = (AVFrame *)YY_dataqueue_peek_first(&(handle->video_pkt_queue), &extra);
        if( vframe == NULL )
            break;
        
        // check if data if out of range
        if( (handle->valid_start_milli >= 0 && vframe->pts < handle->valid_start_milli) ||
           (handle->valid_end_milli >= 0 && vframe->pts > handle->valid_end_milli) )
        {
            vframe = YY_dataqueue_get(&(handle->video_pkt_queue), &extra);
            av_frame_free(&vframe);
            vframe = NULL;
            continue;
        }
        else {
            break;
        }
    }
    
    if( vframe == NULL )
        return 0;
    
    ref->type = YY_VIDEO_FRAME;
    ref->pixel_width = vframe->width;
    ref->pixel_height = vframe->height;
    ref->plane_data[0] = vframe->data[0];
    ref->plane_data[1] = vframe->data[1];
    ref->plane_data[2] = vframe->data[2];
    ref->plane_size[0] = vframe->linesize[0]*vframe->height;
    ref->plane_size[1] = vframe->linesize[1]*(vframe->height/2);
    ref->plane_size[2] = vframe->linesize[2]*(vframe->height/2);
    ref->stride_size[0] = vframe->linesize[0];
    ref->stride_size[1] = vframe->linesize[1];
    ref->stride_size[2] = vframe->linesize[2];
    ref->plane_count = 3;
    ref->timestamp = vframe->pts;
    ref->retain_count = 1;
    
    return 1;
}

int YY_demuxer_remove_next_video(HANDLE_YY_DEMUXER handle)
{
    int extra = 0;
    
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->video_pkt_queue.count <= 0 )
        return 0;
    
    AVFrame * vframe = (AVFrame *)YY_dataqueue_get(&(handle->video_pkt_queue), &extra);
    if( vframe == NULL )
        return 0;
    
    av_frame_free(&vframe);
    
    return 1;
}

int YY_demuxer_pull_audio(HANDLE_YY_DEMUXER handle, uint8_t*buf, int len)
{
    if( handle == NULL ) {
        return 0;
    }
    
    int extra;
    HANDLE_YY_FFREADER reader = (handle->reader_addr);
    
    YY_ffreader_valid_range(reader, handle->valid_start_milli, handle->valid_end_milli);
    
    AVFrame * aframe = NULL;
    do {
        
        aframe = YY_dataqueue_peek_first(&(handle->audio_pkt_queue), &extra);
        
        if( aframe != NULL ) {
            // invalid frame data, drop it
            if( extra != YY_ffreader_get_seekindex(reader) ) {
                // remove frame from queue
                aframe = YY_dataqueue_get(&(handle->audio_pkt_queue), &extra);
                av_frame_free(&aframe);
            }
            // valid frame data
            else {
                
                // check if data if out of range
                if( (reader->valid_start_milli >= 0 && aframe->pts < reader->valid_start_milli) ||
                   (reader->valid_end_milli >= 0 && aframe->pts > reader->valid_end_milli) )
                {
                    aframe = YY_dataqueue_get(&(handle->audio_pkt_queue), &extra);
                    av_frame_free(&aframe);
                }
                // check if there is space in databuffer
                else if( muxer_convert_and_put_audio(handle, aframe) > 0 ) {
                    // audio data is used
                    aframe = YY_dataqueue_get(&(handle->audio_pkt_queue), &extra);
                    av_frame_free(&aframe);
                }
                else {
                    aframe = NULL;
                }
            }
        }
        
    } while(aframe != NULL);
    
    int ret = YY_databuffer_get(&(handle->audio_buffer), buf, len);
//    if( ret < len )
//    YY_DEBUG("YY_databuffer_get %d < %d --- %02x%02x%02x%02x%02x%02x%02x%02x", ret, len, buf[0], buf[1], buf[2], buf[3], buf[4], buf[5], buf[6], buf[7]);
    
    return ret;
}

int YY_demuxer_check_eof(HANDLE_YY_DEMUXER handle)
{
    if( handle == NULL )
        return -1;
    
    HANDLE_YY_FFREADER reader = (handle->reader_addr);
    
    if( handle->video_pkt_queue.count <= 0 && handle->audio_pkt_queue.count <= 0 && YY_ffreader_check_eof(reader) > 0 ) {
        return 1;
    }
    
    return 0;
}

int YY_demuxer_seekto(HANDLE_YY_DEMUXER handle, int milli)
{
    if( handle == NULL && handle->reader_addr != NULL ) {
        return -1;
    }
    
    YY_ffreader_seekto((HANDLE_YY_FFREADER)handle->reader_addr, milli);
    
    return milli;
}
