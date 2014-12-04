package com.lightningstrikesolutions.secondrave.secondraveandroid.app.magic;

import com.google.protobuf.ByteString;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.lightningstrikesolutions.secondrave.secondraveandroid.app.MainActivity;
import com.secondrave.protos.SecondRaveProtos;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by benstpierre on 14-10-27.
 */
public class MediaDownloader implements Runnable, DataCallback, WebSocket.StringCallback {


    private final ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue;
    private Future<WebSocket> webSocket;

    public MediaDownloader(ConcurrentLinkedQueue<DecodedTimedAudioChunk> decodedAudioQueue) {
        this.decodedAudioQueue = decodedAudioQueue;
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
                webSocket.setStringCallback(MediaDownloader.this);
                webSocket.setDataCallback(MediaDownloader.this);
            }
        });
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
}
