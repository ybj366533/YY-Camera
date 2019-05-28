////////////////////////////////////////////////////////////////////////////////
///
/// Example Interface class for SoundTouch native compilation
///
/// Author        : Copyright (c) Olli Parviainen
/// Author e-mail : oparviai 'at' iki.fi
/// WWW           : http://www.surina.net
///
////////////////////////////////////////////////////////////////////////////////
//
// $Id: soundtouch-jni.cpp 212 2015-05-15 10:22:36Z oparviai $
//
////////////////////////////////////////////////////////////////////////////////

#include <jni.h>
#include <android/log.h>
#include <stdexcept>
#include <string>

using namespace std;

#include "./include/SoundTouch.h"
#include "./SoundStretch/WavFile.h"

#include "sonic.h"

#define LOGV(...)   __android_log_print((int)ANDROID_LOG_ERROR, "SOUNDTOUCH", __VA_ARGS__)
//#define LOGV(...)


// String for keeping possible c++ exception error messages. Notice that this isn't
// thread-safe but it's expected that exceptions are special situations that won't
// occur in several threads in parallel.
static string _errMsg = "";


#define DLL_PUBLIC __attribute__ ((visibility ("default")))
#define BUFF_SIZE 4096


using namespace soundtouch;


// Set error message to return
static void _setErrmsg(const char *msg)
{
	_errMsg = msg;
}


#ifdef _OPENMP

#include <pthread.h>
extern pthread_key_t gomp_tls_key;
static void * _p_gomp_tls = NULL;

/// Function to initialize threading for OpenMP.
///
/// This is a workaround for bug in Android NDK v10 regarding OpenMP: OpenMP works only if
/// called from the Android App main thread because in the main thread the gomp_tls storage is
/// properly set, however, Android does not properly initialize gomp_tls storage for other threads.
/// Thus if OpenMP routines are invoked from some other thread than the main thread,
/// the OpenMP routine will crash the application due to NULL pointer access on uninitialized storage.
///
/// This workaround stores the gomp_tls storage from main thread, and copies to other threads.
/// In order this to work, the Application main thread needws to call at least "getVersionString"
/// routine.
static int _init_threading(bool warn)
{
	void *ptr = pthread_getspecific(gomp_tls_key);
	LOGV("JNI thread-specific TLS storage %ld", (long)ptr);
	if (ptr == NULL)
	{
		LOGV("JNI set missing TLS storage to %ld", (long)_p_gomp_tls);
		pthread_setspecific(gomp_tls_key, _p_gomp_tls);
	}
	else
	{
		LOGV("JNI store this TLS storage");
		_p_gomp_tls = ptr;
	}
	// Where critical, show warning if storage still not properly initialized
	if ((warn) && (_p_gomp_tls == NULL))
	{
		_setErrmsg("Error - OpenMP threading not properly initialized: Call SoundTouch.getVersionString() from the App main thread!");
		return -1;
	}
	return 0;
}

#else
static int _init_threading(bool warn)
{
	// do nothing if not OpenMP build
	return 0;
}
#endif


// Processes the sound file
static void _processFile(SoundTouch *pSoundTouch, const char *inFileName, const char *outFileName)
{
    int nSamples;
    int nChannels;
    int buffSizeSamples;
    SAMPLETYPE sampleBuffer[BUFF_SIZE];

    // open input file
    WavInFile inFile(inFileName);
    int sampleRate = inFile.getSampleRate();
    int bits = inFile.getNumBits();
    nChannels = inFile.getNumChannels();

    // create output file
    WavOutFile outFile(outFileName, sampleRate, bits, nChannels);

    pSoundTouch->setSampleRate(sampleRate);
    pSoundTouch->setChannels(nChannels);

    assert(nChannels > 0);
    buffSizeSamples = BUFF_SIZE / nChannels;

    // Process samples read from the input file
    while (inFile.eof() == 0)
    {
        int num;

        // Read a chunk of samples from the input file
        num = inFile.read(sampleBuffer, BUFF_SIZE);
        nSamples = num / nChannels;

        // Feed the samples into SoundTouch processor
        pSoundTouch->putSamples(sampleBuffer, nSamples);

        // Read ready samples from SoundTouch processor & write them output file.
        // NOTES:
        // - 'receiveSamples' doesn't necessarily return any samples at all
        //   during some rounds!
        // - On the other hand, during some round 'receiveSamples' may have more
        //   ready samples than would fit into 'sampleBuffer', and for this reason
        //   the 'receiveSamples' call is iterated for as many times as it
        //   outputs samples.
        do
        {
            nSamples = pSoundTouch->receiveSamples(sampleBuffer, buffSizeSamples);
            outFile.write(sampleBuffer, nSamples * nChannels);
        } while (nSamples != 0);
    }

    // Now the input file is processed, yet 'flush' few last samples that are
    // hiding in the SoundTouch's internal processing pipeline.
    pSoundTouch->flush();
    do
    {
        nSamples = pSoundTouch->receiveSamples(sampleBuffer, buffSizeSamples);
        outFile.write(sampleBuffer, nSamples * nChannels);
    } while (nSamples != 0);
}

// Processes the sound file
static int _processMemory(SoundTouch *pSoundTouch, const short *inData, int inDataLen, short *outData, int outDataLen)
{
	
	int nSamples;
    int nChannels;
    int buffSizeSamples;
	
	nChannels = pSoundTouch->getChannels();
	
	buffSizeSamples = outDataLen / nChannels;
	
	nSamples = inDataLen / nChannels;
	
	pSoundTouch->putSamples((SAMPLETYPE *)inData, nSamples);
	
	SAMPLETYPE *outData_p = (SAMPLETYPE *)outData;
	
	int receiveSamples = 0;
	do
	{
		nSamples = pSoundTouch->receiveSamples(outData_p + receiveSamples * nChannels, buffSizeSamples - receiveSamples);
		receiveSamples += nSamples;
		//LOGV("soundtest %d %d ", nSamples, receiveSamples);
	} while ((nSamples != 0) && (receiveSamples < buffSizeSamples));
	
	return receiveSamples * nChannels;
	
}

extern "C" DLL_PUBLIC jstring Java_net_surina_soundtouch_SoundTouch_getVersionString(JNIEnv *env, jobject thiz)
{
    const char *verStr;

    LOGV("JNI call SoundTouch.getVersionString");

    // Call example SoundTouch routine
    verStr = SoundTouch::getVersionString();

    /// gomp_tls storage bug workaround - see comments in _init_threading() function!
    _init_threading(false);

    int threads = 0;
	#pragma omp parallel
    {
		#pragma omp atomic
    	threads ++;
    }
    LOGV("JNI thread count %d", threads);

    // return version as string
    return env->NewStringUTF(verStr);
}



extern "C" DLL_PUBLIC jlong Java_net_surina_soundtouch_SoundTouch_newInstance(JNIEnv *env, jobject thiz, int sampleRate, int nChannels)
{
	LOGV("JNI call SoundTouch.Java_net_surina_soundtouch_SoundTouch_newInstance");
	//return (jlong)(new SoundTouch());
	SoundTouch *ptr = new SoundTouch();
	ptr->setSampleRate(sampleRate);
    ptr->setChannels(nChannels);
    return (jlong)ptr;

}

extern "C" DLL_PUBLIC void Java_net_surina_soundtouch_SoundTouch_deleteInstance(JNIEnv *env, jobject thiz, jlong handle)
{
	SoundTouch *ptr = (SoundTouch*)handle;
	delete ptr;
}

extern "C" DLL_PUBLIC jlong Java_net_surina_soundtouch_SoundTouch_newSonicInstance(JNIEnv *env, jobject thiz, int sampleRate, int nChannels)
{
    LOGV("JNI call SoundTouch.Java_net_surina_soundtouch_SoundTouch_newSonicInstance");
    //return (jlong)(new SoundTouch());
    sonicStream stream = sonicCreateStream(sampleRate, nChannels);
    return (jlong)stream;
}

extern "C" DLL_PUBLIC void Java_net_surina_soundtouch_SoundTouch_deleteSonicInstance(JNIEnv *env, jobject thiz, jlong handle)
{
    sonicStream stream = (sonicStream)handle;
    sonicDestroyStream(stream);
}


extern "C" DLL_PUBLIC void Java_net_surina_soundtouch_SoundTouch_setTempo(JNIEnv *env, jobject thiz, jlong handle, jfloat tempo)
{
	SoundTouch *ptr = (SoundTouch*)handle;
	ptr->setTempo(tempo);
}


extern "C" DLL_PUBLIC void Java_net_surina_soundtouch_SoundTouch_setPitchSemiTones(JNIEnv *env, jobject thiz, jlong handle, jfloat pitch)
{
	SoundTouch *ptr = (SoundTouch*)handle;
	ptr->setPitchSemiTones(pitch);
}


extern "C" DLL_PUBLIC void Java_net_surina_soundtouch_SoundTouch_setSpeed(JNIEnv *env, jobject thiz, jlong handle, jfloat speed)
{
	SoundTouch *ptr = (SoundTouch*)handle;
	ptr->setRate(speed);
}

extern "C" DLL_PUBLIC void Java_net_surina_soundtouch_SoundTouch_setSonicSpeed(JNIEnv *env, jobject thiz, jlong handle, jfloat speed)
{
    sonicStream stream = (sonicStream)handle;
    if( stream != NULL )
        sonicSetSpeed(stream, speed);
}


extern "C" DLL_PUBLIC jstring Java_net_surina_soundtouch_SoundTouch_getErrorString(JNIEnv *env, jobject thiz)
{
	jstring result = env->NewStringUTF(_errMsg.c_str());
	_errMsg.clear();

	return result;
}


extern "C" DLL_PUBLIC int Java_net_surina_soundtouch_SoundTouch_processFile(JNIEnv *env, jobject thiz, jlong handle, jstring jinputFile, jstring joutputFile)
{
	SoundTouch *ptr = (SoundTouch*)handle;

	const char *inputFile = env->GetStringUTFChars(jinputFile, 0);
	const char *outputFile = env->GetStringUTFChars(joutputFile, 0);

	LOGV("JNI process file %s", inputFile);

    /// gomp_tls storage bug workaround - see comments in _init_threading() function!
    if (_init_threading(true)) return -1;

	try
	{
		_processFile(ptr, inputFile, outputFile);
	}
	catch (const runtime_error &e)
    {
		const char *err = e.what();
        // An exception occurred during processing, return the error message
    	LOGV("JNI exception in SoundTouch::processFile: %s", err);
        _setErrmsg(err);
        return -1;
    }


	env->ReleaseStringUTFChars(jinputFile, inputFile);
	env->ReleaseStringUTFChars(joutputFile, outputFile);

	return 0;
}

extern "C" DLL_PUBLIC int Java_net_surina_soundtouch_SoundTouch_processSonicMemory(JNIEnv *env, jobject thiz, jlong handle, jshortArray inData, int inDataLen, jshortArray outData, int outDataLen)
{
    sonicStream stream = (sonicStream)handle;
    if( stream == NULL )
        return 0;
    
    jshort *native_indata = env->GetShortArrayElements(inData, 0);
    jshort *native_outdata = env->GetShortArrayElements(outData, 0);
    
    if (native_indata == NULL || native_outdata == NULL) {
        LOGV("native_indata or native_outdata is null");
        return -1;
    }
    
    int in_len = inDataLen * 2;
    int out_len = outDataLen * 2;
    
    try
    {
        // write first
        sonicWriteShortToStream(stream, native_indata, (int)in_len/4);
        
        int pcm_data_size = 0;
        int nb = 0;
        do
        {
            short * dest = (short *)&(native_outdata[pcm_data_size/2]);
            
            int left_size = outDataLen*2 - pcm_data_size;
            if( left_size <= 0 )
                break;
            
            // 接收处理后的sample
            nb = sonicReadShortFromStream(stream, dest, left_size/4);
            pcm_data_size = pcm_data_size + nb * 2 * 2;
            
            left_size = outDataLen*2 - pcm_data_size;
            if( left_size <= 0 )
                break;
            
        } while (nb != 0);
        
        // return short len
        out_len = pcm_data_size / 2;
    }
    catch (const runtime_error &e)
    {
        const char *err = e.what();
        // An exception occurred during processing, return the error message
        LOGV("JNI exception in SoundTouch::processFile: %s", err);
        _setErrmsg(err);
        return -1;
    }
    
    env->SetShortArrayRegion(outData, 0, outDataLen, native_outdata);
    
    env->ReleaseShortArrayElements(inData, native_indata, 0);
    env->ReleaseShortArrayElements(outData, native_outdata, 0);
    
    return out_len;
}

extern "C" DLL_PUBLIC int Java_net_surina_soundtouch_SoundTouch_processMemory(JNIEnv *env, jobject thiz, jlong handle, jshortArray inData, int inDataLen, jshortArray outData, int outDataLen)
{
	SoundTouch *ptr = (SoundTouch*)handle;

	//const char *inputFile = env->GetStringUTFChars(jinputFile, 0);
	//const char *outputFile = env->GetStringUTFChars(joutputFile, 0);
	
	jshort *native_indata = env->GetShortArrayElements(inData, 0);
	jshort *native_outdata = env->GetShortArrayElements(outData, 0);
	
	if (native_indata == NULL || native_outdata == NULL) {
		LOGV("native_indata or native_outdata is null");
		return -1;
	}

	//LOGV("Java_net_surina_soundtouch_SoundTouch_processMemory %d", inDataLen);

    /// gomp_tls storage bug workaround - see comments in _init_threading() function!
    if (_init_threading(true)) return -1;
	
	int out_len = 0;

	try
	{
		out_len = _processMemory(ptr, native_indata, inDataLen, native_outdata, outDataLen);
	}
	catch (const runtime_error &e)
    {
		const char *err = e.what();
        // An exception occurred during processing, return the error message
    	LOGV("JNI exception in SoundTouch::processFile: %s", err);
        _setErrmsg(err);
        return -1;
    }
	
	env->SetShortArrayRegion(outData, 0, outDataLen, native_outdata);
	
	env->ReleaseShortArrayElements(inData, native_indata, 0);
	env->ReleaseShortArrayElements(outData, native_outdata, 0);

	return out_len;
}
