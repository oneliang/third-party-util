package com.oneliang.thirdparty.asm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDescription {

	public String className = null;
	public String superClassName = null;
	public int access = 0;
	public String sourceFile = null;
	private boolean publicClass = false;
	public List<String> dependClassNameList = new ArrayList<String>();
	public Map<String, String> dependClassNameMap = new HashMap<String, String>();
	public Map<String, String> referenceFieldNameMap = new HashMap<String, String>();
	public Map<String, String> referenceMethodNameMap = new HashMap<String, String>();
	public List<String> fieldNameList = new ArrayList<String>();
	public List<String> fieldFullNameList = new ArrayList<String>();
	public List<String> privateFieldNameList = new ArrayList<String>();
	public List<String> friendlyFieldNameList = new ArrayList<String>();
	public List<String> protectedFieldNameList = new ArrayList<String>();
	public List<String> methodNameList = new ArrayList<String>();
	public List<String> methodFullNameList = new ArrayList<String>();
	public List<String> privateMethodNameList = new ArrayList<String>();
	public List<String> friendlyMethodNameList = new ArrayList<String>();
	public List<String> protectedMethodNameList = new ArrayList<String>();

	void setPublicClass(boolean publicClass) {
		this.publicClass = publicClass;
	}

	public boolean isPublicClass() {
		return this.publicClass;
	}

	public boolean isNoPrivateField() {
		return this.privateFieldNameList.isEmpty();
	}

	public boolean isNoFriendlyField() {
		return this.friendlyFieldNameList.isEmpty();
	}

	public boolean isNoProtectedField() {
		return this.protectedFieldNameList.isEmpty();
	}

	public boolean isNoPrivateMethod() {
		return this.privateMethodNameList.isEmpty();
	}

	public boolean isNoFriendlyMethod() {
		return this.friendlyMethodNameList.isEmpty();
	}

	public boolean isNoProtectedMethod() {
		return this.protectedMethodNameList.isEmpty();
	}
}
