package com.anthonykeane.speedzone;



public final class ActivityUtils {

    // Used to track what type of request is in process
    public enum REQUEST_TYPE {
        ADD, REMOVE
    }

    public static final String APPTAG = "speedzone";

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    public static final String ACTION_REFRESH_STATUS_LIST =
            "com.anthonykeane.speedzone.activityrecognition.ACTION_REFRESH_STATUS_LIST";

    public static final String CATEGORY_LOCATION_SERVICES =
            "com.anthonykeane.speedzone.activityrecognition.CATEGORY_LOCATION_SERVICES";

    // Constants used to establish the activity update interval
    private static final int MILLISECONDS_PER_SECOND = 1000;

    private static final int DETECTION_INTERVAL_SECONDS = 20;

    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    // Shared Preferences repository name
    public static final String SHARED_PREFERENCES =
            "com.anthonykeane.speedzone.activityrecognition.SHARED_PREFERENCES";

    // Key in the repository for the previous activity
    public static final String KEY_PREVIOUS_ACTIVITY_TYPE =
            "com.anthonykeane.speedzone.activityrecognition.KEY_PREVIOUS_ACTIVITY_TYPE";

    // Constants for constructing the log file name
    public static final String LOG_FILE_NAME_PREFIX = "activityrecognition";
    public static final String LOG_FILE_NAME_SUFFIX = ".log";

    // Keys in the repository for storing the log file info
    public static final String KEY_LOG_FILE_NUMBER =
            "com.anthonykeane.speedzone.activityrecognition.KEY_LOG_FILE_NUMBER";
    public static final String KEY_LOG_FILE_NAME =
            "com.anthonykeane.speedzone.activityrecognition.KEY_LOG_FILE_NAME";


}
