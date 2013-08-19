package com.anthonykeane.speedzone;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
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


public class MainActivity extends Activity {


    //GPS delay stuff
    public static final int delayBetweenGPS_Records = 10000;  //every 500mS log Geo date in Queue.
    private final Handler handler = new Handler();                // used for timers
    private static final String TAG = "ChatHead::Activity";
    private final LocListener gpsListener = new LocListener();    // used by GPS
    private LocationManager locManager;                     // used by GPS
    public final AsyncHttpClient client = new AsyncHttpClient();
    public final RequestParams HTTPrp = new RequestParams();
    public final RequestParams HTTPrp2 = new RequestParams();
    public JSONObject jHereResult = new JSONObject();
    public JSONObject jThereResult = new JSONObject();

    public float DistanceToNextSpeedChange = 0;            //any BIG number or zero
    public int iSecondsToSpeedChange = 0;
    private static String sUUID = "";
    public boolean bCommsLockedOut = false;                   //Lock out comms until last request is serviced
    final public boolean bNotTheService = true;
    private View BigButton;
    private View ErrorButton;
    public int iSpeed = 50;
    public int fFiveValAvgSpeed=60;
    public boolean bZoneError = false;
    public boolean doDebug = true;
    private Location me = new Location("");
    private Location dest = new Location("");

   // @Override
    protected void onNewIntent(){
    Log.i(TAG, "onNewIntent  ");

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BigButton = findViewById(R.id.imageButton);
        ErrorButton = findViewById(R.id.imageButtonError);

        // Receive Settings
        RetreiveSettings();

        // Turn on teh GPS.     set up GPS
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);


        new WhatsNewScreen(this).show();

        callWebService();
        //handler.postDelayed(timedGPSqueue, 8);   //Start timer

        BigButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                //todo  Send Error to URL
                bZoneError = true;
                ImageButton imgerr = (ImageButton) ErrorButton;
                imgerr.setVisibility(View.VISIBLE);
                return true;
            }
        });


        BigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"LONG PRESS to change" , Toast.LENGTH_SHORT).show();
            }
        });

        ErrorButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {


                    // Send Error to URL
                    bZoneError = false;
                    ImageButton imgerr = (ImageButton) ErrorButton;
                    imgerr.setVisibility(View.INVISIBLE);
                    return true;
            }
        });

        ErrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"LONG PRESS to change" , Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause  ");
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Stop the GPS listener
        handler.removeCallbacks(timedGPSqueue);
        locManager.removeUpdates(gpsListener);


    }
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume  ");

        handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);
        Log.i(TAG, "onResume  START TIMER");
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);

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
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy  ");
        handler.removeCallbacks(timedGPSqueue);

            try { // Turn Off the GPS
                locManager.removeUpdates(gpsListener); // Turn Off the GPS
            } catch (Exception e) {e.printStackTrace(); }
            if (locManager!=null){locManager = null;}

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
                    intent.putExtra("doDebug", doDebug);
                    intent.putExtra("iSpeed",iSpeed);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    handler.removeCallbacks(timedGPSqueue);
                    moveTaskToBack(true);
                    startService(intent);
                    //onStop();
                }
                //finish();
                return true;

			case R.id.sendFeedback:
				doDebug = !doDebug;
                if (doDebug) {
                    findViewById(R.id.imageViewDebug).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.imageViewDebug).setVisibility(View.INVISIBLE);
                }

				Toast.makeText(this, getString(R.string.debug), Toast.LENGTH_SHORT).show();
				return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }    //MENU CODE END

    private void callWebService() {
        HTTPrp.put("lat", String.valueOf(LocListener.getLat()));
        HTTPrp.put("lon", String.valueOf(LocListener.getLon()));
        HTTPrp.put("ber", String.valueOf(LocListener.getBearing()));
        HTTPrp.put("speed", String.valueOf(LocListener.getSpeed()));
        HTTPrp.put("UUID", sUUID);
        if (bZoneError) {
            HTTPrp.put("bZoneError", "1");
        } else {
            HTTPrp.put("bZoneError", "0");
        }
        Time now = new Time();
        now.setToNow();
        String xxx = now.format("%Y-%m-%d %H:%M:%S");
        HTTPrp.put("When", xxx);
        Log.i(TAG, "callWebService  " + xxx + "                     *");

        if (doDebug) {
            HTTPrp.put("lat", "-34.069");
            HTTPrp.put("lon", "151.0136");
            HTTPrp.put("ber", "38");
            HTTPrp.put("speed", "99");
            HTTPrp.put("UUID", "test-" + sUUID);
            HTTPrp.put("When", xxx);
        }



        //Toast.makeText(this, String.valueOf(LocListener.getLat()), Toast.LENGTH_SHORT).show();
        if ((0.0 != LocListener.getLat()) || doDebug) {
            client.get(getString(R.string.MyDbWeb), HTTPrp, new JsonHttpResponseHandler() {

                @Override
                public void onFailure(Throwable e, JSONArray errorResponse) {
                    System.out.println(e);
                    Log.i(TAG, "onFailure  ");
                    bCommsLockedOut = false;
                }

                @Override
                public void onFailure(Throwable e, JSONObject errorResponse) {
                    System.out.println(e);
                    bCommsLockedOut = false;
                    //Clear the display if we don't know the value
                    // Skip is too slow to matter

                    if (LocListener.getSpeed()>=40) {
                        setGraphicBtnV((ImageButton) findViewById(R.id.imageButton), 0);
                        setGraphicBtnV((ImageButton) findViewById(R.id.imageBtnSmall), 0);
                        DistanceToNextSpeedChange = 0;
                        iSpeed = 50;
                    }
                }

                @Override
                public void onSuccess(JSONObject response) {
                    bCommsLockedOut = false;
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


                        if (bNotTheService) {
                            TextView textView = (TextView) findViewById(R.id.textView);
                            textView.setText(String.valueOf(DistanceToNextSpeedChange) + "  "+ String.valueOf(iSecondsToSpeedChange) + "\n");

                            //Resize the image based on distance to.
                            ImageButton img = (ImageButton) findViewById(R.id.imageBtnSmall);
                            if (DistanceToNextSpeedChange != 0) {
                                float anmi = 1 / ((DistanceToNextSpeedChange / 1000) + 1);
                                img.setScaleX(anmi);
                                img.setScaleY(anmi);
                            }

                            String sdsd = "\n\n\n\n\n" + String.valueOf(jHereResult.getString("reLon")) + " ,  " + String.valueOf(response.getString("reLat"));
                            textView = null;
                            textView = (TextView) findViewById(R.id.textView2);
                            textView.setText(sdsd);
                        }
                        ImageButton img = (ImageButton) findViewById(R.id.imageButton);
                        iSpeed = jHereResult.getInt("reSpeedLimit");
                        setGraphicBtnV(img, iSpeed);

                        HTTPrp2.put("RE", String.valueOf(jHereResult.getString("RE")));
                        HTTPrp2.put("reSpeedLimit", String.valueOf(jHereResult.getString("reSpeedLimit")));
                        HTTPrp2.put("RdNo", String.valueOf(jHereResult.getString("RdNo")));
                        HTTPrp2.put("rePrescribed", String.valueOf(jHereResult.getString("rePrescribed")));
                        fFiveValAvgSpeed = (int) (((fFiveValAvgSpeed*4)+ LocListener.getSpeed())/5);
                        iSecondsToSpeedChange = (int) ((DistanceToNextSpeedChange * 3.6 / fFiveValAvgSpeed ));


                        //Toast.makeText(getApplicationContext(), iSecondsToSpeedChange, Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "onSuccess  " + iSecondsToSpeedChange + " iSecondsToSpeedChange ");
                        if ((iSecondsToSpeedChange < 30) || (DistanceToNextSpeedChange < 300) || (DistanceToNextSpeedChange == 0))                         //refresh when close only
                            client.get(getString(R.string.MyNextWeb), HTTPrp2, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    jThereResult = response;
                                    ImageButton img = (ImageButton) findViewById(R.id.imageBtnSmall);
                                    try {
                                        setGraphicBtnV(img, jThereResult.getInt("reSpeedLimit"));
                                        dest.setLatitude(jThereResult.getDouble("reLat"));
                                        dest.setLongitude(jThereResult.getDouble("reLon"));
                                        DistanceToNextSpeedChange = me.distanceTo(dest);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }
                            });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    }

    public void setGraphicBtnV(ImageButton img, int iSpeed) {
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

    private final Runnable timedGPSqueue; {
        timedGPSqueue = new Runnable() {
            int iCommsLockedOutCount;
            @Override
            public void run() {
                Log.i(TAG, "run  "+ bCommsLockedOut);
                iCommsLockedOutCount++;
                //if (!bCommsLockedOut || iCommsLockedOutCount>6) {
                callWebService();    // only send comms is last comm is returned.
                bCommsLockedOut = true;
                iCommsLockedOutCount = 0;
//                }
                handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //repeating so needed
                Log.i(TAG, "run  REPEAT TIMER                                  *");
            }
        };
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
