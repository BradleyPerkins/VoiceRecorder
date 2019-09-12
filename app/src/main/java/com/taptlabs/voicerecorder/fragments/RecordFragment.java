package com.taptlabs.voicerecorder.fragments;


import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.taptlabs.voicerecorder.R;
import com.taptlabs.voicerecorder.objects.Audio;
import com.taptlabs.voicerecorder.utilities.DataCache;
import com.taptlabs.voicerecorder.utilities.DataUtils;
import com.taptlabs.voicerecorder.utilities.VisualizerView;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class RecordFragment extends Fragment  implements View.OnClickListener {

    private static ArrayList<Float> amps;

    private Handler handler;
    private Runnable runnable;

    private Handler timerHandler;
    private long startTime = 0L;
    private long timeInMil = 0L;
    private long timeSwapBuff = 0L;
    private long updateTime = 0L;


    private int mInterval = 10;

    private static final int REQUEST_PERMISSION_CODE = 1000;

    private String date;

    private String pathSave = null;
    private String audioTitle = null;
    double file_size = 0;
    private String timerCountStr = "";

    private boolean isRecording = false;
    private boolean isPlayingClip = false;
    private boolean isPausedClip = false;

    private int pausedPos = 0;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    public ImageView recAudioBtn, stopRecBtn, playAudioBtn, pauseBtn, titleEditBtn;
    private TextView titleTV, durationTV, fileSizeTV, dateTV, timerTV, currentTimeTV, totalTimeTV;
    private SeekBar seekBar;

    private VisualizerView visualizerView;
    private Handler visualizerHandler;
    public static final int REPEAT_INTERVAL = 40;


    private RecorderListener mListener;

    public RecordFragment() {}

    public static RecordFragment newInstance() {
        return  new RecordFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_record, container, false);
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //check if path inst null
        if (pathSave != null){
            ArrayList<Audio> audioList = DataCache.loadAudio(getActivity());
            int pos = audioList.size() - 1;
            switch (item.getItemId()) {
                case R.id.action_delete:
                    DataUtils.confirm(pos, audioList, getContext());
                    //navigate back to List
                    break;
                case R.id.action_share:
                    DataUtils.shareClip(pos, audioList, getContext());
                    break;
                default:
                    break;
            }
        }

        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.audio_clip_menu, menu);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        handler = new Handler();
        visualizerHandler = new Handler();
        timerHandler = new Handler();
        startTime = SystemClock.uptimeMillis();

        seekBar = getView().findViewById(R.id.seekbar_rec);
        seekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        seekBar.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

        seekbarToggle(true);
        permCheck();

        visualizerView = getView().findViewById(R.id.visualizer);
        recAudioBtn = getView().findViewById(R.id.record_btn);
        recAudioBtn.setImageResource(R.drawable.rec_unpressed1);
        stopRecBtn = getView().findViewById(R.id.stop_rec_btn);
        playAudioBtn = getView().findViewById(R.id.playback_btn);
        playAudioBtn = getView().findViewById(R.id.playback_btn);
        pauseBtn = getView().findViewById(R.id.pause_btn);
        titleEditBtn = getView().findViewById(R.id.rec_edit_title);
        titleTV = getView().findViewById(R.id.rec_title_tv);
        timerTV = getView().findViewById(R.id.rec_timer_tv);
        dateTV = getView().findViewById(R.id.rec_date_tv);
        durationTV = getView().findViewById(R.id.rec_duration_tv);
        fileSizeTV = getView().findViewById(R.id.rec_file_size_tv);

        currentTimeTV = getView().findViewById(R.id.current_time_tv);
        totalTimeTV = getView().findViewById(R.id.total_time_tv);

        dateTV.setText(DataUtils.getCurDate());

        timerTV.setVisibility(View.INVISIBLE);

        if (pathSave == null){
            buttonConfigure("initial");
        }

        recAudioBtn.setOnClickListener(this);
        stopRecBtn.setOnClickListener(this);
        playAudioBtn.setOnClickListener(this);
        pauseBtn.setOnClickListener(this);
        titleEditBtn.setOnClickListener(this);

        titleEditBtn.setVisibility(View.INVISIBLE);


    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.record_btn:
                if (!isRecording && !isPlayingClip){
                    isRecording = true;
                    isPlayingClip = false;
                    isPausedClip = false;

                    if (amps == null){
                        amps = new ArrayList<>();
                    }

                    amps.clear();
                    if (mediaPlayer != null && isPlayingClip){
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }

                    mediaPlayer = new MediaPlayer();
                    date = DataUtils.getExtendedDateTime();

                    timerTV.setVisibility(View.VISIBLE);

                    String sep = File.separator; // Use this instead of hardcoding the "/"
                    String newFolder = "VoiceRecorderTL";
                    String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                    File myNewFolder = new File(extStorageDirectory + sep + newFolder);
                    myNewFolder.mkdir();
                    pathSave = Environment.getExternalStorageDirectory().toString() + sep + newFolder + sep + date + "_" + ".mp3";

                    setupMediaRecorder();

                    try{
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    startTime = 0L;
                    timeInMil = 0L;
                    timeSwapBuff = 0L;
                    updateTime = 0L;
                    startTime = SystemClock.uptimeMillis();
                    timerHandler.postDelayed(updateTimerThread, 0);
                    buttonConfigure("rec_base");

                }else {
                    isRecording = false;

                    timerTV.setVisibility(View.INVISIBLE);

                    amps = new ArrayList<>();
                    amps = VisualizerView.amplitudesList;

                    //Save out timer text to var
                    timerCountStr = timerTV.getText().toString().trim();

                    visualizerView.clear();
                    mediaRecorder.stop();
                    renameDialog();
                    buttonConfigure("rec_active");

                    //Stop Timer
                    timerHandler.removeCallbacks(updateTimerThread);
                }
                break;

            case R.id.playback_btn:
                seekbarToggle(false);
                isPlayingClip = true;
                //isPausedClip = false;
                if (pathSave != null){
                    isRecording = false;
                    buttonConfigure("play_base");

                    if (mediaPlayer != null){
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }

                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(pathSave);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (isPlayingClip){
                        if (isPausedClip){
                            //Unpause
                            mediaPlayer.seekTo(pausedPos);
                            mediaPlayer.start();
                            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                                    if (b){
                                        mediaPlayer.seekTo(progress*mInterval);
                                    }
                                }
                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {}
                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {}
                            });
                            isPausedClip = false;
                            pauseBtn.setEnabled(true);
                            pauseBtn.setImageResource(R.drawable.pause_unpressed);
                        } else {
                            //Play Clip
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    seekBar.setMax(mediaPlayer.getDuration());
                                    mediaPlayer.start();

                                    initializeSeekBar();
                                }
                            });

                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    buttonConfigure("play_active");
                                    mediaPlayer.pause();

                                    isPlayingClip = false;
                                    isPausedClip = false;
                                }
                            });

                            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                                    if (b){
                                        mediaPlayer.seekTo(progress * mInterval);
                                    }
                                }
                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {}
                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {}
                            });
                        }
                    }
                }
                break;

            //PAUSE BUTTON
            case R.id.pause_btn:
                if (isPlayingClip){
                    if (!isPausedClip){
                        //Pause clip
                        mediaPlayer.pause();
                        pausedPos = mediaPlayer.getCurrentPosition();

                        timerHandler.removeCallbacks(updateTimerThread);
                        buttonConfigure("pause");
                        isPausedClip = true;
                    }
                }
                break;

            //STOP BUTTON
            case R.id.stop_rec_btn:
                seekBar.setProgress(0);
                seekbarToggle(true);
                isPausedClip = false;

                if (isRecording){
                    amps = new ArrayList<>();
                    amps = VisualizerView.amplitudesList;

                    timerTV.setVisibility(View.INVISIBLE);

                    //Save out timer text to var
                    timerCountStr = timerTV.getText().toString().trim();

                    isRecording = false;
                    mediaRecorder.stop();
                    renameDialog();
                    buttonConfigure("stop");

                    //Stop Timer
                    timerHandler.removeCallbacks(updateTimerThread);
                    visualizerHandler.removeCallbacks(updateVisualizer);
                    visualizerView.clear();
                }

                if (isPlayingClip){
                    visualizerView.clear();
                    buttonConfigure("stop");
                    seekBar.setMax(mediaPlayer.getDuration());
                    seekBar.setProgress(0);
                    mediaPlayer.pause();
                    initializeSeekBar();
                    timerHandler.removeCallbacks(updateTimerThread);
                    isPlayingClip = false;
                }
                break;

            case R.id.rec_edit_title:
                renameTitleDialog();
                break;
        }
    }

    private void seekbarToggle(final boolean b) {
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return b;
            }
        });
    }

    protected void initializeSeekBar(){
        seekBar.setMax(mediaPlayer.getDuration()/mInterval);
        runnable = new Runnable() {
            @Override
            public void run() {
                if(mediaPlayer!=null && isPlayingClip){
                    int mCurrentPosition = mediaPlayer.getCurrentPosition()/mInterval; // In milliseconds
                    updatePlaybackTime(mCurrentPosition);
                    seekBar.setProgress(mCurrentPosition);
                }
                handler.postDelayed(runnable,mInterval);
            }
        };
        handler.postDelayed(runnable,mInterval);
    }

    private void updatePlaybackTime(int currentTime){
        int secs= (int) (currentTime/100);
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
        String time = minStr+":"+ secStr;
        timerTV.setText(time);

        totalTimeTV.setText(DataUtils.totalPlaybackTime(mediaPlayer.getDuration()/10));
        currentTimeTV.setText(time);

    }

    private void setupMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(pathSave);

        updateVisualizer.run();
    }

    private void permCheck(){
        if (checkForPermisionFromDevice()){
        } else{
            requestPermissions();
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(getActivity(), new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getContext(), "Permissions Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private boolean checkForPermisionFromDevice(){
        int writeToExternalStorage = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int recordAudioResult = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO);
        return writeToExternalStorage == PackageManager.PERMISSION_GRANTED &&
                recordAudioResult == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RecordFragment.RecorderListener){
            mListener = (RecordFragment.RecorderListener) context;
        }
    }

    //Interface
    public interface RecorderListener {
        void saveAudio(String title, String size, String date, String filePath, String timerCount);
        void updateTitle(String title, String filepath);
    }

    //Name/Save Dialog
    private void renameDialog(){
        titleEditBtn.setVisibility(View.VISIBLE );
        final SimpleDateFormat df= new SimpleDateFormat("MMddyyyyHHmmss");
        final Date d = new Date(System.currentTimeMillis());
        final String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.custom_rename_dialog);

        MobileAds.initialize(getActivity(), "ca-app-pub-4730800757487119/3808060565");
        AdView adView = dialog.findViewById(R.id.rename_adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("CBED1A66CE53567A6EE870CB0F59D107")
                .build();
        adView.loadAd(adRequest);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        final EditText et = dialog.findViewById(R.id.custom_title_et);
        Button btn = dialog.findViewById(R.id.custom_title_btn);

        ImageView cancel = dialog.findViewById(R.id.dialog_cancel_iv);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                titleTV.setText(df.format(d) + ".mp3");
                File file = new File(pathSave);

                double sz = file.length()/1024.0;
                DecimalFormat dec = new DecimalFormat("#.00");
                file_size = Double.parseDouble(dec.format(sz));

                dateTV.setText(DataUtils.getCurDate());
                durationTV.setText(timerCountStr);
                dateTV.setText(DataUtils.getCurDate());
                fileSizeTV.setText(file_size + " KB");

                mListener.saveAudio("untitled" + ".mp3", file_size + " KB", DataUtils.getCurDate(), pathSave, timerCountStr);
                if (mediaPlayer != null){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioTitle = String.valueOf(et.getText());
                if (!audioTitle.isEmpty()) {
                    titleTV.setText(audioTitle + ".mp3");

                    String sep = File.separator; // Use this instead of hardcoding the "/"
                    String folder = "VoiceRecorderTL";
                    String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                    File dir = new File(extStorageDirectory + sep + folder);

                    if (dir.exists()) {
                        File from = new File(Environment.getExternalStorageDirectory().toString() + sep + folder + sep + date + "_" + ".mp3");
                        File to = new File(Environment.getExternalStorageDirectory().toString() + sep + folder + sep + date + "_" + audioTitle + ".mp3");
                        if (from.exists()) {
                            from.renameTo(to);
                            pathSave = String.valueOf(to);
                            File file = new File(pathSave);

                            double sz = file.length()/1024.0;
                            DecimalFormat df = new DecimalFormat("#.00");
                            file_size = Double.parseDouble(df.format(sz));

                            dateTV.setText(DataUtils.getCurDate());
                            titleTV.setText(audioTitle + ".mp3");
                            durationTV.setText(timerCountStr);
                            dateTV.setText(DataUtils.getCurDate());
                            fileSizeTV.setText(file_size + " KB");

                            mListener.saveAudio(audioTitle  + ".mp3", file_size + " KB", DataUtils.getCurDate(), pathSave, timerCountStr);
                            dialog.dismiss();
                        }
                        dialog.dismiss();
                    }
                } else{
                    et.setError("Please name the audio file");
                }
            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void renameTitleDialog(){
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.custom_rename_dialog);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        final EditText et = dialog.findViewById(R.id.custom_title_et);
        Button btn = dialog.findViewById(R.id.custom_title_btn);

        ImageView cancel = dialog.findViewById(R.id.dialog_cancel_iv);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioTitle = String.valueOf(et.getText());
                if (!audioTitle.isEmpty()) {
                    titleTV.setText(audioTitle + ".mp3");
                    //update audiolist new title
                    mListener.updateTitle(audioTitle + ".mp3", pathSave);
                    dialog.dismiss();
                } else{
                    et.setError("Please name the audio file");
                }
            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }


    //Runnables
    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (isRecording){
                int x = mediaRecorder.getMaxAmplitude();
                visualizerView.addAmplitude(x); // update the VisualizeView
                visualizerView.invalidate(); // refresh the VisualizerView
                // update in 40 milliseconds
                visualizerHandler.postDelayed(this, REPEAT_INTERVAL);
            } else {
                visualizerHandler.removeCallbacks(updateVisualizer);
            }
        }
    };

    Runnable updateTimerThread = new Runnable() {
        @Override
        public void run() {
            timeInMil = SystemClock.uptimeMillis() - startTime;
            updateTime = timeSwapBuff+timeInMil;
            int secs= (int) (updateTime/1000);
            int mins = secs/60;
            secs%=60;
            int millisecs=(int)updateTime%1000;
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

            String time = minStr+":"+ secStr;
            timerTV.setText(time);
            timerHandler.postDelayed(this, 0);
        }
    };

    private void buttonConfigure(String keyword){
        switch (keyword) {
            case "stop":
                stopRecBtn.setEnabled(true);
                stopRecBtn.setImageResource(R.drawable.stopped);
                playAudioBtn.setEnabled(true);
                playAudioBtn.setImageResource(R.drawable.play_unpressed);
                recAudioBtn.setEnabled(true);
                recAudioBtn.setImageResource(R.drawable.rec_unpressed1);
                pauseBtn.setEnabled(true);
                pauseBtn.setImageResource(R.drawable.paused);
                break;
            case "pause":
                stopRecBtn.setEnabled(true);
                stopRecBtn.setImageResource(R.drawable.stop_unpressed);
                recAudioBtn.setEnabled(true);
                recAudioBtn.setImageResource(R.drawable.rec_unpressed1);
                playAudioBtn.setEnabled(true);
                playAudioBtn.setImageResource(R.drawable.play_unpressed);
                pauseBtn.setEnabled(false);
                pauseBtn.setImageResource(R.drawable.paused);
                break;
            case "play_base":
                stopRecBtn.setEnabled(true);
                stopRecBtn.setImageResource(R.drawable.stop_unpressed);
                recAudioBtn.setEnabled(false);
                recAudioBtn.setImageResource(R.drawable.rec_unpressed1);
                playAudioBtn.setEnabled(true);
                playAudioBtn.setImageResource(R.drawable.playing);
                pauseBtn.setEnabled(true);
                pauseBtn.setImageResource(R.drawable.pause_unpressed);
                break;
            case "play_active":
                stopRecBtn.setEnabled(false);
                stopRecBtn.setImageResource(R.drawable.stopped);
                playAudioBtn.setEnabled(true);
                playAudioBtn.setImageResource(R.drawable.play_unpressed);
                recAudioBtn.setEnabled(true);
                recAudioBtn.setImageResource(R.drawable.rec_unpressed);
                pauseBtn.setEnabled(false);
                pauseBtn.setImageResource(R.drawable.paused);
                break;
            case "rec_base":
                stopRecBtn.setEnabled(true);
                stopRecBtn.setImageResource(R.drawable.stop_unpressed);
                playAudioBtn.setEnabled(false);
                playAudioBtn.setImageResource(R.drawable.playing);
                recAudioBtn.setEnabled(true);
                recAudioBtn.setImageResource(R.drawable.recording1);
                pauseBtn.setEnabled(false);
                pauseBtn.setImageResource(R.drawable.paused);
                break;
            case "rec_active":
                stopRecBtn.setEnabled(true);
                stopRecBtn.setImageResource(R.drawable.stopped);
                playAudioBtn.setEnabled(true);
                playAudioBtn.setImageResource(R.drawable.play_unpressed);
                recAudioBtn.setEnabled(true);
                recAudioBtn.setImageResource(R.drawable.rec_unpressed1);
                pauseBtn.setEnabled(true);
                pauseBtn.setImageResource(R.drawable.paused);
                break;
            case "initial":
                stopRecBtn.setEnabled(false);
                stopRecBtn.setImageResource(R.drawable.stopped);
                playAudioBtn.setEnabled(false);
                playAudioBtn.setImageResource(R.drawable.playing);
                pauseBtn.setEnabled(false);
                pauseBtn.setImageResource(R.drawable.paused);
                break;
        }
    }

}
