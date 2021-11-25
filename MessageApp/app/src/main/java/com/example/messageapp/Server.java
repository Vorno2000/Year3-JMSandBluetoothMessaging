package com.example.messageapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Server extends AppCompatActivity {
    private static ConnectionThread ct;
    private Handler h = new Handler();
    private ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        spinner = (ProgressBar)findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);
        spinner.bringToFront();

        ct = new ConnectionThread();
        Thread newConnectionThread = new Thread(ct);
        newConnectionThread.start();

        OperationThread ot = new OperationThread(this);
        Thread newOperationThread = new Thread(ot);
        newOperationThread.start();
    }
    //choose to create message or receive messages
    public void createMessage(View view) {
        Intent createMessage = new Intent(this, CreateMessage.class);
        createMessage.putExtra("ConnectionThread", ct);
        startActivity(createMessage);
    }

    public void receiveMessage(View view) {
        Intent receiveMessage = new Intent(this, ReceiveMessage.class);
        receiveMessage.putExtra("ConnectionThread", ct);
        startActivity(receiveMessage);
    }
    //operation thread which loads the loading spinner and lets the connection take place through the socket
    class OperationThread implements Runnable {
        boolean isAlive = true;
        Context context;

        public OperationThread(Context context) {
            this.context = context;
        }

        public void run() {
            while(isAlive) {
                try {
                    spinner.setVisibility(View.VISIBLE);
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                    Thread.sleep(3000);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(!ct.isConnected()) {
                    h.post(new Runnable() {
                        public void run() {
                            Toast.makeText(context, "Cannot connect to server", Toast.LENGTH_LONG).show();
                        }
                    });
                    ((Activity)context).finish();
                    ct.closeConnection();
                }
                else {
                    h.post(new Runnable() {
                        public void run() {
                            Toast.makeText(context, "Connected Successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                stop();
            }
        }

        public void stop() {
            isAlive = false;
        }
    }
}