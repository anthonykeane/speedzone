package com.anthonykeane.speedzone;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static java.util.UUID.randomUUID;

public class ChatHeadService extends Service {

    private boolean bYouMovedIt = false;
	private WindowManager windowManager;
	private List<View> chatHeads;
	private LayoutInflater inflater;
    private static final String TAG = "ChatHead::Service";
    private View BigButton;
    private View ErrorButton;
    private View DebugImage;
    private boolean bZoneError = false;
    private Location me = new Location("");
    private Location dest = new Location("");



    //GPS delay stuff
    public static final int delayBetweenGPS_Records = 10000;  //every 500mS log Geo date in Queue.
    private Handler handler = new Handler();                // used for timers

    private LocListener gpsListener = new LocListener();    // used by GPS
    private LocationManager locManager;                     // used by GPS
    public AsyncHttpClient client = new AsyncHttpClient();
    public RequestParams HTTPrp = new RequestParams();
    public RequestParams HTTPrp2 = new RequestParams();
    public JSONObject jHereResult = new JSONObject();
    public JSONObject jThereResult = new JSONObject();

    public float DistanceToNextSpeedChange = 0;            //any BIG number or zero
    private static String sUUID = "";
    public boolean bCommsLockedOut = false;                   //Lock out comms until last request is serviced
    public int iSpeed = 0;
    public int fFiveValAvgSpeed=60;
    public boolean doDebug = false;

    @Override
	public IBinder onBind(Intent intent) {
		return null;
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
        RetreiveSettings();


    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
Log.i(TAG, "onStartCommand  ");

        // Turn on tht GPS.     set up GPS
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //Create an instance called gpsListener of the class I added called LocListener which is an implements ( is extra to) android.location.LocationListener
        //Start the GPS listener
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);

        callWebService();
        handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //Start timer



        boolean bOK = false;

        try {
            bOK = intent.getBooleanExtra("TheOK",false);
            doDebug = intent.getBooleanExtra("doDebug",false);
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


        final View chatHead = inflater.inflate(R.layout.chat_head, null);

        //TextView txt_title = (TextView) chatHead.findViewById(R.id.txt_title);
        //TextView txt_text = (TextView) chatHead.findViewById(R.id.txt_text);

        //txt_title.setText(intent.getStringExtra("title"));
        //txt_text.setText(intent.getStringExtra("text"));
        BigButton = chatHead.findViewById(R.id.imageButton);
        ErrorButton = chatHead.findViewById(R.id.imageButtonError);
        DebugImage = chatHead.findViewById(R.id.imageViewDebug);

        ImageView imgBug = (ImageView) DebugImage;
        if (doDebug) imgBug.setVisibility(View.VISIBLE);
        else imgBug.setVisibility(View.INVISIBLE);

        ImageButton img = (ImageButton) BigButton;
        // got iSpeed above
        setGraphicBtnV( (ImageButton)  BigButton, iSpeed);


        BigButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!bYouMovedIt) {
                    // Send Error to URL
                    bZoneError = true;
                    ImageButton imgerr = (ImageButton) ErrorButton;
                    imgerr.setVisibility(View.VISIBLE);


                }
                return true;
            }
        });

        ErrorButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!bYouMovedIt)
                {
                    // Send Error to URL
                    bZoneError = false;
                    ImageButton imgerr = (ImageButton) ErrorButton;
                    imgerr.setVisibility(View.GONE);
                }
                return true;
            }
        });


        BigButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!bYouMovedIt)
                {
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

        ErrorButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!bYouMovedIt)
                {
                    // Send Error to URL
                    bZoneError = false;
                    ImageButton imgerr = (ImageButton) ErrorButton;
                    imgerr.setVisibility(View.GONE);
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

        BigButton.setOnTouchListener(new View.OnTouchListener() {
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
                        SaveSetting("params.x",String.valueOf(params.x));
                        SaveSetting("params.y",String.valueOf(params.y));
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

        ErrorButton.setOnTouchListener(new View.OnTouchListener() {
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
                        SaveSetting("params.x",String.valueOf(params.x));
                        SaveSetting("params.y",String.valueOf(params.y));
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

	public void addChatHead(View chatHead, LayoutParams params) {
		Log.i(TAG, "addChatHead  3");chatHeads.add(chatHead);
		windowManager.addView(chatHead, params);
	}

	public void removeChatHead(View chatHead) {
        Log.i(TAG, "removeChatHead  4");

        try { // Turn Off the GPS
            locManager.removeUpdates(gpsListener); // Turn Off the GPS
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

	@Override
	public void onDestroy() {
        Log.i(TAG, "onDestroy  5");
        //Toast.makeText(this,"DISTROYING " , Toast.LENGTH_SHORT).show();
        handler.removeCallbacks(timedGPSqueue);


        try { // Turn Off the GPS
            locManager.removeUpdates(gpsListener); // Turn Off the GPS
        } catch (Exception e) {e.printStackTrace(); }
        if (locManager!=null){locManager = null;}



        for (View chatHead : chatHeads) {
			removeChatHead(chatHead);
		}
        super.onDestroy();
    }

    private void callWebService() {
        Log.i(TAG, "callWebService  ");
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
        Log.i(TAG, "callWebService  " + xxx);

        if (doDebug) {
            HTTPrp.put("lat", "-34.069");
            HTTPrp.put("lon", "151.0136");
            HTTPrp.put("ber", "38");
            HTTPrp.put("speed", "99");
            HTTPrp.put("UUID", "test-" + sUUID);
            HTTPrp.put("When", xxx);
        }

        //Toast.makeText(this, String.valueOf(gpsListener.getLat()), Toast.LENGTH_SHORT).show();
        if ((0.0 != LocListener.getLat()) || doDebug) {
            client.get(getString(R.string.MyDbWeb), HTTPrp, new JsonHttpResponseHandler() {

                @Override
                public void onFailure(Throwable e, JSONArray errorResponse) {
                    System.out.println(e);
                    bCommsLockedOut = false;
                }

                @Override
                public void onFailure(Throwable e, JSONObject errorResponse) {
                    System.out.println(e);
                    bCommsLockedOut = false;
                    //Clear teh display if we don't know the value
                    setGraphicBtnV((ImageButton) BigButton, 0);

                    if (LocListener.getSpeed()>=40) {
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






                        ImageButton img = (ImageButton) BigButton;
                        iSpeed = jHereResult.getInt("reSpeedLimit");
                        setGraphicBtnV(img, iSpeed);

                        HTTPrp2.put("RE", String.valueOf(jHereResult.getString("RE")));
                        HTTPrp2.put("reSpeedLimit", String.valueOf(jHereResult.getString("reSpeedLimit")));
                        HTTPrp2.put("RdNo", String.valueOf(jHereResult.getString("RdNo")));
                        HTTPrp2.put("rePrescribed", String.valueOf(jHereResult.getString("rePrescribed")));
                        fFiveValAvgSpeed = (int) (((fFiveValAvgSpeed*4)+ LocListener.getSpeed())/5);
                        int iSecondsToSpeedChange = (int) ((DistanceToNextSpeedChange * 3600 / fFiveValAvgSpeed ));

                        //Toast.makeText(getApplicationContext(), iSecondsToSpeedChange, Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "onSuccess  " + iSecondsToSpeedChange + " iSecondsToSpeedChange ");
                        if ((iSecondsToSpeedChange < 300) || (DistanceToNextSpeedChange < 3000) || (DistanceToNextSpeedChange == 0))                         //refresh when close only
                            client.get(getString(R.string.MyNextWeb), HTTPrp2, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    jThereResult = response;
                                    //ImageButton img = (ImageButton) findViewById(R.id.imageBtnSmall);
                                    try {
                                        //setGraphicBtnV(img, jThereResult.getInt("reSpeedLimit"));
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

    private void RetreiveSettings() {
        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        sUUID = appSharedPrefs.getString(getString(R.string.myUUID), "");

        if (sUUID.equals("")){
            sUUID= randomUUID().toString();
            SaveSetting(getString(R.string.myUUID), sUUID);
        }

        //String xxx = appSharedPrefs.getString(getString(R.string.settings_debugVerbosityKey), "00"); //must be at least 2 char long
        //debugVerbosity = Long.parseLong(xxx.substring(1), 16);
    }

    private void SaveSetting(String key, String value){

        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        appSharedPrefs.edit().putString(key, value).commit();


    }

    private String LoadSetting(String key){

        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        return appSharedPrefs.getString(key, "0");


    }

    private Runnable timedGPSqueue;
    {
        timedGPSqueue = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run  ");
                if (!bCommsLockedOut) callWebService();    // only send comms is last comm is returned.
                handler.postDelayed(timedGPSqueue, delayBetweenGPS_Records);   //repeating so needed
            }
        };
    }

//////////////////////////////////////////////////////////////////////////////////////////////
} //END OF CODE



