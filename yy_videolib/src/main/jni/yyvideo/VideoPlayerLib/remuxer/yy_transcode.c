
#include "yy_transcode.h"

#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
//#include <fftools/cmdutils.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/opt.h>

#include <pthread.h>

#include <sys/time.h>
#include "yy_data_queue.h"
#include "yy_logger.h"
#include "yy_com_utils.h"

//#define YY_ERROR printf

int64_t eee_system_current_milli()
{
    int64_t milli = 0;

    struct timeval    tp;

    gettimeofday(&tp, NULL);

    //milli = tp.tv_sec * 1000 + tp.tv_usec/1000;
    milli = tp.tv_sec * (int64_t)1000 + tp.tv_usec/1000;

    return milli;
}


typedef struct {
    AVCodecContext *dec_ctx;
    AVCodecContext *enc_ctx;
    double video_rotate;
    int in_video_stream_index;
    int out_video_stream_index;     // 0
} VideoStreamContext;

typedef struct {

    pthread_mutex_t	mutex;
    pthread_cond_t 	cond;
    pthread_t      	thread_id;
    int				finish_flag;        // normal finish
    int             stop_flag;          // stop by user
    int             error_flag;         // stop for error

    const char *out_filename;
    int out_width;
    int out_height;

    AVFormatContext *ifmt_ctx;
    //AVFormatContext *ofmt_ctx;
    VideoStreamContext *stream_ctx;
    //int video_stream_index;

    //AVFilterContext *buffersink_ctx;
    //AVFilterContext *buffersrc_ctx;

    ST_YY_DATA_QUEUE video_frame_queue;

} ST_YY_TRANSCODER;

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
        av_log(NULL, AV_LOG_WARNING, "Odd rotation angle.\n"
               "If you want to help, upload a sample "
               "of this file to ftp://upload.ffmpeg.org/incoming/ "
               "and contact the ffmpeg-devel mailing list. (ffmpeg-devel@ffmpeg.org)");

    return theta;
}

static int open_input_file_video(const char *filename, AVFormatContext **ifmt_ctx_param, VideoStreamContext **video_stream_ctx_param)
{
    int ret;
    unsigned int i;

    AVFormatContext *ifmt_ctx; //= *ifmt
    VideoStreamContext *video_stream_ctx; //= *stream_ctx_param;

    ifmt_ctx = NULL;
    if ((ret = avformat_open_input(&ifmt_ctx, filename, NULL, NULL)) < 0) {
        YY_ERROR("Cannot open input file '%s'", filename);
        return ret;
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx, NULL)) < 0) {
        YY_ERROR("Cannot find stream information");
        return ret;
    }

    video_stream_ctx = av_mallocz_array(1, sizeof(*video_stream_ctx));
    if (!video_stream_ctx)
        return AVERROR(ENOMEM);

    video_stream_ctx->in_video_stream_index = -1;
    video_stream_ctx->out_video_stream_index = 0;
    video_stream_ctx->video_rotate = 0;

    double angle = 0;
    // looking for video stream
    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *stream = ifmt_ctx->streams[i];
        if(stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            AVCodec *dec = avcodec_find_decoder(stream->codecpar->codec_id);
            if (!dec) {
                YY_ERROR("Failed to find decoder for stream #%u\n", i);
                return AVERROR_DECODER_NOT_FOUND;
            }

            AVCodecContext *codec_ctx;

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
            if (codec_ctx->codec_type == AVMEDIA_TYPE_VIDEO) {

                codec_ctx->framerate = av_guess_frame_rate(ifmt_ctx, stream, NULL);
                angle = get_rotation(stream);
                /* Open decoder */
                ret = avcodec_open2(codec_ctx, dec, NULL);
                if (ret < 0) {
                    YY_ERROR("Failed to open decoder for stream #%u\n", i);
                    return ret;
                }
            }
            video_stream_ctx[0].dec_ctx = codec_ctx;
            video_stream_ctx[0].in_video_stream_index = i;
            video_stream_ctx[0].video_rotate = angle;

            break;
        }

    }
	
	if(video_stream_ctx->in_video_stream_index < 0) {
		return -1;
	}

    //av_dump_format(ifmt_ctx, 0, filename, 0);

    *ifmt_ctx_param = ifmt_ctx;
    *video_stream_ctx_param = video_stream_ctx;
    return 0;
}


static int open_output_file_video(const char *filename, AVFormatContext *ifmt_ctx, VideoStreamContext *video_stream_ctx,AVFormatContext **ofmt_ctx_param, int gop_size, int width, int height)
{
    AVStream *out_stream;
    AVStream *in_stream;
    AVCodecContext *dec_ctx, *enc_ctx;
    AVCodec *encoder;
    int ret, out_st_cnt;
    unsigned int i;

    AVDictionary *opt = NULL;

    AVFormatContext *ofmt_ctx;// = *ofmt_ctx_param;

    if(video_stream_ctx == NULL || video_stream_ctx[0].in_video_stream_index < 0 || video_stream_ctx[0].dec_ctx == NULL || video_stream_ctx[0].dec_ctx->codec_type != AVMEDIA_TYPE_VIDEO) {
        return -1;
    }

    ofmt_ctx = NULL;
    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, filename);
    if (!ofmt_ctx) {
        YY_ERROR("Could not create output context\n");
        return AVERROR_UNKNOWN;
    }

    dec_ctx = video_stream_ctx[0].dec_ctx;

    out_stream = avformat_new_stream(ofmt_ctx, NULL);
    if (!out_stream) {
        YY_ERROR("Failed allocating output stream\n");
        return AVERROR_UNKNOWN;
    }
    video_stream_ctx[0].out_video_stream_index = 0;

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
        enc_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

    if(gop_size == 1) {
        av_dict_set(&opt, "preset", "ultrafast", 0);
        av_dict_set(&opt, "qp", "23", 0);
        //av_dict_set(&opt, "crf", "24.0", 0);
        av_dict_set(&opt, "profile", "baseline", 0);
        av_dict_set(&opt, "level", "5.2", 0);
    } else {
        av_dict_set(&opt, "preset", "slow", 0);
        av_dict_set(&opt, "crf", "24.0", 0);
        av_dict_set(&opt, "profile", "high", 0);
        av_dict_set(&opt, "level", "3.1", 0);
    }

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
    video_stream_ctx[0].enc_ctx = enc_ctx;

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

static int init_filters(const char *filters_descr,AVFormatContext *ifmt_ctx, VideoStreamContext *stream_ctx,int video_stream_index,
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

    filter_graph->nb_threads = 8;

    AVCodecContext *dec_ctx = stream_ctx[0].dec_ctx;
    /* buffer video source: the decoded frames from the decoder will be inserted here. */
    snprintf(args, sizeof(args),
            "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
            dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
            time_base.num, time_base.den,
            dec_ctx->sample_aspect_ratio.num, dec_ctx->sample_aspect_ratio.den);

    ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in",
                                       args, NULL, filter_graph);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create buffer source\n");
        goto end;
    }

    /* buffer video sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                       NULL, NULL, filter_graph);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot create buffer sink\n");
        goto end;
    }

    ret = av_opt_set_int_list(buffersink_ctx, "pix_fmts", pix_fmts,
                              AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        av_log(NULL, AV_LOG_ERROR, "Cannot set output pixel format\n");
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

static void * YY_transcode_thread(void * thread_data)
{
    int ret;

    ST_YY_TRANSCODER* transcoder = (ST_YY_TRANSCODER*)thread_data;

    transcoder->error_flag = 0;

    VideoStreamContext *stream_ctx = transcoder->stream_ctx;
    AVFormatContext *ifmt_ctx = transcoder->ifmt_ctx;
    AVFormatContext *ofmt_ctx = NULL;
    AVFrame *filt_frame = av_frame_alloc();
    int video_stream_index = stream_ctx[0].in_video_stream_index;

    AVFilterContext *buffersink_ctx = NULL;//transcoder->buffersink_ctx;
    AVFilterContext *buffersrc_ctx = NULL;//transcoder->buffersrc_ctx;
    AVFilterGraph *filter_graph;

    AVCodecContext *dec_ctx = stream_ctx[0].dec_ctx;
    int in_width = dec_ctx->width;
    int in_height = dec_ctx->height;

    int out_width = transcoder->out_width;//576;
    int out_height = transcoder->out_height;//1024;


    double video_rotate = stream_ctx[0].video_rotate;

    char desc[512];
    int transpose_flag = 0;
	int filter_need = 1;
    if(fabs(video_rotate - 90) < 1.0) {
        transpose_flag = 1;
        if(in_width < in_height) {		//landscape
            int ww = out_width;
            out_width = out_height;
            out_height = ww;
        }
        //out_width = in_height;
        //out_height = in_width;
        if((in_width <= out_height) && (in_height <= out_width)) {
			out_height = in_width;
			out_width = in_height;
            snprintf(desc, sizeof(desc),"transpose=clock");
        } else {
			if(fabs(in_width*1.0/in_height - out_height*1.0/out_width) < 0.01) {
				snprintf(desc, sizeof(desc),"scale=%d:%d,transpose=clock", out_height,out_width);
			} else {
				snprintf(desc, sizeof(desc),"scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2,transpose=clock", out_height,out_width,out_height,out_width);
			}
            
        }
        //snprintf(desc, sizeof(desc),"transpose=clock,scale=%d:%d",width, height);
        //snprintf(desc, sizeof(desc),"transpose=clock");
    } else if (fabs(video_rotate - 180) < 1.0) {
        if(in_width > in_height) {		//landscape
            int ww = out_width;
            out_width = out_height;
            out_height = ww;
        }

        if((in_width <= out_width) && (in_height <= out_height)) {
			out_height = in_height;
			out_width = in_width;
            snprintf(desc, sizeof(desc),"hflip,vflip");
        } else {
			if(fabs(in_width*1.0/in_height - out_width*1.0/out_height) < 0.01) {
				snprintf(desc, sizeof(desc),"scale=%d:%d,hflip,vflip",out_width, out_height);
			} else {
				snprintf(desc, sizeof(desc),"scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2,hflip,vflip",out_width, out_height,out_width, out_height);
			}
            
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
        if((in_width <= out_height) && (in_height <= out_width)) {
			out_height = in_width;
			out_width = in_height;
            snprintf(desc, sizeof(desc),"transpose=cclock");
        } else {
			if(fabs(in_width*1.0/in_height - out_height*1.0/out_width) < 0.01) {
				snprintf(desc, sizeof(desc),"scale=%d:%d,transpose=cclock", out_height,out_width);
			} else {
				snprintf(desc, sizeof(desc),"scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2,transpose=cclock", out_height,out_width, out_height,out_width);
			}
            
        }
        //snprintf(desc, sizeof(desc),"transpose=cclock,scale=%d:%d",width, height);
        //snprintf(desc, sizeof(desc),"transpose=cclock");
    } else{
        if(in_width > in_height) {		//landscape
            int ww = out_width;
            out_width = out_height;
            out_height = ww;
        }

        if((in_width <= out_width) && (in_height <= out_height)) {
			out_height = in_height;
			out_width = in_width;
            snprintf(desc, sizeof(desc),"copy");
			filter_need = 0;
        } else {
			if(fabs(in_width*1.0/in_height - out_width*1.0/out_height) < 0.01) {
				snprintf(desc, sizeof(desc),"scale=%d:%d",out_width, out_height);
			} else {
				snprintf(desc, sizeof(desc),"scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2",out_width, out_height,out_width, out_height);
			}
            
        }
        //snprintf(desc, sizeof(desc),"scale=%d:%d",width, height);
        //snprintf(desc, sizeof(desc),"copy");
    }

    //YY_ERROR("progress %s \n", desc);

    if ((ret = open_output_file_video(transcoder->out_filename, ifmt_ctx, stream_ctx,&ofmt_ctx, 1, out_width, out_height)) < 0){
        transcoder->error_flag = 1;
        goto end;
    }

    if ((ret =init_filters(desc, ifmt_ctx, stream_ctx, stream_ctx[0].in_video_stream_index,
            &buffersink_ctx, &buffersrc_ctx, &filter_graph)) < 0 ){
        transcoder->error_flag = 1;
        goto end;
    }

    while(1) {
        if(transcoder->stop_flag != 0) {
            goto end;       // stop by user
        }

        if(transcoder->video_frame_queue.count > 0) {
            int extra = 0;
            //YY_ERROR("get frame %d \n", transcoder->video_frame_queue.count);
            AVFrame * vframe = YY_dataqueue_get(&(transcoder->video_frame_queue), &extra);
            if(vframe != NULL) {
				if(filter_need != 0 ) {
					if (av_buffersrc_add_frame_flags(buffersrc_ctx, vframe, AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {
						YY_ERROR("Error while feeding the filtergraph\n");
						transcoder->error_flag = 1;
						goto end;
					}

					av_frame_free(&vframe);

					while (1) {

						ret = av_buffersink_get_frame(buffersink_ctx, filt_frame);
						if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
							break;

						if (ret < 0) {
							transcoder->error_flag = 1;
							goto end;
						}

						int got_picture = 0;
						AVPacket enc_pkt;
						enc_pkt.data = NULL;
						enc_pkt.size = 0;

						av_init_packet(&enc_pkt);
						// 不受输入流的gop影响（输入流是全关键帧输出也变成全关键帧）
						filt_frame->key_frame = 0;
						filt_frame->pict_type = AV_PICTURE_TYPE_NONE;

						int ret = avcodec_encode_video2(stream_ctx[0].enc_ctx, &enc_pkt, filt_frame, &got_picture);
						if( ret < 0 ) {
							//printf("Encode Error.\n");
							YY_ERROR("Encode finish\n");
							//av_frame_free(&frame);
							transcoder->error_flag = 1;
							goto end;
						}
						if( got_picture == 1 ) {
							//enc_pkt.stream_index = stream_index;
							enc_pkt.stream_index = stream_ctx[0].out_video_stream_index;
							//pkt.stream_index = pAVStream->index;
							ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
							//av_frame_unref(frame);
						}

						av_frame_unref(filt_frame);
					}					
				} else {
					int got_picture = 0;
					AVPacket enc_pkt;
					enc_pkt.data = NULL;
					enc_pkt.size = 0;

					av_init_packet(&enc_pkt);
					// 不受输入流的gop影响（输入流是全关键帧输出也变成全关键帧）
					vframe->key_frame = 0;
					vframe->pict_type = AV_PICTURE_TYPE_NONE;

					int ret = avcodec_encode_video2(stream_ctx[0].enc_ctx, &enc_pkt, vframe, &got_picture);
					if( ret < 0 ) {
						//printf("Encode Error.\n");
						YY_ERROR("Encode finish\n");
						//av_frame_free(&frame);
						transcoder->error_flag = 1;
						goto end;
					}
					if( got_picture == 1 ) {
						//enc_pkt.stream_index = stream_index;
						enc_pkt.stream_index = stream_ctx[0].out_video_stream_index;
						//pkt.stream_index = pAVStream->index;
						ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
						//av_frame_unref(frame);
					}
					av_frame_free(&vframe);						
				}


            } else {
                //impossible
            }
        }
        else {
            if(transcoder->finish_flag != 0) {
                break;
            } else {
                //wait for decoding?
				YY_milliseconds_sleep(5);
            }
        }
    }

    // flush encoder;
    while(1) {
        int got_picture = 0;
        AVPacket enc_pkt;
        enc_pkt.data = NULL;
        enc_pkt.size = 0;

        av_init_packet(&enc_pkt);
        int ret = avcodec_encode_video2(stream_ctx[0].enc_ctx, &enc_pkt, NULL, &got_picture);
        if( ret < 0 ) {
            ret = 0;
            YY_ERROR("flush finish 1\n");
            break;
        }
        if( got_picture == 1 ) {
            //enc_pkt.stream_index = video_stream_index;
            enc_pkt.stream_index = stream_ctx[0].out_video_stream_index;
            //pkt.stream_index = pAVStream->index;
            ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
        } else {
            YY_ERROR("flush finish 2\n");
            break;
        }
    }

    av_write_trailer(ofmt_ctx);

end:
    //YY_ERROR("%lld thread finish \n", eee_system_current_milli());

    avfilter_graph_free(&filter_graph);

    avcodec_free_context(&stream_ctx[0].enc_ctx);

    if (ofmt_ctx && !(ofmt_ctx->oformat->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);
	
	av_frame_free(&filt_frame);	

    return NULL;
}

int mp4_video_transcode_video(const char *in_filename, const char *out_filename, int gop_size, OnProgress onProgress, void* target, int width, int height, bool crop_flag, int start_time, int end_time)
{

    int ret;
    int i;
    AVFormatContext *ifmt_ctx;
    //AVFormatContext *ofmt_ctx;
    VideoStreamContext *stream_ctx;



    AVPacket packet = { .data = NULL, .size = 0 };
    //AVFrame *frame = av_frame_alloc();
    //AVFrame *filt_frame = av_frame_alloc();

    AVFrame *frame = NULL;
    AVFrame *filt_frame = NULL;

    enum AVMediaType type;
    unsigned int stream_index;
    unsigned int video_stream_index;

    int got_frame;

    av_register_all();
    avfilter_register_all();

    int in_video_stream_index = -1;
    double video_rotate = 0;

    if ((ret = open_input_file_video(in_filename, &ifmt_ctx, &stream_ctx)) < 0)
        goto end;

    ST_YY_TRANSCODER * transcoder = (ST_YY_TRANSCODER *)malloc(sizeof(ST_YY_TRANSCODER));
    memset(transcoder, 0x00, sizeof(ST_YY_TRANSCODER));
    pthread_mutex_init(&transcoder->mutex, NULL);
    pthread_cond_init(&transcoder->cond,NULL);
    transcoder->stream_ctx = stream_ctx;
    //transcoder->ofmt_ctx = ofmt_ctx;
    transcoder->ifmt_ctx = ifmt_ctx;
    transcoder->out_filename = out_filename;
    transcoder->out_width = width;
    transcoder->out_height = height;
    //transcoder->video_stream_index = in_video_stream_index;
    //transcoder->buffersink_ctx = buffersink_ctx;
    //transcoder->buffersrc_ctx = buffersrc_ctx;

    YY_dataqueue_init(&(transcoder->video_frame_queue));

    if( pthread_create(&(transcoder->thread_id), NULL, YY_transcode_thread, transcoder) < 0 )
    {
        YY_ERROR("launch YY_transcode_thread failed");
        free(transcoder);
        goto end;
    }
    //
    //int64_t last_frame_pts = -1;
	
	int64_t starttime_pts = 0;
    int64_t endtime_pts = 0;
    int64_t first_frame_pts = 0;
	int crop_finish = 0;

    AVRational enc_ctx_timebase = (AVRational){1, 90000};

    if( (start_time < 0) || (end_time < 0) || (start_time >= end_time)) {
        crop_flag = false;
		start_time = 0;
    }

    if( crop_flag == true) {
        double pts_ratio = av_q2d(enc_ctx_timebase);
        starttime_pts = start_time / pts_ratio / 1000;
        endtime_pts = end_time / pts_ratio / 1000;
    }

    int64_t in_stream_starttime_pts =start_time / av_q2d(ifmt_ctx->streams[stream_ctx[0].in_video_stream_index]->time_base) / 1000;
	int64_t in_stream_duration =ifmt_ctx->streams[stream_ctx[0].in_video_stream_index]->duration;
	int64_t crop_period = in_stream_duration;
	if( crop_flag == true) {
		int64_t temp = (end_time - start_time) / av_q2d(ifmt_ctx->streams[stream_ctx[0].in_video_stream_index]->time_base) / 1000;
		if(temp < crop_period){
			crop_period = temp;
		}
	}

	if(crop_flag == true) {
		av_seek_frame(ifmt_ctx,stream_ctx[0].in_video_stream_index, in_stream_starttime_pts, AVSEEK_FLAG_BACKWARD);
	}

    /* read all packets */
    while (1) {
        if(transcoder->error_flag != 0) {
            ret = -1;
            goto end;
        }
		
		if(crop_finish != 0) {
			break;
		}
		if(transcoder->video_frame_queue.count > 10) {
            YY_milliseconds_sleep(20);
			YY_ERROR("Decode too fast\n");
            continue;
        }
        if ((ret = av_read_frame(ifmt_ctx, &packet)) < 0){
            ret = 0; //正常情况
            break;
        }
		

        stream_index = packet.stream_index;
        type = ifmt_ctx->streams[packet.stream_index]->codecpar->codec_type;

        if(type==AVMEDIA_TYPE_VIDEO) {

            video_stream_index = stream_index;
			
			//YY_ERROR("packet %lld %lld %lld %lld\n", packet.pts, in_stream_starttime_pts, in_stream_starttime_pts, crop_period);

            //int64_t in_stream_duration =ifmt_ctx->streams[packet.stream_index]->duration;
            int progress = ((packet.pts - in_stream_starttime_pts)<0?0:(packet.pts - in_stream_starttime_pts))*100/crop_period;
            if(progress>=100) {
                progress = 99;
            }
            if(onProgress != NULL) {
                int cancel = onProgress(target, progress);
                if(cancel !=0) {
                    transcoder->stop_flag  = 1;
                    ret = -9999;
                    goto end;
                }
            }


            av_packet_rescale_ts(&packet,
                                 ifmt_ctx->streams[stream_index]->time_base,
                                 enc_ctx_timebase);

            //YY_ERROR("packet %lld\n", packet.pts);

            ret = avcodec_send_packet(stream_ctx[0].dec_ctx, &packet);
            if (ret < 0) {
                YY_ERROR("Error sending a packet for decoding\n");
                goto end;
            }

            while (ret >= 0) {
                frame = av_frame_alloc();
                ret = avcodec_receive_frame(stream_ctx[0].dec_ctx, frame);
                if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
					ret = 0;
                    av_frame_free(&frame);
                    break;
                }
                else if (ret < 0) {
                    av_frame_free(&frame);
                    YY_ERROR("Error during decoding\n");
                    goto end;
                }

                //YY_ERROR("frame %lld %lld %lld \n", frame->pts, starttime_pts, endtime_pts);

                if( (crop_flag == true) && ( (frame->pts < starttime_pts) || (frame->pts > endtime_pts)) ) {
					//YY_ERROR("hhhh frame %lld %lld %lld \n", frame->pts, starttime_pts, endtime_pts);
					if(frame->pts > endtime_pts) {
						crop_finish = 1;
					}
					av_frame_free(&frame);
                    continue;
                }

                // change timestamp
                if (first_frame_pts == 0) {
                    first_frame_pts = frame->pts;
                    frame->pts = 0;
                } else {
                    frame->pts -= first_frame_pts;
                }

                YY_dataqueue_put(&(transcoder->video_frame_queue), frame, 0);
            }


        } else if(type==AVMEDIA_TYPE_AUDIO ) {
        }
        av_packet_unref(&packet);
    }

end:
    transcoder->finish_flag  = 1;

    //YY_ERROR("%lld wait thread finish1 \n", eee_system_current_milli());
    pthread_join(transcoder->thread_id, NULL);
    //YY_ERROR("%lld wait thread finish2  \n",eee_system_current_milli());

	
    av_packet_unref(&packet);
    av_frame_free(&frame);
    av_frame_free(&filt_frame);
    if(ifmt_ctx != NULL) {

        if(stream_ctx[0].dec_ctx != NULL) {
            avcodec_free_context(&stream_ctx[0].dec_ctx);
        }
        av_free(stream_ctx);
        avformat_close_input(&ifmt_ctx);

    }

    pthread_mutex_destroy(&transcoder->mutex);
    pthread_cond_destroy(&transcoder->cond);


    //if (ret < 0)
    //    av_log(NULL, AV_LOG_ERROR, "Error occurred: %s\n", av_err2str(ret));

    //return ret ? 1 : 0;
    int error_flag = transcoder->error_flag;
    free(transcoder);

    if(ret <0 || error_flag != 0) {
        return -1;
    } else {
        return 0;
    }
}

int mp4_video_transcode_audio(const char *in_filename, const char *out_filename, bool crop_flag, int start_time, int end_time)
{
    AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx = NULL, *ofmt_ctx = NULL;
    AVPacket pkt;
    int ret, i;
    int stream_index = 0;

    AVPacket packet = { .data = NULL, .size = 0 };
    enum AVMediaType type;

    int64_t starttime_pts = 0;
    int64_t endtime_pts = 0;
    int64_t first_frame_pts = 0;

    av_register_all();        //todo register once?

    if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
        YY_ERROR("Could not open input file '%s'", in_filename);
        goto end;
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        YY_ERROR("Failed to retrieve input stream information");
        goto end;
    }

    //av_dump_format(ifmt_ctx, 0, in_filename, 0);    //todo comment?

    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
    if (!ofmt_ctx) {
        YY_ERROR("Could not create output context\n");
        ret = AVERROR_UNKNOWN;
        goto end;
    }

    ofmt = ofmt_ctx->oformat;

    int in_stream_video_index = -1;
    int out_stream_video_index = -1;

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;

        // only audio
        if (in_codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            in_stream_video_index = i;
            out_stream_video_index = 0;

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
            out_stream->time_base = in_stream->time_base;
            break;;
        }
    }

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

    if(ofmt_ctx->nb_streams < 1) {
        ret = -1;
        goto end;
    }

    if( (start_time < 0) || (end_time < 0) || (start_time >= end_time)) {
        crop_flag = false;
    }

    if( crop_flag == true) {
        double pts_ratio = av_q2d(ifmt_ctx->streams[in_stream_video_index]->time_base);
        starttime_pts = start_time / pts_ratio / 1000;
        endtime_pts = end_time / pts_ratio / 1000;
    }

    av_seek_frame(ifmt_ctx,in_stream_video_index, starttime_pts, AVSEEK_FLAG_BACKWARD);

    /* read all packets */
    while (1) {

        if ((ret = av_read_frame(ifmt_ctx, &packet)) < 0){
            ret = 0; //正常情况
            break;
        }


        stream_index = packet.stream_index;
        type = ifmt_ctx->streams[packet.stream_index]->codecpar->codec_type;

        if(type==AVMEDIA_TYPE_AUDIO) {

            if( (crop_flag == true) && ( (packet.pts < starttime_pts) || (packet.pts > endtime_pts)) ) {
                continue;
            }

            /* remux this frame without reencoding */
            av_packet_rescale_ts(&packet,
                                 ifmt_ctx->streams[stream_index]->time_base,
                                 ofmt_ctx->streams[out_stream_video_index]->time_base);

            packet.stream_index = out_stream_video_index;

            // change timestamp
            if (first_frame_pts == 0) {
                first_frame_pts = packet.pts;
                packet.pts = 0;
                packet.dts = packet.pts;
            } else {
                packet.pts -= first_frame_pts;
                packet.dts = packet.pts;
            }

            ret = av_interleaved_write_frame(ofmt_ctx, &packet);
            if (ret < 0) {
                YY_ERROR("write audio failed\n");
                goto end;
            }

        }
        av_packet_unref(&packet);
    }
    av_write_trailer(ofmt_ctx);
end:
    if(ifmt_ctx) {
        avformat_close_input(&ifmt_ctx);
    }


    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    if(ofmt_ctx) {
        avformat_free_context(ofmt_ctx);
    }


    if (ret < 0 && ret != AVERROR_EOF) {
        YY_ERROR("Error occurred: %s\n", av_err2str(ret));
        return -1;
    }
    return 0;
}
