package library.photosynthesis.cn.myapplication.util;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {
	/* 拍照的照片存储位置 */
	private static final File PHOTO_DIR = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
	
	public static Uri getPicOutFileUri(){
		String storageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_REMOVED.equals(storageState)){
            return null;
        }
        if (!PHOTO_DIR.exists()){
        	PHOTO_DIR.mkdirs();
        }
        File file = new File(PHOTO_DIR,getPicNameByTime());
        return Uri.fromFile(file);
	}
	
	private static String getPicNameByTime(){
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		return "IMG_"+timeStamp+".jpg"; 
	}
	
	//从相册中选择图，得到选择图片的路径
	public static String getPicPathByGallery(Intent data,Context context){
		Uri uri = data.getData();
		if (!TextUtils.isEmpty(uri.getAuthority())) {
            //查询选择图片    
            Cursor cursor = context.getContentResolver().query(    
                    uri,    
                    new String[] { MediaStore.Images.Media.DATA },    
                    null,     
                    null,     
                    null);    
            //返回 没找到选择图片    
            if (null == cursor) {    
                return null;    
            }    
            //光标移动至开头 获取图片路径    
            cursor.moveToFirst();    
            return cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));    
        }
		return null; 
	}
	
	public static void delFile(File file){
		if(file != null && file.exists()){
			file.delete();
		}
	}
	
	public static synchronized boolean createFile(String path, String fileName) {
        boolean hasFile = false;
        try {
            File dir = new File(path);
            boolean hasDir = dir.exists() || dir.mkdirs();
            if (hasDir) {
                File file = new File(dir, fileName);
                if(file.exists()){
                    file.delete();
                }
                hasFile = file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hasFile;
    }
}
