package com.zfchen.ecusoftwareupdatetool;

public interface SecurityAccess {
	final byte xor_DFSK[] = {0x41, 0x16, 0x71, 0x24}; /* DFSK */
	final byte xor_GEELY[] = {0x65, 0x67, 0x77, (byte)0xE9}; /* Geely */
	final byte xor_ZOTYE[] = {0x41, 0x16, 0x71, 0x24}; /* ZOTYE */

	byte[] generateKey(byte[] seed, byte level);
}
