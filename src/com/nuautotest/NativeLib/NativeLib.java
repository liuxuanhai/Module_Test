package com.nuautotest.NativeLib;

class NativeLib {
	static {
		System.loadLibrary("ndk");
	}

	public native int power_on_off();

	public native int test();
}
