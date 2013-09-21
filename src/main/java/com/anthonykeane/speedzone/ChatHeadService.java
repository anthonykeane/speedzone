package com.anthonykeane.speedzone;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.abs;
import static java.util.UUID.randomUUID;

public class ChatHeadService extends Service implements LocationListener {


    // unique to ChatHeadService
    private boolean bYouMovedIt = false;
	private WindowManager windowManager;
	public List<View> chatHeads;
	private LayoutInflater inflater;

//88888888888888888888888888888888888888888888888888888888888888888888888


    //Is different in ChatHeadService
    private static final boolean bThisIsMainActivity = false;
    private static final String TAG = "ChatHead::Service";
    private static final String TAGd = "ChatHead::Service_focus";
    private   boolean toDIE = false;


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


    private static boolean isRunning;


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
    private int iAlertMode = 3;
    private static final int iMinAccuracy = 9;

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
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

        if(location.hasAccuracy() && location.hasBearing() && location.hasSpeed() && location.getAccuracy()<iMinAccuracy)
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
                        ||  (abs(locLast.getBearing()-locCurrent.getBearing())>7.0)   )
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
        if ( (iDisplayingS != iSpeed) && (bSmall == bThisIsMainActivity) && (locCurrent.getAccuracy()<=iMinAccuracy)  && (locCurrent.hasAccuracy()))
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
        if ( (iDisplayingG != iSpeed) && (bSmall == bThisIsMainActivity) && ((locCurrent.getAccuracy()>iMinAccuracy)  || (!locCurrent.hasAccuracy())))
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

            if (   ((locCurrent.getAccuracy()>=iMinAccuracy) || (!locCurrent.hasAccuracy()))  && bDebug)
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
                                AlertAnnounce();


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

    private void AlertAnnounce() {
        int SpeedLimit = 0;
        int intCurrentSpeeed = (int)(locCurrent.getSpeed()*3.6);
        try { SpeedLimit = jHereResult.getInt("reSpeedLimit");}
        catch (JSONException e) {e.printStackTrace(); }
        if ((!bMute) && (iSpeed != SpeedLimit))
        {
            mTts.speak(getString(R.string.SpeakAlertSpeedChange)  + String.valueOf(SpeedLimit), TextToSpeech.QUEUE_FLUSH, null);

            if (intCurrentSpeeed>(SpeedLimit) && intCurrentSpeeed<(SpeedLimit+3) && (iAlertMode <= 1))
            {
                mTts.speak(getString(R.string.SpeakAlertSpeedChangeSpeeding) , TextToSpeech.QUEUE_ADD, null);
            }
            if (intCurrentSpeeed>=(SpeedLimit+3) && intCurrentSpeeed<(SpeedLimit+10) && (iAlertMode <= 1))
            {
                mTts.speak(getString(R.string.SpeakAlertSpeed1point)  , TextToSpeech.QUEUE_ADD, null);
            }
            if (intCurrentSpeeed>=(SpeedLimit+10) && intCurrentSpeeed<(SpeedLimit+20)  && (iAlertMode <= 3))
            {
                mTts.speak(getString(R.string.SpeakAlertSpeed3points) , TextToSpeech.QUEUE_ADD, null);
            }
            if (intCurrentSpeeed>=(SpeedLimit+20) && intCurrentSpeeed<(SpeedLimit+30) && (iAlertMode <= 4))
            {
                mTts.speak(getString(R.string.SpeakAlertSpeed4points)  , TextToSpeech.QUEUE_ADD, null);
            }
            if (intCurrentSpeeed>=(SpeedLimit+30) && intCurrentSpeeed<(SpeedLimit+45) && (iAlertMode <= 5))
            {
                mTts.speak(getString(R.string.SpeakAlertSpeed5points)  , TextToSpeech.QUEUE_ADD, null);
            }
            if (intCurrentSpeeed>=(SpeedLimit+45) && (iAlertMode <= 6))
            {
                mTts.speak(getString(R.string.SpeakAlertSpeed6points)  , TextToSpeech.QUEUE_ADD, null);
            }
            DistanceToNextSpeedChange = 0;
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
                iSecondsToSpeedChange = (DistanceToNextSpeedChange / (fFiveValAvgSpeed+1));
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
        //Map<String, ?> xx = appSharedPrefs.getAll();


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
        iAlertMode = Integer.parseInt(appSharedPrefs.getString(getString(R.string.settings_Alert_Key), "1"));

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

    private void onStopUpdates() {
    }

    private void onStartUpdates() {
    }

    private void toggleRadioButton() {

    }

    private void noGPS(boolean bNoGps)  {

        try {
            setGraphicBtnV(vImageButton, jHereResult.getInt("reSpeedLimit"), bNoGps);
        } catch (JSONException e) {
            Log.i(TAG, "noGPS - No value for reSpeedLimit ");
        }
    }


    private void initTextToSpeach() {
        //Sound TTS
        Intent checkIntent = new Intent();
       // checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
       // startActivityForResult(checkIntent, intentTTS);
// success, create the TTS instance
        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                mTts.setLanguage(Locale.US);
                //mTts.setLanguage(Locale.getDefault());
                if (!bMute)
                {
                    try {
                        mTts.speak("hello", TextToSpeech.QUEUE_FLUSH, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }


    private void updateAlertImage(boolean b) {
    }


    private void updateTimeoutIcon() {
        if(bCommsTimedOut) {vImageViewTimeout.setVisibility(View.VISIBLE);}
        else{ vImageViewTimeout.setVisibility(View.GONE); }
    }




    private void updateDebugIcon() {
        if(bDebug) {vImageViewDebug.setVisibility(View.VISIBLE);}
        else{ vImageViewDebug.setVisibility(View.GONE); }
    }














    private void updateDebugText() {


    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "onCreate  1");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        inflater = LayoutInflater.from(this);
        chatHeads = new ArrayList<View>();
        Log.i(TAG, "onCreate  ");
        // Retreive Settings
        //RetreiveSettings();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //if(startId ==1)
        isRunning = true;
        {
            toDIE = false;
            final View chatHead = inflater.inflate(R.layout.chat_head, null);
            Log.i("Local Service", "Received start id " + startId + ": " + intent);
            if (chatHead != null)
            {
                vImageButton = chatHead.findViewById(R.id.imageButton);
                vErrorButton = chatHead.findViewById( R.id.imageButtonError);
                vImageBtnSmall = chatHead.findViewById(R.id.imageBtnSmall);
                vImageViewDebug = chatHead.findViewById( R.id.imageViewDebug);
                vImageViewTimeout = chatHead.findViewById(R.id.imageViewTimeout);
            }


            Intent inPower = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int iPluggedIn = inPower.getIntExtra("plugged", 0);

            // Turn on the GPS.     set up GPS
            locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
            //Start the GPS listener
            if (iPluggedIn == 0)
            {
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistanceGPS, this);
            }
            else
            {
                locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistanceGPS, this);
            }

            boolean bOK;

            try {
                bOK = intent.getBooleanExtra("TheOK",false);
                sUUID = intent.getStringExtra("sUUID");
                bDebug = intent.getBooleanExtra("bDebug",false);
                bCommsTimedOut = intent.getBooleanExtra("bCommsTimedOut",false);
                Log.i(TAG, "2.2 bCommsTimedOut is  "+bCommsTimedOut);
                Log.i(TAG, "2.2 bDebug is  "+bDebug);
                updateTimeoutIcon();
                updateDebugIcon();
                //iSpeed =  intent.getIntExtra("iSpeed", 50);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "onStartCommand  Exception");

                toDIE =true;
                onDestroy();
                return 0;
            }


            if(bOK)
            {
                Log.i(TAG, "onStartCommand  THE OK true");
            }
            else
            {
                Log.i(TAG, "onStartCommand  NOT ***********");
            }


            // got iSpeed above
            setGraphicBtnV(vImageButton, 50, true);


            handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //Start timer
            callWebServiceHere();


            vImageButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    if (!bYouMovedIt) {
                        // Send Error to URL
                        bZoneError = true;
                        ImageButton imgerr = (ImageButton) vErrorButton;
                        imgerr.setVisibility(View.VISIBLE);
                        callWebServiceHere();


                    }
                    return true;
                }
            });

            vErrorButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    if (!bYouMovedIt) {
                        // Send Error to URL
                        bZoneError = false;
                        ImageButton imgerr = (ImageButton) vErrorButton;
                        imgerr.setVisibility(View.GONE);
                        callWebServiceHere();
                    }
                    return true;
                }
            });


            vImageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!bYouMovedIt) {
                        Log.i("Service", "onStart() is called");
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        callIntent.setClass(v.getContext(), MainActivity.class);
                        callIntent.putExtra("float","2");
                        // todo   callIntent.putExtra("bZoneError",bZoneError);
                        startActivity(callIntent);
                        removeChatHead(chatHead);
                    }
                }
            });

            vErrorButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!bYouMovedIt) {
                        // Send Error to URL
                        bZoneError = false;
                        ImageButton imgerr = (ImageButton) vErrorButton;
                        imgerr.setVisibility(View.GONE);
                        callWebServiceHere();
                    }
                }
            });



            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(

                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.CENTER;
            // retrieve last position of chathead
            params.x = Integer.parseInt(LoadSetting("params.x"));
            params.y = Integer.parseInt(LoadSetting("params.y"));

            vImageButton.setOnTouchListener(new View.OnTouchListener() {
                private int didwemove;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // to dispatch click / long click event,
                    // you must pass the event to it's default callback View.onTouchEvent

                    //boolean defaultResult = v.onTouchEvent(event);

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            didwemove = params.x;

                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            //return true;
                            bYouMovedIt = false;
                            break;
                        case MotionEvent.ACTION_UP:
                            SaveSetting("params.x", String.valueOf(params.x));
                            SaveSetting("params.y", String.valueOf(params.y));
                            //return true;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            try {
                                windowManager.updateViewLayout(chatHead, params);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.i(TAG, String.valueOf(didwemove) + "    " + String.valueOf(params.x));


                            bYouMovedIt = ((StrictMath.abs(params.x - didwemove) > 10));
                            //return true;
                            break;
                    }
                    return false;
                }
            });

            vErrorButton.setOnTouchListener(new View.OnTouchListener() {
                private int didwemove;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // to dispatch click / long click event,
                    // you must pass the event to it's default callback View.onTouchEvent
                    //boolean defaultResult = v.onTouchEvent(event);

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            didwemove = params.x;

                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            //return true;
                            bYouMovedIt = false;
                            break;
                        case MotionEvent.ACTION_UP:
                            SaveSetting("params.x", String.valueOf(params.x));
                            SaveSetting("params.y", String.valueOf(params.y));
                            //return true;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(chatHead, params);
                            Log.i(TAG, String.valueOf(didwemove) + "    " + String.valueOf(params.x));
                            bYouMovedIt = params.x != didwemove;
                            //return true;
                            break;
                    }
                    return false;
                }
            });


            createTextToSpeech(this, Locale.getDefault());
            addChatHead(chatHead, params);
        }
        return super.onStartCommand(intent, flags, startId);

    }







    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void gpsUpdated() {
        Log.i(TAGd, "gpsUpdated  ");
    }



    // Dummy in Service
    private void setDebugText(int t, String sdsd){}



    private void setDisplayScale(float f){}



    private void SaveSetting(String key, String value){

        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        appSharedPrefs.edit().putString(key, value).commit();
    }

    private String LoadSetting(String key){

        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        return appSharedPrefs.getString(key, "0");
    }

    public void addChatHead(View chatHead, LayoutParams params) {
        Log.i(TAG, "addChatHead  3");chatHeads.add(chatHead);
        windowManager.addView(chatHead, params);
    }

    public void removeChatHead(View chatHead) {
        Log.i(TAG, "removeChatHead  4");

        try { // Turn Off the GPS
            locManager.removeUpdates(this); // Turn Off the GPS
        } catch (Exception e) {e.printStackTrace(); }
        if (locManager!=null){locManager = null;}

        //Toast.makeText(this,"RENOVING " , Toast.LENGTH_SHORT).show();
        chatHeads.remove(chatHead);
        try {
            windowManager.removeView(chatHead);
        } catch (Exception e) {
            //todo catch
            //e.printStackTrace();
        }
        onDestroy();

    }

    public void removeChatHeads(){
        for (View chatHead : chatHeads) {
            removeChatHead(chatHead);
        }
    }




    private void callPOI(){
        if ((locCurrent.getBearing()!= 0.0) || bDebug)
        {
            RequestParams HTTPrpA = new RequestParams();
            HTTPrpA.put("lat", String.valueOf(locCurrent.getLatitude()));
            HTTPrpA.put("lon", String.valueOf(locCurrent.getLongitude()));
            HTTPrpA.put("ber", String.valueOf(locCurrent.getBearing()));
            HTTPrpA.put("speed", String.valueOf(locCurrent.getSpeed()));
            if ((DistanceToPOI < 1000) || (DistanceToPOI == 0))                         //refresh when close only
            {
                client.get(getString(R.string.MyPOIWeb), HTTPrp2, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            poi.setLatitude(response.getDouble("reLat"));
                            poi.setLongitude(response.getDouble("reLon"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, JSONObject errorResponse) {

                        Log.i(TAGd, "onFailure  3");
                        DistanceToPOI = 0;
                        // Completed the request (either success or failure)
                    }

                });
            }
        }
    }




    public static boolean isRunning()
    {
        return isRunning;
    }












} //END OF CODE



