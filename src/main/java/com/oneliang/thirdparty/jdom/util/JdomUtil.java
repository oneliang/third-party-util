package com.oneliang.thirdparty.jdom.util;

import java.lang.reflect.Method;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import com.oneliang.Constant;
import com.oneliang.util.common.ObjectUtil;

public final class JdomUtil{
	
	private static final SAXBuilder saxBuilder = new SAXBuilder();

	/**
	 * <p>Method:parse the file then return the Document</p>
	 * @param fileName
	 * @return jdom Document
	 */
	public static Document parser(final String fileName) {
		return parser(fileName, false);
	}

	/**
	 * <p>Method:parse the file then return the Document</p>
	 * @param fileName
	 * @param validate
	 * @return jdom Document
	 */
	public static Document parser(final String fileName,final boolean validate) {
		if(!validate){
			saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);   
			saxBuilder.setFeature("http://xml.org/sax/features/validation",false);
		}
		Document document = null;
		try {
			document = saxBuilder.build(fileName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return document;
	}
	/**
	 * <p>Method: use for xml initial to object</p>
	 * @param object
	 * @param attributeList
	 * @throws Exception
	 */
	public static void initialFromAttributeList(final Object object,final List<Attribute> attributeList) throws Exception{
		for(Attribute configAttribute:attributeList){
			String attributeName=configAttribute.getName();
			String attributeValue=configAttribute.getValue();
			String methodName=ObjectUtil.fieldNameToMethodName(Constant.Method.PREFIX_SET,attributeName);
			Method method=null;
			try{
				method=object.getClass().getMethod(methodName,String.class);
			}catch(Exception e){
				continue;
			}
			if(method!=null){
				method.invoke(object, new Object[]{attributeValue});
			}
		}
	}
}
