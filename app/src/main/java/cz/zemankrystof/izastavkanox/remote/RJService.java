package cz.zemankrystof.izastavkanox.remote;

import java.util.List;

import cz.zemankrystof.izastavkanox.model.regiojet.RJStation;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface RJService {

    @GET("departures")
    Call<List<RJStation>> getDepartures(@Header("X-Lang") String language);
}
