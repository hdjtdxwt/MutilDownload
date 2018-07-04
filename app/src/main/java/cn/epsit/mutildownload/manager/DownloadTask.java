package cn.epsit.mutildownload.manager;

/**
 * Created by Administrator on 2018/7/5.
 */

public class DownloadTask implements Runnable, ProgressListener  {
    //更新任务进度消息
    private static final int UPDATE_PROGRESS_ID = 0x100;
    //下载网络服务
    private APIService.DownloadApiService downloadApiService;
    //上传网络服务
    private APIService.UploadApiService uploadApiService;
    //下载任务状态
    private STATE state;
    //下载实体类，使用object基类，方便统一获取
    private Object downloadObject;
    //网络服务请求参数列表
    private List<RequestParameter> parameterList;
    //网络下载请求对象
    private Call<File> downloadCall;
    //网络上传请求对象
    private Call<BaseEntity> uploadCall;
    //下载保存文件对象
    private File downloadFile = null;
    //下载任务进度监听器
    private OnProgressListener onProgressListener;
    private DownloadTask mySelf;
    //是否是下载，区分当前任务是下载还是上传
    private boolean isDownload;

    @Override
    public void run() {
        start();
    }

    @Override
    public void onProgress(long addedBytes, long contentLenght, boolean done) {
        sendUpdateProgressMessage(addedBytes, contentLenght, false);
    }

    public enum STATE {
        IDLE,
        PENDING,
        LOADING,
        FAILED,
        FINISHED,
        UNKNOWN,
    }

    private void sendUpdateProgressMessage(long addedBytes, long contentLenght, boolean done) {
        Message message = handler.obtainMessage(UPDATE_PROGRESS_ID);
        message.obj = done;
        message.arg1 = (int) addedBytes;
        message.arg2 = (int) contentLenght;
        handler.sendMessage(message);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_PROGRESS_ID) {
                if (onProgressListener != null) {
                    onProgressListener.onProgress(msg.arg1, msg.arg2, (Boolean) msg.obj);
                }
            }
        }
    };

    public DownloadTask(final Object object, List<RequestParameter> list, final boolean download) {
        downloadObject = object;
        parameterList = list;
        isDownload = download;
        if (isDownload) {
            downloadApiService = HttpApiService.getDownloadApiService(this);
        } else {
            uploadApiService = HttpApiService.getUploadApiService(this);
        }
        state = STATE.IDLE;
        mySelf = this;
    }

    public void start() {
        if (state == STATE.LOADING) {
            return;
        }
        state = STATE.LOADING;
        if (isDownload) {
            download();
        } else {
            upload();
        }
    }

    private void download() {
        if (parameterList != null && parameterList.size() > 1 && downloadApiService != null) {
            //change state pending or idle to loading, notify ui to update.
            sendUpdateProgressMessage(0, 0, false);
            String downloadFilename = parameterList.get(0).getValue();
            String saveFilename = parameterList.get(1).getValue();
            downloadCall = downloadApiService.httpDownloadFile(downloadFilename, saveFilename);
            downloadCall.enqueue(new Callback<File>() {
                @Override
                public void onResponse(Call<File> call, Response<File> response) {
                    Log.i(response.toString());
                    if (response.code() == 200) {
                        mySelf.downloadFile = response.body();
                        if (mySelf.downloadFile != null && !mySelf.downloadFile.getPath().endsWith(".tmp")) {
                            sendUpdateProgressMessage(100, 100, true);
                            mySelf.state = STATE.FINISHED;
                            TaskDispatcher.getInstance().finished(mySelf);
                        } else {
                            mySelf.state = STATE.FAILED;
                            sendUpdateProgressMessage(0, 0, false);
                        }
                    }
                }

                @Override
                public void onFailure(Call<File> call, Throwable t) {
                    mySelf.state = STATE.FAILED;
                    sendUpdateProgressMessage(0, 0, false);
                }
            });
        }
    }

    private void upload() {
        if (parameterList != null && parameterList.size() > 1 && uploadApiService != null) {
            File file = new File(parameterList.get(0).getValue());
            String uid = parameterList.get(1).getValue();
            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
            uploadCall = uploadApiService.upLoad(uid, body); // "34"
            uploadCall.enqueue(new Callback<BaseEntity>() {
                @Override
                public void onResponse(Call<BaseEntity> call, Response<BaseEntity> response) {
                    Log.i(response.body().toString());
                    if (response.code() == 200 && response.body().status.equals("2000")) {
                        sendUpdateProgressMessage(100, 100, true);
                        mySelf.state = STATE.FINISHED;
                        TaskDispatcher.getInstance().finished(mySelf);
                    } else {
                        mySelf.state = STATE.FAILED;
                        sendUpdateProgressMessage(0, 0, false);
                    }
                }

                @Override
                public void onFailure(Call<BaseEntity> call, Throwable t) {
                    Log.e(t.getMessage());
                }
            });
        }
    }

    public void cancel() {
        if (downloadCall != null) {
            downloadCall.cancel();
        }

        handler.removeMessages(UPDATE_PROGRESS_ID);
    }

    public void setState(final STATE state) {
        this.state = state;
    }

    public STATE getState() {
        return state;
    }

    public Object getDownloadObject() {
        return downloadObject;
    }

    public void setDownloadObject(Object downloadObject) {
        this.downloadObject = downloadObject;
    }

    public File getDownloadFile() {
        return downloadFile;
    }

    public boolean isDownload() {
        return isDownload;
    }

    public void setOnProgressListener(final OnProgressListener listener) {
        onProgressListener = listener;
    }

    public interface OnProgressListener {
        void onProgress(long addedBytes, long contentLenght, boolean done);
    }

}
