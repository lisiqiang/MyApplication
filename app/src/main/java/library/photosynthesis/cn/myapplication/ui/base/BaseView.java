package library.photosynthesis.cn.myapplication.ui.base;

/**
 * Created by siqiangli on 2017/5/8 12:01
 */
public interface BaseView {
    /*******内嵌加载*******/
    void showLoading(String title);
    void stopLoading();
    void showErrorTip(String msg);
}
