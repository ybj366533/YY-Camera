#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <libavutil/time.h>
#include <libavutil/imgutils.h>

#include "yy_logger.h"

typedef struct YY_FFMUX_CORE_T {
    
    char filename[2048];
    
    uint8_t video_meta_data[1024];
    int video_meta_size;
    
    int video_width;
    int video_height;
    int video_bitrate;
    int video_fps;
    
    int is_ready;
    
    AVOutputFormat *ofmt;
    AVFormatContext *ofmt_ctx;
    
//    AVCodec *audio_codec;
//    AVCodec *video_codec;
    
//    AVCodecContext *audio_codec_context;
//    AVCodecContext *video_codec_context;
    
    AVStream *out_audio_stream;
    AVStream *out_video_stream;

} YY_FFMUX_CORE;

static AVStream * add_stream2(
                             YY_FFMUX_CORE * mux,
                             AVFormatContext *oc,
                             enum AVCodecID codec_id);

void * initStream(char * out_filename, int video_w, int video_h, int video_bps, int video_fps)
{
    YY_FFMUX_CORE * c = (YY_FFMUX_CORE*)malloc(sizeof(YY_FFMUX_CORE));
    
    memset((uint8_t*)c, 0x00, sizeof(YY_FFMUX_CORE));
    
    if( strlen(out_filename) > sizeof(c->filename) ) {
        free(c);
        return NULL;
    }
    
    strcpy(c->filename, out_filename);
    
    c->video_width = video_w;
    c->video_height = video_h;
    c->video_bitrate = video_bps;
    c->video_fps = video_fps;
    
    return (void*)c;
}

int openStream(void * handle, uint8_t * video_meta, int len)
{
    int ret;
    
    YY_FFMUX_CORE * c = (YY_FFMUX_CORE*)handle;
    
    c->video_meta_size = len;
    memcpy(c->video_meta_data, video_meta, len);
    
    av_register_all();
    avformat_network_init();
    
    avformat_alloc_output_context2(&(c->ofmt_ctx), NULL, NULL, c->filename);
    if (!(c->ofmt_ctx)) {
        YY_ERROR("Could not create output context(%s)\n", c->filename);
        ret = AVERROR_UNKNOWN;
        return ret;
    }
    
    c->ofmt = c->ofmt_ctx->oformat;
    
    // add audio stream
    //c->out_audio_stream = add_stream(c, c->ofmt_ctx, &c->audio_codec, &c->audio_codec_context, AV_CODEC_ID_AAC);
    c->out_audio_stream = add_stream2(c, c->ofmt_ctx, AV_CODEC_ID_AAC);
    if (!c->out_audio_stream) {
        YY_ERROR("Could not create out_audio_stream\n");
        ret = AVERROR_UNKNOWN;
        return ret;
    }
    // add video stream
    //c->out_video_stream = add_stream(c, c->ofmt_ctx, &c->video_codec, &c->video_codec_context, AV_CODEC_ID_H264);
    c->out_video_stream = add_stream2(c, c->ofmt_ctx, AV_CODEC_ID_H264);
    if (!c->out_video_stream) {
        YY_ERROR("Could not create out_video_stream\n");
        ret = AVERROR_UNKNOWN;
        return ret;
    }
    
    av_dump_format(c->ofmt_ctx, 0, c->filename, 1);
    
    if (!(c->ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&c->ofmt_ctx->pb, c->filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            YY_ERROR("Could not open output file '%s'", c->filename);
            ret = AVERROR_UNKNOWN;
            return ret;
        }
    }
    
    ret = avformat_write_header(c->ofmt_ctx, NULL);
    if (ret < 0) {
        YY_ERROR("Error occurred when opening output file\n");
        ret = AVERROR_UNKNOWN;
        return ret;
    }
    
    c->is_ready = 1;
    
    av_dump_format(c->ofmt_ctx, 0, c->filename, 1);
    
    return 0;
}

int closeStream(void * handle)
{
    YY_FFMUX_CORE * c = (YY_FFMUX_CORE*)handle;
    
    if( c->ofmt_ctx != NULL ) {
        
        av_write_trailer(c->ofmt_ctx);
        
        /* close output */
        if (c->ofmt_ctx && !(c->ofmt->flags & AVFMT_NOFILE))
            avio_closep(&c->ofmt_ctx->pb);
        avformat_free_context(c->ofmt_ctx);
    }
    
    free(c);
    
    return 0;
}

int checkStreamReady(void * handle)
{
    YY_FFMUX_CORE * c = (YY_FFMUX_CORE*)handle;
    
    return c->is_ready;
}

void writeAudioFrame(void * handle, uint8_t * data, int len, int64_t pts)
{
    YY_FFMUX_CORE * c = (YY_FFMUX_CORE*)handle;
    
    if( data == NULL ) {
        YY_ERROR("audio is NULL.");
    }
    AVPacket pkt;
    av_init_packet(&pkt);
    
    int pkt_pts = pts / av_q2d(c->out_audio_stream->time_base);
    pkt_pts = pkt_pts / 1000;
    
    pkt.buf = NULL;
    pkt.dts = pkt.pts = pkt_pts;
    pkt.data = data;
    pkt.size = len;
    pkt.stream_index = c->out_audio_stream->index;
    pkt.flags = 0;
    pkt.side_data = NULL;
    pkt.side_data_elems = 0;
    pkt.duration = 0;
    pkt.pos = -1;
    
    //YY_ERROR("@@@ av_audio : %d pts:%d %d \n", pkt.size, pkt_pts, pts);
    int ret = av_interleaved_write_frame(c->ofmt_ctx, &pkt);
    if( ret ) {
        YY_ERROR("av_interleaved_write_frame audio failed %d\n", ret);
    }
    
    return;
}

void writeVideoFrame(void * handle, uint8_t * data, int len, int64_t pts, int isKey)
{
    YY_FFMUX_CORE * c = (YY_FFMUX_CORE*)handle;
    
    // not mp4 format
    if( !(data[0] == 0 && data[1] == 0 && data[2] == 0 && data[3] == 1) ) {
        
        int pos = 0;
        while(pos + 4 < len ) {
            // startcode
            int lenth = (data[pos+0]<<24) + (data[pos+1]<<16) + (data[pos+2]<<8) + (data[pos+3]) + 4;
            data[pos+0] = 0x00;
            data[pos+1] = 0x00;
            data[pos+2] = 0x00;
            data[pos+3] = 0x01;
            pos += lenth;
        }
    }
    
    AVPacket pkt;
    av_init_packet(&pkt);
    
    int pkt_pts = pts / av_q2d(c->out_video_stream->time_base);
    pkt_pts = pkt_pts / 1000;
    
    // pkt.pts = av_rescale_q(c->coded_frame->pts,c->time_base,en_info->video_st->time_base);
    pkt.buf = NULL;
    pkt.dts = pkt.pts = pkt_pts;
    pkt.data = data;
    pkt.size = len;
    pkt.stream_index = c->out_video_stream->index;
    
    pkt.flags = 0;
    if( isKey ) {
        pkt.flags |= AV_PKT_FLAG_KEY;
    }
    pkt.side_data = NULL;
    pkt.side_data_elems = 0;
    pkt.duration = 0;
    pkt.pos = -1;
    
    // av_write_frame
    // YY_ERROR("@@@ av_video : %d pts:%d %d \n", pkt.size, pkt_pts, pts);
    int ret = av_interleaved_write_frame(c->ofmt_ctx, &pkt);
    if( ret ) {
        YY_ERROR("av_interleaved_write_frame video failed %d\n", ret);
    }
    
    return;
}

//#define STREAM_FRAME_RATE 25 /* 25 images/s */
#define STREAM_PIX_FMT    AV_PIX_FMT_YUV420P// /* default pix_fmt */ AV_PIX_FMT_VIDEOTOOLBOX
//#define STREAM_PIX_WIDTH    640
//#define STREAM_PIX_HEIGHT    480
//
//static void log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt, const char *tag)
//{
//    AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;
//
//    YY_ERROR("%s: pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d\n",
//           tag,
//           av_ts2str(pkt->pts), av_ts2timestr(pkt->pts, time_base),
//           av_ts2str(pkt->dts), av_ts2timestr(pkt->dts, time_base),
//           av_ts2str(pkt->duration), av_ts2timestr(pkt->duration, time_base),
//           pkt->stream_index);
//}

/* Add an output stream. */
static AVStream * add_stream2(
                             YY_FFMUX_CORE * mux,
                             AVFormatContext *oc,
                             enum AVCodecID codec_id)
{
    AVStream * st;
    
    st = avformat_new_stream(oc, NULL);
    if (!st) {
        YY_ERROR("Could not allocate stream\n");
        return NULL;
    }
    st->id = oc->nb_streams-1;
    
    if( codec_id == AV_CODEC_ID_AAC ) {
        
        st->codecpar->codec_type = AVMEDIA_TYPE_AUDIO;
        st->codecpar->codec_id = AV_CODEC_ID_AAC;
        st->codecpar->codec_tag = 0;//828601953;
        st->codecpar->format = 8;
        st->codecpar->bit_rate = 128000;
        st->codecpar->bits_per_coded_sample = 16;
        st->codecpar->bits_per_raw_sample = 0;
        
        st->codecpar->profile = 1;
        st->codecpar->level = -99;
        
        st->codecpar->sample_aspect_ratio.num = 0;
        st->codecpar->sample_aspect_ratio.den = 1;
        
        st->codecpar->channel_layout = AV_CH_LAYOUT_STEREO;
        st->codecpar->channels = 2;
        st->codecpar->sample_rate = 44100;
        st->codecpar->frame_size = 1024;
        
        if( st->codecpar->extradata == NULL ) {
            st->codecpar->extradata = av_mallocz(2 + AV_INPUT_BUFFER_PADDING_SIZE);
            memset(st->codecpar->extradata, 0x00, 2 + AV_INPUT_BUFFER_PADDING_SIZE);
        }
        st->codecpar->extradata[0] = 0x12;
        st->codecpar->extradata[1] = 0x10;
        st->codecpar->extradata_size = 2;
    }
    else if( codec_id == AV_CODEC_ID_H264 ) {
        
        st->codecpar->codec_type = AVMEDIA_TYPE_VIDEO;
        st->codecpar->codec_id = AV_CODEC_ID_H264;
        st->codecpar->codec_tag = 0;//828601953;
        st->codecpar->format = 0;
        st->codecpar->bit_rate = mux->video_bitrate;
        st->codecpar->bits_per_coded_sample = 24;
        st->codecpar->bits_per_raw_sample = 8;
        
        st->codecpar->profile = 100;
        st->codecpar->level = 31;
        
        st->codecpar->width = mux->video_width;
        st->codecpar->height = mux->video_height;
        
        st->codecpar->sample_aspect_ratio.num = 0;
        st->codecpar->sample_aspect_ratio.den = 1;
        /*
        YY_ERROR("before ----- \n");
        for( int j=0; j<st->codecpar->extradata_size; j++ ) {
            uint8_t a = st->codecpar->extradata[j];
            YY_ERROR("%02x ", a);
        }
        */
        if( mux->video_meta_size > 0 ) {
            if( st->codecpar->extradata == NULL ) {
                st->codecpar->extradata = av_mallocz(mux->video_meta_size + AV_INPUT_BUFFER_PADDING_SIZE);
                memset(st->codecpar->extradata, 0x00, mux->video_meta_size + AV_INPUT_BUFFER_PADDING_SIZE);
            }
            memcpy(st->codecpar->extradata, mux->video_meta_data, mux->video_meta_size);
            st->codecpar->extradata_size = mux->video_meta_size;
        }
        /*
        YY_ERROR("\nafter ----- \n");
        for( int j=0; j<st->codecpar->extradata_size; j++ ) {
            uint8_t a = st->codecpar->extradata[j];
            YY_ERROR("%02x ", a);
        }
        YY_ERROR("\n");
        */
        /**
         * Extra binary data needed for initializing the decoder, codec-dependent.
         *
         * Must be allocated with av_malloc() and will be freed by
         * avcodec_parameters_free(). The allocated size of extradata must be at
         * least extradata_size + AV_INPUT_BUFFER_PADDING_SIZE, with the padding
         * bytes zeroed.
         */
    }
    
    return st;
}

