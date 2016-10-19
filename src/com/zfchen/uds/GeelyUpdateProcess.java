package com.zfchen.uds;

import com.zfchen.dbhelper.CANDatabaseHelper.UpdateStep;

public class GeelyUpdateProcess implements UpdateProcess {
	ISO14229 iso14229;
	String[] filename;
	public GeelyUpdateProcess(ISO14229 iso14229, String[] file) {
		super();
		this.iso14229 = iso14229;
		this.filename = file;
	}

	@Override
	public boolean PreProgrammSessionControl() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestToExtendSession, "geely", null);
		iso14229.requestDiagService(UpdateStep.DisableDTCStorage, "geely", null);
		iso14229.requestDiagService(UpdateStep.DisableNonDiagComm, "geely", null);
		iso14229.requestDiagService(UpdateStep.RequestToProgrammingSession, "geely", null);
		return false;
	}

	@Override
	public boolean securityAccess() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestSeed, "geely", null);
		iso14229.requestDiagService(UpdateStep.SendKey, "geely", null);
		return false;
	}

	@Override
	public boolean downloadApplicationFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestDownload, "geely", filePath);
		iso14229.requestDiagService(UpdateStep.TransferData, "geely", null);
		iso14229.requestDiagService(UpdateStep.TransferExit, "geely", null);
		iso14229.requestDiagService(UpdateStep.CheckSum, "geely", null);
		iso14229.requestDiagService(UpdateStep.CheckProgrammDependency, "geely", null);
		return false;
	}

	@Override
	public boolean downloadDriverFile(String filePath) {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestDownload, "geely", filePath);
		iso14229.requestDiagService(UpdateStep.TransferData, "geely", null);
		iso14229.requestDiagService(UpdateStep.TransferExit, "geely", null);
		iso14229.requestDiagService(UpdateStep.CheckSum, "geely", null);
		iso14229.requestDiagService(UpdateStep.EraseMemory, "geely", null);
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
		iso14229.requestDiagService(UpdateStep.ResetECU, "geely", null);
		return false;
	}

	@Override
	public boolean readInfoFromECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.ReadECUHardwareNumber, "geely", null);
		iso14229.requestDiagService(UpdateStep.ReadBootloaderID, "geely", null);
		return false;
	}

	@Override
	public boolean writeInfoToECU() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.WriteTesterSerialNumber, "geely", null);
		iso14229.requestDiagService(UpdateStep.WriteConfigureDate, "geely", null);
		return false;
	}

	@Override
	public boolean ExitProgrammSessionControl() {
		// TODO Auto-generated method stub
		iso14229.requestDiagService(UpdateStep.RequestToExtendSession, "geely", null);
		iso14229.requestDiagService(UpdateStep.EnableNonDiagComm, "geely", null);
		iso14229.requestDiagService(UpdateStep.EnableDTCStorage, "geely", null);
		iso14229.requestDiagService(UpdateStep.RequestToDefaultSession, "geely", null);
		return false;
	}

}
