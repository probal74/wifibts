package com.polandro.wifibts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
	private final int NOTIFICATION_ID = 1010;

	
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
		IntentFilter filter = new IntentFilter(WifiBTSTab.NEW_MSG_TO_SERVICE);
        registerReceiver(myReceiver, filter);
		
																				//Open SQLite database
		serviceStartTime = new Timestamp(0);											//Time when the service was started
		wifiMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);					//wifiMgr - connect to the system wifi service
		telMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);			//telMgr - connect to the system telephony service
		if(wifiMgr == null || telMgr == null) {
			openDB();
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
			if(order == WifiBTSTab.PING){
				sendMSGtoGUI("Service already running.\n"); 
			}
			else if(order == WifiBTSTab.START_WIFI){
				startWifiManually();
			}
    }
	
	private void startWifiManually() {
		serviceStartTime.setTime(System.currentTimeMillis());
		wifiMgr.setWifiEnabled(true);
		openDB();
		wifiBTSdb.log(System.currentTimeMillis(), "wifi enabled manually");
		triggerNotification("WifiBTS", "wifi enabled manually");
		closeDB();
	}
	
	private void sendMSGtoGUI(String msg) {
		Intent intent = new Intent(WifiBTSTab.NEW_MSG_TO_GUI);
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
	    	
	    	openDB();
	        /*Cursor c = wifiBTSdb.getLog();
	        if (c.moveToFirst())
	        {
	            do {          
	            	sendMSGtoGUI((new Timestamp(c.getLong(0))).toLocaleString()+" "+c.getString(1)+"\n");
	            } while (c.moveToNext());
	        }*/
	    	
	        Timestamp current_time = new Timestamp(System.currentTimeMillis());
	        String Time = current_time.getHours()+":"+current_time.getMinutes()+":"+current_time.getSeconds()+" "
	        +current_time.getYear()+"-"+current_time.getMonth()+"-"+current_time.getDay();	
	    	if(current_cid != -1 &&
	    			current_time.getTime() - serviceStartTime.getTime() > WIFI_DELAY){
		    	if(!wifiMgr.isWifiEnabled()){
		    		if(wifiBTSdb.checkCID(current_cid)){
		    			wifiMgr.setWifiEnabled(true);
		    			wifiBTSdb.log(System.currentTimeMillis(), "wifi enabled");
		    			triggerNotification("WifiBTS", "wifi enabled@ "+Time);		    			
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
		    				triggerNotification("WifiBTS", "wifi dienabled@ "+Time);
		    			}
		    		}    		
		    	}
	    	}
	    	closeDB();
		}
		catch(Exception e){
			sendMSGtoGUI(e.getMessage());
		}
    	
    }

	private void closeDB() {
		try {
			wifiBTSdb.close();
		}
		catch (SQLiteException e) {
			sendMSGtoGUI("DB exception!!!"); 
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
	
	private void triggerNotification(String Title, String Message)
    {
        CharSequence title = Title;
        CharSequence message = Message;
 
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon, "New wifi event", System.currentTimeMillis());
 
        Intent notificationIntent = new Intent(this, CIDwifiService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
 
        notification.setLatestEventInfo(CIDwifiService.this, title, message, pendingIntent);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
