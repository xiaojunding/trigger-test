package com.ameren.trigger;

public enum DeviceOutageStatus {
	// Filter
    PPO_NOT_VERIFIED,
	NOT_IN_ACTIVE_OC,
	// Initial
	PPO_INITIATED,
	// Successful
	PPO_ALREADY_SENT,
	PPO_SENT,
	// Failed
	PPO_SEND_FAILED,
	DEVICE_RESTORED_FAILED,
	// Final
	DEVICE_MOVED,
	DEVICE_RESTORED;
    
    public boolean isSuccessful() {
        return PPO_SENT.equals(this) || PPO_ALREADY_SENT.equals(this);
    }
    
    public boolean needToRetryForPPO() {
        return PPO_INITIATED.equals(this) || PPO_SEND_FAILED.equals(this);
    }
    
    public static boolean isFiltered(String deviceStatus) {
        return deviceStatus.equals(PPO_NOT_VERIFIED.toString()) || deviceStatus.equals(NOT_IN_ACTIVE_OC.toString());
    }
}