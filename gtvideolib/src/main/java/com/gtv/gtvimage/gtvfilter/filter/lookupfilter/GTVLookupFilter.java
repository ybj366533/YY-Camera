package com.gtv.gtvimage.gtvfilter.filter.lookupfilter;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.gtv.cloud.videoplayer.R;

/**
 * Created by ken on 2018/4/14.
 */

public class GTVLookupFilter extends GTVImageLookupFilter {

    public GTVLookupFilter(Context context, final float intensity, int resid) {
        super(intensity);
        setBitmap(BitmapFactory.decodeResource(context.getResources(), resid));
    }
}
