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

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    Socket socket;
    String SERVER_IP = "";
    int SERVER_PORT;
    Handler handler = new Handler();
    Button btnConnect;
    EditText inputName;
    EditText inputIp;
    EditText inputPort;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InitViewElement();
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString("name", inputName.getText().toString());
                bundle.putString("ip", inputIp.getText().toString());
                bundle.putString("port", inputPort.getText().toString());

                Intent it = new Intent();
                it.putExtras(bundle);
                it.setClass(MainActivity.this, ChatActivity.class);
                startActivity(it);

            }
        });
    }



    private void InitViewElement(){
        btnConnect = (Button) findViewById(R.id.btnConnect);
        inputName = (EditText) findViewById(R.id.inputName);
        inputIp = (EditText) findViewById(R.id.inputIp);
        inputPort = (EditText) findViewById(R.id.inputPort);
    }
}