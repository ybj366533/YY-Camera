//
//  Created by YY on 2018/1/29.
//  Copyright © 2018年 YY. All rights reserved.
//

#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/opt.h>

#include "yy_remuxer.h"
#include "yy_logger.h"

static void log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt, const char *tag)
{
    //AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;
    
    //printf("%s: pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d\n",
    //       tag,
    //       av_ts2str(pkt->pts), av_ts2timestr(pkt->pts, time_base),
    //       av_ts2str(pkt->dts), av_ts2timestr(pkt->dts, time_base),
    //       av_ts2str(pkt->duration), av_ts2timestr(pkt->duration, time_base),
    //       pkt->stream_index);
		   
	//YY_ERROR("%s: pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d\n",
    //       tag,
    //       av_ts2str(pkt->pts), av_ts2timestr(pkt->pts, time_base),
    //       av_ts2str(pkt->dts), av_ts2timestr(pkt->dts, time_base),
    //       av_ts2str(pkt->duration), av_ts2timestr(pkt->duration, time_base),
    //       pkt->stream_index);
}

int YY_mp4_video_reverse(const char *in_filename, const char * out_filename)
{
    
    AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx = NULL, *ofmt_ctx = NULL;
    AVPacket pkt;
    //const char *in_filename, *out_filename;
    int ret, i;
    int stream_index = 0;
    //    int *stream_mapping = NULL;
    //    int stream_mapping_size = 0;
    
    av_register_all();        //todo register once?
    
    if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
        fprintf(stderr, "Could not open input file '%s'", in_filename);
        goto end;
    }
    
    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        fprintf(stderr, "Failed to retrieve input stream information");
        goto end;
    }
    
    av_dump_format(ifmt_ctx, 0, in_filename, 0);    //todo comment?
    
    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
    if (!ofmt_ctx) {
        fprintf(stderr, "Could not create output context\n");
        ret = AVERROR_UNKNOWN;
        goto end;
    }
    
    ///*    stream_mapping_size = ifmt_ctx->nb_streams;
    //    stream_mapping = av_mallocz_array(stream_mapping_size, sizeof(*stream_mapping));
    //    if (!stream_mapping) {
    //        ret = AVERROR(ENOMEM);
    //        goto end;
    //    }*/
    
    ofmt = ofmt_ctx->oformat;
    
    int64_t frame_number = 0;
    int64_t in_stream_duration = 0;
    
    int in_stream_video_index = -1;
    int out_stream_video_index = -1;
    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;
        
        //        if (in_codecpar->codec_type != AVMEDIA_TYPE_AUDIO &&
        //            in_codecpar->codec_type != AVMEDIA_TYPE_VIDEO &&
        //            in_codecpar->codec_type != AVMEDIA_TYPE_SUBTITLE) {
        //            stream_mapping[i] = -1;
        //            continue;
        //        }
        
        // only video
        if (in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            in_stream_video_index = i;
            out_stream_video_index = 0;
            frame_number = in_stream->nb_frames;
            in_stream_duration = in_stream->duration;
//            YY_DEBUG("duration %lld \n", in_stream_duration);
            
            out_stream = avformat_new_stream(ofmt_ctx, NULL);
            if (!out_stream) {
                fprintf(stderr, "Failed allocating output stream\n");
                ret = AVERROR_UNKNOWN;
                goto end;
            }
            
            ret = avcodec_parameters_copy(out_stream->codecpar, in_codecpar);
            if (ret < 0) {
                fprintf(stderr, "Failed to copy codec parameters\n");
                goto end;
            }
            out_stream->codecpar->codec_tag = 0;
            break;;
        }
    }
    
    if (in_stream_video_index == -1) {
        ret = -1;
        fprintf(stderr, "input file no video stream\n");
        goto end;
    }
    av_dump_format(ofmt_ctx, 0, out_filename, 1);
    
    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            fprintf(stderr, "Could not open output file '%s'", out_filename);
            goto end;
        }
    }
    
    ret = avformat_write_header(ofmt_ctx, NULL);
    if (ret < 0) {
        fprintf(stderr, "Error occurred when opening output file\n");
        goto end;
    }
    
    if(frame_number == 0) {
        while (1) {
            AVStream *in_stream, *out_stream;
            
            ret = av_read_frame(ifmt_ctx, &pkt);
            if (ret < 0) {
                YY_ERROR("%ld %d\n", pkt.pts, pkt.size);
                break;
            }
//            YY_DEBUG("%ld %d\n", pkt.pts, pkt.size);
            
            in_stream  = ifmt_ctx->streams[pkt.stream_index];
            if (pkt.stream_index != in_stream_video_index) {
                av_packet_unref(&pkt);
                continue;
            }
            
            frame_number++;
            av_packet_unref(&pkt);
        }
    }
    
    int64_t seek_timestamp = in_stream_duration + 10000;            // todo changge to actual duration?
    int64_t out_pts = 0;
    for ( int i = 0; i < frame_number; ++i) {
        av_seek_frame(ifmt_ctx,in_stream_video_index, seek_timestamp, AVSEEK_FLAG_BACKWARD);
        
        AVStream *in_stream, *out_stream;
        
        ret = av_read_frame(ifmt_ctx, &pkt);
        if (ret < 0) {
            YY_ERROR("%ld %d % ld\n", pkt.pts, pkt.size, pkt.duration);
            //todo  goto end?
            break;
        }
        
//        YY_DEBUG("%ld %d % ld\n", pkt.pts, pkt.size, pkt.duration);
        
        //frame_number++;
        
        in_stream  = ifmt_ctx->streams[pkt.stream_index];
//        if (pkt.stream_index != in_stream_video_index) {
//        	// 指定流id seek，不会到这里把
//            av_packet_unref(&pkt);
//            continue;
//        }
        while (pkt.stream_index != in_stream_video_index ){
        	av_packet_unref(&pkt);

        	ret = av_read_frame(ifmt_ctx, &pkt);
        	if (ret < 0) {
        		break;
        	} else {
        		in_stream  = ifmt_ctx->streams[pkt.stream_index];
        	}

        }

        if (ret < 0) {
        	YY_ERROR("%ld %d % ld\n", pkt.pts, pkt.size, pkt.duration);
        	//todo  goto end?
            break;
        }
        
        pkt.stream_index = out_stream_video_index;
        out_stream = ofmt_ctx->streams[pkt.stream_index];
        log_packet(ifmt_ctx, &pkt, "in");
        
		//todo 不同ffmpeg版本会不一样？
        //seek_timestamp = pkt.pts -1 - ifmt_ctx->streams[in_stream_video_index]->start_time;
		seek_timestamp = pkt.pts -1;
        
        /* copy packet */
        //pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
        //pkt.pts = (in_stream_duration - pkt.pts) < 0 ? 0: (in_stream_duration - pkt.pts);
        //pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
        pkt.pts = out_pts;
        pkt.dts = pkt.pts;
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        out_pts += pkt.duration;
        pkt.pos = -1;
        log_packet(ofmt_ctx, &pkt, "out");
        
        ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
        if (ret < 0) {
            fprintf(stderr, "Error muxing packet\n");
            break;
        }
        av_packet_unref(&pkt);
        //timestamp = pkt.pts -1 - ifmt_ctx->streams[pkt.stream_index]->start_time;
    }
    
    //av_seek_frame(ifmt_ctx,0, 5, AVSEEK_FLAG_FRAME);
    av_write_trailer(ofmt_ctx);
end:
    
    avformat_close_input(&ifmt_ctx);
    
    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);
    
    //av_freep(&stream_mapping);
    
    if (ret < 0 && ret != AVERROR_EOF) {
        fprintf(stderr, "Error occurred: %s\n", av_err2str(ret));
        return -1;
    }
    return 0;
}


//int main(int argc, char **argv)
int YY_mp4_clips_merge(const char *const*in_filenames, int in_file_num, const char * out_filename)
{
    AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx = NULL, *ofmt_ctx = NULL;
    AVPacket pkt;
    const char *in_filename;
    int ret, i;
    int stream_index = 0;
    //int *stream_mapping = NULL;
    //int stream_mapping_size = 0;
    int in_stream_video_index = -1;
    int in_stream_audio_index = -1;
    int out_stream_video_index = -1;
    int out_stream_audio_index = -1;
    
    if (in_file_num < 1) {
        return -1;
    }
    
	YY_ERROR("----merge start\n");
	
    av_register_all();
    
    double current_duration = 0;
    double last_clip_duration = 0;
    
    for ( int k = 0; k < in_file_num; ++k) {
        
        current_duration = last_clip_duration;
        in_filename  = in_filenames[k];
		
		YY_ERROR("----%d %s\n", i, in_filename);
        // the first file
        if (k == 0) {
            if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
                //fprintf(stderr, "Could not open input file '%s'", in_filename);
				YY_ERROR("Could not open input file '%s'", in_filename);
                goto end;
            }
            
            if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
                //fprintf(stderr, "Failed to retrieve input stream information");
				YY_ERROR("Failed to retrieve input stream information");
                goto end;
            }
            
            av_dump_format(ifmt_ctx, 0, in_filename, 0);
            
            avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
            if (!ofmt_ctx) {
                //fprintf(stderr, "Could not create output context\n");
				YY_ERROR("Could not create output context\n");
                ret = AVERROR_UNKNOWN;
                goto end;
            }
            
            ofmt = ofmt_ctx->oformat;
            
            for (i = 0; i < ifmt_ctx->nb_streams; i++) {
                AVStream *out_stream;
                AVStream *in_stream = ifmt_ctx->streams[i];
                AVCodecParameters *in_codecpar = in_stream->codecpar;
                
                if (in_stream_video_index == -1 && in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                    in_stream_video_index = i;
                    out_stream_video_index = stream_index++;
                } else if (in_stream_audio_index == -1 && in_codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
                    in_stream_audio_index = i;
                    out_stream_audio_index = stream_index++;
                } else {
                    continue;
                }
                
                out_stream = avformat_new_stream(ofmt_ctx, NULL);
                if (!out_stream) {
                    YY_ERROR("Failed allocating output stream\n");
                    ret = AVERROR_UNKNOWN;
                    goto end;
                }
                
                ret = avcodec_parameters_copy(out_stream->codecpar, in_codecpar);
                if (ret < 0) {
                    YY_ERROR("Failed to copy codec parameters\n");
                    goto end;
                }
                out_stream->codecpar->codec_tag = 0;
            }
            av_dump_format(ofmt_ctx, 0, out_filename, 1);
            
            if (!(ofmt->flags & AVFMT_NOFILE)) {
                ret = avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE);
                if (ret < 0) {
                    YY_ERROR("Could not open output file '%s'", out_filename);
                    goto end;
                }
            }
            
            ret = avformat_write_header(ofmt_ctx, NULL);
            if (ret < 0) {
                YY_ERROR("Error occurred when opening output file\n");
                goto end;
            }
        } else {
            if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
                YY_ERROR("Could not open input file '%s'", in_filename);
                goto end;
            }
            
            if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
                YY_ERROR("Failed to retrieve input stream information");
                goto end;
            }
            
            in_stream_video_index = -1;
            in_stream_audio_index = -1;
            
            for (i = 0; i < ifmt_ctx->nb_streams; i++) {
                AVStream *out_stream;
                AVStream *in_stream = ifmt_ctx->streams[i];
                AVCodecParameters *in_codecpar = in_stream->codecpar;
                
                if (in_stream_video_index == -1 && in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                    in_stream_video_index = i;
                } else if (in_stream_audio_index == -1 && in_codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
                    in_stream_audio_index = i;
                } else {
                    continue;
                }
            }
            
            av_dump_format(ifmt_ctx, 0, in_filename, 0);
        }
        
        int frame_number = 0;
        while (1) {
            AVStream *in_stream, *out_stream;
            
            ret = av_read_frame(ifmt_ctx, &pkt);
            if (ret < 0)
                break;
            
            //printf("%ld %d\n", pkt.pts, pkt.size);
            
            frame_number++;
            
            in_stream  = ifmt_ctx->streams[pkt.stream_index];
            if (in_stream_video_index != -1 && out_stream_video_index != -1 && pkt.stream_index == in_stream_video_index) {
                pkt.stream_index = out_stream_video_index;
            } else if (in_stream_audio_index != -1 && out_stream_audio_index != -1 && pkt.stream_index == in_stream_audio_index)
            {
                pkt.stream_index = out_stream_audio_index;
            } else {
                av_packet_unref(&pkt);
                continue;
            }
            
            out_stream = ofmt_ctx->streams[pkt.stream_index];
            log_packet(ifmt_ctx, &pkt, "in");
            
            /* copy packet */
            pkt.pts = current_duration /av_q2d(out_stream->time_base)  + av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
            pkt.dts = current_duration/av_q2d(out_stream->time_base) + av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
            pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
            pkt.pos = -1;
			// 20180209 最后的packet的时间戳不一定最小（因为音视频分别chunk的原因？）
			double last_clip_duration_tmp = (pkt.pts + pkt.duration) * av_q2d(out_stream->time_base);
			if (last_clip_duration_tmp > last_clip_duration) {
				last_clip_duration = last_clip_duration_tmp;
			}
            
            log_packet(ofmt_ctx, &pkt, "out");
            
            ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
            if (ret < 0) {
                YY_ERROR("Error muxing packet\n");
                break;
            }
            av_packet_unref(&pkt);
        }
        
        avformat_close_input(&ifmt_ctx);
        
        ifmt_ctx = NULL;
    }
    
    av_write_trailer(ofmt_ctx);
    
end:
    if (ifmt_ctx) {
        avformat_close_input(&ifmt_ctx);
    }
    
    
    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);
    
    //av_freep(&stream_mapping);
    
    if (ret < 0 && ret != AVERROR_EOF) {
        YY_ERROR("Error occurred: %s\n", av_err2str(ret));
        return 1;
    }
    
	YY_ERROR("----merge end\n");
	
    return 0;
}

int YY_mp4_audio_video_merge(const char *in_filename_a, const char *in_filename_v, const char * out_filename)
{
    
    AVOutputFormat *ofmt = NULL;
    AVFormatContext *ofmt_ctx = NULL;
    AVFormatContext *ifmt_ctx_a = NULL, *ifmt_ctx_v = NULL;
    AVPacket pkt;
    //const char *in_filename, *out_filename;
    int ret, i;
    int stream_index = 0;
    //    int *stream_mapping = NULL;
    //    int stream_mapping_size = 0;
    
    int in_stream_video_index = -1;
    int in_stream_audio_index = -1;
    int out_stream_video_index = -1;
    int out_stream_audio_index = -1;
    
    av_register_all();        //todo register once?
	
	YY_INFO("%s %s % s\n", in_filename_a, in_filename_v, out_filename);
    
    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
    if (!ofmt_ctx) {
        fprintf(stderr, "Could not create output context\n");
		YY_ERROR("Could not create output context\n");
        ret = AVERROR_UNKNOWN;
        goto end;
    }
    
    ofmt = ofmt_ctx->oformat;
        
    // video
    if ((ret = avformat_open_input(&ifmt_ctx_v, in_filename_v, 0, 0)) < 0) {
        fprintf(stderr, "Could not open input file '%s'", in_filename_v);
		YY_ERROR("Could not open input file '%s'", in_filename_v);
        goto end;
    }
    
    if ((ret = avformat_find_stream_info(ifmt_ctx_v, 0)) < 0) {
        fprintf(stderr, "Failed to retrieve input stream information");
		YY_ERROR("Failed to retrieve input stream information");
        goto end;
    }
    
    av_dump_format(ifmt_ctx_v, 0, in_filename_v, 0);    //todo comment?
    
    for (i = 0; i < ifmt_ctx_v->nb_streams; i++) {
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx_v->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;
        
        if (in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            in_stream_video_index = i;
            out_stream_video_index = 0;
            
            out_stream = avformat_new_stream(ofmt_ctx, NULL);
            if (!out_stream) {
                fprintf(stderr, "Failed allocating output stream\n");
				YY_ERROR("Failed allocating output stream\n");
                ret = AVERROR_UNKNOWN;
                goto end;
            }
            
            ret = avcodec_parameters_copy(out_stream->codecpar, in_codecpar);
            if (ret < 0) {
                fprintf(stderr, "Failed to copy codec parameters\n");
				YY_ERROR("Failed to copy codec parameters\n");
                goto end;
            }
            out_stream->codecpar->codec_tag = 0;
            break;;
        }
    }
    
    if (in_stream_video_index == -1) {
        ret = -1;
        fprintf(stderr, "input file no audio stream\n");
		YY_ERROR("input file no video stream\n");
        goto end;
    }
	
	// audio
    if ((ret = avformat_open_input(&ifmt_ctx_a, in_filename_a, 0, 0)) < 0) {
        fprintf(stderr, "Could not open input file '%s'", in_filename_a);
		YY_ERROR("Could not open input file '%s'", in_filename_a);
        goto end;
    }
    
    if ((ret = avformat_find_stream_info(ifmt_ctx_a, 0)) < 0) {
        fprintf(stderr, "Failed to retrieve input stream information");
		YY_ERROR("Failed to retrieve input stream information");
        goto end;
    }
    
    av_dump_format(ifmt_ctx_a, 0, in_filename_a, 0);    //todo comment?
    
    for (i = 0; i < ifmt_ctx_a->nb_streams; i++) {
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx_a->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;
        
        if (in_codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            in_stream_audio_index = i;
            out_stream_audio_index = 1;
            
            out_stream = avformat_new_stream(ofmt_ctx, NULL);
            if (!out_stream) {
                fprintf(stderr, "Failed allocating output stream\n");
				YY_ERROR("Failed allocating output stream\n");
                ret = AVERROR_UNKNOWN;
                goto end;
            }
            
            ret = avcodec_parameters_copy(out_stream->codecpar, in_codecpar);
            if (ret < 0) {
                fprintf(stderr, "Failed to copy codec parameters\n");
				YY_ERROR("Failed to copy codec parameters\n");
                goto end;
            }
            out_stream->codecpar->codec_tag = 0;
            break;;
        }
    }
    
    if (in_stream_audio_index == -1) {
        ret = -1;
        fprintf(stderr, "input file no audio stream\n");
		YY_ERROR("input file no audio stream\n");
        goto end;
    }
    
    //av_dump_format(ofmt_ctx, 0, out_filename, 1);
    
    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            fprintf(stderr, "Could not open output file '%s'", out_filename);
			YY_ERROR("Could not open output file '%s'", out_filename);
            goto end;
        }
    }
    
    ret = avformat_write_header(ofmt_ctx, NULL);
    if (ret < 0) {
        fprintf(stderr, "Error occurred when opening output file\n");
		YY_ERROR("Error occurred when opening output file\n");
        goto end;
    }
    
    int64_t cur_pts_v=0,cur_pts_a=0;
	int video_finish_flag = 0;
	int audio_finish_flag = 0;
    while(1) {
        
        AVFormatContext *ifmt_ctx;
        AVStream *in_stream, *out_stream;
		
		int stream_index;
        
        int get_pkt_flag = 0;
        
        if((av_compare_ts(cur_pts_v,ifmt_ctx_v->streams[in_stream_video_index]->time_base,cur_pts_a,ifmt_ctx_a->streams[in_stream_audio_index]->time_base) <= 0
			|| audio_finish_flag == 1)
			&& video_finish_flag != 1){
            ifmt_ctx=ifmt_ctx_v;
            stream_index=out_stream_video_index;
            
            if(av_read_frame(ifmt_ctx, &pkt) >= 0){
                do{
                    in_stream  = ifmt_ctx->streams[pkt.stream_index];
                    out_stream = ofmt_ctx->streams[stream_index];
                    
                    if(pkt.stream_index==in_stream_video_index){
                        cur_pts_v=pkt.pts;
                        
                        //pkt.stream_index = stream_index;
                        get_pkt_flag = 1;
                        break;
                    } else {
                        av_packet_unref(&pkt);
                    }
                }while(av_read_frame(ifmt_ctx, &pkt) >= 0);
            }else{
                //break;
				video_finish_flag = 1;
            }
        } else if (audio_finish_flag != 1) {
            ifmt_ctx=ifmt_ctx_a;
            stream_index=out_stream_audio_index;
            if(av_read_frame(ifmt_ctx, &pkt) >= 0){
                do{
                    in_stream  = ifmt_ctx->streams[pkt.stream_index];
                    out_stream = ofmt_ctx->streams[stream_index];
                    
                    if(pkt.stream_index==in_stream_audio_index){
                        
                        cur_pts_a=pkt.pts;
                        
                        //pkt.stream_index = stream_index;
                        get_pkt_flag = 1;
                        
                        break;
                    } else {
                        av_packet_unref(&pkt);
                    }
                }while(av_read_frame(ifmt_ctx, &pkt) >= 0);
            }else{
                //break;
				audio_finish_flag = 1;
            }
        } else {
			break;
		}
        
        if (get_pkt_flag > 0) {
            
            log_packet(ifmt_ctx, &pkt, "in");
			//YY_ERROR("todo  test %ld\n", pkt.pts);
			
			pkt.stream_index = stream_index;
            
            /* copy packet */
            pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
            pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
            pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
            pkt.pos = -1;
            log_packet(ofmt_ctx, &pkt, "out");
            
            ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
            if (ret < 0) {
                //fprintf(stderr, "Error muxing packet\n");
				YY_ERROR("Error muxing packet\n");
                break;
            }
            av_packet_unref(&pkt);
            //av_free _packet ?
        } else {
            //break;
        }
        
        
    }
    av_write_trailer(ofmt_ctx);
end:
    
    if (ifmt_ctx_a) {
        avformat_close_input(&ifmt_ctx_a);
    }
    if (ifmt_ctx_v) {
        avformat_close_input(&ifmt_ctx_v);
    }
    
    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);
    
    //av_freep(&stream_mapping);
    
    if (ret < 0 && ret != AVERROR_EOF) {
        //fprintf(stderr, "Error occurred: %s\n", av_err2str(ret));
        return -1;
    }
    return 0;
}

int YY_mp4_time_crop(const char * in_filename, int start_milli, int end_milli, const char * out_filename)
{
    AVOutputFormat * ofmt = NULL;
    AVFormatContext * ifmt_ctx = NULL, * ofmt_ctx = NULL;
    AVPacket pkt;
    int ret, i;
    int in_stream_video_index = -1;
    int in_stream_audio_index = -1;
    int out_stream_video_index = -1;
    int out_stream_audio_index = -1;
    int gotkeyframe = 0;
    
    av_register_all();
    
    if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
        YY_ERROR("Could not open input file '%s'", in_filename);
        goto end;
    }
    
    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        YY_ERROR("Failed to retrieve input stream information");
        goto end;
    }
    
    av_dump_format(ifmt_ctx, 0, in_filename, 0);
    
    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
    if (!ofmt_ctx) {
        //fprintf(stderr, "Could not create output context\n");
        YY_ERROR("Could not create output context\n");
        ret = AVERROR_UNKNOWN;
        goto end;
    }
    
    ofmt = ofmt_ctx->oformat;
    
    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx->streams[i];
        
        AVCodecParameters *in_codecpar = in_stream->codecpar;
        
        if (in_stream_video_index == -1 && in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            in_stream_video_index = i;
            out_stream_video_index = i;
        } else if (in_stream_audio_index == -1 && in_codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            in_stream_audio_index = i;
            out_stream_audio_index = i;
        } else {
            continue;
        }
        
        out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (!out_stream) {
            YY_ERROR("Failed allocating output stream\n");
            ret = AVERROR_UNKNOWN;
            goto end;
        }
        
        ret = avcodec_parameters_copy(out_stream->codecpar, in_codecpar);
        if (ret < 0) {
            YY_ERROR("Failed to copy codec parameters\n");
            goto end;
        }
        out_stream->codecpar->codec_tag = 0;
    }
    av_dump_format(ofmt_ctx, 0, out_filename, 1);
    
    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            YY_ERROR("Could not open output file '%s'", out_filename);
            goto end;
        }
    }
    
    ret = avformat_write_header(ofmt_ctx, NULL);
    if (ret < 0) {
        YY_ERROR("Error occurred when opening output file\n");
        goto end;
    }
    
    int frame_number = 0;
    while (1) {
        
        AVStream *in_stream, *out_stream;
        
        ret = av_read_frame(ifmt_ctx, &pkt);
        if (ret < 0)
            break;
        
        frame_number++;
        
        in_stream  = ifmt_ctx->streams[pkt.stream_index];
        if (in_stream_video_index != -1 && out_stream_video_index != -1 && pkt.stream_index == in_stream_video_index) {
            pkt.stream_index = out_stream_video_index;
        } else if (in_stream_audio_index != -1 && out_stream_audio_index != -1 && pkt.stream_index == in_stream_audio_index)
        {
            pkt.stream_index = out_stream_audio_index;
        } else {
            av_packet_unref(&pkt);
            continue;
        }
        log_packet(ifmt_ctx, &pkt, "in");
        int64_t pkt_timestamp = pkt.pts * av_q2d(in_stream->time_base) * 1000;
        int64_t dts_timestamp = pkt.dts * av_q2d(in_stream->time_base) * 1000;
        if( pkt_timestamp < start_milli || pkt_timestamp > end_milli ) {
            //YY_ERROR("dropping packet for timestamp (%lld)\n", pkt_timestamp);
            av_packet_unref(&pkt);
            continue;
        }
        if( pkt.stream_index == out_stream_video_index && (pkt.flags & AV_PKT_FLAG_KEY) == 0 && gotkeyframe == 0 ) {
            //YY_ERROR("dropping packet for keyframe \n");
            av_packet_unref(&pkt);
            continue;
        }
        if( (pkt.flags & AV_PKT_FLAG_KEY) > 0 && pkt.stream_index == out_stream_video_index ) {
            gotkeyframe ++;
        }
        
        out_stream = ofmt_ctx->streams[pkt.stream_index];
        
        int64_t current_pts = 0;
        int64_t current_dts = 0;
        
        if( pkt.stream_index == out_stream_video_index && gotkeyframe == 1 && (pkt.flags & AV_PKT_FLAG_KEY) > 0 ) {
            
            // 第一个I帧强制设置为0
            current_pts = 0;
            current_dts = 0;
        }
        else {
            
            current_pts = pkt_timestamp - start_milli;
            current_dts = dts_timestamp - start_milli;
            if( current_pts < 0 ) {
                YY_ERROR("invalid packet time %d %d \n", (int)pkt_timestamp, start_milli);
                current_pts = 0;
            }
            if( current_dts < 0 ) {
                YY_ERROR("invalid packet time %d %d \n", (int)dts_timestamp, start_milli);
                current_dts = 0;
            }
        }
        
        /* copy packet */
        pkt.pts = (current_pts / av_q2d(out_stream->time_base)) / 1000;
        pkt.dts = (current_dts / av_q2d(out_stream->time_base)) / 1000;
        
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;
        
        log_packet(ofmt_ctx, &pkt, "out");
        
        ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
        if (ret < 0) {
            YY_ERROR("Error muxing packet\n");
            break;
        }
        
        av_packet_unref(&pkt);
    }
    
    av_write_trailer(ofmt_ctx);
    
end:
    
    if (ifmt_ctx) {
        avformat_close_input(&ifmt_ctx);
        ifmt_ctx = NULL;
    }
    
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    
    if( ofmt_ctx ) {
        avformat_free_context(ofmt_ctx);
        ofmt_ctx = NULL;
    }
    
    if (ret < 0 && ret != AVERROR_EOF) {
        YY_ERROR("Error occurred: %s\n", av_err2str(ret));
        return 1;
    }
    
    return 0;
}


static int MyWriteJPEG(AVFrame* pFrame, int width, int height, int iIndex, const char* video_dir, const char* out_file_prefix)
{
    // 输出文件路径
    char out_file[MAX_PATH] = {0};
    sprintf(out_file, "%s%s_%d.jpg", video_dir, out_file_prefix, iIndex);

    // 分配AVFormatContext对象
    AVFormatContext* pFormatCtx = avformat_alloc_context();

    // 设置输出文件格式
    pFormatCtx->oformat = av_guess_format("mjpeg", NULL, NULL);
    // 创建并初始化一个和该url相关的AVIOContext
    if( avio_open(&pFormatCtx->pb, out_file, AVIO_FLAG_READ_WRITE) < 0) {
        YY_ERROR("Couldn't open output file.");
        return -1;
    }

    // 构建一个新stream
    AVStream* pAVStream = avformat_new_stream(pFormatCtx, 0);
    if( pAVStream == NULL ) {
        return -1;
    }

    // 设置该stream的信息
    AVCodecContext* pCodecCtx = pAVStream->codec;

    pCodecCtx->codec_id = pFormatCtx->oformat->video_codec;
    pCodecCtx->codec_type = AVMEDIA_TYPE_VIDEO;
    pCodecCtx->pix_fmt = AV_PIX_FMT_YUVJ420P;
    pCodecCtx->width = width;
    pCodecCtx->height = height;
    pCodecCtx->time_base.num = 1;
    pCodecCtx->time_base.den = 25;
	
	// 不要warning
	avcodec_parameters_from_context(pAVStream->codecpar, pCodecCtx);
	pAVStream->time_base.num = 1;
    pAVStream->time_base.den = 25;

    // Begin Output some information
    av_dump_format(pFormatCtx, 0, out_file, 1);
    // End Output some information

    // 查找解码器
    AVCodec* pCodec = avcodec_find_encoder(pCodecCtx->codec_id);
    if( !pCodec ) {
        YY_ERROR("Codec not found.");
        return -1;
    }
    // 设置pCodecCtx的解码器为pCodec
    if( avcodec_open2(pCodecCtx, pCodec, NULL) < 0 ) {
        YY_ERROR("Could not open codec.");
        return -1;
    }

    //Write Header
    avformat_write_header(pFormatCtx, NULL);

    int y_size = pCodecCtx->width * pCodecCtx->height;

    //Encode
    // 给AVPacket分配足够大的空间
    AVPacket pkt;
    av_new_packet(&pkt, y_size * 3);

    //
    int got_picture = 0;
    int ret = avcodec_encode_video2(pCodecCtx, &pkt, pFrame, &got_picture);
    if( ret < 0 ) {
        printf("Encode Error.\n");
        return -1;
    }
    if( got_picture == 1 ) {
        //pkt.stream_index = pAVStream->index;
        ret = av_write_frame(pFormatCtx, &pkt);
    }

    av_free_packet(&pkt);

    //Write Trailer
    av_write_trailer(pFormatCtx);

    //YY_ERROR("Encode Successful.\n");

    if( pAVStream ) {
        avcodec_close(pAVStream->codec);
    }
    avio_close(pFormatCtx->pb);
    avformat_free_context(pFormatCtx);

    return 0;
}
/*
static int ScaleYUVImage(struct SwsContext* m_pSwsContext, AVCodecContext *pCodecCtx, AVFrame *src_picture, AVFrame *dst_picture, int nDstW, int nDstH)
{
    int nSrcStride[3];
    int nSrcH = pCodecCtx->height;
    int nSrcW = pCodecCtx->width;
    //struct SwsContext* m_pSwsContext;
    //uint8_t *pSrcBuff[3] = {src_picture->data[0],src_picture->data[1], src_picture->data[2]};
    
    nSrcStride[0] = nSrcW ;
    nSrcStride[1] = nSrcW/2 ;
    nSrcStride[2] = nSrcW/2 ;
    
    dst_picture->linesize[0] = nDstW;
    dst_picture->linesize[1] = nDstW / 2;
    dst_picture->linesize[2] = nDstW / 2;
    
    m_pSwsContext = sws_getContext(nSrcW, nSrcH, AV_PIX_FMT_YUV420P,
                                   nDstW, nDstH, AV_PIX_FMT_YUV420P,
                                   SWS_BICUBIC,
                                   NULL, NULL, NULL);
    
    if (NULL == m_pSwsContext)
    {
        printf("ffmpeg get context error!\n");
    }
    
    sws_scale(m_pSwsContext, src_picture->data, src_picture->linesize, 0, pCodecCtx->height, dst_picture->data, dst_picture->linesize);
    
    //printf("line0:%d line1:%d line2:%d\n",dst_picture->linesize[0] ,dst_picture->linesize[1] ,dst_picture->linesize[2]);
    sws_freeContext(m_pSwsContext);
    
    return 1 ;
}
*/
static int MyWriteYUV(AVFrame* pFrame, int width, int height, int iIndex, const char* video_dir, const char* out_file_prefix)
{
    // 输出文件路径
    char out_file[MAX_PATH] = {0};
    sprintf(out_file, "%s%s_%d.yuv", video_dir, out_file_prefix, iIndex, width, height);
    
    FILE * fp = fopen(out_file, "w");
    if( fp != NULL ) {
		fwrite(&width, 1, sizeof(int), fp);
		fwrite(&height, 1, sizeof(int), fp);
        fwrite(pFrame->data[0], 1, pFrame->linesize[0]*height, fp);
        fwrite(pFrame->data[1], 1, pFrame->linesize[1]*height/2, fp);
        fwrite(pFrame->data[2], 1, pFrame->linesize[2]*height/2, fp);
        fclose(fp);
    }
    
    return 0;
}
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

int YY_mp4_video_extract_frame(const char *in_filename, int out_imgformat, const char * out_folder, const char* out_file_prefix, int start_time, int end_time, int *data_timestamp, int data_num, int *out_data_num, float scale)
{

    //AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx = NULL;// *ofmt_ctx = NULL;
    AVPacket pkt;
    AVCodecContext *icodec_ctx;
    AVCodec *iCodec;

    AVFrame *pFrame = NULL;
    // for scale
    AVFrame *pScaled = NULL;
    struct SwsContext* pSwsContext = NULL;

    int frameFinished;

    int ret, i;
	*out_data_num = 0;
	if ((out_imgformat != IMG_FORMAT_JPEG) &&(out_imgformat != IMG_FORMAT_YUV)) {
		return -1;
	}


    av_register_all();		//todo register once?

    if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
        YY_ERROR("Could not open input file '%s'", in_filename);
        goto end;
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        YY_ERROR("Failed to retrieve input stream information");
        goto end;
    }

    //av_dump_format(ifmt_ctx, 0, in_filename, 0);	//todo comment?

    int video_rotate_degree = 0;
    int64_t frame_number = 0;
    int64_t in_stream_duration = 0;

    int in_stream_video_index = -1;

    double pts_ratio = 0;

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {

        AVStream *in_stream = ifmt_ctx->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;

        // only video
        if (in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
        	in_stream_video_index = i;

            frame_number = in_stream->nb_frames;
            in_stream_duration = in_stream->duration;
            pts_ratio = av_q2d(in_stream->time_base);
            //YY_ERROR("duration %ld  %lg  %d\n", in_stream_duration, in_stream_duration * av_q2d(in_stream->time_base), frame_number);

            video_rotate_degree = YY_ffreader_get_rotation(in_stream);
            
            break;
        }
    }

    int64_t starttime_pts = (start_time < 0? 0:start_time) / pts_ratio / 1000;
    int64_t endtime_pts = end_time / pts_ratio / 1000;
    endtime_pts = endtime_pts > in_stream_duration ? in_stream_duration : endtime_pts;

    int64_t sample_interval = (endtime_pts - starttime_pts) / data_num;

    if (in_stream_video_index == -1) {
    	ret = -1;
        YY_ERROR("input file no video stream\n");
        goto end;
    }

    // 寻找解码器
    icodec_ctx = ifmt_ctx->streams[in_stream_video_index]->codec;
    iCodec = avcodec_find_decoder(icodec_ctx->codec_id);
	if( iCodec == NULL ) {
		YY_ERROR ("avcode find decoder failed!\n");
		ret = -1;
		goto end;
	}

	//打开解码器
    if( avcodec_open2(icodec_ctx, iCodec, NULL) < 0 ) {
    	YY_ERROR ("avcode open failed!\n");
		ret = -1;
		goto end;
    }

    //为每帧图像分配内存
    pFrame = av_frame_alloc();
    if( (pFrame == NULL)) {
        YY_ERROR("avcodec alloc frame failed!\n");
		ret = -1;
		goto end;
    }

    frame_number = 0;

	while (1) {
		AVStream *in_stream, *out_stream;
		
		if(frame_number < data_num) {
			int64_t seek_timestamp = starttime_pts + sample_interval * frame_number;

			av_seek_frame(ifmt_ctx,in_stream_video_index, seek_timestamp, AVSEEK_FLAG_BACKWARD);

			ret = av_read_frame(ifmt_ctx, &pkt);
			if (ret < 0) {
				//YY_ERROR("%d %ld %d %ld %lg \n", frame_number, pkt.pts, pkt.size, pkt.pts, pkt.pts * pts_ratio);
				break;
			}

	//		if (pkt.stream_index != in_stream_video_index) {
	//			av_packet_unref(&pkt);
	//			continue;
	//		}

			while (pkt.stream_index != in_stream_video_index || (pkt.flags & AV_PKT_FLAG_DISCARD) > 0 || (pkt.flags & AV_PKT_FLAG_KEY) == 0 ){
				av_packet_unref(&pkt);

				ret = av_read_frame(ifmt_ctx, &pkt);
				if (ret < 0) {
					break;
				} else {
					in_stream  = ifmt_ctx->streams[pkt.stream_index];
				}

			}

			if (ret < 0) {
				//YY_ERROR("%ld %d % ld\n", pkt.pts, pkt.size, pkt.duration);
				//todo  goto end?
				break;
			}

			//avcodec_decode_video2(icodec_ctx, pFrame, &frameFinished, &pkt);
			ret = avcodec_send_packet(icodec_ctx, &pkt);

			frame_number++;
			av_packet_unref(&pkt);
		} else {
			ret = avcodec_send_packet(icodec_ctx, NULL);
		}
		
		if(ret < 0) {
			//printf("hhhhhh add packet null-------------------1\n");
			break;
		}

		while(ret >=0) {
            
            pFrame = av_frame_alloc();
            if( (pFrame == NULL)) {
                YY_ERROR("avcodec alloc frame failed!\n");
                ret = -1;
                goto end;
            }
            
	        ret = avcodec_receive_frame(icodec_ctx, pFrame);
	        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF){
	        	//printf("hhhhhh %d %d %d \n", ret, AVERROR(EAGAIN),AVERROR_EOF);
	            break;
	        }
	        else if (ret < 0) {
	        	//printf("hhhhhh add packet null-------------------2\n");
	        	goto end;
	        }
            
            if( video_rotate_degree == 90 ) {
                AVFrame * frame = pFrame;
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
                pFrame = frame;
            }
            else if( video_rotate_degree == 270 || video_rotate_degree == -90 ) {
                AVFrame * frame = pFrame;
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
                pFrame = frame;
            }
            else if( video_rotate_degree == 180 ) {
                AVFrame * frame = pFrame;
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
                pFrame = frame;
            }
            
            if( scale != 1.0f && scale > 0.0f ) {
                
                int srcWidth = pFrame->width;
                int srcHeight = pFrame->height;
                
                int dstWidth = pFrame->width*scale;
                int dstHeight = pFrame->height*scale;
                
                if( pScaled == NULL ) {
                    pScaled = av_frame_alloc();
                    if (!pScaled){
                        printf("pScaled avcodec_alloc_frame failed\n");
                    }
                    if( av_image_alloc(pScaled->data, pScaled->linesize,
                                       dstWidth, dstHeight, AV_PIX_FMT_YUV420P, 1) < 0 ) {
                        printf("dst_picture avpicture_alloc failed\n");
                    }
                    pScaled->format = AV_PIX_FMT_YUV420P;
                    pScaled->width = dstWidth;
                    pScaled->height = dstHeight;
                }
                if( pSwsContext == NULL ) {
                    
                    pSwsContext = sws_getContext(srcWidth, srcHeight, AV_PIX_FMT_YUV420P,
                                                   dstWidth, dstHeight, AV_PIX_FMT_YUV420P,
                                                   SWS_BICUBIC,
                                                   NULL, NULL, NULL);
                    
                    if (NULL == pSwsContext)
                    {
                        printf("ffmpeg get context error!\n");
                    }
                }
                if( pScaled != NULL && pScaled->data[0] != NULL && pSwsContext != NULL ) {
                    
                    //ScaleYUVImage(icodec_ctx, pFrame, pScaled, icodec_ctx->width*scale, icodec_ctx->height*scale);
                    sws_scale(pSwsContext, pFrame->data, pFrame->linesize, 0, srcHeight, pScaled->data, pScaled->linesize);
                    
                    if(out_imgformat == IMG_FORMAT_JPEG) {
                        MyWriteJPEG(pScaled, dstWidth, dstHeight, *out_data_num, out_folder, out_file_prefix);
                    } else if (out_imgformat == IMG_FORMAT_YUV) {
                        MyWriteYUV(pScaled, dstWidth, dstHeight, *out_data_num, out_folder, out_file_prefix);
                    }
                }
                // error
                else {
                    
                    if(out_imgformat == IMG_FORMAT_JPEG) {
                        MyWriteJPEG(pFrame, pFrame->width, pFrame->height, *out_data_num, out_folder, out_file_prefix);
                    } else if (out_imgformat == IMG_FORMAT_YUV) {
                        MyWriteYUV(pFrame, pFrame->width, pFrame->height, *out_data_num, out_folder, out_file_prefix);
                    }
                }
            }
            else {
                
                if(out_imgformat == IMG_FORMAT_JPEG) {
                    MyWriteJPEG(pFrame, pFrame->width, pFrame->height, *out_data_num, out_folder, out_file_prefix);
                } else if (out_imgformat == IMG_FORMAT_YUV) {
                    MyWriteYUV(pFrame, pFrame->width, pFrame->height, *out_data_num, out_folder, out_file_prefix);
                }
            }
			
			//YY_ERROR("ddddd %d %ld %d %ld %lg \n", frame_number, pkt.pts, pkt.size, pkt.pts, pkt.pts * pts_ratio);
			data_timestamp[*out_data_num] = (int)(pFrame->pts * pts_ratio * 1000);
			(*out_data_num)++;
			
            if( pFrame != NULL ) {
                av_frame_free(&pFrame);
            }
		}
        
		//frame_number++;
		//av_packet_unref(&pkt);
	}

end:
	if(pFrame != NULL) {
		av_free(pFrame);
	}

	if(icodec_ctx != NULL) {
		avcodec_close(icodec_ctx);
	}

	avformat_close_input(&ifmt_ctx);

    if(pSwsContext != NULL) {
        sws_freeContext(pSwsContext);
    }
    if(pScaled != NULL) {
        if(pScaled->data[0]!=NULL){
            av_freep(&pScaled->data[0]);
        }
        av_free(pScaled);
    }
    
	if (ret < 0 && ret != AVERROR_EOF) {
		YY_ERROR("Error occurred: %s\n", av_err2str(ret));
		return -1;
	}
    
	return 0;
}

int mp4_audio_extract_waveform(const char *in_filename, int start_time, int end_time, float* audio_data, int data_num, int *out_data_num)
{

    //AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx = NULL;// *ofmt_ctx = NULL;
    AVPacket pkt;
    AVCodecContext *icodec_ctx;
    AVCodec *iCodec;

    AVFrame *pFrame = NULL;

    int frameFinished;

    int ret, i;

	*out_data_num = 0;

    av_register_all();		//todo register once?

    if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
        YY_ERROR("Could not open input file '%s'", in_filename);
        goto end;
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        YY_ERROR("Failed to retrieve input stream information");
        goto end;
    }

    //av_dump_format(ifmt_ctx, 0, in_filename, 0);	//todo comment?


    int64_t frame_number = 0;
    int64_t in_stream_duration = 0;

    int in_stream_audio_index = -1;

    double pts_ratio = 0;

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {

        AVStream *in_stream = ifmt_ctx->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;

        // only audio
        if (in_codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
        	in_stream_audio_index = i;

            frame_number = in_stream->nb_frames;
            in_stream_duration = in_stream->duration;
            pts_ratio = av_q2d(in_stream->time_base);
            //YY_ERROR("duration %ld  %lg  %d\n", in_stream_duration, in_stream_duration * av_q2d(in_stream->time_base), frame_number);

            break;
        }
    }

    int64_t starttime_pts = (start_time < 0? 0:start_time) / pts_ratio / 1000;
    int64_t endtime_pts = end_time / pts_ratio / 1000;
    endtime_pts = endtime_pts > in_stream_duration ? in_stream_duration : endtime_pts;

    int64_t sample_interval = (endtime_pts - starttime_pts) / data_num;

    if (in_stream_audio_index == -1) {
    	ret = -1;
        YY_ERROR("input file no video stream\n");
        goto end;
    }

    // 寻找解码器
    icodec_ctx = ifmt_ctx->streams[in_stream_audio_index]->codec;
    iCodec = avcodec_find_decoder(icodec_ctx->codec_id);
	if( iCodec == NULL ) {
		YY_ERROR ("avcode find decoder failed!\n");
		ret = -1;
		goto end;
	}

	//打开解码器
    if( avcodec_open2(icodec_ctx, iCodec, NULL) < 0 ) {
    	YY_ERROR ("avcode open failed!\n");
		ret = -1;
		goto end;
    }

    //为每帧图像分配内存
    pFrame = av_frame_alloc();
    if( (pFrame == NULL)) {
        YY_ERROR("avcodec alloc frame failed!\n");
		ret = -1;
		goto end;
    }

    frame_number = 0;

	while (frame_number < data_num) {
		AVStream *in_stream, *out_stream;

		int64_t seek_timestamp = starttime_pts + sample_interval * frame_number;

		av_seek_frame(ifmt_ctx,in_stream_audio_index, seek_timestamp, AVSEEK_FLAG_BACKWARD);

		ret = av_read_frame(ifmt_ctx, &pkt);
		if (ret < 0) {
			//YY_ERROR("%d %ld %d %ld %lg \n", frame_number, pkt.pts, pkt.size, pkt.pts, pkt.pts * pts_ratio);
			break;
		}

//		if (pkt.stream_index != in_stream_video_index) {
//			av_packet_unref(&pkt);
//			continue;
//		}

        while (pkt.stream_index != in_stream_audio_index ){
        	av_packet_unref(&pkt);

        	ret = av_read_frame(ifmt_ctx, &pkt);
        	if (ret < 0) {
        		break;
        	} else {
        		in_stream  = ifmt_ctx->streams[pkt.stream_index];
        	}

        }

        if (ret < 0) {
        	YY_ERROR("%ld %d % ld\n", pkt.pts, pkt.size, pkt.duration);
        	//todo  goto end?
            break;
        }



		avcodec_decode_audio4(icodec_ctx, pFrame, &frameFinished, &pkt);

		if( frameFinished ) {

			//todo
			if(pFrame->format == AV_SAMPLE_FMT_S16P) {
				audio_data[*out_data_num] = ((short*)(pFrame->data[0]))[0];
			} else if  (pFrame->format == AV_SAMPLE_FMT_FLTP) {
				audio_data[*out_data_num] = ((float*)(pFrame->data[0]))[0];
			} else {
				//YY_ERROR("ddddd-----");
				audio_data[*out_data_num] = 0;
			}
			
			YY_ERROR("## %d %lld %d %lld %lg %g %d \n", frame_number, pkt.pts, pkt.size, pkt.pts, pkt.pts * pts_ratio, audio_data[*out_data_num],  pFrame->format);
			
			(*out_data_num)++;

			


			
			//MyWriteJPEG(pFrame, icodec_ctx->width, icodec_ctx->height, frame_number, out_folder);
		}
		frame_number++;
		//YY_ERROR("##ddddd %d %lld %d %lld %lg %g %d \n", frame_number, pkt.pts, pkt.size, pkt.pts, pkt.pts * pts_ratio, audio_data[frame_number],  pFrame->format);


		av_packet_unref(&pkt);
	}

	//*out_data_num = frame_number;
	float abs_max_value = 0;
	for(int i = 0; i < *out_data_num; ++i) {
		if (fabs(audio_data[i]) > abs_max_value) {
			abs_max_value = fabs(audio_data[i]);
		}
	}

	if (abs_max_value != 0 ) {
		for(int i = 0; i < *out_data_num; ++i) {
			audio_data[i] = audio_data[i] / abs_max_value * 100;
		}
	}

end:
	if(pFrame != NULL) {
		av_free(pFrame);
	}


	if(icodec_ctx != NULL) {
		avcodec_close(icodec_ctx);
	}


	avformat_close_input(&ifmt_ctx);



	if (ret < 0 && ret != AVERROR_EOF) {
		YY_ERROR("Error occurred: %s\n", av_err2str(ret));
		return -1;
	}
	return 0;
}


typedef struct StreamContext {
    AVCodecContext *dec_ctx;
    AVCodecContext *enc_ctx;
    int out_stream_index;
} StreamContext;

// fixed point to double
#define CONV_FP(x) ((double) (x)) / (1 << 16)

// double to fixed point
#define CONV_DB(x) (int32_t) ((x) * (1 << 16))

static double av_display_rotation_get(const int32_t matrix[9])
{
    double rotation, scale[2];

    scale[0] = hypot(CONV_FP(matrix[0]), CONV_FP(matrix[3]));
    scale[1] = hypot(CONV_FP(matrix[1]), CONV_FP(matrix[4]));

    if (scale[0] == 0.0 || scale[1] == 0.0)
        return NAN;

    rotation = atan2(CONV_FP(matrix[1]) / scale[1],
                     CONV_FP(matrix[0]) / scale[0]) * 180 / M_PI;

    return -rotation;
}

static double get_rotation(AVStream *st)
{
    uint8_t* displaymatrix = av_stream_get_side_data(st,
                                                     AV_PKT_DATA_DISPLAYMATRIX, NULL);
    double theta = 0;
    if (displaymatrix)
        theta = -av_display_rotation_get((int32_t*) displaymatrix);

    theta -= 360*floor(theta/360 + 0.9/360);

    if (fabs(theta - 90*round(theta/90)) > 2)
        YY_ERROR("Odd rotation angle.\n");

    return theta;
}

static int open_input_file(const char *filename, AVFormatContext **ifmt_ctx_param, StreamContext **stream_ctx_param, int *video_stream_index, double *video_rotate)
{
    int ret;
    unsigned int i;

    AVFormatContext *ifmt_ctx; //= *ifmt_ctx_param;
    StreamContext *stream_ctx; //= *stream_ctx_param;
	
	*video_stream_index = -1;
    *video_rotate = 0;


    ifmt_ctx = NULL;
    if ((ret = avformat_open_input(&ifmt_ctx, filename, NULL, NULL)) < 0) {
        //av_log(NULL, AV_LOG_ERROR, "Cannot open input file\n");
		YY_ERROR("Cannot open input file '%s'", filename);
        return ret;
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx, NULL)) < 0) {
        //av_log(NULL, AV_LOG_ERROR, "Cannot find stream information\n");
		YY_ERROR("Cannot find stream information");
        return ret;
    }

    stream_ctx = av_mallocz_array(ifmt_ctx->nb_streams, sizeof(*stream_ctx));
    if (!stream_ctx)
        return AVERROR(ENOMEM);

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *stream = ifmt_ctx->streams[i];
        AVCodec *dec = avcodec_find_decoder(stream->codecpar->codec_id);
        if( stream->codecpar->codec_id == 0 ) {
            stream_ctx[i].dec_ctx = NULL;
            stream_ctx[i].enc_ctx = NULL;
            continue;
        }
        AVCodecContext *codec_ctx;
        if (!dec) {
            //av_log(NULL, AV_LOG_ERROR, "Failed to find decoder for stream #%u\n", i);
			YY_ERROR("Failed to find decoder for stream #%u\n", i);
            return AVERROR_DECODER_NOT_FOUND;
        }
        codec_ctx = avcodec_alloc_context3(dec);
        if (!codec_ctx) {
            YY_ERROR("Failed to allocate the decoder context for stream #%u\n", i);
            return AVERROR(ENOMEM);
        }
        ret = avcodec_parameters_to_context(codec_ctx, stream->codecpar);
        if (ret < 0) {
            YY_ERROR("Failed to copy decoder parameters to input decoder context "
                   "for stream #%u\n", i);
            return ret;
        }
        /* Reencode video & audio and remux subtitles etc. */
        if (codec_ctx->codec_type == AVMEDIA_TYPE_VIDEO
                || codec_ctx->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (codec_ctx->codec_type == AVMEDIA_TYPE_VIDEO)
                codec_ctx->framerate = av_guess_frame_rate(ifmt_ctx, stream, NULL);
			
			if(codec_ctx->codec_type == AVMEDIA_TYPE_VIDEO){
            	double angle = get_rotation(stream);
            	//printf("%g", angle);
            	*video_stream_index = i;
            	*video_rotate = angle;
            }
            /* Open decoder */
            ret = avcodec_open2(codec_ctx, dec, NULL);
            if (ret < 0) {
                YY_ERROR("Failed to open decoder for stream #%u\n", i);
                return ret;
            }
        }
        stream_ctx[i].dec_ctx = codec_ctx;
    }
	
	if(*video_stream_index < 0) {
		return -1;
	}

    //av_dump_format(ifmt_ctx, 0, filename, 0);

    *ifmt_ctx_param = ifmt_ctx;
    *stream_ctx_param = stream_ctx;
    return 0;
}

static int open_output_file(const char *filename, AVFormatContext *ifmt_ctx, StreamContext *stream_ctx,AVFormatContext **ofmt_ctx_param, int gop_size, int width, int height, int transpose_flag)
{
    AVStream *out_stream;
    AVStream *in_stream;
    AVCodecContext *dec_ctx, *enc_ctx;
    AVCodec *encoder;
    int ret, out_st_cnt;
    unsigned int i;

    AVDictionary *opt = NULL;

    AVFormatContext *ofmt_ctx;// = *ofmt_ctx_param;

    ofmt_ctx = NULL;
    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, filename);
    if (!ofmt_ctx) {
        YY_ERROR("Could not create output context\n");
        return AVERROR_UNKNOWN;
    }

    out_st_cnt = 0;
    for (i = 0; i < ifmt_ctx->nb_streams; i++) {

        in_stream = ifmt_ctx->streams[i];
        dec_ctx = stream_ctx[i].dec_ctx;
        if( dec_ctx == NULL ) {
            stream_ctx[i].enc_ctx = NULL;
            continue;
        }
        
        out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (!out_stream) {
            YY_ERROR("Failed allocating output stream\n");
            return AVERROR_UNKNOWN;
        }
        // 记录输出的流ID
        stream_ctx[i].out_stream_index = out_st_cnt;
        out_st_cnt ++;

        // only video need to transcode?
//        if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO
//                || dec_ctx->codec_type == AVMEDIA_TYPE_AUDIO) {
        if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO) {
            /* in this example, we choose transcoding to same codec */
            encoder = avcodec_find_encoder(dec_ctx->codec_id);
            if (!encoder) {
                YY_ERROR("Necessary encoder not found\n");
                return AVERROR_INVALIDDATA;
            }
            enc_ctx = avcodec_alloc_context3(encoder);
            if (!enc_ctx) {
                YY_ERROR("Failed to allocate the encoder context\n");
                return AVERROR(ENOMEM);
            }

            /* In this example, we transcode to same properties (picture size,
             * sample rate etc.). These properties can be changed for output
             * streams easily using filters */
            if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO) {
            	if(width == 0) {
                    enc_ctx->height = dec_ctx->height;
                    enc_ctx->width = dec_ctx->width;
            	} else {
                    enc_ctx->height = height;//1024;//dec_ctx->width;
                    enc_ctx->width = width;//576;//dec_ctx->height;
            	}
                enc_ctx->sample_aspect_ratio = dec_ctx->sample_aspect_ratio;
                /* take first format from list of supported formats */
                if (encoder->pix_fmts)
                    enc_ctx->pix_fmt = encoder->pix_fmts[0];
                else
                    enc_ctx->pix_fmt = dec_ctx->pix_fmt;
                /* video time_base can be set to whatever is handy and supported by encoder */
                //enc_ctx->time_base = av_inv_q(dec_ctx->framerate);
                enc_ctx->time_base = (AVRational){1, 90000};

                enc_ctx->max_b_frames = 3;//0;
				enc_ctx->refs = 5;
                enc_ctx->gop_size = gop_size;//25;
                enc_ctx->framerate = (AVRational){40,1};
				


                if(ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
                    enc_ctx->flags |= CODEC_FLAG_GLOBAL_HEADER;

				if(gop_size == 1) {
					av_dict_set(&opt, "preset", "ultrafast", 0);
					av_dict_set(&opt, "qp", "15", 0);
					//av_dict_set(&opt, "crf", "24.0", 0);
					av_dict_set(&opt, "profile", "baseline", 0);
					av_dict_set(&opt, "level", "5.2", 0);					
				} else {
					av_dict_set(&opt, "preset", "slow", 0);
					av_dict_set(&opt, "crf", "24.0", 0);
					av_dict_set(&opt, "profile", "high", 0);
					av_dict_set(&opt, "level", "3.1", 0);
				}
            	
            	//av_dict_set(&opt, "tune", "zerolatency", 0);


            } else {
                enc_ctx->sample_rate = dec_ctx->sample_rate;
                enc_ctx->channel_layout = dec_ctx->channel_layout;
                enc_ctx->channels = av_get_channel_layout_nb_channels(enc_ctx->channel_layout);
                /* take first format from list of supported formats */
                enc_ctx->sample_fmt = encoder->sample_fmts[0];
                enc_ctx->time_base = (AVRational){1, enc_ctx->sample_rate};
            }

            /* Third parameter can be used to pass settings to encoder */
            //ret = avcodec_open2(enc_ctx, encoder, NULL);
            ret = avcodec_open2(enc_ctx, encoder, &opt);
            if (ret < 0) {
                YY_ERROR("Cannot open video encoder for stream #%u\n", i);
                return ret;
            }
            ret = avcodec_parameters_from_context(out_stream->codecpar, enc_ctx);
            if (ret < 0) {
                YY_ERROR("Failed to copy encoder parameters to output stream #%u\n", i);
                return ret;
            }
            if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
                enc_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

            out_stream->time_base = enc_ctx->time_base;
            stream_ctx[i].enc_ctx = enc_ctx;
        } else if (dec_ctx->codec_type == AVMEDIA_TYPE_UNKNOWN) {
            YY_ERROR("Elementary stream #%d is of unknown type, cannot proceed\n", i);
            return AVERROR_INVALIDDATA;
        } else {
            /* if this stream must be remuxed */
            ret = avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
            if (ret < 0) {
                YY_ERROR("Copying parameters for stream #%u failed\n", i);
                return ret;
            }
            out_stream->time_base = in_stream->time_base;
        }

    }
    av_dump_format(ofmt_ctx, 0, filename, 1);

    if (!(ofmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            YY_ERROR("Could not open output file '%s'", filename);
            return ret;
        }
    }

    /* init muxer, write output file header */
    ret = avformat_write_header(ofmt_ctx, NULL);
    if (ret < 0) {
        YY_ERROR("Error occurred when opening output file\n");
        return ret;
    }

    *ofmt_ctx_param = ofmt_ctx;
    return 0;
}

static int init_filters(const char *filters_descr,AVFormatContext *ifmt_ctx, StreamContext *stream_ctx,int video_stream_index,
		AVFilterContext **buffersink_ctx_param, AVFilterContext **buffersrc_ctx_param, AVFilterGraph **filter_graph_param)
{

	AVFilterContext *buffersink_ctx;
	AVFilterContext *buffersrc_ctx;
	AVFilterGraph *filter_graph;

    char args[512];
    int ret = 0;
    const AVFilter *buffersrc  = avfilter_get_by_name("buffer");
    const AVFilter *buffersink = avfilter_get_by_name("buffersink");
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs  = avfilter_inout_alloc();
    AVRational time_base = ifmt_ctx->streams[video_stream_index]->time_base;
    enum AVPixelFormat pix_fmts[] = { AV_PIX_FMT_YUV420P, AV_PIX_FMT_NONE };

    filter_graph = avfilter_graph_alloc();
    if (!outputs || !inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    AVCodecContext *dec_ctx = stream_ctx[video_stream_index].dec_ctx;
    /* buffer video source: the decoded frames from the decoder will be inserted here. */
    snprintf(args, sizeof(args),
            "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
            dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
            time_base.num, time_base.den,
            dec_ctx->sample_aspect_ratio.num, dec_ctx->sample_aspect_ratio.den);

    ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in",
                                       args, NULL, filter_graph);
    if (ret < 0) {
        YY_ERROR("Cannot create buffer source\n");
        goto end;
    }

    /* buffer video sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                       NULL, NULL, filter_graph);
    if (ret < 0) {
        YY_ERROR("Cannot create buffer sink\n");
        goto end;
    }

    ret = av_opt_set_int_list(buffersink_ctx, "pix_fmts", pix_fmts,
                              AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        YY_ERROR("Cannot set output pixel format\n");
        goto end;
    }

    /*
     * Set the endpoints for the filter graph. The filter_graph will
     * be linked to the graph described by filters_descr.
     */

    /*
     * The buffer source output must be connected to the input pad of
     * the first filter described by filters_descr; since the first
     * filter input label is not specified, it is set to "in" by
     * default.
     */
    outputs->name       = av_strdup("in");
    outputs->filter_ctx = buffersrc_ctx;
    outputs->pad_idx    = 0;
    outputs->next       = NULL;

    /*
     * The buffer sink input must be connected to the output pad of
     * the last filter described by filters_descr; since the last
     * filter output label is not specified, it is set to "out" by
     * default.
     */
    inputs->name       = av_strdup("out");
    inputs->filter_ctx = buffersink_ctx;
    inputs->pad_idx    = 0;
    inputs->next       = NULL;

    if ((ret = avfilter_graph_parse_ptr(filter_graph, filters_descr,
                                    &inputs, &outputs, NULL)) < 0)
        goto end;

    if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
        goto end;

end:
    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);

    *buffersink_ctx_param = buffersink_ctx;
    *buffersrc_ctx_param = buffersrc_ctx;
    *filter_graph_param = filter_graph;


    return ret;
}

int mp4_transcode_by_x264(const char *in_filename, const char *out_filename, int gop_size, OnProgress onProgress, void* target)
{

	int ret;
	int i;
	static AVFormatContext *ifmt_ctx;
	static AVFormatContext *ofmt_ctx;
	static StreamContext *stream_ctx;

	AVPacket packet = { .data = NULL, .size = 0 };
    AVFrame *frame = NULL;
    enum AVMediaType type;
	unsigned int stream_index;
	unsigned int video_stream_index = -1;

	int got_frame;

    av_register_all();
	
	int in_video_stream_index = -1;
    double video_rotate = 0;

    if ((ret = open_input_file(in_filename, &ifmt_ctx, &stream_ctx, &in_video_stream_index, &video_rotate)) < 0)
        goto end;
    if ((ret = open_output_file(out_filename, ifmt_ctx, stream_ctx,&ofmt_ctx, gop_size,0 ,0, 0)) < 0)
        goto end;
	
    int64_t last_frame_pts = -1;
    
    /* read all packets */
    while (1) {
        if ((ret = av_read_frame(ifmt_ctx, &packet)) < 0){
			ret = 0; //正常情况
			break;
		}
            
        stream_index = packet.stream_index;
        type = ifmt_ctx->streams[packet.stream_index]->codecpar->codec_type;
        //YY_ERROR("Demuxer gave frame of stream_index %u\n", stream_index);

        if(type==AVMEDIA_TYPE_VIDEO) {
			
			video_stream_index = stream_index;
			
            //YY_ERROR("Going to reencode&filter the frame\n");
            frame = av_frame_alloc();
            if (!frame) {
                ret = AVERROR(ENOMEM);
                //break;
				goto end;
            }
//            av_packet_rescale_ts(&packet,
//                                 ifmt_ctx->streams[stream_index]->time_base,
//                                 stream_ctx[stream_index].dec_ctx->time_base);

			int64_t in_stream_duration =ifmt_ctx->streams[packet.stream_index]->duration;
			int progress = packet.pts*100/in_stream_duration;
			if(progress>100) {
				progress = 99;
			}
			int cancel = onProgress(target, progress);
			if(cancel !=0) {
				ret = -9999;
				goto end;
			}

            av_packet_rescale_ts(&packet,
                                 ifmt_ctx->streams[stream_index]->time_base,
								 ofmt_ctx->streams[stream_ctx[stream_index].out_stream_index]->time_base);

            ret = avcodec_decode_video2(stream_ctx[stream_index].dec_ctx, frame,
                    &got_frame, &packet);
            if (ret < 0) {
                av_frame_free(&frame);
                YY_ERROR("Decoding failed\n");
                break;
            }

            if (got_frame) {
                int got_picture = 0;
                AVPacket enc_pkt;
                enc_pkt.data = NULL;
                enc_pkt.size = 0;

                av_init_packet(&enc_pkt);
				// 不受输入流的gop影响（输入流是全关键帧输出也变成全关键帧）
				frame->key_frame = 0;
                frame->pict_type = AV_PICTURE_TYPE_NONE;
				
				if(stream_ctx[stream_index].enc_ctx == NULL) {
					YY_ERROR("Error encode context is null %d\n", stream_index);
					return -1;
				}
				
				int ret = avcodec_encode_video2(stream_ctx[stream_index].enc_ctx, &enc_pkt, frame, &got_picture);
				if( ret < 0 ) {
					//printf("Encode Error.\n");
					YY_ERROR("Encode finish\n");
					av_frame_free(&frame);
					return -1;
				}
				if( got_picture == 1 ) {
					//enc_pkt.stream_index = stream_index;
					enc_pkt.stream_index = stream_ctx[stream_index].out_stream_index;
					//pkt.stream_index = pAVStream->index;
					ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
					//av_frame_free(&frame);
				}
				
				av_frame_free(&frame);
                
                // // 并非第一帧
                // if( last_frame_pts >= 0 ) {
                    // int64_t original_frame_pts = frame->pts;
                    // int64_t test_last_frame_pts = last_frame_pts;
                    // while( last_frame_pts < original_frame_pts ) {
                        
                        // test_last_frame_pts = last_frame_pts + (40 / av_q2d(ofmt_ctx->streams[stream_ctx[stream_index].out_stream_index]->time_base)) / 1000;
                        // if( test_last_frame_pts > original_frame_pts )
                            // break;
                        
                        // frame->pts = test_last_frame_pts;
                        // last_frame_pts = test_last_frame_pts;
                        
                        // int ret2 = avcodec_encode_video2(stream_ctx[stream_index].enc_ctx, &enc_pkt, frame, &got_picture);
                        // if( ret2 < 0 ) {
                            // //printf("Encode Error.\n");
                            // YY_ERROR("Encode finish\n");
                            // av_frame_free(&frame);
                            // return -1;
                        // }
                        // if( got_picture == 1 ) {
                            // //enc_pkt.stream_index = stream_index;
                            // enc_pkt.stream_index = stream_ctx[stream_index].out_stream_index;
                            // //pkt.stream_index = pAVStream->index;
                            // ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
                        // }
                    // }
                    // av_frame_free(&frame);
                // }
                // else {
                    // // 第一帧，原始处理
                    // frame->pts = 0;
                    // last_frame_pts = 0;
                    
                    // int ret = avcodec_encode_video2(stream_ctx[stream_index].enc_ctx, &enc_pkt, frame, &got_picture);
                    // if( ret < 0 ) {
                        // //printf("Encode Error.\n");
                        // YY_ERROR("Encode finish\n");
                        // av_frame_free(&frame);
                        // return -1;
                    // }
                    // if( got_picture == 1 ) {
                        // //enc_pkt.stream_index = stream_index;
                        // enc_pkt.stream_index = stream_ctx[stream_index].out_stream_index;
                        // //pkt.stream_index = pAVStream->index;
                        // ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
                        // av_frame_free(&frame);
                    // }
                // }

            } else {
                av_frame_free(&frame);
            }

        } else if(type==AVMEDIA_TYPE_AUDIO ) {
            /* remux this frame without reencoding */
            av_packet_rescale_ts(&packet,
                                 ifmt_ctx->streams[stream_index]->time_base,
                                 ofmt_ctx->streams[stream_ctx[stream_index].out_stream_index]->time_base);

            packet.stream_index = stream_ctx[stream_index].out_stream_index;
            ret = av_interleaved_write_frame(ofmt_ctx, &packet);
            if (ret < 0) {
                YY_ERROR("write audio failed\n");
                goto end;
            }
        }
        av_packet_unref(&packet);
    }

	if(video_stream_index >=0)
	{
    	// flush encoder;
    	while(1) {
            int got_picture = 0;
            AVPacket enc_pkt;
            enc_pkt.data = NULL;
            enc_pkt.size = 0;

            av_init_packet(&enc_pkt);
			if(stream_ctx[video_stream_index].enc_ctx == NULL) {
				YY_ERROR("Error encode context is null %d\n", video_stream_index);
				break;
			}
            int ret = avcodec_encode_video2(stream_ctx[video_stream_index].enc_ctx, &enc_pkt, NULL, &got_picture);
            if( ret < 0 ) {
				ret = 0;
                YY_ERROR("flush finish 1\n");
                break;
            }
            if( got_picture == 1 ) {
            	//enc_pkt.stream_index = video_stream_index;
                enc_pkt.stream_index = stream_ctx[video_stream_index].out_stream_index;
                //pkt.stream_index = pAVStream->index;
            	ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
            } else {
				YY_ERROR("flush finish 2\n");
            	break;
            }
    	}
    }
    av_write_trailer(ofmt_ctx);
end:
    av_packet_unref(&packet);
    av_frame_free(&frame);
	if(ifmt_ctx != NULL) {
		for (i = 0; i < ifmt_ctx->nb_streams; i++) {
            if( stream_ctx[i].dec_ctx == NULL )
                continue;
			avcodec_free_context(&stream_ctx[i].dec_ctx);
			if (ofmt_ctx )
				avcodec_free_context(&stream_ctx[i].enc_ctx);

		}
		av_free(stream_ctx);
		avformat_close_input(&ifmt_ctx);
		if (ofmt_ctx && !(ofmt_ctx->oformat->flags & AVFMT_NOFILE))
			avio_closep(&ofmt_ctx->pb);
		avformat_free_context(ofmt_ctx);		
	}


    //if (ret < 0)
    //    av_log(NULL, AV_LOG_ERROR, "Error occurred: %s\n", av_err2str(ret));

    return ret ? 1 : 0;
}

int mp4_import_video_by_x264(const char *in_filename, const char *out_filename, int gop_size, OnProgress onProgress, void* target, int width, int height)
{

	int ret;
	int i;
	static AVFormatContext *ifmt_ctx;
	static AVFormatContext *ofmt_ctx;
	static StreamContext *stream_ctx;

	AVFilterContext *buffersink_ctx;
	AVFilterContext *buffersrc_ctx;
	AVFilterGraph *filter_graph;

	AVPacket packet = { .data = NULL, .size = 0 };
    AVFrame *frame = av_frame_alloc();
    AVFrame *filt_frame = av_frame_alloc();
    enum AVMediaType type;
	unsigned int stream_index;
	unsigned int video_stream_index;

	int got_frame;

    av_register_all();
    avfilter_register_all();

    int in_video_stream_index = -1;
    double video_rotate = 0;

    if ((ret = open_input_file(in_filename, &ifmt_ctx, &stream_ctx, &in_video_stream_index, &video_rotate)) < 0)
        goto end;
	
	AVCodecContext *dec_ctx = stream_ctx[video_stream_index].dec_ctx;
	int in_width = dec_ctx->width;
	int in_height = dec_ctx->height;
	
	int out_width = width;//576;
	int out_height = height;//1024;
	
	char desc[512];
	int transpose_flag = 0;
    if(fabs(video_rotate - 90) < 1.0) {
		transpose_flag = 1;
		if(in_width < in_height) {		//landscape
			int ww = out_width;
			out_width = out_height;
			out_height = ww;
		}
		//out_width = in_height;
		//out_height = in_width;
		if((in_width == out_height) && (in_height == out_width)) {
			snprintf(desc, sizeof(desc),"transpose=clock");
		} else {
			snprintf(desc, sizeof(desc),"transpose=clock,scale=%d:%d",out_width, out_height);
		}
        //snprintf(desc, sizeof(desc),"transpose=clock,scale=%d:%d",width, height);
		//snprintf(desc, sizeof(desc),"transpose=clock");
    } else if (fabs(video_rotate - 180) < 1.0) {
		if(in_width > in_height) {		//landscape
			int ww = out_width;
			out_width = out_height;
			out_height = ww;
		}
		
		if((in_width == out_width) && (in_height == out_height)) {
			snprintf(desc, sizeof(desc),"hflip,vflip");
		} else {
			snprintf(desc, sizeof(desc),"hflip,vflip,scale=%d:%d",out_width, out_height);
		}
		
        //snprintf(desc, sizeof(desc),"hflip,vflip,scale=%d:%d",width, height);
		//snprintf(desc, sizeof(desc),"hflip,vflip");
    } else if (fabs(video_rotate - 270) < 1.0) {
		transpose_flag = 1;
		if(in_width < in_height) {		//landscape
			int ww = out_width;
			out_width = out_height;
			out_height = ww;
		}
		if((in_width == out_height) && (in_height == out_width)) {
			snprintf(desc, sizeof(desc),"transpose=cclock");
		} else {
			snprintf(desc, sizeof(desc),"transpose=cclock,scale=%d:%d",out_width, out_height);
		}
        //snprintf(desc, sizeof(desc),"transpose=cclock,scale=%d:%d",width, height);
		//snprintf(desc, sizeof(desc),"transpose=cclock");
    } else{
		if(in_width > in_height) {		//landscape
			int ww = out_width;
			out_width = out_height;
			out_height = ww;
		}
		
		if((in_width == out_width) && (in_height == out_height)) {
			snprintf(desc, sizeof(desc),"copy");
		} else {
			snprintf(desc, sizeof(desc),"scale=%d:%d",out_width, out_height);
		}
        //snprintf(desc, sizeof(desc),"scale=%d:%d",width, height);
		//snprintf(desc, sizeof(desc),"copy");
    }
	
	//YY_ERROR("progress %s \n", desc);
	
    if ((ret = open_output_file(out_filename, ifmt_ctx, stream_ctx,&ofmt_ctx, gop_size, out_width, out_height, transpose_flag)) < 0)
        goto end;



    // todo
    if ((ret =init_filters(desc, ifmt_ctx, stream_ctx, in_video_stream_index,
    		&buffersink_ctx, &buffersrc_ctx, &filter_graph)) < 0 )
    	goto end;

    int64_t last_frame_pts = -1;

    /* read all packets */
    while (1) {
        if ((ret = av_read_frame(ifmt_ctx, &packet)) < 0){
			ret = 0; //正常情况
			break;
		}

        stream_index = packet.stream_index;
        type = ifmt_ctx->streams[packet.stream_index]->codecpar->codec_type;
        //YY_ERROR("Demuxer gave frame of stream_index %u\n", stream_index);

        if(type==AVMEDIA_TYPE_VIDEO) {

			video_stream_index = stream_index;

            //YY_ERROR("Going to reencode&filter the frame\n");
//            frame = av_frame_alloc();
//            if (!frame) {
//                ret = AVERROR(ENOMEM);
//                //break;
//				goto end;
//            }
//            av_packet_rescale_ts(&packet,
//                                 ifmt_ctx->streams[stream_index]->time_base,
//                                 stream_ctx[stream_index].dec_ctx->time_base);

			int64_t in_stream_duration =ifmt_ctx->streams[packet.stream_index]->duration;
			int progress = packet.pts*100/in_stream_duration;
			if(progress>100) {
				progress = 99;
			}
			if(onProgress != NULL) {
				int cancel = onProgress(target, progress);
				if(cancel !=0) {
					ret = -9999;
					goto end;
				}
			}


            av_packet_rescale_ts(&packet,
                                 ifmt_ctx->streams[stream_index]->time_base,
								 ofmt_ctx->streams[stream_ctx[stream_index].out_stream_index]->time_base);

            ret = avcodec_decode_video2(stream_ctx[stream_index].dec_ctx, frame,
                    &got_frame, &packet);
            if (ret < 0) {
            	av_frame_unref(frame);
                YY_ERROR("Decoding failed\n");
                break;
            }

            if (got_frame) {

                /* push the decoded frame into the filtergraph */
                if (av_buffersrc_add_frame_flags(buffersrc_ctx, frame, AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {
                    YY_ERROR("Error while feeding the filtergraph\n");
                    break;
                }

                /* pull filtered frames from the filtergraph */
                while (1) {
                    ret = av_buffersink_get_frame(buffersink_ctx, filt_frame);
                    if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
                        break;
                    if (ret < 0)
                        goto end;
                    //display_frame(filt_frame, buffersink_ctx->inputs[0]->time_base);

                    int got_picture = 0;
                    AVPacket enc_pkt;
                    enc_pkt.data = NULL;
                    enc_pkt.size = 0;

                    av_init_packet(&enc_pkt);
    				// 不受输入流的gop影响（输入流是全关键帧输出也变成全关键帧）
                    filt_frame->key_frame = 0;
                    filt_frame->pict_type = AV_PICTURE_TYPE_NONE;
					
					if(stream_ctx[stream_index].enc_ctx == NULL) {
						YY_ERROR("encode context is null %d\n", stream_index);
						return -1;
					}
					
					int ret = avcodec_encode_video2(stream_ctx[stream_index].enc_ctx, &enc_pkt, filt_frame, &got_picture);
					if( ret < 0 ) {
						//printf("Encode Error.\n");
						YY_ERROR("Encode finish\n");
						//av_frame_free(&frame);
						return -1;
					}
					if( got_picture == 1 ) {
						//enc_pkt.stream_index = stream_index;
						enc_pkt.stream_index = stream_ctx[stream_index].out_stream_index;
						//pkt.stream_index = pAVStream->index;
						ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
						//av_frame_unref(frame);
					}

                    // // 并非第一帧
                    // if( last_frame_pts >= 0 ) {
                        // int64_t original_frame_pts = filt_frame->pts;
                        // int64_t test_last_frame_pts = last_frame_pts;
                        // while( last_frame_pts < original_frame_pts ) {

                            // test_last_frame_pts = last_frame_pts + (40 / av_q2d(ofmt_ctx->streams[stream_ctx[stream_index].out_stream_index]->time_base)) / 1000;
                            // if( test_last_frame_pts > original_frame_pts )
                                // break;

                            // filt_frame->pts = test_last_frame_pts;
                            // last_frame_pts = test_last_frame_pts;

                            // int ret2 = avcodec_encode_video2(stream_ctx[stream_index].enc_ctx, &enc_pkt, filt_frame, &got_picture);
                            // if( ret2 < 0 ) {
                                // //printf("Encode Error.\n");
                                // YY_ERROR("Encode finish\n");
                                // //av_frame_free(&frame);
                                // return -1;
                            // }
                            // if( got_picture == 1 ) {
                                // //enc_pkt.stream_index = stream_index;
                                // enc_pkt.stream_index = stream_ctx[stream_index].out_stream_index;
                                // //pkt.stream_index = pAVStream->index;
                                // ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
                            // }
                        // }
                        // av_frame_unref(frame);
                    // }
                    // else {
                        // // 第一帧，原始处理
                    	// filt_frame->pts = 0;
                        // last_frame_pts = 0;

                        // int ret = avcodec_encode_video2(stream_ctx[stream_index].enc_ctx, &enc_pkt, filt_frame, &got_picture);
                        // if( ret < 0 ) {
                            // //printf("Encode Error.\n");
                            // YY_ERROR("Encode finish\n");
                            // //av_frame_free(&frame);
                            // return -1;
                        // }
                        // if( got_picture == 1 ) {
                            // //enc_pkt.stream_index = stream_index;
                            // enc_pkt.stream_index = stream_ctx[stream_index].out_stream_index;
                            // //pkt.stream_index = pAVStream->index;
                            // ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
                            // //av_frame_unref(frame);
                        // }
                    // }
                    av_frame_unref(filt_frame);
                }

                av_frame_unref(frame);

//                int got_picture = 0;
//                AVPacket enc_pkt;
//                enc_pkt.data = NULL;
//                enc_pkt.size = 0;
//
//                av_init_packet(&enc_pkt);
//				// 不受输入流的gop影响（输入流是全关键帧输出也变成全关键帧）
//				frame->key_frame = 0;
//                frame->pict_type = AV_PICTURE_TYPE_NONE;
//
//                // 并非第一帧
//                if( last_frame_pts >= 0 ) {
//                    int64_t original_frame_pts = frame->pts;
//                    int64_t test_last_frame_pts = last_frame_pts;
//                    while( last_frame_pts < original_frame_pts ) {
//
//                        test_last_frame_pts = last_frame_pts + (40 / av_q2d(ofmt_ctx->streams[stream_ctx[stream_index].out_stream_index]->time_base)) / 1000;
//                        if( test_last_frame_pts > original_frame_pts )
//                            break;
//
//                        frame->pts = test_last_frame_pts;
//                        last_frame_pts = test_last_frame_pts;
//
//                        int ret2 = avcodec_encode_video2(stream_ctx[stream_index].enc_ctx, &enc_pkt, frame, &got_picture);
//                        if( ret2 < 0 ) {
//                            //printf("Encode Error.\n");
//                            YY_ERROR("Encode finish\n");
//                            av_frame_free(&frame);
//                            return -1;
//                        }
//                        if( got_picture == 1 ) {
//                            //enc_pkt.stream_index = stream_index;
//                            enc_pkt.stream_index = stream_ctx[stream_index].out_stream_index;
//                            //pkt.stream_index = pAVStream->index;
//                            ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
//                        }
//                    }
//                    av_frame_unref(frame);
//                }
//                else {
//                    // 第一帧，原始处理
//                    frame->pts = 0;
//                    last_frame_pts = 0;
//
//                    int ret = avcodec_encode_video2(stream_ctx[stream_index].enc_ctx, &enc_pkt, frame, &got_picture);
//                    if( ret < 0 ) {
//                        //printf("Encode Error.\n");
//                        YY_ERROR("Encode finish\n");
//                        av_frame_free(&frame);
//                        return -1;
//                    }
//                    if( got_picture == 1 ) {
//                        //enc_pkt.stream_index = stream_index;
//                        enc_pkt.stream_index = stream_ctx[stream_index].out_stream_index;
//                        //pkt.stream_index = pAVStream->index;
//                        ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
//                        av_frame_unref(frame);
//                    }
//                }

            } else {
            	av_frame_unref(frame);
            }

        } else if(type==AVMEDIA_TYPE_AUDIO ) {
            /* remux this frame without reencoding */
            av_packet_rescale_ts(&packet,
                                 ifmt_ctx->streams[stream_index]->time_base,
                                 ofmt_ctx->streams[stream_ctx[stream_index].out_stream_index]->time_base);

            packet.stream_index = stream_ctx[stream_index].out_stream_index;
            ret = av_interleaved_write_frame(ofmt_ctx, &packet);
            if (ret < 0) {
                YY_ERROR("write audio failed\n");
                goto end;
            }
        }
        av_packet_unref(&packet);
    }

	{
    	// flush encoder;
    	while(1) {
            int got_picture = 0;
            AVPacket enc_pkt;
            enc_pkt.data = NULL;
            enc_pkt.size = 0;

            av_init_packet(&enc_pkt);
            int ret = avcodec_encode_video2(stream_ctx[video_stream_index].enc_ctx, &enc_pkt, NULL, &got_picture);
            if( ret < 0 ) {
				ret = 0;
                YY_ERROR("flush finish 1\n");
                break;
            }
            if( got_picture == 1 ) {
            	//enc_pkt.stream_index = video_stream_index;
                enc_pkt.stream_index = stream_ctx[video_stream_index].out_stream_index;
                //pkt.stream_index = pAVStream->index;
            	ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
            } else {
				YY_ERROR("flush finish 2\n");
            	break;
            }
    	}
    }
    av_write_trailer(ofmt_ctx);
end:
    av_packet_unref(&packet);
    av_frame_free(&frame);
    av_frame_free(&filt_frame);
	if(ifmt_ctx != NULL) {
		for (i = 0; i < ifmt_ctx->nb_streams; i++) {
            if( stream_ctx[i].dec_ctx == NULL )
                continue;
			avcodec_free_context(&stream_ctx[i].dec_ctx);
			if (ofmt_ctx )
				avcodec_free_context(&stream_ctx[i].enc_ctx);

		}
		av_free(stream_ctx);
		avformat_close_input(&ifmt_ctx);
		if (ofmt_ctx && !(ofmt_ctx->oformat->flags & AVFMT_NOFILE))
			avio_closep(&ofmt_ctx->pb);
		avformat_free_context(ofmt_ctx);
	}


    //if (ret < 0)
    //    av_log(NULL, AV_LOG_ERROR, "Error occurred: %s\n", av_err2str(ret));

    return ret ? 1 : 0;
}

int YY_mp4_video_get_info(const char *in_filename, int *video_duration, int *audio_duration, int *width, int *height)
{

    //AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx = NULL;// *ofmt_ctx = NULL;
    //AVPacket pkt;
    //AVCodecContext *icodec_ctx;
    //AVCodec *iCodec;

    //int frameFinished;

    int ret, i;

    *video_duration = 0;
	*audio_duration = 0;
    *width = 0;
    *height = 0;
    av_register_all();		//todo register once?

    if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
        YY_ERROR("Could not open input file '%s'", in_filename);
        goto end;
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        YY_ERROR("Failed to retrieve input stream information");
        goto end;
    }

    //av_dump_format(ifmt_ctx, 0, in_filename, 0);	//todo comment?


    int64_t frame_number = 0;
    int64_t in_stream_duration = 0;

    int in_stream_video_index = -1;

    double pts_ratio = 0;

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {

        AVStream *in_stream = ifmt_ctx->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;

        // only video
        if (in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
        	in_stream_video_index = i;

            frame_number = in_stream->nb_frames;
            in_stream_duration = in_stream->duration;
            pts_ratio = av_q2d(in_stream->time_base);
            *video_duration = in_stream_duration * av_q2d(in_stream->time_base)*1000;
            *width = in_codecpar->width;
            *height = in_codecpar->height;
            //YY_ERROR("duration %ld  %lg  %d\n", in_stream_duration, in_stream_duration * av_q2d(in_stream->time_base), frame_number);

            //break;
        }
		
		if(in_codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
			*audio_duration = in_stream->duration * av_q2d(in_stream->time_base)*1000;
		}
		
    }

end:

	avformat_close_input(&ifmt_ctx);

	if (ret < 0 && ret != AVERROR_EOF) {
		YY_ERROR("Error occurred: %s\n", av_err2str(ret));
		return -1;
	}

	return 0;
}
