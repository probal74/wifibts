package com.polandro.wifibts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter {
	public static final String KEY_ROWID = "_id";
	public static final String KEY_CID = "cid";
	public static final String KEY_SSID = "ssid";

	private static final String DATABASE_NAME = "wifibts";
	private static final String DATABASE_TABLE = "cids";
	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE = "create table cids (_id integer primary key autoincrement, cid text not null, ssid text not null);";

	private final Context context;

	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}

	// ---opens the database---
	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	// ---closes the database---
	public void close() {
		DBHelper.close();
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS cids");
			onCreate(db);
		}
	}

	// ---inserts a cid into the database---
	public long insertCID(String cid, String ssid) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CID, cid);
		initialValues.put(KEY_SSID, ssid);
		return db.insert(DATABASE_TABLE, null, initialValues);
	}

	// ---deletes all cids for one ssid---
	public boolean deleteTitle(String ssid) {
		return db.delete(DATABASE_TABLE, KEY_SSID + "=" + ssid, null) > 0;
	}
	
    //---retrieves all cids for one ssid---
    public Cursor getAllCIDs(String ssid) {
        return db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_CID, KEY_SSID}, KEY_SSID + "=" + ssid, null, null, null, null);
    }
    
    //---checks if cid is already assigned to the ssid
    public boolean checkCID(String cid, String ssid){
    	if (db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_CID, KEY_SSID}, KEY_CID + "=" + cid + " and " + KEY_SSID + "=" + cid, null, null, null, null).getCount() > 0)
    		return true;
    	else
    		return false;
    }    
}
