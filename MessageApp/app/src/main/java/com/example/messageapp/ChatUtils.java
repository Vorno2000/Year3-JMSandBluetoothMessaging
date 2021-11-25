package com.example.messageapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.icu.util.Output;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ChatUtils {
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectionThread connectedThread;

    private final UUID APP_UUID = UUID.fromString("dbe32fd8-cba7-11eb-b8bc-0242ac130003");
    private final String APP_NAME = "MessageApp";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;
    //utility constructor
    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState() {
        return state;
    }
    //sets the state of the message through the handler
    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(Bluetooth.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }
    //thread start allows for the threads to begin and sets the state of the threads
    private synchronized void start() {
        if(connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if(acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        if(connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);
    }
    //thread stop allows to kill the threads
    public synchronized void stop() {
        if(connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if(acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        if(connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }
    //connects the thread and sets the state
    public void connect(BluetoothDevice device) {
        if(state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        if(connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);

    }
    //writes to the buffer after setting the connected thread
    public void write(byte[] buffer) {
        ConnectionThread connThread;
        synchronized (this) {
            if(state != STATE_CONNECTED) {
                return;
            }

            connThread = connectedThread;
        }

        connThread.write(buffer);
    }
    //configure the accept thread for accepting a connection
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            }
            catch (IOException e) {
                Log.e("Accept->Constructor", e.toString());
            }

            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                socket = serverSocket.accept();
            }
            catch(IOException e) {
                Log.e("Accept->Run", e.toString());
                try {
                    serverSocket.close();
                }
                catch(IOException err) {
                    Log.e("Accept->Close", err.toString());
                }
            }

            if(socket != null) {
                switch(state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connect(socket, socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e("Accept->CloseSocket", e.toString());
                        }
                        break;
                }
            }
        }
        public void cancel() {
            try {
                serverSocket.close();
            }
            catch(IOException e) {
                Log.e("Accept->CloseServer", e.toString());
            }
        }
    }
    //thread to establish a connection to the other device
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            }
            catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }

            socket = tmp;
        }

        public void run() {
            try {
                socket.connect();
            }
            catch(IOException e) {
                Log.e("Connect->Run", e.toString());
                try {
                    socket.close();
                }
                catch (IOException err) {
                    Log.e("Connect->CloseSocket", err.toString());
                }
                connectionFailed();
                return;
            }

            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            connect(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            }
            catch(IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }
    //thread to send messages to the connected peer
    private class ConnectionThread extends Thread {
        boolean isAlive = true;
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectionThread(BluetoothSocket socket) {
            this.socket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch(IOException e) {

            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            try{
                while(isAlive) {
                    bytes = inputStream.read(buffer);

                    handler.obtainMessage(Bluetooth.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                }
            }
            catch(IOException e) {
                connectionLost();
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                handler.obtainMessage(Bluetooth.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            }
            catch(IOException e) {

            }
        }

        public void cancel() {
            try {
                socket.close();
                isAlive = false;
            }
            catch (IOException e) {

            }
        }
    }
    //error handling for losing connection to the client
    private void connectionLost() {
        Message message = handler.obtainMessage(Bluetooth.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Bluetooth.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private synchronized void connectionFailed() {
        Message message = handler.obtainMessage(Bluetooth.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Bluetooth.TOAST, "cant connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }
    //connects the peered client to the user
    private synchronized void connect(BluetoothSocket socket, BluetoothDevice device) { ////
        if(connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if(connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectionThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(Bluetooth.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Bluetooth.DEVICE_NAME, device.getName());
        message.setData(bundle);

        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
