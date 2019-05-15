package com.gtv.cloud.editor;

/**
 * Created by gtv on 2018/2/6.
 */

public class GTVComposerCreator {

    public static IGTVVideoComposer getInstance() {

        return new GTVVideoComposer();
    }
}
