package com.example.ytr;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private int currentVideoSeekTime = 0;
    private String videoSourceID = "";
    public static YouTubePlayer player;
    private YouTubePlayerView youTubeView;
    public static PlaybackListener playbackListener;
    private Button changeSrcBtn;
    private EditText videoSrcInput;
    private FirebaseDatabase database;
    private DatabaseReference videoSrc;
    private DatabaseReference videoTime;
    private DatabaseReference playerStatus;
    private YoutubeAdapter youtubeAdapter;
    private RecyclerView mRecyclerView;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initRecyclerView();
        initUIViewListeners();
        initServerConnection();
        initYoutubeListener();
    }

    private void initRecyclerView(){
        mRecyclerView = findViewById(R.id.videos_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        handler = new Handler();

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
        videoSrc = database.getReference(PATH_PLAYER_SRC);
        playerStatus = database.getReference(PATH_PLAYER_STATUS);
        videoTime = database.getReference(PATH_PLAYER_VIDEO_TIME);
        initVideoSrcListener();
        initPlayerStatusListener();
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
        changeSrcBtn = findViewById(R.id.video_src_btn);
        videoSrcInput = findViewById(R.id.video_src_txt);
        changeSrcBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String srcString = videoSrcInput.getText().toString();
                searchOnYoutube(srcString);
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
    }

    private void fillYoutubeVideos(){
        youtubeAdapter = new YoutubeAdapter(getApplicationContext(),searchResults);
        mRecyclerView.setAdapter(youtubeAdapter);
        youtubeAdapter.notifyDataSetChanged();
    }

    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        if (!wasRestored) {
            this.player = player;
            this.player.setPlaybackEventListener(playbackListener);
            getVideoSeekTime();
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
                handler.post(new Runnable(){
                    public void run(){
                        fillYoutubeVideos();
                    }
                });
            }
        }.start();
    }
}
