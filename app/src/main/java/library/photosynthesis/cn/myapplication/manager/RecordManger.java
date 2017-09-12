package library.photosynthesis.cn.myapplication.manager;

import android.media.MediaRecorder;

import java.io.File;

import library.photosynthesis.cn.myapplication.MyApplication;
import library.photosynthesis.cn.myapplication.util.FileUtil;

public class RecordManger {
	/**
	 * 最大录制时间
	 */
	private int maxTime=15000;
	/**
	 * 最大录制大小 默认60m
	 */
	private long maxSize=60*1024*1024;
    /**
	 * 录音后文件
	 */
	private File file; 
    /**
	 * android媒体录音类
	 */
	private MediaRecorder mr;
    /**
	 * 声波振幅监听器
	 */
    private SoundAmplitudeListen soundAmplitudeListen;
    private boolean isPrepared;// 是否准备好了

	private OnRecordInfoListener mOnRecordInfoListener;

	public void setRecordVideoInterface(OnRecordInfoListener mOnRecordInfoListener){
		this.mOnRecordInfoListener = mOnRecordInfoListener;
	}

	public void setMaxTime(int maxTime){
		this.maxTime = maxTime;
	}

	public int getMaxTime(){
		return maxTime;
	}

	public double getVoiceDecibel(){
		if(mr == null || !isPrepared) return 0;
		double ratio = (double) mr.getMaxAmplitude() / 100;
		double db = 0;// 分贝
		//默认的最大音量是100,可以修改，但其实默认的，在测试过程中就有不错的表现
		//你可以传自定义的数字进去，但需要在一定的范围内，比如0-200，就需要在xml文件中配置maxVolume
		//同时，也可以配置灵敏度sensibility
		if (ratio > 1)
			db = 20 * Math.log10(ratio);
		return db;
	}

	// 获得声音的level
	public int getVoiceLevel(int maxLevel) {
		if(isPrepared){
			// mRecorder.getMaxAmplitude()这个是音频的振幅范围，值域是1-32767
			try {
				// 取证+1，否则去不到7
				return maxLevel * mr.getMaxAmplitude() / 32768 + 1;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
			}
		}
		return 1;
	}
	
    /**启动录音并生成文件*/
	public synchronized void startRecordCreateFile() throws Exception {
		String fileDir = MyApplication.getInstance().getSaveFileDir();
		String fileName = System.currentTimeMillis() + "xy";
		boolean hasFile = FileUtil.createFile(fileDir,fileName+".aac");
		isPrepared = false;
		file = new File(fileDir,fileName + ".aac");
		mr = new MediaRecorder(); // 创建录音对象
		//amr
//		mr.setAudioSource(MediaRecorder.AudioSource.DEFAULT);// 从麦克风源进行录音
//		mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);// 设置输出格式
//		mr.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);// 设置编码格式

		//aac
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
		mr.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
		// 设置音频编码为AAC
		mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

		mr.setOnInfoListener(new MediaRecorder.OnInfoListener() {
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
					if(mOnRecordInfoListener != null){
						mOnRecordInfoListener.onRecordFinish();
					}
				}
			}
		});
		mr.setMaxFileSize(maxSize);
		mr.setMaxDuration(maxTime);
		mr.setOutputFile(file.getAbsolutePath());// 设置输出文件
		// 准备录制
		mr.prepare();
		// 开始录制
		mr.start();
		isPrepared = true;
	}
	
	public boolean isRecordStart(){
		return isPrepared;
	}
	
    /**停止录音并返回录音文件*/
	public synchronized File stopRecord() {
		if (mr != null) {
			try {
				isPrepared = false;
				mr.stop();
				mr.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
			mr = null;
		}
		return file;
	}

	public void clearFile(){
		FileUtil.delFile(file);
		file = null;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public MediaRecorder getMr() {
		return mr;
	}

	public void setMr(MediaRecorder mr) {
		this.mr = mr;
	}
	
	public SoundAmplitudeListen getSoundAmplitudeListen() {
		return soundAmplitudeListen;
	}

	public void setSoundAmplitudeListen(SoundAmplitudeListen soundAmplitudeListen) {
		this.soundAmplitudeListen = soundAmplitudeListen;
	}
   public interface SoundAmplitudeListen{
	   public void amplitude(int amplitude, int db, int value);
   }
}
