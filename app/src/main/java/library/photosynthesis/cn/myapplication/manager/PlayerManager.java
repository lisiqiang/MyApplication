package library.photosynthesis.cn.myapplication.manager;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlayerManager {
	//系统播放器
	private MediaPlayer mediaPlayer = new MediaPlayer();
	private MediaPlayerListener listener;
	
	 /** 总共多少任务（根据CPU个数决定创建活动线程的个数,这样取的好处就是可以让手机承受得住） */
	private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
	private static List<Future> resultList = new ArrayList<Future>();
	
	private boolean isPrepared = false;
	
	//播放网络音频
	public void playNet(Context context, String url){
		if(TextUtils.isEmpty(url)){
			return;
		}
		setDataSource(context,url);
	}
	
	//播放本地音频
	public void playLocal(String filePath){
		try {
			if(TextUtils.isEmpty(filePath)){
				return;
			}
			reset();
			mediaPlayer.setDataSource(filePath);
			mediaPlayer.prepare();
			isPrepared = true;
			mediaPlayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void play(File file){
		if(file != null && file.exists()){
			try {
				reset();
				mediaPlayer.setDataSource(file.getAbsolutePath());
				mediaPlayer.prepare();
				isPrepared = true;
				mediaPlayer.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setDataSource(final Context context, final String uriPath){
		for (Future fs : resultList) {
            // 打印各个线程（任务）执行的结果  
            try {
				fs.cancel(true);
			} catch (Exception e) {
				e.printStackTrace();
			} 
        }
		resultList.clear();
		//清除上次执行的任务 （运行、等待任务）
		Future future= cachedThreadPool.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				// TODO Auto-generated method stub
				try {
					reset();
					Uri uri = Uri.parse(uriPath);
					mediaPlayer.setDataSource(context,uri);
					mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);// 设置媒体流类型
					mediaPlayer.prepareAsync();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		});
		resultList.add(future);
	}
	
	public void start(){
		if(mediaPlayer != null && isPrepared){
			mediaPlayer.start();
		}
	}
	
	public void pause(){
		if(mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()){
			mediaPlayer.pause();
		}
	}
	
	public void delVoiceFile(File file){
		if(file != null && file.exists()){
			file.delete();
			reset();
		}
	}
	
	public void reset() {
		if(mediaPlayer != null){
			if(isPrepared){
				mediaPlayer.stop();
			}
			mediaPlayer.reset();
			isPrepared = false;
		}
	}
	
	public void release(){
		if(mediaPlayer != null){
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}
	
	public boolean isPlaying() {
		if(mediaPlayer != null && isPrepared){
			return mediaPlayer.isPlaying();
		}
		return false;
	}

	public int getDuration() {
		if (mediaPlayer != null && isPrepared) {
			return mediaPlayer.getDuration();
		}
		return -1;
	}
	
	public int getCurrentPosition(){
		if (mediaPlayer != null && isPrepared) {
			return mediaPlayer.getCurrentPosition();
		}
		return -1;
	}
	
	public void seekTo(int duration) {
        if (mediaPlayer != null && isPrepared) {
        	mediaPlayer.seekTo(duration);
        }
    }
	
	public boolean isPrepared(){
		return isPrepared;
	}
	
	public void setVolume(float leftVolume, float rightVolume){
		 if (mediaPlayer != null && isPrepared) {
        	mediaPlayer.setVolume(leftVolume,rightVolume);
        }
	}
	
	public void setMediaPlayerListener(MediaPlayerListener l) {
		listener = l;
		mediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
			@Override
			public void onBufferingUpdate(MediaPlayer mp, int percent) {
				if (listener != null) {
					listener.onBufferingUpdate(percent);
				}
			}
		});
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (listener != null) {
					listener.onComplete();
				}
			}
		});
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				isPrepared = true;
				if (listener != null) {
					listener.onPrepared();
				}
			}
		});
		mediaPlayer.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				if (listener != null) {
					listener.onError("");
				}
				return false;
			}
		});
	}
	
	public interface MediaPlayerListener {
		void onComplete();

		void onPrepared();

		void onBufferingUpdate(int percent);

		void onError(String msg);
	}
}
