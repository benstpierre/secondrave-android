package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.MainActivity;
import com.secondrave.protos.SecondRaveProtos;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by benstpierre on 14-10-27.
 */
public class MediaDownloader implements Runnable {


    private final ConcurrentLinkedQueue<SecondRaveProtos.AudioPiece> downloadedAudioQueue;
    private Future<WebSocket> webSocket;

    public MediaDownloader(ConcurrentLinkedQueue<SecondRaveProtos.AudioPiece> downloadedAudioQueue) {
        this.downloadedAudioQueue = downloadedAudioQueue;
    }

    @Override
    public void run() {
        final String url = "http://" + MainActivity.HOST + ":8080/events";
        this.webSocket = AsyncHttpClient.getDefaultInstance().websocket(url, "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        System.out.println("I got a string: " + s);
                    }
                });
                webSocket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        final ByteString byteString = ByteString.copyFrom(bb.getAll());
                        try {
                            downloadedAudioQueue.offer(SecondRaveProtos.AudioPiece.parseFrom(byteString));
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        } finally {
                            bb.recycle();
                        }
                    }
                });
            }
        });
    }


    public void stop() {
        this.webSocket.cancel();
        downloadedAudioQueue.clear();
    }

}
