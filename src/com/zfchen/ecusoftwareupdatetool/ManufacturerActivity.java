package com.zfchen.ecusoftwareupdatetool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class ManufacturerActivity extends Activity {

	private String[] name = { "众泰", "东风小康", "吉利", "北汽银翔" };  	   
    private int[] imageids = { R.drawable.zotye, R.drawable.dfsk, R.drawable.geely, R.drawable.baic };
    private ArrayList<String> listFileName = null;
    private ListView lt1;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manufacturer);
		
		List<Map<String, Object>> listems = new ArrayList<Map<String, Object>>();  
        for (int i = 0; i < name.length; i++) {  
            Map<String, Object> listem = new HashMap<String, Object>();  
            listem.put("head", imageids[i]);  
            listem.put("name", name[i]);  
            listems.add(listem);  
        }
        
        SimpleAdapter simplead = new SimpleAdapter(this, listems,  
                R.layout.custom_list_item, new String[] {"name", "head"},  
                new int[] {R.id.manufacturerName,R.id.head});  
        
        lt1=(ListView)findViewById(R.id.manufacturerList);
        lt1.setOnItemClickListener(listener);	//添加点击事件监听器
        lt1.setAdapter(simplead); 
        /*
		ContentHandlerCarManufacturer myCarManufacturerHandler = new ContentHandlerCarManufacturer();
		XMLParseClass.XMLParse("车厂.txt", ManufacturerActivity.this, myCarManufacturerHandler);
		listFileName = myCarManufacturerHandler.GetListContent(1);
		*/
        listFileName = new ArrayList<>();
        listFileName.add("zotye");
        listFileName.add("dfsk");
        listFileName.add("geely");
        listFileName.add("baic");
	}
	
	OnItemClickListener listener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub
			Intent intent = new Intent();
			/*
			System.out.println(name[position]);
			System.out.println(listFileName.get(position));
			intent.putExtra("filename", listFileName.get(position));  //position:The position of the view(to be clicked) in the adapter.
			*/
			intent.putExtra("manufacturer", listFileName.get(position));
			intent.setClass(ManufacturerActivity.this, UpdateActivity.class);
			ManufacturerActivity.this.startActivity(intent);
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.manufacturer, menu);
		return true;
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
