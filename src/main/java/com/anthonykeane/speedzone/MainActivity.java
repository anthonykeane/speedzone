package com.anthonykeane.speedzone;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.content.*;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spanned;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.lang.Math.abs;
import static java.util.UUID.randomUUID;


public class MainActivity extends Activity implements LocationListener {

    //Is different in MainActivity
    private static final boolean bThisIsMainActivity = true;
    private static final String TAG = "ChatHead::Activity";
    private static final String TAGd = "ChatHead::Activity_focus";

    private static final int intentSettings = 1;
    public static final int itextViewGPSlost = R.id.textViewGPSlost;


    private static final int MAX_LOG_SIZE = 5000;

    // Instantiates a log file utility object, used to log status updates
    private LogFile mLogFile;

    // Store the current request type (ADD or REMOVE)
    private ActivityUtils.REQUEST_TYPE mRequestType;

    // Holds the ListView object in the UI
    private ListView mStatusListView;

    /*
     * Holds activity recognition data, in the form of
     * strings that can contain markup
     */
    private ArrayAdapter<Spanned> mStatusAdapter;

    /*
     *  Intent filter for incoming broadcasts from the
     *  IntentService.
     */
    IntentFilter mBroadcastFilter;

    // Instance of a local broadcast manager
    private LocalBroadcastManager mBroadcastManager;

    // The activity recognition update request object
    private DetectionRequester mDetectionRequester;

    // The activity recognition update removal object
    private DetectionRemover mDetectionRemover;




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
    private String ttsSalute;

    SharedPreferences appSharedPrefs;

    //GPS delay stuff

    private Location locCurrent = new Location(""); // GPS right now
    private Location locLast = new Location("");    // right now n-1
    private Location locLastCallPOI = new Location("");
    private Location locLastCallNext = new Location("");
    private Location locLastCallHere = new Location("");

    private TextToSpeech mTts;

    public static final int delayBetweenGPS_Records = 60000;    //every 500mS log Geo date in Queue.
    public static final long minTime = 1000;                   // don't update GPS if time < mS
    public static final float minDistanceGPS = 10;              // don't update GPS if distance < Meters

    private final Handler handler = new Handler();                // used for timers

    // public final LocListener gpsListener = new LocListener();    // used by GPS

    public int iNeedToResetDisplay = 0;

    public final AsyncHttpClient client = new AsyncHttpClient();
    public final RequestParams HTTPrp = new RequestParams();
    public final RequestParams HTTPrp2 = new RequestParams();

    public JSONObject jHereResult = new JSONObject();
    public JSONObject jThereResult = new JSONObject();

    public int DistanceToNextSpeedChange = 0;            //any BIG number or zero
    private int DistanceToPOI = 0;
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

    //private Location me = new Location("");
    //private Location dest = new Location("");
    private Location poi = new Location("");
    //private static Context context;

    //Flags
    public boolean bZoneError = false;
    public boolean bDebug = false;
    public int iNotCommsLockedOut = 0;                   //Lock out comms until last request is serviced
    public boolean bCommsTimedOut = false;
    private boolean bMute = false;
    private int iDistanceOffset = 0;    // don't think this helps
    private int iDisplayingB = 0;
    private int iDisplayingG = 0;
    private int iDisplayingS = 0;
    private int iLaunchMode = 1;

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

        if(location.hasAccuracy() && location.hasBearing() && location.hasSpeed() && location.getAccuracy()<8)
        {
            noGPS(false);
            Log.i(TAG, "onLocationChanged  GOOD");
            locLast = locCurrent; // this supposed to be here (think again)


            locCurrent = location;
            //me.setLatitude(locCurrent.getLatitude());
            //me.setLongitude(locCurrent.getLongitude());




             callPOI();







            if (DistanceToNextSpeedChange < (int)(locCurrent.distanceTo(locLastCallNext)))
            {
                DistanceToNextSpeedChange = 0; // if last getting farther away recalculate
            }
            else
            {
                DistanceToNextSpeedChange = (int)(locCurrent.distanceTo(locLastCallNext) - iDistanceOffset);
            }





//            if(poi.hasAccuracy())

            //if (DistanceToPOI < (int)(locCurrent.distanceTo(poi)))
            {
                if((DistanceToPOI < (int)(locCurrent.distanceTo(poi))) ||  locCurrent.distanceTo(poi)>300)
                {
                    //locLastCallPOI = new Location(""); // if last getting farther away recalculate
                    DistanceToPOI = 0;
                    updateAlertImage(false);
                }
                else
                {
                    DistanceToPOI = (int)( locCurrent.distanceTo(poi) - iDistanceOffset);
                    updateAlertImage(true);
                }
            }
            //Log.i("GPS", "onLocationChanged  ");



            if (iNotCommsLockedOut ==0){
                // if params of locaton unchanged skip
                if ((abs(locLast.getSpeed() - locCurrent.getSpeed())>2.0)
                    ||  (abs(locLast.getBearing()-locCurrent.getBearing())>15.0)
                    || (DistanceToNextSpeedChange<50)
                    || (DistanceToPOI < 50)    )
                {
                    callWebServiceHere();
                }
                //doStuff();
            }

            updateDebugText();
        }
        else
        {
            Log.i(TAG, "onLocationChanged  BAD");
            noGPS(true);
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



        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((!bSmall && bThisIsMainActivity) && (iDisplayingB != iSpeed))
        {
            iDisplayingB = iSpeed;
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
            img.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce) );
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if ( (iDisplayingS != iSpeed) && (bSmall == bThisIsMainActivity) && (locCurrent.getAccuracy()<=15)  && (locCurrent.hasAccuracy()))
        {
            iDisplayingS = iSpeed;
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
                    img.setImageResource(R.drawable.g50);
                    break;

            }
            img.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce) );
        }


        //noinspection PointlessBooleanExpression,ConstantConditions
        if ( (iDisplayingG != iSpeed) && (bSmall == bThisIsMainActivity) && ((locCurrent.getAccuracy()>15)  || (!locCurrent.hasAccuracy())))
        {

            iDisplayingG = iSpeed;

            switch (iSpeed) {
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
            img.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce) );
        }

    }





    private void callWebServiceHere()
    {
    //    if (locCurrent.hasAccuracy())
        {
            Time now = new Time();
            now.setToNow();

            HTTPrp.put("When", now.format("%Y-%m-%d %H:%M:%S"));
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

            //todo

            if (   ((locCurrent.getAccuracy()>=15) || (!locCurrent.hasAccuracy()))  && bDebug)
            {
                HTTPrp.put("lat", "-33.71013");
                HTTPrp.put("lon", "150.94951");
                HTTPrp.put("ber", "100");
                HTTPrp.put("speed", "99");
                HTTPrp.put("UUID", "test-" + sUUID);
                HTTPrp.put("When", now.format("%Y-%m-%d %H:%M:%S"));
            }

            //Toast.makeText(this, String.valueOf(LocListener.getLat()), Toast.LENGTH_SHORT).show();
            // todo if ((locCurrent.getAccuracy()<15) && (locCurrent.getAccuracy()!=0.0)  || bDebug)
            {

                if(iNotCommsLockedOut == 0)
                {
                    client.get(getString(R.string.MyDbWeb), HTTPrp, new JsonHttpResponseHandler() {

                        @Override
                        public void onFailure(Throwable e, JSONObject errorResponse) {
                            System.out.println(e);
                            Log.i(TAG, "onFailure MyDbWeb");
                            bCommsTimedOut = false;
                            //Clear the display if we don't know the value
                            // Skip is too slow to matter
                            if (locCurrent.getSpeed() >= 7)
                            {
                                NeedToResetDisplay();
                            }
                        }

                        @Override
                        public void onSuccess(JSONObject response) {
                            bCommsTimedOut = false;
                            Log.i(TAG, "           onSuccess MyDbWeb ");
                            jHereResult = response;
                            try {
                               doStuff();
                                Log.i(TAG, "onSuccess - reSpeedLimit " + jHereResult.getInt("reSpeedLimit")   );

                                // if changing speed zone Alert but only if your speed is > than posted
                                if (!bMute && (iSpeed != jHereResult.getInt("reSpeedLimit")) ) {
                                    try {


                                        if ((locCurrent.getSpeed()*3.6)>jHereResult.getInt("reSpeedLimit"))
                                        {
                                            mTts.speak("the Speed is now " + String.valueOf(jHereResult.getInt("reSpeedLimit")), TextToSpeech.QUEUE_FLUSH, null);
                                            mTts.speak("check your speed . " , TextToSpeech.QUEUE_ADD, null);
                                        }
                                    } catch (Exception e) {
                                        Log.i(TAG, "onSuccess - No value for reSpeedLimit");
                                    }
                                    DistanceToNextSpeedChange = 0;
                                }


                                iSpeed = jHereResult.getInt("reSpeedLimit");
                                setGraphicBtnV(vImageButton, iSpeed, false);
                                //Toast.makeText(getApplicationContext(), iSecondsToSpeedChange, Toast.LENGTH_SHORT).show();
                                Log.i(TAG, "onSuccess  " + iSecondsToSpeedChange + " iSecondsToSpeedChange ");


                                if (bThisIsMainActivity) {
                                    //updateDebugText();
                                    MyNextWebService();
                                }



                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onStart() {
                            // Completed the request (either success or failure)
                            //toggleRadioButton();
                            Log.i(TAG, "onStart  ");
                            bCommsTimedOut = true;
                            iNotCommsLockedOut++;
                        }

                        @Override
                        public void onFinish() {
                            // Completed the request (either success or failure)
                            //toggleRadioButton();
                            iNotCommsLockedOut--;
                            if (iNotCommsLockedOut <= 2) iNotCommsLockedOut = 0;
                            updateTimeoutIcon();
                            if (bCommsTimedOut) {
                                setDisplay(0);
                            }
                            Log.i(TAG, "                       onFinish MyDbWeb ");
                        }
                    });
                }

            }
        }
    }


    private void MyNextWebService() {

        try {
            HTTPrp2.put("reMainRoad", oneTo1(String.valueOf(jHereResult.getString("reMainRoad"))));
            HTTPrp2.put("rePrescribed", oneTo1(String.valueOf(jHereResult.getString("rePrescribed"))));
            HTTPrp2.put("RE", String.valueOf(jHereResult.getString("RE")));
            HTTPrp2.put("reSpeedLimit", String.valueOf(jHereResult.getString("reSpeedLimit")));
            HTTPrp2.put("RdNo", String.valueOf(jHereResult.getString("RdNo")));

            if ((iSecondsToSpeedChange < 60) || (DistanceToNextSpeedChange < 1000) || (DistanceToNextSpeedChange == 0))                         //refresh when close only
            {
                Log.i(TAG, "onSuccess  Getting Speed change");
                client.get(getString(R.string.MyNextWeb), HTTPrp2, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.i(TAGd, "onSuccess MyNextWeb  ");
                        bCommsTimedOut = false;
                        jThereResult = response;
                        doStuff();
                    }

                    @Override
                    public void onFailure(Throwable e, JSONObject errorResponse) {

                        Log.i(TAGd, "onFailure  MyNextWeb");
                        DistanceToNextSpeedChange = 0;

                    }


                    @Override
                    public void onFinish() {
                        // Completed the request (either success or failure)

                        updateTimeoutIcon();
                        Log.i(TAGd, "onFinish  MyNextWeb");
                    }
                });
            }
        } catch (JSONException e) {
        Log.i(TAG, "MyNextWebService  NO HereResult");
    }
    }
    private void doStuff() {
        try {
            //me.setLatitude(locCurrent.getLatitude());
            //me.setLongitude(locCurrent.getLongitude());
            locLastCallNext.setLatitude(jThereResult.getDouble("reLat"));
            locLastCallNext.setLongitude(jThereResult.getDouble("reLon"));



            //todo is this line needed?
            DistanceToNextSpeedChange = (int)(locCurrent.distanceTo(locLastCallNext) - iDistanceOffset);
            if (bThisIsMainActivity) {

                if (!bCommsTimedOut) setGraphicBtnV(vImageBtnSmall, jThereResult.getInt("reSpeedLimit"), true);

                fFiveValAvgSpeed = (int) (((fFiveValAvgSpeed * 4) + locCurrent.getSpeed()) / 5);
                iSecondsToSpeedChange = (int) ((DistanceToNextSpeedChange / (fFiveValAvgSpeed+1)));
                //updateDebugText();
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
                try {
                    if (!bMute) mTts.speak(ttsSalute, TextToSpeech.QUEUE_FLUSH, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private final Runnable timedGPSqueue; {
        timedGPSqueue = new Runnable() {
            @Override
            public void run() {
                noGPS(!(locCurrent.hasAccuracy()));
                if (iNotCommsLockedOut < 3){    // DON'T LET THE COMMS QUEUE GET TO BUG
                    callWebServiceHere();
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
        Map<String, ?> xx = appSharedPrefs.getAll();


        bMute = !(appSharedPrefs.getBoolean(getString(R.string.settings_soundKey), false));  // Active Low
        bDebug = appSharedPrefs.getBoolean(getString(R.string.settings_debugKey), false);
//        alertOnGreenLightEnabled = appSharedPrefs.getBoolean(getString(R.string.settings_alertOnGreenLightEnabledKey), false);
//        userEmail = appSharedPrefs.getString(getString(R.string.settings_userEmailKey), "");
        ttsSalute = appSharedPrefs.getString(getString(R.string.settings_ttsSaluteKey), getString(R.string.ttsSalute));
//        ttsSignFound = appSharedPrefs.getString(getString(R.string.settings_ttsSignFoundKey), getString(R.string.ttsSignFound));
//        bExperimental = appSharedPrefs.getBoolean(getString(R.string.settings_bExperimentalKey), false);
//        debugVerbosity = Integer.parseInt(appSharedPrefs.getString(getString(R.string.settings_debugVerbosityKey), "0"));


        if(appSharedPrefs.getBoolean(getString(R.string.settings_activityServicesKey), false)){
            onStartUpdates();
        }else
        {
            onStopUpdates();
        }

        iLaunchMode = Integer.parseInt(appSharedPrefs.getString(getString(R.string.settings_launchTypeKey), "1"));


        updateDebugIcon();
    }


    public void NeedToResetDisplay() {
        iNeedToResetDisplay++;
        Log.i(TAG, "NeedToResetDisplay  "+ iNeedToResetDisplay);
        if (iNeedToResetDisplay>1){
            setDisplay(50);
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
        textView.setVisibility(View.INVISIBLE);
    }
}


    private void toggleRadioButton() {

        RadioButton b = (RadioButton)(findViewById(R.id.radioButton));
        b.setChecked(!b.isChecked());
    }

    private void updateTimeoutIcon() {
        if(bCommsTimedOut) {vImageViewTimeout.setVisibility(View.VISIBLE);}
        else{ vImageViewTimeout.setVisibility(View.INVISIBLE); }
    }

    private void updateAlertImage(boolean bShow)
    {
        ImageView img = (ImageView) findViewById(R.id.imageAlert);
        if (bShow){
            if (img.getVisibility() != View.VISIBLE) {
                img.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce) );
                img.setVisibility(View.VISIBLE);
            }
        }
        else {
            if (img.getVisibility() != View.GONE) {
                img.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce) );
                img.setVisibility(View.GONE);
            }
        }
    }
    private void updateDebugIcon() {
        if(bDebug) {vImageViewDebug.setVisibility(View.VISIBLE);}
        else{ vImageViewDebug.setVisibility(View.INVISIBLE); }
    }


    private void updateDebugText(){
//        if (DistanceToNextSpeedChange>=60){
//            x = String.valueOf((int)(DistanceToNextSpeedChange/1000)+1) + "Km           or       "+ String.valueOf((int) (iSecondsToSpeedChange/60)+1) + "Min\n";
//        }
//        else{
//            x = String.valueOf((int)DistanceToNextSpeedChange) + "M           or       "+ String.valueOf((int) (iSecondsToSpeedChange)) + "Sec\n";
//        }
        ProgressBar pBap = (ProgressBar) findViewById(R.id.progressBar);

        if (DistanceToNextSpeedChange>DistanceToPOI)
        {
            pBap.setProgress(DistanceToPOI);
            pBap.setSecondaryProgress(DistanceToNextSpeedChange );

        }else
        {
            pBap.setProgress(DistanceToNextSpeedChange);
            pBap.setSecondaryProgress(DistanceToPOI);
        }
           try
           {
                if (bDebug) {
                    String x = "dSpeed " + (int)(DistanceToNextSpeedChange) + "\tPOI lc" + (int) locCurrent.distanceTo(locLastCallPOI) + "\n ##  " +(int)locCurrent.distanceTo(poi)+ "\n";
                    setDebugText(itextView, x);
                    x = "\n\n\n\n\n" + " ,  B:" + locCurrent.getBearing()   + " ,  A:" +  locCurrent.getAccuracy()           ;
                    setDebugText(itextView2, x);
                } else {
                    setDebugText(itextView, "");
                    setDebugText(itextView2, "");
                }
            } catch (JSONException e) {
                e.printStackTrace();
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
                callWebServiceHere();
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
                callWebServiceHere();
                return true;
            }
        });

        vErrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "LONG PRESS to change", Toast.LENGTH_SHORT).show();
            }
        });




        callWebServiceHere();
        updateTimeoutIcon();
        updateDebugIcon();

//        //5000 is the starting number (in milliseconds)
//        //1000 is the number to count down each time (in milliseconds)
//          MyCount counter = new MyCount(5000,1000);
//          todo        counter.start();



        // Get a handle to the activity update list
        mStatusListView = (ListView) findViewById(R.id.log_listview);

        // Instantiate an adapter to store update data from the log
        mStatusAdapter = new ArrayAdapter<Spanned>(
                this,
                R.layout.item_layout,
                R.id.log_text
        );

        // Bind the adapter to the status list
        mStatusListView.setAdapter(mStatusAdapter);

        // Set the broadcast receiver intent filer
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Create a new Intent filter for the broadcast receiver
        mBroadcastFilter = new IntentFilter(ActivityUtils.ACTION_REFRESH_STATUS_LIST);
        mBroadcastFilter.addCategory(ActivityUtils.CATEGORY_LOCATION_SERVICES);

        // Get detection requester and remover objects
        mDetectionRequester = new DetectionRequester(this);
        mDetectionRemover = new DetectionRemover(this);

        // Create a new LogFile object
        mLogFile = LogFile.getInstance(this);

        // Receive Settings
        RetreiveSettings();


        // Use instance field for listener
        // It will not be gc'd as long as this instance is kept referenced
        SharedPreferences.OnSharedPreferenceChangeListener splistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                RetreiveSettings();
                Log.i(TAG, "onSharedPreferenceChanged  ");
            }
        };

        appSharedPrefs.registerOnSharedPreferenceChangeListener(splistener);

        if(iLaunchMode == 2)
        {
            Intent intent;
            Bundle extras;
            intent = new Intent(MainActivity.this, ChatHeadService.class);
            intent.putExtra("TheOK", true);
            intent.putExtra(sUUID, "sUUID");
            Log.i(TAG, "bDebug is  " + bDebug);
            intent.putExtra("bDebug", bDebug);
            intent.putExtra("bCommsTimedOut", bCommsTimedOut);
            //intent.putExtra("iSpeed",iSpeed);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            handler.removeCallbacks(timedGPSqueue);
            moveTaskToBack(true);
            startService(intent);
            //onStop();
        }



    }
//
//    //countdowntimer is an abstract class, so extend it and fill in methods
//    public class MyCount extends CountDownTimer {
//        public MyCount(long millisInFuture, long countDownInterval) {
//            super(millisInFuture, countDownInterval);
//        }
//        @Override
//        public void onFinish() {
//            callWebServiceHere();
//        }
//        @Override
//        public void onTick(long millisUntilFinished) {
//            //tv.setText(”Left: ” + millisUntilFinished/1000);
//        }
//    }






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



            // Clear the log display and remove the log files
            case R.id.menu_item_clearlog:
                // Clear the list adapter
                mStatusAdapter.clear();

                // Update the ListView from the empty adapter
                mStatusAdapter.notifyDataSetChanged();

                // Remove log files
                if (!mLogFile.removeLogFiles()) {
                    Log.e(ActivityUtils.APPTAG, getString(R.string.log_file_deletion_error));

                    // Display the results to the user
                } else {

                    Toast.makeText(
                            this,
                            R.string.logs_deleted,
                            Toast.LENGTH_LONG).show();
                }
                // Continue by passing true to the menu handler
                return true;

            // Display the update log
            case R.id.menu_item_showlog:

                // Update the ListView from log files
                updateActivityHistory();

                // Continue by passing true to the menu handler
                return true;

            case R.id.menu_float:

                //if(isMyServiceRunning())
            {
                Intent intent;
                Bundle extras;
                intent = new Intent(MainActivity.this, ChatHeadService.class);
                intent.putExtra("TheOK", true);
                intent.putExtra(sUUID, "sUUID");
                Log.i(TAG, "bDebug is  " + bDebug);
                intent.putExtra("bDebug", bDebug);
                intent.putExtra("bCommsTimedOut", bCommsTimedOut);
                //intent.putExtra("iSpeed",iSpeed);
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
                //vImageBtnSmall.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely));
                updateDebugIcon();
                updateDebugText();
                callWebServiceHere();
                //Toast.makeText(this, String.valueOf(bDebug), Toast.LENGTH_SHORT).show();
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


        // Stop listening to broadcasts when the Activity isn't visible.
        mBroadcastManager.unregisterReceiver(updateListReceiver);


    }
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume  ");

        handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);
        Log.i(TAG, "onResume  START TIMER");
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistanceGPS, this);



        // Register the broadcast receiver
        mBroadcastManager.registerReceiver(
                updateListReceiver,
                mBroadcastFilter);

    }


    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(ActivityUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;

            // Google Play services was not available for some reason
        }
        else {
           // Display an error dialog
            GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0).show();
            return false;


        }
    }
    /**
     * Respond to "Start" button by requesting activity recognition
     * updates.
     */
    public void onStartUpdates() {

        // Check for Google Play services
        if (!servicesConnected()) {

            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = ActivityUtils.REQUEST_TYPE.ADD;

        // Pass the update request to the requester object
        mDetectionRequester.requestUpdates();
    }

    /**
     * Respond to "Stop" button by canceling updates.
     */
    public void onStopUpdates() {

        // Check for Google Play services
        if (!servicesConnected()) {

            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = ActivityUtils.REQUEST_TYPE.REMOVE;

        // Pass the remove request to the remover object
        mDetectionRemover.removeUpdates(mDetectionRequester.getRequestPendingIntent());

        /*
         * Cancel the PendingIntent. Even if the removal request fails, canceling the PendingIntent
         * will stop the updates.
         */
        try {
            mDetectionRequester.getRequestPendingIntent().cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Display the activity detection history stored in the
     * log file
     */
    private void updateActivityHistory() {
        // Try to load data from the history file
        try {
            // Load log file records into the List
            List<Spanned> activityDetectionHistory =
                    mLogFile.loadLogFile();

            // Clear the adapter of existing data
            mStatusAdapter.clear();

            // Add each element of the history to the adapter
            for (Spanned activity : activityDetectionHistory) {
                mStatusAdapter.add(activity);
            }

            // If the number of loaded records is greater than the max log size
            if (mStatusAdapter.getCount() > MAX_LOG_SIZE) {

                // Delete the old log file
                if (!mLogFile.removeLogFiles()) {

                    // Log an error if unable to delete the log file
                    Log.e(ActivityUtils.APPTAG, getString(R.string.log_file_deletion_error));
                }
            }

            // Trigger the adapter to update the display
            mStatusAdapter.notifyDataSetChanged();

            // If an error occurs while reading the history file
        } catch (IOException e) {
            Log.e(ActivityUtils.APPTAG, e.getMessage(), e);
        }
    }

    /**
     * Broadcast receiver that receives activity update intents
     * It checks to see if the ListView contains items. If it
     * doesn't, it pulls in history.
     * This receiver is local only. It can't read broadcast Intents from other apps.
     */
    BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            /*
             * When an Intent is received from the update listener IntentService, update
             * the displayed log.
             */
            mTts.speak("update ", TextToSpeech.QUEUE_FLUSH, null);

            updateActivityHistory();
        }
    };
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



            // If the request code matches the code sent in onConnectionFailed
            case ActivityUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to start activity recognition updates
                        if (ActivityUtils.REQUEST_TYPE.ADD == mRequestType) {

                            // Restart the process of requesting activity recognition updates
                            mDetectionRequester.requestUpdates();

                            // If the request was to remove activity recognition updates
                        } else if (ActivityUtils.REQUEST_TYPE.REMOVE == mRequestType ){

                                /*
                                 * Restart the removal of all activity recognition updates for the
                                 * PendingIntent.
                                 */
                            mDetectionRemover.removeUpdates(
                                    mDetectionRequester.getRequestPendingIntent());

                        }
                        break;

                    // If any other result was returned by Google Play services
                    default:

                        // Report that Google Play services was unable to resolve the problem.
                        Log.d(ActivityUtils.APPTAG, getString(R.string.no_resolution));
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d(ActivityUtils.APPTAG,
                        getString(R.string.unknown_activity_request_code, requestCode));

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


//private Location populateLoc(Location x){
//
//
//
//}

    private void callPOI(){

        if((locCurrent.distanceTo(locLastCallPOI)> 500) || (!locLastCallPOI.hasAccuracy())) // call this is 500m distance of not init-ed
        {

            locLastCallPOI = locCurrent;

            RequestParams HTTPrpA = new RequestParams();
            HTTPrpA.put("lat", String.valueOf(locCurrent.getLatitude()));
            HTTPrpA.put("lon", String.valueOf(locCurrent.getLongitude()));
    //        HTTPrpA.put("ber", String.valueOf(locCurrent.getBearing()));
    //        HTTPrpA.put("speed", String.valueOf(locCurrent.getSpeed()));

    //            HTTPrpA.put("lat", "-35.350");
    //            HTTPrpA.put("lon", "149.11");

                client.get(getString(R.string.MyPOIWeb), HTTPrpA, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {

                            poi = locCurrent; // this is to set values like accuracy etc.
                            poi.setLatitude(response.getDouble("poiLat"));
                            poi.setLongitude(response.getDouble("poiLon"));

                            DistanceToPOI = (int)( locCurrent.distanceTo(poi) - iDistanceOffset);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.i(TAGd, "CallPOI onSuccess  ");
                        //updateAlertImage(true);

                    }

                    public void onStart() {
                        // Completed the request (either success or failure)
                        toggleRadioButton();
                        Log.i(TAG, "onStart  ");

                    }

                    @Override
                    public void onFinish() {
                        // Completed the request (either success or failure)
                        toggleRadioButton();
                        Log.i(TAGd, "CallPOI onFinish  ");
                    }
                    @Override
                    public void onFailure(Throwable e, JSONObject errorResponse) {

                        Log.i(TAGd, "CallPOI onFailure   ");
                        DistanceToPOI = 10000;
                        // Completed the request (either success or failure)
                        updateAlertImage(false);
                    }

                });


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






