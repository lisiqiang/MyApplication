package library.photosynthesis.cn.myapplication.ui.record.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import library.photosynthesis.cn.myapplication.R;
import library.photosynthesis.cn.myapplication.event.RecordPlayEvent;
import library.photosynthesis.cn.myapplication.service.MediaPlayService;
import library.photosynthesis.cn.myapplication.ui.base.BaseActivity;
import library.photosynthesis.cn.myapplication.ui.record.contract.RecordVoiceContract;
import library.photosynthesis.cn.myapplication.ui.record.model.RecordVoiceModel;
import library.photosynthesis.cn.myapplication.ui.record.presenter.RecordVoicePresenter;
import library.photosynthesis.cn.myapplication.util.MediaTimeUtils;
import library.photosynthesis.cn.myapplication.widget.RecordState;
import library.photosynthesis.cn.myapplication.widget.RecordWidget;
import library.photosynthesis.cn.myapplication.widget.VoiceLineView;

/**
 * Created by siqiangli on 2017/5/8 12:01
 */

public class RecordVoiceActivity extends BaseActivity<RecordVoicePresenter,RecordVoiceModel> implements RecordVoiceContract.View,View.OnClickListener{

    private Button restartBtn;
    private RecordWidget recordWidget;
    private Button upLoadBtn;
    private TextView timeTxt;
    private VoiceLineView voiceLineView;
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

    private MediaPlayService mMediaPlayService;
    private Disposable recordDisposable = null;
    private Disposable voiceDisposable = null;

    private ServiceConnection mPlayConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayService.AudioServiceBinder binder = (MediaPlayService.AudioServiceBinder)service;
            mMediaPlayService = binder.getService();
            if(mMediaPlayService != null){
                mMediaPlayService.play(mPresenter.getVoicePath());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMediaPlayService = null;
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordPlayEvent(RecordPlayEvent event) {
        int state = event.getMsgType();
        int curTime = event.getCurPosTime();
        int totalTime = event.getTotalTime();
        switch (state){
            case RecordPlayEvent.PAUSE:
                recordWidget.setPause();
                break;
            case RecordPlayEvent.PLAY:
                updatePlayTimeView(curTime,totalTime);
                break;
            case RecordPlayEvent.STOP:
                recordWidget.setRecordOkState();
                break;
            default:
                break;
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_record_voice;
    }

    @Override
    public void initPresenter() {
        mPresenter.setVM(this,mModel);
    }

    @Override
    public void initView() {
        timeTxt = (TextView)findViewById(R.id.timeTxtId);
        restartBtn = (Button)findViewById(R.id.restartBtnId);
        restartBtn.setOnClickListener(this);
        recordWidget = (RecordWidget)findViewById(R.id.recordBtnId);
        recordWidget.setOnClickListener(this);
        upLoadBtn = (Button)findViewById(R.id.uploadBtnId);
        upLoadBtn.setOnClickListener(this);
        voiceLineView = (VoiceLineView)findViewById(R.id.voicLineViewId);
    }

    @Override
    public void initData() {
        mPresenter.setTimeLimit(timeLimit);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.recordBtnId){
            handlerRecord();
        }else if(id == R.id.restartBtnId){
            showRestartAndUpLoadBtn(false);
            handlerResetRecord();
        }else if(id == R.id.uploadBtnId){
            //上传
            mPresenter.upLoadFile();
        }
    }

    @Override
    public void showLoading(String title) {

    }

    @Override
    public void stopLoading() {

    }

    @Override
    public void showErrorTip(String msg) {

    }

    @Override
    public void updateRecordTimeView(float progress) {
        //更新时间和进度按钮
        if(progress == 0){
            return;
        }
        timeTxt.setText(MediaTimeUtils.stringForTime((int)progress));
        float value = 1.0f * progress / timeLimit ;
        recordWidget.setRecordProgress0(value);
    }

    @Override
    public void updatePlayTimeView(int curTime,int totalTime) {
        if(curTime < 0 || totalTime < 0){
            return;
        }
        //更新时间和进度按钮
        int second = curTime / 1000;
        int total = totalTime / 1000;
        timeTxt.setText(MediaTimeUtils.stringForTime(curTime));
        int value = 0;
        if(total > 0){
            value = second * 100 / total ;
        }
        recordWidget.updatePlayProgress(value);
    }

    @Override
    public void onRecordFinish() {
        stopRecord();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onPause() {
        if(mPresenter.isRecording()){
            stopRecord();
        }
        int state = recordWidget.getRecodState();
        if(state == RecordState.PLAY){
            recordWidget.setPause();
            if(mMediaPlayService != null){
                mMediaPlayService.pause();
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mPresenter.deleteFile();
        if(mMediaPlayService != null){
            unbindService(mPlayConn);
        }
        disposable(recordDisposable);
        disposable(voiceDisposable);
        super.onDestroy();
    }

    private void showRestartAndUpLoadBtn(boolean isShowFlag){
        restartBtn.setVisibility(isShowFlag ? View.VISIBLE:View.GONE);
        upLoadBtn.setVisibility(isShowFlag ? View.VISIBLE:View.GONE);
    }

    private void handlerResetRecord(){
        recordWidget.setIdleState();
        timeTxt.setText(R.string.record_voice_tips);
        if(mMediaPlayService != null){
            mMediaPlayService.reset();
        }
        stopCheckVoiceDbTimer();
        progress = 0;
    }

    private void handlerRecord(){
        int state = recordWidget.getRecodState();
        if(state == RecordState.IDLE){
            startRecord();
        }else if(state == RecordState.RECORD_ING){
            stopRecord();
        }else if(state == RecordState.RECORD_STOP){
            recordWidget.updatePlayProgress(0);
            if(mMediaPlayService == null){
                Intent playIntent = new Intent(this, MediaPlayService.class);
                bindService(playIntent, mPlayConn, Context.BIND_AUTO_CREATE);
            }else{
                mMediaPlayService.play(mPresenter.getVoicePath());
            }
        }else if(state == RecordState.PLAY){
            recordWidget.setPause();
            if(mMediaPlayService != null){
                mMediaPlayService.pause();
            }
        }else if(state == RecordState.PAUSE){
            recordWidget.setPlay();
            if(mMediaPlayService != null){
                mMediaPlayService.play();
            }
        }
    }

    private void startRecord(){
        recordWidget.setRecordProgress0(0);
        mPresenter.startRecord();
        startRecordProgressTimer();
        startCheckVoiceDbTimer();
    }

    private void stopRecord(){
        recordWidget.setRecordOkState();
        mPresenter.stopRecrod();
        progress = 0;
        showRestartAndUpLoadBtn(true);
        stopCheckVoiceDbTimer();
    }

    private void stopCheckVoiceDbTimer(){
        disposable(recordDisposable);
        disposable(voiceDisposable);
        voiceLineView.stop();
    }

    private void disposable(Disposable disposable){
        if(disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
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

    private void startCheckVoiceDbTimer(){
        Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        voiceDisposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        int db = (int)mPresenter.getVoiceDecibel();
                        voiceLineView.setVolume(db);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }
}
