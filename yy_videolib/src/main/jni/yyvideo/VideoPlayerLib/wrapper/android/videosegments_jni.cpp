#include <assert.h>
#include <string.h>
#include <pthread.h>
#include <jni.h>
#include <unistd.h>
#include <android/native_window_jni.h>

#include "videosegments_jni.h"
#include "yy_logger.h"
#include "yy_segments.h"

#define JNI_CLASS_VIDEOSEGMENTS     "com/ybj366533/videolib/impl/YYVideoSegments"


#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
	
static JavaVM* g_jvm = NULL;


//---------------------------------------------------------------------------
// jni function
//---------------------------------------------------------------------------

static long long
YYVideoSegments_open(JNIEnv *env, jobject thiz, jstring path)
{
	const char * c_path = NULL;
	
	YY_INFO("YYVideoSegments_open started ... \n");
    
    c_path = env->GetStringUTFChars(path, NULL);
    
    if( c_path == NULL ) {
        YY_ERROR("YYVideoSegments_open params check ng (path) ... \n");
        return 0;
    }
    
	HANDLE_YY_SEGMENTS_REC handle = YY_segrec_open(c_path);
	
    
    if( c_path ) {
        env->ReleaseStringUTFChars(path, c_path);
    }
	
	return (long long)handle;
}

static void
YYVideoSegments_close(JNIEnv *env, jobject thiz, jlong jhandle) {
    
	YY_INFO("YYVideoSegments_close ... \n");
    
    HANDLE_YY_SEGMENTS_REC handle = (HANDLE_YY_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        YY_ERROR("%d invalid handle... \n", __LINE__);
        return;
    }
    
	YY_segrec_close(handle);
	
    return;
}

static int
YYVideoSegments_getSegmentCount(JNIEnv *env, jobject thiz, jlong jhandle) {
    
	YY_INFO("YYVideoSegments_close ... \n");
    
    HANDLE_YY_SEGMENTS_REC handle = (HANDLE_YY_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        YY_ERROR("%d invalid handle... \n", __LINE__);
        return -1;
    }
    
	return YY_segrec_get_count(handle);
} 

static int
YYVideoSegments_addVideo(JNIEnv *env, jobject thiz, jlong jhandle, jstring fileName, jint duration, jfloat speed) {
    
	YY_INFO("YYVideoSegments_addVideo started ... \n");
    
    HANDLE_YY_SEGMENTS_REC handle = (HANDLE_YY_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        YY_ERROR("%d invalid handle... \n", __LINE__);
        return -1;
    }
	
	const char * c_fileName = NULL;
    
    c_fileName = env->GetStringUTFChars(fileName, NULL);
    
    if( c_fileName == NULL ) {
        YY_ERROR("YYVideoSegments_addVideo params check ng (path) ... \n");
        return -1;
    }
	
	int res = YY_segrec_add_video(handle, c_fileName, duration, (float)speed);
	
	if( c_fileName ) {
        env->ReleaseStringUTFChars(fileName, c_fileName);
    }
    
	return res;
} 

static int
YYVideoSegments_removeLast(JNIEnv *env, jobject thiz, jlong jhandle) {
    
	YY_INFO("YYVideoSegments_removeLast ... \n");
    
    HANDLE_YY_SEGMENTS_REC handle = (HANDLE_YY_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        YY_ERROR("%d invalid handle... \n", __LINE__);
        return -1;
    }
    
	return YY_segrec_remove_last(handle);
}

static int
YYVideoSegments_getVideo(JNIEnv *env, jobject thiz, jlong jhandle, jint index, jobject clipInfo) {
    
	YY_INFO("YYVideoSegments_getVideo ... \n");
    
    HANDLE_YY_SEGMENTS_REC handle = (HANDLE_YY_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        YY_ERROR("%d invalid handle... \n", __LINE__);
        return -1;
    }
	
	char filename[512];
	memset(filename, 0x00, 512);
	int duration = 0;
    float speed = 0.0f;
	
	int res = YY_segrec_get_video(handle, index, filename, &duration, &speed);
	
	if (res > 0) {
		jclass YYVideoClipInfo_Class = env->GetObjectClass(clipInfo);
		if (YYVideoClipInfo_Class) {
			jfieldID fileNameID = env->GetFieldID(YYVideoClipInfo_Class, "fileName","Ljava/lang/String;");
			jstring strFileName = env->NewStringUTF(filename);
			jfieldID speedID = env->GetFieldID(YYVideoClipInfo_Class, "speed", "F");
			jfieldID durationID = env->GetFieldID(YYVideoClipInfo_Class, "duration", "I");
			env->SetIntField(clipInfo, durationID, duration);
            env->SetFloatField(clipInfo, speedID, speed);
			env->SetObjectField(clipInfo, fileNameID, strFileName);
			
		} else {
			res = -1;
		}
	}
	
    
	return res;
}

// ----------------------------------------------------------------------------

static JNINativeMethod g_methods[] = {
    { "_open",					"(Ljava/lang/String;)J",      		(long long *) YYVideoSegments_open },
    { "_close",                 "(J)V",      						(void *) YYVideoSegments_close },
	{"_getSegmentCount",		"(J)I",								(int *) YYVideoSegments_getSegmentCount },
	{"_addVideo",				"(JLjava/lang/String;IF)I",			(int *) YYVideoSegments_addVideo },
	{"_removeLast",				"(J)I",								(int *) YYVideoSegments_removeLast },
	{"_getVideo",				"(JILcom/ybj366533/videolib/recorder/VideoClipInfo;)I",			(int *) YYVideoSegments_getVideo }
};

jint JNI_OnLoad_SEGMENTS(JavaVM *vm, void *reserved)
{
	//YY_logger_set_callback(postLogFromNative);
	
    JNIEnv* env = NULL;
    jclass clazz;

    g_jvm = vm;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

    // find class
    clazz = (env)->FindClass(JNI_CLASS_VIDEOSEGMENTS);
    if (clazz == NULL)
    {
        YY_ERROR("Native registration unable to find class '%s'", JNI_CLASS_VIDEOSEGMENTS);
        return JNI_ERR;
    }
    //g_clazz.clazz = (jclass)env->NewGlobalRef(clazz);

    // register native api
    if((env)->RegisterNatives(clazz, g_methods, NELEM(g_methods)) < 0)
    {
        //YY_ERROR("ERROR: MediaPlayer native registration failed\n");
        return JNI_ERR;
    }

    // call from native
    //g_clazz.jmid_postEventFromNative = (env)->GetStaticMethodID(clazz, "postEventFromNative", "(Ljava/lang/Object;III)V");
	//g_clazz.jmid_postLogFromNative = (env)->GetStaticMethodID(clazz, "postLogFromNative", "(Ljava/lang/String;)V");
	
    return JNI_VERSION_1_4;
}

void JNI_OnUnload_SEGMENTS(JavaVM *jvm, void *reserved)
{
}
