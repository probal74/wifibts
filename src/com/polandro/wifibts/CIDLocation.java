package com.polandro.wifibts;

import java.io.Serializable;
import java.util.Vector;

public class CIDLocation implements Serializable {
	
	//constructor
	public CIDLocation(){
		CIDdb = new Vector<Integer>();
	}
	
	private static final long serialVersionUID = 666L;
	Vector<Integer> CIDdb;

	public boolean addCID(int cid){
		
		CIDdb.add(cid);
		return true;
	}
	
	public boolean delCID(int cid){
		int i=0;
		for(i=0;i<CIDdb.size();i++){
			if(CIDdb.elementAt(i) == cid){
				CIDdb.remove(i);
				return true;
			}
		}		
		return false;
	}
	
	public boolean isCIDhere(int cid){
		int i=0;
		for(i=0;i<CIDdb.size();i++){
			if(CIDdb.elementAt(i) == cid){
				return true;
			}
		}		
		return false;
	}
	
	public int getNumberOfCIDs(){
		return CIDdb.size();
	}
}
