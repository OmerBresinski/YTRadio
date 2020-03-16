package com.example.ytr;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.squareup.picasso.Picasso;

import java.util.List;

public class YoutubeAdapter extends RecyclerView.Adapter<YoutubeAdapter.MyViewHolder> {

    private Context mContext;
    private List<VideoItem> mVideoList;

    public class MyViewHolder extends RecyclerView.ViewHolder{

        public ImageView thumbnail;
        public TextView video_title, video_id, video_description;
        public RelativeLayout video_view;

        public MyViewHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.video_thumbnail);
            video_title = view.findViewById(R.id.video_title);
            video_id = view.findViewById(R.id.video_id);
            video_description = view.findViewById(R.id.video_description);
            video_view = view.findViewById(R.id.video_view);
        }
    }

    public YoutubeAdapter(Context mContext, List<VideoItem> mVideoList) {
        this.mContext = mContext;
        this.mVideoList = mVideoList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.video_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final VideoItem singleVideo = mVideoList.get(position);
        holder.video_id.setText("Video ID : "+singleVideo.getId()+" ");
        holder.video_title.setText(singleVideo.getTitle());
        holder.video_description.setText(singleVideo.getDescription());
        Picasso.with(mContext)
                .load(singleVideo.getThumbnailURL())
                .resize(480,270)
                .centerCrop()
                .into(holder.thumbnail);
        holder.video_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.player.seekToMillis(0);
                MainActivity.player.loadVideo(singleVideo.getId());
                MainActivity.playbackListener.setVideoSource(singleVideo.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mVideoList.size();
    }
}