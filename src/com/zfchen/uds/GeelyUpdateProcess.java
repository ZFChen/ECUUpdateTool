package com.zfchen.uds;

import com.zfchen.dbhelper.CANDatabaseHelper.UpdateStep;

public class GeelyUpdateProcess implements UpdateProcess {
	ISO14229 iso14229;
	String[] filename;
	String manufacturer;
	public GeelyUpdateProcess(ISO14229 iso14229, String[] file, String manu) {
		super();
		this.iso14229 = iso14229;
		this.filename = file;
		this.manufacturer = manu;
	}

	@Override
	public boolean PreProgrammSessionControl() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestToExtendSession, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.DisableDTCStorage, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.DisableNonDiagComm, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.RequestToProgrammingSession, manufacturer, null);
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
	public boolean downloadApplicationFile(String filePath) {
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
	public boolean downloadDriverFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.EraseMemory, manufacturer, filePath);
		iso14229.requestDiagService(UpdateStep.RequestDownload, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.TransferData, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.TransferExit, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.CheckSum, manufacturer, null);
		return false;
	}
	
	@Override
	public boolean downloadCalibrationFile(String filePath) {
		// TODO Auto-generated method stub
		this.downloadDriverFile(filePath);
		return false;
	}
	
	@Override
	public boolean resetECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.ResetECU, manufacturer, null);
		return false;
	}

	@Override
	public boolean readInfoFromECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.ReadECUHardwareNumber, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.ReadBootloaderID, manufacturer, null);
		return false;
	}

	@Override
	public boolean writeInfoToECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.WriteTesterSerialNumber, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.WriteConfigureData, manufacturer, null);
		return false;
	}

	@Override
	public boolean ExitProgrammSessionControl() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestToExtendSession, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.EnableNonDiagComm, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.EnableDTCStorage, manufacturer, null);
		iso14229.requestDiagService(UpdateStep.RequestToDefaultSession, manufacturer, null);
		return false;
	}

	@Override
	public boolean update() {
		// TODO Auto-generated method stub
		this.readInfoFromECU();
		//System.out.println("readInfoFromECU");
		
		this.PreProgrammSessionControl();
		//System.out.println("PreProgrammSessionControl");
		
		this.securityAccess();
		//System.out.println("securityAccess");
		
		this.writeInfoToECU();
		//System.out.println("writeInfoToECU");
		
		this.downloadDriverFile(this.filename[0]);
		//System.out.println("downloadDriverFile");
		
		if(this.filename[1] != null)
			this.downloadApplicationFile(this.filename[1]);
		if(this.filename[2] != null)
			this.downloadCalibrationFile(this.filename[2]);
		this.resetECU();
		this.ExitProgrammSessionControl();
		return false;
	}
	
}
