////////////////////////////////////////////////////////////////////////////////
///
/// Example class that invokes native SoundTouch routines through the JNI
/// interface.
///
/// Author        : Copyright (c) Olli Parviainen
/// Author e-mail : oparviai 'at' iki.fi
/// WWW           : http://www.surina.net
///
////////////////////////////////////////////////////////////////////////////////
//
// $Id: SoundTouch.java 211 2015-05-15 00:07:10Z oparviai $
//
////////////////////////////////////////////////////////////////////////////////

package net.surina.soundtouch;

import android.util.Log;

public final class SoundTouch
{
    // Native interface function that returns SoundTouch version string.
    // This invokes the native c++ routine defined in "soundtouch-jni.cpp".
    public native final static String getVersionString();
    
    private native final void setTempo(long handle, float tempo);

    private native final void setPitchSemiTones(long handle, float pitch);
    
    private native final void setSpeed(long handle, float speed);

    private native final int processFile(long handle, String inputFile, String outputFile);

    private native final int processMemory(long handle, short[] inData, int inDataLen, short[] outData, int outDataLen);

    public native final static String getErrorString();

    private native final static long newInstance(int sampleRate, int nChannels);
    
    private native final void deleteInstance(long handle);

    private native final static long newSonicInstance(int sampleRate, int nChannels);

    private native final void deleteSonicInstance(long handle);

    private native final void setSonicSpeed(long handle, float speed);

    private native final int processSonicMemory(long handle, short[] inData, int inDataLen, short[] outData, int outDataLen);

    long handle = 0;
    long sonicHandle = 0;
    
    public SoundTouch()
    {
        //Log.e("soundtouch", "--------------------------");
    	handle = newInstance(44100, 2);
        sonicHandle = newSonicInstance(44100, 2);
    }
    
    
    public void close()
    {
        if( handle > 0 ) {
            deleteInstance(handle);
            handle = 0;
        }

    	if( sonicHandle > 0 ) {
            deleteSonicInstance(sonicHandle);
            sonicHandle = 0;
        }
    }


    public void setTempo(float tempo)
    {
    	setTempo(handle, tempo);
    }


    public void setPitchSemiTones(float pitch)
    {
    	setPitchSemiTones(handle, pitch);
    }

    
    public void setSpeed(float speed)
    {
    	setSpeed(handle, speed);
    }

    public void setSonicSpeed(float speed)
    {
        setSonicSpeed(sonicHandle, speed);
    }


    public int processFile(String inputFile, String outputFile)
    {
    	return processFile(handle, inputFile, outputFile);
    }

    public int processMemory(short[] inData, int inDataLen, short[] outData, int outDataLen)
    {
        return processMemory(handle, inData, inDataLen, outData, outDataLen);
    }

    public int processSonicMemory(short[] inData, int inDataLen, short[] outData, int outDataLen)
    {
        return processSonicMemory(sonicHandle, inData, inDataLen, outData, outDataLen);
    }

    // Load the native library upon startup
    static
    {
        System.loadLibrary("soundtouch");
    }
}
