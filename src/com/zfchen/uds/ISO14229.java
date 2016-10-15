package com.zfchen.uds;

import java.util.ArrayList;

import android.bluetooth.BluetoothSocket;
import android.database.sqlite.SQLiteDatabase;

import com.zfchen.uds.ISO15765;
import com.zfchen.uds.ISO15765.CANFrameBuffer;
import com.zfchen.uds.ISO15765.SendThread;

import com.zfchen.dbhelper.CANDatabaseHelper;
import com.zfchen.dbhelper.CANDatabaseHelper.*;
import com.zfchen.ecusoftwareupdatetool.Hex2Bin;
import com.zfchen.ecusoftwareupdatetool.Crc;
import com.zfchen.ecusoftwareupdatetool.UpdateActivity.UpdateSoftwareProcess;

public class ISO14229 {
	
	final int blockLength = 514;	/*指定每次传输的最大数据块长度*/
	int response_can_id = 0;
	int request_can_id = 0;
	SQLiteDatabase db = null;
	byte[] CAN_ID = new byte[2];
	CANDatabaseHelper canDBHelper = null;
	ArrayList<Byte> frame = null;
	ISO15765 iso15765 = null;
	Hex2Bin h2b = null;
	Crc crc = null;
	BluetoothSocket socket;
	ArrayList<Byte> data;
	String manufacturer;
	String[] filePath;
	
	public ISO14229(CANDatabaseHelper helper, BluetoothSocket bTsocket, String manufacturer){
		super();
		this.db = helper.getReadableDatabase();
		helper.generateCanDB(db);
		helper.generateUpdateDB(db);
		
		this.canDBHelper = helper;
		this.frame = new ArrayList<Byte>();
		this.h2b = new Hex2Bin();
		this.socket = bTsocket;
		this.manufacturer = manufacturer;
	}
	
	
	public void setFilePath(String[] filePath) {
		this.filePath = filePath;
	}


	/**
	 * @param step 升级步骤
	 * @param manufac 车厂名
	 * @param filePath 升级文件的路径
	 * @return
	 */
	protected byte requestDiagService(UpdateStep step, String manufac, String filePath){	//ReadECUHardwareNumber
		byte positiveCode = 0;	/* 返回该报文对应的积极响应码  */
		SendThread sendThread;
		ArrayList<Byte> message = canDBHelper.getCANMessage(step);
		/*
		for (Byte b : al) {
			System.out.printf("%2h\n",b);
		}
		*/
		positiveCode = message.get(message.size()-1);	//取出积极响应码
		message.remove(message.size()-1);

		//System.out.println(canDBHelper);
		//System.out.println(db);
		ArrayList<Integer> canIDList = canDBHelper.getCANID(db, manufac);
		
		if(step == UpdateStep.RequestToExtendSession || step == UpdateStep.RequestToDefaultSession
				|| step == UpdateStep.DisableDTCStorage || step == UpdateStep.EnableDTCStorage
				|| step == UpdateStep.DisableNonDiagComm || step == UpdateStep.EnableNonDiagComm){ //这几个诊断服务是功能寻址
			request_can_id = canIDList.get(1); //funcCANid
		} else{
			request_can_id = canIDList.get(0); //phyCANid
		}
		response_can_id = canIDList.get(2); //respCANid, 用来过滤从车载CAN网络接收到的报文(考虑将该参数传至下位机，然后由下位机对报文进行过滤)
		
		if(step == UpdateStep.RequestDownload || step == UpdateStep.CheckSum || step == UpdateStep.EraseMemory){
			//对于：请求下载、传输数据、校验数据、擦除逻辑块， 这几个服务需要带额外的参数
			this.addParameter(step, manufac, filePath, message);
		} else if(step == UpdateStep.TransferData){
			this.transferFile(0x514, this.data, this.socket, request_can_id);
			return positiveCode;
		}
		
		this.iso15765 = new ISO15765(this.socket, request_can_id, response_can_id);
		//发送块数据
		//sendThread = iso15765.new SendThread(socket, message);
		//sendThread.start();
		
		iso15765.PackCANFrameData(message, iso15765.frameBuffer, request_can_id);
		int num = iso15765.frameBuffer.getFrame().size();
		for(int i=0; i<num; i++){
			for(int j=0; j<12; j++){
				int a = (int)(iso15765.frameBuffer.getFrame().get(i).data[j]&0xFF);
				System.out.printf("%2h ", a);
			}
			System.out.println();
		}
		//iso15765.PackCANFrameData(message, iso15765.frameBuffer, request_can_id);
		//SendThread sendThread = new SendThread(socket, iso15765.frameBuffer);
		//发送块数据
		//sendThread.start();
		return positiveCode;
	}
	
	public boolean update(String manufac, String[] filePathList, UpdateSoftwareProcess step){
		byte positiveResponse;
		boolean result = false;
		
		step.setProcessStep(UpdateStep.ReadECUHardwareNumber);
		positiveResponse = requestDiagService(UpdateStep.ReadECUHardwareNumber, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
		
		step.setProcessStep(UpdateStep.ReadBootloaderID);
		positiveResponse = requestDiagService(UpdateStep.ReadBootloaderID, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
		
		step.setProcessStep(UpdateStep.RequestToExtendSession);
		positiveResponse = requestDiagService(UpdateStep.RequestToExtendSession, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
		
		step.setProcessStep(UpdateStep.DisableDTCStorage);
		positiveResponse = requestDiagService(UpdateStep.DisableDTCStorage, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.DisableNonDiagComm);
		positiveResponse = requestDiagService(UpdateStep.DisableNonDiagComm, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.RequestToProgrammingSession);
		positiveResponse = requestDiagService(UpdateStep.RequestToProgrammingSession, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.RequestSeed);
		positiveResponse = requestDiagService(UpdateStep.RequestSeed, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.SendKey);
		positiveResponse = requestDiagService(UpdateStep.SendKey, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.WriteTesterSerialNumber);
		positiveResponse = requestDiagService(UpdateStep.WriteTesterSerialNumber, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.WriteConfigureDate);
		positiveResponse = requestDiagService(UpdateStep.WriteConfigureDate, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		//下载flash driver文件
		step.setProcessStep(UpdateStep.RequestDownload);
		positiveResponse = requestDiagService(UpdateStep.RequestDownload, manufacturer, filePathList[0]);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.TransferData);
		positiveResponse = requestDiagService(UpdateStep.TransferData, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.TransferExit);
		positiveResponse = requestDiagService(UpdateStep.TransferExit, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.CheckSum);
		positiveResponse = requestDiagService(UpdateStep.CheckSum, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
		step.setProcessStep(UpdateStep.EraseMemory);
		positiveResponse = requestDiagService(UpdateStep.EraseMemory, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		//下载application或calibration data文件(重复数据下载过程)
//		if(filePathList[1] != null){
//			positiveResponse = requestDiagService(UpdateStep.RequestDownload, manufacturer, filePathList[1]);//application
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
//			positiveResponse = requestDiagService(UpdateStep.TransferData, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
//			positiveResponse = requestDiagService(UpdateStep.TransferExit, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
//			positiveResponse = requestDiagService(UpdateStep.CheckSum, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//		}
//		if(filePathList[2] != null){
//			positiveResponse = requestDiagService(UpdateStep.RequestDownload, manufacturer, filePathList[2]);//calibration data
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
//			positiveResponse = requestDiagService(UpdateStep.TransferData, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
//			positiveResponse = requestDiagService(UpdateStep.TransferExit, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//			
//			positiveResponse = requestDiagService(UpdateStep.CheckSum, manufacturer, null);
//			if(iso15765.getReceiveData().get(0) != positiveResponse)
//				return result;
//		}
//		
//		step.setProcessStep(UpdateStep.CheckProgrammDependency);
//		positiveResponse = requestDiagService(UpdateStep.CheckProgrammDependency, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		step.setProcessStep(UpdateStep.ResetECU);
//		positiveResponse = requestDiagService(UpdateStep.ResetECU, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		step.setProcessStep(UpdateStep.RequestToExtendSession);
//		positiveResponse = requestDiagService(UpdateStep.RequestToExtendSession, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		step.setProcessStep(UpdateStep.EnableNonDiagComm);
//		positiveResponse = requestDiagService(UpdateStep.EnableNonDiagComm, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		step.setProcessStep(UpdateStep.EnableDTCStorage);
//		positiveResponse = requestDiagService(UpdateStep.EnableDTCStorage, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
//		
//		step.setProcessStep(UpdateStep.RequestToDefaultSession);
//		positiveResponse = requestDiagService(UpdateStep.RequestToDefaultSession, manufacturer, null);
//		if(iso15765.getReceiveData().get(0) != positiveResponse)
//			return result;
		
		result = true;
		return result;
	}
	
	protected void addParameter(UpdateStep step, String manufac, String filePath, ArrayList<Byte> frame){
		//int dataLength = h2b.getFileSize(filePath);	//数据长度
		h2b.getFileSize(filePath);
		byte[] fileSize = h2b.getSize();
		byte[] startAddress = h2b.getStarting_address();	//起始地址
		this.data = h2b.getHexFileData(filePath); //hex文件的数据部分
		
		byte[] hex_data = new byte[data.size()];
		for (int i = 0; i < hex_data.length; i++) {
			hex_data[i] = data.get(i);
		}
		switch (manufac) {//校验码(不同厂商规定的校验方式不同)
		case "zotye":
			crc = new Crc(0x4c11db7, 32, false, false);
			break;
		
		case "dfsk":
			crc = new Crc(0x4c11db7, 32, false, false); //DFSK的校验方式有待确定	
			break;
					
		case "geely":
			crc = new Crc(0x4c11db7, 32, true, true);
			break;
			
		case "baic":
			crc = new Crc(0x4c11db7, 32, false, false); //BAIC的校验方式有待确定	
			break;	
			
		default:
			System.out.println("The manufacturer isn't supported!");
			break;
		}
		byte[] check = crc.CRC32(hex_data, data.size());  //使用Crc校验方法，对转换后的数据流（bin文件）进行校验 
		
		switch(step){//根据不同的诊断服务，加入对应的参数
		case RequestDownload:
			for (byte b : startAddress) {	//add starting address
				frame.add(b);
			}
			for (byte b : fileSize) {	//add data length
				frame.add(b);
			}
				break;
		/*		
		case TransferData:
			//除了添加下载数据外，还需要计算序号(初始序号从1开始，之后从0到F循环计数,每发送一个数据块需等待目标ECU的积极响应)
			
				break;*/
				
		case CheckSum:
			for (byte b : check) {	//add CRC result
				frame.add(b);
			}
				break;
				
		case EraseMemory:
			for (byte b : startAddress) {	//add starting address
				frame.add(b);
			}
			for (byte b : fileSize) {	//add data length
				frame.add(b);
			}
				break;
				
		default:
				break;
		}
		
	}
	
	
	/**
	 * @param maxBlockLength 最大的数据块长度
	 * @param transferData 待传输的数据（其中前两个字节为36 01——SID和sn）
	 * @param CANFrame 发送缓冲区（报文帧）
	 * @return
	 */
	protected void transferFile(int maxBlockLength, ArrayList<Byte> transferData, BluetoothSocket socket, int can_id){
		
		//SendThread sendThread = iso15765.new SendThread(socket, transferData);
		SendThread sendThread = iso15765.new SendThread(socket, null);
		ArrayList<Byte> blockFrame = new ArrayList<Byte>();
		blockFrame.ensureCapacity(maxBlockLength);
		int dataLength = transferData.size()-2;	//减去前两个字节：SID和sn
		byte SID = transferData.get(0);
		byte blockNumInitial = transferData.get(1);	//块号的初始值
		int blockNum = dataLength/(maxBlockLength-2);  //最大块长度-2是因为每个块的前两个字节分别为：SID和sn（请求服务ID和块序号），后面的才是原始数据。
		
		for (int i = 0; i < blockNum; i++) {
			//blockFrame.clear();
			blockFrame.add(SID);	//添加诊断服务ID
			blockFrame.add(blockNumInitial++); //添加块号（块号循环:第一次是从1开始计数，自增到FF后，再从0开始循环计数，这里采用byte类型，溢出后自动变为0）
			
			for(int j=0; j<(maxBlockLength-2); j++){	//取出一个数据块的内容
				blockFrame.add(transferData.get(j+(i*(maxBlockLength-2))));
				//iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, can_id);
				//发送块数据
				//sendThread = iso15765.new SendThread(socket, blockFrame);
				iso15765.setSendData(blockFrame);
				sendThread.start();
			}
		}
		
		int lastBlockLength = dataLength%(maxBlockLength-2); //将最后一个数据块的内容提取出来(最后一个数据块长度不足maxBlockLength)
		if(0 != lastBlockLength){
			blockFrame.clear();
			blockFrame.add(SID);
			blockFrame.add(blockNumInitial++);
			for(int i=0; i<lastBlockLength; i++){
				blockFrame.add(transferData.get(dataLength-lastBlockLength+i));
				//iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, can_id);
				//发送块数据
				//sendThread = iso15765.new SendThread(socket, blockFrame);
				iso15765.setSendData(blockFrame);
				sendThread.start();
			}
		}
	}
	
}
