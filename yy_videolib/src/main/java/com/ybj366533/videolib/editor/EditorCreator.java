package com.ybj366533.videolib.editor;

/**
 * Created by YY on 2018/2/6.
 */

public class EditorCreator {
    private static VideoEditor editor;

    public static IVideoEditor getInstance() {

//        if( editor != null ) {
//            return editor;
//        }
//
//        synchronized (EditorCreator.class) {
//
//            if( editor != null ) {
//                return editor;
//            }
//
//            editor = new VideoEditor();
//
//        }
//
//        return editor;

        return new VideoEditor();
    }
}
