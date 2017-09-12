package library.photosynthesis.cn.myapplication.ui.base;

import android.content.Context;

/**
 * Created by siqiangli on 2017/5/8 12:01
 */
public abstract class BasePresenter<T,E>{
    public Context mContext;
    public E mModel;
    public T mView;

    public void setVM(T v, E m) {
        this.mView = v;
        this.mModel = m;
    }
}
