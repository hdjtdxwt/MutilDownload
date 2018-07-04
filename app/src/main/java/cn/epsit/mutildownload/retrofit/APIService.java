package cn.epsit.mutildownload.retrofit;

import java.io.File;

import cn.epsit.mutildownload.model.BaseEntity;
import retrofit2.Call;

/**
 * Created by Administrator on 2018/7/5.
 */

public interface APIService {
    interface DownloadApiService{
        Call<File> httpDownloadFile(String downloadFilename , String saveFilename );
    }
    interface UploadApiService{
        Call<BaseEntity>  upLoad(String uid, okhttp3.MultipartBody.Part part);
    }
}
