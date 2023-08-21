package com.example.chatclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    Socket socket;
    String NAME = "";
    String SERVER_IP = "";
    int SERVER_PORT;
    Handler handler = new Handler();
    TextView chat;
    EditText inputMessage;
    Button btnSend;
    Button btnDisconnect;
    Boolean isServerConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        InitViewElement();

        try {
            Intent it = getIntent();
            if ( it != null ) {
                Bundle bundle = it.getExtras();
                if (bundle != null){
                    NAME = bundle.getString("name");
                    SERVER_IP = bundle.getString("ip");
                    SERVER_PORT = Integer.parseInt(bundle.getString("port"));
                }
            }

        }catch (Exception ex) {
            Log.v("OnCreate", ex.toString());
        }

        try{
            ClientThread clientThread = new ClientThread();
            clientThread.start();

        } catch (Exception ex) {
            Log.v("ClientThread", ex.toString());
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, String> msgMap = new HashMap<String, String>();
                msgMap.put("name", NAME);
                msgMap.put("message", inputMessage.getText().toString());
                msgMap.put("state", "");
                SendMessage sendMessage = new SendMessage(msgMap);
                sendMessage.start();
                inputMessage.setText("");
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                HashMap<String, String> msgMap = new HashMap<String, String>();
                msgMap.put("name", NAME);
                msgMap.put("message", "ByeWorld!");
                msgMap.put("state", "disconnect");
                SendMessage sendMessage = new SendMessage(msgMap);
                sendMessage.start();
                Intent it = new Intent();
                it.setClass(ChatActivity.this, MainActivity.class);
                startActivity(it);
            }
        });


    }


    class ClientThread extends Thread {
        @Override
        public void run() {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                if(socket.isConnected()){
                    isServerConnected = true;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            chat.setText("Connected to server : " + SERVER_PORT + "\n");
                        }
                    });
                    HashMap<String, String> msgMap = new HashMap<String, String>();
                    msgMap.put("name", NAME);
                    msgMap.put("message", "HelloWorld!");
                    msgMap.put("state", "connect");
                    SocketThread socketThread = new SocketThread(socket);
                    socketThread.start();
                    SendMessage sendMessage = new SendMessage(msgMap);
                    sendMessage.start();
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            chat.setText("Connect failed...?");
                        }
                    });
                }
            } catch (IOException e) {
                Log.v("ClientThread", e.toString());
            }
        }
    }

    class SendMessage extends Thread {
        Message msg;
        public SendMessage(HashMap<String,String> msgMap){
            try {
                msg = new Message(msgMap);

            } catch (Exception ex){
                Log.v("SendMessage", ex.toString());
            }
        }

        @Override
        public void run() {
            try {
                if (isServerConnected){
                    OutputStreamWriter outputSW = new OutputStreamWriter(socket.getOutputStream());
                    BufferedWriter bufferedWriter = new BufferedWriter(outputSW);
                    bufferedWriter.write(msg.getJsonString() + "\n");
                    bufferedWriter.flush();
                    Log.d("SendMessage", msg.getJsonString());
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            chat.append("Server disconnected.\n");
                        }
                    });
                }
            } catch (IOException ex) {
                Log.v("SendMessage", ex.toString());
            }
        }
    }

    class SocketThread extends Thread {
        private Socket socket;
        private BufferedReader in;
        private boolean isConnected = true;
        public SocketThread(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            } catch (IOException ex) {
                Log.v("Client SocketThread Construct", ex.toString());
            }
        }

        @Override
        public void run(){
            while (isConnected) {
                try{
                    String str = in.readLine();
                    if(str != null){
                        Message msg = new Message(str);
                        Log.d("SocketThread InputStream Read", msg.getJsonString());
                        if (msg.getState().equals("disconnect")) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        chat.append(msg.getName() + " left!\n");
                                    } catch (JSONException ex) {
                                        Log.v("Show Client Joined", ex.toString());
                                    }
                                }
                            });
//                            isConnected = false;
                        }else if (msg.getState().equals("server-disconnect")) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        chat.append(msg.getName() + " left!\n");
                                    } catch (JSONException ex) {
                                        Log.v("Server Disconnect", ex.toString());
                                    }
                                }
                            });
                            isServerConnected = false;
                            isConnected = false;
                        }
                        else if (msg.getState().equals("connect")) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        chat.append(msg.getName() + " joined!\n");
                                    } catch (JSONException ex) {
                                        Log.v("Show Client Joined", ex.toString());
                                    }
                                }
                            });
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        chat.append(msg.getName() + " : " + msg.getMessage() + "\n");
                                    } catch (JSONException ex) {
                                        Log.v("Show Client Joined", ex.toString());
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception ex) {
                    Log.v("SocketThread InputStream Read", ex.toString());
                }
            }
        }
    }

    class Message{
        private JSONObject jsonObject = new JSONObject();
        public Message(String strMsg) throws JSONException {
            this.jsonObject = new JSONObject(strMsg);
        }
        public Message(HashMap<String, String> mapMsg) throws JSONException {
            for(String key:mapMsg.keySet()) {
                this.jsonObject.put(key, mapMsg.get(key));
            }
        }
        public Message(Message msg) throws JSONException {
            this.jsonObject = new JSONObject(msg.getJsonString());
        }
        public String getName() throws JSONException {
            return this.jsonObject.getString("name");
        }
        public String getMessage() throws JSONException {
            return this.jsonObject.getString("message");
        }
        public String getState() throws JSONException {
            return this.jsonObject.getString("state");
        }
        public String getJsonString() {
            return this.jsonObject.toString();
        }
    }

    private void InitViewElement() {
        chat = (TextView) findViewById(R.id.chat);
        inputMessage = (EditText) findViewById(R.id.inputMessage);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
    }
}