package library.photosynthesis.cn.myapplication.widget.video;

import android.graphics.Point;
import android.media.AudioManager;
import android.text.TextUtils;
import android.view.Surface;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by siqiangli on 2017/5/12 15:51.
 */

public class StarMediaManager implements IMediaPlayer.OnPreparedListener,IMediaPlayer.OnCompletionListener,IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnBufferingUpdateListener,IMediaPlayer.OnSeekCompleteListener,IMediaPlayer.OnInfoListener,IMediaPlayer.OnVideoSizeChangedListener{

    public static String TAG = "StarMediaManger";

    /** 总共多少任务（根据CPU个数决定创建活动线程的个数,这样取的好处就是可以让手机承受得住） */
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private static List<Future> resultList = new ArrayList<Future>();

    private static StarMediaManager starMediaManger;
    private IjkMediaPlayer mediaPlayer;
    private StarTextureView textureView;

    public int currentVideoWidth  = 0;
    public int currentVideoHeight = 0;
    public int lastState;
    public int bufferPercent;
    public int backUpBufferState = -1;
    public int videoRotation;

    private boolean isPrepared = false;

    public static StarMediaManager instance(){
        if(starMediaManger == null){
            synchronized (StarMediaManager.class){
                if(starMediaManger == null){
                    starMediaManger = new StarMediaManager();
                }
            }
        }
        return starMediaManger;
    }

    public StarMediaManager(){
        mediaPlayer = new IjkMediaPlayer();
    }

    public Point getVideoSize(){
        return new Point(currentVideoWidth, currentVideoHeight);
    }

    public StarTextureView getTextureView() {
        return textureView;
    }

    public void setTextureView(StarTextureView textureView) {
        this.textureView = textureView;
    }

    public void prepare(final String url,final Map<String, String> mapHeadData){
        if (TextUtils.isEmpty(url)) return;
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
                    isPrepared = false;
                    currentVideoWidth = 0;
                    currentVideoHeight = 0;
                    if(mediaPlayer == null){
                        mediaPlayer = new IjkMediaPlayer();
                    }else{
                        reset();
                    }
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    if(mapHeadData != null && !mapHeadData.isEmpty()){
                        Method method = mediaPlayer.getClass().getMethod("setDataSource", new Class[] {String.class, Map.class });
                        method.invoke(mediaPlayer, new Object[] {url,mapHeadData});
                    }else{
                        mediaPlayer.setDataSource(url);
                    }
                    mediaPlayer.setOnPreparedListener(StarMediaManager.this);
                    mediaPlayer.setOnCompletionListener(StarMediaManager.this);
                    mediaPlayer.setOnErrorListener(StarMediaManager.this);
                    mediaPlayer.setOnBufferingUpdateListener(StarMediaManager.this);
                    mediaPlayer.setOnInfoListener(StarMediaManager.this);
                    mediaPlayer.setOnSeekCompleteListener(StarMediaManager.this);
                    mediaPlayer.setOnVideoSizeChangedListener(StarMediaManager.this);
                    mediaPlayer.setScreenOnWhilePlaying(true);
                    mediaPlayer.prepareAsync();
                } catch (Exception e) {
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

    public void reset() {
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            isPrepared = false;
        }
    }

    public void release(){
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return (int)mediaPlayer.getDuration();
        }
        return 0;
    }

    public int getCurrentPosition(){
        if (mediaPlayer != null && isPrepared) {
            return (int)mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int msec) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(msec);
        }
    }

    public void releaseMediaPlayer() {
        release();
    }

    public void setDisplay(Surface holder) {
            if (holder == null && mediaPlayer != null) {
                mediaPlayer.setSurface(null);
            } else {
                if (holder.isValid() && mediaPlayer != null) {
                    mediaPlayer.setSurface(holder);
                }
            }
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, final int percent) {
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onBufferingUpdate(percent);
        }
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onAutoCompletion();
        }
    }

    @Override
    public boolean onError(IMediaPlayer mp, final int what, final int extra) {
        isPrepared = false;
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onError(what, extra);
        }
        return false;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, final int what, final int extra) {
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onInfo(what, extra);
        }
        return false;
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        isPrepared = true;
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onPrepared();
        }
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onSeekComplete();
        }
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        currentVideoWidth = mp.getVideoWidth();
        currentVideoHeight = mp.getVideoHeight();
        if (StarVideoPlayerManager.listener() != null) {
            StarVideoPlayerManager.listener().onVideoSizeChanged();
        }
    }
}
