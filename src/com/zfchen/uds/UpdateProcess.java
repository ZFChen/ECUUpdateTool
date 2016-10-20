package com.zfchen.uds;

public interface UpdateProcess {
	public boolean readInfoFromECU();
	
	public boolean PreProgrammSessionControl();
	
	public boolean writeInfoToECU();
	
	public boolean securityAccess();
	
	public boolean downloadDriverFile(String filePath);
	
	public boolean downloadCalibrationFile(String filePath);
	
	public boolean downloadApplicationFile(String filePath);
	
	public boolean resetECU();
	
	public boolean ExitProgrammSessionControl();
	
	public boolean update();
}
