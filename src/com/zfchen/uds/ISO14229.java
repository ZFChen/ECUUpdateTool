package com.zfchen.uds;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.bluetooth.BluetoothSocket;
import android.database.sqlite.SQLiteDatabase;

import com.zfchen.uds.ISO15765;
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
	String path;
	byte[] fileSize;
	byte[] startAddress;
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
		OutputStream outStream = null;
		ArrayList<Byte> message = canDBHelper.getCANMessage(step);
		
		try{
			outStream = this.socket.getOutputStream();
		}catch(IOException e){
			e.printStackTrace();
		}
		positiveCode = message.get(message.size()-1);	//取出积极响应码
		message.remove(message.size()-1);

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
			//this.data.addAll(0, message);
			//message.addAll(this.data);
			System.out.println("the length of file transfered is " + this.data.size());
			this.transferFile(514, this.data, this.socket, request_can_id);
			return positiveCode;
		}
		
		this.iso15765 = new ISO15765(this.socket, request_can_id, response_can_id);
		//发送块数据
		//iso15765.new SendThread(socket, message).start();
		
		iso15765.PackCANFrameData(message, iso15765.frameBuffer, request_can_id);
		int num = iso15765.frameBuffer.getFrame().size();
		for(int i=0; i<num; i++){
			for(int j=0; j<12; j++){
				int a = (int)(iso15765.frameBuffer.getFrame().get(i).data[j]&0xFF);
				System.out.printf("%2h ", a);
			}
			System.out.println();
			
			try {
				outStream.write(iso15765.frameBuffer.getFrame().get(i).data);
				Thread.sleep(1);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//iso15765.PackCANFrameData(message, iso15765.frameBuffer, request_can_id);
		//iso15765.new SendThread(socket, message).start();
		//发送块数据
		return positiveCode;
	}
	
	public boolean update(String manufac, String[] filePathList, UpdateSoftwareProcess step){
		//byte positiveResponse;
		boolean result = false;
		UpdateProcess updateProcess;
		switch (manufac) {
		case "zotye":
		case "baic":
		case "dfsk":	//目前 zotye,baic和dfsk三者的升级流程一样
			updateProcess = new ZotyeUpdateProcess(this, filePathList);
			updateProcess.update();
			break;
					
		case "geely":
			updateProcess = new GeelyUpdateProcess(this, filePathList);
			updateProcess.update();
			break;
			
		default:
			System.out.println("The manufacturer isn't supported!");
			break;
		}
		
		result = true;
		return result;
	}
	
	protected void addParameter(UpdateStep step, String manufac, String filePath, ArrayList<Byte> frame){

		byte[] check = null;
		if((path != filePath) && (filePath!=null)){
			h2b.getFileSize(filePath);
			System.out.println(filePath);
			fileSize = h2b.getSize();
			System.out.println(fileSize);
			startAddress = h2b.getStarting_address();	//起始地址
			System.out.println(startAddress);
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
		case RequestDownload:
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
			
			for(int j=0; j<(maxBlockLength-2); j++){	//取出一个数据块的内容
				blockFrame.add(transferData.get(j+(i*(maxBlockLength-2))));
			}
			/*
			iso15765.setSendData(blockFrame);
			if(sendThread.getState() == Thread.State.NEW)
				sendThread.start();
			else if(sendThread.getState() == Thread.State.WAITING)
				sendThread.notifyAll();
			*/
			
			//发送块数据
			iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, request_can_id);
			try{
				outStream = this.socket.getOutputStream();
			}catch(IOException e){
				e.printStackTrace();
			}
			int num = iso15765.frameBuffer.getFrame().size();
			for(int k=0; k<num; k++){
				for(int m=0; m<12; m++){
					int a = (int)(iso15765.frameBuffer.getFrame().get(k).data[m]&0xFF);
					System.out.printf("%2h ", a);
				}
				System.out.println();
				
				//将数据写入流中——发送数据
				try {
					outStream.write(iso15765.frameBuffer.getFrame().get(k).data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		int lastBlockLength = dataLength%(maxBlockLength-2); //将最后一个数据块的内容提取出来(最后一个数据块长度不足maxBlockLength)
		if(0 != lastBlockLength){
			blockFrame.clear();
			blockFrame.add(SID);
			blockFrame.add(blockNumInitial++);
			for(int i=0; i<lastBlockLength; i++){
				blockFrame.add(transferData.get(dataLength-lastBlockLength+i));
			}
			
			/*
			iso15765.setSendData(blockFrame);
			if(sendThread.getState() == Thread.State.NEW)
				sendThread.start();
			else if(sendThread.getState() == Thread.State.WAITING)
				sendThread.notifyAll();
			
			sendThread.setFlag(false);
			*/
			
			//iso15765.new SendThread(socket, blockFrame).start();
			iso15765.PackCANFrameData(blockFrame, iso15765.frameBuffer, request_can_id);
			int num = iso15765.frameBuffer.getFrame().size();
			for(int k=0; k<num; k++){
				for(int m=0; m<12; m++){
					int a = (int)(iso15765.frameBuffer.getFrame().get(k).data[m]&0xFF);
					System.out.printf("%2h ", a);
				}
				System.out.println();
				
				//将数据写入流中——发送数据
				try {
					outStream.write(iso15765.frameBuffer.getFrame().get(k).data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
	
	
	/*
	public class SendThread extends Thread{
		BluetoothSocket socket;
		OutputStream outStream;
		InputStream inStream;
		ArrayList<Byte> receiveData;
		ArrayList<Byte> sendData;
		CANFrameBuffer buf;
		boolean flag = true;
		
		String[] filePathList;
		UpdateSoftwareProcess step;
		
		public SendThread(BluetoothSocket socket, ArrayList<Byte> output, String[] filePath) {
			super();
			// TODO Auto-generated constructor stub
			this.socket = socket;
			this.sendData = output;
			this.filePathList = filePath;
			
			try{
				this.outStream = socket.getOutputStream();
				this.inStream  = socket.getInputStream();
			}catch(IOException e){
				e.printStackTrace();
			}
			
		}

		public void setFlag(boolean flag) {
			this.flag = flag;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while(flag == true){
				buf = iso15765.new CANFrameBuffer();
				iso15765.PackCANFrameData(sendData, buf, request_can_id);
				
				byte[] tempBuffer = new byte[12];
				int num = buf.getFrame().size();
				
				try{
					outStream.write(buf.getFrame().get(0).data);	// 发送第一帧/单帧
					num += inStream.read(tempBuffer); //每次读取最多12个字节, 返回实际读取到的字节数 
					if(num < 12)
						id = (int)((tempBuffer[1]<<8)|tempBuffer[2]);
					
					if(num > 1){	//对于多帧传输,发送在第一帧之后需要等待接收流控制帧
						//wait();
						for(int i=1; i<=num-1; i++){
							outStream.write(buf.getFrame().get(i).data);
							sleep(1);	//sleep 1 ms
						}
					}
	
				}catch(IOException | InterruptedException e){
					e.printStackTrace();
				}

				for(int j=0; j<num; j++){
					for(int i=0; i<12; i++){
						int a = (int)(buf.getFrame().get(j).data[i]&0xFF);
						System.out.printf("%2h ", a);
					}
					System.out.println();
				}
				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	*/
	
}
