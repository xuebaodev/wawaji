package com.cosofteck.decrypt;

public class Decrypt {
	public native byte[] check(byte[] randomKey, byte[] customerKey);
	static {
		System.loadLibrary("decryptapi");
	}
}
