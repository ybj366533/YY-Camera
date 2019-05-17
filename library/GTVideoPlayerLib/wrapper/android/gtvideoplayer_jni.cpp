#include <assert.h>
#include <string.h>
#include <pthread.h>
#include <jni.h>
#include <unistd.h>
#include <android/native_window_jni.h>

#include "gtvideoplayer_jni.h"
#include "gtv_logger.h"
#include "gtv_player.h"

#include "gtv_raw_frame.h"
#include "gtv_com_def.h"

#include "gtvvideosegments_jni.h"
#include "gtvmp4remuxer_jni.h"
#include "gtvideodemuxer_jni.h"

#define JNI_CLASS_GTVIDEOPLAYER     "com/gtv/cloud/videoplayer/GTVideoPlayer"


#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
	
static JavaVM* g_jvm = NULL;

typedef struct player_fields_t {
    
    jclass clazz;

    jmethodID jmid_postEventFromNative;
	jmethodID jmid_postLogFromNative;
	
} player_fields_t;

static player_fields_t g_clazz;

typedef struct ST_WRAPPER_GTVIDEO_PLAYER_T {
    
    uint8_t             magic_s[4];
    HANDLE_GTV_PLAYER   player_handle;
    ANativeWindow       * native_window;
    void                * weak_this_target;
    uint8_t             magic_e[4];
    
} ST_WRAPPER_GTVIDEO_PLAYER;

static int wrapper_is_valid(ST_WRAPPER_GTVIDEO_PLAYER * wrapper)
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

static void wrapper_magic_init(ST_WRAPPER_GTVIDEO_PLAYER * wrapper)
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
//---------------------------------------------------------------------------
// call back function
//---------------------------------------------------------------------------

inline static void _postEventFromNative(JNIEnv *env, jobject weakThiz, jint what, jint arg1, jint arg2)
{
    (env)->CallStaticVoidMethod(g_clazz.clazz, g_clazz.jmid_postEventFromNative, weakThiz, what, arg1, arg2);
}

inline static void _postLogFromNative(JNIEnv *env, const char *logMsg)
{
    jstring strLogMsg =(env)->NewStringUTF(logMsg);
    (env)->CallStaticVoidMethod(g_clazz.clazz, g_clazz.jmid_postLogFromNative, strLogMsg );
	env->DeleteLocalRef(strLogMsg);
}

//static void postEventFromNative(void * target, void * player, int event, int arg1, int arg2);
//static void postLogFromNative(const char* logMsg);

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

    //GTV_ERROR("GetEnv Success");

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
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)target;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return;
    }
    
    if (wrapper->native_window == NULL) {
        GTV_ERROR("%d invalid native_window... \n", __LINE__);
        return;
    }
	
	HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
    	GTV_ERROR("%d invalid player... \n", __LINE__);
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

static void postEventFromNative(void * target, HANDLE_GTV_PLAYER player, int what, int arg1, int arg2)
{
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)target;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return;
    }
    
    jobject weak_thiz = (jobject) (wrapper->weak_this_target);

    int needsDetach = 0;
    JNIEnv* env = NULL;

    env = getJNIEnv(&needsDetach);

    if( env == NULL ) {
		return;
    }

    _postEventFromNative(env, weak_thiz, what, arg1, arg2);

    if(needsDetach) {
        (g_jvm)->DetachCurrentThread();
    }

    return;
}

static void postLogFromNative(const char* logMsg)
{
    int needsDetach = 0;
    JNIEnv* env = NULL;

    env = getJNIEnv(&needsDetach);

    if( env == NULL ) {
		return;
    }

    _postLogFromNative(env, logMsg);

    if(needsDetach) {
        (g_jvm)->DetachCurrentThread();
    }

    return;
}

//---------------------------------------------------------------------------
// jni function
//---------------------------------------------------------------------------

static long long
GTVideoPlayer_play(JNIEnv *env, jobject thiz, jstring url, jobject weak_this, jobject android_surface)
{
	const char * c_url = NULL;
	
	GTV_INFO("GTVideoPlayer_play started ... \n");
    
    c_url = env->GetStringUTFChars(url, NULL);
    
    if( c_url == NULL ) {
        GTV_ERROR("GTVideoPlayer_play params check ng (url or surface) ... \n");
        return 0;
    }
    
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER*)malloc(sizeof(ST_WRAPPER_GTVIDEO_PLAYER));
    
    memset((uint8_t*)wrapper, 0x00, sizeof(ST_WRAPPER_GTVIDEO_PLAYER));
    
    wrapper_magic_init(wrapper);
    
	wrapper->weak_this_target = env->NewGlobalRef(weak_this);
    
	wrapper->native_window = NULL;
	if (android_surface != NULL) {
		wrapper->native_window = ANativeWindow_fromSurface(env, android_surface);
	}
    
    wrapper->player_handle = gtv_player_open((char*)c_url, postEventFromNative, postDataFromNative, wrapper);
    
    if( c_url ) {
        env->ReleaseStringUTFChars(url, c_url);
    }
	
	return (long long)wrapper;
}

static void
GTVideoPlayer_close(JNIEnv *env, jobject thiz, jlong jwrapper) {
    
	GTV_INFO("GTVideoPlayer_close ... \n");
    
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return;
    }
    
	HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r != NULL ) {
        gtv_player_close(r);
        wrapper->player_handle = NULL;
    }
	
    if( wrapper->native_window != NULL ) {
        ANativeWindow_release(wrapper->native_window);
        wrapper->native_window = NULL;
    }

    if( wrapper->weak_this_target != NULL ) {
        env->DeleteGlobalRef((jobject)wrapper->weak_this_target);
        wrapper->weak_this_target = NULL;
    }

    free(wrapper);
    
    return;
} 

static void
GTVideoPlayer_pause(JNIEnv *env, jobject thiz, jlong jwrapper) {

	GTV_INFO("GTVideoPlayer_pause ... \n");

    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return;
    }

	HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
    	GTV_ERROR("%d invalid player... \n", __LINE__);
    }

    gtv_player_set_pause_mode(r, D_GTV_COM_FLAG_ON);

    return;
}

static void
GTVideoPlayer_resume(JNIEnv *env, jobject thiz, jlong jwrapper) {

	GTV_INFO("GTVideoPlayer_resume ... \n");

    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return;
    }

	HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
    	GTV_ERROR("%d invalid player... \n", __LINE__);
    }

    gtv_player_set_pause_mode(r, D_GTV_COM_FLAG_OFF);

    return;
}

static int
GTVideoPlayer_seekTo(JNIEnv *env, jobject thiz, jlong jwrapper, int milli) {
    
    GTV_INFO("GTVideoPlayer_seekTo %d ... \n", milli);
    
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid player... \n", __LINE__);
        return -2;
    }
    
    return gtv_player_seekto(r, milli);
}

static int
GTVideoPlayer_getDuration(JNIEnv *env, jobject thiz, jlong jwrapper) {
    
//    GTV_INFO("GTVideoPlayer_getDuration ... \n");
    
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid player... \n", __LINE__);
        return -2;
    }
    
    return gtv_player_get_duration(r);
}

static int
GTVideoPlayer_currentTimestamp(JNIEnv *env, jobject thiz, jlong jwrapper) {
    
//    GTV_INFO("GTVideoPlayer_currentTimestamp ... \n");
    
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid player... \n", __LINE__);
        return -2;
    }
    
    return gtv_player_current_timestamp(r);
}

static int
GTVideoPlayer_checkStreamStatus(JNIEnv *env, jobject thiz, jlong jwrapper) {
    
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid player... \n", __LINE__);
        return -2;
    }
    
    return gtv_player_check_status(r);
}

static int
GTVideoPlayer_setRange(JNIEnv *env, jobject thiz, jlong jwrapper, jint s, jint e) {
    
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid player... \n", __LINE__);
        return -2;
    }
    
    return gtv_player_set_range(r, (int64_t)s, (int64_t)e);
}

static int
GTVideoPlayer_pullAudioData(JNIEnv *env, jobject thiz, jlong jwrapper, jbyteArray jdata, jint size) {
    
    int ret = 0;
    uint8_t * out_data = NULL;
    
    ST_WRAPPER_GTVIDEO_PLAYER * wrapper = (ST_WRAPPER_GTVIDEO_PLAYER *)jwrapper;
    if( wrapper_is_valid(wrapper) == 0 ) {
        GTV_ERROR("%d invalid wrapper... \n", __LINE__);
        return -1;
    }
    
    HANDLE_GTV_PLAYER r = (HANDLE_GTV_PLAYER)(wrapper->player_handle);
    if( r == NULL ) {
        GTV_ERROR("%d invalid player... \n", __LINE__);
        return -2;
    }
    
    out_data = (uint8_t*)(env->GetByteArrayElements(jdata, 0));
    if( out_data == NULL ) {
        GTV_ERROR("GTVideoPlayer_pullAudioData out_data is null.\n");
        return -3;
    }
    
    ret = gtv_player_pull_audio(r, out_data, size);
    
    if( out_data ) {
        (env)->ReleaseByteArrayElements(jdata, (jbyte*)out_data, 0);
    }
    
    return ret;
}

static int
GTVideoPlayer_SetLogLevel(JNIEnv *env, jobject thiz, int level){
    
	gtv_logger_set_level(level);
    
    return 0;
} 

// ----------------------------------------------------------------------------

static JNINativeMethod g_methods[] = {
    { "_play",					"(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)J",      						(long long *) GTVideoPlayer_play },
    { "_close",                 "(J)V",      						(void *) GTVideoPlayer_close },
	{ "_pause",       			"(J)V",      						(void *) GTVideoPlayer_pause },
	{ "_resume",       			"(J)V",      						(void *) GTVideoPlayer_resume },
    { "_seekTo",                "(JI)I",                            (void *) GTVideoPlayer_seekTo },
    { "_getDuration",           "(J)I",                             (void *) GTVideoPlayer_getDuration },
    { "_checkStreamStatus",     "(J)I",                             (void *) GTVideoPlayer_checkStreamStatus },
    { "_setRange",              "(JII)I",                           (void *) GTVideoPlayer_setRange },
    { "_currentTimestamp",      "(J)I",                             (void *) GTVideoPlayer_currentTimestamp },
    { "_pullAudioData",         "(J[BI)I",                          (void *) GTVideoPlayer_pullAudioData },
	{ "_setLogLevel",			"(I)V",      						(void *) GTVideoPlayer_SetLogLevel }
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	gtv_logger_set_callback(postLogFromNative);
	
    JNIEnv* env = NULL;
    jclass clazz;

    g_jvm = vm;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

    // find class
    clazz = (env)->FindClass(JNI_CLASS_GTVIDEOPLAYER);
    if (clazz == NULL)
    {
        GTV_ERROR("Native registration unable to find class '%s'", JNI_CLASS_GTVIDEOPLAYER);
        return JNI_ERR;
    }
    g_clazz.clazz = (jclass)env->NewGlobalRef(clazz);

    // register native api
    if((env)->RegisterNatives(clazz, g_methods, NELEM(g_methods)) < 0)
    {
        //GTV_ERROR("ERROR: MediaPlayer native registration failed\n");
        return JNI_ERR;
    }

    // call from native
    g_clazz.jmid_postEventFromNative = (env)->GetStaticMethodID(clazz, "postEventFromNative", "(Ljava/lang/Object;III)V");
	g_clazz.jmid_postLogFromNative = (env)->GetStaticMethodID(clazz, "postLogFromNative", "(Ljava/lang/String;)V");
	
	JNI_OnLoad_SEGMENTS(vm, reserved);
	JNI_OnLoad_REMUXER(vm, reserved);
	JNI_OnLoad_DEMUXER(vm, reserved);
	
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved)
{
}

void  JNI_AttachThread()
{
	if (g_jvm != NULL) {
		
		JNIEnv* env = NULL;
		
		if ((g_jvm)->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK){
		
			int status = (g_jvm)->AttachCurrentThread(&env, 0);

			if (status < 0){
				//GTV_ERROR("failed to attach current thread");
			}
		}
	}

}

void JNI_detachThread()
{
	if (g_jvm != NULL) {
		(g_jvm)->DetachCurrentThread();
	}
}

