package com.gtv.cloud.recorder;

/**
 * Created by gtv on 2018/2/6.
 */

public class GTVRecordCreator2 {
    private static GTVVideoRecorder2 recorder;

    public static IGTVVideoRecorder2 getInstance() {

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

        return new GTVVideoRecorder2();
    }


}
