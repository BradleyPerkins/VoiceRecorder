package com.taptlabs.voicerecorder.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.taptlabs.voicerecorder.R;
import com.taptlabs.voicerecorder.objects.Audio;
import com.taptlabs.voicerecorder.utilities.AudioListAdapter;

import java.util.ArrayList;


public class AudioListFragment extends Fragment {
    private static final String ARG_AUDIO = "ARG_AUDIO";
    private static final String ARG_POS = "ARG_POS";

    private static ArrayList<Audio> audioList;
    private static int pos;

    public RecyclerView recyclerView;
    public static AudioListAdapter adapter;

    private AudioListener mListener;

    public AudioListFragment() {}

    public static AudioListFragment newInstance(ArrayList<Audio> list, int pos) {
        AudioListFragment fragment = new AudioListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_AUDIO, list);
        args.putInt(ARG_POS, pos);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        audioList = (ArrayList<Audio>) getArguments().getSerializable(ARG_AUDIO);
        pos = getArguments().getInt(ARG_POS);

        adapter = new AudioListAdapter(getContext(), audioList);
        recyclerView = getView().findViewById(R.id.recyclerView);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.getLayoutManager().scrollToPosition(pos);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new AudioListAdapter.AdapterListener() {
            @Override
            public void audioNav(int pos) {
                mListener.audioNav(pos);
            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        super.onAttach(context);
        if (context instanceof AudioListener){
            mListener = (AudioListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface AudioListener {
        void audioNav(int pos);
    }
}
