package library.photosynthesis.cn.myapplication.util.thumbnail;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import library.photosynthesis.cn.myapplication.util.MediaTimeUtils;

/**
 * Created by siqiangli on 2017/5/17.
 */
public class ThumbnailManger {
    private LruCache<String, MediaDataBean> mMemoryCache;//内存缓存
    private DiskLruCache mDiskCache;//磁盘缓存
    private static ThumbnailManger mInstance;//获取图片下载单例引用
    private HashMap<String,String> thumbnailMap;
    /**
     * 构造器
     *
     * @param context
     */
    private ThumbnailManger(Context context) {
        int maxMemory = (int) Runtime.getRuntime().maxMemory();//获取系统分配给应用的总内存大小
        int mCacheSize = maxMemory / 8;//设置图片内存缓存占用八分之一
        mMemoryCache = new LruCache<String, MediaDataBean>(mCacheSize) {
            //必须重写此方法，来测量Bitmap的大小
            @Override
            protected int sizeOf(String key, MediaDataBean value) {

                if (value.videoDrawable !=null && value.videoDrawable instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) value.videoDrawable).getBitmap();
                    return bitmap == null ? 0 : bitmap.getByteCount();
                }
                return super.sizeOf(key, value);
            }
        };

        thumbnailMap = new HashMap<String,String>();

        File cacheDir = context.getCacheDir();//指定的是数据的缓存地址
        long diskCacheSize = 1024 * 1024 * 30;//最多可以缓存多少字节的数据
        int appVersion = DiskLruUtils.getAppVersion(context);//指定当前应用程序的版本号
        int valueCount = 1;//指定同一个key可以对应多少个缓存文件
        try {
            mDiskCache = DiskLruCache.open(cacheDir, appVersion, valueCount, diskCacheSize);
        } catch (Exception ex) {
        }
    }

    /**
     * 获取单例引用
     *
     * @return
     */
    public static ThumbnailManger getInstance(Context context) {
        if (mInstance == null) {
            synchronized (ThumbnailManger.class) {
                if (mInstance == null) {
                    mInstance = new ThumbnailManger(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    /**
     *
     * @param imageView
     * @param duraionTxt
     * @param videoUrl
     * @param fileType 0:video 1:audio
     */
    public void disPlayThumbnailAndDuration(final ImageView imageView, final TextView duraionTxt, final String fileUrl, final int fileType) {
        //生成唯一key
        final String key = DiskLruUtils.hashKeyForDisk(fileUrl);
        final String keyDuration = DiskLruUtils.hashKeyForDisk(fileUrl+"duration");
        //先从内存中读取
        final MediaDataBean mediaDataBeanFromMemCache = getMediaDataBeanFromMemCache(key);
        if (mediaDataBeanFromMemCache != null) {
            if (imageView != null && imageView.getTag() != null && imageView.getTag().equals(fileUrl)) {
                imageView.setImageDrawable(mediaDataBeanFromMemCache.videoDrawable);
            }
            if(duraionTxt != null && duraionTxt.getTag() != null && duraionTxt.getTag().equals(fileUrl)) {
                if(!TextUtils.isEmpty(mediaDataBeanFromMemCache.duration)){
                    duraionTxt.setText(MediaTimeUtils.stringForTime(Integer.valueOf(mediaDataBeanFromMemCache.duration)));
                }
            }
            return;
        }

        Observable.just(fileUrl)
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(@NonNull String s) throws Exception {
                        return !thumbnailMap.containsKey(s);
                    }
                })
                .map(new Function<String, MediaDataBean>() {
                    @Override
                    public MediaDataBean apply(@NonNull String s) throws Exception {
                        MediaDataBean mediaDataBean = new MediaDataBean();
                        //从磁盘中读取
                        Drawable drawableFromDiskCache = getDrawableFromDiskCache(key);
                        String duration = getStringFromDiskCache(keyDuration);
                        if (drawableFromDiskCache != null || !TextUtils.isEmpty(duration)) {
                            mediaDataBean.videoDrawable = drawableFromDiskCache;
                            mediaDataBean.duration = duration;
                            addMediaDataBeanToMemoryCache(key,mediaDataBean);
                            return mediaDataBean;
                        }
                        //视频中获取
                        if(fileType == 0){
                            int w = imageView.getWidth();
                            int h = imageView.getHeight();
                            return createVideoThumbnail(s,w,h);
                        }
                        return createAudioBean(s);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<MediaDataBean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onNext(@NonNull MediaDataBean mediaDataBean) {
                        if(mediaDataBean == null) return;
                        if (imageView != null && imageView.getTag() != null && imageView.getTag().equals(fileUrl)) {
                            imageView.setImageDrawable(mediaDataBean.videoDrawable);
                        }
                        if(duraionTxt != null && duraionTxt.getTag() != null && duraionTxt.getTag().equals(fileUrl)) {
                            if(!TextUtils.isEmpty(mediaDataBean.duration)){
                                duraionTxt.setText(MediaTimeUtils.stringForTime(Integer.valueOf(mediaDataBean.duration)));
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        thumbnailMap.remove(fileUrl);
                    }

                    @Override
                    public void onComplete() {
                        thumbnailMap.remove(fileUrl);
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private MediaDataBean createVideoThumbnail(String url, int width, int height) {
        thumbnailMap.put(url,url);
        MediaDataBean mediaDataBean = null;
        Bitmap bitmap = null;
        String metadata = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int kind = MediaStore.Video.Thumbnails.MINI_KIND;
        try {
            if (Build.VERSION.SDK_INT >= 14) {
                retriever.setDataSource(url, new HashMap<String, String>());
            } else {
                retriever.setDataSource(url);
            }
            bitmap = retriever.getFrameAtTime();
            metadata = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        if (kind == MediaStore.Images.Thumbnails.MICRO_KIND && bitmap != null) {
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        String key = DiskLruUtils.hashKeyForDisk(url);
        Drawable drawable = DiskLruUtils.bitmap2Drawable(bitmap);
        //加入磁盘缓存
        addBitmapToDiskCache(key, DiskLruUtils.bitmap2Bytes(bitmap));
        //缓存duration
        String keyDuration = DiskLruUtils.hashKeyForDisk(url+"duration");
        addStringToDiskCache(keyDuration,metadata);

        mediaDataBean = new MediaDataBean();
        mediaDataBean.duration = metadata;
        mediaDataBean.videoDrawable = drawable;

        //加入内存缓存
        addMediaDataBeanToMemoryCache(key,mediaDataBean);

        return mediaDataBean;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private MediaDataBean createAudioBean(String url) {
        thumbnailMap.put(url,url);
        MediaDataBean mediaDataBean = null;
        String metadata = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (Build.VERSION.SDK_INT >= 14) {
                retriever.setDataSource(url, new HashMap<String, String>());
            } else {
                retriever.setDataSource(url);
            }
            metadata = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        //缓存duration
        String key = DiskLruUtils.hashKeyForDisk(url);
        addStringToDiskCache(key,metadata);

        mediaDataBean = new MediaDataBean();
        mediaDataBean.duration = metadata;
        //加入内存缓存
        addMediaDataBeanToMemoryCache(key, mediaDataBean);
        return mediaDataBean;
    }

    /**
     * 添加Drawable到内存缓存
     *
     * @param key
     * @param drawable
     */
    private void addMediaDataBeanToMemoryCache(String key, MediaDataBean mediaDataBean) {
        if (getMediaDataBeanFromMemCache(key) == null && mediaDataBean != null) {
            mMemoryCache.put(key,mediaDataBean);
        }
    }

    /**
     * 从内存缓存中获取一个Drawable
     *
     * @param key
     * @return
     */
    public MediaDataBean getMediaDataBeanFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * 从磁盘缓存中获取一个Drawable
     *
     * @param key
     * @return
     */
    public Drawable getDrawableFromDiskCache(String key) {
        try {
            DiskLruCache.Snapshot snapShot = mDiskCache.get(key);
            if (snapShot != null) {
                InputStream is = snapShot.getInputStream(0);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                Drawable drawable = DiskLruUtils.bitmap2Drawable(bitmap);
                return drawable;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getStringFromDiskCache(String key){
        try {
            DiskLruCache.Snapshot snapShot = mDiskCache.get(key);
            if (snapShot != null) {
                String content = snapShot.getString(0);
                return content;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addStringToDiskCache(String key, String data){
        if(TextUtils.isEmpty(data)){
            return;
        }
        InputStream is = new ByteArrayInputStream(data.getBytes());
        BufferedInputStream bufferedInputStream = new BufferedInputStream(is);
        OutputStream out = null;
        try {
            DiskLruCache.Editor editor = mDiskCache.edit(key);
            if (editor != null) {
                out = editor.newOutputStream(0);
                int line;
                while ((line = bufferedInputStream.read()) != -1){
                    out.write(line);
                }
                editor.commit();
            }
            mDiskCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            DiskLruUtils.closeQuietly(out);
        }
    }

    /**
     * 添加Bitmap到磁盘缓存
     *
     * @param key
     * @param value
     */
    private void addBitmapToDiskCache(String key, byte[] value) {
        OutputStream out = null;
        try {
            DiskLruCache.Editor editor = mDiskCache.edit(key);
            if (editor != null) {
                out = editor.newOutputStream(0);
                if (value != null && value.length > 0) {
                    out.write(value);
                    out.flush();
                    editor.commit();
                } else {
                    editor.abort();
                }
            }
            mDiskCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            DiskLruUtils.closeQuietly(out);
        }
    }

    /**
     * 从缓存中移除
     *
     * @param key
     */
    public void removeCache(String key) {
        removeCacheFromMemory(key);
        removeCacheFromDisk(key);
    }

    /**
     * 从内存缓存中移除
     *
     * @param key
     */
    public void removeCacheFromMemory(String key) {
        mMemoryCache.remove(key);
    }

    /**
     * 从磁盘缓存中移除
     *
     * @param key
     */
    public void removeCacheFromDisk(String key) {
        try {
            mDiskCache.remove(key);
        } catch (Exception e) {
        }
    }

    /**
     * 磁盘缓存大小
     *
     * @return
     */
    public long diskCacheSize() {
        return mDiskCache.size();
    }

    /**
     * 内存缓存大小
     *
     * @return
     */
    public long memoryCacheSize() {
        return mMemoryCache.size();
    }

    /**
     * 关闭磁盘缓存
     */
    public void closeDiskCache() {
        try {
            mDiskCache.close();
        } catch (Exception e) {
        }
    }

    /**
     * 清理缓存
     */
    public void cleanCache() {
        cleanMemoryCCache();
        cleanDiskCache();
    }

    /**
     * 清理磁盘缓存
     */
    public void cleanDiskCache() {
        try {
            mDiskCache.delete();
        } catch (Exception e) {
        }
    }

    /**
     * 清理内存缓存
     */
    public void cleanMemoryCCache() {
        mMemoryCache.evictAll();
    }
}
