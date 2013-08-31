package com.anthonykeane.speedzone;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import static java.util.UUID.randomUUID;


public class MainActivity extends Activity implements LocationListener {

    //Is different in MainActivity
    private static final boolean bThisIsMainActivity = true;
    private static final String TAG = "ChatHead::Activity";
    private static final String TAGd = "ChatHead::Activity_focus";

    private static final int intentSettings = 1;
    public static final int itextViewGPSlost = R.id.textViewGPSlost;

//    < click here
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
// code below this line is same in MainActivity and Service

    //private static final int intentTTS = 3;
    //private String ttsSalute;

    SharedPreferences appSharedPrefs;

    //GPS delay stuff

    private Location locCurrent = new Location("");
    private Location locLast = new Location("");

    private TextToSpeech mTts;

    public static final int delayBetweenGPS_Records = 10000;    //every 500mS log Geo date in Queue.
    public static final long minTime = 1000;                   // don't update GPS if time < mS
    public static final float minDistanceGPS = 0;              // don't update GPS if distance < Meters

    private final Handler handler = new Handler();                // used for timers

    // public final LocListener gpsListener = new LocListener();    // used by GPS

    public int iNeedToResetDisplay = 0;

    public final AsyncHttpClient client = new AsyncHttpClient();
    public final RequestParams HTTPrp = new RequestParams();
    public final RequestParams HTTPrp2 = new RequestParams();

    public JSONObject jHereResult = new JSONObject();
    public JSONObject jThereResult = new JSONObject();

    public float DistanceToNextSpeedChange = 0;            //any BIG number or zero
    public int iSecondsToSpeedChange = 0;
    private static String sUUID = "";

    private static LocationManager locManager;
    private View vImageButton;
    private View vErrorButton;
    private View vImageBtnSmall;
    private View vImageViewDebug;
    private View vImageViewTimeout;

    public static final int itextView = R.id.textView;
    public static final int itextView2 = R.id.textView2;


    public int iSpeed = 50;
    public int fFiveValAvgSpeed=60;

    private Location me = new Location("");
    private Location dest = new Location("");
    //private static Context context;

    //Flags
    public boolean bZoneError = false;
    public boolean bDebug = false;
    public int iNotCommsLockedOut = 0;                   //Lock out comms until last request is serviced
    public boolean bCommsTimedOut = false;
    private boolean bMute = false;
    private int iDistanceOffset = 50;


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy  5");
        handler.removeCallbacks(timedGPSqueue);

        try { // Turn Off the GPS
            locManager.removeUpdates(this); // Turn Off the GPS
        } catch (Exception e) {Log.i(TAG, "onDestroy - GPS is already null"); }
        if (locManager!=null){locManager = null;}

        removeChatHeads();



        //When you are done using TTS, be a good citizen and tell it "you won't be needing its services anymore"
        try {
            mTts.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {


        locLast = locCurrent;
        locCurrent = location;
        Log.i("GPS", "onLocationChanged  ");
        if (iNotCommsLockedOut ==0){

            // if params of locaton unchanged skip

            if ((int) locLast.getSpeed() != (int) locCurrent.getSpeed()
                    && (int) (locLast.getBearing() / 6) != (int) (locCurrent.getBearing() / 6)) {
                callWebService();
            }
            doStuff();
        }



    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    private void setDisplay(int tmp) {
        setGraphicBtnV(vImageButton, tmp,false);
        if(bThisIsMainActivity) setGraphicBtnV(vImageBtnSmall, tmp, true);
        DistanceToNextSpeedChange = tmp;
        iSpeed = tmp;
    }

    public void setGraphicBtnV(View x, int iSpeed, boolean bSmall) {


        ImageButton img = (ImageButton) x;


        if (!bSmall && bThisIsMainActivity){
            switch (iSpeed){
                case 40:
                    img.setImageResource(R.drawable.b40);
                    break;
                case 50:
                    img.setImageResource(R.drawable.b50);
                    break;
                case 60:
                    img.setImageResource(R.drawable.b60);
                    break;
                case 70:
                    img.setImageResource(R.drawable.b70);
                    break;
                case 80:
                    img.setImageResource(R.drawable.b80);
                    break;
                case 90:
                    img.setImageResource(R.drawable.b90);
                    break;
                case 100:
                    img.setImageResource(R.drawable.b100);
                    break;
                case 110:
                    img.setImageResource(R.drawable.b110);
                    break;
                default:
                    img.setImageResource(R.drawable.b50);
                    break;

            }
        }

        if ((bSmall && bThisIsMainActivity)
                || (!bThisIsMainActivity && (locCurrent.getAccuracy()>15) && (locCurrent.getAccuracy()!=0.0)) ) {
            switch (iSpeed){
                case 40:
                    img.setImageResource(R.drawable.g40);
                    break;
                case 50:
                    img.setImageResource(R.drawable.g50);
                    break;
                case 60:
                    img.setImageResource(R.drawable.g60);
                    break;
                case 70:
                    img.setImageResource(R.drawable.g70);
                    break;
                case 80:
                    img.setImageResource(R.drawable.g80);
                    break;
                case 90:
                    img.setImageResource(R.drawable.g90);
                    break;
                case 100:
                    img.setImageResource(R.drawable.g100);
                    break;
                case 110:
                    img.setImageResource(R.drawable.g110);
                    break;
                default:
                    img.setImageResource(R.drawable.g50);
                    break;

            }
        }

        if (!bThisIsMainActivity && (locCurrent.getAccuracy()<=15)  && (locCurrent.getAccuracy()!=0.0)) {
            switch (iSpeed){
                case 40:
                    img.setImageResource(R.drawable.s40);
                    break;
                case 50:
                    img.setImageResource(R.drawable.s50);
                    break;
                case 60:
                    img.setImageResource(R.drawable.s60);
                    break;
                case 70:
                    img.setImageResource(R.drawable.s70);
                    break;
                case 80:
                    img.setImageResource(R.drawable.s80);
                    break;
                case 90:
                    img.setImageResource(R.drawable.s90);
                    break;
                case 100:
                    img.setImageResource(R.drawable.s100);
                    break;
                case 110:
                    img.setImageResource(R.drawable.s110);
                    break;
                default:
                    img.setImageResource(R.drawable.s50);
                    break;

            }

        }
    }


    private void callWebService() {

        Time now = new Time();
        now.setToNow();
        String xxx = now.format("%Y-%m-%d %H:%M:%S");
        Log.i(TAG, "callWebService  " + xxx + "                     *");

        HTTPrp.put("When", xxx);
        HTTPrp.put("UUID", sUUID);
        HTTPrp.put("lat", String.valueOf(locCurrent.getLatitude()));
        HTTPrp.put("lon", String.valueOf(locCurrent.getLongitude()));
        HTTPrp.put("ber", String.valueOf(locCurrent.getBearing()));
        HTTPrp.put("speed", String.valueOf(locCurrent.getSpeed()));

        if (bZoneError) {
            HTTPrp.put("bZoneError", "1");
        } else {
            HTTPrp.put("bZoneError", "0");
        }


        if (   ((locCurrent.getAccuracy()>=15) || (locCurrent.getAccuracy()==0.0))  && bDebug) {
            HTTPrp.put("lat", "-33.71013");
            HTTPrp.put("lon", "150.94951");
            HTTPrp.put("ber", "100");
            HTTPrp.put("speed", "99");
            HTTPrp.put("UUID", "test-" + sUUID);
            HTTPrp.put("When", xxx);
        }
//     -34.069 151.0136
        //HTTPrp.put("ber", "133");

        //Toast.makeText(this, String.valueOf(LocListener.getLat()), Toast.LENGTH_SHORT).show();
        if ((locCurrent.getAccuracy()<15) && (locCurrent.getAccuracy()!=0.0)  || bDebug)
        {

            if(iNotCommsLockedOut == 0)
            {
                client.post(getString(R.string.MyDbWeb), HTTPrp, new JsonHttpResponseHandler() {

                    @Override
                    public void onFailure(Throwable e, JSONArray errorResponse) {
                        System.out.println(e);
                        Log.i(TAGd, "onFailure  ");
                        //Clear the display if we don't know the value
                        // Skip is too slow to matter
                        //if (locCurrent.getSpeed() >= 40)
                        {
                            NeedToResetDisplay();
                        }
                        bCommsTimedOut = false;
                    }

                    @Override
                    public void onFailure(Throwable e, JSONObject errorResponse) {
                        System.out.println(e);
                        Log.i(TAGd, "onFailure  ");
                        bCommsTimedOut = false;
                        //Clear the display if we don't know the value
                        // Skip is too slow to matter
                        if (locCurrent.getSpeed() >= 20) {
                            NeedToResetDisplay();
                        }
                    }

                    @Override
                    public void onSuccess(JSONObject response) {
                        bCommsTimedOut = false;
                        Log.i(TAGd, "           onSuccess  ");
                        jHereResult = response;
                        try {
                            //Calculate Distance
                            me = new Location("");
                            dest = new Location("");
                            me.setLatitude(locCurrent.getLatitude());
                            me.setLongitude(locCurrent.getLongitude());
                            try {
                                dest.setLatitude(jThereResult.getDouble("reLat"));
                                dest.setLongitude(jThereResult.getDouble("reLon"));
                                DistanceToNextSpeedChange = me.distanceTo(dest);
                            } catch (JSONException e) {
                                Log.i(TAG, "onSuccess - No value for reLat");
                            }


                            if (!bMute && (iSpeed != jHereResult.getInt("reSpeedLimit"))) {
                                try {
                                    mTts.speak("the Speed is now " + String.valueOf(jHereResult.getInt("reSpeedLimit")), TextToSpeech.QUEUE_FLUSH, null);
                                } catch (Exception e) {
                                    Log.i(TAG, "onSuccess - No value for reSpeedLimit");
                                }
                            }


                            iSpeed = jHereResult.getInt("reSpeedLimit");
                            setGraphicBtnV(vImageButton, iSpeed, false);

                            HTTPrp2.put("reMainRoad", oneTo1(String.valueOf(jHereResult.getString("reMainRoad"))));
                            HTTPrp2.put("rePrescribed", oneTo1(String.valueOf(jHereResult.getString("rePrescribed"))));


                            HTTPrp2.put("RE", String.valueOf(jHereResult.getString("RE")));
                            HTTPrp2.put("reSpeedLimit", String.valueOf(jHereResult.getString("reSpeedLimit")));
                            HTTPrp2.put("RdNo", String.valueOf(jHereResult.getString("RdNo")));
                            fFiveValAvgSpeed = (int) (((fFiveValAvgSpeed * 4) + locCurrent.getSpeed()) / 5);
                            iSecondsToSpeedChange = (int) ((DistanceToNextSpeedChange / fFiveValAvgSpeed));


                            //Toast.makeText(getApplicationContext(), iSecondsToSpeedChange, Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "onSuccess  " + iSecondsToSpeedChange + " iSecondsToSpeedChange ");


                            if (bThisIsMainActivity) {
                                updateDebugText();
                            }


                            if ((iSecondsToSpeedChange < 60) || (DistanceToNextSpeedChange < 1000) || (DistanceToNextSpeedChange == 0))                         //refresh when close only
                            {
                                Log.i(TAG, "onSuccess  Getting Speec change");
                                client.post(getString(R.string.MyNextWeb), HTTPrp2, new JsonHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        Log.i(TAG, "onSuccess MyNextWeb  ");
                                        bCommsTimedOut = false;
                                        jThereResult = response;

                                        doStuff();

                                    }

                                    @Override
                                    public void onFinish() {
                                        // Completed the request (either success or failure)

                                        updateTimeoutIcon();
                                        Log.i(TAGd, "onFinish  ");
                                    }
                                });

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onStart() {
                        // Completed the request (either success or failure)
                        //toggleRadioButton();
                        Log.i(TAGd, "onStart  ");
                        bCommsTimedOut = true;
                        iNotCommsLockedOut++;
                    }

                    @Override
                    public void onFinish() {
                        // Completed the request (either success or failure)
                        toggleRadioButton();
                        iNotCommsLockedOut--;
                        if (iNotCommsLockedOut <= 0) iNotCommsLockedOut = 0;
                        updateTimeoutIcon();
                        if (bCommsTimedOut) {
                            setDisplay(0);
                        }
                        Log.i(TAGd, "                       onFinish  ");
                    }
                });
            }

        }
    }

    private void doStuff() {
        try {
            dest.setLatitude(jThereResult.getDouble("reLat"));
            dest.setLongitude(jThereResult.getDouble("reLon"));
            DistanceToNextSpeedChange = me.distanceTo(dest) - iDistanceOffset;
            if (bThisIsMainActivity) {

                setGraphicBtnV(vImageBtnSmall, jThereResult.getInt("reSpeedLimit"), true);
                //Resize the image based on distance to.
//                float anmi = 0;
//                if (DistanceToNextSpeedChange != 0) {
//                    anmi = 1 / ((DistanceToNextSpeedChange + 1) / 1000);
//
//                }
//                final float v = (anmi > 1) ? 1 : anmi;
//                setDisplayScale((v<0.3)? (float) 0.3 :v);
                fFiveValAvgSpeed = (int) (((fFiveValAvgSpeed * 4) + locCurrent.getSpeed()) / 5);
                iSecondsToSpeedChange = (int) ((DistanceToNextSpeedChange / fFiveValAvgSpeed));
                updateDebugText();
            }


        } catch (JSONException e) {
            //e.printStackTrace();
            Log.i(TAG, "doStuff - no value for Lat");
        }
    }
    private String oneTo1(String x) {
        if(x.equals("\u0001")){
            return "1";
        }
        else {
            return "0";
        }
    }


    public void createTextToSpeech(final Context context, final Locale locale)
    {
        mTts = new TextToSpeech(context, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if (status == TextToSpeech.SUCCESS)
                {
                    Locale defaultOrPassedIn = locale;
                    if (locale == null)
                    {
                        defaultOrPassedIn = Locale.getDefault();
                    }
                    // check if language is available
                    switch (mTts.isLanguageAvailable(defaultOrPassedIn))
                    {
                        case TextToSpeech.LANG_AVAILABLE:
                        case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                        case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                            Log.d(TAG, "SUPPORTED");
                            mTts.setLanguage(locale);
                            //pass the tts back to the main
                            //activity for use
                            break;
                        case TextToSpeech.LANG_MISSING_DATA:
                            Log.d(TAG, "MISSING_DATA");
                            Log.d(TAG, "require data...");
                            Intent installIntent = new Intent();
                            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                            context.startActivity(installIntent);
                            break;
                        case TextToSpeech.LANG_NOT_SUPPORTED:
                            Log.d(TAG, "NOT SUPPORTED");
                            break;
                    }
                }
            }
        });
    }

    private final Runnable timedGPSqueue; {
        timedGPSqueue = new Runnable() {
            @Override
            public void run() {
                noGPS((locCurrent.getLatitude() == 0.0));
                if (iNotCommsLockedOut < 6){    // DON'T LET THE COMMS QUEUE GET TO BUG
                    callWebService();
                }
                handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //repeating so needed

                Log.i(TAG, "run  REPEAT TIMER  "+ locCurrent.getAccuracy());
            }
        };
    }


    private void RetreiveSettings() {


        sUUID = appSharedPrefs.getString(getString(R.string.myUUID), "");

        if (sUUID.equals("")){
            sUUID= randomUUID().toString();
            appSharedPrefs.edit().putString(getString(R.string.myUUID)  ,sUUID ).commit();
        }
        //Map<String, ?> xx = appSharedPrefs.getAll();


        bMute = !(appSharedPrefs.getBoolean(getString(R.string.settings_soundKey), false));  // Active Low
        bDebug = appSharedPrefs.getBoolean(getString(R.string.settings_debugKey), false);
//        alertOnGreenLightEnabled = appSharedPrefs.getBoolean(getString(R.string.settings_alertOnGreenLightEnabledKey), false);
//        userEmail = appSharedPrefs.getString(getString(R.string.settings_userEmailKey), "");
//        ttsSalute = appSharedPrefs.getString(getString(R.string.settings_ttsSaluteKey), getString(R.string.ttsSalute));
//        ttsSignFound = appSharedPrefs.getString(getString(R.string.settings_ttsSignFoundKey), getString(R.string.ttsSignFound));
//        bExperimental = appSharedPrefs.getBoolean(getString(R.string.settings_bExperimentalKey), false);
//        debugVerbosity = Integer.parseInt(appSharedPrefs.getString(getString(R.string.settings_debugVerbosityKey), "0"));

        updateDebugIcon();
    }


    public void NeedToResetDisplay() {
        iNeedToResetDisplay++;
        if (iNeedToResetDisplay>3){
            setDisplay(0);
            iNeedToResetDisplay = 0;

        }
    }

//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////MAIN - END  OF COMMON CODE//////////////////////////////////////////////////////////


    private void noGPS(boolean bNoGps)  {

    TextView textView = (TextView) findViewById(itextViewGPSlost);
    if (bNoGps) {
        textView.setVisibility(View.VISIBLE);
    } else {
        textView.setVisibility(View.GONE);
    }
}


    private void toggleRadioButton() {

        RadioButton b = (RadioButton)(findViewById(R.id.radioButton));
        b.setChecked(!b.isChecked());
    }

    private void updateTimeoutIcon() {
        if(bCommsTimedOut) {vImageViewTimeout.setVisibility(View.VISIBLE);}
        else{ vImageViewTimeout.setVisibility(View.GONE); }
    }




    private void updateDebugIcon() {
        if(bDebug) {vImageViewDebug.setVisibility(View.VISIBLE);}
        else{ vImageViewDebug.setVisibility(View.GONE); }
    }


    private void updateDebugText() throws JSONException {
//        if (DistanceToNextSpeedChange>=60){
//            x = String.valueOf((int)(DistanceToNextSpeedChange/1000)+1) + "Km           or       "+ String.valueOf((int) (iSecondsToSpeedChange/60)+1) + "Min\n";
//        }
//        else{
//            x = String.valueOf((int)DistanceToNextSpeedChange) + "M           or       "+ String.valueOf((int) (iSecondsToSpeedChange)) + "Sec\n";
//        }


            if (bDebug) {
                String x = String.valueOf(DistanceToNextSpeedChange) + "  " + String.valueOf(iSecondsToSpeedChange) + "\n";
                setDebugText(itextView, x);
                x = "\n\n\n\n\n" + locCurrent.getLatitude() + "," + locCurrent.getLongitude() + " ,  B:" + locCurrent.getBearing()   + " ,  A:" +  locCurrent.getAccuracy()           ;
                setDebugText(itextView2, x);
            } else {
                setDebugText(itextView, "");
                setDebugText(itextView2, "");
            }

        }

    //todo
//    public static Context getAppContext() {
//        return context;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //client.setTimeout(2000);
//
//
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        createTextToSpeech(this, Locale.getDefault());

        new WhatsNewScreen(this).show();


        vImageButton = findViewById(R.id.imageButton);
        vErrorButton = findViewById( R.id.imageButtonError);
        vImageBtnSmall = findViewById(R.id.imageBtnSmall);
        vImageViewDebug =  findViewById( R.id.imageViewDebug);
        vImageViewTimeout = findViewById(R.id.imageViewTimeout);

        // Receive Settings
        RetreiveSettings();

        // Use instance field for listener
        // It will not be gc'd as long as this instance is kept referenced
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                RetreiveSettings();
                Log.i(TAG, "onSharedPreferenceChanged  ");
            }
        };

        appSharedPrefs.registerOnSharedPreferenceChangeListener(listener);



        // Turn on teh GPS.     set up GPS
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistanceGPS, this);



        //handler.postDelayed(timedGPSqueue, 8);   //Start timer

        vImageButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                //Send human reported speed error to server
                bZoneError = true;
                ImageButton imgerr = (ImageButton) vErrorButton;
                imgerr.setVisibility(View.VISIBLE);
                callWebService();
                return true;
            }
        });

        vImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "LONG PRESS to change", Toast.LENGTH_SHORT).show();
            }
        });

        vErrorButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Send Error to URL
                bZoneError = false;
                ImageButton imgerr = (ImageButton) vErrorButton;
                imgerr.setVisibility(View.INVISIBLE);
                callWebService();
                return true;
            }
        });

        vErrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "LONG PRESS to change", Toast.LENGTH_SHORT).show();
            }
        });


//        try {
//            mTts.speak(ttsSalute, TextToSpeech.QUEUE_FLUSH, null);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        callWebService();
        updateTimeoutIcon();
        updateDebugIcon();

//        //5000 is the starting number (in milliseconds)
//        //1000 is the number to count down each time (in milliseconds)
//        MyCount counter = new MyCount(5000,1000);
//todo        counter.start();


    }

    //countdowntimer is an abstract class, so extend it and fill in methods
    public class MyCount extends CountDownTimer {
        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onFinish() {
            callWebService();
        }
        @Override
        public void onTick(long millisUntilFinished) {
            //tv.setText(”Left: ” + millisUntilFinished/1000);
        }
    }






    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        switch (item.getItemId()) {
            case R.id.menu_float:

                //if(isMyServiceRunning())
            {
                Intent intent;
                Bundle extras;
                intent = new Intent(MainActivity.this, ChatHeadService.class);
                intent.putExtra("TheOK", true);
                intent.putExtra(sUUID, "");
                Log.i(TAG, "bDebug is  " + bDebug);
                intent.putExtra("bDebug", bDebug);
                intent.putExtra("bCommsTimedOut", bCommsTimedOut);
                intent.putExtra("iSpeed",iSpeed);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                handler.removeCallbacks(timedGPSqueue);
                moveTaskToBack(true);
                startService(intent);
                //onStop();
            }
            //finish();
            return true;


            case R.id.settings:
                // Settings Menu

//                if (isTablet(this)){
//
//                Intent i = new Intent(this, PreferencesActivity.class);
//                startActivityForResult(i, intentSettings);
//
//                }
//                else
//
//                {
                Intent i = new Intent(this, PreferencesActivitySingle.class);
                startActivityForResult(i, intentSettings);
//                }
//
                return true;


            case R.id.menu_feedback:
                sendFeedback();
                finish();
                return true;

            case R.id.menu_debug:
                bDebug = !bDebug;
                updateDebugIcon();
                try {
                    updateDebugText();
                } catch (JSONException e) {
                    Log.i(TAG, "onOptionsItemSelected - No Value for reLon");
                }
                callWebService();
                Toast.makeText(this, String.valueOf(bDebug), Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }    //MENU CODE END

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause  ");
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Stop the GPS listener
        handler.removeCallbacks(timedGPSqueue);
        locManager.removeUpdates(this);


    }
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume  ");

        handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);
        Log.i(TAG, "onResume  START TIMER");
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistanceGPS, this);

    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ChatHeadService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /*
    Dummy in Service
    private void setDebugText(int t, String sdsd){}
    private void setDisplayScale(int t){}
    */

    public void removeChatHeads(){}

    private void setDebugText(int t, String s) throws JSONException {
        TextView textView = (TextView) findViewById(t);
        textView.setText(s);
    }


    private void setDisplayScale(float anmi) {
//        ImageButton img = (ImageButton) vImageBtnSmall;
//        img.setScaleX(anmi);
//        img.setScaleY(anmi);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case intentSettings:
                // Retreive Settings
               // RetreiveSettings();
                break;
        }
    }


    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void sendFeedback() {
        try {
            int i = 3 / 0;
        } catch (Exception e) {
            ApplicationErrorReport report = new ApplicationErrorReport();
            report.packageName = report.processName = getApplication().getPackageName();
            report.time = System.currentTimeMillis();
            report.type = ApplicationErrorReport.TYPE_CRASH;
            report.systemApp = false;

            ApplicationErrorReport.CrashInfo crash = new ApplicationErrorReport.CrashInfo();
            crash.exceptionClassName = e.getClass().getSimpleName();
            crash.exceptionMessage = e.getMessage();

            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);
            e.printStackTrace(printer);

            crash.stackTrace = writer.toString();

            StackTraceElement stack = e.getStackTrace()[0];
            crash.throwClassName = stack.getClassName();
            crash.throwFileName = stack.getFileName();
            crash.throwLineNumber = stack.getLineNumber();
            crash.throwMethodName = stack.getMethodName();

            report.crashInfo = crash;

            Intent intent = new Intent(Intent.ACTION_APP_ERROR);
            intent.putExtra(Intent.EXTRA_BUG_REPORT, report);
            startActivity(intent);
        }
    }





//////////////////////////////////////////////////////////////////////////////////////////////
} //END OF CODE




//SELECT `RE` , `reLat` , `reLon` , `reBearing` , `reSpeedLimit` , `RdNo` , `rePrescribed` , `reRoadName`
//FROM (
//
//   SELECT `RE` , `reLat` , `reLon` , `reBearing` , `reSpeedLimit` , `RdNo` , `rePrescribed` , `reRoadName`
//   FROM (
//
//        SELECT `RE` , `reLat` , `reLon` , `reBearing` , `reSpeedLimit` , `RdNo` , `rePrescribed` , `reRoadName`
//        FROM `tblSpeedZone`
//        WHERE `RdNo` = '0006004'
//        ) AS a
//
//   WHERE `rePrescribed` = TRUE AND `reSpeedLimit` <> 60
//   ) AS b
//
//WHERE re <= '0006004\1230C1C\000259'
//ORDER BY 1
//LIMIT 1 , 3




// use speed; CREATE TABLE tblSpeedZone (RE varchar(22) , reLat float , reLon float , reBearing int , reSpeedLimit int , RdNo varchar(7),  rePrescribed bool, reRoadName nvarchar(50));

// awk -F';' '{print $1 "," $6 ","     $7","   $8","  $9"," $11"," $13","     $15;}'  reid_definitions.txt > /etc/tblSpeedZone.txt
// "reLat"      ,"reLon","RLID","Filename","RDNO","LKNO","rePrescribed","reBearing","reSpeedLimit","CWAY_CODE"
//    9             10      11       12       13     14      15               16         17
//,"-36.09","146.91354579700","20000002229503000050","0000002\2295C1C\000050","0000002","2295","0","64","110","C"



// ALTER TABLE `tblSpeedZone` ADD INDEX( `reLat`, `reLon`)


//http://220.233.25.75/mydb2.php?lat=-34.0488&lon=151.052&ber=23
//http://220.233.25.75/mynext.php?reSpeedLimit=90&RdNo=0000001&rePrescribed=0&RE=00000010570C2C000055f9


//        SELECT top 1
//        [RE]
//        ,[reLat]
//        ,[reLon]
//        ,[reBearing]
//        ,[reSpeedLimit]
//        ,[RdNo]
//        ,[rePrescribed]
//        ,[reRoadName]
//        FROM [RISSxplr].[Application].[ReId_Definitions]
//        WHERE re <= '0006004\1230C1C\000259'
//        and RdNo = '0006004'
//        and rePrescribed = 0
//        and reSpeedLimit <> 110
//        order by [RE] desc
