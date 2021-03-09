package cz.zemankrystof.izastavkanox;

import android.app.DownloadManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.izastavkanox.R;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import cz.zemankrystof.izastavkanox.model.flix.Flix;
import cz.zemankrystof.izastavkanox.model.flix.FlixDeparture;
import cz.zemankrystof.izastavkanox.model.regiojet.RJConnectionStation;
import cz.zemankrystof.izastavkanox.model.regiojet.RJStation;
import cz.zemankrystof.izastavkanox.remote.ApiUtils;
import cz.zemankrystof.izastavkanox.remote.BannerService;
import cz.zemankrystof.izastavkanox.remote.FlixService;
import cz.zemankrystof.izastavkanox.remote.RJService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MainActivity extends AppCompatActivity {

    ConstraintLayout background_theme;

    private FlixService flixService;
    private RJService rjService;
    private BannerService bannerService;

    public static boolean RJ_READY = false;
    public static boolean FLIX_READY = false;

    // Stops autorefresh handler
    Handler loadStopsHandler;
    Runnable loadStopsRunnable;

    // Banner handler
    Handler loadBannerHandler;
    Runnable loadBannerRunnable;

    //Device policies are essential :)
    private DevicePolicyManager mDpm;
    private ImageView bannerIV;

    private static List<Object> depList = new ArrayList<>();

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e("CfgChanged", "RIGHT NOW");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e("onSaveInstanceS", "Called?");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_main);
        Log.e("OnCreate", "Activity Recreated!!");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeViews();

        if(savedInstanceState == null){
            Log.d("SavedInstance", "Null");

            //TODO uncomment
            ComponentName deviceAdmin = new ComponentName(this, AdminReceiver.class);
            mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            //mDpm.setKeyguardDisabled(deviceAdmin, true);
            if (!mDpm.isAdminActive(deviceAdmin)) {
                Toast.makeText(this, "No Admin rights!!!", Toast.LENGTH_SHORT).show();
            }

            if (mDpm.isDeviceOwnerApp(getPackageName())) {
//                mDpm.setLockTaskPackages(deviceAdmin, new String[]{getPackageName()});
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mDpm.setKeyguardDisabled(deviceAdmin, true);
                }
            } else {
                Toast.makeText(this, "Admin privileges not received!!!", Toast.LENGTH_SHORT).show();
            }

            rjService = ApiUtils.getRJService();
            flixService = ApiUtils.getFlixService();
            bannerService = ApiUtils.getBannerService();
//            startLockTask();

            background_theme.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    Rect r = new Rect();
                    background_theme.getWindowVisibleDisplayFrame(r);
                    int screenHeight = background_theme.getRootView().getHeight();

                    // r.bottom is the position above soft keypad or device button.
                    // if keypad is shown, the r.bottom is smaller than that before.
                    int keypadHeight = screenHeight - r.bottom;

                    //Log.d("Keyboard", "keypadHeight = " + keypadHeight);

                    Window window = getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    //TODO uncomment
//                    window.setStatusBarColor(getResources().getColor(Color.TRANSPARENT));

                    if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                        // keyboard is opened
                        View decorView = getWindow().getDecorView();
                        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                    else {
                        // keyboard is closed
                        View decorView = getWindow().getDecorView();
                        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                }
            });
        }

    }

    public void initializeViews() {
        bannerIV = this.findViewById(R.id.bannerIV);
        initializeBottomBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(loadStopsHandler != null && loadStopsRunnable != null) {
            loadStopsHandler.removeCallbacks(loadStopsRunnable);
            loadStopsRunnable = null;
        }
        if(loadBannerHandler != null && loadBannerRunnable != null){
            loadBannerHandler.removeCallbacks(loadBannerRunnable);
            loadBannerRunnable = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainOnResume", "Called");
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        rjService = ApiUtils.getRJService();
        flixService = ApiUtils.getFlixService();
        bannerService = ApiUtils.getBannerService();
        startLoadStopsRefreshHandler();
        startLoadBannerHandler();
    }


    public void initializeBottomBar(){
        background_theme = (ConstraintLayout) findViewById(R.id.background_theme);
    }


    public void loadRJStops(){
        rjService.getDepartures("cs").enqueue(new Callback<List<RJStation>>() {
            @Override
            public void onResponse(Call<List<RJStation>> call, Response<List<RJStation>> response) {
                if(response.isSuccessful()) {
                    Log.d("Station size", "" + response.body().size());
                    List<RJStation> rjStations = filterRJDepartures(response.body());
                    if (rjStations.size() > 0) {
                        depList.addAll(rjStations);
                    }
                }
                RJ_READY = true;
                refreshStopDisplay();
            }

            @Override
            public void onFailure(Call<List<RJStation>> call, Throwable t) {
                Log.d("Error", "E: " + t.getMessage() + " " + t.getCause());
                RJ_READY = true;
                refreshStopDisplay();
            }
        });
    }

    public void loadFlixStops(){
        flixService.getDepartures("8d28c50a4cdd97f66f25d8868fe2167c", "cs").enqueue(new Callback<Flix>() {
            @Override
            public void onResponse(Call<Flix> call, Response<Flix> response) {
                if(response.body() != null) {
                    if(response.body().getTimetable().getFlixDepartures() != null) {
                        Log.d("Flix", "" + response.body().getTimetable().getFlixDepartures());
                        List<FlixDeparture> deps = filterFlixDepartures(response.body().getTimetable().getFlixDepartures());
                        deps = filterCancelledFlix(deps);
                        depList.addAll(deps);
                    }
                    FLIX_READY = true;
                    refreshStopDisplay();
                }
            }

            @Override
            public void onFailure(Call<Flix> call, Throwable t) {
                Log.d("Error", "E: " + t.getMessage() + " " + t.getCause());
                FLIX_READY = true;
                refreshStopDisplay();
            }
        });
    }

    public void refreshStopDisplay(){
        if(FLIX_READY && RJ_READY) {
            Log.d("OMG", "DepList: " + depList.size());
            if (depList.size() < 10) {
                int size = depList.size();
                if (depList.size() == 0){
                    size = 1;
                }
                for (int i = size; i < 10; i++) {
                    TableLayout jzdrady = findViewById(R.id.linesTable);
                    TableRow row = (TableRow) jzdrady.getChildAt(i);
                    ((ImageView) row.getChildAt(0)).setVisibility(View.INVISIBLE);
                    ((TextView) row.getChildAt(1)).setText("");
                    ((TextView) ((LinearLayout) row.getChildAt(2)).getChildAt(0)).setText("");
                    ((TextView) ((LinearLayout) row.getChildAt(2)).getChildAt(1)).setText("");
                    ((TextView) ((LinearLayout) row.getChildAt(3)).getChildAt(0)).setText("");
                    ((TextView) ((LinearLayout) row.getChildAt(3)).getChildAt(1)).setText("");
                    ((TextView) row.getChildAt(4)).setText("");
                }
            }
            if (depList.size() > 0) {
                Collections.sort(depList, new Comparator<Object>() {
                    @Override
                    public int compare(Object o, Object t1) {
                        Long do1 = null;
                        Long do2 = null;
                        Log.d("DATA", "" + o.getClass().getName() + " " + t1.getClass().getName());
                        if (o instanceof RJStation) {
                            for (RJConnectionStation station : ((RJStation) o).getConnectionStations()) {
                                if (station.getStationId() == 10204002) {
                                    if (station.getDeparture() != null) {
                                        do1 = getRJDate(station.getDeparture());
                                        break;
                                    }
                                }
                            }
                        } else if (o instanceof FlixDeparture) {
                            do1 = getFlixDate(((FlixDeparture) o).getDatetime().getTimestamp().toString());
                        }
                        if (t1 instanceof RJStation) {
                            for (RJConnectionStation station : ((RJStation) t1).getConnectionStations()) {
                                if (station.getStationId() == 10204002) {
                                    if (station.getDeparture() != null) {
                                        do2 = getRJDate(station.getDeparture());
                                        break;
                                    }
                                }
                            }
                        } else if (t1 instanceof FlixDeparture) {
                            do2 = getFlixDate(((FlixDeparture) t1).getDatetime().getTimestamp().toString());
                        }
                        Log.d("Comp", " Comparing: " + do1 + " and : " + do2);
//                        if (do2) {
//                        }
                        return Long.valueOf(do1).compareTo(Long.valueOf(do2));
//                       return do1.compareTo(do2);
                    }
                });

                for (Object des : depList) {
                    String time = "";
                    String depPlace = "";
                    if (des instanceof RJStation) {
                        for (RJConnectionStation station : ((RJStation) des).getConnectionStations()) {
                            if (station.getStationId() == 10204002) {
                                time = getRJTime(station.getDeparture());
                                depPlace = getRJDir(((RJStation) des).getLabel());
                                Log.d("REVISITED", " time: " + time + " dir: " + depPlace);
                                break;
                            }
                        }
//                       time = getRJTime(((RJStation) des).getConnectionStations().get(0).getDeparture());
//                       depPlace = getRJDir(((RJStation) des).getLabel());
                    } else if (des instanceof FlixDeparture) {
                        time = getFlixTime(((FlixDeparture) des).getDatetime().getTimestamp().toString());
                        depPlace = getFlixDir(((FlixDeparture) des).getDirection());
                    }
                    Log.d("SORTED", "Time: " + time + " Dep: " + depPlace);
                }
                Log.d("LIST", "SIZE : " + depList.size());


                int i = 0;
                while ((i < 9) && (i < depList.size())) {
                    Object it = depList.get(i);
                    String time = "";
                    String direction = "";
                    String carrier = "";
                    String depPlatform = "";
                    String number = "";
                    String accDir = "";

                    TableLayout jzdrady = findViewById(R.id.linesTable);
                    TableRow row = (TableRow) jzdrady.getChildAt(i + 1);


                    int color = 0;
                    int picColor = 0;
                    if (it instanceof RJStation) {
                        for (RJConnectionStation connectionStation : ((RJStation) it).getConnectionStations()) {
                            if (connectionStation.getStationId() == 10204002) {
                                picColor = R.drawable.ic_piktogram_rj;
                                color = getResources().getColor(R.color.gold);
                                time = getRJTime(connectionStation.getDeparture());
                                direction = getRJDir(((RJStation) it).getLabel());
                                number = ((RJStation) it).getNumber();
                                carrier = "RegioJet";
                                Log.d("DEP", " Platf: " + depPlatform);
                                depPlatform = connectionStation.getPlatform();
                                break;
                            }
                        }
                    } else if (it instanceof FlixDeparture) {
                        picColor = R.drawable.ic_piktogram_flixbus;
                        color = getResources().getColor(R.color.lime);
                        time = getFlixTime(((FlixDeparture) it).getDatetime().getTimestamp().toString());
                        direction = getFlixDir(((FlixDeparture) it).getDirection());
                        accDir = getFlixAccDir(((FlixDeparture) it).getDirection());
                        number = ((FlixDeparture) it).getLineCode();
                        carrier = "FlixBus";
                    }


                    Log.d("TAG", "ROW " + i);
                    ((ImageView) row.getChildAt(0)).setVisibility(View.VISIBLE);
                    ((ImageView) row.getChildAt(0)).setImageResource(picColor);
                    ((TextView) row.getChildAt(1)).setText(time);
                    ((TextView) ((LinearLayout) row.getChildAt(2)).getChildAt(0)).setText(direction);
                    ((TextView) ((LinearLayout) row.getChildAt(2)).getChildAt(1)).setText(accDir);
                    ((TextView) ((LinearLayout) row.getChildAt(3)).getChildAt(0)).setText(number);
                    ((TextView) ((LinearLayout) row.getChildAt(3)).getChildAt(1)).setText(carrier);
                    ((TextView) ((LinearLayout) row.getChildAt(3)).getChildAt(1)).setTextColor(color);
                    ((TextView) row.getChildAt(4)).setText(depPlatform);
                    i++;
                }
            }
        }
    }

    public Long getFlixDate(String date){
        Long d = Long.parseLong(date)*1000;
        return d;
    }

    public List<FlixDeparture> filterCancelledFlix(List<FlixDeparture> depList){
        for (Iterator<FlixDeparture> it = depList.iterator(); it.hasNext(); ) {
            FlixDeparture flixDeparture = it.next();
            if (flixDeparture.getIsCancelled()) {
                it.remove();
            }
        }
        return depList;
    }

    public String getFlixTime(String date){
        Log.e("LIA", "tim " + date);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Long dt = Long.parseLong(date)*1000;
        Log.e("LIAa", "tim " + dt);
        Date d = new Date(dt);
        Log.e("LIAaa", "tim " + d.getTime());
        String f = sdf.format(d);
        Log.e("LIAaaa", "tim " + f);
        return f;

    }

    public Long getRJDate(String date){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssZZZZZ");
        Date dat = null;
        try {
            dat = df.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Log.d("RJDATE", " " + dat);
        Long rjdat = dat.getTime();
        return rjdat;
    }

    public String getRJTime(String date){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssZZZZZ");
        Date d = null;
        try {
            d = df.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        DateFormat datik = new SimpleDateFormat("HH:mm");
        String f = datik.format(d);
        return f;
    }

    public String getRJDir(String dir){
        String var = dir.split(" ")[4];
        return var;
    }

    public String getFlixDir(String dir){
        String var = dir.split("[, ]")[0];
        return var;
    }

    public String getFlixAccDir(String dir){
        Log.d("SPLIT", " " + dir);
        String splitted [] = dir.split(",", 2);
        Log.d("SPLIT", " " + splitted.length);
        if (splitted.length > 1){
            return  splitted[1].replace(",", "");
        }else{
            splitted = dir.split(" ", 2);
            if (splitted.length > 1){
                return  splitted[1].replace("-", "");
            }
            return "";
        }
    }

    public List<FlixDeparture> filterFlixDepartures(List<FlixDeparture> departures){
        Date now = Calendar.getInstance().getTime();
        List<FlixDeparture> filtered = new ArrayList<>();
        for (FlixDeparture dep:departures){
            Date depDate = new Date(dep.getDatetime().getTimestamp());
            //Log.d("Curr", "Time: " + now.getTime() + " DepTime: " + depDate.getTime());
            long flixTim = depDate.getTime()*1000;
            if ((flixTim > now.getTime()) && (flixTim < (now.getTime() + 43200000))){
                //Log.d("FILTER", "Adding filtered dep: " + depDate.getTime());
                filtered.add(dep);
            }
        }
        return filtered;
    }

    public List<RJStation> filterRJDepartures(List<RJStation> rjstations){
        List<RJStation> listik = new ArrayList<>();
        for(RJStation stat: rjstations){
            String splitted[] = stat.getLabel().split(" ");
            List<String> split_list = Arrays.asList(splitted);
            String directionTo = split_list.get(split_list.indexOf("→") + 1);
            String directionFrom = split_list.get(split_list.indexOf("→") - 1);
            Log.d("Direction: ", directionTo);
            Date now = Calendar.getInstance().getTime();
            if (!directionTo.toLowerCase().contains("brno")){
                Log.d("Direction", "Adding to list " + directionTo);
                if((directionFrom.equalsIgnoreCase("brno")) && stat.getConnectionStations().get(0).getStationId() != 10204002) {

                }else{
                    for(RJConnectionStation rjConnectionStation: stat.getConnectionStations()){
                        if (rjConnectionStation.getStationId() == 10204002){
                            Log.d("TIIIME", "Dep: " + getRJDate(rjConnectionStation.getDeparture()) + " Now: " + now.getTime());
                            long rjTim = getRJDate(rjConnectionStation.getDeparture());
                            if ((rjTim > now.getTime()) && (rjTim < (now.getTime() + 43200000))){

                                listik.add(stat);
                            }
                        }
                    }

                }
            }
        }

        return listik;
    }
    public void startLoadStopsRefreshHandler(){
        if(loadStopsHandler == null || loadStopsRunnable == null) {
            loadStopsHandler = new Handler();
            loadStopsRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d("MainActivity", "Refreshing stop departures!");
                    depList.clear();
                    RJ_READY = false;
                    FLIX_READY = false;
                    loadRJStops();
                    loadFlixStops();
                    loadStopsHandler.postDelayed(loadStopsRunnable, 30000);
                }
            };
        }
        loadStopsHandler.post(loadStopsRunnable);
    }

    public void startLoadBannerHandler(){
        if(loadBannerHandler == null || loadBannerRunnable == null) {
            loadBannerHandler = new Handler();
            loadBannerRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d("MainActivity", "Starting the Banner handler");
//                    DownloadBannerList();
                    loadBanner();
                    loadBannerHandler.postDelayed(loadBannerRunnable, 57600000);
                }
            };
        }
        loadBannerHandler.post(loadBannerRunnable);
    }

    public void loadBanner(){
//        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//        Uri uri = Uri.parse("https://www.dpmb.cz/img/srv/an-benesova-reklama.png");
            new DownloadBannerAsyncTask().execute();
//        DownloadManager.Request request = new DownloadManager.Request(uri);
//        request.setTitle("Banner");
//        request.setDescription("Downloading.");
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
//        request.setVisibleInDownloadsUi(false);
//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "banner");
//        downloadManager.enqueue(request);
//        if (bannerURLs.size() > 0) {
//            Picasso.get().load("http://10.0.230.1/images/" + bannerURLs.get(bannerNum)).into(bannerIV);
//        Picasso.with(this).load("https://www.dpmb.cz/img/srv/an-benesova-reklama.png").memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE).into(bannerIV);
//        Picasso.with(this).load("https://www.dpmb.cz/img/srv/an-benesova-reklama.png").into(bannerIV);
//        Glide.with(this).load("https://www.dpmb.cz/img/srv/an-benesova-reklama.png").into(bannerIV);
        //            if(bannerNum < bannerURLs.size()){
//                bannerNum = 0;
//            }else{
//                bannerNum += 1;
//            }
//        }
    }

    private class DownloadBannerAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            DownloadBanner();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Glide.with(getApplicationContext()).load(getExternalFilesDir(DIRECTORY_DOWNLOADS).getAbsolutePath() + "/../" + "/banner/banner.png").into(bannerIV);
        }
    }

    public void DownloadBanner(){

        try {
            URL u = new URL("https://www.dpmb.cz/img/srv/an-benesova-reklama.png");
            InputStream is = u.openStream();

            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[1024];
            int length;

            String path = getExternalFilesDir(DIRECTORY_DOWNLOADS).getAbsolutePath();
            File file = new File( path + "/../" + "/banner");
            if(!file.exists()){
                file.mkdirs();
                Log.d("FILE", "CREATED " + file.getAbsolutePath());
            }
            File banner = new File(file.getAbsolutePath() + "/banner.png");
            if (banner.exists() != true){
                banner.createNewFile();
            }else {
                banner.delete();
                banner.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(banner);
            while ((length = dis.read(buffer))>0) {
                fos.write(buffer, 0, length);
            }

        } catch (MalformedURLException mue) {
            Log.e("SYNC getUpdate", "malformed url error", mue);
        } catch (IOException ioe) {
            Log.e("SYNC getUpdate", "io error " + ioe.getMessage());
        } catch (SecurityException se) {
            Log.e("SYNC getUpdate", "security error", se);
        }
    }

}