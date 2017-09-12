package library.photosynthesis.cn.myapplication.ui.record.model;

import library.photosynthesis.cn.myapplication.manager.OnRecordInfoListener;
import library.photosynthesis.cn.myapplication.manager.RecordManger;
import library.photosynthesis.cn.myapplication.ui.record.contract.RecordVoiceContract;

/**
 * Created by siqiangli on 2017/5/8 22:16
 */

public class RecordVoiceModel implements RecordVoiceContract.Model{

    private RecordManger recordManger = new RecordManger();
    private boolean isRecording = false;

    @Override
    public void upLoadFile(String filePath) {

    }

    @Override
    public void startRecord() {
        try {
            recordManger.clearFile();
            recordManger.startRecordCreateFile();
            isRecording = true;
        } catch (Exception e) {
            e.printStackTrace();
            isRecording = false;
        }
    }

    @Override
    public void stopRecord() {
        isRecording = false;
        recordManger.stopRecord();
    }

    @Override
    public String getVoicePath() {
        String voicePath = null;
        if(recordManger.getFile() != null){
            voicePath = recordManger.getFile().getAbsolutePath();
        }
        return voicePath;
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public double getVoiceDecibel() {
        return recordManger.getVoiceDecibel();
    }

    @Override
    public void deleteFile() {
        try {
            recordManger.clearFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMaxTime(int maxTime) {
        recordManger.setMaxTime(maxTime);
    }

    @Override
    public void setRecordVideoInterface(OnRecordInfoListener mOnRecordInfoListener) {
        recordManger.setRecordVideoInterface(mOnRecordInfoListener);
    }
}
