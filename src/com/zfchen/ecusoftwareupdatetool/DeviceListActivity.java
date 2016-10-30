package com.zfchen.ecusoftwareupdatetool;

import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceListActivity extends Activity{

	// Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    public static BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //蓝牙串口服务
	private static final int REQUEST_CONNECT_DEVICE = 0;
	private static final int REQUEST_ENABLE_BT = 1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_list);
		
     // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter == null){
        	Toast.makeText(this, "该设备没有蓝牙，无法运行此程序", Toast.LENGTH_LONG).show();
        	finish();
        	return;
        }
        if(!mBtAdapter.isEnabled()){
        	/* ACTION_REQUEST_ENABLE: 显示一个允许用户打开蓝牙的系统Activity */
        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }else{
        	DisplayPairedDevice();
        }
	}

	void DisplayPairedDevice(){
		// Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
     // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.pairedDevice);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
     // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
     // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "no paired device found";
            mPairedDevicesArrayAdapter.add(noDevices);
        }
	}
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect

            // Get the device MAC address, which is the last 17 chars in the View
        	//String XT720_BluetoothMAC = "D0:37:61:65:D6:A2";
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		//super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
		case REQUEST_ENABLE_BT:
			if(resultCode == Activity.RESULT_OK){
				DisplayPairedDevice();
		        
			}else{
				Toast.makeText(this, "用户未允许蓝牙打开", Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		}
	}
	
    
    
    
    
    
    
    
    
}
