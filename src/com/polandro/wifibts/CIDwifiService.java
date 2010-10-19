package com.polandro.wifibts;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
	private CIDLocation CIDdb;
	private String FILENAME = "cidloc.db";
	private FileInputStream fis;
	private ObjectInputStream ofis;
	private FileOutputStream fos;
	private ObjectOutputStream ofos;
	private int current_cid;
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
            	//RefreshLACCID();
            	GCL = (GsmCellLocation)telMgr.getCellLocation(); 
            	current_cid = GCL.getCid();
            	RefreshLACCID();
            	SaveCIDdb();
            }              
        };
        // Register the listener with the telephony manager
        telMgr.listen(listener, PhoneStateListener.LISTEN_CELL_LOCATION);
	}
	
	private void showDataFromIntent(Intent intent) {
        String msg = intent.getStringExtra("ToService"); 
        sendMSGtoGUI("Service already running.\n");        
    }
	
	private void sendMSGtoGUI(String msg){
		Intent intent = new Intent(NEW_MSG_TO_GUI);  
    	intent.putExtra("ToGUI",msg);
    	sendBroadcast(intent);
	}
	
	private void OpenCIDdb(){
    	try {
    		fis = openFileInput(FILENAME);
			ofis = new ObjectInputStream(fis);
			Object obj = ofis.readObject();
			if(obj instanceof CIDLocation){
				CIDdb = (CIDLocation)obj;
				sendMSGtoGUI("Loaded DB. "+CIDdb.getNumberOfCIDs()+" known CIDs \n");
			}
		} catch (Exception e) {
			sendMSGtoGUI("No file found. Creating new DB\n");
			CIDdb = new CIDLocation();				
		}
    }
	
	private void SaveCIDdb(){
    	try {
			fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
			ofos = new ObjectOutputStream(fos);
			ofos.writeObject(CIDdb);
			ofos.close();
			fos.close();
			sendMSGtoGUI("DB saved with "+CIDdb.getNumberOfCIDs()+" known CIDs\n");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	private void RefreshLACCID(){
	   	
		sendMSGtoGUI("Current CID: "+current_cid+"\n");
    	WifiInfo winfo = wifiMgr.getConnectionInfo();
    	
    	if(current_cid != -1){
	    	if(!wifiMgr.isWifiEnabled()){
	    		if(CIDdb.isCIDhere(current_cid)){
	    			wifiMgr.setWifiEnabled(true);
	    		}
	    	}
	    	else{
	    		if(!CIDdb.isCIDhere(current_cid)){
	    			if(winfo.getNetworkId() != -1){
	    				CIDdb.addCID(current_cid);
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
		sendMSGtoGUI("Service stopped.\n");
		SaveCIDdb();    
	    super.onDestroy();
	}

}
