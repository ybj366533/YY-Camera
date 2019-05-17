//
//  gtv_live_player.c
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
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

#include "gtv_player.h"
#include "gtv_logger.h"
#include "gtv_com_utils.h"

#include "gtv_ffmpeg_reader.h"

#if defined(TARGET_OS_ANDROID)
#include "gtvideoplayer_jni.h"
#endif


static char * _gtv_ver = "0.0.2";

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// private function
////////////////////////////////////////////////////////////////////////////////////////////////////////////

static void live_player_notify_event(HANDLE_GTV_PLAYER handle, int event, int param1, int param2)
{
    if( handle->target == NULL || handle->event_callback == NULL ) {
        return;
    }
    
    handle->event_callback(handle->target, handle, event, param1, param2);
}

static int convert_and_put_audio(HANDLE_GTV_PLAYER handle, AVFrame * f)
{
    // AVSampleFormat sf;
    // AV_CH_LAYOUT_5POINT1_BACK AV_CH_LAYOUT_STEREO
    if (f->format != AV_SAMPLE_FMT_S16 ||
        f->channel_layout != AV_CH_LAYOUT_STEREO ||
        f->sample_rate != 44100 ) {
        
        // at the very first, check if any old swr cache exist
        if( handle->audio_swr_len > 0 ) {
            
            int empty = gtv_databuffer_space(&(handle->audio_buffer));
            
            if( empty > handle->audio_swr_len ) {
                gtv_databuffer_put(&(handle->audio_buffer), handle->audio_swr_cache, handle->audio_swr_len, 1);
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
                
                GTV_ERROR("Cannot create sample rate converter for conversion of %d Hz %s %d channels to %d Hz %s %d channels!\n",
                          f->sample_rate, av_get_sample_fmt_name(f->format), av_frame_get_channels(f),
                          44100, av_get_sample_fmt_name(AV_SAMPLE_FMT_S16), 2);
                return -1;
            }
            
            // TODO:not support AV_CH_LAYOUT_5POINT1_BACK
            struct SwrContext * swr_ctx = (struct SwrContext*)handle->swr_ctx_addr;
            if (swr_init(swr_ctx) < 0) {
                
                GTV_ERROR("Cannot create sample rate converter for conversion of %d Hz %s %d channels to %d Hz %s %d channels!\n",
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
                GTV_ERROR("av_samples_get_buffer_size() failed %d %d \n", out_size, out_count);
                return -3;
            }
            
            len2 = swr_convert(swr_ctx, out, out_count, in, f->nb_samples);
            if (len2 < 0) {
                GTV_ERROR("swr_convert() failed\n");
                return -4;
            }
            if (len2 == out_count) {
                GTV_ERROR("audio buffer is probably too small\n");
            }
            handle->audio_swr_len = len2 * 2 * av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
            
            // put it to audio_buffer if possibleint empty = gtv_databuffer_space(&(handle->audio_buffer));
            int empty = gtv_databuffer_space(&(handle->audio_buffer));
            
            if( empty > handle->audio_swr_len ) {
                gtv_databuffer_put(&(handle->audio_buffer), handle->audio_swr_cache, handle->audio_swr_len, 1);
                handle->audio_swr_len = 0;
            }
            
            return len2;
        }
        
        return -88;
    }
    // no need convert
    else {
        
        int empty = gtv_databuffer_space(&(handle->audio_buffer));
        if( empty > f->linesize[0] ) {
            gtv_databuffer_put(&(handle->audio_buffer), f->data[0], f->linesize[0], 1);
            return f->linesize[0];
        }
        else {
            return -77;
        }
    }
}

static void * gtv_player_thread(void * thread_data)
{
    HANDLE_GTV_PLAYER handle = (HANDLE_GTV_PLAYER)thread_data;
    int64_t inited_timestamp = 0;
    int player_error_no = GTV_PLAYER_NO_ERROR;
    int video_width,video_height;
//    int first_rend = 0;
    
    HANDLE_GTV_FFREADER reader = NULL;
    
    if( handle == NULL )
        return NULL;
    
    {
        struct sched_param sched;
        int policy;
        pthread_t thread = pthread_self();
        
        if (pthread_getschedparam(thread, &policy, &sched) < 0) {
            GTV_ERROR("pthread_getschedparam() failed");
        }
        else {
            sched.sched_priority = sched_get_priority_max(policy);
            if (pthread_setschedparam(thread, policy, &sched) < 0) {
                GTV_ERROR("pthread_setschedparam() failed");
            }
        }
    }
    
    handle->first_rend = 0;
    handle->thread_running_flag = D_GTV_COM_FLAG_ON;
	
	#if defined(TARGET_OS_ANDROID)
	JNI_AttachThread();
	#endif
	
    // notify inited
    live_player_notify_event(handle, GTV_PLAYER_EVT_INITED, 0, 0);
    GTV_INFO("gtv-player inited (%s) \n", handle->server_url);

    inited_timestamp = gtv_system_current_milli();
    player_error_no = GTV_PLAYER_NO_ERROR;
    
    // open reader
    reader = gtv_ffreader_open(handle->server_url, &(handle->audio_pkt_queue), &(handle->video_pkt_queue));
    handle->reader_addr = (void*)reader;
    handle->start_play_flag = D_GTV_COM_FLAG_OFF;
    
    handle->stream_sts = GTV_PLAYER_STREAM_OPENED;
    
    gtv_ffreader_valid_range(reader, handle->valid_start_milli, handle->valid_end_milli);
    
    // 先开始缓冲，缓冲到一定程度在开始，如果在1秒内还没能完成缓冲，结束，抛错
    while(handle->quit_flag == D_GTV_COM_FLAG_OFF && handle->start_play_flag == D_GTV_COM_FLAG_OFF ) {
        
        gtv_milliseconds_sleep(20);
        
        if( gtv_ffreader_check_opened(reader) > 0 ) {
            
            // 缓冲一定程度再开始 TODO:
            int is_video_ready = 1;
            int is_audio_ready = 1;
            if( gtv_ffreader_audio_existed(reader) && handle->audio_pkt_queue.count < 5 ) {
                is_audio_ready = 0;
            }
            if( gtv_ffreader_video_existed(reader) && handle->video_pkt_queue.count < 1 ) {
                is_video_ready = 0;
            }
            if( is_audio_ready > 0 && is_video_ready > 0 ) {
                handle->start_play_flag = D_GTV_COM_FLAG_ON;
                break;
            }
        }
//        if( handle->video_pkt_queue.count > 5 || handle->audio_pkt_queue.count > 5 ) {
//            handle->start_play_flag = D_GTV_COM_FLAG_ON;
//            break;
//        }
        
        // 缓冲超时
        int64_t curr_milli = gtv_system_current_milli();
        if( curr_milli - inited_timestamp > 6000 ) {
            player_error_no = GTV_PLAYER_TIMEOUT_ERROR;
            break;
        }
    }
    
    // 通知外部数据准备完成
    if( player_error_no == GTV_PLAYER_NO_ERROR ) {
        gtv_ffreader_video_size(reader, &video_width, &video_height);
        GTV_INFO("gtv-player prepared (%d,%d) (%s) %d %d dur:%d \n", video_width, video_height, handle->server_url, handle->video_pkt_queue.count, handle->audio_pkt_queue.count, gtv_ffreader_get_duration(reader));
        live_player_notify_event(handle, GTV_PLAYER_EVT_PREPARED, video_width, video_height);
    }
    else {
        
        GTV_INFO("gtv-player prepare failed (%s) %d %d dur:%d \n", handle->server_url, handle->video_pkt_queue.count, handle->audio_pkt_queue.count, gtv_ffreader_get_duration(reader));
    }
    
    // start play
    handle->current_timestamp = 0;
    handle->stream_sts = GTV_PLAYER_STREAM_STREAMING;
    
    int64_t last_loop_timestamp = gtv_system_current_milli();
    int64_t last_video_timestamp = gtv_system_current_milli();
    int64_t last_videoframe_pts = -1;
    float sleeptime_for_next = 5.0f;
    
    while(handle->quit_flag == D_GTV_COM_FLAG_OFF && player_error_no == GTV_PLAYER_NO_ERROR) {
        
        if( gtv_ffreader_inseeking(handle->reader_addr) == 0 ) {
            
            if( sleeptime_for_next > 0 && sleeptime_for_next < 50 ) {
//                int64_t bef = av_gettime_relative();
                gtv_useconds_sleep((int)(sleeptime_for_next * 1000.0f));
//                gtv_useconds_sleep(100);
//                int64_t aft = av_gettime_relative();
//                if( aft - bef > 3000 ) {
//                    GTV_DEBUG("#### player delay %d %f \n", (int)(aft - bef), sleeptime_for_next);
//                }
            }
//            int32_t diff = (int32_t)(gtv_system_current_milli() - last_loop_timestamp);
//            if( diff < 10 ) {
//                gtv_milliseconds_sleep(15-diff);
//            }
        }
        
        last_loop_timestamp = gtv_system_current_milli();
        sleeptime_for_next = 5.0f;
        
        // 确保即使被暂停也能显示一帧数据(但是设置了range之后，如果没有seek的话，不会显示第一帧)
        if( handle->pause_flag > 0 ) {
            handle->stream_sts = GTV_PLAYER_STREAM_PAUSED;
            if( handle->first_rend != 0 )
                continue;
        }
        
        int extra = 0;
        
        // we update range every time here
        gtv_ffreader_valid_range(reader, handle->valid_start_milli, handle->valid_end_milli);
        
        // read avframe from audio queue
        // 只有pause状态会主动读取数据，目的是seek后丢弃无效数据
        if( handle->pause_flag > 0 )
        {
            AVFrame * aframe = NULL;
            do {
                
                aframe = gtv_dataqueue_peek_first(&(handle->audio_pkt_queue), &extra);
                
                if( aframe != NULL ) {
                    // invalid frame data, drop it
                    if( extra != gtv_ffreader_get_seekindex(reader) && handle->pause_flag > 0 ) {
                        // remove frame from queue
                        aframe = gtv_dataqueue_get(&(handle->audio_pkt_queue), &extra);
                        av_frame_free(&aframe);
                    }
                    // valid frame data
                    else {
                        aframe = NULL;
                    }
                }
                
            } while(aframe != NULL);
        }
        
        // read avframe from video queue
        {
            AVFrame * vframe = NULL;
            do {
                
                vframe = gtv_dataqueue_peek_first(&(handle->video_pkt_queue), &extra);
                
                if( vframe != NULL ) {
                    
                    int seek_force_disp = 0;
                    // if in seeking
                    if( gtv_ffreader_inseeking((handle->reader_addr)) > 0 ) {
                        int diff = (int)(vframe->pts - handle->current_timestamp);
                        if( diff > -2000 && diff < 2000 ) {
                            seek_force_disp = 1;
                        }
                    }
                    
                    // invalid frame data, drop it
                    if( extra != gtv_ffreader_get_seekindex(reader) && seek_force_disp == 0 ) {
                        // remove frame from queue
                        vframe = gtv_dataqueue_get(&(handle->video_pkt_queue), &extra);
                        av_frame_free(&vframe);
                    }
                    // valid frame data
                    else {
                        
                        // check if data if out of range
                        if( (reader->valid_start_milli >= 0 && vframe->pts < reader->valid_start_milli) ||
                           (reader->valid_end_milli >= 0 && vframe->pts > reader->valid_end_milli) )
                        {
                            vframe = gtv_dataqueue_get(&(handle->video_pkt_queue), &extra);
                            av_frame_free(&vframe);
                        }
                        // check if frame need to be displayed
                        else if( handle->current_timestamp >= vframe->pts-150 ||
                                (handle->current_timestamp >= handle->valid_start_milli && handle->first_rend == 0) || seek_force_disp > 0 ) {
                            
                            if( seek_force_disp == 0 ) {
                                if( last_video_timestamp > 0 && last_videoframe_pts > 0 ) {
                                    int64_t pts_diff = (vframe->pts-last_videoframe_pts);
                                    int64_t timepassed = av_gettime_relative()/1000 - last_video_timestamp;
                                    if( handle->current_timestamp - vframe->pts > 350 ) {
                                        pts_diff = pts_diff * 4 / 5;
                                    }
                                    else if( handle->current_timestamp - vframe->pts > 250 ) {
                                        pts_diff = pts_diff * 9 / 10;
                                    }
                                    if( pts_diff > 3 && pts_diff < 100 ) {
                                        if( timepassed < pts_diff ) {
                                            if( pts_diff - timepassed > 20 ) {
                                                sleeptime_for_next = 10.0f;
                                            }
                                            else if( pts_diff - timepassed > 10 ) {
                                                sleeptime_for_next = 1.0f;
                                            }
                                            else {
                                                sleeptime_for_next = 0.1f;
                                            }
                                            //sleeptime_for_next = (int)(pts_diff - timepassed);
                                            vframe = NULL;
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            if( handle->data_callback != NULL ) {
                                
                                ST_GTV_RAW_FRAME raw_frame;
                                
                                raw_frame.type = GTV_VIDEO_FRAME;
                                raw_frame.pixel_width = vframe->width;
                                raw_frame.pixel_height = vframe->height;
                                raw_frame.plane_data[0] = vframe->data[0];
                                raw_frame.plane_data[1] = vframe->data[1];
                                raw_frame.plane_data[2] = vframe->data[2];
                                raw_frame.plane_size[0] = vframe->linesize[0]*vframe->height;
                                raw_frame.plane_size[1] = vframe->linesize[1]*(vframe->height/2);
                                raw_frame.plane_size[2] = vframe->linesize[2]*(vframe->height/2);
                                raw_frame.stride_size[0] = vframe->linesize[0];
                                raw_frame.stride_size[1] = vframe->linesize[1];
                                raw_frame.stride_size[2] = vframe->linesize[2];
                                raw_frame.plane_count = 3;
                                raw_frame.timestamp = vframe->pts;
                                raw_frame.retain_count = 1;
                                
                                if( abs( (int)(vframe->pts-last_videoframe_pts) - (int)(av_gettime_relative()/1000-last_video_timestamp) ) > 3 )
                                GTV_DEBUG("#### player display pts:%lld (%d-%d) diff:%d \n", vframe->pts, (int)(vframe->pts-last_videoframe_pts), (int)(av_gettime_relative()/1000-last_video_timestamp), handle->current_timestamp-vframe->pts);
                                // save last video info
                                last_videoframe_pts = vframe->pts;
                                last_video_timestamp = av_gettime_relative()/1000;
//                                GTV_DEBUG("#### player display pts:%lld\n", vframe->pts);
                                
                                handle->data_callback(handle->target, &raw_frame);
                                
                                if( handle->first_rend == 0 ) {
                                    handle->first_rend = 1;
                                }
                                
                                // remove frame from queue
                                vframe = gtv_dataqueue_get(&(handle->video_pkt_queue), &extra);
                                av_frame_free(&vframe);
                            }
                            else {
                                // redirect it to another queue
                                vframe = gtv_dataqueue_get(&(handle->video_pkt_queue), &extra);
                                gtv_dataqueue_put(&(handle->redirect_pkt_queue), vframe, extra);
                                
                                if( handle->first_rend == 0 ) {
                                    handle->first_rend = 1;
                                }
                            }
                        }
                        else {
                            // not the right time to disp
//                            GTV_DEBUG("curr time %lld but video pts %lld ", handle->current_timestamp, vframe->pts);
                            vframe = NULL;
                        }
                    }
                }
                
            } while(vframe != NULL);
        }
        
        // update reading status
        if( gtv_ffreader_check_eof(reader) > 0 && handle->video_pkt_queue.count == 0 && handle->audio_pkt_queue.count == 0 ) {
            handle->stream_sts = GTV_PLAYER_STREAM_EOF;
        }
        else {
            if( handle->pause_flag > 0 ) {
                handle->stream_sts = GTV_PLAYER_STREAM_PAUSED;
            }
            else {
                handle->stream_sts = GTV_PLAYER_STREAM_STREAMING;
            }
        }
//        GTV_DEBUG("#### video:%d, audio:%d\n", handle->video_pkt_queue.count, handle->audio_pkt_queue.count);
    }
    
    // 通知外部，播放结束(如果需要retry并且外部没有要求停止，不发送通知)
    if( handle->quit_flag == D_GTV_COM_FLAG_ON ) {
        live_player_notify_event(handle, GTV_PLAYER_EVT_FINISHED, player_error_no, 0);
    }
    GTV_INFO("gtv-player finished (%s) dur:(%d) err:(%d) \n", handle->server_url, (int)(gtv_system_current_milli()-inited_timestamp), player_error_no);
    
    // destroy reader
    GTV_INFO("gtv-player gtv_ffreader_close \n");
    if( reader != NULL ) {
        handle->reader_addr = NULL;// TODO: mutex
        gtv_ffreader_close(reader);
        reader = NULL;
    }
    
    GTV_INFO("gtv-player finished completely %d \n", handle->quit_flag);
    
    handle->start_play_flag = D_GTV_COM_FLAG_OFF;
    handle->thread_running_flag = D_GTV_COM_FLAG_OFF;
	
	#if defined(TARGET_OS_ANDROID)
	JNI_detachThread();
	#endif
    
    return NULL;
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// public function
////////////////////////////////////////////////////////////////////////////////////////////////////////////

HANDLE_GTV_PLAYER gtv_player_open(char * url, FUNC_PLAYER_EVENT_NOTIFY evt_func, FUNC_PLAYER_DATA_NOTIFY data_func, void * target)
{
    HANDLE_GTV_PLAYER r;
    
    if( url == NULL || strlen(url) >= GTV_COM_URL_LIMIT_LEN-1 ) {
        GTV_ERROR("gtv_player_open ng url \n");
        return NULL;
    }
    
    r = (HANDLE_GTV_PLAYER)malloc(sizeof(ST_GTV_LIVE_PLAYER));
    
    memset(r, 0x00, sizeof(ST_GTV_LIVE_PLAYER));
    
    strncpy(r->server_url, url, GTV_COM_URL_LIMIT_LEN-1);
    
    r->target = target;
    r->event_callback = evt_func;
    r->data_callback = data_func;
    r->quit_flag = D_GTV_COM_FLAG_OFF;
    r->thread_running_flag = D_GTV_COM_FLAG_ON; // 防止线程还没启动，就被close
    
	r->pause_flag = D_GTV_COM_FLAG_OFF;
	r->start_play_flag = D_GTV_COM_FLAG_OFF;
    r->stream_sts = GTV_PLAYER_STREAM_UNKNOWN;

    r->valid_start_milli = r->valid_end_milli = -1;
    
    gtv_dataqueue_init(&(r->video_pkt_queue));
    gtv_dataqueue_init(&(r->audio_pkt_queue));
    gtv_dataqueue_init(&(r->redirect_pkt_queue));
    
    gtv_databuffer_init(&(r->audio_buffer), 4096*3);
    
    // start thread
    if( pthread_create(&(r->thread_id), NULL, gtv_player_thread, r) < 0 )
    {
        GTV_ERROR("launch gtv_player_thread failed");
        gtv_dataqueue_destroy(&(r->video_pkt_queue), 0);
        gtv_dataqueue_destroy(&(r->audio_pkt_queue), 0);
        gtv_dataqueue_destroy(&(r->redirect_pkt_queue), 0);
        
        gtv_databuffer_destroy(&(r->audio_buffer));
        
        free(r);
        return NULL;
    }
    GTV_INFO("gtv-player is opened (%s) (ver:%s) \n", r->server_url, _gtv_ver);
    
    return (HANDLE_GTV_PLAYER)r;
}

// player等待线程结束后再销毁，外部需要等销毁完成后，才能释放target对象，否则，会导致target先于内部线程释放
void gtv_player_close(HANDLE_GTV_PLAYER handle)
{
    if( handle == NULL ) {
        return;
    }
    
    GTV_INFO("gtv-player is closed (%s) \n", handle->server_url);
    
    handle->quit_flag = D_GTV_COM_FLAG_ON;
    handle->target = NULL;
    
    // 等待线程结束，再关闭
    while (handle->thread_running_flag != D_GTV_COM_FLAG_OFF) {
        usleep(30*1000);
    }
    
    // 销毁每一个packet和frame，不能依靠dataqueue自带的方法
    AVFrame * frame = NULL;
    int extra = 0;
    while( (frame = (AVFrame*)gtv_dataqueue_get(&(handle->video_pkt_queue), &extra)) != NULL ) {
        av_frame_free(&frame);
    }
    gtv_dataqueue_destroy(&(handle->video_pkt_queue), 0);
    
    while( (frame = (AVFrame*)gtv_dataqueue_get(&(handle->audio_pkt_queue), &extra)) != NULL ) {
        av_frame_free(&frame);
    }
    gtv_dataqueue_destroy(&(handle->audio_pkt_queue), 0);
    
    while( (frame = (AVFrame*)gtv_dataqueue_get(&(handle->redirect_pkt_queue), &extra)) != NULL ) {
        av_frame_free(&frame);
    }
    gtv_dataqueue_destroy(&(handle->redirect_pkt_queue), 0);
    
    // destroy buffer
    gtv_databuffer_destroy(&(handle->audio_buffer));
    
    // real free
    free(handle);
}

int gtv_player_set_range(HANDLE_GTV_PLAYER handle, int64_t start_milli, int64_t end_milli)
{
    if( handle == NULL ) {
        return -1;
    }
    GTV_INFO("gtv-player is set range (%lld,%lld) \n", start_milli, end_milli);
    handle->valid_start_milli = start_milli;
    handle->valid_end_milli = end_milli;
    
    return 0;
}

void gtv_player_set_pause_mode(HANDLE_GTV_PLAYER handle, uint8_t mode)
{
    GTV_INFO("gtv-player is set pause mode (%d) \n", mode);
	if (mode == D_GTV_COM_FLAG_OFF)
	{
		handle->pause_flag = D_GTV_COM_FLAG_OFF;
	}
    else
	{
		handle->pause_flag = D_GTV_COM_FLAG_ON;
	}
}

int gtv_player_peek_next_video(HANDLE_GTV_PLAYER handle, ST_GTV_RAW_FRAME_REF ref)
{
    int extra = 0;
    
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->redirect_pkt_queue.count <= 0 )
        return 0;
    
    AVFrame * vframe = (AVFrame *)gtv_dataqueue_peek_first(&(handle->redirect_pkt_queue), &extra);
    if( vframe == NULL )
        return 0;
    
    ref->type = GTV_VIDEO_FRAME;
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

int gtv_player_remove_next_video(HANDLE_GTV_PLAYER handle)
{
    int extra = 0;
    
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->redirect_pkt_queue.count <= 0 )
        return 0;
    
    AVFrame * vframe = (AVFrame *)gtv_dataqueue_get(&(handle->redirect_pkt_queue), &extra);
    if( vframe == NULL )
        return 0;
    
    av_frame_free(&vframe);
    
    return 1;
}

int gtv_player_pull_audio(HANDLE_GTV_PLAYER handle, uint8_t*buf, int len)
{
    if( handle == NULL ) {
        return 0;
    }
    
    // if paused get none data
    if( handle->pause_flag || handle->quit_flag ) {
        return 0;
    }
    
    // TODO: mutex
    {
        int extra;
        HANDLE_GTV_FFREADER reader = (handle->reader_addr);
        
        AVFrame * aframe = NULL;
        do {
            
            aframe = gtv_dataqueue_peek_first(&(handle->audio_pkt_queue), &extra);
            
            if( aframe != NULL ) {
                // invalid frame data, drop it
                if( extra != gtv_ffreader_get_seekindex(reader) ) {
                    // remove frame from queue
                    aframe = gtv_dataqueue_get(&(handle->audio_pkt_queue), &extra);
                    av_frame_free(&aframe);
					aframe = -1;		// 为了可以继续排除seek之前的数据，设为-1
                }
                // valid frame data
                else {
                    
                    // check if data if out of range
                    if( (reader->valid_start_milli >= 0 && aframe->pts < reader->valid_start_milli) ||
                       (reader->valid_end_milli >= 0 && aframe->pts > reader->valid_end_milli) )
                    {
                        // to force timestamp goto valid_end_milli
                        if( handle->current_timestamp < reader->valid_end_milli && aframe->pts > reader->valid_end_milli ) {
                            handle->current_timestamp = reader->valid_end_milli;
                        }
                        
                        aframe = gtv_dataqueue_get(&(handle->audio_pkt_queue), &extra);
                        av_frame_free(&aframe);
                        aframe = -1;
                    }
                    // check if there is space in databuffer
                    else if( convert_and_put_audio(handle, aframe) > 0 ) {
                        // audio data is used
                        aframe = gtv_dataqueue_get(&(handle->audio_pkt_queue), &extra);
                        handle->current_timestamp = aframe->pts + (aframe->nb_samples*1000/44100);// TODO:
                        av_frame_free(&aframe);
                        aframe = -1;
                    }
                    else {
                        aframe = NULL;
                    }
                }
            }
            
        } while(aframe != NULL);
    }
    
    int ret = gtv_databuffer_get(&(handle->audio_buffer), buf, len);
//    if( ret < len )
//    GTV_DEBUG("gtv_databuffer_get %d < %d --- %02x%02x%02x%02x%02x%02x%02x%02x", ret, len, buf[0], buf[1], buf[2], buf[3], buf[4], buf[5], buf[6], buf[7]);
    
    // 防止seek中的时候上位抛错
    if( gtv_ffreader_inseeking((handle->reader_addr)) > 0 ) {
        if( ret == 0 ) {
            ret = len;
            memset(buf, 0x00, len);
            return ret;
        }
    }
    
    // no data in audio_buffer
    if( ret == 0 ) {
        int dur = gtv_player_get_duration(handle);
        if( handle->current_timestamp >= 0 && handle->current_timestamp < dur ) {
            HANDLE_GTV_FFREADER r = (HANDLE_GTV_FFREADER) handle->reader_addr;
            // only video data left
            if( r != NULL && r->audio_packet_decode_queue.count == 0 && (r->video_packet_decode_queue.count > 0 || r->weak_video_frame_queue->count > 0) ) {
                ret = len;
                memset(buf, 0x00, len);
                handle->current_timestamp += 20;
                return ret;
            }
            if( gtv_ffreader_check_eof(r) > 0 && handle->audio_pkt_queue.count == 0 && handle->video_pkt_queue.count == 0 ) {
                handle->current_timestamp = dur;
            }
        }
    }
    
    return ret;
}

int gtv_player_current_timestamp(HANDLE_GTV_PLAYER handle)
{
    int dur = 0;
    
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->stream_sts == GTV_PLAYER_STREAM_EOF ) {
        dur = gtv_ffreader_get_duration((HANDLE_GTV_FFREADER)handle->reader_addr);
        if( dur > 0 )
            return dur;
    }
    
    return (int)(handle->current_timestamp);
}

int gtv_player_get_duration(HANDLE_GTV_PLAYER handle)
{
    if( handle == NULL && handle->reader_addr != NULL ) {
        return 0;
    }
    
    if( handle->quit_flag ) {
        return 0;
    }
    
    // TODO:mutex
    int dur = gtv_ffreader_get_duration((HANDLE_GTV_FFREADER)handle->reader_addr);
    
    return dur;
}

int gtv_player_seekto(HANDLE_GTV_PLAYER handle, int milli)
{
    if( handle == NULL && handle->reader_addr != NULL ) {
        return -1;
    }
    
    if( handle->quit_flag ) {
        return -1;
    }
    
    // TODO:mutex
    handle->first_rend = 0;
    handle->current_timestamp = milli;  // conflict with thread
    handle->stream_sts = GTV_PLAYER_STREAM_STREAMING; // force into streaming status
    gtv_ffreader_seekto((HANDLE_GTV_FFREADER)handle->reader_addr, milli);
	gtv_databuffer_clear(&(handle->audio_buffer));
    
    return milli;
}

int gtv_player_check_status(HANDLE_GTV_PLAYER handle)
{
    /*
     GTV_PLAYER_STREAM_OPENED        = 0x5000,
     GTV_PLAYER_STREAM_STREAMING     = 0x5001,
     GTV_PLAYER_STREAM_EOF           = 0x5002
    */
    if( handle == NULL )
        return GTV_PLAYER_STREAM_UNKNOWN;
    
    return handle->stream_sts;
}
