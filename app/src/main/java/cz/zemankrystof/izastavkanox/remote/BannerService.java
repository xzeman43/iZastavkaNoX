package cz.zemankrystof.izastavkanox.remote;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

public interface BannerService {

    @Streaming
    @GET("an-benesova-reklama.png")
    Call<ResponseBody> getBanner();
}
