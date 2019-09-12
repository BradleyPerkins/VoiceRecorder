package com.taptlabs.voicerecorder.fragments;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.taptlabs.voicerecorder.R;
import com.taptlabs.voicerecorder.activities.MainActivity;
import com.taptlabs.voicerecorder.objects.Audio;
import com.taptlabs.voicerecorder.utilities.DataUtils;

import java.io.IOException;
import java.util.ArrayList;


public class PlayerFragment extends Fragment {

    private static final String ARG_AUDIO = "ARG_AUDIO";
    private static final String ARG_POS = "ARG_POS";

    private static int pos;
    private static ArrayList<Audio> audioList;
    private String audioTitle = null;

    private MediaPlayer mediaPlayer;

    private Handler handler;
    private Runnable runnable;


    private ImageView prevBtn;
    private ImageView stopBtn;
    private ImageView playBtn;
    private ImageView pauseBtn;
    private ImageView nextBtn;
    private ImageView titleEditBtn;

    private TextView currentTV;
    private TextView totalTV;
    private TextView titleTV;
    private TextView durationTV;
    private TextView sizeTV;
    private TextView dateTV;

    private SeekBar seekbar;


    private String pathSave = null;
    private boolean isPaused = false;

    private int mInterval = 10;
    private int pausedPos = 0;


    public PlayerFragment() {}

    public static PlayerFragment newInstance(ArrayList<Audio> list, int pos) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_AUDIO, list);
        args.putInt(ARG_POS, pos);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.audio_clip_menu, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments() != null){
            audioList = (ArrayList<Audio>) getArguments().getSerializable(ARG_AUDIO);
            pos = getArguments().getInt(ARG_POS);

            handler = new Handler();

            titleTV = getView().findViewById(R.id.pl_title_tv);
            durationTV = getView().findViewById(R.id.pl_duration_tv);
            sizeTV = getView().findViewById(R.id.pl_size_tv);
            dateTV = getView().findViewById(R.id.pl_date_tv);

            seekbar = getView().findViewById(R.id.pl_seekbar);
            currentTV = getView().findViewById(R.id.pl_current_time_tv);
            totalTV = getView().findViewById(R.id.pl_total_time_tv);

            prevBtn = getView().findViewById(R.id.pl_prev_btn);
            stopBtn = getView().findViewById(R.id.pl_stop_rec_btn);
            playBtn = getView().findViewById(R.id.pl_playback_btn);
            pauseBtn = getView().findViewById(R.id.pl_pause_btn);
            nextBtn = getView().findViewById(R.id.pl_next_btn);
            titleEditBtn = getView().findViewById(R.id.pl_edit_title);

            titleEditBtn.setVisibility(View.VISIBLE);

            titleTV.setText(audioList.get(pos).getTitle());
            durationTV.setText(audioList.get(pos).getClipLength());
            sizeTV.setText(audioList.get(pos).getSize());
            dateTV.setText(audioList.get(pos).getDate());

            seekbar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            seekbar.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
            seekbarToggle(true);

            buttonConfigure("play_ready");

            titleEditBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renameTitleDialog();
                }
            });


            prevBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.prevClip();
                    if (mediaPlayer != null){
                        mediaPlayer.stop();
                    }
                }
            });

            nextBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.nextClip();
                    if (mediaPlayer != null){
                        mediaPlayer.stop();
                    }
                }
            });

            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (audioList.get(pos).getFilePath() != null){
                        buttonConfigure("playing");
                        seekbarToggle(false);

                        if (mediaPlayer != null){
                            //mediaPlayer = new MediaPlayer();
                            mediaPlayer.stop();
                            mediaPlayer.release();
                        }

                        mediaPlayer = new MediaPlayer();
                        try {
                            mediaPlayer.setDataSource(audioList.get(pos).getFilePath());
                            mediaPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //if (mediaPlayer.isPlaying()){
                            if (!isPaused){
                                //Play Clip
                                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {
                                        seekbar.setMax(mediaPlayer.getDuration());
                                        mediaPlayer.start();

                                        initializeSeekBar();
                                    }
                                });
                                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        mediaPlayer.pause();
                                        buttonConfigure("play_ready");
                                        isPaused = false;
                                    }
                                });
                                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

                            } else{
                                //UnPause Clip
                                mediaPlayer.seekTo(pausedPos);
                                mediaPlayer.start();
                                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                                isPaused = false;
                                pauseBtn.setEnabled(true);
                                pauseBtn.setImageResource(R.drawable.pause_unpressed);
                            }
                        //}
                    }
                }
            });

            pauseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mediaPlayer.isPlaying()){
                        if (!isPaused){
                            //Pause clip
                            //seekbarToggle(false);
                            mediaPlayer.pause();
                            pausedPos = mediaPlayer.getCurrentPosition();
                            buttonConfigure("pause");
                            isPaused = true;
                        }
                    }
                }
            });

            stopBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekbar.setProgress(0);
                    seekbarToggle(true);
                    isPaused = false;
                    if (mediaPlayer.isPlaying()){
                        buttonConfigure("stop");
                        seekbar.setMax(mediaPlayer.getDuration());
                        seekbar.setProgress(0);
                        mediaPlayer.pause();
                        initializeSeekBar();
                    }
                }
            });

        }
    }

    private void seekbarToggle(final boolean b) {
        seekbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return b;
            }
        });
    }

    private void buttonConfigure(String keyword){
        switch (keyword) {
            case "stop":
                stopBtn.setEnabled(true);
                stopBtn.setImageResource(R.drawable.stopped);
                playBtn.setEnabled(true);
                playBtn.setImageResource(R.drawable.play_unpressed);
                pauseBtn.setEnabled(true);
                pauseBtn.setImageResource(R.drawable.paused);

                prevNextBtnConfig();

                break;
            case "pause":
                stopBtn.setEnabled(true);
                stopBtn.setImageResource(R.drawable.stop_unpressed);
                playBtn.setEnabled(true);
                playBtn.setImageResource(R.drawable.play_unpressed);
                pauseBtn.setEnabled(false);
                pauseBtn.setImageResource(R.drawable.paused);
                prevNextBtnConfig();
                break;
            case "play_ready":
                stopBtn.setEnabled(false);
                stopBtn.setImageResource(R.drawable.stopped);
                playBtn.setEnabled(true);
                playBtn.setImageResource(R.drawable.play_unpressed);
                pauseBtn.setEnabled(false);
                pauseBtn.setImageResource(R.drawable.paused);

                prevNextBtnConfig();

                break;
            case "playing":
                stopBtn.setEnabled(true);
                stopBtn.setImageResource(R.drawable.stop_unpressed);
                playBtn.setEnabled(false);
                playBtn.setImageResource(R.drawable.playing);
                pauseBtn.setEnabled(true);
                pauseBtn.setImageResource(R.drawable.pause_unpressed);
                prevNextBtnConfig();
                break;
        }
    }

    protected void initializeSeekBar(){
        seekbar.setActivated(true);
        seekbar.setMax(mediaPlayer.getDuration()/mInterval);
        runnable = new Runnable() {
            @Override
            public void run() {
                if(mediaPlayer!=null && mediaPlayer.isPlaying()){
                    int mCurrentPosition = mediaPlayer.getCurrentPosition()/mInterval; // In milliseconds
                    updatePlaybackTime(mCurrentPosition);
                    seekbar.setProgress(mCurrentPosition);
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

        totalTV.setText(DataUtils.totalPlaybackTime(mediaPlayer.getDuration()/10));
        currentTV.setText(time);

    }

    private void prevNextBtnConfig() {
        if (pos > 0){
            prevBtn.setEnabled(true);
            prevBtn.setImageResource(R.drawable.back_unpressed);
        } else {
            prevBtn.setEnabled(false);
            prevBtn.setImageResource(R.drawable.back);
        }
        if (pos < (audioList.size() - 1)){
            nextBtn.setEnabled(true);
            nextBtn.setImageResource(R.drawable.next_unpressed);
        } else {
            nextBtn.setEnabled(false);
            nextBtn.setImageResource(R.drawable.next);
        }
    }

    private void renameTitleDialog(){
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.custom_rename_dialog);

        MobileAds.initialize(getContext(), "ca-app-pub-4730800757487119/3808060565");
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
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioTitle = String.valueOf(et.getText());
                if (!audioTitle.isEmpty()) {
                    titleTV.setText(audioTitle + ".mp3");
                    //update audiolist new title
                    mListener.updateTitle(audioTitle + ".mp3", audioList.get(pos).getFilePath());
                    dialog.dismiss();
                } else{
                    et.setError("Please name the audio file");
                }
            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }


    private PlayerFragment.PlayerListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PlayerFragment.PlayerListener){
            mListener = (PlayerFragment.PlayerListener) context;
        }
    }

    //Interface
    public interface PlayerListener {
        void prevClip();
        void nextClip();
        void updateTitle(String title, String filepath);
    }
}
