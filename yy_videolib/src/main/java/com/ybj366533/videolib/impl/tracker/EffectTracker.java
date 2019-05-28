package com.ybj366533.videolib.impl.tracker;

import android.support.annotation.Keep;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gtv on 15/10/13.
 */
public class EffectTracker {

    private static final String TAG = "GTVREC";

    private static boolean _libraryLoaded = false;

    private static List<EffectTracker> _streamPool = new ArrayList<EffectTracker>();

    private long streamer_addr = 0;

    private boolean isReady = false;

    static {
        if(!_libraryLoaded) {

            try {
                System.loadLibrary("yysticker");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            _libraryLoaded = true;
        }
    }

    public EffectTracker() {


        streamer_addr = _createTrackerJNI();

        synchronized (_streamPool){

            _streamPool.add(this);
        }
    }

    ////////////////////////////////////////////////////////
    private static EffectTracker _tracker_instance = null;
    public static EffectTracker getInstance() {

        if( _tracker_instance == null ) {

            _tracker_instance = new EffectTracker();
        }
        return _tracker_instance;
    }

    public void destroy() {

        if( streamer_addr > 0 ) {
            _closeTrackerJNI(streamer_addr);
        }

        streamer_addr = 0;

        synchronized (_streamPool){

            _streamPool.remove(this);
        }
    }

    public int loadModel(String path) {

        if( this.streamer_addr == 0 || path == null ) {
            return -1;
        }
        int res = _loadModelJNI(this.streamer_addr, path);
        if (res == 0) {
            isReady = true;
        }

        return res;
    }

    public int updateFaceRect(int x, int y, int width, int height) {

        if( this.streamer_addr == 0 ) {
            return -1;
        }

        return _updateFaceRectJNI(this.streamer_addr, x, y, width, height);
    }

    public int trackImageAJNI(byte[] data, int size, int width, int height, int centerX, int centerY, float angle, float[] output) {

        if( this.streamer_addr == 0 || this.isReady == false) {
            return -1;
        }

        return _trackImageAJNI(this.streamer_addr, data, size, width, height, centerX, centerY, angle, output);
    }

    public int trackImageJNI(byte[] data, int size, int width, int height, float[] output) {

        if( this.streamer_addr == 0 || this.isReady == false) {
            return -1;
        }

        return _trackImageJNI(this.streamer_addr, data, size, width, height, output);
    }

    public void getRotationParams(float[] output) {

        if( this.streamer_addr == 0 || this.isReady == false) {
            return;
        }

        _getRotationParamsJNI(this.streamer_addr, output);
    }

    public int getEnclosingBox(float[] output) {

        if( this.streamer_addr == 0 || this.isReady == false) {
            return -1;
        }

        return _getEnclosingBoxJNI(this.streamer_addr, output);
    }

    public int getBigEyePositionJNI(int[] output) {

        if( this.streamer_addr == 0 ) {
            return -1;
        }

        return _getBigEyePositionJNI(this.streamer_addr, output);
    }

    public int getThinFacePositionJNI(int[] output) {

        if( this.streamer_addr == 0 ) {
            return -1;
        }

        return _getThinFacePositionJNI(this.streamer_addr, output);
    }

    public int getSmallFacePositionJNI(int[] output) {

        if( this.streamer_addr == 0 ) {
            return -1;
        }

        return _getSmallFacePositionJNI(this.streamer_addr, output);
    }

    public void getGLESImageVertex(float centerX, float centerY, float angle, float canvasWidth, float canvasHeight, float[] output) {

        if( this.streamer_addr == 0 ) {
            return;
        }

        _getGLESImageVertexJNI(this.streamer_addr, centerX, centerY, angle, canvasWidth, canvasHeight, output);
    }
    /*
    public int testDrawTexture(int tid, int filterInputTextureUniform, int filterPositionAttribute, int filterTextureCoordinateAttribute) {

        if( this.streamer_addr == 0 ) {
            return -1;
        }

        return _testDrawTextureJNI(this.streamer_addr, tid, filterInputTextureUniform, filterPositionAttribute, filterTextureCoordinateAttribute);
    }
*/
    private String currentStickerFolderName = null;
    public String getCurrentStickerFolderName() {
        return currentStickerFolderName;
    }

    private String lastStickerFolderName = null;
    public String getLastStickerFolderName() {
        return lastStickerFolderName;
    }

    public void startPlaySticker(String foldername) {

        if( this.streamer_addr == 0 ) {
            return;
        }

        currentStickerFolderName = foldername;

        _startPlayStickerJNI(this.streamer_addr, foldername, -1);
    }

    public void startPlaySticker(String foldername, int loopCount) {

        if( this.streamer_addr == 0 ) {
            return;
        }

        currentStickerFolderName = foldername;

        _startPlayStickerJNI(this.streamer_addr, foldername, loopCount);
    }

    public void stopPlaySticker() {

        if( this.streamer_addr == 0 ) {
            return;
        }

        lastStickerFolderName = currentStickerFolderName;
        currentStickerFolderName = null;

        _stopPlayStickerJNI(this.streamer_addr);
    }

    public void prepareTexture() {

        if( this.streamer_addr == 0 ) {
            return;
        }

        _prepareTextureJNI(this.streamer_addr);
    }

    public int prepareTextureFrames(int count, int ready) {

        if( this.streamer_addr == 0 ) {
            return -1;
        }

        return _prepareTextureFramesJNI(this.streamer_addr, count, ready);
    }

    public void clearTexture() {

        if( this.streamer_addr == 0 ) {
            return;
        }

        _clearTextureJNI(this.streamer_addr);
    }

    public void drawWithUniform(int unif, int pos, int coor) {

        if( this.streamer_addr == 0 ) {
            return;
        }

        _drawWithUniformJNI(this.streamer_addr, unif, pos, coor);
    }

    public void udpateCanvasSize(int w, int h) {

        if( this.streamer_addr == 0 ) {
            return;
        }

        _udpateCanvasSizeJNI(this.streamer_addr, w, h);
    }

    public void glReadPixels(int x, int y, int width, int height,
                             int format, int type) {
        _glReadPixelsJNI(x, y, width, height, format, type);
    }

    @Keep
    private native long _createTrackerJNI();
    @Keep
    private native void _closeTrackerJNI(long streamer_addr);
    @Keep
    private native int _loadModelJNI(long streamer_addr, String filename);
    @Keep
    private native void _getGLESImageVertexJNI(long streamer_addr, float centerX, float centerY, float angle, float canvasWidth, float canvasHeight, float[] jout);
    @Keep
    private native void _getRotationParamsJNI(long streamer_addr, float[] output);
    @Keep
    private native int _getEnclosingBoxJNI(long streamer_addr, float[] output);
    @Keep
    private native int _trackImageAJNI(long streamer_addr, byte[] data, int size, int width, int height, int centerX, int centerY, float angle, float[] output);
    @Keep
    private native int _trackImageJNI(long streamer_addr, byte[] data, int size, int width, int height, float[] output);
    @Keep
    private native int _getBigEyePositionJNI(long streamer_addr, int[] output);
    @Keep
    private native int _getThinFacePositionJNI(long streamer_addr, int[] output);
    @Keep
    private native int _getSmallFacePositionJNI(long streamer_addr, int[] output);
    @Keep
    private native int _updateFaceRectJNI(long streamer_addr, int x, int y, int width, int height);
    @Keep
    private native void _startPlayStickerJNI(long streamer_addr, String foldername, int loop);
    @Keep
    private native void _stopPlayStickerJNI(long streamer_addr);
    @Keep
    private native void _prepareTextureJNI(long streamer_addr);
    @Keep
    private native int _prepareTextureFramesJNI(long streamer_addr, int count, int ready);
    @Keep
    private native void _clearTextureJNI(long streamer_addr);
    @Keep
    private native void _drawWithUniformJNI(long streamer_addr, int unif, int pos, int coor);
    @Keep
    private native void _udpateCanvasSizeJNI(long streamer_addr, int w, int h);
    @Keep
    private native void _glReadPixelsJNI(int x, int y, int width, int height,
                                        int format, int type);

//    private native int _testDrawTextureJNI(long streamer_addr, int texid, int filterInputTextureUniform, int filterPositionAttribute, int filterTextureCoordinateAttribute);
}
