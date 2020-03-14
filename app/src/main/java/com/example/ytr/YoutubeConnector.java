package com.example.ytr;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;

public class YoutubeConnector {

    private YouTube youtube;
    private YouTube.Search.List query;
    public static final String KEY = Config.YOUTUBE_API_KEY;
    public static final String PACKAGENAME = "com.example.ytr";
    public static final String SHA1 = "2F:64:69:79:C3:50:12:DF:22:5B:2D:A4:E3:C7:57:2D:B8:BB:72:09";
    private static final long MAXRESULTS = 25;

    public YoutubeConnector(Context context) {
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {

            @Override
            public void initialize(HttpRequest request) throws IOException {
                request.getHeaders().set("X-Android-Package", PACKAGENAME);
                request.getHeaders().set("X-Android-Cert",SHA1);
            }
        }).setApplicationName("SearchYoutube").build();

        try {
            query = youtube.search().list("id,snippet");
            query.setKey(KEY);
            query.setType("video");
            query.setFields("items(id/kind,id/videoId,snippet/title,snippet/description,snippet/thumbnails/high/url)");

        } catch (IOException e) {
            Log.d("YC", "Could not initialize: " + e);
        }
    }

    public List<VideoItem> search(String keywords) {
        query.setQ(keywords);
        query.setMaxResults(MAXRESULTS);
        try {
            SearchListResponse response = query.execute();
            List<SearchResult> results = response.getItems();
            List<VideoItem> items = new ArrayList<>();
            if(results != null){
                items = setItemsList(results.iterator());
            }
            Log.d("Items:", items.get(0).getId());
            return items;

        } catch (IOException e) {
            Log.d("YC", "Could not search: " + e);
            return null;
        }
    }

    private static List<VideoItem> setItemsList(Iterator<SearchResult> iteratorSearchResults) {
        List<VideoItem> tempSetItems = new ArrayList<>();
        if (!iteratorSearchResults.hasNext()) {
            Log.d("Search", "Could not find anything.");
        }
        while (iteratorSearchResults.hasNext()) {
            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();
            if (rId.getKind().equals("youtube#video")) {
                VideoItem item = new VideoItem();
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getHigh();
                item.setId(singleVideo.getId().getVideoId());
                item.setTitle(singleVideo.getSnippet().getTitle());
                item.setDescription(singleVideo.getSnippet().getDescription());
                item.setThumbnailURL(thumbnail.getUrl());
                tempSetItems.add(item);
            }
        }
        return tempSetItems;
    }
}