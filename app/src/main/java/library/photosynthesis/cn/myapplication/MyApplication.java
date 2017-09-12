package library.photosynthesis.cn.myapplication;

import android.app.Application;

/**
 * Created by siqiangli on 2017/5/8 15:00
 */

public class MyApplication extends Application{
    private static MyApplication myApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
    }

    public static MyApplication getInstance(){
        return myApplication;
    }

    //获取音视频文件保存的目录
    public String getSaveFileDir(){
        return myApplication.getExternalFilesDir(null).getAbsolutePath()+"/av";
    }
}
