package com.ybj366533.yycamera.widget.webp

import android.content.Context
import android.graphics.Bitmap

import com.bumptech.glide.Glide
import com.bumptech.glide.load.ImageHeaderParser
import com.bumptech.glide.load.ImageHeaderParserUtils
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Initializable
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.UnitTransformation
import com.bumptech.glide.load.resource.drawable.DrawableResource
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import com.facebook.animated.webp.WebPImage

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by Summer on 17/7/16.
 */

class WebpResourceDecoder @JvmOverloads constructor(context: Context, private val mParsers: List<ImageHeaderParser> = Glide.get(context).registry.imageHeaderParsers, private val mByteArrayPool: ArrayPool = Glide.get(context).arrayPool, private val mBitmapPool: BitmapPool = Glide.get(context).bitmapPool) : ResourceDecoder<InputStream, WebpDrawable> {
    private val mContext: Context
    private val mProvider: GifBitmapProvider

    init {
        mContext = context.applicationContext
        mProvider = GifBitmapProvider(mBitmapPool, mByteArrayPool)

    }

    @Throws(IOException::class)
    override fun handles(inputStream: InputStream, options: Options): Boolean {
        val type = ImageHeaderParserUtils.getType(mParsers, inputStream, mByteArrayPool)
        return type == ImageHeaderParser.ImageType.WEBP || type == ImageHeaderParser.ImageType.WEBP_A
    }

    @Throws(IOException::class)
    override fun decode(inputStream: InputStream, width: Int, height: Int, options: Options): Resource<WebpDrawable>? {
        val swapStream = ByteArrayOutputStream()
        val buff = ByteArray(100) //buff用于存放循环读取的临时数据
        var rc = 0
        while ((rc = inputStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc)
        }
        val in_b = swapStream.toByteArray()
        val webp = WebPImage.create(in_b)

        val sampleSize = getSampleSize(webp.width, webp.height, width, height)
        val webpDecoder = WebpDecoder(mProvider, webp, sampleSize)
        val firstFrame = webpDecoder.nextFrame ?: return null

        val unitTransformation = UnitTransformation.get<Bitmap>()

        return WebpDrawableResource(WebpDrawable(mContext, webpDecoder, mBitmapPool, unitTransformation, width, height,
                firstFrame))
    }

    private fun getSampleSize(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        val exactSampleSize = Math.min(srcHeight / targetHeight,
                srcWidth / targetWidth)
        val powerOfTwoSampleSize = if (exactSampleSize == 0) 0 else Integer.highestOneBit(exactSampleSize)
        // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
        // than 0.
        return Math.max(1, powerOfTwoSampleSize)
    }


    inner class WebpDrawableResource(drawable: WebpDrawable) : DrawableResource<WebpDrawable>(drawable), Initializable {

        override fun getResourceClass(): Class<WebpDrawable> {
            return WebpDrawable::class.java
        }

        override fun getSize(): Int {
            return drawable.size
        }

        override fun recycle() {

        }

        override fun initialize() {

        }
    }

}
