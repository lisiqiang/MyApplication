package library.photosynthesis.cn.myapplication.widget.video;

/**
 * Created by siqiangli on 2017/5/12 15:42.
 */

public interface StarMediaPlayerListener {
    void onPrepared();

    void onCompletion();

    void onAutoCompletion();

    void onBufferingUpdate(int percent);

    void onSeekComplete();

    void onError(int what, int extra);

    void onInfo(int what, int extra);

    void onVideoSizeChanged();

    void goBackThisListener();

    boolean goToOtherListener();
}