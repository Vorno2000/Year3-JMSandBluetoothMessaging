package com.example.messageapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;

public class ConnectionThread implements Runnable, Serializable {
    private static final String SERVER_IP = "192.168.1.69";
    private static final int SERVER_PORT = 5000;
    private static Socket socket;
    //runs the connection thread through the socket
    public void run() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
        } catch (IOException e) {
            Log.e("Server", "Error with code" + e);
        }
    }
    //checks if socket is connected
    public boolean isConnected() {
        return socket != null;
    }
    //closes connection
    public void closeConnection() {
        try {
            if(socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //get socket
    public Socket getSocket() {
        return socket;
    }
}
