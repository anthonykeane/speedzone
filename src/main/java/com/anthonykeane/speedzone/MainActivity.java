package com.anthonykeane.speedzone;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.Time;
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
    private Handler handler = new Handler();                // used for timers
    private static final String TAG = "ChatHead::Activity";
    private LocListener gpsListener = new LocListener();    // used by GPS
    private LocationManager locManager;                     // used by GPS
    public AsyncHttpClient client = new AsyncHttpClient();
    public RequestParams HTTPrp = new RequestParams();
    public RequestParams HTTPrp2 = new RequestParams();
    public JSONObject jHereResult = new JSONObject();
    public JSONObject jThereResult = new JSONObject();
    public boolean doDebug = false;
    public float DistanceToNextSpeedChange = 0;            //any BIG number or zero
    private static String sUUID = "";
    public boolean bCommsLockedOut = false;                   //Lock out comms until last request is serviced
    final public boolean bNotTheService = true;
    private View BigButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BigButton = findViewById(R.id.imageButton);
//        Button bt = new Button(getApplicationContext());
//        WindowManager.LayoutParams param = new WindowManager.LayoutParams();
//        param.flags =WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,PixelFormat.TRANSLUCENT);
//        param.format = PixelFormat.RGBA_8888;
//        param.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
//        param.gravity = Gravity.TOP | Gravity.RIGHT;
//        param.width = 400;
//        param.height = 400;
//        param.alpha = 10;
//
//        WindowManager wmgr = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
//        wmgr.addView(bt, param);



//
//        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
//                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.RGBA_8888);
//
//        params.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_VERTICAL;
//        params.width = 400;
//        params.height = 400;
//        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
//        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//        View myView = inflater.inflate(R.layout.activity_main, null);
//        myView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                //Log.d(TAG, "touch me");
//                return false;
//            }
//        });
//
//        // Add layout to window manager
//        wm.addView(myView, params);
//





        // Retreive Settings
        RetreiveSettings();

        // Turn on teh GPS.     set up GPS
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);

        new WhatsNewScreen(this).show();
        handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //Start timer



//        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent;
//                Bundle extras;
//                intent = new Intent(MainActivity.this, ChatHeadService.class);
//                intent.putExtra("title", "Ciao");
//                intent.putExtra("text", "ChatHead");
//
//                handler.removeCallbacks(timedGPSqueue);
//                startService(intent);
//                finish();
//            }
//        });


    }

    @Override
    public void onPause() {
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Stop the GPS listener
        handler.removeCallbacks(timedGPSqueue);
        locManager.removeUpdates(gpsListener);
        super.onPause();

    }
    @Override
    public void onResume() {

        handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
        super.onResume();
    }
    @Override
    public void onDestroy() {

        handler.removeCallbacks(timedGPSqueue);
        locManager.removeUpdates(gpsListener);
        // Turn Off the GPS
        locManager = null;
        super.onDestroy();
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
                Intent intent;
                Bundle extras;
                intent = new Intent(MainActivity.this, ChatHeadService.class);
                intent.putExtra("title", "Ciao");
                intent.putExtra("text", "ChatHead");

                handler.removeCallbacks(timedGPSqueue);
                startService(intent);
                finish();
                return true;

			case R.id.sendFeedback:
				doDebug = !doDebug;
				Toast.makeText(this, getString(R.string.debug), Toast.LENGTH_SHORT).show();
				return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }    //MENU CODE END

    private void callWebService() {

        HTTPrp.put("lat",String.valueOf(gpsListener.getLat()));
        HTTPrp.put("lon", String.valueOf(gpsListener.getLon() ));
        HTTPrp.put("ber", String.valueOf(gpsListener.getBearing() ));
        HTTPrp.put("speed", String.valueOf(gpsListener.getSpeed() ));
        HTTPrp.put("UUID", sUUID);

        Time now = new Time();
        now.setToNow();
        String xxx = now.format("%Y-%m-%d %H:%m:%S");
        HTTPrp.put("When", xxx);




        if (doDebug)
        {
            HTTPrp.put("lat", "-34.069");
            HTTPrp.put("lon", "151.0136");
            HTTPrp.put("ber", "38");
            HTTPrp.put("speed", "99" );
            HTTPrp.put("UUID", "test");
            HTTPrp.put("When", xxx);
        }

        Toast.makeText(this,String.valueOf(gpsListener.getLat()) , Toast.LENGTH_SHORT).show();
        if((0.0 !=  gpsListener.getLat()) || doDebug)
        {
            Toast.makeText(this,"Sending "+delayBetweenGPS_Records , Toast.LENGTH_SHORT).show();
            client.get(getString(R.string.MyDbWeb), HTTPrp, new JsonHttpResponseHandler()
            {

                @Override
                public void onFailure(Throwable e,JSONArray errorResponse){
                    //System.out.println(e);
                    bCommsLockedOut = false;
                }
                @Override
                public void onFailure(Throwable e,JSONObject errorResponse){
                    //System.out.println(e);
                    bCommsLockedOut = false;
                    //Clear teh display if we don't know the value
                    setGraphicBtnV( (ImageButton) findViewById(R.id.imageButton), 0);
                    setGraphicBtnV((ImageButton) findViewById(R.id.imageBtnSmall), 0);
                    DistanceToNextSpeedChange = 0;
    //                img.setScaleX(1);
    //                img.setScaleY(1);
                }

                @Override
                public void onSuccess(JSONObject response)
                {
                    //System.out.println("that");
                    //System.out.println(response);
                    bCommsLockedOut = false;
                    jHereResult = response;

                    try
                    {
                        if (bNotTheService) {
                            String sdsd =   String.valueOf(response.getString("reSpeedLimit"))+"\n"+  String.valueOf(response.getString("reLon"))+"\n"+  String.valueOf(response.getString("reLat"));
                            TextView textView = null;
                            textView = (TextView) findViewById(R.id.textView2);
                            textView.setText(sdsd);
                        }


                        ImageButton img = (ImageButton) findViewById(R.id.imageButton);
                        int iSpeed = response.getInt("reSpeedLimit");
                        setGraphicBtnV(img, iSpeed);

                        HTTPrp2.put("RE", String.valueOf(response.getString("RE")));
                        HTTPrp2.put("reSpeedLimit", String.valueOf(response.getString("reSpeedLimit")));
                        HTTPrp2.put("RdNo", String.valueOf(response.getString("RdNo")));
                        HTTPrp2.put("rePrescribed", String.valueOf(response.getString("rePrescribed")));

                        if((DistanceToNextSpeedChange < 300) || (DistanceToNextSpeedChange==0))                         //refresh when close only
                                client.get(getString(R.string.MyNextWeb), HTTPrp2, new JsonHttpResponseHandler()
                        {
                            @Override
                            public void onSuccess(JSONObject response) {
                                //System.out.println("that");
                                //System.out.println(response);
                                jThereResult = response;
                                try {
                                    //Calculate Distance
                                    Location me   = new Location("");
                                    Location dest = new Location("");
                                    me.setLatitude(jHereResult.getDouble("reLat"));
                                    me.setLongitude(jHereResult.getDouble("reLon"));
                                    dest.setLatitude(jThereResult.getDouble("reLat"));
                                    dest.setLongitude(jThereResult.getDouble("reLon"));

                                    DistanceToNextSpeedChange  = me.distanceTo(dest);
                                    if (bNotTheService) {
                                        TextView textView = (TextView) findViewById(R.id.textView);
                                        textView.setText(String.valueOf(DistanceToNextSpeedChange)+"\n");
                                    }
                                    //Resize the image based on distance to.
                                    ImageButton img = (ImageButton) findViewById(R.id.imageBtnSmall);
                                    int iSpeed = response.getInt("reSpeedLimit");
                                    setGraphicBtnV(img, iSpeed);

                                    float anmi = 1/((DistanceToNextSpeedChange/1000)+1);
                                    img.setScaleX(anmi);
                                    img.setScaleY(anmi);

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











//
//private void callWebService() {
//    Log.i(TAG, "callWebService  ");
//    HTTPrp.put("lat",String.valueOf(gpsListener.getLat()));
//    HTTPrp.put("lon", String.valueOf(gpsListener.getLon() ));
//    HTTPrp.put("ber", String.valueOf(gpsListener.getBearing() ));
//    HTTPrp.put("speed", String.valueOf(gpsListener.getSpeed() ));
//    HTTPrp.put("UUID", sUUID);
//
//    Time now = new Time();
//    now.setToNow();
//    String xxx = now.format("%Y-%m-%d %H:%m:%S");
//    HTTPrp.put("When", xxx);
//
//
//
//
//    if (doDebug)
//    {
//        HTTPrp.put("lat", "-34.069");
//        HTTPrp.put("lon", "151.0136");
//        HTTPrp.put("ber", "38");
//        HTTPrp.put("speed", "99" );
//        HTTPrp.put("UUID", "test");
//        HTTPrp.put("When", xxx);
//    }
//
//    //Toast.makeText(this, String.valueOf(gpsListener.getLat()), Toast.LENGTH_SHORT).show();
//    if((0.0 !=  gpsListener.getLat()) || doDebug)
//    {
//        Toast.makeText(this,"Sending "+delayBetweenGPS_Records , Toast.LENGTH_SHORT).show();
//        client.get(getString(R.string.MyDbWeb), HTTPrp, new JsonHttpResponseHandler()
//        {
//
//            @Override
//            public void onFailure(Throwable e,JSONArray errorResponse){
//                System.out.println(e);
//                bCommsLockedOut = false;
//            }
//            @Override
//            public void onFailure(Throwable e,JSONObject errorResponse){
//                System.out.println(e);
//                bCommsLockedOut = false;
//                //Clear teh display if we don't know the value
//                setGraphicBtnV( (ImageButton)  BigButton, 0);
//
//                DistanceToNextSpeedChange = 0;
//                //                img.setScaleX(1);
//                //                img.setScaleY(1);
//            }
//
//            @Override
//            public void onSuccess(JSONObject response)
//            {
//
//                Log.i(TAG, "HERE onSuccess  " + response);
//                bCommsLockedOut = false;
//                jHereResult = response;
//
//
//                try {
//                    //Calculate Distance between here (whrer i am now and there there the next speed change is
//                    Location me   = new Location("");
//                    Location dest = new Location("");
//                    me.setLatitude(jHereResult.getDouble("reLat"));
//                    me.setLongitude(jHereResult.getDouble("reLon"));
//                    dest.setLatitude(jThereResult.getDouble("reLat"));
//                    dest.setLongitude(jThereResult.getDouble("reLon"));
//
//                    DistanceToNextSpeedChange  = me.distanceTo(dest);
//
//
//                    if (bNotTheService) {
//                        String sdsd =   String.valueOf(response.getString("reSpeedLimit"))+"\n"+  String.valueOf(response.getString("reLon"))+"\n"+  String.valueOf(response.getString("reLat"));
//                        TextView textView2 = null;
//                        textView2 = (TextView) findViewById(R.id.textView2);
//                        textView2.setText(sdsd);
//
//                        TextView textView = (TextView) findViewById(R.id.textView);
//                        textView.setText(String.valueOf(DistanceToNextSpeedChange)+"\n");
//                        setGraphicBtnV((ImageButton) findViewById(R.id.imageBtnSmall), 0);
//                        //Resize the image based on distance to.
//                        ImageButton img = (ImageButton) findViewById(R.id.imageBtnSmall);
//                        int iSpeed = response.getInt("reSpeedLimit");
//                        setGraphicBtnV(img, iSpeed);
//
//                        float anmi = 1/((DistanceToNextSpeedChange/1000)+1);
//                        img.setScaleX(anmi);
//                        img.setScaleY(anmi);
//                    }
//
//
//
//                    Log.i(TAG, ".......onSuccess  "+ DistanceToNextSpeedChange);
//
//                    ImageButton img = (ImageButton) BigButton;
//                    int iSpeed = response.getInt("reSpeedLimit");
//                    setGraphicBtnV(img, iSpeed);
//
//                    HTTPrp2.put("RE", String.valueOf(response.getString("RE")));
//                    HTTPrp2.put("reSpeedLimit", String.valueOf(response.getString("reSpeedLimit")));
//                    HTTPrp2.put("RdNo", String.valueOf(response.getString("RdNo")));
//                    HTTPrp2.put("rePrescribed", String.valueOf(response.getString("rePrescribed")));
//
//                    if((DistanceToNextSpeedChange < 300) || (DistanceToNextSpeedChange==0))                         //refresh when close only
//                        client.get(getString(R.string.MyNextWeb), HTTPrp2, new JsonHttpResponseHandler()
//                        {
//                            @Override
//                            public void onSuccess(JSONObject response) {
//
//                                Log.i(TAG, "THERE onSuccess  "+response);
//                                jThereResult = response;
//                            }
//                        });
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        });
//    }
//}

    private Runnable timedGPSqueue;
    {
        timedGPSqueue = new Runnable() {
            @Override
            public void run() {

                if (!bCommsLockedOut) callWebService();    // only send comms is last comm is returned.
                handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //repeating so needed
            }
        };
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
                img.setImageResource(R.drawable.b00);
                break;

        }
    }

    private void RetreiveSettings() {
        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        sUUID = appSharedPrefs.getString(getString(R.string.myUUID), "");

        if (sUUID==""){
            sUUID= randomUUID().toString();
            appSharedPrefs.edit().putString(getString(R.string.myUUID)  ,sUUID ).commit();
        }

        //String xxx = appSharedPrefs.getString(getString(R.string.settings_debugVerbosityKey), "00"); //must be at least 2 char long
        //debugVerbosity = Long.parseLong(xxx.substring(1), 16);
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
