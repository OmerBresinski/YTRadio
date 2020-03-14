package com.example.ytr;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener, AppParams {

    private static final int RECOVERY_REQUEST = 1;
    private List<VideoItem> searchResults = new ArrayList<>();
    private int currentUserCount;
    private int currentVideoSeekTime = 0;
    private boolean hasRecentlyPaused = false;
    private String videoSourceID = "";
    private YouTubePlayer player;
    private YouTubePlayerView youTubeView;
    private PlaybackListener playbackListener;
    private Button changeSrcBtn;
    private EditText videoSrcInput;
    private TextView userCounter;
    private FirebaseDatabase database;
    private DatabaseReference videoSrc;
    private DatabaseReference videoTime;
    private DatabaseReference userCount;
    private DatabaseReference playerStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUIViewListeners();
        initServerConnection();
        initYoutubeListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(hasRecentlyPaused){
            currentUserCount++;
            userCount.setValue(currentUserCount);
            hasRecentlyPaused = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentUserCount--;
        hasRecentlyPaused = true;
        userCount.setValue(currentUserCount);
    }

    private void getVideoSeekTime(){
        videoTime.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                currentVideoSeekTime = dataSnapshot.getValue(Integer.class) * 1000;
                player.loadVideo(videoSourceID, currentVideoSeekTime);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Firebase Timer", "Error", databaseError.toException());
            }
        });
    }

    private void initServerConnection(){
        database = FirebaseDatabase.getInstance();
//        chat = database.getReference(PATH_PLAYER_CHAT);
        videoSrc = database.getReference(PATH_PLAYER_SRC);
        playerStatus = database.getReference(PATH_PLAYER_STATUS);
        videoTime = database.getReference(PATH_PLAYER_VIDEO_TIME);
        userCount = database.getReference(PATH_PLAYER_USER_COUNT);
        initVideoSrcListener();
        initPlayerStatusListener();
        initUserCount();
    }

    private void initUserCount(){
        userCount.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUserCount = dataSnapshot.getValue(Integer.class);
                Log.d("First:", ""+currentUserCount);
                userCounter.setText(currentUserCount+"");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase Log:", databaseError.toException());
            }
        });
    }

    private void initPlayerStatusListener(){
        playerStatus.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if(value.equals(PAUSED_VALUE) && player.isPlaying()) {
                    player.pause();
                } else if(value.equals(PLAYING_VALUE) && !player.isPlaying()) {
                    player.play();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase Log:", databaseError.toException());
            }
        });
    }

    private void initVideoSrcListener(){
        videoSrc.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                videoSourceID = value;
                player.loadVideo(value, currentVideoSeekTime);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase Log:", databaseError.toException());
            }
        });
    }

    private void initYoutubeListener(){
        youTubeView = findViewById(R.id.youtube_view);
        youTubeView.initialize(Config.YOUTUBE_API_KEY, this);
        playbackListener = new PlaybackListener(playerStatus, videoSrc, videoTime);
    }

    private void initUIViewListeners(){
        userCounter = findViewById(R.id.user_counter);
        changeSrcBtn = findViewById(R.id.video_src_btn);
        videoSrcInput = findViewById(R.id.video_src_txt);
        changeSrcBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String srcString = videoSrcInput.getText().toString();
                searchOnYoutube(srcString);
            }
        });
    }

    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        if (!wasRestored) {
            this.player = player;
            this.player.setPlaybackEventListener(playbackListener);
            getVideoSeekTime();
            currentUserCount++;
            userCount.setValue(currentUserCount);
        }
    }

    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, RECOVERY_REQUEST).show();
        } else {
            String error = String.format(getString(R.string.player_error), errorReason.toString());
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_REQUEST) {
            getYouTubePlayerProvider().initialize(Config.YOUTUBE_API_KEY, this);
        }
    }

    protected Provider getYouTubePlayerProvider() {
        return youTubeView;
    }

    private void searchOnYoutube(final String keywords){
        new Thread(){
            public void run(){
                YoutubeConnector yc = new YoutubeConnector(MainActivity.this);
                searchResults = yc.search(keywords);
            }
        }.start();
    }
}
