package com.zfchen.uds;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;

import android.bluetooth.BluetoothSocket;
import android.database.sqlite.SQLiteDatabase;

import com.zfchen.uds.ISO15765;
import com.zfchen.uds.ISO15765.ReceiveThread;
import com.zfchen.dbhelper.CANDatabaseHelper;
import com.zfchen.dbhelper.CANDatabaseHelper.*;
import com.zfchen.ecusoftwareupdatetool.Hex2Bin;
import com.zfchen.ecusoftwareupdatetool.Crc;
import com.zfchen.ecusoftwareupdatetool.SecurityAccess;
import com.zfchen.ecusoftwareupdatetool.Seed2Key;
import com.zfchen.ecusoftwareupdatetool.UpdateActivity.UpdateSoftwareProcess;

public class ISO14229 {
	
	final int MaxBlockLength = 514;	/*指定每次传输的最大数据块长度*/
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
	String path;
	byte[] fileSize;
	byte[] startAddress;
	ReceiveThread receiveThread;
	EnumMap<UpdateStep, Boolean> result;
	SecurityAccess securityAccess;
	
	public ISO14229(CANDatabaseHelper helper, BluetoothSocket bTsocket, String manufacturer){
		super();
		this.db = helper.getReadableDatabase();
		helper.generateCanDB(db);
		
		if(manufacturer.equals("geely"))
			helper.generateUpdateGeelyDatabase();
		else
			helper.generateUpdateForyouDatabase();
		
		this.canDBHelper = helper;
		this.frame = new ArrayList<Byte>();
		this.h2b = new Hex2Bin();
		this.socket = bTsocket;
		this.manufacturer = manufacturer;
		this.result = helper.getResultResponse();
	}
	
	public SecurityAccess getSecurityAccess() {
		return securityAccess;
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
	protected boolean requestDiagService(UpdateStep step, String manufac, String filePath){	//ReadECUHardwareNumber
		
		boolean response = false;	/* 返回该报文对应的响应结果  */
		OutputStream outStream = null;
		ArrayList<Byte> message = canDBHelper.getCANMessage(step);
		
		try{
			outStream = this.socket.getOutputStream();
		}catch(IOException e){
			e.printStackTrace();
		}
		
		message.remove(message.size()-1);//移除积极响应码
		
		ArrayList<Integer> canIDList = canDBHelper.getCANID(db, manufac);
		
		if(step == UpdateStep.RequestToExtendSession || step == UpdateStep.RequestToDefaultSession
				|| step == UpdateStep.DisableDTCStorage || step == UpdateStep.EnableDTCStorage
				|| step == UpdateStep.DisableNonDiagComm || step == UpdateStep.EnableNonDiagComm){ //这几个诊断服务是功能寻址
			request_can_id = canIDList.get(1); //funcCANid
		} else{
			request_can_id = canIDList.get(0); //phyCANid
		}
		response_can_id = canIDList.get(2); //respCANid, 用来过滤从车载CAN网络接收到的报文(考虑将该参数传至下位机，然后由下位机对报文进行过滤)
		
		if(step == UpdateStep.RequestDownload || step == UpdateStep.CheckSum || step == UpdateStep.EraseMemory
				|| step == UpdateStep.SendKey || step == UpdateStep.DisableNonDiagComm 
				|| step == UpdateStep.WriteUpdateDate || step == UpdateStep.WriteECUSparePartNumber
				|| step == UpdateStep.WriteTesterSerialNumber ){
			//对于：请求下载、传输数据、校验数据、擦除逻辑块， 这几个服务需要带额外的参数
			this.addParameter(step, manufac, filePath, message);
		} else if(step == UpdateStep.TransferData){
			//this.data.addAll(0, message);
			//message.addAll(this.data);
			System.out.println("the length of file transfered is " + this.data.size());
			this.transferFile(MaxBlockLength, this.data, this.socket, request_can_id);
			return response;
		}
		
		this.iso15765 = new ISO15765(this.socket, request_can_id, response_can_id);
		
		if(receiveThread == null){
			receiveThread = iso15765.new ReceiveThread(this.socket, this.canDBHelper);
			receiveThread.start();
		}
		
		response = sendCANNetworkFrame(outStream, message, step);
		
		return response;
	}
	
	
	public boolean update(String manufac, String[] filePathList, UpdateSoftwareProcess step){
		//byte positiveResponse;
		boolean result = false;
		UpdateProcess updateProcess;
		
		this.securityAccess = new Seed2Key(manufac);
		
		switch (manufac) {
		//目前 zotye,baic和dfsk三者的升级流程都是参照华阳的软件升级规格书
		case "zotye":
			updateProcess = new ForyouUpdateProcess(this, filePathList, manufac);
			result = updateProcess.update();
			break;
			
		case "baic":
			updateProcess = new ForyouUpdateProcess(this, filePathList, manufac);
			result = updateProcess.update();
			break;
			
		case "dfsk":
			updateProcess = new ForyouUpdateProcess(this, filePathList, manufac);
			result = updateProcess.update();
			break;
					
		case "geely":	//吉利的升级流程参照"吉利的升级规格书"
			updateProcess = new GeelyUpdateProcess(this, filePathList, manufac);
			result = updateProcess.update();
			break;
			
		default:
			System.out.println("The manufacturer isn't supported!");
			break;
		}
		
		receiveThread.setStopReceiveMessageFlag(false);	//结束接收线程
		return result;
	}
	
	protected void addParameter(UpdateStep step, String manufac, String filePath, ArrayList<Byte> frame){

		byte[] check = null;
		if((path != filePath) && (filePath!=null)){
			h2b.getFileSize(filePath);
			//System.out.println(filePath);
			fileSize = h2b.getSize();
			startAddress = h2b.getStarting_address();	//起始地址
			
			System.out.print("startAddress = ");
			for (byte b : startAddress) {
				System.out.printf("%2h ",(int)(b&0xFF));
			}
			System.out.println();
			
			this.data = h2b.getHexFileData(filePath);	//hex文件的数据部分
			this.path = filePath;
		}
		
		if(step == UpdateStep.CheckSum){
			byte[] hex_data = new byte[this.data.size()];
			for (int i = 0; i < this.data.size(); i++) {
				hex_data[i] = this.data.get(i);
			}
			switch (manufac) {	//校验码(不同厂商规定的校验方式不同)
			case "zotye":
				crc = new Crc(0x4c11db7, 32, false, false);
				break;
			
			case "dfsk":
				crc = new Crc(0x4c11db7, 32, false, false);	//DFSK的校验方式有待确定	
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
			
			check = crc.CRC32(hex_data, hex_data.length);  //使用CRC校验方法，对转换后的数据流（bin文件）进行校验 
		}
		
		switch(step){//根据不同的诊断服务，加入对应的参数
		case DisableNonDiagComm:
			frame.add((byte) 0x01);	//enableRxAndDisableTx 
				break;
		
		case SendKey:
			byte[] seed = receiveThread.getSeed();
			byte[] key = this.getSecurityAccess().generateKey(seed, (byte) 5);
			for (byte b : key) {	//add starting address
				frame.add(b);
			}
			break;
			
		case RequestDownload:
			frame.add((byte) 0x00);	//DataFormatIdentifier(00:表示没有使用加密、解密方法)
			frame.add((byte) 0x44);	//addressAndLengthFormatIdentifier
			for (byte b : startAddress) {	//add starting address
				frame.add(b);
			}
			for (byte b : fileSize) {	//add data length
				frame.add(b);
			}
				break;
				
		case CheckSum:
			for (byte b : check) {	//add CRC result
				frame.add(b);
			}
				break;
				
		case EraseMemory:
			frame.add((byte) 0x44);	//AddressLengthFormatIdentifier
			for (byte b : startAddress) {	//add starting address
				frame.add(b);
			}
			for (byte b : fileSize) {	//add data length
				frame.add(b);
			}
				break;
		
		case WriteUpdateDate:
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd",Locale.CHINA);	//获取系统时间,需要加上位置信息(Locale)
			byte[] temp_data = sdf.format(new java.util.Date()).substring(2).getBytes();	/*ASC II格式的日期(例如：20161031)*/
			byte[] data = new byte[3];
			
			for(int i=0; i< temp_data.length; i++){
				if( temp_data[i]>='0' && temp_data[i]<='9'){
					temp_data[i] = (byte)(temp_data[i]-'0');
				} else {
					System.out.printf("The format of system time is error!");
				}
			}
			
			for(int i=0; i< temp_data.length-1; i+=2){
				data[(i/2)]=(byte) ((temp_data[i]<<4)|temp_data[i+1]);
			}
			
			for (byte b : data) {	//add current data
				frame.add(b);
			}
				break;
				
				
		case WriteECUSparePartNumber:
			byte[] partNum = "0123456789abcde".getBytes();
			for (byte c : partNum) {
				frame.add(c);
			}
			break;
			
		case WriteTesterSerialNumber:
			byte[] serialNum = "0123456789".getBytes();
			for (byte c : serialNum) {
				frame.add(c);
			}
			break;
				
		default:
				break;
		}
		
	}
	
	
	/** 除了添加下载数据外，还需要计算序号(初始序号从1开始，之后从0到F循环计数,每发送一个数据块需等待目标ECU的积极响应)
	 * @param maxBlockLength 最大的数据块长度
	 * @param transferData 待传输的数据（其中前两个字节为36 01——SID和sn）
	 * @param CANFrame 发送缓冲区（报文帧）
	 * @return
	 */
	protected void transferFile(int maxBlockLength, ArrayList<Byte> transferData, BluetoothSocket socket, int can_id){
		OutputStream outStream = null;
		//SendThread sendThread = iso15765.new SendThread(socket, null);
		ArrayList<Byte> blockFrame = new ArrayList<Byte>();
		blockFrame.ensureCapacity(maxBlockLength);
		int dataLength = transferData.size();	//原始数据长度
		
		byte SID = 0x36; //添加两个字节：SID(数据传输的ID号)和sn(块号)
		byte blockNumInitial = 1;
		int blockNum = dataLength/(maxBlockLength-2);  //最大块长度-2是因为每个块的前两个字节分别为：SID和sn（请求服务ID和块序号），后面的才是原始数据
		
		for (int i = 0; i < blockNum; i++) {
			blockFrame.clear();
			blockFrame.add(SID);	//添加诊断服务ID
			blockFrame.add(blockNumInitial++); //添加块号（块号循环:第一次是从1开始计数，自增到FF后，再从0开始循环计数，这里采用byte类型，溢出后自动变为0）
			
			try{
				outStream = this.socket.getOutputStream();
			}catch(IOException e){
				e.printStackTrace();
			}
			
			for(int j=0; j<(maxBlockLength-2); j++){	//取出一个数据块的内容
				blockFrame.add(transferData.get(j+(i*(maxBlockLength-2))));
			}
			
			sendCANNetworkFrame(outStream, blockFrame, UpdateStep.TransferData);
		}
		
		int lastBlockLength = dataLength%(maxBlockLength-2); //将最后一个数据块的内容提取出来(最后一个数据块长度不足maxBlockLength)
		if(0 != lastBlockLength){
			blockFrame.clear();
			blockFrame.add(SID);
			blockFrame.add(blockNumInitial++);
			for(int i=0; i<lastBlockLength; i++){
				blockFrame.add(transferData.get(dataLength-lastBlockLength+i));
			}
			
			sendCANNetworkFrame(outStream, blockFrame, UpdateStep.TransferData);
		}
	}

	/**
	 * @param outStream
	 * @param frame
	 */
	protected boolean sendCANNetworkFrame(OutputStream outStream,
			ArrayList<Byte> frame, UpdateStep step) {
		
		//发送请求前先清除对应的响应
		result.put(step, null);
		
		//发送块数据
		iso15765.PackCANFrameData(frame, iso15765.frameBuffer, request_can_id);
		int num = iso15765.frameBuffer.getFrame().size();
		for(int k=0; k<num; k++){
			/*
			for(int m=0; m<12; m++){
				int a = (int)(iso15765.frameBuffer.getFrame().get(k).data[m]&0xFF);
				System.out.printf("%2h ", a);
			}
			System.out.println();
			*/
			//将数据写入流中——发送数据
			try {
				outStream.write(iso15765.frameBuffer.getFrame().get(k).data);
				
				if((num>1) && (k==0)){	//multiple frame
					System.out.println("wait the flow control frame...");
					synchronized (result) {
						result.put(UpdateStep.FlowControl, null);
						result.wait(1000);//等待流控制帧
						if((result.get(UpdateStep.FlowControl) == null)||(result.get(UpdateStep.FlowControl) == false))
							return false;
							//System.out.println("Don't receive the flow control frame!");
						else
							result.put(UpdateStep.FlowControl, null);
					}
				}
				
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("wait the positive response...");
		synchronized (result) {
			try {
				result.wait(1000);
				if((result.get(step) == null)||(result.get(step) == false))
					return false;
					//System.out.println("Don't receive the positive response!");
				else
					result.put(step, null);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//等待
		}
		
		return true;
	}
	
}
