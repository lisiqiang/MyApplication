package library.photosynthesis.cn.myapplication.ui.record.activity;


import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import library.photosynthesis.cn.myapplication.R;
import library.photosynthesis.cn.myapplication.manager.CameraManager;
import library.photosynthesis.cn.myapplication.manager.OnRecordInfoListener;
import library.photosynthesis.cn.myapplication.manager.VideoPlayerManager;
import library.photosynthesis.cn.myapplication.ui.base.BaseActivity;
import library.photosynthesis.cn.myapplication.util.MediaTimeUtils;
import library.photosynthesis.cn.myapplication.util.ScreenUtil;
import library.photosynthesis.cn.myapplication.widget.CameraView;
import library.photosynthesis.cn.myapplication.widget.RecordState;
import library.photosynthesis.cn.myapplication.widget.RecordWidget;

/**
 * Created by siqiangli on 2017/5/8 12:02
 */

public class RecordVideoActivity extends BaseActivity implements View.OnClickListener{
    private Button cancelBtn;
    private Button restartBtn;
    private RecordWidget recordWidget;
    private ImageView switchBtn;
    private Button upLoadBtn;

    private TextView timeTxt;
    private TextureView textureView,playTextureView;
    private FrameLayout playContainView;
    private CameraView cameraView;

    private CameraManager cameraManager;
    private VideoPlayerManager videoPlayerManager;
    private boolean isRecording = false;

    private Disposable recordDisposable = null;
    private Disposable playDisposable = null;

    private OrientationEventListener mOrientationListener;
    private int screenWidth,screenHeight;

    /**
     * 最长录制时间
     */
    private int timeLimit = 15000;
    /**
     * 开始时间
     */
    private long startTime;
    /**
     * 进度
     */
    private float progress;

    @Override
    public int getLayoutId() {
        return R.layout.activity_record_video;
    }

    @Override
    public void initPresenter() {
    }

    @Override
    public void initView() {
        timeTxt = (TextView)findViewById(R.id.timeTxtId);
        cancelBtn = (Button)findViewById(R.id.cancelBtnId);
        cancelBtn.setOnClickListener(this);
        restartBtn = (Button)findViewById(R.id.restartBtnId);
        restartBtn.setOnClickListener(this);
        recordWidget = (RecordWidget)findViewById(R.id.recordBtnId);
        recordWidget.setOnClickListener(this);
        switchBtn = (ImageView)findViewById(R.id.switchBtnId);
        switchBtn.setOnClickListener(this);
        upLoadBtn = (Button)findViewById(R.id.uploadBtnId);
        upLoadBtn.setOnClickListener(this);
        textureView = (TextureView) findViewById(R.id.mTextureView);
        playContainView = (FrameLayout) findViewById(R.id.playContainView);
        cameraView = (CameraView) findViewById(R.id.mCameraView);
    }

    @Override
    public void initData() {
        screenWidth = ScreenUtil.getScreenWidth(getApplicationContext());
        screenHeight = ScreenUtil.getScreenHight(getApplicationContext());
        cameraManager = CameraManager.getInstance(getApplication());
        cameraManager.setMaxTime(timeLimit);
        cameraManager.clearFile();
        cameraManager.setRecordVideoInterface(new OnRecordInfoListener() {
            @Override
            public void onRecordFinish() {
                stopRecord();
            }
        });
        videoPlayerManager = VideoPlayerManager.getInstance();
        videoPlayerManager.setMediaPlayerListener(new VideoPlayerManager.MediaPlayerListener() {
            @Override
            public void onPrepared() {
                startPlayProgressTimer();
                Toast.makeText(getApplicationContext(),"time:"+videoPlayerManager.getDuration() / 1000,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCompletion() {
                dispose(playDisposable);
                updatePlayTimeView(videoPlayerManager.getDuration(),videoPlayerManager.getDuration());
                recordWidget.setRecordOkState();
            }
        });
        cameraManager.setCameraType(0);
        cameraView.setOnViewTouchListener(new CameraView.OnViewTouchListener() {
            @Override
            public void handleFocus(float x, float y) {
                cameraManager.handleFocusMetering(x, y);
            }

            @Override
            public void handleZoom(boolean zoom) {
                cameraManager.handleZoom(zoom);
            }
        });

        mOrientationListener = new OrientationEventListener(this,SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                handlerRotation(orientation);
            }
        };

        if (mOrientationListener.canDetectOrientation()) {
            Log.v("xxa", "Can detect orientation");
            mOrientationListener.enable();
        } else {
            Log.v("xxa", "Cannot detect orientation");
            mOrientationListener.disable();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String recorderPath = cameraManager.getVideoPath();
        if (textureView.isAvailable()) {
            if (TextUtils.isEmpty(recorderPath)) {
                cameraManager.openCamera(textureView.getSurfaceTexture(),textureView.getWidth(), textureView.getHeight());
            }
        } else {
            textureView.setSurfaceTextureListener(listener);
        }
        Log.i("xxa","000000000000000000");
        if(playTextureView != null && !playTextureView.isAvailable()){
            Log.i("xxa","1111111111111111111");
            playTextureView.setSurfaceTextureListener(playListener);
        }else{
            Log.i("xxa","222222222222222222222");
            if (!TextUtils.isEmpty(recorderPath)) {
                Log.i("xxa","3333333333333333333333");
                addPlayTextureView(cameraManager.rotationDirection);
            }
        }
    }

    /**
     * camera回调监听
     */
    private TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            String recorderPath = cameraManager.getVideoPath();
            if (TextUtils.isEmpty(recorderPath)) {
                cameraManager.openCamera(textureView.getSurfaceTexture(),textureView.getWidth(), textureView.getHeight());
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private TextureView.SurfaceTextureListener playListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            String recorderPath = cameraManager.getVideoPath();
            if (!TextUtils.isEmpty(recorderPath)) {
                recordWidget.updatePlayProgress(0);
                videoPlayerManager.playMedia(new Surface(texture), cameraManager.getVideoPath());
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.recordBtnId){
            handlerRecord();
        }else if(id == R.id.restartBtnId){
            handlerRestartRecord();
        }else if(id == R.id.uploadBtnId){
            //上传文件
            upLoadFile();
        }else if(id == R.id.cancelBtnId){
            onBackPressed();
        }else if(id == R.id.switchBtnId){
            //前后置摄像切换
            cameraManager.changeCameraFacing(textureView.getSurfaceTexture(),
                    textureView.getWidth(), textureView.getHeight());
        }
    }

    private void upLoadFile(){
        String filePath = cameraManager.getVideoPath();
        Log.i("xxa","filePaht:"+filePath);
        if(!TextUtils.isEmpty(filePath)){
            //网络上传

        }
    }

    private void showView(View view){
        if(view.getVisibility() == View.GONE){
            view.setVisibility(View.VISIBLE);
        }
    }

    private void hideView(View view){
        if(view.getVisibility() == View.VISIBLE){
            view.setVisibility(View.GONE);
        }
    }

    private void handlerRestartRecord(){
        hideView(restartBtn);
        hideView(upLoadBtn);
        showView(cancelBtn);
        showView(switchBtn);
        recordWidget.setIdleState();
        videoPlayerManager.stopMedia();
        if (playContainView.getChildCount() > 0) {
            playContainView.removeAllViews();
        }
        playTextureView = null;
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        layoutParams.width = screenWidth;
        layoutParams.height = screenHeight;
        textureView.setLayoutParams(layoutParams);
        cameraManager.openCamera(textureView.getSurfaceTexture(),screenWidth,screenHeight);
        cameraManager.clearFile();
        timeTxt.setText(MediaTimeUtils.stringForTime(0));
    }

    private void handlerRecord(){
        int state = recordWidget.getRecodState();
        if(state == RecordState.IDLE){
            startRecord();
        }else if(state == RecordState.RECORD_ING){
            if(isRecording){
                stopRecord();
            }
        }else if(state == RecordState.RECORD_STOP){
            recordWidget.updatePlayProgress(0);
            //播放
            addPlayTextureView(cameraManager.rotationDirection);
            playTextureView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    videoPlayerManager.playMedia(new Surface(playTextureView.getSurfaceTexture()), cameraManager.getVideoPath());
                }
            },300);
        }else if(state == RecordState.PLAY){
            recordWidget.setPause();
            videoPlayerManager.pauseMedia();
            dispose(playDisposable);
        }else if(state == RecordState.PAUSE){
            recordWidget.setPlay();
            videoPlayerManager.resumePlay();
            startPlayProgressTimer();
        }
    }

    private void addPlayTextureView(int rotationDirection){
        Log.i("xxxxa","rotationDirection:"+rotationDirection);

        if (playContainView.getChildCount() > 0) {
            playContainView.removeAllViews();
        }
        playTextureView = new TextureView(this);
        int width = 0;
        int height = 0;
        if(rotationDirection == 0){//竖屏
            width = screenWidth;
            height = screenHeight;
            playTextureView.setRotation(0);
        }else if(rotationDirection == 90){//横屏 right
            width = screenHeight;
            height = screenWidth;
            playTextureView.setRotation(270);
        }else if(rotationDirection == 270){ //横屏 left
            width = screenHeight;
            height = screenWidth;
            playTextureView.setRotation(90);
        }

        playTextureView.setSurfaceTextureListener(playListener);
        FrameLayout.LayoutParams playParams = new FrameLayout.LayoutParams(width,height);
        playParams.gravity= Gravity.CENTER;
        playContainView.addView(playTextureView, playParams);

        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        layoutParams.width = 0;
        layoutParams.height = 0;
        textureView.setLayoutParams(layoutParams);
    }

    private void startRecord(){
        hideView(cancelBtn);
        hideView(switchBtn);
        recordWidget.setRecordProgress0(0);
        cameraManager.setCameraType(1);
        try {
            cameraManager.startMediaRecord();
            isRecording = true;
            startRecordProgressTimer();
        } catch (Exception e) {
            e.printStackTrace();
            isRecording = false;
        }
    }

    private void stopRecord(){
        showView(restartBtn);
        showView(upLoadBtn);
        progress=0;
        dispose(recordDisposable);
        recordWidget.setRecordOkState();
        cameraManager.stopMediaRecord();
        cameraManager.closeCamera();
        isRecording = false;
    }

    private void startRecordProgressTimer(){
        startTime = System.currentTimeMillis();
        Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        recordDisposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        if(progress < timeLimit){
                            progress = System.currentTimeMillis() - startTime;
                            updateRecordTimeView(progress);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void startPlayProgressTimer(){
        Observable.interval(0, 300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        playDisposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        int curTime = videoPlayerManager.getCurrentPosition();
                        int duration = videoPlayerManager.getDuration();
                        updatePlayTimeView(curTime,duration);
                        if(curTime >= duration){
                            playDisposable.dispose();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void updatePlayTimeView(int curTime,int totalTime){
        if(curTime < 0 || totalTime < 0){
            return;
        }
        int second = curTime / 1000;
        int total = totalTime / 1000;
        timeTxt.setText(MediaTimeUtils.stringForTime(curTime));
        int value = 0;
        if(total > 0){
            value = second * 100 / total ;
        }
        recordWidget.updatePlayProgress(value);
    }

    private void updateRecordTimeView(float progress) {
        //更新时间和进度按钮
        if(progress == 0){
            return;
        }
        timeTxt.setText(MediaTimeUtils.stringForTime((int)progress));
        float value = 1.0f * progress / timeLimit ;
        recordWidget.setRecordProgress0(value);
    }

    private void dispose(Disposable disposable){
        if(disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
    }

    private void handlerRotation(int rotation) {
        if (!isRecording) {
            if (((rotation >= 0) && (rotation <= 45)) || (rotation >= 315)) {
                // 竖屏拍摄
                cameraManager.setRotationFlag(0);
                cameraManager.setRotationRecord(90);
            } else if ((rotation >= 225) && (rotation < 315)) {
                // 横屏拍摄 left
                cameraManager.setRotationFlag(270);
                cameraManager.setRotationRecord(0);
            }else if((rotation > 45) && (rotation < 135)){
                //横屏拍摄 right
                cameraManager.setRotationFlag(90);
                cameraManager.setRotationRecord(180);
            }
        }
    }

    @Override
    protected void onPause() {
        if (isRecording) {
            stopRecord();
        }
        dispose(playDisposable);
        if(videoPlayerManager.isPlaying()){
            recordWidget.setPause();
            videoPlayerManager.pauseMedia();
        }
        cameraManager.closeCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cameraView.removeOnZoomListener();
        videoPlayerManager.stopMedia();
        super.onDestroy();
    }

}
