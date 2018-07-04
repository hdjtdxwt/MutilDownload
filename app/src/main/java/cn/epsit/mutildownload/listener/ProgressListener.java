package cn.epsit.mutildownload.listener;

/**
 * Created by Administrator on 2018/7/5.
 */

public interface ProgressListener {
    public void onProgress(long addedBytes, long contentLenght, boolean done);
}
