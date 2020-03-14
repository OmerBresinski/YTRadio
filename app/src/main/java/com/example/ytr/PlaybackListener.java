package com.example.ytr;
import android.util.Log;

import com.google.android.youtube.player.YouTubePlayer.PlaybackEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;

public class PlaybackListener implements PlaybackEventListener {

    private Double timerValue;
    private DatabaseReference status;
    private DatabaseReference videoSrc;
    private DatabaseReference videoTime;
    private Timer videoTimer = new Timer();

    public PlaybackListener(DatabaseReference status, DatabaseReference videoSrc, DatabaseReference videoTime){
        this.status = status;
        this.videoSrc = videoSrc;
        this.videoTime = videoTime;
        videoTime.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                timerValue = dataSnapshot.getValue(Integer.class) * 1.0;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Firebase Timer", "Error", databaseError.toException());
            }
        });
    }

    @Override
    public void onPlaying() {
        status.setValue("Playing");
        this.videoTimer = new Timer();
        startVideoTimer();
    }

    @Override
    public void onPaused() {
        status.setValue("Paused");
        videoTimer.cancel();
    }

    @Override
    public void onStopped() {
        status.setValue("Stopped");
        videoTime.setValue(0);
        videoTimer.cancel();
    }

    @Override
    public void onBuffering(boolean b) {
//        status.setValue("Buffering: "+ b);
    }

    @Override
    public void onSeekTo(int i) {
        this.timerValue = i / 1000.0;
        videoTimer.cancel();
        status.setValue("Seek to:"+ i);
        videoTime.setValue(timerValue);
        Log.d("Seek to:", i+"");
    }

    private void startVideoTimer(){
        videoTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                timerValue++;
                videoTime.setValue(timerValue);
            }
        },0,1000);
    }

    public void setVideoSource(String videoSrc){
        this.videoSrc.setValue(videoSrc);
    }
}
