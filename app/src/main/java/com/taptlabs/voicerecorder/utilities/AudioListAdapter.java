package com.taptlabs.voicerecorder.utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.taptlabs.voicerecorder.R;
import com.taptlabs.voicerecorder.fragments.AudioListFragment;
import com.taptlabs.voicerecorder.objects.Audio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Audio> audioList;

    private MediaPlayer mediaPlayer;

    private boolean isPlayingClip = false;
    private boolean isStoppedClip = false;

    public Handler handler;
    public Runnable runnable;

    private int mInterval = 10;

    public AdapterListener mListener;

    public AudioListAdapter(Context context, ArrayList<Audio> audioList) {
        this.context = context;
        this.audioList = audioList;
    }

    @NonNull
    @Override
    public AudioListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the custom layout
        View view = inflater.inflate(R.layout.audio_item, viewGroup, false);
        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AudioListAdapter.ViewHolder viewHolder, int i) {
        Audio audio = audioList.get(i);
        TextView titleTV = viewHolder.audioTitle;
        titleTV.setText(audio.getTitle());
        TextView dateTV = viewHolder.audioDate;
        dateTV.setText(audio.getDate());
        TextView sizeTV = viewHolder.audioSize;
        sizeTV.setText(audio.getSize());
        TextView lengthTV = viewHolder.audioLendth;
        lengthTV.setText(audio.getClipLength());
    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

    public void setOnItemClickListener(AdapterListener listener) {
        mListener = (AdapterListener) listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView audioTitle;
        public TextView audioDate;
        public TextView audioSize;
        public TextView audioLendth;
        public TextView totalTV;
        public TextView currentTV;
        public ImageView audioCard;
        public SeekBar seekbar;
        public ImageView playBtn, stopBtn, menuBtn;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);

            handler = new Handler();

            totalTV = itemView.findViewById(R.id.total_card_time_tv);
            currentTV = itemView.findViewById(R.id.current_card_time_tv);
            audioCard = itemView.findViewById(R.id.card_iv);
            audioTitle = itemView.findViewById(R.id.card_title);
            audioDate = itemView.findViewById(R.id.card_date);
            audioLendth = itemView.findViewById(R.id.card_length);
            audioSize = itemView.findViewById(R.id.card_size);
            seekbar = itemView.findViewById(R.id.card_seekBar);
            seekbar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            seekbar.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

            playBtn = itemView.findViewById(R.id.card_play_btn);
            stopBtn = itemView.findViewById(R.id.card_stop_btn);
            menuBtn = itemView.findViewById(R.id.card_menu_btn);

            audioCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION){
                        mListener.audioNav(position);
                    }
                }
            });

            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String filepath = audioList.get(getAdapterPosition()).getFilePath();
                    if (filepath != null){
                        if (mediaPlayer != null){
                            mediaPlayer.stop();
                            mediaPlayer.release();
                        }
                        mediaPlayer = new MediaPlayer();
                        try {
                            mediaPlayer.setDataSource(filepath);
                            mediaPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    seekbar.setMax(mediaPlayer.getDuration());
                                    mediaPlayer.start();
                                    isPlayingClip = true;
                                    initializeSeekBar();
                                }
                            });

                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    mediaPlayer.pause();
                                    isPlayingClip = false;
                                }
                            });

                            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                                    if (b) {
                                        mediaPlayer.seekTo(progress * mInterval);
                                    }
                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {
                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {
                                }
                            });

                        }

                    }
                }
            });

            stopBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                        mediaPlayer.stop();
                        initializeSeekBar();
                    }
                }
            });

            menuBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    menuDialog(position);
                }
            });
        }

        protected void initializeSeekBar(){
            seekbar.setMax(mediaPlayer.getDuration()/mInterval);
            runnable = new Runnable() {
                @Override
                public void run() {
                    if(mediaPlayer!=null && isPlayingClip){
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
    }

    private void menuDialog(final int pos){

        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.list_menu_dialog);

        MobileAds.initialize(context, "ca-app-pub-4730800757487119/3808060565");
        AdView adView = dialog.findViewById(R.id.menu_adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("CBED1A66CE53567A6EE870CB0F59D107")
                .build();
        adView.loadAd(adRequest);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        ImageView cancel = dialog.findViewById(R.id.list_menu_cancel_btn);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        LinearLayout deleteLL = dialog.findViewById(R.id.list_menu_delete_ll);
        deleteLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //confirm deletetion
                dialog.dismiss();
                DataUtils.confirm(pos, audioList, context);
            }
        });

        LinearLayout shareLL = dialog.findViewById(R.id.list_menu_share_ll);
        shareLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Share Clip
                DataUtils.shareClip(pos, audioList, context);
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }


    public interface AdapterListener{
        void audioNav(int pos);
    }
}
