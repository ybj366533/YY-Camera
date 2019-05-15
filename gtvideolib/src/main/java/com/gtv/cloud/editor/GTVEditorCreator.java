package com.gtv.cloud.editor;

/**
 * Created by gtv on 2018/2/6.
 */

public class GTVEditorCreator {
    private static GTVVideoEditor editor;

    public static IGTVVideoEditor getInstance() {

//        if( editor != null ) {
//            return editor;
//        }
//
//        synchronized (GTVEditorCreator.class) {
//
//            if( editor != null ) {
//                return editor;
//            }
//
//            editor = new GTVVideoEditor();
//
//        }
//
//        return editor;

        return new GTVVideoEditor();
    }
}
