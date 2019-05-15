package com.gtv.cloud.recorder;

import com.gtv.cloud.recorder.GTVVideoRecorder;
import com.gtv.cloud.recorder.IGTVVideoRecorder;

/**
 * Created by gtv on 2018/2/6.
 */

public class GTVRecordCreator {
    private static GTVVideoRecorder recorder;

    public static IGTVVideoRecorder getInstance() {

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

        return new GTVVideoRecorder();
    }


}
