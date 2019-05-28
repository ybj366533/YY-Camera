#include <assert.h>
#include <string.h>
#include <pthread.h>
#include <jni.h>
#include <unistd.h>
#include <android/native_window_jni.h>

#include "mp4remuxer_jni.h"
#include "yy_logger.h"
#include "yy_remuxer.h"
#include "yy_mp4Remux.h"
#include "yy_transcode.h"

#include "libimg2webp.h"

//#include "YY_raw_frame.h"
//#include "YY_com_def.h"

#define JNI_CLASS_YY_MP4Help     "com/ybj366533/videolib/impl/YYMP4Help"


#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
	
static JavaVM* g_jvm = NULL;


//---------------------------------------------------------------------------
// jni function
//---------------------------------------------------------------------------

static int
YY_MP4Help_Mp4VideoClipsMerge(JNIEnv *env, jclass clazz, jobjectArray fileListArr, jstring outputFilePath, jstring outputFilePathRev, jstring tempFolder) {
    
	YY_ERROR("YY_MP4Help_Mp4VideoClipsMerge ... \n");
	
	const char * c_outputFilePath = NULL;
	const char * c_outputFilePathRev = NULL;
	const char * c_tempFolder = NULL;
    
    c_outputFilePath = env->GetStringUTFChars(outputFilePath, NULL);
    
    if( c_outputFilePath == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4VideoClipsMerge params check ng (path) ... \n");
        return -1;
    }
	
	c_outputFilePathRev = env->GetStringUTFChars(outputFilePathRev, NULL);
    
    if( c_outputFilePathRev == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4VideoClipsMerge params check ng (path) ... \n");
        return -1;
    }
	
	c_tempFolder = env->GetStringUTFChars(tempFolder, NULL);
    
    if( c_tempFolder == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4VideoClipsMerge params check ng (path) ... \n");
        return -1;
    }
	
	jsize len = env->GetArrayLength(fileListArr);
	
	char **c_fileList = (char **) malloc(len*sizeof(char *));
	
	
	for ( int i = 0; i < len; ++i) {
		jstring fileName = (jstring)env->GetObjectArrayElement(fileListArr, i);
		const char* temp_str = (char *)env->GetStringUTFChars(fileName, 0);
        c_fileList[i] = (char *) malloc(strlen(temp_str)+1);
		strcpy(c_fileList[i],temp_str);
		env->ReleaseStringUTFChars(fileName, temp_str);
		env->DeleteLocalRef(fileName); 
		
		//YY_ERROR("YY_MP4Help_Mp4VideoClipsMerge params check ng (path %s ) ... qqqqqqq\n", c_fileList[i]);
	}
	
	    // 输出文件路径
    char temp_file[MAX_PATH] = {0};
    sprintf(temp_file, "%st2.mp4", c_tempFolder);
	
	YY_mp4_clips_merge(c_fileList, len, c_outputFilePath);
	
	//if( c_outputFilePathRev != NULL && c_tempFolder != NULL){
	//	YY_mp4_video_reverse(c_outputFilePath, temp_file);		//todo
	//	YY_mp4_audio_video_merge(c_outputFilePath, temp_file, c_outputFilePathRev);
	//}

	//YY_ERROR("YY_MP4Help_Mp4VideoClipsMerge params check ng (path %s ) ... qqqqqqq\n", temp_file);
	
	
	for (int i = 0; i < len; ++i) {
		free(c_fileList[i]);
	}
	
	free(c_fileList);
	
    if( c_outputFilePath ) {
        env->ReleaseStringUTFChars(outputFilePath, c_outputFilePath);
    }
	
	if( c_outputFilePathRev ) {
        env->ReleaseStringUTFChars(outputFilePathRev, c_outputFilePathRev);
    }
	
	if( c_tempFolder ) {
        env->ReleaseStringUTFChars(tempFolder, c_tempFolder);
    }
	
	return 0;
}

static int
YY_MP4Help_Mp4AudioVideoMerge(JNIEnv *env, jclass clazz, jstring audioFilePath, jstring videoFilePath, jstring outputFilePath) {
    
	YY_ERROR("YY_MP4Help_Mp4AudioVideoMerge ... \n");

	const char * c_videoFilePath = NULL;
	const char * c_audioFilePath = NULL;
	
	const char * c_outputFilePath = NULL;

	c_videoFilePath = env->GetStringUTFChars(videoFilePath, NULL);
    
    if( c_videoFilePath == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4AudioVideoMerge params check ng (path) ... \n");
        return -1;
    }
	
	c_audioFilePath = env->GetStringUTFChars(audioFilePath, NULL);
    
    if( c_audioFilePath == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4AudioVideoMerge params check ng (path) ... \n");
        return -1;
    }
    
    c_outputFilePath = env->GetStringUTFChars(outputFilePath, NULL);
    
    if( c_outputFilePath == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4AudioVideoMerge params check ng (path) ... \n");
        return -1;
    }
	
	
	YY_mp4_audio_video_merge(c_audioFilePath, c_videoFilePath, c_outputFilePath);
	
	
    if( c_audioFilePath ) {
        env->ReleaseStringUTFChars(audioFilePath, c_audioFilePath);
    }
	
    if( c_videoFilePath ) {
        env->ReleaseStringUTFChars(videoFilePath, c_videoFilePath);
    }
	
    if( c_outputFilePath ) {
        env->ReleaseStringUTFChars(outputFilePath, c_outputFilePath);
    }
	
	return 0;
}

static int
YY_MP4Help_MP4FileSetCover(JNIEnv *env, jclass thiz, jstring filenameFrom, jstring filenameTo, jstring filenameTag)
{
    const char * c_file_name_from = NULL;
	const char * c_file_name_to = NULL;
	const char * c_file_name_tag = NULL;
    int ret = 0;
    
    c_file_name_from = env->GetStringUTFChars(filenameFrom, NULL);
	
	
    
    if( c_file_name_from == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileSetCover params check ng (filename) ... \n");
        return -1;
    }
	
	c_file_name_to = env->GetStringUTFChars(filenameTo, NULL);
    
    if( c_file_name_to == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileSetCover params check ng (filename) ... \n");
        return -1;
    }
    
	c_file_name_tag = env->GetStringUTFChars(filenameTag, NULL);
    
    if( c_file_name_tag == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileSetCover params check ng (filename) ... \n");
        return -1;
    }
    
	//YY_ERROR("YY_MP4Help_MP4FileSetCover params check ng (filename)%s %s %s ... \n", c_file_name_from, c_file_name_to, c_file_name_tag);
	
    ret =  mp4_set_cover(c_file_name_from, c_file_name_to, c_file_name_tag);
    
    //LOGD("YY_MP4Help_MP4FileSetCover finished %d ... ... \n", ret);
    
    if( c_file_name_from ) {
        env->ReleaseStringUTFChars(filenameFrom, c_file_name_from);
    }
	
	if( c_file_name_to ) {
        env->ReleaseStringUTFChars(filenameTo, c_file_name_to);
    }
	
	if( c_file_name_tag ) {
        env->ReleaseStringUTFChars(filenameTag, c_file_name_tag);
    }
    
	
    return ret;
}

static int
YY_MP4Help_Mp4VideoExtractFrame(JNIEnv *env, jclass clazz, jstring inFilePath, jstring outDirPath, jstring outFilePrefix, jint startTime, jint endTime,
jintArray dataTimeStamp, jint dataNum, jint imgFormat, jfloat scale) {
    
	YY_ERROR("YY_MP4Help_Mp4VideoExtractFrame ... \n");
	
	const char * c_inFilePath = NULL;
	const char * c_outDirPath = NULL;
	const char * c_outFilePrefix = NULL;
	int *c_dataTimeStamp = NULL;
	
	if ((imgFormat != IMG_FORMAT_JPEG) &&(imgFormat != IMG_FORMAT_YUV)) {
		return -1;
	}
    
    c_inFilePath = env->GetStringUTFChars(inFilePath, NULL);
    
    if( c_inFilePath == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4VideoExtractFrame params check ng (path) ... \n");
        return -1;
    }
	
    c_outDirPath = env->GetStringUTFChars(outDirPath, NULL);
    
    if( c_outDirPath == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4VideoExtractFrame params check ng (path) ... \n");
        return -1;
    }
	
	c_outFilePrefix = env->GetStringUTFChars(outFilePrefix, NULL);
    
    if( c_outFilePrefix == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4VideoExtractFrame params check ng (path) ... \n");
        return -1;
    }
	
	c_dataTimeStamp = (int*)(env->GetIntArrayElements(dataTimeStamp, 0));
    if( c_dataTimeStamp == NULL ) {
        YY_ERROR("YY_MP4Help_Mp4VideoExtractFrame out_data is null.\n");
        return -4;
    }
	
	
	int out_data_num = 0;
	
	YY_mp4_video_extract_frame(c_inFilePath, imgFormat, c_outDirPath, c_outFilePrefix, startTime, endTime, c_dataTimeStamp, dataNum, &out_data_num, scale);

	
    if( c_inFilePath ) {
        env->ReleaseStringUTFChars(inFilePath, c_inFilePath);
    }
	
	if( c_outDirPath ) {
        env->ReleaseStringUTFChars(outDirPath, c_outDirPath);
    }
	
	if( c_outFilePrefix ) {
        env->ReleaseStringUTFChars(outFilePrefix, c_outFilePrefix);
    }
	
	if( c_dataTimeStamp ) {
        (env)->ReleaseIntArrayElements(dataTimeStamp, (jint*)c_dataTimeStamp, 0);
    }
	
    
	return out_data_num;
}

static int
YY_MP4Help_AudioExtractWaveForm(JNIEnv *env, jclass clazz, jstring inFilePath, jint startTime, jint endTime, jfloatArray audioData, jint dataNum) {
    
	YY_ERROR("YY_MP4Help_AudioExtractWaveForm ... \n");
	
	const char * c_inFilePath = NULL;
	float *c_audioData = NULL;
    
    c_inFilePath = env->GetStringUTFChars(inFilePath, NULL);
    
    if( c_inFilePath == NULL ) {
        YY_ERROR("YY_MP4Help_AudioExtractWaveForm params check ng (path) ... \n");
        return -1;
    }
	
	c_audioData = (float*)(env->GetFloatArrayElements(audioData, 0));
    if( c_audioData == NULL ) {
        YY_ERROR("YY_MP4Help_AudioExtractWaveForm out_data is null.\n");
        return -4;
    }
	
	
	int out_data_num = 0;
	
	
	mp4_audio_extract_waveform(c_inFilePath, startTime, endTime, c_audioData, dataNum, &out_data_num);

	
    if( c_inFilePath ) {
        env->ReleaseStringUTFChars(inFilePath, c_inFilePath);
    }
	
	
	if( c_audioData ) {
        (env)->ReleaseFloatArrayElements(audioData, (float*)c_audioData, 0);
    }
	
    
	return out_data_num;
}

static int
YY_MP4Help_ImgToWebp(JNIEnv *env, jclass clazz, jobjectArray fileListArr, jstring outputFilePath) {
    
	YY_ERROR("YY_MP4Help_ImgToWebp ... \n");
	
	const char * c_outputFilePath = NULL;
    
    c_outputFilePath = env->GetStringUTFChars(outputFilePath, NULL);
    
    if( c_outputFilePath == NULL ) {
        YY_ERROR("YY_MP4Help_ImgToWebp params check ng (path) ... \n");
        return -1;
    }
	
	
	jsize len = env->GetArrayLength(fileListArr);
	
	char **c_fileList = (char **) malloc(len*sizeof(char *));
	
	for ( int i = 0; i < len; ++i) {
		jstring fileName = (jstring)env->GetObjectArrayElement(fileListArr, i);
		const char* temp_str = (char *)env->GetStringUTFChars(fileName, 0);
        c_fileList[i] = (char *) malloc(strlen(temp_str)+1);
		strcpy(c_fileList[i],temp_str);
		env->ReleaseStringUTFChars(fileName, temp_str);
		env->DeleteLocalRef(fileName); 
		
		//YY_ERROR("YY_MP4Help_ImgToWebp params check ng (path %s ) ... \n", c_fileList[i]);
	}
	
	int ret = img2webp((const char**)c_fileList, len, c_outputFilePath);
	
	//
	for (int i = 0; i < len; ++i) {
		free(c_fileList[i]);
	}
	
	free(c_fileList);
	
	
    if( c_outputFilePath ) {
        env->ReleaseStringUTFChars(outputFilePath, c_outputFilePath);
    }
	
	
	return 0;
}

typedef struct ST_X264_TRANSCODE_CALLBACK_WRAPPER_T {
    
	JNIEnv *env;
	jobject listener;
	jmethodID jmid_onProgress;
    //jclass clazz;

    //jmethodID jmid_postEventFromNative;
	//jmethodID jmid_postLogFromNative;
	
} ST_X264_TRANSCODE_CALLBACK_WRAPPER;

int OnProgressCallback(void* target, int progress)
{
	/*
	jobject listener = (jobject)target;
	jclass listener_Class = env->GetObjectClass(listener);
	if (listener_Class) {
		jmethodID onProgress = (env)->GetMethodID(listener_Class, "onProgress",
            "(I)V");
		//jfieldID fileNameID = env->GetFieldID(YYVideoClipInfo_Class, "fileName","Ljava/lang/String;");
		//jstring strFileName = env->NewStringUTF(filename);
		//jfieldID speedID = env->GetFieldID(YYVideoClipInfo_Class, "speed", "F");
		//jfieldID durationID = env->GetFieldID(YYVideoClipInfo_Class, "duration", "I");
		//env->SetIntField(clipInfo, durationID, duration);
		//env->SetFloatField(clipInfo, speedID, speed);
		//env->SetObjectField(clipInfo, fileNameID, strFileName);
		
		if(onProgress) {
			env->CallObjectMethod(listener, onProgress, 100);
		}
		
		
	} else {
		//return -1;
	}
	*/
	
	ST_X264_TRANSCODE_CALLBACK_WRAPPER *wrapper = (ST_X264_TRANSCODE_CALLBACK_WRAPPER*)target;
	if(wrapper->listener != NULL) {
		return wrapper->env->CallIntMethod(wrapper->listener, wrapper->jmid_onProgress, progress);
		
	}
	return 0;
	
	
}
static int
YY_MP4Help_MP4FileTranscodeByX264(JNIEnv *env, jclass thiz, jstring filenameFrom, jstring filenameTo, jint gopSize, jobject listener)
{
    const char * c_file_name_from = NULL;
	const char * c_file_name_to = NULL;
    int ret = 0;
    
    c_file_name_from = env->GetStringUTFChars(filenameFrom, NULL);
	
    if( c_file_name_from == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileTranscodeByX264 params check ng (filename) ... \n");
        return -1;
    }
	
	c_file_name_to = env->GetStringUTFChars(filenameTo, NULL);
    
    if( c_file_name_to == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileTranscodeByX264 params check ng (filename) ... \n");
        return -1;
    }
	
	
	// todo  reference count ????
	ST_X264_TRANSCODE_CALLBACK_WRAPPER * wrapper = (ST_X264_TRANSCODE_CALLBACK_WRAPPER*)malloc(sizeof(ST_X264_TRANSCODE_CALLBACK_WRAPPER));
    
    memset((uint8_t*)wrapper, 0x00, sizeof(ST_X264_TRANSCODE_CALLBACK_WRAPPER));
	wrapper->env = env;
	wrapper->listener = listener;
	
	if(listener != NULL) {
		jclass listener_Class = env->GetObjectClass(listener);
		if (listener_Class) {
			wrapper->jmid_onProgress = (env)->GetMethodID(listener_Class, "onProgress",
				"(I)I");
			//jfieldID fileNameID = env->GetFieldID(YYVideoClipInfo_Class, "fileName","Ljava/lang/String;");
			//jstring strFileName = env->NewStringUTF(filename);
			//jfieldID speedID = env->GetFieldID(YYVideoClipInfo_Class, "speed", "F");
			//jfieldID durationID = env->GetFieldID(YYVideoClipInfo_Class, "duration", "I");
			//env->SetIntField(clipInfo, durationID, duration);
			//env->SetFloatField(clipInfo, speedID, speed);
			//env->SetObjectField(clipInfo, fileNameID, strFileName);
			
			//if(onProgress) {
			//	env->CallObjectMethod(listener, onProgress, 100);
			//}
			
			//todo pankong
			if (wrapper->jmid_onProgress == NULL) {
				return -1;
			}
			
			
		} else {
			return -1;
		}
	}
	
	
    
	//YY_ERROR("YY_MP4Help_MP4FileSetCover params check ng (filename)%s %s %s ... \n", c_file_name_from, c_file_name_to, c_file_name_tag);
	
    ret =  mp4_transcode_by_x264(c_file_name_from, c_file_name_to, gopSize,OnProgressCallback, wrapper);
    
    //LOGD("YY_MP4Help_MP4FileSetCover finished %d ... ... \n", ret);
    
    if( c_file_name_from ) {
        env->ReleaseStringUTFChars(filenameFrom, c_file_name_from);
    }
	
	if( c_file_name_to ) {
        env->ReleaseStringUTFChars(filenameTo, c_file_name_to);
    }
	
	free(wrapper);
    
	
    return ret;
}

// video transcode
static int
YY_MP4Help_MP4FileImportVideoByX264_video(JNIEnv *env, jclass thiz, jstring filenameFrom, jstring filenameTo, jint gopSize, jobject listener, jint width, jint height, jint crop_flag, jint start_time, jint end_time)
{
    const char * c_file_name_from = NULL;
	const char * c_file_name_to = NULL;
    int ret = 0;
    
    c_file_name_from = env->GetStringUTFChars(filenameFrom, NULL);
	
    if( c_file_name_from == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileImportVideoByX264 params check ng (filename) ... \n");
        return -1;
    }
	
	c_file_name_to = env->GetStringUTFChars(filenameTo, NULL);
    
    if( c_file_name_to == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileImportVideoByX264 params check ng (filename) ... \n");
        return -1;
    }
	
	
	// todo  reference count ????
	ST_X264_TRANSCODE_CALLBACK_WRAPPER * wrapper = (ST_X264_TRANSCODE_CALLBACK_WRAPPER*)malloc(sizeof(ST_X264_TRANSCODE_CALLBACK_WRAPPER));
    
    memset((uint8_t*)wrapper, 0x00, sizeof(ST_X264_TRANSCODE_CALLBACK_WRAPPER));
	wrapper->env = env;
	wrapper->listener = listener;
	
	if(listener != NULL) {
		jclass listener_Class = env->GetObjectClass(listener);
		if (listener_Class) {
			wrapper->jmid_onProgress = (env)->GetMethodID(listener_Class, "onProgress",
				"(I)I");
			//jfieldID fileNameID = env->GetFieldID(YYVideoClipInfo_Class, "fileName","Ljava/lang/String;");
			//jstring strFileName = env->NewStringUTF(filename);
			//jfieldID speedID = env->GetFieldID(YYVideoClipInfo_Class, "speed", "F");
			//jfieldID durationID = env->GetFieldID(YYVideoClipInfo_Class, "duration", "I");
			//env->SetIntField(clipInfo, durationID, duration);
			//env->SetFloatField(clipInfo, speedID, speed);
			//env->SetObjectField(clipInfo, fileNameID, strFileName);
			
			//if(onProgress) {
			//	env->CallObjectMethod(listener, onProgress, 100);
			//}
			
			//todo pankong
			if (wrapper->jmid_onProgress == NULL) {
				return -1;
			}
			
			
		} else {
			return -1;
		}
	}
	
	
    
	//YY_ERROR("YY_MP4Help_MP4FileSetCover params check ng (filename)%s %s %s ... \n", c_file_name_from, c_file_name_to, c_file_name_tag);
	
    ret =  mp4_video_transcode_video(c_file_name_from, c_file_name_to, gopSize,OnProgressCallback, wrapper, width ,height, (crop_flag!=0?true:false), start_time, end_time);
    
    //LOGD("YY_MP4Help_MP4FileSetCover finished %d ... ... \n", ret);
    
    if( c_file_name_from ) {
        env->ReleaseStringUTFChars(filenameFrom, c_file_name_from);
    }
	
	if( c_file_name_to ) {
        env->ReleaseStringUTFChars(filenameTo, c_file_name_to);
    }
	
	free(wrapper);
    
	
    return ret;
}

static int
YY_MP4Help_MP4FileImportVideoByX264_audio(JNIEnv *env, jclass thiz, jstring filenameFrom, jstring filenameTo, jint crop_flag, jint start_time, jint end_time)
{
    const char * c_file_name_from = NULL;
	const char * c_file_name_to = NULL;
    int ret = 0;
    
    c_file_name_from = env->GetStringUTFChars(filenameFrom, NULL);
	
    if( c_file_name_from == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileImportVideoByX264_audio params check ng (filename) ... \n");
        return -1;
    }
	
	c_file_name_to = env->GetStringUTFChars(filenameTo, NULL);
    
    if( c_file_name_to == NULL ) {
        YY_ERROR("YY_MP4Help_MP4FileImportVideoByX264_audio params check ng (filename) ... \n");
        return -1;
    }
	
	

    ret =  mp4_video_transcode_audio(c_file_name_from, c_file_name_to, (crop_flag!=0?true:false), start_time, end_time);
    
    //LOGD("YY_MP4Help_MP4FileSetCover finished %d ... ... \n", ret);
    
    if( c_file_name_from ) {
        env->ReleaseStringUTFChars(filenameFrom, c_file_name_from);
    }
	
	if( c_file_name_to ) {
        env->ReleaseStringUTFChars(filenameTo, c_file_name_to);
    }
	
    
	
    return ret;
}

static int
YY_MP4Help_GetVideoInfo(JNIEnv *env, jclass clazz, jstring inFilePath, jintArray duration, jintArray video_size) {
    
	YY_ERROR("YY_MP4Help_GetVideoInfo ... \n");
	
	const char * c_inFilePath = NULL;
	int *c_duration = NULL;
	int *c_video_size = NULL;
    
    c_inFilePath = env->GetStringUTFChars(inFilePath, NULL);
    
    if( c_inFilePath == NULL ) {
        YY_ERROR("YY_MP4Help_GetVideoInfo params check ng (path) ... \n");
        return -1;
    }
	
	c_duration = (int*)(env->GetIntArrayElements(duration, 0));
    if( c_duration == NULL ) {
        YY_ERROR("YY_MP4Help_GetVideoInfo out_data is null.\n");
        return -4;
    }
	
	c_video_size = (int*)(env->GetIntArrayElements(video_size, 0));
    if( c_video_size == NULL ) {
        YY_ERROR("YY_MP4Help_GetVideoInfo out_data is null.\n");
        return -4;
    }
	
	
	int width,height;
	
	
	YY_mp4_video_get_info(c_inFilePath, c_duration, c_duration+1, &width, &height);
	c_video_size[0] = width;
	c_video_size[1] = height;

	
    if( c_inFilePath ) {
        env->ReleaseStringUTFChars(inFilePath, c_inFilePath);
    }
	
	
	if( c_duration ) {
        (env)->ReleaseIntArrayElements(duration, (int*)c_duration, 0);
    }
	
	if( c_video_size ) {
        (env)->ReleaseIntArrayElements(video_size, (int*)c_video_size, 0);
    }
	
	if(width == 0) {
		return -1;
	}
    
	return 0;
}

// ----------------------------------------------------------------------------

static JNINativeMethod g_methods[] = {
    { "_Mp4VideoClipsMerge",					"([Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I",      		(int *) YY_MP4Help_Mp4VideoClipsMerge },
	{ "_Mp4AudioVideoMerge",					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I",      		(int *) YY_MP4Help_Mp4AudioVideoMerge },
	{"_MP4FileSetCoverJNI", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void*)YY_MP4Help_MP4FileSetCover},
	{"_Mp4VideoExtractFrame", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II[IIIF)I", (int*)YY_MP4Help_Mp4VideoExtractFrame},
	{"_AudioExtractWaveForm", "(Ljava/lang/String;II[FI)I", (int*)YY_MP4Help_AudioExtractWaveForm},
	{ "_ImgToWebp",					"([Ljava/lang/String;Ljava/lang/String;)I",      		(int *) YY_MP4Help_ImgToWebp },
	{"_MP4FileTranscodeByX264", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;)I", (int*)YY_MP4Help_MP4FileTranscodeByX264},
	{"_MP4FileImportVideoByX264_video", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;IIIII)I", (int*)YY_MP4Help_MP4FileImportVideoByX264_video},
	{"_MP4FileImportVideoByX264_audio", "(Ljava/lang/String;Ljava/lang/String;III)I", (int*)YY_MP4Help_MP4FileImportVideoByX264_audio},
	{"_GetVideoInfo", "(Ljava/lang/String;[I[I)I", (int*)YY_MP4Help_GetVideoInfo}

};

jint JNI_OnLoad_REMUXER(JavaVM *vm, void *reserved)
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
    clazz = (env)->FindClass(JNI_CLASS_YY_MP4Help);
    if (clazz == NULL)
    {
        YY_ERROR("Native registration unable to find class '%s'", JNI_CLASS_YY_MP4Help);
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

void JNI_OnUnload_REMUXER(JavaVM *jvm, void *reserved)
{
}
