package com.ybj366533.yycamera.widget.photopicker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.base.BaseActivity

import java.io.File
import me.iwf.photopicker.fragment.ImagePagerFragment

import me.iwf.photopicker.PhotoPicker.KEY_SELECTED_PHOTOS
import me.iwf.photopicker.PhotoPreview.EXTRA_CURRENT_ITEM
import me.iwf.photopicker.PhotoPreview.EXTRA_PHOTOS
import me.iwf.photopicker.PhotoPreview.EXTRA_SHOW_DELETE

/**
 * 预览保存图片
 * Created by donglua on 15/6/24.
 */
class PhotoPreviewActivity : BaseActivity() {

    private var pagerFragment: ImagePagerFragment? = null

    private var actionBar: ActionBar? = null
    private var showDelete: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.__picker_activity_photo_pager)

        val currentItem = intent.getIntExtra(EXTRA_CURRENT_ITEM, 0)
        val paths = intent.getStringArrayListExtra(EXTRA_PHOTOS)
        showDelete = intent.getBooleanExtra(EXTRA_SHOW_DELETE, true)

        if (pagerFragment == null) {
            pagerFragment = supportFragmentManager.findFragmentById(R.id.photoPagerFragment) as ImagePagerFragment
        }
        pagerFragment!!.setPhotos(paths, currentItem)

        val mToolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(mToolbar)

        actionBar = supportActionBar

        if (actionBar != null) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
            updateActionBarTitle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                actionBar!!.elevation = 25f
            }
        }


        pagerFragment!!.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                updateActionBarTitle()
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        if (showDelete) {
//            menuInflater.inflate(R.menu.menu_picker_preview, menu)
//        }
        return true
    }


    override fun onBackPressed() {

        val intent = Intent()
        intent.putExtra(KEY_SELECTED_PHOTOS, pagerFragment!!.paths)
        setResult(RESULT_OK, intent)
        finish()

        super.onBackPressed()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

//        if (item.itemId == R.id.save) {
//            val index = pagerFragment!!.currentItem
//
//            val deletedPath = pagerFragment!!.paths[index]
//            val start = deletedPath.lastIndexOf('/')
//            val fileName = deletedPath.substring(start)
//
//
//            imageDownload(deletedPath) {
//
//                try {
//                    val saveFile = File("$sdcardPath/Pictures", fileName)
//                    it?.copyTo(saveFile, true)
//                    runOnUi {
//                        val snackbar = Snackbar.make(pagerFragment!!.view!!, "保存到$saveFile",
//                                Snackbar.LENGTH_LONG)
//                        snackbar.show()
//                    }
//                } catch (e: Throwable) {
//                    val snackbar = Snackbar.make(pagerFragment!!.view!!, "保存失败，请检查读写权限",
//                            Snackbar.LENGTH_LONG)
//                    snackbar.show()
//                }
//            }
//
//            return true
//        }

        return super.onOptionsItemSelected(item)
    }

    fun updateActionBarTitle() {
        if (actionBar != null)
            actionBar!!.title = getString(R.string.__picker_image_index, pagerFragment!!.viewPager.currentItem + 1,
                    pagerFragment!!.paths.size)
    }
}