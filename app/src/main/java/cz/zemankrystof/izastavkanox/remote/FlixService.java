package cz.zemankrystof.izastavkanox.remote;

import cz.zemankrystof.izastavkanox.model.flix.Flix;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface FlixService {
    @GET("timetable.json")
    Call<Flix> getDepartures(@Header("X-API-Authentication") String apiKey, @Header("Accept-Language") String lang);
}
