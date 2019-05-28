package com.ybj366533.videolib.impl.utils;

import android.content.Context;
import android.os.Environment;

import com.ybj366533.videolib.utils.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class YYFileUtils {

    public static boolean deleteFile(String path) {
        File file = new File(path);
        if (file.exists())
            file.delete();
        return true;
    }

    public static boolean copyFileIfNeed(Context context, String fileName) {
        String path = getFilePath(context, fileName);
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                //如果模型文件不存在
                try {
                    if (file.exists())
                        file.delete();

                    file.createNewFile();
                    InputStream in = context.getApplicationContext().getAssets().open(fileName);
                    if(in == null)
                    {
                        LogUtils.LOGE("copyMode", "the src is not existed");
                        return false;
                    }
                    OutputStream out = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    file.delete();
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean createDirectoryIfNeed(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.exists();
    }
    public static String getFilePath(Context context, String fileName) {
        String path = null;
        File dataDir = context.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + fileName;
        }
        return path;
    }

    // 获取内部处理用的临时目录
    public static String getTempFolder(Context context) {
        String path = null;
        File dataDir = context.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + "tmp/";
            createDirectoryIfNeed(path);
        }
        return path;
    }

//    public static List<Bitmap> getStickerImage(Context context) {
//        List<Bitmap> stickerList = new ArrayList<>();
//
//        Bitmap icon0 = BitmapFactory.decodeResource(context.getResources(), R.drawable.none);
//        Bitmap icon1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.bunny);
//        Bitmap icon2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.rabbiteating);
//
//        stickerList.add(icon0);
//        stickerList.add(icon1);
//        stickerList.add(icon2);
//
//        return stickerList;
//    }

    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                LogUtils.LOGE("FileUtil", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    public static String getExternalFilesPath(Context context){
        //String pathdir = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator;
        String pathdir = null;
        File dataDir = context.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            pathdir = dataDir.getAbsolutePath() + File.separator;
        }

        return pathdir;
    }

}
