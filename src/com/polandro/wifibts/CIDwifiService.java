package com.polandro.wifibts;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

public class CIDwifiService extends Service {

	private TelephonyManager telMgr;
	private PhoneStateListener listener;
	private GsmCellLocation GCL;
	public static final String NEW_MSG = "com.gregory.Intents.CID_CHANGE";
	
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
		telMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		listener = new PhoneStateListener() {
            @Override
            public void onCellLocationChanged(CellLocation location) {            
            	//RefreshLACCID();
            	GCL = (GsmCellLocation)telMgr.getCellLocation(); 
            	Intent intent = new Intent(NEW_MSG);  
            	intent.putExtra("ReceiverData",GCL.getCid());
            	sendBroadcast(intent);
            }              
        };
        // Register the listener wit the telephony manager
        telMgr.listen(listener, PhoneStateListener.LISTEN_CELL_LOCATION);
	}
	
	@Override
	public void onDestroy() {
	    	     
	    super.onDestroy();
	}

}
