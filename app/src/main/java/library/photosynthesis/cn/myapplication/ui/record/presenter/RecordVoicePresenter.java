package library.photosynthesis.cn.myapplication.ui.record.presenter;

import android.text.TextUtils;

import library.photosynthesis.cn.myapplication.manager.OnRecordInfoListener;
import library.photosynthesis.cn.myapplication.ui.record.contract.RecordVoiceContract;

/**
 * Created by siqiangli on 2017/5/8 22:16
 */

public class RecordVoicePresenter extends RecordVoiceContract.Presenter{

    @Override
    public void startRecord() {
        mModel.setRecordVideoInterface(new OnRecordInfoListener() {
            @Override
            public void onRecordFinish() {
                mView.onRecordFinish();
            }
        });
        mModel.startRecord();
    }

    @Override
    public void stopRecrod() {
        mModel.stopRecord();
    }

    @Override
    public boolean isRecording() {
        return mModel.isRecording();
    }

    @Override
    public String getVoicePath() {
        return mModel.getVoicePath();
    }

    @Override
    public double getVoiceDecibel() {
        return mModel.getVoiceDecibel();
    }

    @Override
    public void deleteFile() {
            mModel.deleteFile();
    }

    @Override
    public void setTimeLimit(int timeLimit) {
        mModel.setMaxTime(timeLimit);
    }

    @Override
    public void upLoadFile() {
        //上传成功删除录音文件
        String filePath = mModel.getVoicePath();
        if(!TextUtils.isEmpty(filePath)){
            mModel.upLoadFile(filePath);
        }
    }
}
