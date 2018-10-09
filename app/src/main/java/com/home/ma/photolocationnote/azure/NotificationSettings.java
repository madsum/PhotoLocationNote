package com.home.ma.photolocationnote.azure;

public class NotificationSettings {

    public static String SenderId = "622396949720";
    public static String HubName = "PhotoLocationNote2NotificationHub";
    public static String HubListenString = "Endpoint=sb://photolocationnote2notificationhubnamespace.servicebus.windows.net/;"+
            "SharedAccessKeyName=DefaultListenSharedAccessSignature;"+
            "SharedAccessKey=+R9mhNtMtpihM+fBSDmD4hShSF86aXJgtSSPdX0QqjQ=";
    public static String HubFullAccessString = "Endpoint=sb://photolocationnote2notificationhubnamespace.servicebus.windows.net/;"+
            "SharedAccessKeyName=DefaultFullSharedAccessSignature;"+
            "SharedAccessKey=rNiMaoRsAOyKMKDT6B4sSrc5osnd6SqUW7K8kVpq01U=";
}