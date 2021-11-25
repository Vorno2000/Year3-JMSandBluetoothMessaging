package com.example.messageapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class CreateMessage extends AppCompatActivity {
    private BufferedReader in;
    private PrintWriter out;
    private ConnectionThread ct;
    private Handler h = new Handler();
    private OperationThread ot;
    private Context context;
    private EditText editText;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);

        context = this;

        editText = (EditText)findViewById(R.id.editText);
        sendButton = (Button)findViewById(R.id.sendButton);

        ct = (ConnectionThread)getIntent().getSerializableExtra("ConnectionThread");

        ot = new OperationThread();
        Thread newOperationThread = new Thread(ot);
        newOperationThread.start();
    }

    @Override
    public void onBackPressed() {
    }
    //when send button is clicked send message and finish activity
    public void sendButton(View view) {
        String sendMessage = editText.getText().toString();
        if(!sendMessage.equals("")) {
            SendMessageThread smt = new SendMessageThread(sendMessage);
            Thread t = new Thread(smt);
            t.start();
        }

        ot.stop();
        ((Activity)context).finish();
    }
    //operation thread which allows a user to create a message - checks validity and error handling
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

                out.println("//write");
                String response = null;
                try {
                    Thread.sleep(200);
                    response = in.readLine();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                String finalResponse = response;
                //checks to see if the server responded and worked correctly
                if(finalResponse != null) {
                    if(finalResponse.contains("//success")) {
                        try {
                            String completedResponse = in.readLine();

                            if(completedResponse.contains("//error")) {
                                String errorResponse = completedResponse.replace("//error ","");
                                h.post(new Runnable() {
                                    public void run() {
                                        Toast.makeText(CreateMessage.this, completedResponse, Toast.LENGTH_LONG).show();
                                    }
                                });
                                Intent mainIntent = new Intent(context, MainActivity.class);
                                startActivity(mainIntent);
                                ((Activity)context).finish();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(finalResponse.contains("//busy")) {
                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Another device is writing a message", Toast.LENGTH_SHORT).show();
                            }
                        });
                        ((Activity)context).finish();
                    }
                    else if(finalResponse.contains("//error")) {
                        String otherResponse = finalResponse.replace("//error ","");
                        h.post(new Runnable() {
                            public void run() {
                                Toast.makeText(CreateMessage.this, otherResponse, Toast.LENGTH_LONG).show();
                            }
                        });
                        Intent mainIntent = new Intent(context, MainActivity.class);
                        startActivity(mainIntent);
                        ((Activity)context).finish();
                    }
                }

                stop();
            }
        }

        public void stop() {
            isAlive = false;
        }
    }
    //thread for sending a message
    class SendMessageThread implements Runnable {
        private boolean isAlive = true;
        private String message;

        public SendMessageThread(String message) {
            this.message = message;
        }

        public void run() {
            while(isAlive) {
                out.println(message);
                stop();
            }
        }

        public void stop() { isAlive = false; }
    }
}