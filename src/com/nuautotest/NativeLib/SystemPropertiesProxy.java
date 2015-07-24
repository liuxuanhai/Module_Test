package com.nuautotest.NativeLib;

import java.lang.reflect.Method;
import android.content.Context;


public class SystemPropertiesProxy {

	/**
	 * This class cannot be instantiated
	 */
	private SystemPropertiesProxy() {

	}

	/**
	 * Get the value for the given key.
	 *
	 * @return an empty string if the key isn't found
	 * @throws IllegalArgumentException if the key exceeds 32 characters
	 */
	public static String get(Context context, String key) throws IllegalArgumentException {
		String ret = "";
		try {
			ClassLoader cl = context.getClassLoader();
			if (cl == null) return ret;
			@SuppressWarnings("rawtypes")
			Class SystemProperties = cl.loadClass("android.os.SystemProperties");

			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes = new Class[1];
			paramTypes[0] = String.class;

			@SuppressWarnings("rawtypes")
			Method get = SystemProperties.getMethod("get", paramTypes);

			//Parameters
			Object[] params = new Object[1];
			params[0] = key;

			ret = (String)get.invoke(SystemProperties, params);

		} catch (IllegalArgumentException iAE) {
			throw iAE;
		} catch (Exception e) {
			ret = "";
		}
		return ret;
	}
}