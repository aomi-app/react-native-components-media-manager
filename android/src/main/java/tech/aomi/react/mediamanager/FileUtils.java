package tech.aomi.react.mediamanager;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 文件工具
 *
 * @author 田尘殇Sean sean.snow@live.com
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * uri 转换为 file path
     *
     * @param uriStr 资源URI
     * @return file path
     */
    public static String getFilePathFromContentUri(ContentResolver contentResolver, String uriStr, String[] filePathColumn) {
        Uri uri = Uri.parse(uriStr);
        Cursor cursor = contentResolver.query(uri, filePathColumn, null, null, null);
        if (null != cursor) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();

            return filePath;
        }

        return null;
    }


    public static void compressImage(ContentResolver contentResolver, Uri uri, int quality) {
        OutputStream imageOut = null;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri);
            imageOut = contentResolver.openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, imageOut);
        } catch (IOException e) {
            Log.e(TAG, "压缩图片失败", e);
        } finally {
            if (null != imageOut)
                try {
                    imageOut.close();
                } catch (IOException ignored) {
                }
        }
    }

    public static void fixOrientationAndScale(ContentResolver contentResolver, String path, int width, int height) {
        int degree = readPictureDegree(path);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        Bitmap newBitmap = rotateAndScale(degree, width, height, bitmap);
        OutputStream imageOut = null;
        try {
            imageOut = contentResolver.openOutputStream(Uri.fromFile(new File(path)));
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOut);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != imageOut)
                try {
                    imageOut.close();
                } catch (IOException ignored) {
                }
        }
    }

    public static void setImageGps(String path, double latitude, double longitude) {
        try {
            ExifInterface exif = new ExifInterface(path);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, toGps(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude > 0 ? "N" : "S");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, toGps(longitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude > 0 ? "E" : "W");
            exif.saveAttributes();
        } catch (IOException e) {
            Log.e(TAG, "设置GPS失败", e);
        }
    }

    private static String toGps(double gps) {
        int a = (int) gps;
        double bb = (gps - a) * 60;
        int b = (int) bb;
        double cc = bb - b;
        String c = (cc * 60) + "";
        return a + "," + b + "," + c + "";
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    private static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "读取图片信息失败", e);
        }
        return degree;
    }

    /**
     * 旋转图片和缩放照片
     *
     * @param angle  度数
     * @param bitmap 图片
     * @return Bitmap
     */
    private static Bitmap rotateAndScale(int angle, int newWidth, int newHeight, Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (newWidth != 0 && newHeight != 0) {
            int tempMin = Math.min(newWidth, newHeight);
            int tempMax = Math.max(newWidth, newHeight);

            if (width < height) {
                newWidth = tempMin;
                newHeight = tempMax;
            } else {
                newWidth = tempMax;
                newHeight = tempMin;
            }
            //计算缩放率，新尺寸除原始尺寸
            float widthScale = ((float) newWidth) / width;
            float heightScale = ((float) newHeight) / height;

            // 缩放图片动作
            matrix.postScale(widthScale, heightScale);
        }
        matrix.postRotate(angle);
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

}
