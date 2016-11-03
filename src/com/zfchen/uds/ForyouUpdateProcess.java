package com.zfchen.uds;

import com.zfchen.dbhelper.CANDatabaseHelper.UpdateStep;

public class ForyouUpdateProcess implements UpdateProcess {

	ISO14229 iso14229;
	String[] filename;
	String manufacturer;
	public ForyouUpdateProcess(ISO14229 iso14229, String[] filename, String manu) {
		super();
		this.iso14229 = iso14229;
		this.filename = filename;
		this.manufacturer = manu;
	}

	@Override
	public boolean readInfoFromECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestToExtendSession, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.ReadECUSparePartNumber, "zotye", null);	//22 F1 87
		return false;
	}

	@Override
	public boolean PreProgrammSessionControl() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.CheckProgrammCondition, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.DisableDTCStorage, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.DisableNonDiagComm, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.RequestToProgrammingSession, manufacturer, null);
		return false;
	}

	@Override
	public boolean writeInfoToECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.WriteECUSparePartNumber, manufacturer, null);	//2E F1 87
		iso14229.requestDiagService(UpdateStep.WriteTesterSerialNumber, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.WriteUpdateDate, manufacturer, null);	//写入升级日期(需要加入日期，格式为：16 08 29)
		return false;
	}

	@Override
	public boolean securityAccess() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestSeed, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.SendKey, manufacturer, null);
		return false;
	}

	@Override
	public boolean downloadDriverFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestDownload, manufacturer, filePath);
		iso14229.requestDiagService(UpdateStep.TransferData, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.TransferExit, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.CheckSum, manufacturer, null);
		
		return false;
	}

	@Override
	public boolean downloadCalibrationFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.EraseMemory, manufacturer, filePath);
		iso14229.requestDiagService(UpdateStep.RequestDownload, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.TransferData, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.TransferExit, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.CheckSum, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.CheckProgrammDependency, manufacturer, null);
		return false;
	}
	
	@Override
	public boolean downloadApplicationFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.EraseMemory, manufacturer, filePath);
		iso14229.requestDiagService(UpdateStep.RequestDownload, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.TransferData, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.TransferExit, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.CheckSum, manufacturer, null);
		return false;
	}

	@Override
	public boolean resetECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.ResetECU, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.RequestToDefaultSession, manufacturer, null);
		return false;
	}

	@Override
	public boolean ExitProgrammSessionControl() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update() {
		// TODO Auto-generated method stub
		this.readInfoFromECU();
		this.PreProgrammSessionControl();
		this.securityAccess();
		// 0:driver, 1:application, 2:calibration data
		this.downloadDriverFile(this.filename[0]);
		
		if(this.filename[1] != null)
			this.downloadApplicationFile(this.filename[1]);
		if(this.filename[2] != null)
			this.downloadCalibrationFile(this.filename[2]);
		
		this.writeInfoToECU();
		this.resetECU();
		
		return false;
	}
	
	
}
