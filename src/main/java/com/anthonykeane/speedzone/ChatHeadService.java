package com.anthonykeane.speedzone;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

//88888888888888888888888888888888888888888888888888888888888888888888888



//////////////////////////////////////////////////////////////////////////////////////////////
// code below this line is same in MainActivity and Service



    //GPS delay stuff

    private Location locCurrent = new Location("");
    private Location locLast = new Location("");


    public static final int delayBetweenGPS_Records = 10000;    //every 500mS log Geo date in Queue.
    public static final long minTime = 3000;                   // don't update GPS if time < 3000mS
    public static final float minDistanceGPS = 30;              // don't update GPS if distance < 30M

    private final Handler handler = new Handler();                // used for timers

    // public final LocListener gpsListener = new LocListener();    // used by GPS

    public final AsyncHttpClient client = new AsyncHttpClient();
    public final RequestParams HTTPrp = new RequestParams();
    public final RequestParams HTTPrp2 = new RequestParams();

    public JSONObject jHereResult = new JSONObject();
    public JSONObject jThereResult = new JSONObject();

    public float DistanceToNextSpeedChange = 0;            //any BIG number or zero
    public int iSecondsToSpeedChange = 0;
    private static String sUUID = "";

    private LocationManager locManager;
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
    private static Context context;

    //Flags
    public boolean bZoneError = false;
    public boolean bDebug = false;
    public int iNotCommsLockedOut = 0;                   //Lock out comms until last request is serviced
    public boolean bCommsTimedOut = true;


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy  5");
        handler.removeCallbacks(timedGPSqueue);

        try { // Turn Off the GPS
            locManager.removeUpdates(this); // Turn Off the GPS
        } catch (Exception e) {e.printStackTrace(); }
        if (locManager!=null){locManager = null;}

        removeChatHeads();
    }

    private void updateDebugIcon() {
        if(bDebug) {vImageViewDebug.setVisibility(View.VISIBLE);}
        else{ vImageViewDebug.setVisibility(View.GONE); }
    }

    private void updateTimeoutIcon() {
        if(bCommsTimedOut) {vImageViewTimeout.setVisibility(View.VISIBLE);}
        else{ vImageViewTimeout.setVisibility(View.GONE); }
    }

    private final Runnable timedGPSqueue; {
        timedGPSqueue = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run  ");
                if (iNotCommsLockedOut < 6){    // DON'T LET THE COMMS QUEUE GET TO BUG
                    callWebService();
                }
                handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //repeating so needed
                Log.i(TAG, "run  REPEAT TIMER                                  *");
            }
        };
    }

    private void setDisplay(int tmp) {
        setGraphicBtnV(vImageButton, tmp);
        setGraphicBtnV(vImageBtnSmall, tmp);
        DistanceToNextSpeedChange = tmp;
        iSpeed = tmp;
    }

    public void setGraphicBtnV(View x, int iSpeed) {

        ImageButton img = (ImageButton) x;
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

    private void RetreiveSettings() {
        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        sUUID = appSharedPrefs.getString(getString(R.string.myUUID), "");

        if (sUUID.equals("")){
            sUUID= randomUUID().toString();
            appSharedPrefs.edit().putString(getString(R.string.myUUID)  ,sUUID ).commit();
        }

        //String xxx = appSharedPrefs.getString(getString(R.string.settings_debugVerbosityKey), "00"); //must be at least 2 char long
        //debugVerbosity = Long.parseLong(xxx.substring(1), 16);
    }

    private void callWebService() {

        //todo
        Time now = new Time();
        now.setToNow();
        String xxx = now.format("%Y-%m-%d %H:%M:%S");

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


        Log.i(TAG, "callWebService  " + xxx + "                     *");

        if (bDebug) {
            HTTPrp.put("lat", "-34.069");
            HTTPrp.put("lon", "151.0136");
            HTTPrp.put("ber", "38");
            HTTPrp.put("speed", "99");
            HTTPrp.put("UUID", "test-" + sUUID);
            HTTPrp.put("When", xxx);
        }

        //HTTPrp.put("ber", "133");

        //Toast.makeText(this, String.valueOf(LocListener.getLat()), Toast.LENGTH_SHORT).show();
        if ((0.0 != locCurrent.getLatitude()) || bDebug) {

            if(iNotCommsLockedOut == 0)
            {
                client.post(getString(R.string.MyDbWeb), HTTPrp, new JsonHttpResponseHandler() {

                    @Override
                    public void onFailure(Throwable e, JSONArray errorResponse) {
                        System.out.println(e);
                        Log.i(TAGd, "onFailure  ");

                        bCommsTimedOut = false;
                    }

                    @Override
                    public void onFailure(Throwable e, JSONObject errorResponse) {
                        System.out.println(e);
                        Log.i(TAGd, "onFailure  ");
                        bCommsTimedOut = false;
                        //Clear the display if we don't know the value
                        // Skip is too slow to matter
                        if (locCurrent.getSpeed() >= 40) {
                            setDisplay(0);
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
                            me.setLatitude(jHereResult.getDouble("reLat"));
                            me.setLongitude(jHereResult.getDouble("reLon"));
                            try {
                                dest.setLatitude(jThereResult.getDouble("reLat"));
                                dest.setLongitude(jThereResult.getDouble("reLon"));
                                DistanceToNextSpeedChange = me.distanceTo(dest);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            iSpeed = jHereResult.getInt("reSpeedLimit");
                            setGraphicBtnV(vImageButton, iSpeed);

                            HTTPrp2.put("RE", String.valueOf(jHereResult.getString("RE")));
                            HTTPrp2.put("reSpeedLimit", String.valueOf(jHereResult.getString("reSpeedLimit")));
                            HTTPrp2.put("RdNo", String.valueOf(jHereResult.getString("RdNo")));
                            HTTPrp2.put("rePrescribed", String.valueOf(jHereResult.getString("rePrescribed")));
                            fFiveValAvgSpeed = (int) (((fFiveValAvgSpeed * 4) + locCurrent.getSpeed()) / 5);
                            iSecondsToSpeedChange = (int) ((DistanceToNextSpeedChange * 3.6 / fFiveValAvgSpeed));


                            //Toast.makeText(getApplicationContext(), iSecondsToSpeedChange, Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "onSuccess  " + iSecondsToSpeedChange + " iSecondsToSpeedChange ");


                            if (bThisIsMainActivity) {
                                updateDebugText();
                            }


                            if ((iSecondsToSpeedChange < 30) || (DistanceToNextSpeedChange < 600) || (DistanceToNextSpeedChange == 0))                         //refresh when close only
                            {
                                Log.i(TAG, "onSuccess  Getting Speec change");
                                client.post(getString(R.string.MyNextWeb), HTTPrp2, new JsonHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        Log.i(TAG, "onSuccess MyNextWeb  ");
                                        bCommsTimedOut = false;
                                        jThereResult = response;

                                        try {
                                            dest.setLatitude(jThereResult.getDouble("reLat"));
                                            dest.setLongitude(jThereResult.getDouble("reLon"));
                                            DistanceToNextSpeedChange = me.distanceTo(dest);
                                            if (bThisIsMainActivity) {

                                                setGraphicBtnV(vImageBtnSmall, jThereResult.getInt("reSpeedLimit"));
                                                //Resize the image based on distance to.
                                                float anmi = 0;
                                                if (DistanceToNextSpeedChange != 0) {
                                                    anmi = 1 / ((DistanceToNextSpeedChange + 1) / 1000);

                                                }
                                                final float v = (anmi > 1) ? 1 : anmi;
                                                setDisplayScale((v<0.3)? (float) 0.3 :v);

                                                updateDebugText();
                                            }


                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

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
                        Log.i(TAGd, "onStart  ");
                        bCommsTimedOut = true;
                        iNotCommsLockedOut++;
                    }

                    @Override
                    public void onFinish() {
                        // Completed the request (either success or failure)
                        iNotCommsLockedOut--;
                        if (iNotCommsLockedOut<=0) iNotCommsLockedOut = 0;
                        updateTimeoutIcon();
                        Log.i(TAGd, "                       onFinish  ");
                    }
                });
            }

        }
    }




    @Override
    public void onLocationChanged(Location location) {


        locLast = locCurrent;
        locCurrent = location;
        Log.i("GPS", "onLocationChanged  ");
        if (iNotCommsLockedOut ==0){
            callWebService();
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
//////////////////////////////////////////////////////////////////////////////////////////////






















    private void updateDebugText() throws JSONException {
        String x = String.valueOf(DistanceToNextSpeedChange) + "  "+ String.valueOf(iSecondsToSpeedChange) + "\n";
        setDebugText(itextView ,x);
        x = "\n\n\n\n\n" + String.valueOf(jHereResult.getString("reLon")) + " ,  " + String.valueOf(jHereResult.getString("reLat")+" ,  " + String.valueOf(jHereResult.getString("reBearing")));
        setDebugText(itextView2, x);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate  1");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        inflater = LayoutInflater.from(this);
        chatHeads = new ArrayList<View>();
        Log.i(TAG, "onCreate  ");


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand  2?");

        final View chatHead = inflater.inflate(R.layout.chat_head, null);

        vImageButton = chatHead.findViewById(R.id.imageButton);
        vErrorButton = chatHead.findViewById( R.id.imageButtonError);
        vImageBtnSmall = chatHead.findViewById(R.id.imageBtnSmall);
        vImageViewDebug = chatHead.findViewById( R.id.imageViewDebug);
        vImageViewTimeout = chatHead.findViewById(R.id.imageViewTimeout);

        // Retreive Settings
        RetreiveSettings();

        // Turn on tht GPS.     set up GPS
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistanceGPS, this);

        callWebService();
        handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //Start timer



        boolean bOK = false;

        try {
            bOK = intent.getBooleanExtra("TheOK",false);
            bDebug = intent.getBooleanExtra("bDebug",false);
            Log.i(TAG, "bDebug is  "+bDebug);
            updateDebugIcon();
            bCommsTimedOut = intent.getBooleanExtra("bCommsTimedOut",false);
            Log.i(TAG, "bCommsTimedOut is  "+bCommsTimedOut);
            updateTimeoutIcon();
            iSpeed =  intent.getIntExtra("iSpeed", 50);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "onStartCommand  Exception");
            return 0;
        }


        if(bOK)
        {
            Log.i(TAG, "onStartCommand  THE OK "+bOK);
        }
        else
        {
            Log.i(TAG, "onStartCommand  NOT ***********"+bOK);
        }



        // got iSpeed above
        setGraphicBtnV(vImageButton, iSpeed);


        vImageButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!bYouMovedIt) {
                    // Send Error to URL
                    bZoneError = true;
                    ImageButton imgerr = (ImageButton) vErrorButton;
                    imgerr.setVisibility(View.VISIBLE);
                    callWebService();


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
                    callWebService();
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
                    callWebService();
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
                boolean defaultResult = v.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        didwemove = params.x;

                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        //return true;
                        bYouMovedIt = false;
                        Log.i(TAG, String.valueOf(bYouMovedIt));
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
                boolean defaultResult = v.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        didwemove = params.x;

                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        //return true;
                        bYouMovedIt = false;
                        Log.i(TAG, String.valueOf(bYouMovedIt));
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

        addChatHead(chatHead, params);

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


} //END OF CODE



