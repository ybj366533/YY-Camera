package com.ybj366533.videolib.recorder;

/**
 * Created by YY on 2018/2/6.
 */

public class RecordCreator {
    private static VideoRecorder recorder;

    public static IVideoRecorder getInstance() {

//        if( recorder != null ) {
//            return recorder;
//        }
//
//        synchronized (IVideoRecorder.class) {
//
//            if( recorder != null ) {
//                return recorder;
//            }
//
//            recorder = new VideoRecorder();
//
//        }
//
//        return recorder;

        return new VideoRecorder();
    }


}
