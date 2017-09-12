package library.photosynthesis.cn.myapplication.widget.video;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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
import library.photosynthesis.cn.myapplication.util.BitmapUtil;

/**
 * Created by siqiangli on 2017/5/12 16:15.
 */

public class StarVideoPlayerStandard extends StarVideoPlayer{

    private ImageView backButton;
    private ProgressBar loadingProgressBar;
    private TextView titleTextView;
    private ImageView coverImageView;
    private ImageView thumbImageView;//视频缩略图

    private static Bitmap pauseSwitchCoverBitmap = null;
    private static boolean isRefreshCover         = false;

    private Disposable controlShowDisposable = null;

    public StarVideoPlayerStandard(Context context) {
        super(context);
    }

    public StarVideoPlayerStandard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getLayoutId() {
        return R.layout.star_video_layout_standard;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        titleTextView = (TextView) findViewById(R.id.title);
        backButton = (ImageView) findViewById(R.id.back);
        coverImageView = (ImageView) findViewById(R.id.cover);
        thumbImageView = (ImageView) findViewById(R.id.thumb);
        loadingProgressBar = (ProgressBar) findViewById(R.id.loading);
        backButton.setOnClickListener(this);
        thumbImageView.setOnClickListener(this);
    }

    public ImageView getThumbImageView(){
        return  thumbImageView;
    }

    @Override
    public boolean setUp(String url, int screen, Object... objects) {
        if (objects.length == 0) return false;
        if (super.setUp(url, screen, objects)) {
            if (pauseSwitchCoverBitmap != null && coverImageView.getBackground() == null) {
                coverImageView.setBackgroundColor(Color.parseColor("#222222"));//防止在复用的时候导致，闪一下上次暂停切换缓存的图的问题
            }
            if (objects.length > 0){
                titleTextView.setText(objects[0].toString());
            }
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                fullscreenButton.setImageResource(R.drawable.star_video_fullscreen);
                backButton.setVisibility(View.VISIBLE);
            } else if (currentScreen == SCREEN_LAYOUT_LIST) {
                fullscreenButton.setImageResource(R.drawable.star_video_small);
                backButton.setVisibility(View.GONE);
            }
            return true;
        }
        return false;
    }

    @Override
    public void setUiWitStateAndScreen(int state) {
        super.setUiWitStateAndScreen(state);
        switch (currentState) {
            case CURRENT_STATE_NORMAL:
                changeUiToNormal();
                break;
            case CURRENT_STATE_PREPARING:
                changeUiToPreparingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING:
                changeUiToPlayingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PAUSE:
                changeUiToPauseShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_ERROR:
                changeUiToError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                changeUiToCompleteShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                changeUiToPlayingBufferingShow();
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    if (!mChangePosition && !mChangeVolume) {
                        onClickUiToggle();
                    }
                    break;
            }
        } else if (id == R.id.progress) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelDismissControlViewTimer();
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    break;
            }
        }
        return super.onTouch(v, event);
    }

    @Override
    public void addTextureView() {
        super.addTextureView();
//        coverImageView.setRotation(StarMediaManager.instance().videoRotation);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int id = v.getId();
        if(id == R.id.thumb){
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL) {
                if (!url.startsWith("file") && !StarUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                startPlayLogic();
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onClickUiToggle();
            }
        } else if (id == R.id.surface_container) {
            startDismissControlViewTimer();
        } else if (id == R.id.back) {
            backPress();
        } else if (id == R.id.start) {
            if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_PAUSE) {
                isRefreshCover = true;//播放会才会刷新图缓存
            }
        }
    }

    @Override
    public void prepareVideo() {
        coverImageView.setBackgroundColor(Color.parseColor("#222222"));
        coverImageView.setImageBitmap(null);
        super.prepareVideo();
    }

    @Override
    public void startWindowFullscreen() {
        obtainCover();
        super.startWindowFullscreen();
        if (currentState == CURRENT_STATE_PAUSE) {
            refreshCover(pauseSwitchCoverBitmap);
        }
    }

    private void obtainCover() {
        if (currentState == CURRENT_STATE_PAUSE) {
            if (isRefreshCover) {
                pauseSwitchCoverBitmap = BitmapUtil.rotateBitmap(StarMediaManager.instance().getTextureView().getBitmap(),StarMediaManager.instance().videoRotation);
                Log.i("xxa","pauseSwitchCoverBitmap:"+"w:"+pauseSwitchCoverBitmap.getWidth()+ " h:"+pauseSwitchCoverBitmap.getHeight());
                isRefreshCover = false;
            }
        }
    }

    @Override
    public boolean goToOtherListener() {
        obtainCover();
        boolean b = super.goToOtherListener();
        if (currentState == CURRENT_STATE_PAUSE) {
            refreshCover(pauseSwitchCoverBitmap);
        }
        return b;
    }


    public void refreshCover(Bitmap bitmap) {
        if (pauseSwitchCoverBitmap != null) {
            StarVideoPlayerStandard starVideoPlayerStandard = getCurStarVideoPlayerStandard();
            if(starVideoPlayerStandard != null){
                starVideoPlayerStandard.coverImageView.setBackgroundColor(Color.parseColor("#000000"));
                starVideoPlayerStandard.coverImageView.setImageBitmap(bitmap);
                starVideoPlayerStandard.coverImageView.setVisibility(VISIBLE);
            }
        }
    }

    private StarVideoPlayerStandard getCurStarVideoPlayerStandard(){
        try {
            StarVideoPlayerStandard starVideoPlayerStandard = ((StarVideoPlayerStandard) StarVideoPlayerManager.listener());
            return starVideoPlayerStandard;
        }catch (Exception e){
        }
        return null;
    }

    @Override
    public void showWifiDialog() {
        super.showWifiDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(getResources().getString(R.string.tips_not_wifi));
        builder.setPositiveButton(getResources().getString(R.string.tips_not_wifi_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startPlayLogic();
                WIFI_TIP_DIALOG_SHOWED = true;
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.tips_not_wifi_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        cancelDismissControlViewTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        if (currentState == CURRENT_STATE_PAUSE) {
            isRefreshCover = true;
        }
        startDismissControlViewTimer();
    }

    public void startPlayLogic() {
        prepareVideo();
        startDismissControlViewTimer();
    }

    public void onClickUiToggle() {
        if (currentState == CURRENT_STATE_PREPARING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPreparingClear();
            } else {
                changeUiToPreparingShow();
            }
        } else if (currentState == CURRENT_STATE_PLAYING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingClear();
            } else {
                changeUiToPlayingShow();
            }
        } else if (currentState == CURRENT_STATE_PAUSE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPauseClear();
            } else {
                changeUiToPauseShow();
            }
        } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToCompleteClear();
            } else {
                changeUiToCompleteShow();
            }
        } else if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingBufferingClear();
            } else {
                changeUiToPlayingBufferingShow();
            }
        }
    }

    public void changeUiToNormal() {
        setAllControlsVisible(View.VISIBLE, View.INVISIBLE, View.VISIBLE,View.INVISIBLE, View.VISIBLE, View.VISIBLE);
        updateStartImage();
    }

    public void changeUiToPreparingShow() {
        setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.INVISIBLE,View.VISIBLE, View.INVISIBLE, View.VISIBLE);
    }

    public void changeUiToPreparingClear() {
        setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,View.VISIBLE, View.INVISIBLE, View.VISIBLE);
    }

    public void changeUiToPlayingShow() {
        setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
        updateStartImage();
    }

    public void changeUiToPlayingClear() {
        setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
    }

    public void changeUiToPauseShow() {
        setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,View.INVISIBLE, View.INVISIBLE, coverImageView.getVisibility());
        updateStartImage();
    }

    public void changeUiToPauseClear() {
        setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,View.INVISIBLE, View.INVISIBLE, coverImageView.getVisibility());
    }

    public void changeUiToPlayingBufferingShow() {
        setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.INVISIBLE,View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
    }

    public void changeUiToPlayingBufferingClear() {
        setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
        updateStartImage();
    }

    public void changeUiToCompleteShow() {
        setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
        updateStartImage();
    }

    public void changeUiToCompleteClear() {
        setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.VISIBLE,View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
        updateStartImage();
    }

    public void changeUiToError() {
        setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.VISIBLE,View.INVISIBLE, View.INVISIBLE, View.VISIBLE);
        updateStartImage();
    }

    public void setAllControlsVisible(int topCon, int bottomCon, int startBtn, int loadingPro,int thumbImg, int coverImg) {
        topContainer.setVisibility(topCon);
        bottomContainer.setVisibility(bottomCon);
        startButton.setVisibility(startBtn);
        loadingProgressBar.setVisibility(loadingPro);
        thumbImageView.setVisibility(thumbImg);
        coverImageView.setVisibility(coverImg);
    }

    public void updateStartImage() {
        if (currentState == CURRENT_STATE_PLAYING) {
            startButton.setImageResource(R.drawable.star_video_click_pause_selector);
        } else if (currentState == CURRENT_STATE_ERROR) {
            startButton.setImageResource(R.drawable.star_video_click_error_selector);
        } else {
            startButton.setImageResource(R.drawable.star_video_click_play_selector);
        }
    }

    protected Dialog mProgressDialog;
    protected ProgressBar mDialogProgressBar;
    protected TextView    mDialogSeekTime;
    protected TextView    mDialogTotalTime;
    protected ImageView   mDialogIcon;

    @Override
    public void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        super.showProgressDialog(deltaX, seekTime, seekTimePosition, totalTime, totalTimeDuration);
        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.star_video_progress_dialog, null);
            View content = localView.findViewById(R.id.content);
            content.setRotation(this.getRotation());
            mDialogProgressBar = ((ProgressBar) localView.findViewById(R.id.duration_progressbar));
            mDialogSeekTime = ((TextView) localView.findViewById(R.id.tv_current));
            mDialogTotalTime = ((TextView) localView.findViewById(R.id.tv_duration));
            mDialogIcon = ((ImageView) localView.findViewById(R.id.duration_image_tip));
            mProgressDialog = new Dialog(getContext(),R.style.jc_style_dialog_progress);
            mProgressDialog.setContentView(localView);
            mProgressDialog.getWindow().addFlags(Window.FEATURE_ACTION_BAR);
            mProgressDialog.getWindow().addFlags(32);
            mProgressDialog.getWindow().addFlags(16);
            mProgressDialog.getWindow().setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = mProgressDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.CENTER;
            mProgressDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        mDialogSeekTime.setText(seekTime);
        mDialogTotalTime.setText(" / " + totalTime);
        mDialogProgressBar.setProgress(totalTimeDuration <= 0 ? 0 : (seekTimePosition * 100 / totalTimeDuration));
        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.star_video_forward);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.star_video_backward);
        }

    }

    @Override
    public void dismissProgressDialog() {
        super.dismissProgressDialog();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }


    protected Dialog      mVolumeDialog;
    protected ProgressBar mDialogVolumeProgressBar;

    @Override
    public void showVolumeDialog(float deltaY, int volumePercent) {
        super.showVolumeDialog(deltaY, volumePercent);
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.star_video_volume_dialog, null);
            View content = localView.findViewById(R.id.content);
            content.setRotation(this.getRotation());
            mDialogVolumeProgressBar = ((ProgressBar) localView.findViewById(R.id.volume_progressbar));
            mVolumeDialog = new Dialog(getContext(), R.style.jc_style_dialog_progress);
            mVolumeDialog.setContentView(localView);
            mVolumeDialog.getWindow().addFlags(8);
            mVolumeDialog.getWindow().addFlags(32);
            mVolumeDialog.getWindow().addFlags(16);
            mVolumeDialog.getWindow().setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = mVolumeDialog.getWindow().getAttributes();
            if(this.getRotation() == 90){
                localLayoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            }else{
                localLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
            }
            mVolumeDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }

        mDialogVolumeProgressBar.setProgress(volumePercent);
    }

    @Override
    public void dismissVolumeDialog() {
        super.dismissVolumeDialog();
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
        }
    }

    public void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();

        Observable.just(0L)
                .delay(2500,TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        controlShowDisposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        if (currentState != CURRENT_STATE_NORMAL && currentState != CURRENT_STATE_ERROR && currentState != CURRENT_STATE_AUTO_COMPLETE) {
                                bottomContainer.setVisibility(View.INVISIBLE);
                                topContainer.setVisibility(View.INVISIBLE);
                                startButton.setVisibility(View.INVISIBLE);
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

    public void cancelDismissControlViewTimer() {
        if(controlShowDisposable != null && !controlShowDisposable.isDisposed()){
            controlShowDisposable.dispose();
        }
    }
}
