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

#include "yy_player.h"
#include "yy_logger.h"
#include "yy_com_utils.h"

#include "yy_ffmpeg_reader.h"

#if defined(TARGET_OS_ANDROID)
#include "videoplayer_jni.h"
#endif


static char * _YY_ver = "0.0.2";

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// private function
////////////////////////////////////////////////////////////////////////////////////////////////////////////

static void live_player_notify_event(HANDLE_YY_PLAYER handle, int event, int param1, int param2)
{
    if( handle->target == NULL || handle->event_callback == NULL ) {
        return;
    }
    
    handle->event_callback(handle->target, handle, event, param1, param2);
}

static int convert_and_put_audio(HANDLE_YY_PLAYER handle, AVFrame * f)
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

static void * YY_player_thread(void * thread_data)
{
    HANDLE_YY_PLAYER handle = (HANDLE_YY_PLAYER)thread_data;
    int64_t inited_timestamp = 0;
    int player_error_no = YY_PLAYER_NO_ERROR;
    int video_width,video_height;
//    int first_rend = 0;
    
    HANDLE_YY_FFREADER reader = NULL;
    
    if( handle == NULL )
        return NULL;
    
    {
        struct sched_param sched;
        int policy;
        pthread_t thread = pthread_self();
        
        if (pthread_getschedparam(thread, &policy, &sched) < 0) {
            YY_ERROR("pthread_getschedparam() failed");
        }
        else {
            sched.sched_priority = sched_get_priority_max(policy);
            if (pthread_setschedparam(thread, policy, &sched) < 0) {
                YY_ERROR("pthread_setschedparam() failed");
            }
        }
    }
    
    handle->first_rend = 0;
    handle->thread_running_flag = D_YY_COM_FLAG_ON;
	
	#if defined(TARGET_OS_ANDROID)
	JNI_AttachThread();
	#endif
	
    // notify inited
    live_player_notify_event(handle, YY_PLAYER_EVT_INITED, 0, 0);
    YY_INFO("YY-player inited (%s) \n", handle->server_url);

    inited_timestamp = YY_system_current_milli();
    player_error_no = YY_PLAYER_NO_ERROR;
    
    // open reader
    reader = YY_ffreader_open(handle->server_url, &(handle->audio_pkt_queue), &(handle->video_pkt_queue));
    handle->reader_addr = (void*)reader;
    handle->start_play_flag = D_YY_COM_FLAG_OFF;
    
    handle->stream_sts = YY_PLAYER_STREAM_OPENED;
    
    YY_ffreader_valid_range(reader, handle->valid_start_milli, handle->valid_end_milli);
    
    // 先开始缓冲，缓冲到一定程度在开始，如果在1秒内还没能完成缓冲，结束，抛错
    while(handle->quit_flag == D_YY_COM_FLAG_OFF && handle->start_play_flag == D_YY_COM_FLAG_OFF ) {
        
        YY_milliseconds_sleep(20);
        
        if( YY_ffreader_check_opened(reader) > 0 ) {
            
            // 缓冲一定程度再开始 TODO:
            int is_video_ready = 1;
            int is_audio_ready = 1;
            if( YY_ffreader_audio_existed(reader) && handle->audio_pkt_queue.count < 5 ) {
                is_audio_ready = 0;
            }
            if( YY_ffreader_video_existed(reader) && handle->video_pkt_queue.count < 1 ) {
                is_video_ready = 0;
            }
            if( is_audio_ready > 0 && is_video_ready > 0 ) {
                handle->start_play_flag = D_YY_COM_FLAG_ON;
                break;
            }
        }
//        if( handle->video_pkt_queue.count > 5 || handle->audio_pkt_queue.count > 5 ) {
//            handle->start_play_flag = D_YY_COM_FLAG_ON;
//            break;
//        }
        
        // 缓冲超时
        int64_t curr_milli = YY_system_current_milli();
        if( curr_milli - inited_timestamp > 6000 ) {
            player_error_no = YY_PLAYER_TIMEOUT_ERROR;
            break;
        }
    }
    
    // 通知外部数据准备完成
    if( player_error_no == YY_PLAYER_NO_ERROR ) {
        YY_ffreader_video_size(reader, &video_width, &video_height);
        YY_INFO("YY-player prepared (%d,%d) (%s) %d %d dur:%d \n", video_width, video_height, handle->server_url, handle->video_pkt_queue.count, handle->audio_pkt_queue.count, YY_ffreader_get_duration(reader));
        live_player_notify_event(handle, YY_PLAYER_EVT_PREPARED, video_width, video_height);
    }
    else {
        
        YY_INFO("YY-player prepare failed (%s) %d %d dur:%d \n", handle->server_url, handle->video_pkt_queue.count, handle->audio_pkt_queue.count, YY_ffreader_get_duration(reader));
    }
    
    // start play
    handle->current_timestamp = 0;
    handle->stream_sts = YY_PLAYER_STREAM_STREAMING;
    
    int64_t last_loop_timestamp = YY_system_current_milli();
    int64_t last_video_timestamp = YY_system_current_milli();
    int64_t last_videoframe_pts = -1;
    float sleeptime_for_next = 5.0f;
    
    while(handle->quit_flag == D_YY_COM_FLAG_OFF && player_error_no == YY_PLAYER_NO_ERROR) {
        
        if( YY_ffreader_inseeking(handle->reader_addr) == 0 ) {
            
            if( sleeptime_for_next > 0 && sleeptime_for_next < 50 ) {
//                int64_t bef = av_gettime_relative();
                YY_useconds_sleep((int)(sleeptime_for_next * 1000.0f));
//                YY_useconds_sleep(100);
//                int64_t aft = av_gettime_relative();
//                if( aft - bef > 3000 ) {
//                    YY_DEBUG("#### player delay %d %f \n", (int)(aft - bef), sleeptime_for_next);
//                }
            }
//            int32_t diff = (int32_t)(YY_system_current_milli() - last_loop_timestamp);
//            if( diff < 10 ) {
//                YY_milliseconds_sleep(15-diff);
//            }
        }
        
        last_loop_timestamp = YY_system_current_milli();
        sleeptime_for_next = 5.0f;
        
        // 确保即使被暂停也能显示一帧数据(但是设置了range之后，如果没有seek的话，不会显示第一帧)
        if( handle->pause_flag > 0 ) {
            handle->stream_sts = YY_PLAYER_STREAM_PAUSED;
            if( handle->first_rend != 0 )
                continue;
        }
        
        int extra = 0;
        
        // we update range every time here
        YY_ffreader_valid_range(reader, handle->valid_start_milli, handle->valid_end_milli);
        
        // read avframe from audio queue
        // 只有pause状态会主动读取数据，目的是seek后丢弃无效数据
        if( handle->pause_flag > 0 )
        {
            AVFrame * aframe = NULL;
            do {
                
                aframe = YY_dataqueue_peek_first(&(handle->audio_pkt_queue), &extra);
                
                if( aframe != NULL ) {
                    // invalid frame data, drop it
                    if( extra != YY_ffreader_get_seekindex(reader) && handle->pause_flag > 0 ) {
                        // remove frame from queue
                        aframe = YY_dataqueue_get(&(handle->audio_pkt_queue), &extra);
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
                
                vframe = YY_dataqueue_peek_first(&(handle->video_pkt_queue), &extra);
                
                if( vframe != NULL ) {
                    
                    int seek_force_disp = 0;
                    // if in seeking
                    if( YY_ffreader_inseeking((handle->reader_addr)) > 0 ) {
                        int diff = (int)(vframe->pts - handle->current_timestamp);
                        if( diff > -2000 && diff < 2000 ) {
                            seek_force_disp = 1;
                        }
                    }
                    
                    // invalid frame data, drop it
                    if( extra != YY_ffreader_get_seekindex(reader) && seek_force_disp == 0 ) {
                        // remove frame from queue
                        vframe = YY_dataqueue_get(&(handle->video_pkt_queue), &extra);
                        av_frame_free(&vframe);
                    }
                    // valid frame data
                    else {
                        
                        // check if data if out of range
                        if( (reader->valid_start_milli >= 0 && vframe->pts < reader->valid_start_milli) ||
                           (reader->valid_end_milli >= 0 && vframe->pts > reader->valid_end_milli) )
                        {
                            vframe = YY_dataqueue_get(&(handle->video_pkt_queue), &extra);
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
                                
                                ST_YY_RAW_FRAME raw_frame;
                                
                                raw_frame.type = YY_VIDEO_FRAME;
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
                                YY_DEBUG("#### player display pts:%lld (%d-%d) diff:%d \n", vframe->pts, (int)(vframe->pts-last_videoframe_pts), (int)(av_gettime_relative()/1000-last_video_timestamp), handle->current_timestamp-vframe->pts);
                                // save last video info
                                last_videoframe_pts = vframe->pts;
                                last_video_timestamp = av_gettime_relative()/1000;
//                                YY_DEBUG("#### player display pts:%lld\n", vframe->pts);
                                
                                handle->data_callback(handle->target, &raw_frame);
                                
                                if( handle->first_rend == 0 ) {
                                    handle->first_rend = 1;
                                }
                                
                                // remove frame from queue
                                vframe = YY_dataqueue_get(&(handle->video_pkt_queue), &extra);
                                av_frame_free(&vframe);
                            }
                            else {
                                // redirect it to another queue
                                vframe = YY_dataqueue_get(&(handle->video_pkt_queue), &extra);
                                YY_dataqueue_put(&(handle->redirect_pkt_queue), vframe, extra);
                                
                                if( handle->first_rend == 0 ) {
                                    handle->first_rend = 1;
                                }
                            }
                        }
                        else {
                            // not the right time to disp
//                            YY_DEBUG("curr time %lld but video pts %lld ", handle->current_timestamp, vframe->pts);
                            vframe = NULL;
                        }
                    }
                }
                
            } while(vframe != NULL);
        }
        
        // update reading status
        if( YY_ffreader_check_eof(reader) > 0 && handle->video_pkt_queue.count == 0 && handle->audio_pkt_queue.count == 0 ) {
            handle->stream_sts = YY_PLAYER_STREAM_EOF;
        }
        else {
            if( handle->pause_flag > 0 ) {
                handle->stream_sts = YY_PLAYER_STREAM_PAUSED;
            }
            else {
                handle->stream_sts = YY_PLAYER_STREAM_STREAMING;
            }
        }
//        YY_DEBUG("#### video:%d, audio:%d\n", handle->video_pkt_queue.count, handle->audio_pkt_queue.count);
    }
    
    // 通知外部，播放结束(如果需要retry并且外部没有要求停止，不发送通知)
    if( handle->quit_flag == D_YY_COM_FLAG_ON ) {
        live_player_notify_event(handle, YY_PLAYER_EVT_FINISHED, player_error_no, 0);
    }
    YY_INFO("YY-player finished (%s) dur:(%d) err:(%d) \n", handle->server_url, (int)(YY_system_current_milli()-inited_timestamp), player_error_no);
    
    // destroy reader
    YY_INFO("YY-player YY_ffreader_close \n");
    if( reader != NULL ) {
        handle->reader_addr = NULL;// TODO: mutex
        YY_ffreader_close(reader);
        reader = NULL;
    }
    
    YY_INFO("YY-player finished completely %d \n", handle->quit_flag);
    
    handle->start_play_flag = D_YY_COM_FLAG_OFF;
    handle->thread_running_flag = D_YY_COM_FLAG_OFF;
	
	#if defined(TARGET_OS_ANDROID)
	JNI_detachThread();
	#endif
    
    return NULL;
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// public function
////////////////////////////////////////////////////////////////////////////////////////////////////////////

HANDLE_YY_PLAYER YY_player_open(char * url, FUNC_PLAYER_EVENT_NOTIFY evt_func, FUNC_PLAYER_DATA_NOTIFY data_func, void * target)
{
    HANDLE_YY_PLAYER r;
    
    if( url == NULL || strlen(url) >= YY_COM_URL_LIMIT_LEN-1 ) {
        YY_ERROR("YY_player_open ng url \n");
        return NULL;
    }
    
    r = (HANDLE_YY_PLAYER)malloc(sizeof(ST_YY_LIVE_PLAYER));
    
    memset(r, 0x00, sizeof(ST_YY_LIVE_PLAYER));
    
    strncpy(r->server_url, url, YY_COM_URL_LIMIT_LEN-1);
    
    r->target = target;
    r->event_callback = evt_func;
    r->data_callback = data_func;
    r->quit_flag = D_YY_COM_FLAG_OFF;
    r->thread_running_flag = D_YY_COM_FLAG_ON; // 防止线程还没启动，就被close
    
	r->pause_flag = D_YY_COM_FLAG_OFF;
	r->start_play_flag = D_YY_COM_FLAG_OFF;
    r->stream_sts = YY_PLAYER_STREAM_UNKNOWN;

    r->valid_start_milli = r->valid_end_milli = -1;
    
    YY_dataqueue_init(&(r->video_pkt_queue));
    YY_dataqueue_init(&(r->audio_pkt_queue));
    YY_dataqueue_init(&(r->redirect_pkt_queue));
    
    YY_databuffer_init(&(r->audio_buffer), 4096*3);
    
    // start thread
    if( pthread_create(&(r->thread_id), NULL, YY_player_thread, r) < 0 )
    {
        YY_ERROR("launch YY_player_thread failed");
        YY_dataqueue_destroy(&(r->video_pkt_queue), 0);
        YY_dataqueue_destroy(&(r->audio_pkt_queue), 0);
        YY_dataqueue_destroy(&(r->redirect_pkt_queue), 0);
        
        YY_databuffer_destroy(&(r->audio_buffer));
        
        free(r);
        return NULL;
    }
    YY_INFO("YY-player is opened (%s) (ver:%s) \n", r->server_url, _YY_ver);
    
    return (HANDLE_YY_PLAYER)r;
}

// player等待线程结束后再销毁，外部需要等销毁完成后，才能释放target对象，否则，会导致target先于内部线程释放
void YY_player_close(HANDLE_YY_PLAYER handle)
{
    if( handle == NULL ) {
        return;
    }
    
    YY_INFO("YY-player is closed (%s) \n", handle->server_url);
    
    handle->quit_flag = D_YY_COM_FLAG_ON;
    handle->target = NULL;
    
    // 等待线程结束，再关闭
    while (handle->thread_running_flag != D_YY_COM_FLAG_OFF) {
        usleep(30*1000);
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
    
    while( (frame = (AVFrame*)YY_dataqueue_get(&(handle->redirect_pkt_queue), &extra)) != NULL ) {
        av_frame_free(&frame);
    }
    YY_dataqueue_destroy(&(handle->redirect_pkt_queue), 0);
    
    // destroy buffer
    YY_databuffer_destroy(&(handle->audio_buffer));
    
    // real free
    free(handle);
}

int YY_player_set_range(HANDLE_YY_PLAYER handle, int64_t start_milli, int64_t end_milli)
{
    if( handle == NULL ) {
        return -1;
    }
    YY_INFO("YY-player is set range (%lld,%lld) \n", start_milli, end_milli);
    handle->valid_start_milli = start_milli;
    handle->valid_end_milli = end_milli;
    
    return 0;
}

void YY_player_set_pause_mode(HANDLE_YY_PLAYER handle, uint8_t mode)
{
    YY_INFO("YY-player is set pause mode (%d) \n", mode);
	if (mode == D_YY_COM_FLAG_OFF)
	{
		handle->pause_flag = D_YY_COM_FLAG_OFF;
	}
    else
	{
		handle->pause_flag = D_YY_COM_FLAG_ON;
	}
}

int YY_player_peek_next_video(HANDLE_YY_PLAYER handle, ST_YY_RAW_FRAME_REF ref)
{
    int extra = 0;
    
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->redirect_pkt_queue.count <= 0 )
        return 0;
    
    AVFrame * vframe = (AVFrame *)YY_dataqueue_peek_first(&(handle->redirect_pkt_queue), &extra);
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

int YY_player_remove_next_video(HANDLE_YY_PLAYER handle)
{
    int extra = 0;
    
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->redirect_pkt_queue.count <= 0 )
        return 0;
    
    AVFrame * vframe = (AVFrame *)YY_dataqueue_get(&(handle->redirect_pkt_queue), &extra);
    if( vframe == NULL )
        return 0;
    
    av_frame_free(&vframe);
    
    return 1;
}

int YY_player_pull_audio(HANDLE_YY_PLAYER handle, uint8_t*buf, int len)
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
        HANDLE_YY_FFREADER reader = (handle->reader_addr);
        
        AVFrame * aframe = NULL;
        do {
            
            aframe = YY_dataqueue_peek_first(&(handle->audio_pkt_queue), &extra);
            
            if( aframe != NULL ) {
                // invalid frame data, drop it
                if( extra != YY_ffreader_get_seekindex(reader) ) {
                    // remove frame from queue
                    aframe = YY_dataqueue_get(&(handle->audio_pkt_queue), &extra);
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
                        
                        aframe = YY_dataqueue_get(&(handle->audio_pkt_queue), &extra);
                        av_frame_free(&aframe);
                        aframe = -1;
                    }
                    // check if there is space in databuffer
                    else if( convert_and_put_audio(handle, aframe) > 0 ) {
                        // audio data is used
                        aframe = YY_dataqueue_get(&(handle->audio_pkt_queue), &extra);
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
    
    int ret = YY_databuffer_get(&(handle->audio_buffer), buf, len);
//    if( ret < len )
//    YY_DEBUG("YY_databuffer_get %d < %d --- %02x%02x%02x%02x%02x%02x%02x%02x", ret, len, buf[0], buf[1], buf[2], buf[3], buf[4], buf[5], buf[6], buf[7]);
    
    // 防止seek中的时候上位抛错
    if( YY_ffreader_inseeking((handle->reader_addr)) > 0 ) {
        if( ret == 0 ) {
            ret = len;
            memset(buf, 0x00, len);
            return ret;
        }
    }
    
    // no data in audio_buffer
    if( ret == 0 ) {
        int dur = YY_player_get_duration(handle);
        if( handle->current_timestamp >= 0 && handle->current_timestamp < dur ) {
            HANDLE_YY_FFREADER r = (HANDLE_YY_FFREADER) handle->reader_addr;
            // only video data left
            if( r != NULL && r->audio_packet_decode_queue.count == 0 && (r->video_packet_decode_queue.count > 0 || r->weak_video_frame_queue->count > 0) ) {
                ret = len;
                memset(buf, 0x00, len);
                handle->current_timestamp += 20;
                return ret;
            }
            if( YY_ffreader_check_eof(r) > 0 && handle->audio_pkt_queue.count == 0 && handle->video_pkt_queue.count == 0 ) {
                handle->current_timestamp = dur;
            }
        }
    }
    
    return ret;
}

int YY_player_current_timestamp(HANDLE_YY_PLAYER handle)
{
    int dur = 0;
    
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->stream_sts == YY_PLAYER_STREAM_EOF ) {
        dur = YY_ffreader_get_duration((HANDLE_YY_FFREADER)handle->reader_addr);
        if( dur > 0 )
            return dur;
    }
    
    return (int)(handle->current_timestamp);
}

int YY_player_get_duration(HANDLE_YY_PLAYER handle)
{
    if( handle == NULL && handle->reader_addr != NULL ) {
        return 0;
    }
    
    if( handle->quit_flag ) {
        return 0;
    }
    
    // TODO:mutex
    int dur = YY_ffreader_get_duration((HANDLE_YY_FFREADER)handle->reader_addr);
    
    return dur;
}

int YY_player_seekto(HANDLE_YY_PLAYER handle, int milli)
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
    handle->stream_sts = YY_PLAYER_STREAM_STREAMING; // force into streaming status
    YY_ffreader_seekto((HANDLE_YY_FFREADER)handle->reader_addr, milli);
	YY_databuffer_clear(&(handle->audio_buffer));
    
    return milli;
}

int YY_player_check_status(HANDLE_YY_PLAYER handle)
{
    /*
     YY_PLAYER_STREAM_OPENED        = 0x5000,
     YY_PLAYER_STREAM_STREAMING     = 0x5001,
     YY_PLAYER_STREAM_EOF           = 0x5002
    */
    if( handle == NULL )
        return YY_PLAYER_STREAM_UNKNOWN;
    
    return handle->stream_sts;
}
