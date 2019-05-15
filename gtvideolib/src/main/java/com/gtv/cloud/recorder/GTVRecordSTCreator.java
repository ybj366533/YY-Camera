package com.gtv.cloud.recorder;

/**
 * Created by gtv on 2018/2/6.
 */

public class GTVRecordSTCreator {
    private static GTVVideoRecorderST recorder;

    public static IGTVVideoRecorderST getInstance() {

//        if( recorder != null ) {
//            return recorder;
//        }
//
//        synchronized (IGTVVideoRecorder.class) {
//
//            if( recorder != null ) {
//                return recorder;
//            }
//
//            recorder = new GTVVideoRecorder();
//
//        }
//
//        return recorder;

        return new GTVVideoRecorderST();
    }


}
