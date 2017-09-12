package library.photosynthesis.cn.myapplication.ui.video.activity;

import android.view.KeyEvent;

import library.photosynthesis.cn.myapplication.R;
import library.photosynthesis.cn.myapplication.ui.base.BaseActivity;
import library.photosynthesis.cn.myapplication.util.thumbnail.ThumbnailManger;
import library.photosynthesis.cn.myapplication.widget.video.StarVideoPlayer;
import library.photosynthesis.cn.myapplication.widget.video.StarVideoPlayerStandard;

/**
 * Created by siqiangli on 2017/5/12 14:28.
 */

public class TestPlayVideoActivity extends BaseActivity {

    private StarVideoPlayerStandard videoPlayer;
    private StarVideoPlayerStandard videoPlayer1;
    private StarVideoPlayerStandard videoPlayer2;

    @Override
    public int getLayoutId() {
        return R.layout.activity_test_play_video;
    }

    @Override
    public void initPresenter() {

    }

    @Override
    public void initView() {
        videoPlayer = (StarVideoPlayerStandard) findViewById(R.id.videoPlayerId);
        videoPlayer1 = (StarVideoPlayerStandard) findViewById(R.id.videoPlayer1Id);
        videoPlayer2 = (StarVideoPlayerStandard) findViewById(R.id.videoPlayer2Id);
    }

    @Override
    public void initData() {
        String mp4Url = "/storage/emulated/0/Android/data/library.photosynthesis.cn.myapplication/files/av/xy.mp4";
        boolean resultFlag = videoPlayer.setUp(mp4Url, StarVideoPlayerStandard.SCREEN_LAYOUT_LIST,"龙斌大话电影：《入殓师》告诉生者如何生如何死");
        if(resultFlag){
            videoPlayer.getThumbImageView().setTag(mp4Url);
            ThumbnailManger.getInstance(mContext).disPlayThumbnailAndDuration(videoPlayer.getThumbImageView(),null,mp4Url,0);
//            String imgUrl = "http://vimg1.ws.126.net/image/snapshot/2015/5/O/H/VAO32ETOH.jpg";
//            Glide.with(mContext).load(imgUrl)
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .centerCrop()
//                    .error(R.mipmap.ic_launcher)
//                    .crossFade().into(videoPlayer.getThumbImageView());
        }
        String mp4Url1 = "http://file.shinyread.cn/project/stuTask/95451331fe68484b85d754f6ea2517d7.mp4";
        boolean resultFlag1 = videoPlayer1.setUp(mp4Url1, StarVideoPlayerStandard.SCREEN_LAYOUT_LIST,"牛奶咖啡献声《欢乐颂2》插曲《咖喱咖喱》，甜蜜的画面虐狗");
        if(resultFlag1){
//            String imgUrl = "http://vimg2.ws.126.net/image/snapshot/2017/5/O/C/VCJE3CEOC.jpg";
//            Glide.with(mContext).load(imgUrl)
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .centerCrop()
//                    .error(R.mipmap.ic_launcher)
//                    .crossFade().into(videoPlayer1.getThumbImageView());
        }

        String mp4Url2 = "http://file.shinyread.cn/project/activity/b7ca6f3d7f554367bda9572dab37c822.mp4";
        boolean resultFlag2 = videoPlayer2.setUp(mp4Url2, StarVideoPlayerStandard.SCREEN_LAYOUT_LIST,"牛奶咖啡献声《欢乐颂2》插曲《咖喱咖喱》，甜蜜的画面虐狗");
    }

    @Override
    protected void onPause() {
        super.onPause();
        StarVideoPlayer.releaseAllVideos();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            boolean result = StarVideoPlayer.backPress();
            if(result){
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
