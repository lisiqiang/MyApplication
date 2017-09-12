package library.photosynthesis.cn.myapplication.ui.record.contract;

import library.photosynthesis.cn.myapplication.manager.OnRecordInfoListener;
import library.photosynthesis.cn.myapplication.ui.base.BaseModel;
import library.photosynthesis.cn.myapplication.ui.base.BasePresenter;
import library.photosynthesis.cn.myapplication.ui.base.BaseView;

/**
 * Created by siqiangli on 2017/5/8 22:15
 */

public interface RecordVoiceContract {

    interface Model extends BaseModel {
        //数据上传
        void upLoadFile(String filePath);
        //录音管理数据逻辑
        void startRecord();
        void stopRecord();
        String getVoicePath();
        boolean isRecording();
        double getVoiceDecibel();
        void deleteFile();
        void setMaxTime(int maxTime);
        void setRecordVideoInterface(OnRecordInfoListener mOnRecordInfoListener);

    }

    interface View extends BaseView {
        void updateRecordTimeView(float progress);
        void updatePlayTimeView(int time,int totalTime);
        void onRecordFinish();
    }

    abstract static class Presenter extends BasePresenter<View, Model> {
        public abstract void startRecord();
        public abstract void stopRecrod();
        public abstract boolean isRecording();
        public abstract String getVoicePath();
        public abstract double getVoiceDecibel();
        public abstract void deleteFile();
        public abstract void setTimeLimit(int timeLimit);
        public abstract void upLoadFile();
    }
}
