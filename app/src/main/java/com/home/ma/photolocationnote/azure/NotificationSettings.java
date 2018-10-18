package com.home.ma.photolocationnote.azure;

public class NotificationSettings {

    // Google console project number
    public static String SenderId = "622396949720";
    // Notification hub name
    public static String HubName = "PhotoLocationNote2NotificationHub";
    // Notification listen connection string to listen for push notification
    public static String HubListenString = "Endpoint=sb://photolocationnote2notificationhubnamespace.servicebus.windows.net/;"+
            "SharedAccessKeyName=DefaultListenSharedAccessSignature;"+
            "SharedAccessKey=+R9mhNtMtpihM+fBSDmD4hShSF86aXJgtSSPdX0QqjQ=";
    // Notification full access connection string to send or manage push notification
    public static String HubFullAccessString = "Endpoint=sb://photolocationnote2notificationhubnamespace.servicebus.windows.net/;"+
            "SharedAccessKeyName=DefaultFullSharedAccessSignature;"+
            "SharedAccessKey=rNiMaoRsAOyKMKDT6B4sSrc5osnd6SqUW7K8kVpq01U=";
}