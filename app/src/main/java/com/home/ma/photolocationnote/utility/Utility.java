package com.home.ma.photolocationnote.utility;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.io.File;

/**
 * Created by masum on 11/08/15.
 */
public class Utility {

    public static void displayWarning(Context context, String title, String message){
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Fatal error");
        alertDialog.setMessage("First enbale network and GPS. Then retry again");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public static boolean deleteFile(String path){
        File deleteFile = new File(path);
        if(deleteFile.exists()){
            deleteFile.delete();
            return true;
        }else{
            return false;
        }
    }

    public static boolean fileExist(String path){
        File file = new File(path);
        if(file.exists()){
            return true;
        }else{
            return false;
        }
    }
}
