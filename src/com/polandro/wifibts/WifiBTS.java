package com.polandro.wifibts;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.polandro.wifibts.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class WifiBTS extends Activity {
	
	public static final String NEW_MSG = "com.gregory.Intents.CID_CHANGE";
	private WifiManager wifiMgr;
	private TextView tv;
	private CIDLocation CIDdb;
	private String FILENAME = "cidloc.db";
	private FileInputStream fis;
	private ObjectInputStream ofis;
	private FileOutputStream fos;
	private ObjectOutputStream ofos;
	private SampleReceiver myReceiver;
	private int current_cid;

	
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
        IntentFilter filter = new IntentFilter(NEW_MSG);
        registerReceiver(myReceiver, filter);
        
        wifiMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        setContentView(tv);
        
        OpenCIDdb();
    }
    
    private void OpenCIDdb(){
    	try {
    		fis = openFileInput(FILENAME);
			ofis = new ObjectInputStream(fis);
			Object obj = ofis.readObject();
			if(obj instanceof CIDLocation){
				CIDdb = (CIDLocation)obj;
				tv.append("Loaded DB. "+CIDdb.getNumberOfCIDs()+" known CIDs \n");
			}
		} catch (Exception e) {
			tv.append("No file found. Creating new DB\n");
			CIDdb = new CIDLocation();				
		}
    }
    
    private void showDataFromIntent(Intent intent) {
        current_cid = intent.getIntExtra("ReceiverData", -1);
        RefreshLACCID();
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
        case R.id.refresh:
        	RefreshLACCID();
            return true;
        case R.id.wifi_toggle:
            WifiToggle();
            return true;
        case R.id.StartService:
        	ComponentName starting = startService(new Intent(WifiBTS.this, CIDwifiService.class));
        	if(starting == null){
        		tv.append("Service started.\n");
        	}        	
            return true;
        case R.id.StopService:
        	boolean stopping = 	stopService(new Intent(WifiBTS.this, CIDwifiService.class));
        	if(stopping){
        		tv.append("Service stoped.\n");
        	}        	
            return true;
        case R.id.save:
        	SaveCIDdb();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void WifiToggle(){
    	if(wifiMgr.getWifiState() == WifiManager.WIFI_STATE_ENABLED){
        	tv.append("Wifi is enabled. Disabling...");
        	wifiMgr.setWifiEnabled(false);
        	tv.append(" OK\n");
        }
    	else if(wifiMgr.getWifiState() == WifiManager.WIFI_STATE_DISABLED){
    		tv.append("Wifi is disabled. Enabling...");
        	wifiMgr.setWifiEnabled(true);
        	tv.append(" OK\n");
    	}
    }
    
    private void RefreshLACCID(){
    	   	
    	tv.append("Current CID: "+current_cid+"\n");
    	WifiInfo winfo = wifiMgr.getConnectionInfo();
    	//Main logic
    	//jesli wifi jest wyłączone
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
    		//jesli CID jest bazie to włączyć wifi
    		//jesli CIDa nie ma w bazie to w sumie nic
    	
    	//jesli wifi jest włączone
    		//jesli CID jest w bazie, jest to dobrze, cyzli nic nie robic
    		//jesli CIDA nie ma w bazie
    			//jesli jestesmy podłączeni do jakiejs sieci, zapisać CID do bazy
    			//jesli nie jesteśmy podłączeni do żadnej sieci, czyli wifi w trybie scan, to nalezy wifi wyłączyć tutaj ___
    	
    	//+logica jak traci zasięg (winda)
    	
    }
    
    private void SaveCIDdb(){
    	try {
			fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
			ofos = new ObjectOutputStream(fos);
			ofos.writeObject(CIDdb);
			ofos.close();
			fos.close();
			tv.append("DB saved with "+CIDdb.getNumberOfCIDs()+" known CIDs\n");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    /*
    @Override
    protected void onPause(){
    	tv.append("Program paused by Android.\n");
    	//SaveCIDdb();
    }
    
    @Override
    protected void onResume(){
    	tv.append("Program resumed by Android.\n");
    	//OpenCIDdb();
    }
    */
    
    @Override
    protected void onDestroy() {
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }
    
    
    
}