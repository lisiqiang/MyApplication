package library.photosynthesis.cn.myapplication.manager;

import android.app.Application;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import library.photosynthesis.cn.myapplication.MyApplication;
import library.photosynthesis.cn.myapplication.util.CameraUtils;
import library.photosynthesis.cn.myapplication.util.FileUtil;

/**
 * 相机管理类
 * Created by siqiangli on 2017/5/10 15:28.
 */

public final class CameraManager {

    /**
     * 最大录制时间
     */
    private int maxTime=15000;
    /**
     * 最大录制大小 默认60m
     */
    private long maxSize=60*1024*1024;

    private Application context;
    /**
     * camera
     */
    private Camera mCamera;
    /**
     * 视频录制
     */
    private MediaRecorder mMediaRecorder;
    /**
     * 相机闪光状态
     */
    private int cameraFlash;
    /**
     * 前后置状态
     */
    private int cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    /**
     * 是否支持前置摄像,是否支持闪光
     */
    private boolean isSupportFrontCamera;
    /**
     * 录制视频的相关参数
     */
    private CamcorderProfile mProfile;
    /**
     * 0为拍照, 1为录像
     */
    private int cameraType;

    /**录频后的文件*/
    private File file;

    /**竖屏视频需要的角度 跟着重力感应变化*/
    private int rotationRecord = 90;
    /**跟着重力感应变化*/
    private int rotationFlag = 0;

    /**用于视频播放画布的旋转*/
    public int rotationDirection = 0;

    private CameraManager(Application context) {
        this.context = context;
        isSupportFrontCamera = CameraUtils.isSupportFrontCamera();
    }

    private static CameraManager INSTANCE;

    public static CameraManager getInstance(Application context) {
        if (INSTANCE == null) {
            synchronized (CameraManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CameraManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public int getRotationFlag() {
        return rotationFlag;
    }

    public void setRotationFlag(int rotationFlag) {
        this.rotationFlag = rotationFlag;
    }

    public void setRotationRecord(int rotationRecord){
        this.rotationRecord = rotationRecord;
    }

    public int getRotationRecord(){
        return this.rotationRecord;
    }

    /**
     * 打开camera
     */
    public void openCamera(SurfaceTexture surfaceTexture, int width, int height) {
        if (mCamera == null) {
            try {
                mCamera = Camera.open(cameraFacing);//打开当前选中的摄像头
                mProfile = CamcorderProfile.get(cameraFacing, CamcorderProfile.QUALITY_HIGH);
                mCamera.setDisplayOrientation(90);//默认竖直拍照
                mCamera.setPreviewTexture(surfaceTexture);
                initCameraParameters(cameraFacing, width, height);
                mCamera.startPreview();
            } catch (Exception e) {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        }
    }

    /**
     * 开启预览,前提是camera初始化了
     */
    public void restartPreview() {
        if (mCamera == null) return;
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            int zoom = parameters.getZoom();
            if (zoom > 0) {
                parameters.setZoom(0);
                mCamera.setParameters(parameters);
            }
            mCamera.startPreview();
        } catch (Exception e) {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        }
    }

    private void initCameraParameters(int cameraId, int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null) {
                if (cameraType == 0) {
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                } else {
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                }
            }
        }
        parameters.setRotation(90);//设置旋转代码,
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);//闪光灯关闭

        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        if (!isEmpty(pictureSizes) && !isEmpty(previewSizes)) {
            Camera.Size optimalPicSize = getOptimalSize(pictureSizes, width, height);
            Camera.Size optimalPreSize = getOptimalSize(previewSizes, width, height);
            parameters.setPictureSize(optimalPicSize.width, optimalPicSize.height);
            parameters.setPreviewSize(optimalPreSize.width, optimalPreSize.height);
            mProfile.videoFrameWidth = optimalPreSize.width;
            mProfile.videoFrameHeight = optimalPreSize.height;
//            mProfile.videoBitRate = 5000000;//此参数主要决定视频拍出大小
            mProfile.videoBitRate = 2*1024*1024;
        }
        mCamera.setParameters(parameters);
    }

    /**
     * 释放摄像头
     */
    public void closeCamera() {
        this.cameraType = 0;
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        }
    }

    public void setMaxTime(int maxTime){
        this.maxTime = maxTime;
    }

    public int getMaxTime(){
        return maxTime;
    }

    public void clearFile(){
        FileUtil.delFile(file);
        file = null;
    }

    /**
     * 集合不为空
     *
     * @param list
     * @param <E>
     * @return
     */
    private <E> boolean isEmpty(List<E> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 获取最佳预览相机Size参数
     *
     * @return
     */
    private Camera.Size getOptimalSize(List<Camera.Size> sizes, int w, int h) {
        Camera.Size optimalSize = null;
        float targetRadio = h / (float) w;
        float optimalDif = Float.MAX_VALUE; //最匹配的比例
        int optimalMaxDif = Integer.MAX_VALUE;//最优的最大值差距
        for (Camera.Size size : sizes) {
            float newOptimal = size.width / (float) size.height;
            float newDiff = Math.abs(newOptimal - targetRadio);
            if (newDiff < optimalDif) { //更好的尺寸
                optimalDif = newDiff;
                optimalSize = size;
                optimalMaxDif = Math.abs(h - size.width);
            } else if (newDiff == optimalDif) {//更好的尺寸
                int newOptimalMaxDif = Math.abs(h - size.width);
                if (newOptimalMaxDif < optimalMaxDif) {
                    optimalDif = newDiff;
                    optimalSize = size;
                    optimalMaxDif = newOptimalMaxDif;
                }
            }
        }
        return optimalSize;
    }

    /**
     * 缩放
     * @param isZoomIn
     */
    public void handleZoom(boolean isZoomIn) {
        if (mCamera == null) return;
        Camera.Parameters params = mCamera.getParameters();
        if (params == null) return;
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            mCamera.setParameters(params);
        } else {
        }
    }

    /**
     * 更换前后置摄像
     */
    public void changeCameraFacing(SurfaceTexture surfaceTexture, int width, int height) {
        if(isSupportFrontCamera) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
            for(int i = 0; i < cameraCount; i++) {
                Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
                if(cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) { //现在是后置，变更为前置
                    if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位为前置
                        closeCamera();
                        cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
                        openCamera(surfaceTexture, width, height);
                        break;
                    }
                } else {//现在是前置， 变更为后置
                    if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位
                        closeCamera();
                        cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                        openCamera(surfaceTexture, width, height);
                        break;
                    }
                }
            }
        } else { //不支持摄像机
            Toast.makeText(context, "您的手机不支持前置摄像", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 拍照
     */
    public void takePhoto(Camera.PictureCallback callback) {
        if (mCamera != null) {
            try {
                mCamera.takePicture(null, null, callback);
            } catch(Exception e) {
                Toast.makeText(context, "拍摄失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getVideoPath() {
        String videoPath = null;
        if(file != null){
            videoPath = file.getAbsolutePath();
        }
        return videoPath;
    }

    /**
     * 开始录制视频
     */
    public void startMediaRecord() throws Exception{
        if (mCamera == null || mProfile == null) return;
        releaseMediaRecorder();
        mCamera.stopPreview();
        mCamera.unlock();
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        if (isCameraFrontFacing() && rotationRecord == 90) {
            mMediaRecorder.setOrientationHint(270);
        }else{
            mMediaRecorder.setOrientationHint(rotationRecord);
        }
        rotationDirection = rotationFlag;

        String fileDir = MyApplication.getInstance().getSaveFileDir();
        String fileName = "xy";
        FileUtil.createFile(fileDir,fileName+".mp4");
        file = new File(fileDir,fileName + ".mp4");
        mMediaRecorder.reset();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setProfile(mProfile);
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    if(mOnRecordInfoListener != null){
                        mOnRecordInfoListener.onRecordFinish();
                    }
                }
            }
        });
        mMediaRecorder.setMaxFileSize(maxSize);
        mMediaRecorder.setMaxDuration(maxTime);
        mMediaRecorder.setOutputFile(file.getAbsolutePath());
        mMediaRecorder.prepare();
        mMediaRecorder.start();
    }

    /**
     * 停止录制
     */
    public void stopMediaRecord() {
        this.cameraType = 0;
        stopRecorder();
        releaseMediaRecorder();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;
                mCamera.lock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecorder() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public boolean isCameraFrontFacing() {
        return cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    /**
     * 设置对焦类型
     * @param cameraType
     */
    public void setCameraType(int cameraType) {
        this.cameraType = cameraType;
        if (mCamera != null) {//拍摄视频时
            if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Camera.Parameters parameters = mCamera.getParameters();
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null) {
                    if (cameraType == 0) {
                        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                    } else {
                        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        }
                    }
                }
            }
        }
    }

    /**
     * 对焦
     * @param x
     * @param y
     */
    public void handleFocusMetering(float x, float y) {
        if(mCamera == null){
            return;
        }
        Camera.Parameters params = mCamera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();
        Rect focusRect = calculateTapArea(x, y, 1f, previewSize);
        Rect meteringRect = calculateTapArea(x, y, 1.5f, previewSize);
        mCamera.cancelAutoFocus();

        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 1000));
            params.setFocusAreas(focusAreas);
        } else {
        }
        if (params.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 1000));
            params.setMeteringAreas(meteringAreas);
        } else {
        }
        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mCamera.setParameters(params);

        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocusMode);
                camera.setParameters(params);
            }
        });
    }

    private Rect calculateTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / previewSize.width - 1000);
        int centerY = (int) (y / previewSize.height - 1000);
        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private OnRecordInfoListener mOnRecordInfoListener;
    public void setRecordVideoInterface(OnRecordInfoListener mOnRecordInfoListener){
        this.mOnRecordInfoListener = mOnRecordInfoListener;
    }
}
