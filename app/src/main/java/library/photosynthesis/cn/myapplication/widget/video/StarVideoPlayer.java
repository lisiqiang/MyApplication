package library.photosynthesis.cn.myapplication.widget.video;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import library.photosynthesis.cn.myapplication.R;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * 参考网上demo
 *
 * Created by siqiangli on 2017/5/12 15:33.
 */

public abstract class StarVideoPlayer extends FrameLayout implements StarMediaPlayerListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener, TextureView.SurfaceTextureListener {

    public static final String TAG = "StarVideoPlayer";
    public static final int THRESHOLD                = 80;

    public static boolean ACTION_BAR_EXIST           = true;
    public static boolean TOOL_BAR_EXIST             = true;
    public static boolean WIFI_TIP_DIALOG_SHOWED     = false;

    //currentScreen以下状态
    public static final int SCREEN_LAYOUT_LIST       = 0;
    public static final int SCREEN_WINDOW_FULLSCREEN = 1;
    public static final int SCREEN_LAYOUT_DETAIL     = 2;

    //currentState以下状态
    public static final int CURRENT_STATE_NORMAL                  = 0;
    public static final int CURRENT_STATE_PREPARING               = 1;
    public static final int CURRENT_STATE_PLAYING                 = 2;
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    public static final int CURRENT_STATE_PAUSE                   = 5;
    public static final int CURRENT_STATE_AUTO_COMPLETE           = 6;
    public static final int CURRENT_STATE_ERROR                   = 7;

    public int currentState  = -1;
    public int currentScreen = -1;

    public String              url             = null;
    public Object[]            objects         = null;
    public Map<String, String> mapHeadData     = new HashMap<>();
    public int                 seekToInAdvance = -1;

    public ImageView startButton;
    public SeekBar   progressBar;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup topContainer, bottomContainer;
    public Surface surface;

    protected int               mScreenWidth;
    protected int               mScreenHeight;
    protected AudioManager mAudioManager;

    protected boolean mTouchingProgressBar;
    protected float   mDownX;
    protected float   mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected int     mDownPosition;
    protected int     mGestureDownVolume;
    protected int     mSeekTimePosition;

    private Disposable progressDisposable = null;

    public StarVideoPlayer(Context context) {
        super(context);
        init(context);
    }

    public StarVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        View view = View.inflate(context, getLayoutId(), this);
        startButton = (ImageView) view.findViewById(R.id.start);
        fullscreenButton = (ImageView) view.findViewById(R.id.fullscreen);
        progressBar = (SeekBar) view.findViewById(R.id.progress);
        currentTimeTextView = (TextView) view.findViewById(R.id.current);
        totalTimeTextView = (TextView) view.findViewById(R.id.total);
        bottomContainer = (ViewGroup) view.findViewById(R.id.layout_bottom);
        textureViewContainer = (RelativeLayout) view.findViewById(R.id.surface_container);
        topContainer = (ViewGroup) view.findViewById(R.id.layout_top);

        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);

        textureViewContainer.setOnTouchListener(this);
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    public boolean setUp(String url, int screen, Object... objects) {
        this.currentState = CURRENT_STATE_NORMAL;
        this.url = url;
        this.objects = objects;
        this.currentScreen = screen;
        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
        return true;
    }

    public boolean setUp(String url, int screen, Map<String, String> mapHeadData, Object... objects) {
        if (setUp(url, screen, objects)) {
            this.mapHeadData.clear();
            this.mapHeadData.putAll(mapHeadData);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.start) {
            if (TextUtils.isEmpty(url)) {
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) {
                if (!url.startsWith("file") && !StarUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                prepareVideo();
            } else if (currentState == CURRENT_STATE_PLAYING) {
                StarMediaManager.instance().pause();
                setUiWitStateAndScreen(CURRENT_STATE_PAUSE);
            } else if (currentState == CURRENT_STATE_PAUSE) {
                StarMediaManager.instance().start();
                setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                prepareVideo();
            }
        } else if (id == R.id.fullscreen) {
            if (currentState == CURRENT_STATE_AUTO_COMPLETE) return;
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                //全屏下点击返回小屏状态
                backPress();
            } else {
                startWindowFullscreen();
            }
        } else if (id == R.id.surface_container && currentState == CURRENT_STATE_ERROR) {
            prepareVideo();
        }
    }

    public void prepareVideo() {
        if (currentScreen != SCREEN_WINDOW_FULLSCREEN) {
            if (StarVideoPlayerManager.listener() != null) {
                StarVideoPlayerManager.listener().onCompletion();
            }
        }
        StarVideoPlayerManager.setListener(this);
        addTextureView();
        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        StarUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        StarMediaManager.instance().prepare(url,mapHeadData);
        StarMediaManager.instance().bufferPercent = 0;
        StarMediaManager.instance().videoRotation = 0;
        setUiWitStateAndScreen(CURRENT_STATE_PREPARING);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchingProgressBar = true;
                    mDownX = x;
                    mDownY = y;
                    mChangeVolume = false;
                    mChangePosition = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                        if (!mChangePosition && !mChangeVolume) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                cancelProgressTimer();
                                if (absDeltaX >= THRESHOLD) {
                                    // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                                    // 否则会因为mediaplayer的状态非法导致App Crash
                                    if (currentState != CURRENT_STATE_ERROR) {
                                        mChangePosition = true;
                                        mDownPosition = getCurrentPositionWhenPlaying();
                                    }
                                } else {
                                    mChangeVolume = true;
                                    mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        int totalTimeDuration = getDuration();
                        mSeekTimePosition = (int) (mDownPosition + deltaX * totalTimeDuration / mScreenWidth);
                        if (mSeekTimePosition > totalTimeDuration)
                            mSeekTimePosition = totalTimeDuration;
                        String seekTime = StarUtils.stringForTime(mSeekTimePosition);
                        String totalTime = StarUtils.stringForTime(totalTimeDuration);
                        showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY;
                        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                        int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);

                        showVolumeDialog(-deltaY, volumePercent);
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumeDialog();
                    if (mChangePosition) {
                        StarMediaManager.instance().seekTo(mSeekTimePosition);
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        progressBar.setProgress(progress);
                    }
                    startProgressTimer();
                    break;
            }
        }
        return false;
    }

    public void addTextureView() {
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }
        StarTextureView textureView = new StarTextureView(getContext());
        textureView.setVideoSize(StarMediaManager.instance().getVideoSize());
        textureView.setRotation(StarMediaManager.instance().videoRotation);
        textureView.setSurfaceTextureListener(this);
        StarMediaManager.instance().setTextureView(textureView);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        textureViewContainer.addView(textureView, layoutParams);
    }

    public void setUiWitStateAndScreen(int state) {
        currentState = state;
        switch (currentState) {
            case CURRENT_STATE_NORMAL:
                if (isCurrentMediaListener()) {
                    cancelProgressTimer();
                    StarMediaManager.instance().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_PREPARING:
                resetProgressAndTime();
                break;
            case CURRENT_STATE_PLAYING:
            case CURRENT_STATE_PAUSE:
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                startProgressTimer();
                break;
            case CURRENT_STATE_ERROR:
                if (isCurrentMediaListener()) {
                    StarMediaManager.instance().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                cancelProgressTimer();
                progressBar.setProgress(100);
                currentTimeTextView.setText(totalTimeTextView.getText());
                break;
        }
    }

    public void startProgressTimer() {
        cancelProgressTimer();
        Observable.interval(0, 300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        progressDisposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE || currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
                            setTextAndProgress(StarMediaManager.instance().bufferPercent);
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

    public void cancelProgressTimer() {
        if(progressDisposable != null && !progressDisposable.isDisposed()){
            progressDisposable.dispose();
        }
    }

    public void clearFullscreenLayout() {
        ViewGroup vp = (ViewGroup) (StarUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(R.id.fullScreenVideoId);
        if (oldF != null) {
            vp.removeView(oldF);
        }
        showSupportActionBar(getContext());
    }

    @Override
    public void onPrepared() {
        if (currentState != CURRENT_STATE_PREPARING) return;
        StarMediaManager.instance().start();
        if (seekToInAdvance != -1) {
            StarMediaManager.instance().seekTo(seekToInAdvance);
            seekToInAdvance = -1;
        }
        setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
    }

    @Override
    public void onAutoCompletion() {
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onCompletion();
            StarVideoPlayerManager.setListener(null);
        }
        if (StarVideoPlayerManager.lastListener() != null) {
            StarVideoPlayerManager.lastListener().onCompletion();
            StarVideoPlayerManager.setLastListener(null);
        }
    }

    @Override
    public void onCompletion() {
        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }

        StarVideoPlayerManager.setListener(null);
        StarMediaManager.instance().currentVideoWidth = 0;
        StarMediaManager.instance().currentVideoHeight = 0;

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        StarUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        clearFullscreenLayout();

    }

    @Override
    public boolean goToOtherListener() {
        if (currentScreen == StarVideoPlayer.SCREEN_WINDOW_FULLSCREEN) {
            if (StarVideoPlayerManager.lastListener() == null) {
                StarVideoPlayerManager.listener().onCompletion();
                showSupportActionBar(getContext());
                return true;
            }
            ViewGroup vp = (ViewGroup) (StarUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
            vp.removeView(this);
            StarVideoPlayerManager.setListener(StarVideoPlayerManager.lastListener());
            StarVideoPlayerManager.setLastListener(null);
            StarMediaManager.instance().lastState = currentState;//save state
            StarVideoPlayerManager.listener().goBackThisListener();
            return true;
        }
        return false;
    }

    @Override
    public void onBufferingUpdate(int percent) {
        if (currentState != CURRENT_STATE_NORMAL && currentState != CURRENT_STATE_PREPARING) {
            StarMediaManager.instance().bufferPercent = percent;
            setTextAndProgress(percent);
        }
    }

    @Override
    public void onSeekComplete() {
    }

    @Override
    public void onError(int what, int extra) {
        if (what != 38 && what != -38) {
            setUiWitStateAndScreen(CURRENT_STATE_ERROR);
        }
    }

    @Override
    public void onInfo(int what, int extra) {
        if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
            StarMediaManager.instance().backUpBufferState = currentState;
            setUiWitStateAndScreen(CURRENT_STATE_PLAYING_BUFFERING_START);
            Log.d(TAG, "MEDIA_INFO_BUFFERING_START");
        } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
            if (StarMediaManager.instance().backUpBufferState != -1) {
                setUiWitStateAndScreen(StarMediaManager.instance().backUpBufferState);
                StarMediaManager.instance().backUpBufferState = -1;
            }
            Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
        } else if (what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED) {
            StarMediaManager.instance().videoRotation = extra;
            StarMediaManager.instance().getTextureView().setRotation(extra);
            Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED");
        }

    }

    @Override
    public void onVideoSizeChanged() {
        int mVideoWidth = StarMediaManager.instance().currentVideoWidth;
        int mVideoHeight = StarMediaManager.instance().currentVideoHeight;
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            StarMediaManager.instance().getTextureView().setVideoSize(new Point(mVideoWidth, mVideoHeight));
        }
    }

    @Override
    public void goBackThisListener() {
        currentState = StarMediaManager.instance().lastState;
        setUiWitStateAndScreen(currentState);
        addTextureView();
        showSupportActionBar(getContext());
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.surface = new Surface(surface);
        StarMediaManager.instance().setDisplay(this.surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        surface.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        if(vpdown != null){
            vpdown.requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        startProgressTimer();
        ViewParent vpup = getParent();
        if(vpup != null){
            vpup.requestDisallowInterceptTouchEvent(false);
        }
        if (currentState != CURRENT_STATE_PLAYING && currentState != CURRENT_STATE_PAUSE) return;
        int time = seekBar.getProgress() * getDuration() / 100;
        StarMediaManager.instance().seekTo(time);
    }

    public static boolean backPress() {
        if (StarVideoPlayerManager.listener() != null) {
            return StarVideoPlayerManager.listener().goToOtherListener();
        }
        return false;
    }

    public void startWindowFullscreen() {
        hideSupportActionBar(getContext());
        ViewGroup vp = (ViewGroup) (StarUtils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(R.id.fullScreenVideoId);
        if (old != null) {
            vp.removeView(old);
        }
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }
        try {
            Constructor<StarVideoPlayer> constructor = (Constructor<StarVideoPlayer>) StarVideoPlayer.this.getClass().getConstructor(Context.class);
            StarVideoPlayer starVideoPlayer = constructor.newInstance(getContext());
            starVideoPlayer.setId(R.id.fullScreenVideoId);
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            int w = wm.getDefaultDisplay().getWidth();
            int h = wm.getDefaultDisplay().getHeight();
            FrameLayout.LayoutParams lp;
//            if(StarMediaManager.instance().videoRotation == 0){
//                lp = new FrameLayout.LayoutParams(h, w);
//                lp.setMargins((w - h) / 2, -(w - h) / 2, 0, 0);
//            }else{
                lp = new FrameLayout.LayoutParams(w, h);
                lp.setMargins(0, 0, 0, 0);
//            }
            vp.addView(starVideoPlayer, lp);
            starVideoPlayer.setUp(url, StarVideoPlayer.SCREEN_WINDOW_FULLSCREEN, objects);
            starVideoPlayer.setUiWitStateAndScreen(currentState);
            starVideoPlayer.addTextureView();
//            if(StarMediaManager.instance().videoRotation == 0){
//                starVideoPlayer.setRotation(90);
//            }else{
                starVideoPlayer.setRotation(0);
//            }

            final Animation ra = AnimationUtils.loadAnimation(getContext(), R.anim.start_fullscreen);
            starVideoPlayer.setAnimation(ra);

            StarVideoPlayerManager.setLastListener(this);
            StarVideoPlayerManager.setListener(starVideoPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCurrentPositionWhenPlaying() {
        int position = 0;
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE || currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            position = StarMediaManager.instance().getCurrentPosition();
        }
        return position;
    }

    public int getDuration() {
        return StarMediaManager.instance().getDuration();
    }

    public void setTextAndProgress(int secProgress) {
        int position = getCurrentPositionWhenPlaying();
        int duration = getDuration();
        int progress = position * 100 / (duration == 0 ? 1 : duration);
        setProgressAndTime(progress, secProgress, position, duration);
    }

    public void setProgressAndTime(int progress, int secProgress, int currentTime, int totalTime) {
        if (!mTouchingProgressBar) {
            if (progress != 0) progressBar.setProgress(progress);
        }
        if (secProgress > 95) secProgress = 100;
        if (secProgress != 0) progressBar.setSecondaryProgress(secProgress);
        if (currentTime != 0) currentTimeTextView.setText(StarUtils.stringForTime(currentTime));
        totalTimeTextView.setText(StarUtils.stringForTime(totalTime));
    }

    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(StarUtils.stringForTime(0));
        totalTimeTextView.setText(StarUtils.stringForTime(0));
    }

    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    StarMediaManager.instance().pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    public boolean isCurrentMediaListener() {
        return StarVideoPlayerManager.listener() != null && StarVideoPlayerManager.listener() == this;
    }

    public static void releaseAllVideos() {
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onCompletion();
        }
        if (StarVideoPlayerManager.lastListener() != null) {
            StarVideoPlayerManager.lastListener().onCompletion();
        }
        StarMediaManager.instance().releaseMediaPlayer();
    }

    public static void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = StarUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            StarUtils.getAppCompActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static void showSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = StarUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.show();
            }
        }
        if (TOOL_BAR_EXIST) {
            StarUtils.getAppCompActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void showWifiDialog() {
    }

    public void showProgressDialog(float deltaX, String seekTime, int seekTimePosition,String totalTime, int totalTimeDuration) {
    }

    public void dismissProgressDialog() {
    }

    public void showVolumeDialog(float deltaY, int volumePercent) {
    }

    public void dismissVolumeDialog() {
    }

    public abstract int getLayoutId();
}