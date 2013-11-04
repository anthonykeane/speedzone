package com.anthonykeane.speedzone;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
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

import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.abs;

public class ChatHeadService extends Service implements LocationListener {


    // unique to ChatHeadService
    private boolean bYouMovedIt = false;
    private WindowManager windowManager;
    public List<View> chatHeads;
    private LayoutInflater inflater;

//88888888888888888888888888888888888888888888888888888888888888888888888


    //Is different in ChatHeadService
    private static final boolean bThisIsMainActivity = false;
    private static final String TAG = "SpeedZone::Service";
    private static final String TAGd = "SpeedZone::Service_focus";
    private boolean toDIE = false;
    private static boolean didFloatCallNormal;

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


    @SuppressWarnings("All")

    private final Time now = new Time();
    private final Time tLast = new Time();
    private final Time tLast2 = new Time();



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
        EasyTracker.getInstance(this).set("Where","FloatStop");
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
            if(MainActivity.POIActive(7)||bDebug) callSchoolZone();

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
        //if ((iDisplayingS != iSpeed) && (bSmall == bThisIsMainActivity) && (locCurrent.getAccuracy() <= iMinAccuracy) && (locCurrent.hasAccuracy())) {
        if ((iDisplayingS != iSpeed) && (bSmall == bThisIsMainActivity)) {
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
        if ((iDisplayingG != iSpeed) && (bSmall == !bThisIsMainActivity)) {

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
        try { SpeedLimit = jHereResult.getInt("reSpeedLimit");
        Log.i(TAG, "AlertAnnounce ");

        if ((tLast2.toMillis(true) + 20000) > now.toMillis(true))
        {
            tLast2.setToNow();


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
        } catch (JSONException e) {e.printStackTrace();}
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
            if (bThisIsMainActivity){
            if (!bCommsTimedOut)
                setGraphicBtnV(vImageBtnSmall, jThereResult.getInt("reSpeedLimit"), true);
            }
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

/*
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
|| (cal.get(Calendar.HOUR) == 9) && (cal.get(Calendar.MINUTE) >= 30)
|| (cal.get(Calendar.HOUR) ==  15)
|| (cal.get(Calendar.HOUR) == 14) && (cal.get(Calendar.MINUTE) >= 30)
);

}


return true;

}
*/

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
                    if (MainActivity.POIActive(iWhenPOI)) {
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
                    if (MainActivity.POIActive(7))
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

    private void noGPS(boolean bNoGps) {
//
//        try {
//            setGraphicBtnV(vImageButton, jHereResult.getInt("reSpeedLimit"), bNoGps);
//        } catch (JSONException e) {
//            // Log.i(TAG, "noGPS - No value for reSpeedLimit ");
//        }
    }


    private void updateTimeoutIcon() {
        if (bCommsTimedOut) {
            vImageViewTimeout.setVisibility(View.VISIBLE);
        } else {
            vImageViewTimeout.setVisibility(View.GONE);
        }
    }


    private void updateDebugIcon() {
        if (bDebug) {
            vImageViewDebug.setVisibility(View.VISIBLE);
        } else {
            vImageViewDebug.setVisibility(View.GONE);
        }
    }


    private void updateDebugText() {


    }

    @Override
    public void onCreate() {

        EasyTracker.getInstance(this).set("Where","FloatStart");
        super.onCreate();

        // Log.i(TAG, "onCreate  1");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        inflater = LayoutInflater.from(this);
        chatHeads = new ArrayList<View>();
        // Log.i(TAG, "onCreate  ");
        // Retreive Settings
        //RetreiveSettings();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //if(startId ==1)
        isRunning = true;
        // Log.i(TAG, "onStartCommand  "+ isRunning);
        didFloatCallNormal = false;
        {
            toDIE = false;
            final View chatHead = inflater.inflate(R.layout.chat_head, null);
            // Log.i("Local Service", "Received start id " + startId + ": " + intent);
            if (chatHead != null) {
                vImageButton = chatHead.findViewById(R.id.imageButton);
                vErrorButton = chatHead.findViewById(R.id.imageButtonError);
                vImageBtnSmall = chatHead.findViewById(R.id.imageBtnSmall);
                vImageViewDebug = chatHead.findViewById(R.id.imageViewDebug);
                vImageViewTimeout = chatHead.findViewById(R.id.imageViewTimeout);
                vImageAlert = chatHead.findViewById(R.id.imageAlert);
                vImageSZAlert = chatHead.findViewById(R.id.imageSZAlert);
            }


            // Are we charging / charged?
            Intent inPower = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int status = inPower.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            //int iPluggedIn = inPower.getIntExtra("plugged", 0);

            // Turn on the GPS.     set up GPS
            if (locManager == null) {
                locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
                //Start the GPS listener
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistanceGPS, this);
            }


            boolean bOK;

            try {
                bOK = intent.getBooleanExtra("TheOK", false);
                sUUID = intent.getStringExtra("sUUID");
                bDebug = intent.getBooleanExtra("bDebug", false);
                bCommsTimedOut = intent.getBooleanExtra("bCommsTimedOut", false);
                iSpeed = intent.getIntExtra("iSpeed",50);


                // Log.i(TAG, "2.2 bCommsTimedOut is  " + bCommsTimedOut);
                // Log.i(TAG, "2.2 bDebug is  " + bDebug);
                updateTimeoutIcon();
                updateDebugIcon();
                //iSpeed =  intent.getIntExtra("iSpeed", 50);
            } catch (Exception e) {
                e.printStackTrace();
                // Log.i(TAG, "onStartCommand  Exception");
                toDIE = true;
                onDestroy();
                return 0;
            }




            if (bOK) {
                // Log.i(TAG, "onStartCommand  THE OK true");
            } else {
                // Log.i(TAG, "onStartCommand  NOT ***********");
            }


            // got iSpeed above
            setGraphicBtnV(vImageButton, iSpeed, true);


            handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //Start timer
            callWebServiceHere();


            vImageButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    if (!bYouMovedIt) {
                        // Send Error to URL
                        createNotification(vErrorButton);
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
                        // Log.i("Service", "onStart() is called");
                        didFloatCallNormal = true;
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        callIntent.setClass(v.getContext(), MainActivity.class);
                        callIntent.putExtra("float", "2");
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
                            // Log.i(TAG, String.valueOf(didwemove) + "    " + String.valueOf(params.x));


                            bYouMovedIt = ((StrictMath.abs(params.x - didwemove) > 60));
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
                            // Log.i(TAG, String.valueOf(didwemove) + "    " + String.valueOf(params.x));
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
        // Log.i(TAGd, "gpsUpdated  ");
    }


    // Dummy in Service
    private void setDebugText(int t, String sdsd) {
    }


    private void setDisplayScale(float f) {
    }


    private void SaveSetting(String key, String value) {

        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        appSharedPrefs.edit().putString(key, value).commit();
    }

    private String LoadSetting(String key) {

        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        return appSharedPrefs.getString(key, "0");
    }

    public void addChatHead(View chatHead, LayoutParams params) {
        // Log.i(TAG, "addChatHead  3");
        chatHeads.add(chatHead);
        windowManager.addView(chatHead, params);
    }

    public void removeChatHead(View chatHead) {
        // Log.i(TAG, "removeChatHead  4");

        try { // Turn Off the GPS
            locManager.removeUpdates(this); // Turn Off the GPS
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (locManager != null) {
            locManager = null;
        }

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

    public void removeChatHeads() {
        isRunning = false;
        for (View chatHead : chatHeads) {
            removeChatHead(chatHead);
        }
    }



    public static boolean isRunning() {
        return isRunning;
    }

    public static boolean didFloatCallNormal() {

        if(didFloatCallNormal){
            didFloatCallNormal = false;
            return true;
        }
        return false;
    }


} //END OF CODE



