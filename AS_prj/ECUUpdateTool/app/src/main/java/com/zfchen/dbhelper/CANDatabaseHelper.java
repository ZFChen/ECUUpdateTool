package com.zfchen.dbhelper;

import java.util.ArrayList;
import java.util.EnumMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class CANDatabaseHelper extends SQLiteOpenHelper {
	
	public enum UpdateStep{ /*java的枚举类型定义语法与C中的有些区别, 最后一个成员后面有一个';'号*/
		ReadECUSparePartNumber,
		ReadECUHardwareNumber,
		ReadBootloaderID,
		RequestToExtendSession,
		DisableDTCStorage,
		DisableNonDiagComm,
		RequestToProgrammingSession,
		RequestSeed,
		SendKey,
		WriteTesterSerialNumber,
		WriteECUSparePartNumber,
		WriteConfigureData,
		WriteUpdateDate,
		RequestDownload,
		TransferData,
		TransferExit,
		CheckSum,
		EraseMemory,
		CheckProgrammDependency,
		CheckProgrammCondition,
		ResetECU,
		EnableNonDiagComm,
		EnableDTCStorage,
		RequestToDefaultSession,
		FlowControl
	};
	
	EnumMap<UpdateStep, String> stepMap = null;
	EnumMap<UpdateStep, Boolean> resultResponse = null;
	
	ArrayList<Integer> CANIdList = null;
	ArrayList<Byte> CANMessage = null;
	
	final String CREATE_CANID_TABLE_SQL = "CREATE TABLE IF NOT EXISTS candb (canid integer primary key autoincrement, " +
			"manufacturer varchar(10), phyCANid varchar(4),funcCANid varchar(4), respCANid varchar(4))";
	
	/*final String CREATE_UPDATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS updateMessage (messageid integer primary key autoincrement, " +
			"step varchar(10), CAN_Message varchar(12))";*/
	
	public CANDatabaseHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
		CANIdList = new ArrayList<>();
		CANMessage = new ArrayList<>();
		stepMap = new EnumMap<UpdateStep, String>(UpdateStep.class);
		resultResponse = new EnumMap<UpdateStep, Boolean>(UpdateStep.class);
		this.initResponseDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase arg0) {
		// TODO Auto-generated method stub
		arg0.execSQL(CREATE_CANID_TABLE_SQL);
	}
	
	@Override
	public synchronized void close() {
		// TODO Auto-generated method stub
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		System.out.println("--------onUpdate Called--------"
				+ oldVersion + "--->" + newVersion);
	}
	
	public void generateCanDB(SQLiteDatabase db){
		insertData(db, "zotye", "76D", "7DF", "70D");
		insertData(db, "dfsk", "726", "7DF", "7A6");
		insertData(db, "geely", "7C6", "7DF", "7CE");	
		insertData(db, "baic", "76D", "7DF", "70D"); /* CAN ID 有待更新 */
	}
	
	public void generateUpdateGeelyDatabase(){	/* 参照Geely的Bootloader流程 */
		System.out.println("generate Geely Database!");
		stepMap.clear();
		stepMap.put(UpdateStep.ReadECUHardwareNumber, "22F19362");	/*最后一个byte是积极响应码*/
		stepMap.put(UpdateStep.ReadBootloaderID, "22F18062");
		stepMap.put(UpdateStep.RequestToExtendSession, "100350");
		stepMap.put(UpdateStep.DisableDTCStorage, "8502C5");
		stepMap.put(UpdateStep.DisableNonDiagComm, "280368");
		stepMap.put(UpdateStep.RequestToProgrammingSession, "100250");
		stepMap.put(UpdateStep.RequestSeed, "271167");
		stepMap.put(UpdateStep.SendKey, "271267");
		stepMap.put(UpdateStep.WriteTesterSerialNumber, "2EF1986E");
		stepMap.put(UpdateStep.WriteConfigureData, "2EF1996E");
		stepMap.put(UpdateStep.RequestDownload, "3474");
		stepMap.put(UpdateStep.TransferData, "360176");
		stepMap.put(UpdateStep.TransferExit, "3777");
		stepMap.put(UpdateStep.CheckSum, "3101020271");
		stepMap.put(UpdateStep.EraseMemory, "3101FF0071");
		stepMap.put(UpdateStep.CheckProgrammDependency, "3101FF0171");
		stepMap.put(UpdateStep.ResetECU, "110151");
		stepMap.put(UpdateStep.EnableNonDiagComm, "28000168");
		stepMap.put(UpdateStep.EnableDTCStorage, "8501C5");
		stepMap.put(UpdateStep.RequestToDefaultSession, "100150");
	}
	
	public void generateUpdateForyouDatabase(){	/* 参照恒润的Bootloader流程 */
		stepMap.clear();
		System.out.println("generate Foryou Database!");
		stepMap.put(UpdateStep.ReadECUSparePartNumber, "22F18762");	/*最后一个byte是积极响应码*/
		stepMap.put(UpdateStep.RequestToExtendSession, "100350");
		stepMap.put(UpdateStep.DisableDTCStorage, "8502C5");
		stepMap.put(UpdateStep.DisableNonDiagComm, "280368");	//remove parameter 01, in addParameter() add it.
		stepMap.put(UpdateStep.RequestToProgrammingSession, "100250");
		stepMap.put(UpdateStep.RequestSeed, "270567");
		stepMap.put(UpdateStep.SendKey, "270667");
		stepMap.put(UpdateStep.WriteTesterSerialNumber, "2EF1986E");
		stepMap.put(UpdateStep.WriteECUSparePartNumber, "2EF1876E");
		stepMap.put(UpdateStep.WriteUpdateDate, "2EF1996E");
		stepMap.put(UpdateStep.RequestDownload, "3474");	//remove parameter 00 44, in addParameter() add it.
		stepMap.put(UpdateStep.TransferData, "360176");
		stepMap.put(UpdateStep.TransferExit, "3777");
		stepMap.put(UpdateStep.CheckSum, "3101F00171");
		stepMap.put(UpdateStep.EraseMemory, "3101FF0071");	//remove parameter 44, in addParameter() add it.
		stepMap.put(UpdateStep.CheckProgrammCondition, "3101F00071");
		stepMap.put(UpdateStep.CheckProgrammDependency, "3101FF0171");
		stepMap.put(UpdateStep.ResetECU, "110151");
		stepMap.put(UpdateStep.RequestToDefaultSession, "100150");
	}
	
	
	public EnumMap<UpdateStep, String> getStepMap() {
		return stepMap;
	}

	public void initResponseDatabase(){
		resultResponse.clear();
		resultResponse.put(UpdateStep.ReadECUSparePartNumber, null);
		resultResponse.put(UpdateStep.ReadECUHardwareNumber, null);
		resultResponse.put(UpdateStep.ReadBootloaderID, null);
		resultResponse.put(UpdateStep.RequestToExtendSession, null);
		resultResponse.put(UpdateStep.DisableDTCStorage, null);
		resultResponse.put(UpdateStep.RequestToProgrammingSession, null);
		resultResponse.put(UpdateStep.RequestSeed, null);
		resultResponse.put(UpdateStep.SendKey, null);
		resultResponse.put(UpdateStep.WriteTesterSerialNumber, null);
		resultResponse.put(UpdateStep.WriteECUSparePartNumber, null);
		resultResponse.put(UpdateStep.WriteConfigureData, null);
		resultResponse.put(UpdateStep.WriteUpdateDate, null);
		resultResponse.put(UpdateStep.RequestDownload, null);
		resultResponse.put(UpdateStep.TransferData, null);
		resultResponse.put(UpdateStep.TransferExit, null);
		resultResponse.put(UpdateStep.CheckSum, null);
		resultResponse.put(UpdateStep.EraseMemory, null);
		resultResponse.put(UpdateStep.CheckProgrammCondition, null);
		resultResponse.put(UpdateStep.CheckProgrammDependency, null);
		resultResponse.put(UpdateStep.ResetECU, null);
		resultResponse.put(UpdateStep.EnableNonDiagComm, null);
		resultResponse.put(UpdateStep.EnableDTCStorage, null);
		resultResponse.put(UpdateStep.RequestToDefaultSession, null);
		resultResponse.put(UpdateStep.FlowControl, null);
	}
	
	/*
	public ArrayList<Integer> getCANMessage(UpdateStep step){
		CANMessage.clear();
		CANMessage.add(Integer.parseInt(stepMap.get(step), 16));
		return CANMessage;
	}*/
	
	public ArrayList<Byte> getCANMessage(UpdateStep step){
		CANMessage.clear();
		String str = stepMap.get(step);
		//将十六进制字符串转换为byte数组
		byte a[] = str.getBytes();
		for(int i=0; i< a.length; i++){ 
			if( a[i]>='0' && a[i]<='9'){ //数字0~9
				a[i] = (byte)(a[i]-'0');
			} else if( a[i]>='A' && a[i]<='Z'){ //大写字母 A~Z
				a[i] =(byte) (a[i]-'0'-7);
			} else if( a[i]>='a' && a[i]<='z'){ //小写字母 a~z
				a[i] =(byte) (a[i]-'A'-23);
			} else{
				System.out.println("The String isn't Hex!");
			}
		}
		for(int i=0; i<(a.length)/2; i++){
			CANMessage.add((byte)(a[2*i]<<4 | a[2*i+1]));
		}
		//CANMessage.add(Integer.parseInt(stepMap.get(step), 16));
		return CANMessage;
	}
	
	
	
	public EnumMap<UpdateStep, Boolean> getResultResponse() {
		return resultResponse;
	}

	public void setResultResponse(EnumMap<UpdateStep, Boolean> resultResponse) {
		this.resultResponse = resultResponse;
	}
	
	
	
	public ArrayList<Integer> getCANID(SQLiteDatabase db, String key){
		CANIdList.clear();
		Cursor cursor = db.rawQuery("select * from candb where manufacturer like ?", new String[]{key});
		if(cursor.moveToFirst()){
			/*
			String phyId = cursor.getString(cursor.getColumnIndex("phyCANid"));
			String funcId = cursor.getString(cursor.getColumnIndex("funcCANid"));
			String respId = cursor.getString(cursor.getColumnIndex("respCANid"));
			//System.out.printf("phyCANid=%s\n",fun);
			System.out.printf("phyCANid=%4h\n",Integer.parseInt(funcId, 16));
			
			/* 对于包含非数字（a,b,c）的字符串，直接使用parseInt()转换时会抛出异常  */
			/* 对于十六进制的字符串，在转换时需要使用 parseInt("", 16), 需要指定16进制，否则会报错  */
			CANIdList.add(Integer.parseInt(cursor.getString(cursor.getColumnIndex("phyCANid")), 16));  
			CANIdList.add(Integer.parseInt(cursor.getString(cursor.getColumnIndex("funcCANid")), 16));
			CANIdList.add(Integer.parseInt(cursor.getString(cursor.getColumnIndex("respCANid")), 16));
			/* 另外，在使用CANIdList之前，没有进行实例化, 导致运行时报空指针异常  */
			cursor.close();
		}
		
		return CANIdList;
	}
	
	private void insertData(SQLiteDatabase db, String manufaturer, String phyid, String funcid,  String respid)
	{
		// 执行插入语句
		db.execSQL("insert into candb values(null , ? , ?, ? , ?)"
			, new String[] {manufaturer, phyid, funcid, respid});
	}

}

