package com.anthonykeane.speedzone;


import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class NotificationReceiverActivity extends Activity {

    private static final int intentSendEmail = 2;
//    public String myInternalFile = "ToBeEmailed";// used to R/W internal file.
//    private String userEmail = "";


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to

        switch (requestCode) {
            case intentSendEmail:
                // The Intent's data Uri identifies which contact was selected.
//                File myDirectoryPath = getFilesDir();
//                File dir = new File(String.valueOf(myDirectoryPath));
//                for (File fileIn : dir.listFiles()) {
//                    if (fileIn.getName().endsWith(".png")) {
//                        fileIn.delete();
//                    }
//                }
                super.finish();
                break;

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notificationclicked);

        PackageInfo pinfo = null;
        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
//        userEmail = appSharedPrefs.getString(getString(R.string.settings_userEmailKey), "");


        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.emailAddress)});
//        intent.putExtra(Intent.EXTRA_BCC, new String[]{userEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.emailSubject).concat(" ").concat(pinfo != null ? pinfo.versionName : null));
        intent.putExtra(Intent.EXTRA_TEXT,  getString(R.string.emailBody));

//
//        File myDirectoryPath = getFilesDir();
//        File dir = new File(String.valueOf(myDirectoryPath));
//        ArrayList<Uri> uris = new ArrayList<Uri>();
//        //convert from paths to Android friendly Parcelable Uri's
//        for (File fileIn : dir.listFiles()) {
//            if (fileIn.getName().endsWith(".png")) {
//
//                Uri u = Uri.fromFile(fileIn);
//                uris.add(u);
//            }
//        }
//
//        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);


        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        startActivityForResult(intent, intentSendEmail);

    }


    @Override
    public void finish() {
        super.finish();
    }
}
