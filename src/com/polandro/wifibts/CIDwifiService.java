package com.polandro.wifibts;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

public class CIDwifiService extends Service {

	private TelephonyManager telMgr;
	private WifiManager wifiMgr;
	private PhoneStateListener listener;
	private GsmCellLocation GCL;
	private DBAdapter wifiBTSdb;
	private int current_cid;
	private String current_ssid;
	private SampleReceiver myReceiver;
	public static final String NEW_MSG_TO_GUI = "com.gregory.Intents.MESSAGE_TO_GUI";
	public static final String NEW_MSG_TO_SERVICE = "com.gregory.Intents.MESSAGE_TO_SERVICE";
	
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
		wifiMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		telMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		
		myReceiver = new SampleReceiver();
        IntentFilter filter = new IntentFilter(NEW_MSG_TO_SERVICE);
        registerReceiver(myReceiver, filter);
		
		listener = new PhoneStateListener() {
            @Override
            public void onCellLocationChanged(CellLocation location) {            
            	GCL = (GsmCellLocation)telMgr.getCellLocation(); 
            	current_cid = GCL.getCid();
            	RefreshLACCID();
            }              
        };
        // Register the listener wit the telephony manager
        telMgr.listen(listener, PhoneStateListener.LISTEN_CELL_LOCATION);
	}
	
	private void showDataFromIntent(Intent intent) {
        	String msg = intent.getStringExtra("ToService");        
        	sendMSGtoGUI("Service already running.\n");        
    }
	
	private void sendMSGtoGUI(String msg) {
		Intent intent = new Intent(NEW_MSG_TO_GUI);  
    	intent.putExtra("ToGUI",msg);
    	sendBroadcast(intent);
	}
	
	private void OpenCIDdb() {
		wifiBTSdb = new DBAdapter(this);
		wifiBTSdb.open();
    }
	
	private void RefreshLACCID() {
		sendMSGtoGUI("Current CID: "+current_cid+"\n");		
    	WifiInfo winfo = wifiMgr.getConnectionInfo();
    	current_ssid = winfo.getSSID();
    	sendMSGtoGUI("Current SSID: "+current_ssid+"\nRecorded CIDs:\n");
    	
        Cursor c = wifiBTSdb.getAllCIDs(current_ssid);
        if (c.moveToFirst())
        {
            do {          
            	sendMSGtoGUI(c.getString(1)+",");
            } while (c.moveToNext());
        }
    	
    	
    	if(current_cid != -1){
	    	if(!wifiMgr.isWifiEnabled()){
	    		if(wifiBTSdb.checkCID(current_cid)){
	    			wifiMgr.setWifiEnabled(true);
	    		}
	    	}
	    	else{
	    		if(!wifiBTSdb.checkCID(current_cid)){
	    			if(winfo.getNetworkId() != -1){
	    				wifiBTSdb.addCID(current_cid, current_ssid);
	    			}
	    			else{
	    				wifiMgr.setWifiEnabled(false);
	    			}
	    		}    		
	    	}
    	}
    	
    }
	
	@Override
	public void onDestroy() {
		wifiBTSdb.close(); 
		sendMSGtoGUI("Service stopped.\n"); 
	    super.onDestroy();
	}

}
