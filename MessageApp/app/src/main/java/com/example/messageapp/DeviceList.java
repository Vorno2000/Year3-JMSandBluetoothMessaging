package com.example.messageapp;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Set;

public class DeviceList extends AppCompatActivity {
    private Context context;
    private ListView pairedDeviceList, availableDeviceList;
    private ArrayAdapter<String> pairedDevicesAdapter, availableDevicesAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        context = this;
        init();
        scanDevices();
    }
    //initialises all the views and adapters to add to and from the devices found
    private void init() {
        pairedDeviceList = findViewById(R.id.PairedDeviceListView);
        availableDeviceList = findViewById(R.id.AvailableDeviceListView);

        progressBar = findViewById(R.id.DeviceListProgressBar);

        pairedDevicesAdapter = new ArrayAdapter<String>(context, R.layout.device_list_item);
        availableDevicesAdapter = new ArrayAdapter<String>(context, R.layout.device_list_item);

        pairedDeviceList.setAdapter(pairedDevicesAdapter);
        availableDeviceList.setAdapter(availableDevicesAdapter);

        availableDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress", address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if(pairedDevices != null && pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener, intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener, intentFilter1);
    }
    //controls the adapters and allows for the search to begin and end
    private BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    availableDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressBar.setVisibility(View.GONE);
                if(availableDevicesAdapter.getCount() == 0) {
                    Toast.makeText(context, "No new devices found", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(context, "Click on the device to begin chatting", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    //scans for devices in vicinity
    public void scanDevices() {
        progressBar.setVisibility(View.VISIBLE);
        availableDevicesAdapter.clear();

        Toast.makeText(context, "Scan Started", Toast.LENGTH_SHORT).show();

        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }
}