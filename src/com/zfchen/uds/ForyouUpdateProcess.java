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
		boolean result = false;
		result = iso14229.requestDiagService(UpdateStep.RequestToExtendSession, manufacturer, null);
		if(result == true)
			result = iso14229.requestDiagService(UpdateStep.ReadECUSparePartNumber, "zotye", null);	//22 F1 87
		return result;
	}

	
	@Override
	public boolean PreProgrammSessionControl() {
		// TODO Auto-generated method stub
		boolean result = false;
		result = iso14229.requestDiagService(UpdateStep.CheckProgrammCondition, manufacturer, null);
		if(result == true){
			result = iso14229.requestDiagService(UpdateStep.DisableDTCStorage, manufacturer, null);
			if(result == true){
				result = iso14229.requestDiagService(UpdateStep.DisableNonDiagComm, manufacturer, null);
				if(result == true){
					result = iso14229.requestDiagService(UpdateStep.RequestToProgrammingSession, manufacturer, null);
				}
			}
		}

		return result;
	}

	
	@Override
	public boolean writeInfoToECU() {
		// TODO Auto-generated method stub
		boolean result = false;
		result = iso14229.requestDiagService(UpdateStep.WriteECUSparePartNumber, manufacturer, null);	//2E F1 87
		if(result == true){
			result = iso14229.requestDiagService(UpdateStep.WriteTesterSerialNumber, manufacturer, null);
			if(result == true){
				result = iso14229.requestDiagService(UpdateStep.WriteUpdateDate, manufacturer, null);	//写入升级日期(需要加入日期，格式为：16 08 29)
			}
		}
		
		return result;
	}

	@Override
	public boolean securityAccess() {
		// TODO Auto-generated method stub
		boolean result = false;
		result = iso14229.requestDiagService(UpdateStep.RequestSeed, manufacturer, null);
		if(result == true)
			result = iso14229.requestDiagService(UpdateStep.SendKey, manufacturer, null);
		
		return result;
	}

	
	@Override
	public boolean downloadDriverFile(String filePath) {
		// TODO Auto-generated method stub
		boolean result = false;
		result = iso14229.requestDiagService(UpdateStep.RequestDownload, manufacturer, filePath);
		if(result == true){
			result = iso14229.requestDiagService(UpdateStep.TransferData, manufacturer, null);
			if(result == true){
				result = iso14229.requestDiagService(UpdateStep.TransferExit, manufacturer, null);
				if(result == true){
					result = iso14229.requestDiagService(UpdateStep.CheckSum, manufacturer, null);
				}
			}
		}
		
		return false;
	}

	
	@Override
	public boolean downloadCalibrationFile(String filePath) {
		// TODO Auto-generated method stub
		if( iso14229.requestDiagService(UpdateStep.EraseMemory, manufacturer, filePath) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.RequestDownload, manufacturer, null) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.TransferData, manufacturer, null) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.TransferExit, manufacturer, null) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.CheckSum, manufacturer, null) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.CheckProgrammDependency, manufacturer, null) == false )
			return false;
		
		return true;
	}
	
	@Override
	public boolean downloadApplicationFile(String filePath) {
		// TODO Auto-generated method stub
		if( iso14229.requestDiagService(UpdateStep.EraseMemory, manufacturer, filePath) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.RequestDownload, manufacturer, filePath) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.TransferData, manufacturer, filePath) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.TransferExit, manufacturer, filePath) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.CheckSum, manufacturer, filePath) == false )
			return false;
		
		return true;
	}

	@Override
	public boolean resetECU() {
		// TODO Auto-generated method stub
		if( iso14229.requestDiagService(UpdateStep.ResetECU, manufacturer, null) == false )
			return false;
		
		if( iso14229.requestDiagService(UpdateStep.RequestToDefaultSession, manufacturer, null) == false )
			return false;
		
		return true;
	}

	@Override
	public boolean ExitProgrammSessionControl() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean update() {
		// TODO Auto-generated method stub
		if (this.readInfoFromECU() == false)
			return false;
		
		if(this.PreProgrammSessionControl() == false)
			return false;
		
		if(this.securityAccess() == false)
			return false;
		
		// 0:driver, 1:application, 2:calibration data
		if(this.downloadDriverFile(this.filename[0]) == false)
			return false;
		
		if(this.filename[1] != null)
			if(this.downloadApplicationFile(this.filename[1]) == false)
				return false;
		
		if(this.filename[2] != null)
			if(this.downloadCalibrationFile(this.filename[2]) == false)
				return false;
		
		if(this.writeInfoToECU() == false)
			return false;
		
		this.resetECU();
		
		return true;
	}
	
	
}
