package com.anthonykeane.speedzone;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static java.util.UUID.randomUUID;


public class MainActivity extends Activity implements LocationListener {

    //Is different in MainActivity
    private static final boolean bThisIsMainActivity = true;
    private static final String TAG = "ChatHead::Activity";
    private static final String TAGd = "ChatHead::Activity_focus";

    private static final int intentSettings = 1;


//////////////////////////////////////////////////////////////////////////////////////////////
// code below this line is same in MainActivity and Service

    private static final int intentTTS = 3;

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
    private boolean bMute = true;


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


        bMute = !(appSharedPrefs.getBoolean(getString(R.string.settings_soundKey), false));  // Active Low
        bDebug = appSharedPrefs.getBoolean(getString(R.string.settings_debugKey), false);
//        alertOnGreenLightEnabled = appSharedPrefs.getBoolean(getString(R.string.settings_alertOnGreenLightEnabledKey), false);
//        userEmail = appSharedPrefs.getString(getString(R.string.settings_userEmailKey), "");
//        ttsSalute = appSharedPrefs.getString(getString(R.string.settings_ttsSaluteKey), getString(R.string.ttsSalute));
//        ttsSignFound = appSharedPrefs.getString(getString(R.string.settings_ttsSignFoundKey), getString(R.string.ttsSignFound));
//        bExperimental = appSharedPrefs.getBoolean(getString(R.string.settings_bExperimentalKey), false);
//        debugVerbosity = Integer.parseInt(appSharedPrefs.getString(getString(R.string.settings_debugVerbosityKey), "0"));


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
        String x ="";
//        if (DistanceToNextSpeedChange>=60){
//            x = String.valueOf((int)(DistanceToNextSpeedChange/1000)+1) + "Km           or       "+ String.valueOf((int) (iSecondsToSpeedChange/60)+1) + "Min\n";
//        }
//        else{
//            x = String.valueOf((int)DistanceToNextSpeedChange) + "M           or       "+ String.valueOf((int) (iSecondsToSpeedChange)) + "Sec\n";
//        }
        x = String.valueOf((int)DistanceToNextSpeedChange) + "M           or       "+ String.valueOf((int) (iSecondsToSpeedChange)) + "Sec\n";
        setDebugText(itextView ,x);
        x = String.valueOf(jHereResult.getString("reLon")) + " ,  " + String.valueOf(jHereResult.getString("reLat")+" ,  " + String.valueOf(jHereResult.getString("reBearing")));
        setDebugText(itextView2, x);
    }


    //todo
    public static Context getAppContext() {
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new WhatsNewScreen(this).show();

//
//
        setContentView(R.layout.activity_main);


        vImageButton = findViewById(R.id.imageButton);
        vErrorButton = findViewById( R.id.imageButtonError);
        vImageBtnSmall = findViewById(R.id.imageBtnSmall);
        vImageViewDebug =  findViewById( R.id.imageViewDebug);
        vImageViewTimeout = findViewById(R.id.imageViewTimeout);

        // Receive Settings
        RetreiveSettings();

        // Turn on teh GPS.     set up GPS
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistanceGPS, this);



        callWebService();
        //handler.postDelayed(timedGPSqueue, 8);   //Start timer

        vImageButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                //todo  Send Error to URL
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


        updateTimeoutIcon();
        updateDebugIcon();

    }

//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


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

    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first

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
            case R.id.action_settings:

                //if(isMyServiceRunning())
            {
                Intent intent;
                Bundle extras;
                intent = new Intent(MainActivity.this, ChatHeadService.class);
                intent.putExtra("TheOK", true);
                Log.i(TAG, "bDebug is  "+bDebug);
                intent.putExtra("bCommsTimedOut", bDebug);
                intent.putExtra("", bCommsTimedOut);
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
                Intent i = new Intent(this, PreferencesActivity.class);
                startActivityForResult(i, intentSettings);
//
//                }
//                else
//
//                {
//                    Intent i = new Intent(this, PreferencesActivitySingle.class);
//                    startActivityForResult(i, intentSettings);
//                }
//
                RetreiveSettings();
//                SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
//                bMute = appSharedPrefs.getBoolean(getString(R.string.settings_soundKey), false);
//                doDebug = appSharedPrefs.getBoolean(getString(R.string.settings_displayKey), false);
//                alertOnGreenLightEnabled = appSharedPrefs.getBoolean(getString(R.string.settings_alertOnGreenLightEnabledKey), false);


                return true;





            case R.id.sendFeedback:
                bDebug = !bDebug;
                updateDebugIcon();

                Toast.makeText(this, String.valueOf(bDebug), Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }    //MENU CODE END



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
//

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
