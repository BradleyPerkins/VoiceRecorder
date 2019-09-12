package com.taptlabs.voicerecorder.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.taptlabs.voicerecorder.activities.MainActivity;
import com.taptlabs.voicerecorder.fragments.AudioListFragment;
import com.taptlabs.voicerecorder.objects.Audio;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DataUtils {

    //Get current Date
    public static String getCurDate(){
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = new Date();
        return dateFormat.format(date);
    }

    //Get current Date/Time
    public static String getExtendedDateTime(){
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    //Get total time
    public static String totalPlaybackTime(int totalTime){
        int secs= (int) (totalTime/100);
        int mins = secs/60;
        secs%=60;
        String secStr = "";
        if (secs<10){
            secStr = "0" + secs;
        }else {
            secStr = String.valueOf(secs);
        }
        String minStr = "";
        if (mins<10){
            minStr = "0" + mins;
        }else {
            minStr = String.valueOf(mins);
        }
        return minStr+":"+ secStr;
    }





    //Share Clip
    public static void shareClip(int pos, ArrayList<Audio> audioList, Context context) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/*");
        share.putExtra(Intent.EXTRA_SUBJECT, "Voice Recorder Plus");
        share.putExtra(Intent.EXTRA_TEXT, audioList.get(pos).getTitle() + " audio clip was sent!");
        Uri fileUri = FileProvider.getUriForFile(context, "com.sharefileprovider", new File(audioList.get(pos).getFilePath()));
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.putExtra(Intent.EXTRA_STREAM, fileUri);
        context.startActivity(Intent.createChooser(share, "Share Sound File"));
    }

    //Confirm delete
    public static void confirm(final int pos, final ArrayList<Audio> audioList, final Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure want to delete this audio clip?");
        builder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        audioList.remove(pos);
                        DataCache.saveAudio(context, audioList);
                        AudioListFragment.adapter.notifyDataSetChanged();
                        Toast.makeText(context, "Audio Clip Deleted", Toast.LENGTH_SHORT).show();
                        Intent backMain = new Intent(context, MainActivity.class);
                        context.startActivity(backMain);
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
