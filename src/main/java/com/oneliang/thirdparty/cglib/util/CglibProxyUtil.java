package com.oneliang.thirdparty.cglib.util;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;

public final class CglibProxyUtil {

	/**
	 * new proxy instance
	 * @param <T>
	 * @param clazz
	 * @param argumentTypes
	 * @param arguments
	 * @param callback
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T newProxyInstance(Class<T> clazz,Class<?>[] argumentTypes,Object[] arguments, Callback callback) {
		T object = null;
		try {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(clazz);
			enhancer.setCallback(callback);
			object = (T)enhancer.create(argumentTypes, arguments);
		} catch (Exception e) {
			throw new CglibProxyUtilException(e);
		}
		return object;
	}

	/**
	 * CglibProxyUtilException
	 */
	public static final class CglibProxyUtilException extends RuntimeException{
		private static final long serialVersionUID = -6547875268457052983L;
		public CglibProxyUtilException(Throwable cause) {
			super(cause);
		}
	}
}
