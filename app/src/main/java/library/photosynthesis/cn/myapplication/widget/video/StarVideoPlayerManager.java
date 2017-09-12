package library.photosynthesis.cn.myapplication.widget.video;

import java.lang.ref.WeakReference;

/**
 * Created by siqiangli on 2017/5/12 16:12.
 */

public class StarVideoPlayerManager {
    private static WeakReference<StarMediaPlayerListener> LISTENER;
    private static WeakReference<StarMediaPlayerListener> LAST_LISTENER;

    public static StarMediaPlayerListener listener() {
        if (LISTENER == null)
            return null;
        return LISTENER.get();
    }

    public static StarMediaPlayerListener lastListener() {
        if (LAST_LISTENER == null)
            return null;
        return LAST_LISTENER.get();
    }

    public static void setListener(StarMediaPlayerListener listener) {
        if (listener == null)
            LISTENER = null;
        else
            LISTENER = new WeakReference<>(listener);
    }

    public static void setLastListener(StarMediaPlayerListener lastListener) {
        if (lastListener == null)
            LAST_LISTENER = null;
        else
            LAST_LISTENER = new WeakReference<>(lastListener);
    }
}
