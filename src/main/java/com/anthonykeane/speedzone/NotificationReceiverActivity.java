package com.anthonykeane.speedzone;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.Bundle;

public class NotificationReceiverActivity extends Activity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notificationclicked);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

    }



    @Override
    public void finish() {
        super.finish();
    }
}
