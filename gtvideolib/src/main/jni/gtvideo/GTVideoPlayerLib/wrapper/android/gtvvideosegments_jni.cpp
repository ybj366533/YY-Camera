#include <assert.h>
#include <string.h>
#include <pthread.h>
#include <jni.h>
#include <unistd.h>
#include <android/native_window_jni.h>

#include "gtvvideosegments_jni.h"
#include "gtv_logger.h"
#include "gtv_segments.h"

//#include "gtv_raw_frame.h"
//#include "gtv_com_def.h"

#define JNI_CLASS_GTVVIDEOSEGMENTS     "com/gtv/cloud/impl/GTVVideoSegments"


#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
	
static JavaVM* g_jvm = NULL;


//---------------------------------------------------------------------------
// jni function
//---------------------------------------------------------------------------

static long long
GTVVideoSegments_open(JNIEnv *env, jobject thiz, jstring path)
{
	const char * c_path = NULL;
	
	GTV_INFO("GTVVideoSegments_open started ... \n");
    
    c_path = env->GetStringUTFChars(path, NULL);
    
    if( c_path == NULL ) {
        GTV_ERROR("GTVVideoSegments_open params check ng (path) ... \n");
        return 0;
    }
    
	HANDLE_GTV_SEGMENTS_REC handle = gtv_segrec_open(c_path);
	
    
    if( c_path ) {
        env->ReleaseStringUTFChars(path, c_path);
    }
	
	return (long long)handle;
}

static void
GTVVideoSegments_close(JNIEnv *env, jobject thiz, jlong jhandle) {
    
	GTV_INFO("GTVVideoSegments_close ... \n");
    
    HANDLE_GTV_SEGMENTS_REC handle = (HANDLE_GTV_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        GTV_ERROR("%d invalid handle... \n", __LINE__);
        return;
    }
    
	gtv_segrec_close(handle);
	
    return;
}

static int
GTVVideoSegments_getSegmentCount(JNIEnv *env, jobject thiz, jlong jhandle) {
    
	GTV_INFO("GTVVideoSegments_close ... \n");
    
    HANDLE_GTV_SEGMENTS_REC handle = (HANDLE_GTV_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        GTV_ERROR("%d invalid handle... \n", __LINE__);
        return -1;
    }
    
	return gtv_segrec_get_count(handle);
} 

static int
GTVVideoSegments_addVideo(JNIEnv *env, jobject thiz, jlong jhandle, jstring fileName, jint duration, jfloat speed) {
    
	GTV_INFO("GTVVideoSegments_addVideo started ... \n");
    
    HANDLE_GTV_SEGMENTS_REC handle = (HANDLE_GTV_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        GTV_ERROR("%d invalid handle... \n", __LINE__);
        return -1;
    }
	
	const char * c_fileName = NULL;
    
    c_fileName = env->GetStringUTFChars(fileName, NULL);
    
    if( c_fileName == NULL ) {
        GTV_ERROR("GTVVideoSegments_addVideo params check ng (path) ... \n");
        return -1;
    }
	
	int res = gtv_segrec_add_video(handle, c_fileName, duration, (float)speed);
	
	if( c_fileName ) {
        env->ReleaseStringUTFChars(fileName, c_fileName);
    }
    
	return res;
} 

static int
GTVVideoSegments_removeLast(JNIEnv *env, jobject thiz, jlong jhandle) {
    
	GTV_INFO("GTVVideoSegments_removeLast ... \n");
    
    HANDLE_GTV_SEGMENTS_REC handle = (HANDLE_GTV_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        GTV_ERROR("%d invalid handle... \n", __LINE__);
        return -1;
    }
    
	return gtv_segrec_remove_last(handle);
}

static int
GTVVideoSegments_getVideo(JNIEnv *env, jobject thiz, jlong jhandle, jint index, jobject clipInfo) {
    
	GTV_INFO("GTVVideoSegments_getVideo ... \n");
    
    HANDLE_GTV_SEGMENTS_REC handle = (HANDLE_GTV_SEGMENTS_REC)jhandle;
    if( handle == 0 ) {
        GTV_ERROR("%d invalid handle... \n", __LINE__);
        return -1;
    }
	
	char filename[512];
	memset(filename, 0x00, 512);
	int duration = 0;
    float speed = 0.0f;
	
	int res = gtv_segrec_get_video(handle, index, filename, &duration, &speed);
	
	if (res > 0) {
		jclass GTVVideoClipInfo_Class = env->GetObjectClass(clipInfo);
		if (GTVVideoClipInfo_Class) {
			jfieldID fileNameID = env->GetFieldID(GTVVideoClipInfo_Class, "fileName","Ljava/lang/String;");
			jstring strFileName = env->NewStringUTF(filename);
			jfieldID speedID = env->GetFieldID(GTVVideoClipInfo_Class, "speed", "F");
			jfieldID durationID = env->GetFieldID(GTVVideoClipInfo_Class, "duration", "I");
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
    { "_open",					"(Ljava/lang/String;)J",      		(long long *) GTVVideoSegments_open },
    { "_close",                 "(J)V",      						(void *) GTVVideoSegments_close },
	{"_getSegmentCount",		"(J)I",								(int *) GTVVideoSegments_getSegmentCount },
	{"_addVideo",				"(JLjava/lang/String;IF)I",			(int *) GTVVideoSegments_addVideo },
	{"_removeLast",				"(J)I",								(int *) GTVVideoSegments_removeLast },
	{"_getVideo",				"(JILcom/gtv/cloud/recorder/GTVVideoClipInfo;)I",			(int *) GTVVideoSegments_getVideo }
};

jint JNI_OnLoad_SEGMENTS(JavaVM *vm, void *reserved)
{
	//gtv_logger_set_callback(postLogFromNative);
	
    JNIEnv* env = NULL;
    jclass clazz;

    g_jvm = vm;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

    // find class
    clazz = (env)->FindClass(JNI_CLASS_GTVVIDEOSEGMENTS);
    if (clazz == NULL)
    {
        GTV_ERROR("Native registration unable to find class '%s'", JNI_CLASS_GTVVIDEOSEGMENTS);
        return JNI_ERR;
    }
    //g_clazz.clazz = (jclass)env->NewGlobalRef(clazz);

    // register native api
    if((env)->RegisterNatives(clazz, g_methods, NELEM(g_methods)) < 0)
    {
        //GTV_ERROR("ERROR: MediaPlayer native registration failed\n");
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
