package library.photosynthesis.cn.myapplication.widget.video;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

/**
 * Created by siqiangli on 2017/5/12 15:36.
 */

public class StarTextureView extends TextureView {
    private static final String TAG = "StarTextureView";

    // x as width, y as height
    private Point mVideoSize;

    public StarTextureView(Context context) {
        super(context);
        init();
    }

    public StarTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mVideoSize = new Point(0, 0);
    }

    public void setVideoSize(Point videoSize) {
        if (!mVideoSize.equals(videoSize)) {
            this.mVideoSize = videoSize;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int videoWidth = mVideoSize.x;
        int videoHeight = mVideoSize.y;
        int width = getDefaultSize(videoWidth, widthMeasureSpec);
        int height = getDefaultSize(videoHeight, heightMeasureSpec);
        Log.i("xxa", "width=" + width);
        Log.i("xxa", "height =" + height);
        Log.i("xxa", "videoWidth=" + videoWidth + ", " + "videoHeight=" + videoHeight);
        if (width > 0 && height > 0) {

            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            Log.i(TAG, "videoWidth=" + videoWidth + ", " + "videoHeight=" + videoHeight);
            Log.i(TAG, "viewRotation =" + getRotation());
            Log.i(TAG, "viewWidth  [" + MeasureSpec.toString(widthMeasureSpec) + "]");
            Log.i(TAG, "viewHeight [" + MeasureSpec.toString(heightMeasureSpec) + "]");
            float ratation = getRotation();
            if(ratation == 90 || ratation == 270){
                int value = heightSpecSize;
                heightSpecSize = widthSpecSize;
                widthSpecSize = value;
            }

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if (videoWidth * height < width * videoHeight) {
                    width = height * videoWidth / videoHeight;
                } else if (videoWidth * height > width * videoHeight) {
                    height = width * videoHeight / videoWidth;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * videoHeight / videoWidth;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * videoWidth / videoHeight;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = videoWidth;
                height = videoHeight;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * videoWidth / videoHeight;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * videoHeight / videoWidth;
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        Log.i("xxa", "viewWidth=" + width + ", " + "viewHeight=" + height);
        setMeasuredDimension(width, height);
    }
}
