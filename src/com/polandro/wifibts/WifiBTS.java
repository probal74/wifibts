package com.polandro.wifibts;

//FAjne to, test repozytorium

import com.polandro.wifibts.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class WifiBTS extends Activity {
	
	public static final String NEW_MSG_TO_GUI = "com.polandro.Intents.MESSAGE_TO_GUI";
	public static final String NEW_MSG_TO_SERVICE = "com.polandro.Intents.MESSAGE_TO_SERVICE";
	public static final int START_WIFI = 1;
	public static final int PING = 2;
	private TextView tv;	
	private SampleReceiver myReceiver;
	
	private class SampleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        showDataFromIntent(intent);
	    }
	}
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tv = new TextView(this);
        myReceiver = new SampleReceiver();
        IntentFilter filter = new IntentFilter(NEW_MSG_TO_GUI);
        registerReceiver(myReceiver, filter);               
        setContentView(tv);
        tv.append("Herzlich wilkommen :)\n");
        sendMSGtoService(START_WIFI);
    }
    
    private void sendMSGtoService(int msg){
		Intent intent = new Intent(NEW_MSG_TO_SERVICE);  
    	intent.putExtra("ToService",msg);
    	sendBroadcast(intent);
	}
    
    private void showDataFromIntent(Intent intent) {
        String msg = intent.getStringExtra("ToGUI");
        tv.append(msg);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
                
        case R.id.StartService:
        	ComponentName starting = startService(new Intent(WifiBTS.this, CIDwifiService.class));
        	if(starting == null){
        		tv.append("Service started.\n");
        	}
        	else{
        		tv.append("Service already started.\n");
        	}
            return true;
        case R.id.StopService:
        	boolean stopping = 	stopService(new Intent(WifiBTS.this, CIDwifiService.class));
        	if(stopping){
        		tv.append("Service stopping...\n");
        	}
        	else{
        		tv.append("Service already stopped.\n");
        	}
            return true;
        case R.id.StartWifi:
        	sendMSGtoService(WifiBTS.START_WIFI);        
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onDestroy() {
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }
    
}