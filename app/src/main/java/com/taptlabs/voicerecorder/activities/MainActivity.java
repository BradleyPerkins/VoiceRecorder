package com.taptlabs.voicerecorder.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.taptlabs.voicerecorder.R;
import com.taptlabs.voicerecorder.fragments.AudioListFragment;
import com.taptlabs.voicerecorder.fragments.PlayerFragment;
import com.taptlabs.voicerecorder.fragments.RecordFragment;
import com.taptlabs.voicerecorder.objects.Audio;
import com.taptlabs.voicerecorder.utilities.AudioListAdapter;
import com.taptlabs.voicerecorder.utilities.DataCache;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecordFragment.RecorderListener, AudioListFragment.AudioListener, PlayerFragment.PlayerListener {

    //TODO function to edit recording files by cutting some parts not needed
    //TODO show remaining space of internal storage or SD card
    //TODO save/pause/resume/cancel recording process control
    //TODO microphone tool to adjust the sensitive
    //TODO have list of files with basic functions: search, arrange

    private FloatingActionButton recBtn;
    private boolean isRecording = false;
    private boolean isPlayer = false;

    ArrayList<Audio> audioList;
    private int audioNavPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("");

        MobileAds.initialize(this, "ca-app-pub-4730800757487119/3808060565");
        AdView adView = findViewById(R.id.main_adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("CBED1A66CE53567A6EE870CB0F59D107")
                .build();
        adView.loadAd(adRequest);

        audioList = DataCache.loadAudio(this);

        recBtn = findViewById(R.id.fab);
        recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordClip();
            }
        });

        if (audioList != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_placeholder, AudioListFragment.newInstance(audioList, 0)).commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (isRecording ){
            isRecording = false;
            isPlayer = false;
            audioNavPos = 0;
            recBtn.show();
            getSupportFragmentManager().beginTransaction().replace(R.id.main_placeholder, AudioListFragment.newInstance(audioList, audioNavPos)).commit();
        } else if (isPlayer){
            isPlayer = false;
            isRecording = false;
            getSupportFragmentManager().beginTransaction().replace(R.id.main_placeholder, AudioListFragment.newInstance(audioList, audioNavPos)).commit();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void recordClip() {
        isRecording = true;
        recBtn.hide();
        getSupportFragmentManager().beginTransaction().replace(R.id.main_placeholder, RecordFragment.newInstance()).commit();
    }

    @Override
    public void saveAudio(String title, String size, String date, String filePath , String timerCount) {
        Audio item = new Audio(title, size, date, filePath, timerCount);
        ArrayList<Audio> list = DataCache.loadAudio(this);
        list.add(item);
        DataCache.saveAudio(this, list);
        audioList = DataCache.loadAudio(this);
        //update adapter
        AudioListFragment.adapter.notifyDataSetChanged();
    }

    @Override
    public void updateTitle(String title, String filePath) {
        //Get Position of filepath in arraylist
        ArrayList<Audio> list =  DataCache.loadAudio(this);
        for (int i=0; i<list.size();i++){
            if (list.get(i).getFilePath().equals(filePath)){
                list.get(i).setTitle(title);
                DataCache.saveAudio(this, list);
                audioList = DataCache.loadAudio(this);
                AudioListFragment.adapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    public void audioNav(int pos) {
        //Navigate to Player and pass position
        audioNavPos = pos;
        isPlayer = true;
        getSupportFragmentManager().beginTransaction().replace(R.id.main_placeholder, PlayerFragment.newInstance(audioList, pos)).commit();
    }

    @Override
    public void prevClip() {
        audioNavPos--;
        isPlayer = true;
        getSupportFragmentManager().beginTransaction().replace(R.id.main_placeholder, PlayerFragment.newInstance(audioList, audioNavPos)).commit();

    }

    @Override
    public void nextClip() {
        audioNavPos++;
        isPlayer = true;
        getSupportFragmentManager().beginTransaction().replace(R.id.main_placeholder, PlayerFragment.newInstance(audioList, audioNavPos)).commit();
    }

}
