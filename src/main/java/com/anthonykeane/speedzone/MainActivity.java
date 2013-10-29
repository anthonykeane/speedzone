package com.anthonykeane.speedzone;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spanned;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.abs;
import static java.util.UUID.randomUUID;





@SuppressWarnings("EmptyMethod")
public class MainActivity extends Activity implements LocationListener {

    //Is different in MainActivity
    private static final boolean bThisIsMainActivity = true;
    private static final String TAG = "SpeedZone::Activity";
    private static final String TAGd = "SpeedZone::Activity_focus";

    private static final int intentSettings = 1;
    private static final int itextViewGPSlost = R.id.textViewGPSlost;


    private static final int MAX_LOG_SIZE = 10;

    // Instantiates a log file utility object, used to log status updates
    private LogFile mLogFile;

    // Store the current request type (ADD or REMOVE)
    private ActivityUtils.REQUEST_TYPE mRequestType;

    /*
     * Holds activity recognition data, in the form of
     * strings that can contain markup
     */
    private ArrayAdapter<Spanned> mStatusAdapter;

    /*
     *  Intent filter for incoming broadcasts from the
     *  IntentService.
     */
    private IntentFilter mBroadcastFilter;

    // Instance of a local broadcast manager
    private LocalBroadcastManager mBroadcastManager;

    // The activity recognition update request object
    private DetectionRequester mDetectionRequester;

    // The activty recognition update removal object
    private DetectionRemover mDetectionRemover;
    private boolean Floating = true;


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


    private final Time now = new Time();
    private final Time tLast = new Time();

    @SuppressWarnings("All")
    private static boolean isRunning;
    //private static final int intentTTS = 3;
    private String ttsSalute;

    private SharedPreferences appSharedPrefs;

    //GPS delay stuff

    private Location locCurrent = new Location(""); // GPS right now
    private Location locLastCallPOI = new Location("");
    private final Location locNextSpeedChange = new Location("");
    private Location locLastCallSchoolZone = new Location("");

    private TextToSpeech mTts;

    private static final int delayBetweenGPS_Records = 60000;    //every 500mS log Geo date in Queue.
    private static final long minTime = 1000;                   // don't update GPS if time < mS
    private static final float minDistanceGPS = 10;              // don't update GPS if distance < Meters

    private final Handler handler = new Handler();                // used for timers

    // public final LocListener gpsListener = new LocListener();    // used by GPS

    private int iNeedToResetDisplay = 0;

    private final AsyncHttpClient client = new AsyncHttpClient();
    private final RequestParams HTTPrp = new RequestParams();
    private final RequestParams HTTPrp2 = new RequestParams();

    private JSONObject jHereResult = new JSONObject();
    private JSONObject jThereResult = new JSONObject();

    private int DistanceToNextSpeedChange = 0;            //any BIG number or zero
    private int DistanceToPOI = 0;
    private int DistanceToSZ = 0;
    private int iSecondsToSpeedChange = 0;
    private static String sUUID = "";

    private static LocationManager locManager;
    private View vImageButton;
    private View vErrorButton;
    private View vImageBtnSmall;
    private View vImageViewDebug;
    private View vImageViewTimeout;
    private View vImageAlert;
    private View vImageSZAlert;

    private static final int itextView = R.id.textView;
    private static final int itextView2 = R.id.textView2;


    private int iSpeed = 50;
    private int fFiveValAvgSpeed = 60;

    //private Location me = new Location("");
    private Location poiSZ = new Location("");
    private final Location poi = new Location("");
    //private static Context context;

    //Flags
    private boolean bZoneError = false;
    private boolean bDebug = false;
    private boolean bAnnoy = false;
    private int iNotCommsLockedOut = 0;                   //Lock out comms until last request is serviced
    private boolean bCommsTimedOut = false;
    private boolean bMute = false;
    private final int iDistanceOffset = 0;    // don't think this helps
    private int iDisplayingB = 0;
    private int iDisplayingG = 0;
    private int iDisplayingS = 0;
    private int iLaunchMode = 1;
    private int iAlertMode = 3;
    private static final int iMinAccuracy = 9;
    private int iPOIminDistance = 10;
    private int iSZminDistance = 10;
    private boolean bPhoneActive_Hide;
    private boolean bActivityPowerKey;
    private int iTypeOfPOI;
    private int iWhenPOI;
    private static final float iShowPOIwithin = 200;
    private static final float iShowSZwithin = 50;
    private int iAlertVolume = 100;

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        //Log.i(TAG, "onDestroy  5");
        handler.removeCallbacks(timedGPSqueue);

        try { // Turn Off the GPS
            locManager.removeUpdates(this); // Turn Off the GPS
        } catch (Exception e) {
            //Log.i(TAG, "onDestroy - GPS is already null");
        }
        if (locManager != null) {
            locManager = null;
        }

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

        if (location.hasAccuracy() && location.hasBearing() && location.hasSpeed() && location.getAccuracy() < iMinAccuracy)
        {
            noGPS(false);
            //Log.i(TAG, "onLocationChanged  GOOD");
            Location locLast = locCurrent;


            locCurrent = location;

            callPOI();
            if(POIActive(7)||bDebug) callSchoolZone();

            DistanceToNextSpeedChange = (int) (locCurrent.distanceTo(locNextSpeedChange) - iDistanceOffset);
            if (DistanceToNextSpeedChange < 60) callWebServiceHere();
            updateAlertImage((locCurrent.distanceTo(poi) < iShowPOIwithin) && DistanceToPOI > locCurrent.distanceTo(poi));
            updateSZAlertImage((locCurrent.distanceTo(poiSZ) < iShowSZwithin) && DistanceToSZ > locCurrent.distanceTo(poiSZ));

            DistanceToPOI = (int) (locCurrent.distanceTo(poi) - iDistanceOffset);
            DistanceToSZ = (int) (locCurrent.distanceTo(poiSZ) - iDistanceOffset);


            float fMinUpdateDistance = 30;
            if (iNotCommsLockedOut == 0 && ((abs(locLast.getSpeed() - locCurrent.getSpeed()) > 2.0)
                    || (abs(locLast.getBearing() - locCurrent.getBearing()) > 7.0)
                    || (locLast.distanceTo(locCurrent) > fMinUpdateDistance))) {
                callWebServiceHere();
            }
        } else {
            //Log.i(TAG, "onLocationChanged  BAD");
        }
        noGPS(!(location.hasAccuracy() && location.getAccuracy() < iMinAccuracy));
        updateDebugText();

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
        setGraphicBtnV(vImageButton, tmp, false);
        if (bThisIsMainActivity) setGraphicBtnV(vImageBtnSmall, tmp, true);
        DistanceToNextSpeedChange = tmp;
        iSpeed = tmp;
    }

    void setGraphicBtnV(View x, int iSpeed, boolean bSmall) {


        ImageButton img = (ImageButton) x;


        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((!bSmall && bThisIsMainActivity) && (iDisplayingB != iSpeed)) {
            iDisplayingB = iSpeed;
            switch (iSpeed) {
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
            //noinspection ConstantConditions
            img.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((iDisplayingS != iSpeed) && (bSmall == bThisIsMainActivity) && (locCurrent.getAccuracy() <= iMinAccuracy) && (locCurrent.hasAccuracy())) {
            iDisplayingS = iSpeed;
            switch (iSpeed) {
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
            //noinspection ConstantConditions
            img.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
        }


        //noinspection PointlessBooleanExpression,ConstantConditions
        if ((iDisplayingG != iSpeed) && (bSmall == bThisIsMainActivity) && ((locCurrent.getAccuracy() > iMinAccuracy) || (!locCurrent.hasAccuracy()))) {

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

            //noinspection ConstantConditions
            img.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
        }

    }


    private void callWebServiceHere() {
        //Log.i(TAG, "callWebServiceHere  " +locCurrent );
        if (locCurrent.hasAccuracy())
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


            //Log.i(TAG, "callWebServiceHere  "+ HTTPrp);


            //todo

//            if (   ((locCurrent.getAccuracy()>=iMinAccuracy) || (!locCurrent.hasAccuracy()))  && bDebug)
//            {
//                HTTPrp.put("lat", "-33.71013");
//                HTTPrp.put("lon", "150.94951");
//                HTTPrp.put("ber", "100");
//                HTTPrp.put("speed", "99");
//                HTTPrp.put("UUID", "test-" + sUUID);
//                HTTPrp.put("When", now.format("%Y-%m-%d %H:%M:%S"));
//            }

            //Toast.makeText(this, String.valueOf(LocListener.getLat()), Toast.LENGTH_SHORT).show();
            // todo if ((locCurrent.getAccuracy()<15) && (locCurrent.getAccuracy()!=0.0)  || bDebug)
            {

                if (iNotCommsLockedOut == 0) {
                    client.get(getString(R.string.MyDbWeb), HTTPrp, new JsonHttpResponseHandler() {

                        @Override
                        public void onFailure(Throwable e, JSONObject errorResponse) {
                            System.out.println(e);
                            //Log.i(TAG, "onFailure MyDbWeb");
                            bCommsTimedOut = false;
                            //Clear the display if we don't know the value
                            // Skip is too slow to matter
                            if (locCurrent.getSpeed() >= 7) {
                                NeedToResetDisplay();
                            }
                        }

                        @Override
                        public void onSuccess(JSONObject response) {
                            bCommsTimedOut = false;
                            //Log.i(TAG, "           onSuccess MyDbWeb ");
                            jHereResult = response;
                            try {
                                doStuff();
                                //Log.i(TAG, "onSuccess - reSpeedLimit " + jHereResult.getInt("reSpeedLimit"));

                                // if changing speed zone Alert but only if your speed is > than posted
                                AlertAnnounce();


                                iSpeed = jHereResult.getInt("reSpeedLimit");
                                setGraphicBtnV(vImageButton, iSpeed, false);
                                //Toast.makeText(getApplicationContext(), iSecondsToSpeedChange, Toast.LENGTH_SHORT).show();
                                //Log.i(TAG, "onSuccess  " + iSecondsToSpeedChange + " iSecondsToSpeedChange ");

                                MyNextWebService();



                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onStart() {
                            // Completed the request (either success or failure)
                            //toggleRadioButton();
                            //Log.i(TAG, "onStart  MyDbWeb");
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
                            //Log.i(TAG, "                       onFinish MyDbWeb ");
                        }
                    });
                }

            }
        }
    }

    private void AlertAnnounce() {
        int SpeedLimit = 0;
        int intCurrentSpeeed = (int) (locCurrent.getSpeed() * 3.6);
        try { SpeedLimit = jHereResult.getInt("reSpeedLimit");} catch (JSONException e) {e.printStackTrace();}
        Log.i(TAG, "AlertAnnounce ");

        if ((tLast.toMillis(true) + 20000) > now.toMillis(true))
        {
            tLast.setToNow();


            if ((!bMute) && (SpeedLimit != 0) && ((iSpeed != SpeedLimit) || bAnnoy) ) {

                if ((intCurrentSpeeed > SpeedLimit) || (iSpeed != SpeedLimit))
                {
                    mTts.speak(getString(R.string.SpeakAlertSpeedChange) + String.valueOf(SpeedLimit), TextToSpeech.QUEUE_ADD, null);
                }
                // don't get confused....this code iterates through all cases , the breaks are INSIDE the IF statement.
                switch (iAlertMode){
                    case 0:

                    case 1:
                        if (intCurrentSpeeed > (SpeedLimit) && intCurrentSpeeed < (SpeedLimit + 3)) {
                            mTts.speak(getString(R.string.SpeakAlertSpeedChangeSpeeding), TextToSpeech.QUEUE_ADD, null);
                            break;
                        }
                        if (intCurrentSpeeed >= (SpeedLimit + 3) && intCurrentSpeeed < (SpeedLimit + 10)) {
                            mTts.speak(getString(R.string.SpeakAlertSpeed1point), TextToSpeech.QUEUE_ADD, null);
                            break;
                        }

                    case 3:
                        if (intCurrentSpeeed >= (SpeedLimit + 10) && intCurrentSpeeed < (SpeedLimit + 20) ) {
                            mTts.speak(getString(R.string.SpeakAlertSpeed3points), TextToSpeech.QUEUE_ADD, null);
                            break;
                        }

                    case 4:
                        if (intCurrentSpeeed >= (SpeedLimit + 20) && intCurrentSpeeed < (SpeedLimit + 30) ) {
                            mTts.speak(getString(R.string.SpeakAlertSpeed4points), TextToSpeech.QUEUE_ADD, null);
                            break;
                        }

                    case 5:
                        if (intCurrentSpeeed >= (SpeedLimit + 30) && intCurrentSpeeed < (SpeedLimit + 45) ) {
                            mTts.speak(getString(R.string.SpeakAlertSpeed5points), TextToSpeech.QUEUE_ADD, null);
                            break;
                        }

                    case 6:
                        if (intCurrentSpeeed >= (SpeedLimit + 45)) {
                            mTts.speak(getString(R.string.SpeakAlertSpeed6points), TextToSpeech.QUEUE_ADD, null);
                            break;
                        }

                }

                /*
                if (iAlertMode <= 0) {
                    mTts.speak(getString(R.string.SpeakAlertSpeedChange) + String.valueOf(SpeedLimit), TextToSpeech.QUEUE_ADD, null);
                }
                if (intCurrentSpeeed > (SpeedLimit) && intCurrentSpeeed < (SpeedLimit + 3) && (iAlertMode <= 1)) {
                    mTts.speak(getString(R.string.SpeakAlertSpeedChangeSpeeding), TextToSpeech.QUEUE_ADD, null);
                }
                if (intCurrentSpeeed >= (SpeedLimit + 3) && intCurrentSpeeed < (SpeedLimit + 10) && (iAlertMode <= 2)) {
                    mTts.speak(getString(R.string.SpeakAlertSpeed1point), TextToSpeech.QUEUE_ADD, null);
                }
                if (intCurrentSpeeed >= (SpeedLimit + 10) && intCurrentSpeeed < (SpeedLimit + 20) && (iAlertMode <= 3)) {
                    mTts.speak(getString(R.string.SpeakAlertSpeed3points), TextToSpeech.QUEUE_ADD, null);
                }
                if (intCurrentSpeeed >= (SpeedLimit + 20) && intCurrentSpeeed < (SpeedLimit + 30) && (iAlertMode <= 4)) {
                    mTts.speak(getString(R.string.SpeakAlertSpeed4points), TextToSpeech.QUEUE_ADD, null);
                }
                if (intCurrentSpeeed >= (SpeedLimit + 30) && intCurrentSpeeed < (SpeedLimit + 45) && (iAlertMode <= 5)) {
                    mTts.speak(getString(R.string.SpeakAlertSpeed5points), TextToSpeech.QUEUE_ADD, null);
                }
                if (intCurrentSpeeed >= (SpeedLimit + 45) && (iAlertMode <= 6)) {
                    mTts.speak(getString(R.string.SpeakAlertSpeed6points), TextToSpeech.QUEUE_ADD, null);
                }
                */
                DistanceToNextSpeedChange = 0;
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

            if ((iSecondsToSpeedChange < 60) || (DistanceToNextSpeedChange < 200) || (DistanceToNextSpeedChange == 0))                         //refresh when close only
            {
                //Log.i(TAG, "onSuccess  Getting Speed change");
                client.get(getString(R.string.MyNextWeb), HTTPrp2, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        //Log.i(TAGd, "onSuccess MyNextWeb  ");
                        bCommsTimedOut = false;
                        jThereResult = response;
                        doStuff();
                    }

                    @Override
                    public void onFailure(Throwable e, JSONObject errorResponse) {

                        //Log.i(TAGd, "onFailure  MyNextWeb");
                        DistanceToNextSpeedChange = 0;

                    }


                    @Override
                    public void onFinish() {
                        // Completed the request (either success or failure)

                        updateTimeoutIcon();
                        //Log.i(TAGd, "onFinish  MyNextWeb");
                    }
                });
            }
        } catch (JSONException e) {
            //Log.i(TAG, "MyNextWebService  NO HereResult");
        }
    }

    private void doStuff() {
        try {
            //me.setLatitude(locCurrent.getLatitude());
            //me.setLongitude(locCurrent.getLongitude());
            locNextSpeedChange.setLatitude(jThereResult.getDouble("reLat"));
            locNextSpeedChange.setLongitude(jThereResult.getDouble("reLon"));


            //todo is this line needed?
            //if (bThisIsMainActivity)
                if (!bCommsTimedOut)
                    setGraphicBtnV(vImageBtnSmall, jThereResult.getInt("reSpeedLimit"), true);

                fFiveValAvgSpeed = (int) (((fFiveValAvgSpeed * 4) + locCurrent.getSpeed()) / 5);
                iSecondsToSpeedChange = (DistanceToNextSpeedChange / (fFiveValAvgSpeed + 1));
                //updateDebugText();
       } catch (JSONException e) {
            //e.printStackTrace();
            //Log.i(TAG, "doStuff - no value for Lat");
        }
    }

    private String oneTo1(String x) {
        if (x.equals("\u0001")) {
            return "1";
        } else {
            return "0";
        }
    }


    void createTextToSpeech(final Context context, final Locale locale) {
        mTts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Locale defaultOrPassedIn = locale;
                    if (locale == null) {
                        defaultOrPassedIn = Locale.getDefault();
                    }
                    // check if language is available
                    switch (mTts.isLanguageAvailable(defaultOrPassedIn)) {
                        case TextToSpeech.LANG_AVAILABLE:
                        case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                        case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                            //Log.d(TAG, "SUPPORTED");
                            mTts.setLanguage(locale);
                            //pass the tts back to the main
                            //activity for use
                            break;
                        case TextToSpeech.LANG_MISSING_DATA:
                            //Log.d(TAG, "MISSING_DATA");
                            //Log.d(TAG, "require data...");
                            Intent installIntent = new Intent();
                            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                            context.startActivity(installIntent);
                            break;
                        case TextToSpeech.LANG_NOT_SUPPORTED:
                            //Log.d(TAG, "NOT SUPPORTED");
                            break;
                    }
                }
                try {
                    if (!bMute) mTts.speak(ttsSalute, TextToSpeech.QUEUE_ADD, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private final Runnable timedGPSqueue;

    {
        timedGPSqueue = new Runnable() {
            @Override
            public void run() {
                if(bAnnoy){
                    Log.i(TAG, "Annoy ");
                    AlertAnnounce();
                }
                noGPS(!(locCurrent.hasAccuracy()));
                if (iNotCommsLockedOut < 3) {    // DON'T LET THE COMMS QUEUE GET TO BUG
                    callWebServiceHere();
                }
                handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //repeating so needed

                //Log.i(TAG, "run  REPEAT TIMER  " + locCurrent.getAccuracy());
            }
        };
    }



    void NeedToResetDisplay() {
        iNeedToResetDisplay++;
        //Log.i(TAG, "NeedToResetDisplay  " + iNeedToResetDisplay);
        if (iNeedToResetDisplay > 1) {
            setDisplay(50);
            iNeedToResetDisplay = 0;

        }
    }

    private boolean POIActive(int iWhenPOI) {

        Calendar cal = Calendar.getInstance();
//
//        int millisecond = cal.get(Calendar.MILLISECOND);
//        int second = cal.get(Calendar.SECOND);
//        int minute = cal.get(Calendar.MINUTE);
//        //12 hour format
//        int hour = cal.get(Calendar.HOUR);
//        //24 hour format
//        int hourofday = cal.get(Calendar.HOUR_OF_DAY);
//
//        //Same goes for the date, as follows:
//
//        int dayofyear = cal.get(Calendar.DAY_OF_YEAR);
//        int year = cal.get(Calendar.YEAR);
//        int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
//        int dayofmonth = cal.get(Calendar.DAY_OF_MONTH);
//


        //check day of week
        switch (iWhenPOI) {
            case 0:
                return true;
            case 1:
            case 2:
            case 3:
            case 5:
            case 7:
                if ((cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) || (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)) {
                    return false;
                }
        }
        // check time
        switch (iWhenPOI) {

            case 1:
                return (cal.get(Calendar.HOUR) >=  6 && cal.get(Calendar.HOUR) < 10);

            case 2:
                return ((cal.get(Calendar.HOUR) >=  6 && cal.get(Calendar.HOUR) < 10)
                     || (cal.get(Calendar.HOUR) >= 15 && cal.get(Calendar.HOUR) < 19));

            case 3:
                return ((cal.get(Calendar.HOUR) >= 6  && cal.get(Calendar.HOUR) < 10)
                     || (cal.get(Calendar.HOUR) >= 15 && cal.get(Calendar.HOUR) < 20));

            case 4:
            case 5:
                return (cal.get(Calendar.HOUR) >= 6 && cal.get(Calendar.HOUR) < 20);

            case 6:
                return (cal.get(Calendar.HOUR) >= 15 && cal.get(Calendar.HOUR) < 19);

            case 7:
                return ((cal.get(Calendar.HOUR) ==  8)
                     || (cal.get(Calendar.HOUR) == 9) && (cal.get(Calendar.MINUTE) <= 30)
                     || (cal.get(Calendar.HOUR) ==  15)
                     || (cal.get(Calendar.HOUR) == 14) && (cal.get(Calendar.MINUTE) <= 30)
                );

        }


        return true;

    }

    private void callSchoolZone() {

        //locLastCallSchoolZone
        // iSZminDistance
        // DistanceToPOI


        // iTypeOfPOI
        // iWhenPOI
        //

        if ((locCurrent.distanceTo(locLastCallSchoolZone) > iSZminDistance) || (!locLastCallSchoolZone.hasAccuracy())) // call this is Xm distance of not init-ed
        {
            //Log.i(TAG, "callPOI  ");
            locLastCallSchoolZone = locCurrent;
            if (iSZminDistance > 100) iSZminDistance = 100;

            RequestParams HTTPrpA = new RequestParams();
            HTTPrpA.put("lat", String.valueOf(locCurrent.getLatitude()));
            HTTPrpA.put("lon", String.valueOf(locCurrent.getLongitude()));

            client.get(getString(R.string.mydbSchoolZone), HTTPrpA, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        poiSZ.setLatitude(response.getDouble("SchLat"));
                        poiSZ.setLongitude(response.getDouble("SchLon"));
                        poiSZ.setAccuracy(5);
                        DistanceToSZ = (int) (locCurrent.distanceTo(poiSZ) - iDistanceOffset);
                        //iTypeOfPOI = 8; //response.getInt("poiType");
                        //iWhenPOI = 7; //response.getInt("poiWhen");
                        iSZminDistance = (int) (DistanceToSZ * 0.8);
                        //Log.i(TAG, "callPOI onSuccess  " + poi.getLatitude() + " " + poi.getLongitude());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void onStart() {
                    // Completed the request (either success or failure)
                    toggleRadioButton();
                    //Log.i(TAG, "callPOI onStart  ");

                }

                @Override
                public void onFinish() {
                    // Completed the request (either success or failure)
                    toggleRadioButton();
                    //Log.i(TAGd, "CallPOI onFinish  ");
                }

                @Override
                public void onFailure(Throwable e, JSONObject errorResponse) {

                    //Log.i(TAGd, "CallPOI onFailure   ");
                    DistanceToSZ = 0;
                    // Completed the request (either success or failure)
                    iSZminDistance = 100;
                    updateAlertImage(false);
                }

            });


        }

    }


    private void callPOI() {

        if ((locCurrent.distanceTo(locLastCallPOI) > iPOIminDistance) || (!locLastCallPOI.hasAccuracy())) // call this is Xm distance of not init-ed
        {
            //Log.i(TAG, "callPOI  ");
            locLastCallPOI = locCurrent;
            if (iPOIminDistance > 1000) iPOIminDistance = 1000;

            RequestParams HTTPrpA = new RequestParams();
            HTTPrpA.put("lat", String.valueOf(locCurrent.getLatitude()));
            HTTPrpA.put("lon", String.valueOf(locCurrent.getLongitude()));

            client.get(getString(R.string.MyPOIWeb), HTTPrpA, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        poi.setLatitude(response.getDouble("poiLat"));
                        poi.setLongitude(response.getDouble("poiLon"));
                        poi.setAccuracy(5);
                        DistanceToPOI = (int) (locCurrent.distanceTo(poi) - iDistanceOffset);
                        iTypeOfPOI = response.getInt("poiType");
                        iWhenPOI = response.getInt("poiWhen");
                        iPOIminDistance = (int) (DistanceToPOI * 0.8);
                        //Log.i(TAG, "callPOI onSuccess  " + poi.getLatitude() + " " + poi.getLongitude());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void onStart() {
                    // Completed the request (either success or failure)
                    toggleRadioButton();
                    //Log.i(TAG, "callPOI onStart  ");

                }

                @Override
                public void onFinish() {
                    // Completed the request (either success or failure)
                    toggleRadioButton();
                    //Log.i(TAGd, "CallPOI onFinish  ");
                }

                @Override
                public void onFailure(Throwable e, JSONObject errorResponse) {

                    //Log.i(TAGd, "CallPOI onFailure   ");
                    DistanceToPOI = 0;
                    // Completed the request (either success or failure)
                    iPOIminDistance = 1000;
                    updateAlertImage(false);
                }
            });
        }
    }


    private void updateAlertImage(boolean bShow) {
        if (bShow) {
            if ((vImageAlert.getVisibility() != View.VISIBLE))
            {
                float poiBer = abs(locCurrent.bearingTo(poi));
                float curBer = abs(locCurrent.getBearing());
                if(poiBer>180){poiBer=abs(poiBer-360);}
                if(curBer>180){curBer=abs(curBer-360);}
                Log.i(TAG, "Bearing" + (abs(curBer-poiBer)) +  " " + curBer + " " + poiBer );
//                try {
//                    setDebugText(itextView2, "Bearing " + (abs(curBer-poiBer)) +  " c:" + curBer + " p:" + poiBer);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
                if(  (abs(curBer-poiBer)<10))
                {   //noinspection ConstantConditions
                    vImageAlert.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                    vImageAlert.setVisibility(View.VISIBLE);
                    //iTypeOfPOI and iWhenPOI comes from callPOI() return
                    // if Speed Camera etc are active at this time of day then ...
                    if (POIActive(iWhenPOI)) {
                        String poiAlertMessage = getResources().getStringArray(R.array.poiTypeArray)[iTypeOfPOI];
                        mTts.speak(poiAlertMessage, TextToSpeech.QUEUE_FLUSH, null);
                    }
                    else
                    {
                        if(bDebug) mTts.speak("POI not Active", TextToSpeech.QUEUE_ADD, null);

                    }
                }
                else      //todo skipping
                {if(bDebug) mTts.speak("skipping", TextToSpeech.QUEUE_ADD, null); }
            }
        } else {
            if (vImageAlert.getVisibility() != View.GONE)
            {
                //noinspection ConstantConditions
                vImageAlert.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                vImageAlert.setVisibility(View.GONE);
            }
        }
    }









    private void updateSZAlertImage(boolean bShow) {
        if (bShow) {
            if ((vImageSZAlert.getVisibility() != View.VISIBLE))
            {
                float poiBer = abs(locCurrent.bearingTo(poiSZ));
                float curBer = abs(locCurrent.getBearing());
                if(poiBer>180){poiBer=abs(poiBer-360);}
                if(curBer>180){curBer=abs(curBer-360);}
                Log.i(TAG, "Bearing " + (abs(curBer-poiBer)) +  " " + curBer + " " + poiBer );
//                try {
//                    setDebugText(itextView2, "Bearing" + (abs(curBer-poiBer)) +  " c:" + curBer + " p:" + poiBer);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
                if(  (abs(curBer-poiBer)<20))
                {

                    //noinspection ConstantConditions
                    vImageSZAlert.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                    vImageSZAlert.setVisibility(View.VISIBLE);
                    //iTypeOfPOI and iWhenPOI comes from callPOI() return
                    // if Speed Camera etc are active at this time of day then ...
                    if (POIActive(7))
                    {
                        String poiAlertMessage = getResources().getStringArray(R.array.poiTypeArray)[8];
                        mTts.speak(poiAlertMessage, TextToSpeech.QUEUE_ADD, null);

                    }
                    else
                    {
                       if(bDebug) mTts.speak("School Zone but not School time", TextToSpeech.QUEUE_ADD, null);

                    }
                }
//                else
//                {                    //todo skipping
//                    mTts.speak("skipping", TextToSpeech.QUEUE_ADD, null);
//                }
            }
        } else {
            if (vImageSZAlert.getVisibility() != View.GONE)
            {
                //noinspection ConstantConditions
                vImageSZAlert.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                vImageSZAlert.setVisibility(View.GONE);
            }
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


    private void RetreiveSettings() {


        sUUID = appSharedPrefs.getString(getString(R.string.myUUID), "");

        if (sUUID.equals("")) {
            sUUID = randomUUID().toString();
            appSharedPrefs.edit().putString(getString(R.string.myUUID), sUUID).commit();
        }
//        Map<String, ?> xx = appSharedPrefs.getAll();

        bMute = !(appSharedPrefs.getBoolean(getString(R.string.settings_soundKey), true));  // Active Low
        bDebug = appSharedPrefs.getBoolean(getString(R.string.settings_debugKey), false);
        bAnnoy = appSharedPrefs.getBoolean(getString(R.string.settings_AnnoyKey), true);
//        alertOnGreenLightEnabled = appSharedPrefs.getBoolean(getString(R.string.settings_alertOnGreenLightEnabledKey), false);
//        userEmail = appSharedPrefs.getString(getString(R.string.settings_userEmailKey), "");
        ttsSalute = appSharedPrefs.getString(getString(R.string.settings_ttsSaluteKey), getString(R.string.ttsSalute));
//        ttsSignFound = appSharedPrefs.getString(getString(R.string.settings_ttsSignFoundKey), getString(R.string.ttsSignFound));
//        bExperimental = appSharedPrefs.getBoolean(getString(R.string.settings_bExperimentalKey), false);
//        debugVerbosity = Integer.parseInt(appSharedPrefs.getString(getString(R.string.settings_debugVerbosityKey), "0"));
        bActivityPowerKey = appSharedPrefs.getBoolean(getString(R.string.settings_activityPowerKey), true);

        iLaunchMode  = Integer.parseInt(appSharedPrefs.getString(getString(R.string.settings_launchTypeKey), "1"));
        iAlertMode   = Integer.parseInt(appSharedPrefs.getString(getString(R.string.settings_Alert_Key), "0"));
        iAlertVolume = appSharedPrefs.getInt(getString(R.string.settings_VolumeKey), 100);

        if (appSharedPrefs.getBoolean(getString(R.string.settings_activityServicesKey), false)) {
            onStartUpdates();
        } else {
            onStopUpdates();
        }
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
        am.setStreamVolume(am.STREAM_MUSIC, (iAlertVolume*amStreamMusicMaxVol)/100,0 );


        updateDebugIcon();
    }








    private void noGPS(boolean bNoGps) {

        TextView textView = (TextView) findViewById(itextViewGPSlost);
        if (bNoGps) {
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.INVISIBLE);
        }
    }


    private void toggleRadioButton() {

        RadioButton b = (RadioButton) (findViewById(R.id.radioButton));
        b.setChecked(!b.isChecked());
    }

    private void updateTimeoutIcon() {
        if (bCommsTimedOut) {
            vImageViewTimeout.setVisibility(View.VISIBLE);
        } else {
            vImageViewTimeout.setVisibility(View.INVISIBLE);
        }
    }



    private void updateDebugIcon() {
        if (bDebug) {
            vImageViewDebug.setVisibility(View.VISIBLE);
        } else {
            vImageViewDebug.setVisibility(View.INVISIBLE);
        }
    }


    private void updateDebugText() {

        ProgressBar pBap = (ProgressBar) findViewById(R.id.progressBar);

        DistanceToPOI = (int) locCurrent.distanceTo(poi);

        if (DistanceToNextSpeedChange > DistanceToPOI) {
            pBap.setProgress(DistanceToPOI);
            pBap.setSecondaryProgress(DistanceToNextSpeedChange);

        } else {
            pBap.setProgress(DistanceToNextSpeedChange);
            pBap.setSecondaryProgress(DistanceToPOI);
        }
        try {
            if (bDebug) {
                String x = "Speed Change in  " + DistanceToNextSpeedChange + "m"
                        + "\ncallPOI was called " + (int) locCurrent.distanceTo(locLastCallPOI) + "m ago"
                        + "\nPOI was Detected " + DistanceToPOI + "m Away"
                        + "\nA:" + locCurrent.getAccuracy() + "\tH:" + locCurrent.hasAccuracy()
                        + "\tmin:" + iPOIminDistance
//                        + "\nLat:" + locCurrent.getLatitude()
//                        + "\nLon:" + locCurrent.getLongitude()
                        + "\nSpe:" + locCurrent.getSpeed()*3.6;
                setDebugText(itextView, x);

            } else {
                setDebugText(itextView, "");
                //setDebugText(itextView2, "");
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
//        Debug.startMethodTracing("anthony.trace");
        //client.setTimeout(2000);
//
//
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());


        vImageButton = findViewById(R.id.imageButton);
        vErrorButton = findViewById(R.id.imageButtonError);
        vImageBtnSmall = findViewById(R.id.imageBtnSmall);
        vImageViewDebug = findViewById(R.id.imageViewDebug);
        vImageViewTimeout = findViewById(R.id.imageViewTimeout);
        vImageAlert = findViewById(R.id.imageAlert);
        vImageSZAlert = findViewById(R.id.imageSZAlert);
        // Turn on the GPS.     set up GPS
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
                createNotification(vErrorButton);
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


//        //5000 is the starting number (in milliseconds)
//        //1000 is the number to count down each time (in milliseconds)
//          MyCount counter = new MyCount(5000,1000);
//          todo        counter.start();


        // Get a handle to the activity update list
        ListView mStatusListView = (ListView) findViewById(R.id.log_listview);

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
        // if launch
        if(LaunchOrKill()); // needs a preference so must be AFTER RetreiveSettings()
        {
            EasyTracker.getInstance(this).activityStart(this);  // Add this method.

            createTextToSpeech(this, Locale.getDefault());

            new WhatsNewScreen(this).show();

            callWebServiceHere();
            updateTimeoutIcon();
            updateDebugIcon();
            // thanks to http://stackoverflow.com/a/3104265
            SharedPreferences.OnSharedPreferenceChangeListener splistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    // Implementation
                    RetreiveSettings();
                    //Log.i(TAG, "onSharedPreferenceChanged  ");
                }
            };
            appSharedPrefs.registerOnSharedPreferenceChangeListener(splistener);
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
public void onStart() {
    super.onStart();

}

    @Override
    public void onStop() {
        super.onStop();
        if(!isMyServiceRunning())
        {
            EasyTracker.getInstance(this).activityStop(this);  // Add this method.
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

//
//            // Clear the log display and remove the log files
//            case R.id.menu_item_clearlog:
//                // Clear the list adapter
//                mStatusAdapter.clear();
//
//                // Update the ListView from the empty adapter
//                mStatusAdapter.notifyDataSetChanged();
//
//                // Remove log files
//                if (!mLogFile.removeLogFiles()) {
//                    //Log.e(ActivityUtils.APPTAG, getString(R.string.log_file_deletion_error));
//
//                    // Display the results to the user
//                } else {
//
//                    Toast.makeText(
//                            this,
//                            R.string.logs_deleted,
//                            Toast.LENGTH_LONG).show();
//                }
//                // Continue by passing true to the menu handler
//                return true;
//
//            // Display the update log
//            case R.id.menu_item_showlog:
//
//                // Update the ListView from log files
//                updateActivityHistory();
//
//                // Continue by passing true to the menu handler
//                return true;

            case R.id.menu_float:

                //if(isMyServiceRunning())
            {

                //iLaunchMode = 2;
                callFloat();
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

    private void callFloat() {
        Floating = true;
        Intent intent;
        //Bundle extras;
        intent = new Intent(MainActivity.this, ChatHeadService.class);
        intent.putExtra("TheOK", true);
        intent.putExtra("sUUID",sUUID);
        //Log.i(TAG, "bDebug is  " + bDebug);
        intent.putExtra("bDebug", bDebug);
        intent.putExtra("bCommsTimedOut", bCommsTimedOut);
        //intent.putExtra("iSpeed",iSpeed);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        handler.removeCallbacks(timedGPSqueue);
        moveTaskToBack(true);
        stopService(intent);
        startService(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        //Log.i(TAG, "onPause  ");
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Stop the GPS listener
        handler.removeCallbacks(timedGPSqueue);
        locManager.removeUpdates(this);


        // Stop listening to broadcasts when the Activity isn't visible.
        mBroadcastManager.unregisterReceiver(updateListReceiver);
        //Debug.stopMethodTracing();

    }

    @Override
    public void onResume() {
        super.onResume();


        LaunchOrKill(); // needs a preference so must be AFTER RetreiveSettings()


        //Log.i(TAG, "onResume  ");

        handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);
        //Log.i(TAG, "onResume  START TIMER");
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistanceGPS, this);


        // Register the broadcast receiver
        mBroadcastManager.registerReceiver(
                updateListReceiver,
                mBroadcastFilter);


    }

    private boolean LaunchOrKill() {

        // if not called by Float
        if(!didFloatCallNotmal())
        {

            // Already Running?
            if (isMyServiceRunning())
            {
                Log.i(TAG, "isMyServiceRunning   ");
                moveTaskToBack(isTaskRoot());
                return false;
                //callFloat();
            }

            // Are we Connected to external power?
            Intent inPower = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int status = inPower.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

            if (!(isCharging || bActivityPowerKey ))
            {
                Log.i(TAG, "isCharging !bActivityPowerKey     ");

                now.setToNow();


                final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(2);


                for (ActivityManager.RunningTaskInfo recentTask : recentTasks) {
                    bPhoneActive_Hide = recentTask.baseActivity.toShortString().equals("{com.android.contacts/com.android.contacts.activities.DialtactsActivity}")
                            || recentTask.baseActivity.toShortString().equals("{com.android.contacts/com.android.contacts.activities.PeopleActivity}")
                            || recentTask.baseActivity.toShortString().equals("{com.android.phone/com.android.phone.InCallScreen}");
                }

                if(bPhoneActive_Hide)
                {
                    Log.i(TAG, "ON the PHONE   ");
                    moveTaskToBack(isTaskRoot());
                    return false;
                    //callFloat();
                }

                if ((tLast.toMillis(true) + 4000) < now.toMillis(true))
                {

                    if (!bPhoneActive_Hide)
                    {
                        LayoutInflater inflater = getLayoutInflater();
                        View layout = inflater.inflate(R.layout.toast,
                                (ViewGroup) findViewById(R.id.toast_layout_root));

                        assert layout != null;
                        ImageView image = (ImageView) layout.findViewById(R.id.image);
                        image.setImageResource(R.drawable.ic_launcher);
                        TextView text = (TextView) layout.findViewById(R.id.text);
                        text.setText("CLICK THE ICON AGAIN NOW\nnot this frame!.\nSee Power in Settings");

                        Toast toast = new Toast(getApplicationContext());
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.setView(layout);
                        toast.show();

                        tLast.setToNow();
                    }
                    //Toast.makeText(this, "<CENTER>'SpeedZone NSW'\n\nDisabled while on battery, \nConnect Power to Launch or\n\n CLICK AGAIN \n\nthen See Settings</CENTER>" , Toast.LENGTH_LONG).show();
                }
//                else
//                {
//                    if (iLaunchMode == 2) {
//                        Log.i(TAG, "iLaunchMode      ");
//                        callFloat();
//                    }
//
//                }
                Floating = false;
            }
            else
            {
                if (iLaunchMode == 2 && Floating) {
                    Log.i(TAG, "iLaunchMode      ");
                    callFloat();
                }

            }
            return false;
        }
        return true;
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean ServiceNotConnected() {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            //Log.d(ActivityUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return false;

            // Google Play services was not available for some reason
        } else {
            return true;
        }
    }

    /**
     * Respond to "Start" button by requesting activity recognition
     * updates.
     */
    void onStartUpdates() {

        // Check for Google Play services
        if (ServiceNotConnected()) {

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
    void onStopUpdates() {

        // Check for Google Play services
        if (ServiceNotConnected()) {

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
                    //Log.e(ActivityUtils.APPTAG, getString(R.string.log_file_deletion_error));
                }
            }

            // Trigger the adapter to update the display
            mStatusAdapter.notifyDataSetChanged();

            // If an error occurs while reading the history file
        } catch (IOException e) {
            //Log.e(ActivityUtils.APPTAG, e.getMessage(), e);
        }
    }

    /**
     * Broadcast receiver that receives activity update intents
     * It checks to see if the ListView contains items. If it
     * doesn't, it pulls in history.
     * This receiver is local only. It can't read broadcast Intents from other apps.
     */
    private final BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            /*
             * When an Intent is received from the update listener IntentService, update
             * the displayed //Log.
             */
            mTts.speak("update ", TextToSpeech.QUEUE_ADD, null);

            updateActivityHistory();
        }
    };

    private boolean isMyServiceRunning() {


//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (ChatHeadService.class.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
        return ChatHeadService.isRunning();
    }

    private boolean didFloatCallNotmal() {


//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (ChatHeadService.class.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
        return ChatHeadService.didFloatCallNormal();
    }




    /*
    Dummy in Service
    private void setDebugText(int t, String sdsd){}
    private void setDisplayScale(int t){}
    */

    void removeChatHeads() {
    }

    private void setDebugText(int t, String s) throws JSONException {
        TextView textView = (TextView) findViewById(t);
        textView.setText(s);
    }


    @SuppressWarnings("All")
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
            case ActivityUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to start activity recognition updates
                        if (ActivityUtils.REQUEST_TYPE.ADD == mRequestType) {

                            // Restart the process of requesting activity recognition updates
                            mDetectionRequester.requestUpdates();

                            // If the request was to remove activity recognition updates
                        } else if (ActivityUtils.REQUEST_TYPE.REMOVE == mRequestType) {

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
                        //Log.d(ActivityUtils.APPTAG, getString(R.string.no_resolution));
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                //Log.d(ActivityUtils.APPTAG,getString(R.string.unknown_activity_request_code, requestCode));

                break;


        }
    }


    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void sendFeedback() {
        try {
            @SuppressWarnings("UnusedAssignment") int i = 3 / 0;
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



    static boolean isSDK17() {
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void createNotification(View view) {
        // Prepare intent which is triggered if the
        // notification is selected
        Intent intent = new Intent(this, NotificationReceiverActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Build notification

            Notification.Builder builder = new Notification.Builder(this);
            builder.setContentTitle("Speed Zone");
            builder.setContentText("Error Logged Click to send, swipe to cancel");
            builder.setSmallIcon(R.drawable.ic_launcher);
            if (isSDK17()) {
                builder.setContentIntent(pIntent);
                builder.addAction(R.drawable.stat_notify_email_generic, "Click here to send data", pIntent);

            } else {
                builder.setContentIntent(pIntent);
            }
        Notification noti = builder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Hide the notification after its selected
        noti.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(0, noti);


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






