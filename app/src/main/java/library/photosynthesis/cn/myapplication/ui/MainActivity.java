package library.photosynthesis.cn.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import library.photosynthesis.cn.myapplication.R;
import library.photosynthesis.cn.myapplication.ui.record.activity.RecordVideoActivity;
import library.photosynthesis.cn.myapplication.ui.record.activity.RecordVoiceActivity;
import library.photosynthesis.cn.myapplication.ui.video.activity.TestPlayVideoActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button voiceBtn,videoBtn,testVideoBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        voiceBtn = (Button)findViewById(R.id.voiceBtnId);
        voiceBtn.setOnClickListener(this);
        videoBtn = (Button)findViewById(R.id.videoBtnId);
        videoBtn.setOnClickListener(this);
        testVideoBtn = (Button)findViewById(R.id.testVideoBtnId);
        testVideoBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.voiceBtnId){
            skipRecordVoice();
        }else if(id == R.id.videoBtnId){
            skipRecordVideo();
        }else if(id == R.id.testVideoBtnId){
            skipTestPlayVideo();
        }
    }

    private void skipRecordVoice(){
        Intent intent = new Intent(getApplicationContext(),RecordVoiceActivity.class);
        this.startActivity(intent);
    }

    private void skipRecordVideo(){
        Intent intent = new Intent(getApplicationContext(),RecordVideoActivity.class);
        this.startActivity(intent);
    }

    private void skipTestPlayVideo(){

//        Uri uri = Uri.parse("http://file.shinyread.cn/project/activity/b7ca6f3d7f554367bda9572dab37c822.mp4");
//        // 调用系统自带的播放器来播放流媒体视频
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(uri, "video/mp4");
//        this.startActivity(intent);

        Intent intent = new Intent(getApplicationContext(),TestPlayVideoActivity.class);
        this.startActivity(intent);
    }
}
