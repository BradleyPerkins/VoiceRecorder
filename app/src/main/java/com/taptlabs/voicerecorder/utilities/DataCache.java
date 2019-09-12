package com.taptlabs.voicerecorder.utilities;

import android.content.Context;

import com.taptlabs.voicerecorder.objects.Audio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class DataCache {

    private static final String FILE_NAME = "voice_rec_plus.dat";

    public static void saveAudio(Context context, ArrayList<Audio> audioList) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(audioList);
            oos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Audio> loadAudio(Context context) {
        ArrayList<Audio> audio = null;
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            audio = (ArrayList<Audio>)ois.readObject();
            ois.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        if(audio == null) {
            audio = new ArrayList<>();
        }
        return audio;
    }
}
