package library.photosynthesis.cn.myapplication.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import library.photosynthesis.cn.myapplication.event.RecordPlayEvent;
import library.photosynthesis.cn.myapplication.manager.PlayerManager;

public class MediaPlayService extends Service implements Runnable {


	private PlayerManager mPlayerManager = null;

	private int bufferPercent = 0;
	private ScheduledThreadPoolExecutor executor;
	private AudioManager mAudioManager;
	private AudioServiceBinder mBinder = new AudioServiceBinder();

	private RecordPlayEvent mRecordPlayEvent = new RecordPlayEvent();

	int playState = RecordPlayEvent.STOP;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		if (mPlayerManager == null) {
			mPlayerManager = new PlayerManager();
			mPlayerManager.setMediaPlayerListener(mMediaPlayerListener);
			mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}

	public void play(String url) {
		if (executor == null) {
			executor = new ScheduledThreadPoolExecutor(1);
			executor.scheduleAtFixedRate(this, 0, 200, TimeUnit.MILLISECONDS);
		}
		if (mPlayerManager != null) {
			int result = mAudioManager.requestAudioFocus(audioFocusChangeListener
                    , AudioManager.STREAM_MUSIC
                    , AudioManager.AUDIOFOCUS_GAIN);
            if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED != result) {
                return;
            }
            if (mPlayerManager != null) {
				bufferPercent = 0;
				if(!TextUtils.isEmpty(url)) {
					if(url.startsWith("http") || url.startsWith("https")) {
						mPlayerManager.playNet(this, url);
					}else {
						mPlayerManager.playLocal(url);
					}
				}
    		}
		}
	}

	public void resume() {
        if (mPlayerManager == null){
        	return;
        }
        if (!mPlayerManager.isPlaying()) {
        	mPlayerManager.start();
        }
    }

    public void seekTo(int duration) {
        if (mPlayerManager != null) {
        	mPlayerManager.seekTo(duration);
            resume();
        }
    }

    public boolean isPlaying() {
        if (mPlayerManager == null){
        	return false;
        }
        return mPlayerManager.isPlaying();
    }
	
	public void play() {
		int result = mAudioManager.requestAudioFocus(audioFocusChangeListener
                , AudioManager.STREAM_MUSIC
                , AudioManager.AUDIOFOCUS_GAIN);
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED != result) {
            return;
        }
		
		if (mPlayerManager != null) {
			mPlayerManager.start();
		}
	}

	public void pause() {
		if (mPlayerManager != null) {
			mPlayerManager.pause();
		}
	}

	public void stop() {
		if (mPlayerManager != null) {
			mPlayerManager.release();
		}
	}

	public void reset(){
		if (mPlayerManager != null) {
			mPlayerManager.reset();
		}
	}

	//播放状态
	public int getPlayState(){
		return playState;
	}
	
	public int getCurrentPosition(){
		return mPlayerManager.getCurrentPosition();
	}
	
	public int getDuration(){
		return mPlayerManager.getDuration();
	}
	

	public int getBufferPercent(){
		return bufferPercent;
	}
	
	private PlayerManager.MediaPlayerListener mMediaPlayerListener = new PlayerManager.MediaPlayerListener() {
		@Override
		public void onComplete() {
			// TODO Auto-generated method stub
			//解决播放完1s的误差
			if(mPlayerManager.isPrepared()){
				mRecordPlayEvent.setMsgType(RecordPlayEvent.PLAY);
				mRecordPlayEvent.setCurPosTime(mPlayerManager.getDuration());
				mRecordPlayEvent.setTotalTime(mPlayerManager.getDuration());
				EventBus.getDefault().post(mRecordPlayEvent);
			}
			playState = RecordPlayEvent.STOP;
			mRecordPlayEvent.setMsgType(RecordPlayEvent.STOP);
			EventBus.getDefault().post(mRecordPlayEvent);
		}

		@Override
		public void onPrepared() {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(),"time:"+mPlayerManager.getDuration() / 1000,Toast.LENGTH_SHORT).show();
			bufferPercent = 0;
			mPlayerManager.start();
			playState = RecordPlayEvent.PLAY;
		}

		@Override
		public void onBufferingUpdate(int percent) {
			// TODO Auto-generated method stub
			int duration = getDuration();
			if(duration > 0){
				bufferPercent = percent;
			}
		}

		@Override
		public void onError(String msg) {
			// TODO Auto-generated method stub
			playState = RecordPlayEvent.STOP;
			mRecordPlayEvent.setMsgType(RecordPlayEvent.STOP);
			EventBus.getDefault().post(mRecordPlayEvent);
		}

	};

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (mPlayerManager == null || !mPlayerManager.isPlaying()) {
            return;
        }
		playState = RecordPlayEvent.PLAY;
		//通过eventBus去更新进度
		mRecordPlayEvent.setMsgType(RecordPlayEvent.PLAY);
		mRecordPlayEvent.setCurPosTime(mPlayerManager.getCurrentPosition());
		mRecordPlayEvent.setTotalTime(mPlayerManager.getDuration());
		EventBus.getDefault().post(mRecordPlayEvent);
	}

	 AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
	        public void onAudioFocusChange(int focusChange) {
	            /**
	             * AUDIOFOCUS_GAIN：获得音频焦点。
	             * AUDIOFOCUS_LOSS：失去音频焦点，并且会持续很长时间。这是我们需要停止MediaPlayer的播放。
	             * AUDIOFOCUS_LOSS_TRANSIENT
	             * ：失去音频焦点，但并不会持续很长时间，需要暂停MediaPlayer的播放，等待重新获得音频焦点。
	             * AUDIOFOCUS_REQUEST_GRANTED 永久获取媒体焦点（播放音乐）
	             * AUDIOFOCUS_GAIN_TRANSIENT 暂时获取焦点 适用于短暂的音频
	             * AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK 我们应用跟其他应用共用焦点
	             * 我们播放的时候其他音频会降低音量
	             */

	            switch (focusChange) {
	                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
						if (mPlayerManager != null && mPlayerManager.isPlaying()){
							pause();
							playState = RecordPlayEvent.PAUSE;
							mRecordPlayEvent.setMsgType(RecordPlayEvent.PAUSE);
							EventBus.getDefault().post(mRecordPlayEvent);
						}
	                    break;
	                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	                	if (mPlayerManager != null){
	                		mPlayerManager.setVolume(0.5f, 0.5f);
	                	}
	                    break;
	                case AudioManager.AUDIOFOCUS_GAIN:
	                	if (mPlayerManager != null){
	                		mPlayerManager.setVolume(1.0f, 1.0f);
	                	}
	                    break;
	                case AudioManager.AUDIOFOCUS_LOSS:
	                	if (mPlayerManager != null && mPlayerManager.isPlaying()){
	                		pause();
							playState = RecordPlayEvent.PAUSE;
							mRecordPlayEvent.setMsgType(RecordPlayEvent.PAUSE);
							EventBus.getDefault().post(mRecordPlayEvent);
	                	}
	                    break;
	            }

	        }
	    };
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		mAudioManager.abandonAudioFocus(audioFocusChangeListener);
		
		if (mPlayerManager != null) {
			mPlayerManager.release();
		}
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
		stopForeground(true);
	}

	public class AudioServiceBinder extends Binder {
		public MediaPlayService getService() {
			return MediaPlayService.this;
		}
	}
}
