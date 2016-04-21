package com.oneliang.thirdparty.asm.aop;

public abstract interface Jointer {

	/**
	 * get static method before,include the class full name
	 * @return String
	 */
	public abstract String getStaticMethodBefore();

	/**
	 * get static method after,include the class full name
	 * @return String
	 */
	public abstract String getStaticMethodAfter();

	/**
	 * need to joint
	 * @param access
	 * @param name
	 * @param desc
	 * @param signature
	 * @param exceptions
	 * @return boolean
	 */
	public abstract boolean needToJoint(int access, String name, String desc, String signature, String[] exceptions);
}