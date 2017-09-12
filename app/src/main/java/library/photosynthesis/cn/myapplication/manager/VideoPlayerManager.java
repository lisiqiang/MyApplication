package library.photosynthesis.cn.myapplication.manager;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;

/**
 * Created by siqiangli on 2017/5/10.
 */

public class VideoPlayerManager {

    private MediaPlayer mPlayer;

    private static VideoPlayerManager INSTANCE;

    private boolean isPrepared = false;

    private int duration = 0;

    public static VideoPlayerManager getInstance() {
        if (INSTANCE == null) {
            synchronized (VideoPlayerManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new VideoPlayerManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 播放Media
     */
    public void playMedia(Surface surface, String mediaPath) {
        try {
            duration = 0;
            isPrepared = false;
            if (mPlayer == null) {
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(mediaPath);
            } else {
                if (mPlayer.isPlaying()) {
                    mPlayer.stop();
                }
                mPlayer.reset();
                mPlayer.setDataSource(mediaPath);
            }
            mPlayer.setSurface(surface);
            mPlayer.prepareAsync();
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    isPrepared = true;
                    if(mMediaPlayerListener != null){
                        mMediaPlayerListener.onPrepared();
                    }
                }
            });
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){

                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPrepared = false;
                    if(mMediaPlayerListener != null){
                        mMediaPlayerListener.onCompletion();
                    }
                }
            });
            mPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    Log.i("xxa","widht:"+width + "height:"+height);
                    if(width > height){//横屏

                    }else {//竖屏

                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            isPrepared = false;
        }
    }

    /**
     * 停止播放Media
     */
    public void stopMedia() {
        try {
            if (mPlayer != null) {
                if (mPlayer.isPlaying()) {
                    mPlayer.stop();
                }
                mPlayer.release();
                mPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying(){
        if (mPlayer != null) {
            if (isPrepared && mPlayer.isPlaying()) {
                return true;
            }
        }
        return false;
    }

    public void pauseMedia(){
        try {
            if (mPlayer != null) {
                if (isPrepared && mPlayer.isPlaying()) {
                    mPlayer.pause();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resumePlay(){
        try {
            if (mPlayer != null) {
                if (isPrepared && !mPlayer.isPlaying()) {
                    mPlayer.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getDuration() {
        if (mPlayer != null && isPrepared) {
            if(duration == 0){
                duration = mPlayer.getDuration();
            }
        }
        return duration;
    }

    public int getCurrentPosition(){
        if (mPlayer != null && isPrepared) {
            return mPlayer.getCurrentPosition();
        }
        return -1;
    }

    private MediaPlayerListener mMediaPlayerListener;

    public void setMediaPlayerListener(MediaPlayerListener mMediaPlayerListener){
        this.mMediaPlayerListener = mMediaPlayerListener;
    }

    public interface MediaPlayerListener {
        void onPrepared();
        void onCompletion();
    }

}
