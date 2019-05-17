#include <assert.h>
#include <string.h>
#include <pthread.h>
#include <jni.h>
#include <unistd.h>
#include <android/native_window_jni.h>

#include "gtvideodemuxer_jni.h"
#include "gtv_logger.h"
#include "gtv_demuxer.h"

#include "gtv_raw_frame.h"
#include "gtv_com_def.h"

#define JNI_CLASS_GTVIDEODEMUXER     "com/gtv/cloud/videoplayer/GTVideoDemuxer"


#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
	
static JavaVM* g_jvm = NULL;

typedef struct demuxer_fields_t {
    
    jclass clazz;

} demuxer_fields_t;

static demuxer_fields_t g_clazz;

typedef struct ST_WRAPPER_GTVIDEO_DEMUXER_T {
    
    uint8_t             magic_s[4];
    HANDLE_GTV_DEMUXER  demuxer_handle;
    ANativeWindow       * native_window;
    uint8_t             magic_e[4];
    
} ST_WRAPPER_GTVIDEO_DEMUXER;

static int wrapper_is_valid(ST_WRAPPER_GTVIDEO_DEMUXER * wrapper)
{
    if( wrapper == NULL )
        return 0;
    
    if( wrapper->magic_s[0] != 0xFF || wrapper->magic_s[1] != 0xFF || wrapper->magic_s[2] != 0xFF || wrapper->magic_s[3] != 0xFF ) {
        return 0;
    }
    
    if( wrapper->magic_e[0] != 0x55 || wrapper->magic_e[1] != 0x55 || wrapper->magic_e[2] != 0x55 || wrapper->magic_e[3] != 0x55 ) {
        return 0;
    }
    
    return 1;
}

static void wrapper_magic_init(ST_WRAPPER_GTVIDEO_DEMUXER * wrapper)
{
    if( wrapper == NULL )
        return;
    
    wrapper->magic_s[0] = 0xFF;
    wrapper->magic_s[1] = 0xFF;
    wrapper->magic_s[2] = 0xFF;
    wrapper->magic_s[3] = 0xFF;
    
    wrapper->magic_e[0] = 0x55;
    wrapper->magic_e[1] = 0x55;
    wrapper->magic_e[2] = 0x55;
    wrapper->magic_e[3] = 0x55;
    
    return;
}

static JNIEnv* getJNIEnv(int* needsDetach)
{
    JNIEnv* env = NULL;

    if ((g_jvm)->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK){

        int status = (g_jvm)->AttachCurrentThread(&env, 0);

        if (status < 0){
            //GTV_ERROR("failed to attach current thread");
            return NULL;
        }

        *needsDetach = 1;
    }

    return env;
}

static int android_render_yv12_on_yv12(ANativeWindow_Buffer *out_buffer, ST_GTV_RAW_FRAME_REF ref)
{
    // SDLTRACE("SDL_VoutAndroid: android_render_yv12_on_yv12(%p)", overlay);
    int min_height = out_buffer->height > ref->pixel_height ? ref->pixel_height : out_buffer->height;
    int dst_y_stride = out_buffer->stride;
    int dst_c_stride = DJNALIGN(out_buffer->stride / 2, 16);
    int dst_y_size = dst_y_stride * out_buffer->height;
    int dst_c_size = dst_c_stride * out_buffer->height / 2;
    
    uint8_t *dst_pixels_array[] = {
        (uint8_t *)out_buffer->bits,
        (uint8_t *)out_buffer->bits + dst_y_size,
        (uint8_t *)out_buffer->bits + dst_y_size + dst_c_size,
    };
	
	// openh264出来的结果是YUV，这边需要的yv12=YVU
	uint8_t *src_pixels_array[] = {
        (uint8_t *)ref->plane_data[0],
        (uint8_t *)ref->plane_data[2],
        (uint8_t *)ref->plane_data[1],
    };
	
    int dst_line_height[] = { min_height, min_height / 2, min_height / 2 };
    int dst_line_size_array[] = { dst_y_stride, dst_c_stride, dst_c_stride };
    
    int src_line_width[] = { (int)ref->pixel_width, (int)ref->pixel_width / 2, (int)ref->pixel_width / 2 };
	int src_line_width_stride[] = { (int)ref->stride_size[0], (int)ref->stride_size[1], (int)ref->stride_size[2] };
//    int dst_line_width[] = { out_buffer->width, out_buffer->width / 2, out_buffer->width / 2 };
    
    for (int i = 0; i < 3; ++i) {
		
        int dst_line_size = dst_line_size_array[i];
        int src_line_size = src_line_width[i];
        int line_height = dst_line_height[i];
        uint8_t *dst_pixels = dst_pixels_array[i];
        const uint8_t *src_pixels = src_pixels_array[i];
		
		
		for(int m = 0; m < line_height; ++m)
		{
			 memcpy(dst_pixels, src_pixels, src_line_size);
			 src_pixels += src_line_width_stride[i];
			 dst_pixels += dst_line_size;
		}
    }
    
    return 0;
}

#define HAL_PIXEL_FORMAT_YV12   0x32315659

static void postDataFromNative(void * target, ST_GTV_RAW_FRAME_REF ref)
{
    int retval;
    char overlay_format[5] = { 'Y', 'V', '1', '2', 0 };
    
    ANativeWindow * native_window = NULL;
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)target;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return;
    }
    
    if (wrapper->native_window == NULL) {
        GTV_ERROR("%d invalid native_window... \n", __LINE__);
        return;
    }
	
	HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r == NULL ) {
    	GTV_ERROR("%d invalid demuxer... \n", __LINE__);
		return;
    }

    if (!ref) {
        GTV_ERROR("%d invalid frame... \n", __LINE__);
        return;
    }
    
    if (ref->pixel_width <= 0 || ref->pixel_height <= 0) {
        GTV_ERROR("%d invalid frame size ... %d,%d \n", __LINE__, ref->pixel_width, ref->pixel_height);
        return;
    }
    
    native_window = wrapper->native_window;
    
    int curr_w = ANativeWindow_getWidth(native_window);
    int curr_h = ANativeWindow_getHeight(native_window);
    int curr_format = ANativeWindow_getFormat(native_window);
    int buff_w = ref->pixel_width;
    int buff_h = ref->pixel_height;
	
    if (curr_format != HAL_PIXEL_FORMAT_YV12) {
        GTV_ERROR("ANativeWindow_setBuffersGeometry: w=%d, h=%d, f=%.4s(0x%x) => w=%d, h=%d, f=%.4s",
              curr_w, curr_h, (char*) &curr_format, curr_format,
              buff_w, buff_h, (char*) overlay_format);
        retval = ANativeWindow_setBuffersGeometry(native_window, buff_w, buff_h, HAL_PIXEL_FORMAT_YV12);
        if (retval < 0) {
            GTV_ERROR("ANativeWindow_setBuffersGeometry: failed %d", retval);
            return;
        }
    }
    
    ANativeWindow_Buffer out_buffer;
    retval = ANativeWindow_lock(native_window, &out_buffer, NULL);
    if (retval < 0) {
        GTV_ERROR("ANativeWindow_lock: failed %d", retval);
        return;
    }
    
    if (out_buffer.width != buff_w || out_buffer.height != buff_h) {
        GTV_ERROR("unexpected native window buffer (%p)(w:%d, h:%d, fmt:'%.4s'0x%x), expecting (w:%d, h:%d, fmt:'%.4s')",
              native_window,
              out_buffer.width, out_buffer.height, (char*)&out_buffer.format, out_buffer.format,
              buff_w, buff_h, (char*)overlay_format);
        // TODO: 8 set all black
        ANativeWindow_unlockAndPost(native_window);
        ANativeWindow_setBuffersGeometry(native_window, buff_w, buff_h, HAL_PIXEL_FORMAT_YV12);
        return;
    }
    
    int render_ret = android_render_yv12_on_yv12(&out_buffer, ref);
    if (render_ret < 0) {
        // TODO: 8 set all black
        // return after unlock image;
    }
    
    retval = ANativeWindow_unlockAndPost(native_window);
    if (retval < 0) {
        GTV_ERROR("ANativeWindow_unlockAndPost: failed %d", retval);
        return;
    }
	
    return;
}

//---------------------------------------------------------------------------
// jni function
//---------------------------------------------------------------------------

static long long
GTVideoDemuxer_open(JNIEnv *env, jobject thiz, jstring url, jobject android_surface)
{
	const char * c_url = NULL;
	
	GTV_INFO("GTVideoDemuxer_open started ... \n");
    
    c_url = env->GetStringUTFChars(url, NULL);
    
    if( c_url == NULL ) {
        GTV_ERROR("GTVideoDemuxer_open params check ng (url or surface) ... \n");
        return 0;
    }
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER*)malloc(sizeof(ST_WRAPPER_GTVIDEO_DEMUXER));
    
    memset((uint8_t*)wrapper, 0x00, sizeof(ST_WRAPPER_GTVIDEO_DEMUXER));
    
    wrapper_magic_init(wrapper);
    
	wrapper->native_window = NULL;
	if (android_surface != NULL) {
		wrapper->native_window = ANativeWindow_fromSurface(env, android_surface);
	}
    
    wrapper->demuxer_handle = gtv_demuxer_open((char*)c_url);
    
    if( c_url ) {
        env->ReleaseStringUTFChars(url, c_url);
    }
	
	return (long long)wrapper;
}

static void
GTVideoDemuxer_close(JNIEnv *env, jobject thiz, jlong jwrapper) {
    
	GTV_INFO("GTVideoDemuxer_close ... \n");
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return;
    }
    
	HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r != NULL ) {
        gtv_demuxer_close(r);
        wrapper->demuxer_handle = NULL;
    }
	
    if( wrapper->native_window != NULL ) {
        ANativeWindow_release(wrapper->native_window);
        wrapper->native_window = NULL;
    }

    free(wrapper);
    
    return;
}

static int
GTVideoDemuxer_seekTo(JNIEnv *env, jobject thiz, jlong jwrapper, int milli) {
    
    GTV_INFO("GTVideoDemuxer_seekTo %d ... \n", milli);
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid demuxer... \n", __LINE__);
        return -2;
    }
    
    return gtv_demuxer_seekto(r, milli);
}

static int
GTVideoDemuxer_checkEof(JNIEnv *env, jobject thiz, jlong jwrapper) {
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid demuxer... \n", __LINE__);
        return -2;
    }
    
    return gtv_demuxer_check_eof(r);
}

static int
GTVideoDemuxer_setRange(JNIEnv *env, jobject thiz, jlong jwrapper, jint s, jint e) {
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid demuxer... \n", __LINE__);
        return -2;
    }
    
    return gtv_demuxer_set_range(r, (int64_t)s, (int64_t)e);
}

static int
GTVideoDemuxer_pullAudioData(JNIEnv *env, jobject thiz, jlong jwrapper, jbyteArray jdata, jint size) {
    
    int ret = 0;
    uint8_t * out_data = NULL;
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid demuxer... \n", __LINE__);
        return -2;
    }
    
    out_data = (uint8_t*)(env->GetByteArrayElements(jdata, 0));
    if( out_data == NULL ) {
        GTV_ERROR("GTVideoDemuxer_pullAudioData out_data is null.\n");
        return -3;
    }
    
    ret = gtv_demuxer_pull_audio(r, out_data, size);
    
    if( out_data ) {
        (env)->ReleaseByteArrayElements(jdata, (jbyte*)out_data, 0);
    }
    
    return ret;
}

static int
GTVideoDemuxer_nextVideoTimestamp(JNIEnv *env, jobject thiz, jlong jwrapper, jintArray jdata) {
    
    int ret = 0;
    int * out_data = NULL;
    ST_GTV_RAW_FRAME raw_frame;
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid demuxer... \n", __LINE__);
        return -2;
    }
    
    ret = gtv_demuxer_peek_next_video(r, &raw_frame);
    if( ret <= 0 ) {
        return -3;
    }
    
    out_data = (int*)(env->GetIntArrayElements(jdata, 0));
    if( out_data == NULL ) {
        GTV_ERROR("GTVideoDemuxer_nextVideoTimestamp out_data is null.\n");
        return -4;
    }
    
    out_data[0] = raw_frame.pixel_width;
    out_data[1] = raw_frame.pixel_height;
    
    if( out_data ) {
        (env)->ReleaseIntArrayElements(jdata, (jint*)out_data, 0);
    }
    
    return (int)raw_frame.timestamp;
}

static int
GTVideoDemuxer_peekNextVideo(JNIEnv *env, jobject thiz, jlong jwrapper) {
    
    int ret = 0;
    ST_GTV_RAW_FRAME raw_frame;
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid demuxer... \n", __LINE__);
        return -2;
    }
    
    ret = gtv_demuxer_peek_next_video(r, &raw_frame);
    if( ret <= 0 ) {
        return 0;
    }
    
    // use native window process data
    postDataFromNative(wrapper, &raw_frame);
    
    return ret;
}

static int
GTVideoDemuxer_removeNextVideo(JNIEnv *env, jobject thiz, jlong jwrapper) {
    
    int ret = 0;
    
    ST_WRAPPER_GTVIDEO_DEMUXER * wrapper = (ST_WRAPPER_GTVIDEO_DEMUXER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_DEMUXER r = (HANDLE_GTV_DEMUXER)(wrapper->demuxer_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid demuxer... \n", __LINE__);
        return -2;
    }
    
    ret = gtv_demuxer_remove_next_video(r);
    
    return ret;
}

// ----------------------------------------------------------------------------

static JNINativeMethod g_methods[] = {
    { "_open",					"(Ljava/lang/String;Ljava/lang/Object;)J",
                                                                    (long long *) GTVideoDemuxer_open },
    { "_close",                 "(J)V",      						(void *) GTVideoDemuxer_close },
    { "_seekTo",                "(JI)I",                            (void *) GTVideoDemuxer_seekTo },
    { "_checkEof",              "(J)I",                             (void *) GTVideoDemuxer_checkEof },
    { "_setRange",              "(JII)I",                           (void *) GTVideoDemuxer_setRange },
    { "_nextVideoTimestamp",    "(J[I)I",                           (void *) GTVideoDemuxer_nextVideoTimestamp },
    { "_peekNextVideo",         "(J)I",                             (void *) GTVideoDemuxer_peekNextVideo },
    { "_removeNextVideo",       "(J)I",                             (void *) GTVideoDemuxer_removeNextVideo },
    { "_pullAudioData",         "(J[BI)I",                          (void *) GTVideoDemuxer_pullAudioData }
};

JNIEXPORT jint JNI_OnLoad_DEMUXER(JavaVM *vm, void *reserved)
{
    JNIEnv* env = NULL;
    jclass clazz;

    g_jvm = vm;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

    // find class
    clazz = (env)->FindClass(JNI_CLASS_GTVIDEODEMUXER);
    if (clazz == NULL)
    {
        GTV_ERROR("Native registration unable to find class '%s'", JNI_CLASS_GTVIDEODEMUXER);
        return JNI_ERR;
    }
    g_clazz.clazz = (jclass)env->NewGlobalRef(clazz);

    // register native api
    if((env)->RegisterNatives(clazz, g_methods, NELEM(g_methods)) < 0)
    {
        //GTV_ERROR("ERROR: MediaPlayer native registration failed\n");
        return JNI_ERR;
    }

    return JNI_VERSION_1_4;
}
