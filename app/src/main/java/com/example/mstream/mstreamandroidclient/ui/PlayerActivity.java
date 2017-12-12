package com.example.mstream.mstreamandroidclient.ui;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mstream.mstreamandroidclient.MetadataObject;
import com.example.mstream.mstreamandroidclient.MstreamQueueObject;
import com.example.mstream.mstreamandroidclient.QueueManager;
import com.example.mstream.mstreamandroidclient.R;

import java.util.HashMap;
import java.util.Map;

//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;

// Активити плеера

public class PlayerActivity extends AppCompatActivity {

    // SQLite DB
    //private DatabaseHelper mstreamDB;

    private BroadcastReceiver syncReceiver;

    // Это место для хранения объектов метаданных для асинхронного поиска
    Map<Long, MetadataObject> downloadQueue = new HashMap<Long, MetadataObject>();


    // Media Controls!
    private MediaBrowserCompat mediaBrowser;

    // Main browser
    //private BaseBrowserAdapter baseBrowserAdapter;
    // Playlist Adapter

    //private QueueAdapter queueAdapter;

    //private ServerListAdapter serverListAdapter;

    // Кнопки плеера
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private ImageButton previousButton;

    // Кнопки для очереди
    private ImageButton shouldLoop;
    private ImageButton moreQueueOptions;

    // оставшееся время
    private TextView timeLeftText;


    // Seek bar
    private SeekBar seekBar;
    private int currentPosition;
    private Handler seekHAndler = new Handler();
    private boolean handledLock = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Database
        //mstreamDB = new DatabaseHelper(this);

//        // Servers
//        ServerStore.loadServers();
//
//        // Sync Settings
//        SyncSettingsStore.loadSyncSettings();
//
//
//        // if sync settings are not set up
//        if(SyncSettingsStore.storagePath == null || SyncSettingsStore.storagePath.isEmpty()){
//            File mStreamDir = new File( this.getExternalFilesDir("mstream-storage").toString() );
//
//            // Check if dir exists
//            if(! mStreamDir.exists()){
//                mStreamDir.mkdirs();
//            }
//
//            SyncSettingsStore.setSyncPath( mStreamDir.toString());
//        }


        // Кнопка плай/пауза
        playPauseButton = (ImageButton) findViewById(R.id.play_pause);
        playPauseButton.setEnabled(true);
        playPauseButton.setOnClickListener(playPauseButtonListener);

        // кнопка следующая песня
        nextButton = (ImageButton) findViewById(R.id.next_song);
        nextButton.setEnabled(true);
        nextButton.setOnClickListener(nextButtonListener);

        // конпка предыдущая песня
        previousButton = (ImageButton) findViewById(R.id.previous_song);
        previousButton.setEnabled(true);
        previousButton.setOnClickListener(previousButtonListener);

//        // Loop
//        shouldLoop = (ImageButton) this.findViewById(R.id.should_loop);
//        shouldLoop.setOnClickListener(loopButtonListener);
//
//        moreQueueOptions = (ImageButton) this.findViewById(R.id.queue_more_options);
//        moreQueueOptions.setEnabled(true);
//        moreQueueOptions.setOnClickListener(moreQueueOptionsListner);


        // Time left text
        // timeLeftText = (TextView) findViewById(R.id.time_left_text);

        // Sync callback
//        syncReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                //check if the broadcast message is for our enqueued download
//                Long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
//
//                // Check if id is in downloadQueueManager
//                MetadataObject moo = downloadQueue.get(referenceId);
//
//                if(moo == null){
//                    return;
//                }
//
//                // Set the local file path
//                moo.setLocalFile(moo.getDownloadingToPath());
//                moo.setSyncing(false);
//                moo.setDownloadingToPath(null);
//
//                QueueManager.updateHashToLocalFile(moo.getSha256Hash(), moo.getLocalFile());
//
//                // update DB
//                mstreamDB.addFileToDataBase(moo);
//
//                // Remove from queue
//                downloadQueue.remove(referenceId);
//
//                updateQueueView();
//            }
//        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(syncReceiver, filter);

        // Seekbar
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setPadding(0, 0, 0, 0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //timeLeftText.setText(DateUtils.formatElapsedTime((seekBar.getMax() - progress) / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekMedia(seekBar.getProgress());
//                scheduleSeekbarUpdate();
            }
        });
        seekBar.setMax(192470);






//        // Старт аудио сервиса, не уверен, что он нам вообще нужен
//        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MStreamAudioService.class),
//                new MediaBrowserCompat.ConnectionCallback() {
//                    @Override
//                    public void onConnected() {
//                        try {
//                            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
//                            // This is what gives us access to everything!
//                            MediaControllerCompat controller = new MediaControllerCompat(PlayerActivity.this, token);
//                            setMediaController(, controller);
//                            EventBus.getDefault().postSticky(new MediaControllerConnectedEvent());
//                        } catch (RemoteException e) {
//                            Log.e(BaseActivity.class.getSimpleName(), "Error creating controller", e);
//                        }
//                    }
//
//                    @Override
//                    public void onConnectionSuspended() {
//                        EventBus.getDefault().removeStickyEvent(MediaControllerConnectedEvent.class);
//                    }
//
//                    @Override
//                    public void onConnectionFailed() {
//                        EventBus.getDefault().removeStickyEvent(MediaControllerConnectedEvent.class);
//                    }
//                }, null);
//
//        EventBus.getDefault().register(this);
//        MediaControllerCompat controller = getSupportMediaController();
//        if (controller != null) {
//            onConnected();
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
       //mediaBrowser.connect();

//        // Queue Adapter
//        RecyclerView queueView = (RecyclerView) findViewById(R.id.queue_recycler);
//        queueView.setLayoutManager(new LinearLayoutManager(this));
//        queueAdapter = new QueueAdapter(new ArrayList<MstreamQueueObject>(), new QueueAdapter.OnClickQueueItem() {
//            @Override
//            public void onQueueClick(MediaSessionCompat.QueueItem item, int itemPos){
//                // Go To Song
//                // TODO
//                QueueManager.goToQueuePosition(itemPos);
//                // QueueManager.updateMetadata();
//
//                // Play
//                playMedia();
//
//                // update view
//                queueAdapter.notifyDataSetChanged();
//            }
//        });
//        queueAdapter.clear();
//        queueAdapter.add(QueueManager.getIt()); // TODO
//        queueView.setAdapter(queueAdapter);
//
//        // TODO: Set color for shuffle and repeat button
//        if(QueueManager.getShouldLoop()){
//            shouldLoop.setColorFilter(Color.rgb(102,132,178));
//        }else{
//            shouldLoop.setColorFilter(Color.rgb(255,255,255));

        }





    @Override
    protected void onStop() {
        super.onStop();
        //mediaBrowser.disconnect();
    }

    @Override
    public void onBackPressed() {
        // Если drawer открыт, когда пользователь нажимает «Назад», сначала закройте его
        DrawerLayout drawer = ((DrawerLayout) findViewById(R.id.drawer_layout));
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // Если он уже закрыт, завершите операцию.
            super.onBackPressed();
        }
    }


    //public MediaBrowserCompat getMediaBrowser() {
    //    return mediaBrowser;
    //}

//
//    public void getPlaylists(){
//        String loginURL = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath("playlist").appendPath("getall").build().toString();
//        Request request = new Request.Builder()
//                .url(loginURL)
//                .addHeader("x-access-token", ServerStore.currentServer.getServerJWT())
//                .build();
//
//        // Callback
//        Callback loginCallback = new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                toastIt("Failed To Connect To Server");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if(response.code() != 200){
//                    toastIt("Files Failed");
//                }else{
//                    // Get the vPath and JWT
//                    try {
//                        JSONArray contents = new JSONArray(response.body().string());
//                        // JSONArray contents = responseJson.getJSONArray("albums");
//                        // final ArrayList<BaseBrowserItem> serverFileList = new ArrayList<>();
//                        baseBrowserList.clear();
//                        backupBrowserList.clear();
//
//                        for (int i = 0; i < contents.length(); i++) {
//                            String playlist = contents.getString(i);
//                            JSONObject responseJson = new JSONObject(playlist);
//                            playlist = responseJson.getString("name");
//
//                            // For directories use the relative directory path
//                            baseBrowserList.add(new BaseBrowserItem.Builder("playlist", playlist, playlist).build());
//                            backupBrowserList.add(new BaseBrowserItem.Builder("playlist", playlist, playlist).build());
//                        }
//
//                        addIt(baseBrowserList);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        toastIt("Failed to decoded server response. WTF");
//                    }
//                }
//            }
//        };
//
//        // Make call
//        OkHttpClient okHttpClient = ((MStreamApplication) getApplicationContext()).getOkHttpClient();
//        okHttpClient.newCall(request).enqueue(loginCallback);
//    }
//
//    public void getAlbums(){
//        String loginURL = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath("db").appendPath("albums").build().toString();
//        Request request = new Request.Builder()
//                .url(loginURL)
//                .addHeader("x-access-token", ServerStore.currentServer.getServerJWT())
//                .build();
//
//        // Callback
//        Callback loginCallback = new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                toastIt("Failed To Connect To Server");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if(response.code() != 200){
//                    toastIt("Files Failed");
//                }else{
//                    // Get the vPath and JWT
//                    try {
//                        JSONObject responseJson = new JSONObject(response.body().string());
//                        JSONArray contents = responseJson.getJSONArray("albums");
//                        // final ArrayList<BaseBrowserItem> serverFileList = new ArrayList<>();
//                        baseBrowserList.clear();
//                        backupBrowserList.clear();
//
//
//                        for (int i = 0; i < contents.length(); i++) {
//                            String artist = contents.getString(i);
//
//                            // For directories use the relative directory path
//                            baseBrowserList.add(new BaseBrowserItem.Builder("album", artist, artist).build());
//                            backupBrowserList.add(new BaseBrowserItem.Builder("album", artist, artist).build());
//                        }
//
//                        addIt(baseBrowserList);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        toastIt("Failed to decoded server response. WTF");
//                    }
//                }
//            }
//        };
//
//        // Make call
//        OkHttpClient okHttpClient = ((MStreamApplication) getApplicationContext()).getOkHttpClient();
//        okHttpClient.newCall(request).enqueue(loginCallback);
//    }
//
//    public void getArtists(){
//        String loginURL = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath("db").appendPath("artists").build().toString();
//        Request request = new Request.Builder()
//                .url(loginURL)
//                .addHeader("x-access-token", ServerStore.currentServer.getServerJWT())
//                .build();
//
//        // Callback
//        Callback loginCallback = new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                toastIt("Failed To Connect To Server");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if(response.code() != 200){
//                    toastIt("Files Failed");
//                }else{
//                    // Get the vPath and JWT
//                    try {
//                        JSONObject responseJson = new JSONObject(response.body().string());
//                        JSONArray contents = responseJson.getJSONArray("artists");
//                        // final ArrayList<BaseBrowserItem> serverFileList = new ArrayList<>();
//                        baseBrowserList.clear();
//                        backupBrowserList.clear();
//
//
//                        for (int i = 0; i < contents.length(); i++) {
//                            String artist = contents.getString(i);
//
//                            // For directories use the relative directory path
//                            baseBrowserList.add(new BaseBrowserItem.Builder("artist", artist, artist).build());
//                            backupBrowserList.add(new BaseBrowserItem.Builder("artist", artist, artist).build());
//                        }
//
//                        addIt(baseBrowserList);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        toastIt("Failed to decoded server response. WTF");
//                    }
//                }
//            }
//        };
//
//        // Make call
//        OkHttpClient okHttpClient = ((MStreamApplication) getApplicationContext()).getOkHttpClient();
//        okHttpClient.newCall(request).enqueue(loginCallback);
//    }
//
//    public void getArtistsAlbums(String artist){
//        JSONObject jsonObj = new JSONObject();
//        try{
//            jsonObj.put("artist", artist);
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
//        okhttp3.RequestBody body = RequestBody.create(JSON, jsonObj.toString());
//
//        String loginURL = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath("db").appendPath("artists-albums").build().toString();
//        Request request = new Request.Builder()
//                .url(loginURL)
//                .addHeader("x-access-token", ServerStore.currentServer.getServerJWT())
//                .post(body)
//                .build();
//
//        // Callback
//        Callback loginCallback = new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                toastIt("Failed To Connect To Server");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if(response.code() != 200){
//                    toastIt("Files Failed");
//                }else{
//                    // Get the vPath and JWT
//                    try {
//                        JSONObject responseJson = new JSONObject(response.body().string());
//                        JSONArray contents = responseJson.getJSONArray("albums");
//                        // final ArrayList<BaseBrowserItem> serverFileList = new ArrayList<>();
//                        baseBrowserList.clear();
//                        backupBrowserList.clear();
//
//                        for (int i = 0; i < contents.length(); i++) {
//                            String artist = contents.getString(i);
//
//                            // For directories use the relative directory path
//                            baseBrowserList.add(new BaseBrowserItem.Builder("album", artist, artist).build());
//                            backupBrowserList.add(new BaseBrowserItem.Builder("album", artist, artist).build());
//                        }
//
//                        addIt(baseBrowserList);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        toastIt("Failed to decoded server response. WTF");
//                    }
//                }
//            }
//        };
//
//        // Make call
//        OkHttpClient okHttpClient = ((MStreamApplication) getApplicationContext()).getOkHttpClient();
//        okHttpClient.newCall(request).enqueue(loginCallback);
//    }
//
//    public void getAlbumSongs(String album){
//        JSONObject jsonObj = new JSONObject();
//        try{
//            jsonObj.put("album", album);
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
//        okhttp3.RequestBody body = RequestBody.create(JSON, jsonObj.toString());
//
//        String loginURL = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath("db").appendPath("album-songs").build().toString();
//        Request request = new Request.Builder()
//                .url(loginURL)
//                .addHeader("x-access-token", ServerStore.currentServer.getServerJWT())
//                .post(body)
//                .build();
//
//        // Callback
//        Callback loginCallback = new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                toastIt("Failed To Connect To Server");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if(response.code() != 200){
//                    toastIt("Files Failed");
//                }else{
//                    // Get the vPath and JWT
//                    try {
//                        // JSONObject responseJson = new JSONObject(response.body().string());
//                        JSONArray contents = new JSONArray(response.body().string());
//                        // final ArrayList<BaseBrowserItem> serverFileList = new ArrayList<>();
//                        baseBrowserList.clear();
//                        backupBrowserList.clear();
//
//
//                        for (int i = 0; i < contents.length(); i++) {
//                            JSONObject fileJson = contents.getJSONObject(i);
//                            String filepath = fileJson.getString("filepath");
//                            String link;
//                            JSONObject metadata = fileJson.getJSONObject("metadata");
//
//                            // For music we provide the whole URL
//                            // This way the playlist can handle files from multiple servers
//                            // String fileUrl = serverUrl + currentPath + fileJson.getString("name");
//                            String fileUrl = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath(ServerStore.currentServer.getServerVPath()).build().toString();
//                            if(fileUrl.charAt(fileUrl.length() - 1) != '/'){
//                                fileUrl = fileUrl + "/";
//                            }
//                            fileUrl = fileUrl + filepath;
//                            fileUrl = Uri.parse(fileUrl).buildUpon().appendQueryParameter("token", ServerStore.currentServer.getServerJWT()).build().toString();
//
//                            try {
//                                // We need to encode the URL to handle files with special characters
//                                // Thank You stack overflow
//                                URL url = new URL(fileUrl);
//                                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
//                                link = uri.toASCIIString();
//                            } catch (MalformedURLException | URISyntaxException e) {
//                                link = ""; // TODO: Better exception handling
//                            }
//
//
//                            MetadataObject tempMeta = new MetadataObject.Builder(link).filepath(filepath).build();
//
//                            String filename = metadata.getString("filename");
//                            tempMeta.setArtist(metadata.getString("artist"));
//                            tempMeta.setAlbum(metadata.getString("album"));
//                            tempMeta.setTitle(metadata.getString("title"));
//                            tempMeta.setSha256Hash(metadata.getString("hash"));
//                            tempMeta.setYear(metadata.getInt("year"));
//                            tempMeta.setTrack(metadata.getInt("track"));
//                            tempMeta.setAlbumArtUrlViaHash(metadata.getString("album-art"));
//                            tempMeta.setFilename(filename);
//
//                            BaseBrowserItem tempItem = new BaseBrowserItem.Builder("file", link, filename).metadata(tempMeta).build( );
//
//                            baseBrowserList.add(tempItem);
//                            backupBrowserList.add(tempItem);
//                        }
//
//                        addIt(baseBrowserList);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        toastIt("Failed to decoded server response. WTF");
//                    }
//                }
//            }
//        };
//
//        // Make call
//        OkHttpClient okHttpClient = ((MStreamApplication) getApplicationContext()).getOkHttpClient();
//        okHttpClient.newCall(request).enqueue(loginCallback);
//    }
//
//    public void getFiles(String directroy){
//        JSONObject jsonObj = new JSONObject();
//        try{
//            jsonObj.put("dir", directroy);
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
//        okhttp3.RequestBody body = RequestBody.create(JSON, jsonObj.toString());
//
//        String loginURL = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath("dirparser").build().toString();
//        Request request = new Request.Builder()
//                .url(loginURL)
//                .addHeader("x-access-token", ServerStore.currentServer.getServerJWT())
//                .post(body)
//                .build();
//
//        // Callback
//        Callback loginCallback = new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                toastIt("Failed To Connect To Server");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if(response.code() != 200){
//                    toastIt("Files Failed");
//                }else{
//                    // Get the vPath and JWT
//                    try {
//                        JSONObject responseJson = new JSONObject(response.body().string());
//                        JSONArray contents = responseJson.getJSONArray("contents");
//                        // final ArrayList<BaseBrowserItem> serverFileList = new ArrayList<>();
//                        baseBrowserList.clear();
//                        backupBrowserList.clear();
//
//                        String currentPath = responseJson.getString("path");
//
//                        for (int i = 0; i < contents.length(); i++) {
//                            JSONObject fileJson = contents.getJSONObject(i);
//                            String type = fileJson.getString("type");
//                            String link;
//                            if (type.equals("directory")) {
//                                // For directories use the relative directory path
//                                String name = fileJson.getString("name");
//                                link = currentPath + name + "/";
//                                baseBrowserList.add(new BaseBrowserItem.Builder("directory", link, name).build());
//                                backupBrowserList.add(new BaseBrowserItem.Builder("directory", link, name).build());
//
//                            } else {
//                                String name = fileJson.getString("name");
//
//                                // For music we provide the whole URL
//                                // This way the playlist can handle files from multiple servers
//                                // String fileUrl = serverUrl + currentPath + fileJson.getString("name");
//                                String fileUrl = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath(ServerStore.currentServer.getServerVPath()).build().toString();
//                                if(fileUrl.charAt(fileUrl.length() - 1) != '/'){
//                                    fileUrl = fileUrl + "/";
//                                }
//                                fileUrl = fileUrl + currentPath  + name;
//                                fileUrl = Uri.parse(fileUrl).buildUpon().appendQueryParameter("token", ServerStore.currentServer.getServerJWT()).build().toString();
//
//                                try {
//                                    // We need to encode the URL to handle files with special characters
//                                    // Thank You stack overflow
//                                    URL url = new URL(fileUrl);
//                                    URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
//                                    link = uri.toASCIIString();
//                                } catch (MalformedURLException | URISyntaxException e) {
//                                    link = ""; // TODO: Better exception handling
//                                }
//
//                                MetadataObject tempMeta = new MetadataObject.Builder(link).filename(name).filepath(currentPath + name).build();
//                                BaseBrowserItem tempItem = new BaseBrowserItem.Builder("file", link, name).metadata(tempMeta).build( );
//
//                                backupBrowserList.add(tempItem);
//                                baseBrowserList.add(tempItem);
//                            }
//                        }
//
//                        addIt(baseBrowserList);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        toastIt("Failed to decoded server response. WTF");
//                    }
//                }
//            }
//        };
//
//        // Make call
//        OkHttpClient okHttpClient = ((MStreamApplication) getApplicationContext()).getOkHttpClient();
//        okHttpClient.newCall(request).enqueue(loginCallback);
//    }
//
//    private void getMetadataAndAddToQueue(MetadataObject moo){
//        final MstreamQueueObject mqo = new MstreamQueueObject(moo);
//        mqo.constructQueueItem();
//
//        // Addd to the queue
//        QueueManager.addToQueue4(mqo);
//
//        // Redraw the queue // TODO: There's got to be a better way
//        queueAdapter.clear();
//        queueAdapter.add(QueueManager.getIt());
//
//        // TODO: Is this necessary ???
//        pingQueueListener();
//
//        // If the hash is set, it means we got the metadata already. No need to run this again
//        if(mqo.getMetadata().getSha256Hash() != null && !mqo.getMetadata().getSha256Hash().isEmpty()){
//            // Double check that the file is synced though
//            syncFile(mqo.getMetadata(), false);
//            mqo.constructQueueItem();
//            return;
//        }
//
//
//        // Prepare a the metadata request
//        JSONObject jsonObj = new JSONObject();
//        try{
//            jsonObj.put("filepath", moo.getFilepath());
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
//        okhttp3.RequestBody body = RequestBody.create(JSON, jsonObj.toString());
//
//        String metadataURL = Uri.parse(ServerStore.currentServer.getServerUrl()).buildUpon().appendPath("db").appendPath("metadata").build().toString();
//        Request request = new Request.Builder()
//                .url(metadataURL)
//                .addHeader("x-access-token", ServerStore.currentServer.getServerJWT())
//                .post(body)
//                .build();
//
//        // Callback
//        Callback metadataCallback = new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                toastIt("Failed To Connect To Server");
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if(response.code() != 200){
//                    toastIt("Files Failed");
//                }else{
//                    // Get the vPath and JWT
//                    try {
//                        JSONObject responseJson = new JSONObject(response.body().string());
//                        JSONObject contents = responseJson.getJSONObject("metadata");
//                        // final ArrayList<BaseBrowserItem> serverFileList = new ArrayList<>();
//                        final MetadataObject mqoMeta = mqo.getMetadata();
//
//                        // TODO: Double check all returned values exist
//
//                        mqoMeta.setSha256Hash(contents.getString("hash"));
//                        mqoMeta.setArtist(contents.getString("artist"));
//                        mqoMeta.setAlbum(contents.getString("album"));
//                        mqoMeta.setTitle(contents.getString("title"));
//                        mqoMeta.setAlbumArtUrlViaHash(contents.getString("album-art"));
//
//                        mqoMeta.setYear(contents.getInt("year"));
//                        mqoMeta.setTrack(contents.getInt("track"));
//
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        toastIt("Failed to decoded server response. WTF");
//                    }
//
//                    // lookup if local copy is available here
//                    syncFile(mqo.getMetadata(), false);
//
//                    mqo.constructQueueItem();
//                    updateQueueView();
//                }
//            }
//        };
//
//        // Make call
//        OkHttpClient okHttpClient = ((MStreamApplication) getApplicationContext()).getOkHttpClient();
//        okHttpClient.newCall(request).enqueue(metadataCallback);
//    }

    private void toastIt(final String toastText){
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(PlayerActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private void updateQueueView(){
//        runOnUiThread(new Runnable() {
//            public void run()
//            {
//                queueAdapter.notifyDataSetChanged();
//            }
//        });
//    }
//
//    private void addIt(final ArrayList<BaseBrowserItem> serverFileList){
//        runOnUiThread(new Runnable() {
//            public void run() {
//                baseBrowserAdapter.clear();
//                baseBrowserAdapter.add(serverFileList);
//            }
//        });
//    }

    //  Next button listener
    private final View.OnClickListener nextButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            goToNextSong();
        }
    };

    // Previous button listener
    private final View.OnClickListener previousButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            goToPreviousSong();
        }
    };

//    // Loop button listener
//    private final View.OnClickListener loopButtonListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            boolean isLoop = QueueManager.toggleShouldLoop();
//            if(isLoop){
//                shouldLoop.setColorFilter(Color.rgb(102,132,178));
//            }else{
//                shouldLoop.setColorFilter(Color.rgb(255, 255, 255));
//            }
//        }
//    };

//    private final View.OnClickListener moreQueueOptionsListner = new View.OnClickListener(){
//        @Override
//        public void onClick(View v){
//            //Creating the instance of PopupMenu
//            PopupMenu popup = new PopupMenu(v.getContext(), moreQueueOptions);
//            //Inflating the Popup using xml file
//            popup.getMenuInflater()
//                    .inflate(R.menu.more_queue_options_menu, popup.getMenu());
//
//            //registering popup with OnMenuItemClickListener
//            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//                @Override
//                public boolean onMenuItemClick(MenuItem item) {
//                    if(item.getTitleCondensed().equals("clear_queue")){
//                        QueueManager.clearQueue();
//                        queueAdapter.clear();
//                        queueAdapter.add(QueueManager.getIt());
//                    }
//
//                    return true;
//                }
//            });
//
//            popup.show(); //showing popup menu
//        }
//    };

    // Play/pause button listener
    private final View.OnClickListener playPauseButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //так было
            //MediaControllerCompat controller = getSupportMediaController()
            //так стало
            MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
            PlaybackStateCompat stateObj = controller.getPlaybackState();
            final int state = stateObj == null ? PlaybackStateCompat.STATE_NONE : stateObj.getState();
            switch (v.getId()) {
                case R.id.play_pause:
                    if (state == PlaybackStateCompat.STATE_PAUSED ||
                            state == PlaybackStateCompat.STATE_STOPPED ||
                            state == PlaybackStateCompat.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackStateCompat.STATE_PLAYING ||
                            state == PlaybackStateCompat.STATE_BUFFERING ||
                            state == PlaybackStateCompat.STATE_CONNECTING) {
                        pauseMedia();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            onPlaybackStateChanged2(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            onMetadataChanged2(metadata);
        }
    };

    private void playMedia() {
        //так было
        //MediaControllerCompat controller = getSupportMediaController()
        //так стало
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia() {
        //так было
        //MediaControllerCompat controller = getSupportMediaController()
        //так стало
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }

    private void seekMedia(int progress) {
        //так было
        //MediaControllerCompat controller = getSupportMediaController()
        //так стало
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
        if (controller != null) {
            controller.getTransportControls().seekTo(progress);
        }
    }

    private void addIt(MediaSessionCompat.QueueItem xxx){
        //так было
        //MediaControllerCompat controller = getSupportMediaController()
        //так стало
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
        Bundle bbb = new Bundle();
        bbb.putString("lol", xxx.getDescription().getMediaUri().toString());

        if (controller != null) {
            controller.getTransportControls().sendCustomAction("addToQueue", bbb );
        }
    }

    private void pingQueueListener(){
        //так было
        //MediaControllerCompat controller = getSupportMediaController()
        //так стало
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);

        if (controller != null) {
            controller.getTransportControls().sendCustomAction("pingQueueListener", null );
        }
    }

    private void goToNextSong(){
        //так было
        //MediaControllerCompat controller = getSupportMediaController()
        //так стало
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
        if (controller != null) {
            controller.getTransportControls().skipToNext();
            //queueAdapter.notifyDataSetChanged();
        }
    }

    private void goToPreviousSong(){
        //так было
        //MediaControllerCompat controller = getSupportMediaController()
        //так стало
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
        if (controller != null) {
            controller.getTransportControls().skipToPrevious();
            //queueAdapter.notifyDataSetChanged();

        }
    }

    public void onConnected() {
        //так было
        //MediaControllerCompat controller = getSupportMediaController()
        //так стало
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
        if (controller != null) {
            onMetadataChanged2(controller.getMetadata());
            onPlaybackStateChanged2(controller.getPlaybackState());
            controller.registerCallback(mediaControllerCallback);
        }
    }

//    @Subscribe(sticky = true)
//    public void onConnectedToMediaController(MediaControllerConnectedEvent e) {
//        // Добавление callback MediaController, чтобы можно перерисовать список при изменении метаданных:
//        //так было
//        //MediaControllerCompat controller = getSupportMediaController()
//        //так стало
//        MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerActivity.this);
//        if (controller != null) {
//            controller.registerCallback(mediaControllerCallback);
//        }
//    }

    public void removeQueueItem(MstreamQueueObject mqo){
        int status = QueueManager.removeFromQueue(mqo);
        // TODO: Need to update the queue without causing the thing to flash
        //queueAdapter.clear();
        //queueAdapter.add(QueueManager.getIt());

        if(status == 1){
            // Play
            playMedia();
        }
    }


    private void onPlaybackStateChanged2(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }

        // This is a dirty hack to get around google's shitty callback structure
        // Since we can't pass in the max length, we just pass it in as the state as a negative number
        // This works because no state has a number less than -1
        if(state.getState() < -1){
            seekBar.setMax(state.getState() * -1);
            return;
        }

//        if (getActivity() == null) {
//            Log.w(TAG, "onPlaybackStateChanged called when getActivity null," +
//                    "this should not happen if the callback was properly unregistered. Ignoring.");
//            return;
//        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                // Toast.makeText(this, "Playback error: " + state.getErrorMessage(), Toast.LENGTH_LONG).show(); // TODO: Why does this keep getting called
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_PLAYING:
            case PlaybackStateCompat.STATE_REWINDING:
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
            default:
                break;
        }

        currentPosition = (int) state.getPosition();

        seekBar.setSecondaryProgress((int) state.getBufferedPosition());
        seekBar.setProgress(currentPosition);

        if (enablePlay) {
            playPauseButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_36dp));
            seekHAndler.removeCallbacks(myRunnable);
            handledLock = false;

        } else {
            playPauseButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_black_36dp));

            if(!handledLock){
                handledLock = true;
                seekHAndler.post(myRunnable);
            }
        }

        // Update playlist here?
        //updateQueueView();
    }

    private Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            currentPosition = currentPosition + 100;
            seekBar.setProgress(currentPosition );
            seekHAndler.postDelayed(this, 100);
        }
    };

    private void onMetadataChanged2(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }


        // Otherwise, need to set up event system between media player and this view.
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        // seekBar.setMax(duration);
    }


    private boolean checkIfSynced(MetadataObject moo){
        // Check for hash in moo
        if(moo.getSha256Hash() == null || moo.getSha256Hash().isEmpty()){
            return false;
        }

        //Check for hash in local DB
//        String hashPath = mstreamDB.checkForHash(moo.getSha256Hash());
//        if(hashPath != null && !hashPath.isEmpty() ){
//            toastIt("Synced File!");
//            moo.setLocalFile(hashPath);
//            return true;
//        }

        return false;
    }

    // Function that downloads file
    public void syncFile(MetadataObject moo, boolean autoSync){
        // Get Sync Path
        //String syncPath = SyncSettingsStore.storagePath;
//        if(syncPath == null || syncPath.isEmpty()){
//            return;
//        }

        // Check if synced
        boolean isSynced = checkIfSynced(moo);
        if(isSynced){
            return;
        }

        // Should we overwrite or just let it rip


        // Work out a way to sync non-hashed files
        if(moo.getSha256Hash() == null || moo.getSha256Hash().isEmpty()){
            toastIt("Cannot sync non-hashed files. For now...");
            return;
        }



        // If  not synced and autoSync = true
        if(autoSync){
            long downloadReference;

            // Create request for android download manager
            DownloadManager downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            Uri androidUri = android.net.Uri.parse(moo.getUrl());
            DownloadManager.Request request = new DownloadManager.Request(androidUri);

            // Set Destination
            //File tempFile = new File(SyncSettingsStore.storagePath, moo.getFilepath());
            //File tempFile2 = new File("mstream-storage", moo.getFilepath());

            // Set title of request
            //request.setTitle(tempFile.getName());

            // Or should we hide them and be selfish so users can only get to them via mSream
            // request.allowScanningByMediaScanner();

            //Setting description of request
            request.setDescription("Android Data download using DownloadManager.");
            // Set Notification Visibility
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // TODO: Make invisible and put some kind of UI


            //request.setDestinationInExternalFilesDir(PlayerActivity.this, tempFile2.getParent(), tempFile.getName());


            //Enqueue download and save into referenceId
            downloadReference = downloadManager.enqueue(request);

            //moo.setDownloadingToPath(tempFile.toString());
            moo.setSyncing(true);
            downloadQueue.put(downloadReference, moo);

        }

    }

}
