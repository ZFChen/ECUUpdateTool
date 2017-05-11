package com.zfchen.ecusoftwareupdatetool;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	/*---控件声明----*/
	private Button diagFunction = null;
	private Button ConnectDevice = null;
	private Button auxilaryFunction = null;
	private Button extendFunction = null;
	private TextView leftTitle = null;
	private TextView rightTitle = null;
	/*---蓝牙操作命令---*/
	private static final int REQUEST_CONNECT_DEVICE = 0;
	private static final int REQUEST_ENABLE_BT = 1;
	public static boolean ConnectState = false;
	/*-----蓝牙操作相关-----*/
	private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //蓝牙串口服务
	public static String ServerBluetoothMAC = null;
	/* BluetoothAdapter: 管理和连接蓝牙设备 */
	public static BluetoothAdapter adapter = null;
	public static BluetoothDevice device = null;
	
	/* 客户端使用BluetoothSocket主动发起蓝牙连接  */
	public static BluetoothSocket socket = null;
	
	private boolean connectThreadFlag = false;
	BluetoothReceiver BluetoothStateDetect = null;
	/*----handlerMessage-----*/
	myHandler messageHandler = null;
	private static final int connectSuccess = 0;
	private static final int connectFail = 1;
	private static final int connectGiveUp = 2;
	/*-------连接请求线程-------*/
	ConnectThread connectThread = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* 启用窗口的扩展特性， FEATURE_CUSTOM_TITLE：自定义标题 */
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);	
		setContentView(R.layout.activity_main); /* 加载view布局文件 */
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.custom_title);  //设置自定义标题布局
		/* 得到控件对象(setContentView必须要放在findviewbyid之前，因为view在加载之前是无法引用的) */
		diagFunction = (Button) findViewById(R.id.diagFunction);	//诊断功能按钮
		diagFunction.getBackground().setAlpha(0);
		
		ConnectDevice = (Button) findViewById(R.id.connectDevice);	//连接设备按钮
		ConnectDevice.getBackground().setAlpha(0);
		
		auxilaryFunction = (Button) findViewById(R.id.auxilaryFunction);	//辅助功能按钮
		auxilaryFunction.getBackground().setAlpha(0);
		
		extendFunction = (Button) findViewById(R.id.extendFunction);	//扩展功能按钮
		extendFunction.getBackground().setAlpha(0);
		
		/* 设置标题  */
		leftTitle = (TextView) findViewById(R.id.title_left_text);
		rightTitle = (TextView) findViewById(R.id.title_right_text);
		leftTitle.setText(R.string.title_activity_main); //设置标题为“汽车ECU故障诊断仪”
		rightTitle.setText("设备未连接");
		
		diagFunction.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					diagFunction.getBackground().setAlpha(255);
					break;
				case MotionEvent.ACTION_UP:
					v.performClick();	//处理点击事件
					diagFunction.getBackground().setAlpha(0);
					Intent intent = new Intent();
					//intent.putExtra("filename", "车系.txt");
					//intent.setClass(MainActivity.this, CarSeriesSelectActivity.class);
					intent.setClass(MainActivity.this, ManufacturerActivity.class);
					MainActivity.this.startActivity(intent);  //跳转到“车厂选择”Activity
					break;
				}
				return false;
			}
		});
		
		ConnectDevice.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					ConnectDevice.getBackground().setAlpha(255);
					break;
				case MotionEvent.ACTION_UP:
					v.performClick();	//处理点击事件
					ConnectDevice.getBackground().setAlpha(0);
					Intent serverIntent = new Intent(MainActivity.this,
							DeviceListActivity.class);
					
					/* startActivityForResult: 以指定的请求码启动Intent匹配的Activity，而且程序将会等到新启动Activity的结果 */
					startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
					break;
				}
				return false;
			}
		});
		
		auxilaryFunction.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					auxilaryFunction.getBackground().setAlpha(255);
					break;
				case MotionEvent.ACTION_UP:
					v.performClick();	//处理点击事件
					auxilaryFunction.getBackground().setAlpha(0);
					break;
				}
				return false;
			}
		});
		
		extendFunction.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					extendFunction.getBackground().setAlpha(255);
					break;
				case MotionEvent.ACTION_UP:
					v.performClick();	//处理点击事件
					extendFunction.getBackground().setAlpha(0);
					break;
				}
				return false;
			}
		});
		
		ShowTips();
		/*----------*/
		IntentFilter filter = new IntentFilter(
				BluetoothDevice.ACTION_ACL_DISCONNECTED);
		BluetoothStateDetect = new BluetoothReceiver();
		registerReceiver(BluetoothStateDetect, filter);
		
	}

	public class BluetoothReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				Toast.makeText(getApplicationContext(), "蓝牙连接断开了！",
						Toast.LENGTH_LONG).show();
				rightTitle.setText("连接断开了");
				Intent myIntent = new Intent();
				myIntent.setClass(MainActivity.this, DisconnectWarningActivity.class);
				startActivity(myIntent);
			}
		}

	}

	public int getResourceId(String name) {
		try {
			Field field = R.drawable.class.getField(name);
			return Integer.parseInt(field.get(null).toString());
		} catch (Exception e) {

		}
		return 0;
	}

	void ShowTips() {
		String html = "<img src='car_tips'/>  软件使用小提示：请先选择”连接设备“，待蓝牙连接成功后，即可进入”软件升级“功能！";
		CharSequence ch = Html.fromHtml(html, new ImageGetter() {

			@Override
			public Drawable getDrawable(String source) {
				// TODO Auto-generated method stub
				Drawable drawable = getResources().getDrawable(
						getResourceId(source));
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight());
				return drawable;
			}
		}, null);	//fromHtml()的用法（图片是如何显示的？）
		TextView tips = (TextView) findViewById(R.id.start_tips);
		tips.setText(ch);
		html = "<img src='adayo_logo'/>";
		ch = Html.fromHtml(html, new ImageGetter() {

			@Override
			public Drawable getDrawable(String source) {
				// TODO Auto-generated method stub
				Drawable drawable = getResources().getDrawable(
						getResourceId(source));
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
						drawable.getIntrinsicHeight());
				return drawable;
			}
		}, null);
		TextView logo = (TextView) findViewById(R.id.logo);
		logo.setText(ch);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (DeviceListActivity.mBtAdapter != null) {
			DeviceListActivity.mBtAdapter.disable();
		}
		if (connectThread != null) {
			connectThread = null;
			try {
				socket.close();

			} catch (IOException i) {
				i.printStackTrace();
			}
		}
		connectThreadFlag = false;
		unregisterReceiver(BluetoothStateDetect);
	}
	
	public class StateCheckThread extends Thread {

		StateCheckThread() {

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();

		}

	}

	public class ConnectThread extends Thread {
		public ConnectThread() {

			try {
				adapter = DeviceListActivity.mBtAdapter;
				device = adapter.getRemoteDevice(ServerBluetoothMAC);// 服务器的蓝牙地址
				socket = device.createRfcommSocketToServiceRecord(uuid);
				connectThreadFlag = true;
			} catch (IOException e) {
				e.printStackTrace();
				connectThreadFlag = false;
			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			int tryCount = 0;
			while (connectThreadFlag) {
				try {
					// System.out.println("尝试与服务器进行通讯");
					socket.connect();
					connectThreadFlag = false;
					ConnectState = true;
					// System.out.println("蓝牙连接成功");
					/*
					 * ParcelUuid temp[]; int len = device.getUuids().length;
					 * temp = device.getUuids(); for(int i = 0;i < len;i++){
					 * System.out.println(temp[i].getUuid()); }
					 */
					SendMessage(connectSuccess);
				} catch (IOException i) {
					i.printStackTrace();
					// System.out.println("蓝牙连接失败");
					ConnectState = false;
					tryCount++;
					if (tryCount < 10) {
						SendMessage(connectFail);
					} else {
						// System.out.println("放弃尝试连接");
						connectThreadFlag = false;
						SendMessage(connectGiveUp);
					}
					try {
						Thread.sleep(2000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	void SendMessage(int arg) {
		Message msg = messageHandler.obtainMessage();
		msg.what = arg;
		msg.sendToTarget();
	}

	class myHandler extends Handler {
		public myHandler() {

		}

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			String content = null;
			switch (msg.what) {
			case connectSuccess:
				content = "连接成功";
				rightTitle.setText("连接成功");
				break;
			case connectFail:
				content = "连接失败，再次尝试连接";
				rightTitle.setText("连接失败，再次尝试连接");
				break;
			case connectGiveUp:
				content = "尝试连接超时，放弃连接";
				rightTitle.setText("尝试连接超时，放弃连接");
				break;
			default:

				break;
			}
			Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT).show();
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				if (connectThread != null) {
					connectThread = null;
					try {
						socket.close();
					} catch (IOException i) {
						i.printStackTrace();
					}
				}
				connectThreadFlag = false;

				ServerBluetoothMAC = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				connectThread = new ConnectThread();
				connectThread.start();
				/*---创建消息处理对象--*/
				messageHandler = new myHandler();

			} else {
				Toast.makeText(this, "用户未选择配对设备,请重新选择", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
