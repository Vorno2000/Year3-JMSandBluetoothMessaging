package com.example.messageapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void serverButton(View view) {
        Intent server = new Intent(this, Server.class);
        startActivity(server);
    }

    public void bluetoothButton(View view) {
        Intent bluetooth = new Intent(this, Bluetooth.class);
        startActivity(bluetooth);
    }

    @Override
    public void onBackPressed() {
    }
}