package com.zfchen.ecusoftwareupdatetool;

public class Crc {
	final int order;
	final long polynom;
	
	final long crcinit;
	final long crcxor;

	long crcmask;
	long crchighbit;
	long crcinit_direct;
	long crcinit_nondirect;
	long crctab[];
	
	final boolean direct;
	final boolean refin;
	final boolean refout;
	byte[] checkSum = new byte[4];
	
	public Crc(long poly, int n ,boolean in_reverse, boolean out_reverse) {
		super();
		// TODO Auto-generated constructor stub
		direct = true;
		crcinit = 0xffffffffL;
		crcxor = 0xffffffffL;
		
		polynom = poly;	//0x4c11db7
		order = n;
		refin = in_reverse;
		refout = out_reverse;
		
	}


	private long reflect (long crc, int bitnum) {
		// reflects the lower 'bitnum' bits of 'crc'

		long i, j=1, crcout=0;

		for (i=(long)1<<(bitnum-1); i!=0; i>>=1) {
			if ((crc & i)!= 0) crcout|=j;
			j<<= 1;
		}
		return (crcout);
	}
	
	
	public void crc_table(){
		int i, j;
		long crc;
		boolean bit;
		crctab = new long[256];
		
		for (i=0; i<256; i++) {

			crc=(long)i;
			if (refin)
				crc=reflect(crc, 8);
			crc<<= order-8;

			for (j=0; j<8; j++) {

				if((crc & crchighbit) != 0)
					bit = true;
				else
					bit = false;
				crc<<= 1;
				if (bit)
					crc ^= polynom;
			}			

			if (refin)
				crc = reflect(crc, order);
			crc&= crcmask;
			crctab[i]= crc;
		}
	}
	
	
	protected long crcbitbybit(byte p[], long len) {

		// bit by bit algorithm with augmented zero bytes.
		// does not use lookup table, suited for polynom orders between 1...32.

		long c;
		boolean bit;
		int i, j;
		long crc = crcinit_nondirect;

		for (i=0; i<len; i++) {

			c = (long) p[i];
			if (refin)
				c = reflect(c, 8);

			for (j=0x80; j>0; j>>=1) {

				if((crc & crchighbit) != 0)
					bit = true;
				else
					bit = false;
				crc <<= 1;
				if ((c & j) != 0)
					crc|= 1;
				if (bit)
					crc^= polynom;
			}
		}	

		for (i=0; i<order; i++) {

			if((crc & crchighbit) != 0)
				bit = true;
			else
				bit = false;
			crc<<= 1;
			if (bit)
				crc^= polynom;
		}

		if (refout)
			crc=reflect(crc, order);
		crc^= crcxor;
		crc&= crcmask;

		return(crc);
	}

	
	protected long crctablefast (byte p[], long len) {

		// fast lookup table algorithm without augmented zero bytes, e.g. used in pkzip.
		// only usable with polynom orders of 8, 16, 24 or 32.

		long crc = crcinit_direct;
		
		if (refin)
			crc = reflect(crc, order);
		int i = 0;
		int temp;
		if (!refin){
			while ((len--) > 0)
				try {
					temp = (int)(p[i++] & 0xffL);	//由于p是byte数组，对于第八位（最高位）为1的数，直接取值进行运算时，会被当作负数处理，导致在crctab数组中查表时出现数组越界的情况
					crc = (crc << 8) ^ crctab[ ((int)((crc >> (order-8)) & 0xffL) ^ temp) ];
				} catch (ArrayIndexOutOfBoundsException e) {
					// TODO Auto-generated catch block
					//System.out.printf("byte[%d]=%d\n", i-1, p[i-1]);
					e.printStackTrace();
				}
		} else {
			while ((len--) > 0)
				try {
					temp = (int)(p[i++] & 0xffL);
					crc = (crc >> 8) ^ crctab[ (int) ((crc & 0xff) ^ temp)];
				} catch (ArrayIndexOutOfBoundsException e) {
					// TODO: handle exception
					e.printStackTrace();
				}
		}		

		if (refout^refin)
			crc = reflect(crc, order);
		crc^= crcxor;
		crc&= crcmask;

		return(crc);
	}


	long crctable (byte p[], long len) {

		// normal lookup table algorithm with augmented zero bytes.
		// only usable with polynom orders of 8, 16, 24 or 32.

		long crc = crcinit_nondirect;

		if (refin) crc = reflect(crc, order);
		int i = 0;
		int temp;
		if (!refin)
			while ((len--) > 0){
				temp = (int)(p[i++] & 0xffL);
				crc = ((crc << 8) | temp) ^ crctab[ (int) ((crc >> (order-8)) & 0xffL)];
			}
				
		else
			while ((len--) > 0){
				temp = (int)(p[i++] & 0xffL);
				crc = ((crc >> 8) | (temp << (order-8))) ^ crctab[ (int) (crc & 0xffL)];
			}

		if (!refin)
			while (++len < order/8)
				crc = (crc << 8) ^ crctab[ (int) ((crc >> (order-8)) & 0xffL)];
		else
			while (++len < order/8)
				crc = (crc >> 8) ^ crctab[(int) (crc & 0xffL)];

		if (refout^refin)
			crc = reflect(crc, order);
		crc ^= crcxor;
		crc &= crcmask;

		return(crc);
	}
	
	
	public void CRC16(){
		return;
	}
	
	
	public byte[] CRC32(byte p[], long len){
		long crc = 0;
		int i = 0;
		boolean bit;
		
		this.paraInit();
	
		this.crc_table();
		
		// compute missing initial CRC value
		if (!direct) {
	
			crcinit_nondirect = crcinit;
			crc = crcinit;
			for (i=0; i<order; i++) {
	
				if((crc & crchighbit) != 0)
					bit = true;
				else
					bit = false;
				
				crc<<= 1;
				if (bit)
					crc ^= polynom;
			}
			crc &= crcmask;
			crcinit_direct = crc;
		}
	
		else {
	
			crcinit_direct = crcinit;
			crc = crcinit;
			for (i=0; i<order; i++) {
	
				if((crc & 1) != 0)
					bit = true;
				else
					bit = false;
				
				if (bit)
					crc ^= polynom;
				crc >>= 1;
				if (bit)
					crc |= crchighbit;
			}	
			crcinit_nondirect = crc;
		}
		
		long temp = this.crctablefast(p, len);
		for(int j=3; j>=0; j--){
			checkSum[3-j] = (byte)(temp>>(j*8));
		}
		
		return checkSum;
	}

	
	/**
	 * init the parameter of crc.
	 */
	protected void paraInit() {
		crcmask = ((((long)1<<(order-1))-1)<<1)|1;
		crchighbit = (long)1<<(order-1);		
		
		if ((order < 1) || (order > 32)) {
			System.out.println("ERROR, invalid order, it must be between 1..32.");
			return;
		}

		if (polynom != (polynom & crcmask)) {
			System.out.println("ERROR, invalid polynom.\n");
			return;
		}

		if (crcinit != (crcinit & crcmask)) {
			System.out.println("ERROR, invalid crcinit.\n");
			return;
		}

		if (crcxor != (crcxor & crcmask)) {
			System.out.println("ERROR, invalid crcxor.\n");
			return;
		}
	}
	
}
