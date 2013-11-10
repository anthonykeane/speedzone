package com.anthonykeane.speedzone;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;

@SuppressWarnings("EmptyMethod")
public class tut1 extends Activity  {

    private final Handler handler = new Handler();                // used for timers
    private int helpItem = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Debug.startMethodTracing("anthony.trace");
        //client.setTimeout(2000);
//
//
        setContentView(R.layout.tutpage1);
//        handler.postDelayed(timedGPSqueue, 2000);

        findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.imageBtnSmall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.imageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.imageView3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.imageView2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });






    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

//    private final Runnable timedGPSqueue;
//
//    {
//        timedGPSqueue = new Runnable() {
//            @Override
//            public void run() {
//
//
//                switch (helpItem) {
//                    case 0:
//                        explainMain();
//                        helpItem = 1;
//                        handler.postDelayed(timedGPSqueue, 6500);   //repeating so needed
//                        break;
//                    case 1:
//                        explainNextSeed();
//                        helpItem = 2;
//                        handler.postDelayed(timedGPSqueue, 6500);   //repeating so needed
//                        break;
//                    case 2:
//                        explainimageAlert();
//                        helpItem = 3;
//                        handler.postDelayed(timedGPSqueue, 6500);   //repeating so needed
//                        break;
//                    case 3:
//                        explainimageSZAlert();
//                        helpItem = 4;
//                        handler.postDelayed(timedGPSqueue, 6500);   //repeating so needed
//                        break;
//                    case 4:
//                        explainMain();
//                        helpItem = 5;
//                        handler.postDelayed(timedGPSqueue, 6500);   //repeating so needed
//                        break;
//                    case 5:
//                        explainMain();
//                        helpItem = 0;
//                        handler.postDelayed(timedGPSqueue, 6500);   //repeating so needed
//                        break;
//
//                    default:
//
//                        break;
//
//                }
//
//
//
//
//
//
//                //Log.i(TAG, "run  REPEAT TIMER  " + locCurrent.getAccuracy());
//            }
//        };
//    }


//    private void explainMain(){
//        MainActivity.mTts.speak("This is the main speed limit indicator, it displays the current posted speed limit", TextToSpeech.QUEUE_ADD, null);
//        findViewById(R.id.imageButton).startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce10));
//    }
//    private void explainNextSeed(){
//        MainActivity.mTts.speak("This is the next speed limit indicator, it displays the upconning posted speed limit", TextToSpeech.QUEUE_ADD, null);
//        findViewById(R.id.imageBtnSmall).startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce10));
//    }
//    private void explainimageAlert(){
//        findViewById(R.id.imageAlert).setVisibility(View.VISIBLE);
//        MainActivity.mTts.speak("This is the main speed limit indicator, it displays the current posted speed limit", TextToSpeech.QUEUE_ADD, null);
//        findViewById(R.id.imageAlert).startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce10));
//    }
//    private void explainimageSZAlert(){
//        findViewById(R.id.imageAlert).setVisibility(View.VISIBLE);
//        findViewById(R.id.imageSZAlert).setVisibility(View.VISIBLE);
//        MainActivity.mTts.speak("This is the main speed limit indicator, it displays the current posted speed limit", TextToSpeech.QUEUE_ADD, null);
//        findViewById(R.id.imageSZAlert).startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce10));
//    }
//    private void explainMain(){
//        findViewById(R.id.imageSZAlert).setVisibility(View.INVISIBLE);
//        MainActivity.mTts.speak("This is the main speed limit indicator, it displays the current posted speed limit", TextToSpeech.QUEUE_ADD, null);
//        findViewById(R.id.imageButtonError).startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce10));
//    }
//    private void explainMain(){
//        MainActivity.mTts.speak("This is the main speed limit indicator, it displays the current posted speed limit", TextToSpeech.QUEUE_ADD, null);
//        findViewById(R.id.imageButton).startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce10));
//    }
//    private void explainMain(){
//        MainActivity.mTts.speak("This is the main speed limit indicator, it displays the current posted speed limit", TextToSpeech.QUEUE_ADD, null);
//        findViewById(R.id.imageButton).startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce10));
//    }

//////////////////////////////////////////////////////////////////////////////////////////////
} //END OF CODE

