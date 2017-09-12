package library.photosynthesis.cn.myapplication.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import library.photosynthesis.cn.myapplication.R;
import library.photosynthesis.cn.myapplication.util.BitmapUtil;

/**
 * Created by siqiangli on 2017/5/9 16:21
 */
public class RecordWidget extends View {

    private static final String TAG = RecordWidget.class.getSimpleName();
    
    private Paint mCirclePaint;//圆形画笔 画背景圆圈
    private Paint mDrawingPaint;//进度画笔 画进度
    private Paint mStatePaint;//状态画笔 画录音、录视频图标  播放、暂停图标

    private float mRadius;
    private float mCircleLineWidth;
    private float mProgressLineWidth;

    private float mCenterX;
    private float mCenterY;

    private int mCircleBackgroundColor;
    private int mDrawingColor;
    private int mStateColor;

    private RectF mRecrodingRect;
    private RectF mCircleBounds;//下载进度
    private RectF mPlayRect1,mPlayRect2;
    private Path mStopPath;

    private float mProgressValue = 0;


    private int mState;

    private int mType = 0; //0：录语音 1：录视频

    private Bitmap recordBitmap;

    public RecordWidget(Context context) {
        super(context);
    }

    public RecordWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
        init();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RecordWidget, 0, 0);
        try {
            mType = array.getInt(R.styleable.RecordWidget_typeMode, 0);
            mRadius = array.getDimension(R.styleable.RecordWidget_circleRadius, 0);
            mCircleLineWidth = array.getDimension(R.styleable.RecordWidget_circleLineWidth, 0);
            mProgressLineWidth = array.getDimension(R.styleable.RecordWidget_progressLineWidth, 0);
            mStateColor = array.getColor(R.styleable.RecordWidget_stateColor, 0);
            mDrawingColor = array.getColor(R.styleable.RecordWidget_drawingColor, 0);
            mCircleBackgroundColor = array.getColor(R.styleable.RecordWidget_circleBackgroundColor, 0);
        } finally {
            array.recycle();
        }
    }

    private void init() {
        //圆
        mCirclePaint = new Paint();
        mCirclePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setColor(mCircleBackgroundColor);
        mCirclePaint.setStrokeWidth(mCircleLineWidth);

        //进度
        mDrawingPaint = new Paint();
        mDrawingPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mDrawingPaint.setStyle(Paint.Style.STROKE);
        mDrawingPaint.setColor(mDrawingColor);
        mDrawingPaint.setStrokeWidth(mProgressLineWidth);

        //各种状态图
        mStatePaint = new Paint();
        mStatePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mStatePaint.setColor(mStateColor);
        mStatePaint.setStyle(Paint.Style.FILL);

        mState = RecordState.IDLE;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.task_icon_audio_default);

        recordBitmap = BitmapUtil.zoomImg(bitmap,(int)mRadius,(int)mRadius);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mCenterX = w / 2f;
        mCenterY = h / 2f;

        float distance =  mRadius * 2 / 5;
        mRecrodingRect = new RectF();
        mRecrodingRect.top = mCenterY - distance;
        mRecrodingRect.left = mCenterX - distance;
        mRecrodingRect.bottom = mCenterY + distance;
        mRecrodingRect.right = mCenterX + distance;

        mCircleBounds = new RectF();
        mCircleBounds.left = mCenterX - mRadius;
        mCircleBounds.top = mCenterY - mRadius;
        mCircleBounds.right = mCenterX + mRadius;
        mCircleBounds.bottom = mCenterY + mRadius;

        mPlayRect1 = new RectF();
        mPlayRect1.top = mCenterY - mRadius * 2 / 5;
        mPlayRect1.left = mCenterX - mRadius * 2 / 5;
        mPlayRect1.bottom = mCenterY + mRadius * 2 / 5;
        mPlayRect1.right = mCenterX - mRadius / 5;

        mPlayRect2 = new RectF();
        mPlayRect2.top = mCenterY - mRadius * 2 / 5;
        mPlayRect2.left = mCenterX + mRadius / 5;
        mPlayRect2.bottom = mCenterY + mRadius * 2 / 5;
        mPlayRect2.right = mCenterX + mRadius * 2 / 5;

        float val = (float)Math.sqrt(3.0);
        mStopPath = new Path();
        mStopPath.moveTo(mCenterX- mRadius*2/5 * val/3, mCenterY-mRadius*2/5f);// 此点为多边形的起点
        mStopPath.lineTo(mCenterX+mRadius*2/5 * val*2/3, mCenterY);
        mStopPath.lineTo(mCenterX-mRadius*2/5 * val/3, mCenterY+mRadius*2/5f);
        mStopPath.close(); //使这些点构成封闭的多边形
    }


    private void resetValues() {
        mProgressValue = 0;
    }

    private void drawing(Canvas canvas) {
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mCirclePaint);
        switch (mState) {
            case RecordState.IDLE://准备状态
                if(mType == 0){//录音Icon
                    canvas.drawBitmap(recordBitmap,mCenterX-recordBitmap.getWidth()/2,mCenterY-recordBitmap.getHeight()/2,new Paint());
                }else{
                    canvas.drawCircle(mCenterX, mCenterY, mRadius * 7 / 8, mStatePaint);
                }
                break;
            case RecordState.RECORD_ING://录制中
                canvas.drawRoundRect(mRecrodingRect, 10, 10,mStatePaint);
                //画进度
                canvas.drawArc(mCircleBounds,-90,mProgressValue, false, mDrawingPaint);
                break;
            case RecordState.RECORD_STOP://录制成功
                canvas.drawPath(mStopPath,mStatePaint);
                break;
            case RecordState.PLAY://播放
                canvas.drawRoundRect(mPlayRect1, mRadius / 5, mRadius / 5,mStatePaint);
                canvas.drawRoundRect(mPlayRect2, mRadius / 5, mRadius / 5,mStatePaint);
                canvas.drawArc(mCircleBounds,-90,mProgressValue, false, mDrawingPaint);
                break;
            case RecordState.PAUSE:
                canvas.drawPath(mStopPath,mStatePaint);
                canvas.drawArc(mCircleBounds,-90,mProgressValue, false, mDrawingPaint);
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawing(canvas);
    }

    public void setIdleState(){
        mState = RecordState.IDLE;
        resetValues();
        invalidate();
    }
    
    public void setRecordOkState(){
        mState = RecordState.RECORD_STOP;
        invalidate();
    }

    public void setRecordProgress0(float value){
        mState = RecordState.RECORD_ING;
        mProgressValue = value * 360f;
        invalidate();
    }

    public void setPlay(){
        mState = RecordState.PLAY;
        invalidate();
    }

    public void updatePlayProgress(int value){
        mState = RecordState.PLAY;
        if(value > 100)
            return;
        mProgressValue = value * 3.6f;
        invalidate();
    }

    public void setPause(){
        mState = RecordState.PAUSE;
        invalidate();
    }

    public int getRecodState(){
        return mState;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mState = mState;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mState = savedState.mState;
            super.onRestoreInstanceState(savedState.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    static class SavedState extends BaseSavedState {

        private boolean isFlashing;
        private boolean isConfigurationChanged;
        private long[] mmCurrentPlayTime;
        private int mState;

        public SavedState(Parcel source) {
            super(source);
            isFlashing = source.readInt() == 1;
            isConfigurationChanged = source.readInt() == 1;
            mmCurrentPlayTime = source.createLongArray();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isFlashing ? 1 : 0);
            dest.writeInt(isConfigurationChanged ? 1 : 0);
            dest.writeLongArray(mmCurrentPlayTime);

        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
