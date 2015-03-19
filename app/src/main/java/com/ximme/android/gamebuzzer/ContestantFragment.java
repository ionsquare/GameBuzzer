package com.ximme.android.gamebuzzer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * TODO List of things that need to be done
 * Present user with di
 */

public class ContestantFragment extends Fragment {
    private static final String TAG = ContestantFragment.class.getSimpleName();

    // Layout elements
    private Button mBuzz;
    private MediaPlayer player;

    public ContestantFragment(){
        // Empty Constructor, not necessary for anything but seems to be convention
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_contestant, container, false);

        mBuzz = (Button) v.findViewById(R.id.buzz);
        mBuzz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This executes when the Host button is clicked
                player = MediaPlayer.create(getActivity(), R.raw.buzzer);
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);

                player.start();
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                        @Override
                        public void onCompletion(MediaPlayer player){
                            player.release();
                        }
                });
            }
        });

        return v;
    }

}
