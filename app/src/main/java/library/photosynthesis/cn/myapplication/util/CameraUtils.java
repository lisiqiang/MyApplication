package library.photosynthesis.cn.myapplication.util;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;

/**
 * Created by you on 2016/10/21.
 */

public class CameraUtils {

    /**
     * 摄像机是否支持前置拍照
     * @return
     */
    public static boolean isSupportFrontCamera() {
        final int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否支持闪光
     * @param context
     * @return
     */
    public static boolean isSupportFlashCamera(Context context) {
        FeatureInfo[] features = context.getPackageManager().getSystemAvailableFeatures();
        for(FeatureInfo info : features) {
            if(PackageManager.FEATURE_CAMERA_FLASH.equals(info.name))
                return true;
        }
        return false;
    }

}
