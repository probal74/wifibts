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
	public static final String KEY_DATE = "date";
	public static final String KEY_LOG = "text";

	private static final String DATABASE_NAME = "wifibts";
	private static final String DATABASE_TABLE = "cids";
	private static final String DATABASE_TABLE_LOG = "log";
	private static final int DATABASE_VERSION = 3;

	private static final String DATABASE_CREATE_CIDS = "create table cids (_id integer primary key autoincrement, cid integer not null, ssid text not null);";
	private static final String DATABASE_CREATE_LOG = "create table log (date integer, text text);";
	
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
			db.execSQL(DATABASE_CREATE_CIDS);
			db.execSQL(DATABASE_CREATE_LOG);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS cids");
			db.execSQL("DROP TABLE IF EXISTS log");
			onCreate(db);
		}
	}

	// ---inserts a cid into the database---
	public long addCID(int cid, String ssid) {
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
        return db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_CID, KEY_SSID}, KEY_SSID + "='" + ssid + "'", null, null, null, null);
    }
    
    //---checks if cid is already in db---
    public boolean checkCID(int cid){
    	if (db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_CID, KEY_SSID}, KEY_CID + "=" + cid, null, null, null, null).getCount() > 0)
    		return true;
    	else
    		return false;
    }    
    public long log(long date, String text) {
    	ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_DATE, date);
		initialValues.put(KEY_LOG, text);
		return db.insert(DATABASE_TABLE_LOG, null, initialValues);    
    }
    
    public Cursor getLog() {
        return db.query(DATABASE_TABLE_LOG, new String[] {KEY_DATE, KEY_LOG}, null, null, null, null, KEY_DATE, "LIMIT = 10");
    }
}
