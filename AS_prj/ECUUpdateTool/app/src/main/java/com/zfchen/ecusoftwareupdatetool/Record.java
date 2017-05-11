package com.zfchen.ecusoftwareupdatetool;

import java.util.ArrayList;

public class Record implements Cloneable{
	long address;
	ArrayList<Integer> list;
	
	public long getAddress() {
		return address;
	}
	public void setAddress(long address) {
		this.address = address;
	}
	public void setAddressAndList(long address, ArrayList<Integer> list) {
		this.address = address;
		this.list = list;
	}
	public ArrayList<Integer> getList() {
		return list;
	}
	public void setList(ArrayList<Integer> list) {
		this.list = list;
	}
	
	public Record(long address, ArrayList<Integer> list) {
		super();
		this.address = address;
		this.list = list;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		Record record = null;
        try
        {
        	record = (Record) super.clone();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
		
		return record;
	}
	
}
