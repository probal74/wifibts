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
	private Timestamp wifiStartTime;
	private int WIFI_DELAY = 10000; //10s
	
	private class SampleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        showDataFromIntent(intent);
	    }
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
		
	@Override
	public void onCreate() {

		super.onCreate();
		startservice();
	}
	
	private void startservice() {
		OpenCIDdb();
		wifiStartTime = new Timestamp(0);
		wifiMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		telMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		if(wifiMgr == null || telMgr == null){
			//android fuckup
			sendMSGtoGUI("Cannot access hardware :(\n");
			this.onDestroy();
		}
		
		myReceiver = new SampleReceiver();
		IntentFilter filter = new IntentFilter(WifiBTS.NEW_MSG_TO_SERVICE);
        registerReceiver(myReceiver, filter);
		
		listener = new PhoneStateListener() {
            @Override
            public void onCellLocationChanged(CellLocation location) {            
            	GCL = (GsmCellLocation)telMgr.getCellLocation();
            	if(GCL != null){
            		current_cid = GCL.getCid();
            		RefreshLACCID();
            	}            	
            }              
        };
        // Register the listener with the telephony manager
        telMgr.listen(listener, PhoneStateListener.LISTEN_CELL_LOCATION);
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
	
	private void startWifiManually(){
		wifiStartTime.setTime(System.currentTimeMillis());
		wifiMgr.setWifiEnabled(true);
		wifiBTSdb.log(System.currentTimeMillis(), "wifi enabled manually");
	}
	
	private void sendMSGtoGUI(String msg) {
		Intent intent = new Intent(WifiBTS.NEW_MSG_TO_GUI);
    	intent.putExtra("ToGUI",msg);
    	sendBroadcast(intent);
	}
	
	private void OpenCIDdb() {
		try {
			wifiBTSdb = new DBAdapter(this);
			wifiBTSdb.open();
		}
		catch(SQLiteException e){
			sendMSGtoGUI("DB exception!!!"); 
		}
    }
	
	private void RefreshLACCID() {
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
	    	//<LOGIC>
	    	if(current_cid != -1 &&
	    			current_time.getTime() - wifiStartTime.getTime() > WIFI_DELAY){
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
	    	//</LOGIC>
		}
		catch(Exception e){
			sendMSGtoGUI(e.getMessage());
		}
    	
    }
	
	@Override
	public void onDestroy() {
		try{
			wifiBTSdb.close();
		}
		catch(SQLiteException e){
			sendMSGtoGUI("DB exception!!!"); 
		}
		sendMSGtoGUI("Service stopped.\n"); 
	    super.onDestroy();
	}

}
