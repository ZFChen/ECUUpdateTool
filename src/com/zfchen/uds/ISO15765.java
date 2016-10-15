package com.zfchen.uds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.bluetooth.BluetoothSocket;

public class ISO15765 {
	
	/* 网络层：CAN报文的帧类别  */
	enum ISO15765FrameType{
		SINGLE_FRAME,
		FIRST_FRAME,
		CONSECUTIVE_FRAME,
		FLOW_CONTROL_FRAME,
		INVALID_FRAME};
	
	/*-------CAN报文缓冲类定义--------*/
	public class Item{	//每条数据长度为12个字节，格式为： 0xee(一个起始字节)+CAN ID（2个字节）+数据（8个字节）+校验和（一个字节）
		public byte[] data = new byte[12];
	}
	
	public class CANFrameBuffer{
		protected ArrayList<Item> frame = null;
		
		public CANFrameBuffer(){
			super();
			frame = new ArrayList<Item>();
		}
		
		public ArrayList<Item> getFrame() {
			return frame;
		}

		public void setFrame(ArrayList<Item> frame) {
			this.frame = frame;
		}
	}
	
	ArrayList<Integer> CANReceiveBuffer = null;
	BluetoothSocket socket;
	
	int request_can_id = 0;
	int response_can_id = 0;
	ArrayList<Byte> receiveData;
	ArrayList<Byte> sendData;
	
	public ArrayList<Integer> getCANReceiveBuffer() {
		return CANReceiveBuffer;
	}

	CANFrameBuffer frameBuffer = null;
	OutputStream outStream;
	InputStream inStream;
	
	public ISO15765(BluetoothSocket bTsocket, int request_id, int response_id) {
		super();
		CANReceiveBuffer = new ArrayList<Integer>();
		frameBuffer = new CANFrameBuffer();
		this.socket = bTsocket;
		this.request_can_id = request_id;
		this.response_can_id = response_id;
	}
	
	public CANFrameBuffer getFrameBuffer() {
		return frameBuffer;
	}
	
	public void setSendData(ArrayList<Byte> sendData) {
		this.sendData = sendData;
	}

	public ArrayList<Byte> getReceiveData() {
		return receiveData;
	}

	/*---------------ISO15765协议的打包算法(应用层-->网络层)------------------*/
	/**
	 * @param al 待传输的数据
	 * @param sendBuf 发送缓冲区
	 * @param CANID 该报文的ID号（发往哪个节点）
	 */
	public void PackCANFrameData(ArrayList<Byte> al, CANFrameBuffer sendBuf, int CANID){
		int sn = 0;
		sendBuf.frame.clear();
		
		//根据报文的数据长度, 计算报文的帧数
		if(al.size() <= 7){
			sn = 1;	/*长度<=7, 采用单帧传输*/
		}else{
			sn = (al.size()-6)/7;	/* 第一帧的数据长度为6个字节，后面连续帧的数据长度为7个字节 */
			if((al.size()-6)%7 == 0){
				sn = sn + 1;
			}else{
				sn = sn + 2;
			}
		}

		for (int i = 0; i < sn; i++) {	/* 首先根据数据长度(帧数)填充 CANFrameBuffer */
			sendBuf.frame.add(new Item());
		}
		
		/* 网络层中,每条报文的数据长度为:1~4095（最长为4095个字节） */
		if(sn == 1){ /* single frame */
			sendBuf.getFrame().get(0).data[3] = (byte)al.size(); //填充“数据长度字节”
			for(byte i=0; i<al.size(); i++ ){
				sendBuf.getFrame().get(0).data[i+4] = al.get(i).byteValue(); /*byteValue():将Integer类型强制转换成byte，只取低八位，高位不要*/
			}
		}else{   /* multiple frame */
			sendBuf.getFrame().get(0).data[3] = (byte)(0x10+(al.size()>>8));  /* first frame, 前两个字节代表帧类型和数据长度*/
			sendBuf.getFrame().get(0).data[4] = (byte)al.size();
			for(byte i = 0;i < 6; i++){
				sendBuf.getFrame().get(0).data[i+5] = al.get(i).byteValue();
			}
			
			int CANConFrameNum = (al.size()-6)/7; /* 计算剩余数据所占的（连续）帧数 */
			/*最初连续帧的序号从1开始，递增至F后回到0，然后从0至F循环计数*/
			if(sn <= 16){ /* 第一帧、序号1-->序号F，共16帧报文 (没有循环计数)*/
				int index = 6;	/* 6是相对于第一帧的offset */
				/* snCount:连续帧的序号, 0x21~0x2F, 0x20~0x2F */
				byte snCount = 0x21;	/*0x21:2为N_PCITYPE(连续帧),1为序号 ,类型和序号组成连续帧数据的第一个字节*/
				for(int j = 1; j <= CANConFrameNum; j++){
					for(byte m = 0;m < 7;m++){
						sendBuf.getFrame().get(j).data[m+4] = al.get(index++).byteValue();
					}
					sendBuf.getFrame().get(j).data[3] = snCount++;
				}
				if((al.size()-6)%7 == 0){
					//刚好是整数帧
				}else{
					sendBuf.getFrame().get(CANConFrameNum+1).data[3] = snCount;
					for(int m = 0; m < (al.size()-6)%7; m++){
						sendBuf.getFrame().get(CANConFrameNum+1).data[m+4] = al.get(index++).byteValue();
					}
				}
			}else{  /*循环计数)*/
				int index = 6;
				byte snCount = 0x21;
				for(int j = 1;j < 16;j++){ /*序号从1到F*/
					for(int m = 0;m < 7;m++){
						sendBuf.getFrame().get(j).data[m+4] = al.get(index++).byteValue();
					}
					sendBuf.getFrame().get(j).data[3] = snCount++;
				}
				/*序号递增至F后,从0开始循环计数*/
				snCount = 0x20;
				for(int j = 1;j <= sn-16;j++){	/*sn-16:剩下的帧数（前面循环一轮刚好16帧）*/
					if(j != sn-16){
						for(int m = 0;m < 7;m++){
							sendBuf.getFrame().get(j+15).data[m+4] = al.get(index++).byteValue();
						}
					}else{ /* 最后一帧数据 */
						/*最后一帧数据的长度 = 总长度-(第一轮循环的数据)-(中间循环的数据), al.size()-(6+15*7)-(j-1)*7 ==> al.size()-(6+(j+14)*7) */
						for(int m = 0;m < al.size()-(6+(j+14)*7);m++){
							sendBuf.getFrame().get(j+15).data[m+4] = al.get(index++).byteValue();
						}
					}
					sendBuf.getFrame().get(j+15).data[3] = snCount++;
					if(snCount == 0x30){	//sn:2F-->20,循环计数
						snCount = 0x20;
					}
				}
			}
		}
		
		/* 填充帧头和checksum(与下位机采用同一种数据格式),格式为：0xee | CANID(H) | CANID(H) | N_PCI | checksum
		 * 其中N_PCI为网络层协议控制信息(Network layer protocol control information),包括:单帧、第一帧、连续帧和流控帧  */
		for(int i = 0;i < sn;i++){
			sendBuf.getFrame().get(i).data[0] = (byte)0xee;
			sendBuf.getFrame().get(i).data[1] = (byte)(CANID>>8);	/* 取CAN ID的高八位（对于标准帧，CAN ID为12位，这里用16位来表示） */
			sendBuf.getFrame().get(i).data[2] = (byte)CANID;		/* 取CAN ID的低八位 */
			sendBuf.getFrame().get(i).data[11] = (byte)this.CheckSum(sendBuf.getFrame().get(i).data);
		}
	}
	
	/*----------校验和计算------------*/
	protected int CheckSum(byte[] buffer){	/* 累加求和（数组最后一个数除外） */
		int checkSum = 0;
		for(int i = 0;i < buffer.length-2;i++){
			checkSum += buffer[i];
		}
		return checkSum;
	}
	
	/*---------------ISO15765协议的解包算法(网络层-->应用层)------------------*/
	public ArrayList<Byte> UnPackCANFrameData(CANFrameBuffer receiveBuf){
		int dataLength = 0;
		ArrayList<Byte> receiveData = new ArrayList<Byte>();
		if((receiveBuf.getFrame().get(0).data[3]&0xf0) == 0x10){	//第一帧 first frame
			//获取该条报文的长度
			dataLength = ((receiveBuf.getFrame().get(0).data[3]&0x0f)<<8)+receiveBuf.getFrame().get(0).data[4];	/* 第一帧中，第一个字节的底4位、第2个字节，组成数据长度 */
			
			for (int i = 0; i < dataLength; i++) {/* 首先根据数据长度(帧数)填充 ArrayList */
				receiveData.add((byte) 0);
			}
			
			//sn 表示帧的数量
			int sn = (dataLength-6)/7; /* 连续帧数量 */
			if((dataLength-6)%7 == 0){
				sn = sn + 1;
			}else{
				sn = sn + 2;
			}
			
			/*---------下面循环用于判断是否丢帧-----------*/
			for(int j = 2;j < sn;j++){
				//利用每次发送连续帧, "sn的值递增1"的特性作为检验依据
				/*第一帧没有序号，紧随其后的连续帧序号从2开始，因此这里的j初始值为2， 也就是计算第3帧报文与第2帧报文的序号之差 */
				int tempData = receiveBuf.getFrame().get(j).data[3] - receiveBuf.getFrame().get(j-1).data[3];
				if(tempData == 1){
					/* 两次序号间隔为1是正常情况 */
				}else if(tempData == -15){
					//sn递增到F之后, 重新从0开始计数(也就是从0~F循环计数,所以第一个字节的值对应为20~2F)
					if((receiveBuf.getFrame().get(j).data[3] == 0x20)&&(receiveBuf.getFrame().get(j-1).data[3] == 0x2f)){
						
					}else{
						return null;
					}
				}else{
					return null;
				}
			}
			
			for(int k = 0;k < 6;k++){	/* 第一帧中类型和数据长度占前两个2字节，数据占后6个字节，这里是取第一帧中的数据 */
				receiveData.set(k, (byte)receiveBuf.getFrame().get(0).data[k+5]);
			}
			
			if(sn > 2){ /* sn>2(多帧传输时sn>=2),表示有多条连续帧 */
				for(int k = 0;k < sn - 2;k++){
					for(int m = 0;m < 7;m++){
						/* 连续帧的索引(sn)从1开始（第一帧的索引为0）,以为k从0开始递增，所以这里使用k+1 */
						receiveData.set(m+k*7+6, (byte)receiveBuf.getFrame().get(k+1).data[m+4]);	/* 在自定义的数据格式中，连续帧的数据从第4个字节开始（第3个字节为类型和序号），所以这里的offset为4(m+4) */
					}
				}
				for(int k = 0;k < dataLength-(6+7*(sn-2));k++){ /*最后一条连续帧 ,剩余字节数 = 总长度-第一帧数据-前面所有的连续帧数据    ==> dataLength-6-(sn-2)*7 */
					receiveData.set(k+(sn-2)*7+6, (byte)receiveBuf.getFrame().get(sn-1).data[k+4]);	/* 总共sn帧报文，最后一帧的索引为sn-1（0到sn-1） */
				}
			}else{  /* sn=2(多帧传输时sn>=2),表示只有一条连续帧 */
				for(int k = 0;k < dataLength-6;k++){
					receiveData.set(k+6, (byte)receiveBuf.getFrame().get(1).data[k+4]);
					
				}
			}
		}else{//single frame
			dataLength = receiveBuf.getFrame().get(0).data[3];
			for(int i = 0;i < dataLength;i++){
				receiveData.set(i, (byte)receiveBuf.getFrame().get(0).data[i+4]);
			}
		}
		return receiveData;
	}
	
	/*-----------接收到数据后根据帧类型进行处理-----------*/
	
	/**
	 * @param data 帧类型字节
	 * @param receiveBuffer 接收报文缓冲区（存储一帧报文，12个字节）
	 * @param id 目标ECU节点地址
	 */
	public int ReceiveNetworkFrameHandle(byte[] receiveBuffer, int id){
		ISO15765FrameType frameType;
		byte type = (byte) (receiveBuffer[3]&0xf0);
		int length = 0;
		//boolean result = true;	/* 该初始值有待确定 */
		switch(type){//根据不同的传输方式(帧格式)进行处理
			case 0x00:
				frameType = ISO15765FrameType.SINGLE_FRAME;//单帧
					break;
			case 0x10:
				frameType = ISO15765FrameType.FIRST_FRAME;//第一帧
					break;
			case 0x20:
				frameType = ISO15765FrameType.CONSECUTIVE_FRAME;//连续帧
					break;
			case 0x30:
				frameType = ISO15765FrameType.FLOW_CONTROL_FRAME;//流控制帧
					break;
			default:
				frameType = ISO15765FrameType.INVALID_FRAME;//无效帧
					break;
		}
		
		switch(frameType){
			case SINGLE_FRAME:
				for(int i = 0;i < 12;i++){
					this.frameBuffer.getFrame().get(0).data[i] = receiveBuffer[i];
				}
				length = receiveBuffer[3];
				break;
				
			case FLOW_CONTROL_FRAME:
					for(int i = 0;i < 12;i++){
						this.frameBuffer.getFrame().get(0).data[i] = receiveBuffer[i];
					}
					break;
					
			case FIRST_FRAME:	/* 接收到第一帧,需要发送流控制帧 */
					length = ((receiveBuffer[3]&0x0f)<<8)+receiveBuffer[4];	/* 获取报文（网络层）的数据长度 */
					//System.out.println("Total receive CAN message length = "+length);
					Item item = new Item();
					item.data = SendFlowControlFrame(id);
					this.SendMessageToDevice(item.data);
					break;
					
			case CONSECUTIVE_FRAME:
					break;
					
			case INVALID_FRAME:
					break;
		}
		return length;
	}
	
	/*----------发送流控帧---------*/
	public byte[] SendFlowControlFrame(int CANID){
		byte[] sendBuffer = new byte[12];
		sendBuffer[0] = (byte)0xee;
		sendBuffer[1] = (byte)(CANID>>8);
		sendBuffer[2] = (byte)CANID;
		sendBuffer[3] = (byte)0x30;	/* 0x30:其中3表示流控帧， 0表示流状态（继续发送） */
		sendBuffer[4] = (byte)0x0;	/* BS = 0 */
		sendBuffer[5] = (byte)0x01;	/* STmin = 1ms */
		sendBuffer[11] = (byte)CheckSum(sendBuffer);
		return sendBuffer;
	}
	
	/*------将数据发送给蓝牙转接板(下位机)---------*/
	public void SendMessageToDevice(byte[] buf){
		try{
			outStream = socket.getOutputStream();
			outStream.write(buf);
		}catch(IOException i){
			i.printStackTrace();
		}
	}
	
	protected class ReceiveThread extends Thread{
		BluetoothSocket socket;
		InputStream inStream;
		CANFrameBuffer buf;
		
		public ReceiveThread(BluetoothSocket socket) {
			super();
			// TODO Auto-generated constructor stub
			this.socket = socket;
			try{
				inStream = socket.getInputStream();
			}catch(IOException e){
				e.printStackTrace();
			}
		}

		@Override
		public synchronized void start() {
			// TODO Auto-generated method stub
			super.start();
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			byte[] tempBuffer = new byte[12];
			int num = 0;
			buf = new CANFrameBuffer();
			int id = 0;
			
			while(id == response_can_id){
				try{
					num += inStream.read(tempBuffer); //每次读取最多12个字节, 返回实际读取到的字节数 
					if(num < 12)
						id = (int)((tempBuffer[1]<<8)|tempBuffer[2]);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			 
			int length = ReceiveNetworkFrameHandle(tempBuffer, request_can_id);
			Item item = new Item();
			item.data = tempBuffer.clone();
			buf.getFrame().add(item);
			
			if(length <= 7){
				//single frame
			}else {
				//根据报文的数据长度, 计算报文的帧数
				int sn = (length-6)/7;	/* 第一帧的数据长度为6个字节，后面连续帧的数据长度为7个字节 */
				if((length-6)%7 == 0){
					sn = sn + 1;
				}else{
					sn = sn + 2;
				}
				for(int i=0; i<sn; i++){
					if(id == response_can_id){	//流控制帧不能存入接收数据缓存中
						try {
							inStream.read(tempBuffer);
							Item it = new Item();
							it.data = tempBuffer.clone();
							buf.getFrame().add(it);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			if(tempBuffer[3] == (byte)0x30) //接收到流控帧
				this.notify();	//唤醒其他线程
			
			receiveData = UnPackCANFrameData(buf);	//返回接收到的数据
		}
	}
	
	
	public class SendThread extends Thread{
		BluetoothSocket socket;
		OutputStream outStream;
		ArrayList<Byte> receiveData;
		CANFrameBuffer buf;
		
		public SendThread(BluetoothSocket socket, ArrayList<Byte> output) {
			super();
			// TODO Auto-generated constructor stub
			this.socket = socket;
			sendData = output;
			/*
			try{
				outStream = socket.getOutputStream();
			}catch(IOException e){
				e.printStackTrace();
			}
			*/
		}

		@Override
		public synchronized void start() {
			// TODO Auto-generated method stub
			super.start();
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			
			buf = new CANFrameBuffer();
			PackCANFrameData(sendData, buf, request_can_id);
			
			//byte[] tempBuffer = new byte[12];
			int num = buf.getFrame().size();
			/*
			try{
				outStream.write(buf.getFrame().get(0).data);	// 发送第一帧/单帧
				
				ReceiveThread receiveThread = new ReceiveThread(socket);
				receiveThread.start();
				
				if(num > 1){	//对于多帧传输,发送在第一帧之后需要等待接收流控制帧
					wait();
					for(int i=1; i<=num-1; i++)
						outStream.write(buf.getFrame().get(i).data);
				}

			}catch(IOException | InterruptedException e){
				e.printStackTrace();
			}
			*/
			for(int j=0; j<num; j++){
				for(int i=0; i<12; i++){
					int a = (int)(buf.getFrame().get(j).data[i]&0xFF);
					System.out.printf("%2h ", a);
				}
				System.out.println();
			}
		}
	}
}
