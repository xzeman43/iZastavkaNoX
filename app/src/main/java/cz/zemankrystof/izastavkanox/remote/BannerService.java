package cz.zemankrystof.izastavkanox.remote;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface BannerService {

    @GET("an-benesova-reklama.png")
    Call<ResponseBody> getBannerList();
}
