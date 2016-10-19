package com.zfchen.uds;

import com.zfchen.dbhelper.CANDatabaseHelper.UpdateStep;

public class ZotyeUpdateProcess implements UpdateProcess {

	ISO14229 iso14229;
	String[] filename;
	
	public ZotyeUpdateProcess(ISO14229 iso14229, String[] filename) {
		super();
		this.iso14229 = iso14229;
		this.filename = filename;
	}

	@Override
	public boolean readInfoFromECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.ReadECUHardwareNumber, "zotye", null);	//22 F1 87
		return false;
	}

	@Override
	public boolean PreProgrammSessionControl() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestToExtendSession, "zotye", null);
		iso14229.requestDiagService(UpdateStep.DisableDTCStorage, "zotye", null);
		iso14229.requestDiagService(UpdateStep.DisableNonDiagComm, "zotye", null);
		iso14229.requestDiagService(UpdateStep.RequestToProgrammingSession, "zotye", null);
		return false;
	}

	@Override
	public boolean writeInfoToECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.WriteTesterSerialNumber, "zotye", null);	//2E F1 87
		return false;
	}

	@Override
	public boolean securityAccess() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestSeed, "zotye", null);
		iso14229.requestDiagService(UpdateStep.SendKey, "zotye", null);
		return false;
	}

	@Override
	public boolean downloadDriverFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestDownload, "zotye", filePath);
		iso14229.requestDiagService(UpdateStep.TransferData, "zotye", null);
		iso14229.requestDiagService(UpdateStep.TransferExit, "zotye", null);
		iso14229.requestDiagService(UpdateStep.CheckSum, "zotye", null);
		iso14229.requestDiagService(UpdateStep.EraseMemory, "zotye", null);
		return false;
	}

	@Override
	public boolean downloadCalibrationFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestDownload, "zotye", filePath);
		iso14229.requestDiagService(UpdateStep.TransferData, "zotye", null);
		iso14229.requestDiagService(UpdateStep.TransferExit, "zotye", null);
		iso14229.requestDiagService(UpdateStep.CheckSum, "zotye", null);
		iso14229.requestDiagService(UpdateStep.CheckProgrammDependency, "zotye", null);
		return false;
	}
	
	@Override
	public boolean downloadApplicationFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestDownload, "zotye", filePath);
		iso14229.requestDiagService(UpdateStep.TransferData, "zotye", null);
		iso14229.requestDiagService(UpdateStep.TransferExit, "zotye", null);
		iso14229.requestDiagService(UpdateStep.CheckSum, "zotye", null);
		return false;
	}

	@Override
	public boolean resetECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.ResetECU, "zotye", null);
		return false;
	}

	@Override
	public boolean ExitProgrammSessionControl() {
		// TODO Auto-generated method stub
		return false;
	}

}
