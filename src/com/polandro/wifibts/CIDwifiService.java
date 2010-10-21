package com.polandro.wifibts;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import java.sql.Timestamp;

public class CIDwifiService extends Service {
	private TelephonyManager telMgr;
	private WifiManager wifiMgr;
	private PhoneStateListener listener;
	private GsmCellLocation GCL;
	private DBAdapter wifiBTSdb;
	private int current_cid;
	private String current_ssid;
	private SampleReceiver myReceiver;
	private Timestamp serviceStartTime;
	private int WIFI_DELAY = 10000; //10s
	
	private class SampleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        showDataFromIntent(intent);
	    }
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
		
	@Override
	public void onCreate() {
		super.onCreate();
		startservice();
	}
	
	private void startservice() {
		myReceiver = new SampleReceiver();												//Register receiver for messages from GUI
		IntentFilter filter = new IntentFilter(WifiBTS.NEW_MSG_TO_SERVICE);
        registerReceiver(myReceiver, filter);
		
		openDB();																		//Open SQLite database
		serviceStartTime = new Timestamp(0);											//Time when the service was started
		wifiMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);					//wifiMgr - connect to the system wifi service
		telMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);			//telMgr - connect to the system telephony service
		if(wifiMgr == null || telMgr == null) {
			wifiBTSdb.log(System.currentTimeMillis(), "Cannot access hardware");
			this.onDestroy();
		}
		
		listener = new PhoneStateListener() {											//Listener for events from telephony manager
            public void onCellLocationChanged(CellLocation location) {            		//GsmCellLocation changed event
            	GCL = (GsmCellLocation)telMgr.getCellLocation();						
            	if (GCL != null) {														//If current location is available
            		current_cid = GCL.getCid();											//get the actual CellID
            		refreshCID();													//Refresh status
            	}            	
            }              
        };
        telMgr.listen(listener, PhoneStateListener.LISTEN_CELL_LOCATION);				//Register the listener with the telephony manager
	}
	
	private void showDataFromIntent(Intent intent) { //request parser        	
			int order = intent.getIntExtra("ToService", -1);
			if(order == WifiBTS.PING){
				sendMSGtoGUI("Service already running.\n"); 
			}
			else if(order == WifiBTS.START_WIFI){
				startWifiManually();
			}
    }
	
	private void startWifiManually() {
		serviceStartTime.setTime(System.currentTimeMillis());
		wifiMgr.setWifiEnabled(true);
		wifiBTSdb.log(System.currentTimeMillis(), "wifi enabled manually");
	}
	
	private void sendMSGtoGUI(String msg) {
		Intent intent = new Intent(WifiBTS.NEW_MSG_TO_GUI);
    	intent.putExtra("ToGUI",msg);
    	sendBroadcast(intent);
	}
	
	private void openDB() {
		try {
			wifiBTSdb = new DBAdapter(this);
			wifiBTSdb.open();
		}
		catch(SQLiteException e){
			sendMSGtoGUI("DB exception!!!"); 
		}
    }
	
	private void refreshCID() {
		try{
	    	WifiInfo winfo = wifiMgr.getConnectionInfo();
	    	current_ssid = winfo.getSSID();
	    	
	        Cursor c = wifiBTSdb.getLog();
	        if (c.moveToFirst())
	        {
	            do {          
	            	sendMSGtoGUI((new Timestamp(c.getLong(0))).toLocaleString()+" "+c.getString(1)+"\n");
	            } while (c.moveToNext());
	        }
	    	
	        Timestamp current_time = new Timestamp(System.currentTimeMillis());
	    	if(current_cid != -1 &&
	    			current_time.getTime() - serviceStartTime.getTime() > WIFI_DELAY){
		    	if(!wifiMgr.isWifiEnabled()){
		    		if(wifiBTSdb.checkCID(current_cid)){
		    			wifiMgr.setWifiEnabled(true);
		    			wifiBTSdb.log(System.currentTimeMillis(), "wifi enabled");
		    		}
		    	}
		    	else{
		    		if(!wifiBTSdb.checkCID(current_cid)){
		    			if(winfo.getNetworkId() != -1){
		    				wifiBTSdb.addCID(current_cid, current_ssid);
		    			}
		    			else{
		    				wifiMgr.setWifiEnabled(false);
		    				wifiBTSdb.log(System.currentTimeMillis(), "wifi disabled");
		    			}
		    		}    		
		    	}
	    	}
		}
		catch(Exception e){
			sendMSGtoGUI(e.getMessage());
		}
    	
    }
	
	@Override
	public void onDestroy() {
		try {
			wifiBTSdb.close();
		}
		catch (SQLiteException e) {
			sendMSGtoGUI("DB exception!!!"); 
		}
		sendMSGtoGUI("Service stopped.\n"); 
	    super.onDestroy();
	}

}
