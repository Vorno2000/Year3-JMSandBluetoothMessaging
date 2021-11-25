package com.example.messageapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ReceiveMessage extends AppCompatActivity {
    private Context context;
    private TextView receiveTextView;
    private BufferedReader in;
    private PrintWriter out;
    private ConnectionThread ct;
    private OperationThread ot;
    private ProgressBar spinner;
    Handler h = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_message);

        context = this;
        ct = (ConnectionThread)getIntent().getSerializableExtra("ConnectionThread");

        receiveTextView = (TextView)findViewById(R.id.receiveTextView);
        spinner = (ProgressBar)findViewById(R.id.progressBar2);
        spinner.setVisibility(View.GONE);

        ot = new OperationThread();
        Thread t = new Thread(ot);
        t.start();
    }
    //button to end receiving messages
    public void finishReceiveButton(View view) {
        ot.stop();
        ((Activity)this).finish();
    }

    @Override
    public void onBackPressed() {
    }
    //operation thread to check that the server is sending messages correctly and error handling
    class OperationThread implements Runnable {
        private boolean isAlive = true;

        public void run() {
            while(isAlive) {
                try {
                    in = new BufferedReader(new InputStreamReader(ct.getSocket().getInputStream()));
                    out = new PrintWriter(ct.getSocket().getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                out.println("//listen");

                String response = null;
                try {
                    Thread.sleep(200);

                    response = in.readLine();
                    if(response.equals("//complete")) {
                        response = in.readLine();
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                String finalResponse = response;
                //checks that the response is successful or returned and error
                if(finalResponse != null) {
                    if(finalResponse.contains("//success")) {
                        try {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    spinner.setVisibility(View.VISIBLE);
                                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                }
                            });

                            receivingThread rt = beginThread();

                            Thread.sleep(15000);

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    spinner.setVisibility(View.GONE);
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                }
                            });

                            rt.stop();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //error handling
                    else if(finalResponse.contains("//error")) {
                        String otherResponse = finalResponse.replace("//error ","");
                        h.post(new Runnable() {
                            public void run() {
                                Toast.makeText(ReceiveMessage.this, otherResponse, Toast.LENGTH_LONG).show();
                            }
                        });
                        Intent mainIntent = new Intent(context, MainActivity.class);
                        startActivity(mainIntent);
                        ((Activity)context).finish();
                    }
                    else if(finalResponse.contains("//busy")) {
                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Another device is reading a message", Toast.LENGTH_SHORT).show();
                            }
                        });
                        ((Activity)context).finish();
                    }
                }
                stop();
            }
        }

        public void stop() { isAlive = false; }
    }

    public receivingThread beginThread() {
        receivingThread rt = new receivingThread();
        Thread newThread = new Thread(rt);
        newThread.start();

        return rt;
    }
    //receiving thread  which posts all the messages to the text view
    class receivingThread implements Runnable {
        private boolean isAlive = true;
        private String displayMessage = "";

        public void run() {
            while(isAlive) {
                String message = "";
                try {
                    message = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //error handling
                if (!message.equals(null)) {
                    if(!message.contains("//success")) {
                        if(!message.contains("//complete")) {
                            if(!message.contains("null")) {
                                if (message.contains("//busy")) {
                                    h.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, "Another device is reading a message", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    ((Activity) context).finish();
                                } else {
                                    if (displayMessage.equals(""))
                                        displayMessage = message + "\n";
                                    else
                                        displayMessage += message + "\n";
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            receiveTextView.setText(displayMessage);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
                else {
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Could not receive a message", Toast.LENGTH_SHORT).show();
                        }
                    });
                    ((Activity) context).finish();
                    break;
                }
            }
        }
        public void stop() {
            isAlive = false;
        }
    }
}