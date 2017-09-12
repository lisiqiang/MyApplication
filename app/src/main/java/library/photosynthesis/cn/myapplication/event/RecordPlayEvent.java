package library.photosynthesis.cn.myapplication.event;

/**
 * Created by siqiangli on 2017/5/10.
 */

public class RecordPlayEvent {
    public static final int PAUSE = 1;
    public static final int PLAY = 2;
    public static final int STOP = 3;
    private int msgType;
    private int curPosTime;
    private int totalTime;

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public int getCurPosTime() {
        return curPosTime;
    }

    public void setCurPosTime(int curPosTime) {
        this.curPosTime = curPosTime;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

}
