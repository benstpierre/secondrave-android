package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import android.util.Log;
import com.google.protobuf.ByteString;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.WebSocket;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.MainActivity;
import com.secondrave.protos.SecondRaveProtos;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

/**
 * Created by benstpierre on 14-10-27.
 */
public class MediaDownloader implements Runnable, DataCallback, WebSocket.StringCallback {


    private static final String TAG = "MediaDownloader";
    private final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue;
    private Future<WebSocket> webSocket;
    private MainActivity mainActivity;

    public MediaDownloader(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue, MainActivity mainActivity) {
        this.decodedAudioQueue = decodedAudioQueue;
        this.mainActivity = mainActivity;
    }

    @Override
    public void run() {
        final String uri = "http://" + MainActivity.HOST + ":8080/events";
        final AsyncHttpClient asyncHttpClient = AsyncHttpClient.getDefaultInstance();
        final AsyncHttpGet get = new AsyncHttpGet(uri.replace("ws://", "http://").replace("wss://", "https://"));
        get.setTimeout(5000);
        this.mainActivity.showMessage("Connecting...");
        this.webSocket = asyncHttpClient.websocket(get, "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    doError(ex, webSocket);
                    return;
                } else {
                    MediaDownloader.this.mainActivity.showMessage("Connected");
                }
                webSocket.setStringCallback(MediaDownloader.this);
                webSocket.setDataCallback(MediaDownloader.this);
            }
        });
    }

    private void doError(Exception ex, WebSocket webSocket) {
        Log.e(TAG, "Unable to open websocket", ex);
        if (this.mainActivity != null) {
            if (ex instanceof TimeoutException) {
                this.mainActivity.showMessage("Unable to connect to host @" + MainActivity.HOST);
            } else if (ex.getMessage() != null) {
                this.mainActivity.showMessage(ex.getMessage());
            } else {
                this.mainActivity.showMessage("Unknown error" + ex.getClass().getName());
            }
        }
    }

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        final ByteString byteString = ByteString.copyFrom(bb.getAll());
        try {
            final SecondRaveProtos.AudioPiece audioPiece = SecondRaveProtos.AudioPiece.parseFrom(byteString);

            final InputStream inputStream = audioPiece.getAudioData().newInput();

            decodedAudioQueue.offer(new DecodedTimedAudioChunk(audioPiece.getAudioData().toByteArray(), audioPiece.getPlayAt(), audioPiece.getDuration(), true));

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bb.recycle();
        }
    }

    @Override
    public void onStringAvailable(String s) {
        System.out.println("Received a string: " + s);
    }

    public void stop() {
        this.webSocket.cancel();
    }

    public void setActivity(MainActivity activity) {
        this.mainActivity = activity;
    }
}
