package com.oneliang.thirdparty.test.annotation;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.oneliang.Constants;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.BaseLogger;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;
//import com.sun.tools.javac.Main;

public class TestJavacProcessor {

	static{
//		LoggerManager.registerLogger(Constants.Symbol.WILDCARD, new FileLogger(Logger.Level.VERBOSE, new File("/D:/a.txt")));
		LoggerManager.registerLogger(Constants.Symbol.WILDCARD, new BaseLogger(Logger.Level.VERBOSE));
	}
	private static final Logger logger=LoggerManager.getLogger(TestJavacProcessor.class);

	private static boolean isWindowsOS=false;
	static{
		String osName = System.getProperty("os.name");
		if (StringUtil.isNotBlank(osName)&&osName.toLowerCase().indexOf("windows") > -1) {
			isWindowsOS = true;
		}
	}

	public static void main(String[] args){
		List<String> sourceList=new ArrayList<String>();
		sourceList.add("/D:/Dandelion/java/githubWorkspace/third-party-util/src/main/java/com/oneliang/test/java/annotation/TestJavaClass.java");
		sourceList.add("/D:/Dandelion/java/githubWorkspace/third-party-util/src/main/java/com/oneliang/test/java/annotation/Log.java");
		sourceList.add("/D:/Dandelion/java/githubWorkspace/third-party-util/src/main/java/com/oneliang/test/java/annotation/TestAnnotation.java");
		List<String> classpathList=null;
		String destinationDirectory="/D:/a";
		FileUtil.createDirectory(destinationDirectory);
		javac(classpathList, sourceList, destinationDirectory, true);
	}

	/**
	 * tool.jar javac
	 * @param classpathList
	 * @param sourceList
	 * @param destinationDirectory
	 * @param isDebug
	 * @return int,exit code
	 */
	public static int javac(List<String> classpathList,List<String> sourceList,String destinationDirectory,boolean isDebug){
		List<String> parameterList=new ArrayList<String>();
//		parameterList.add("javac");
		if(classpathList!=null&&!classpathList.isEmpty()){
			String seperator=isWindowsOS?Constants.Symbol.SEMICOLON:Constants.Symbol.COLON;
			parameterList.add("-classpath");
			parameterList.add(listToCommandString(classpathList, null, seperator));
		}
		if(isDebug){
			parameterList.add("-g");
		}
//		parameterList.add("-nowarn");
//		parameterList.add("-verbose");
		parameterList.add("-d");
		parameterList.add(destinationDirectory);
		parameterList.add("-encoding");
		parameterList.add(Constants.Encoding.UTF8);
		
		parameterList.add("-processor");
		parameterList.add(AnnotationProcessor.class.getName());
		
		if(sourceList!=null&&!sourceList.isEmpty()){
			for(String source:sourceList){
				parameterList.add(source);
			}
		}else{
			throw new RuntimeException("source list can not be null or empty");
		}
		return executeCommand(parameterList.toArray(new String[]{}));
//		return Main.compile(parameterList.toArray(new String[]{}));
	}

	/**
	 * list to command string
	 * @param stringList
	 * @param appendString
	 * @param seperator
	 * @return String
	 */
	public static String listToCommandString(List<String> stringList,String appendString,String seperator){
		StringBuilder stringBuilder=new StringBuilder();
		int index=0;
		for(String string:stringList){
			stringBuilder.append(string+StringUtil.nullToBlank(appendString));
			if(index<stringList.size()-1){
				stringBuilder.append(seperator);
			}
			index++;
		}
		return stringBuilder.toString();
	}

	/**
	 * execute command
	 * @param command
	 * @return int,exit code
	 */
	public static int executeCommand(String[] commandArray) {
		return executeCommand(commandArray, true);
	}

	/**
	 * execute command
	 * @param command
	 * @param needToLogCommand
	 * @return int,exit code
	 */
	public static int executeCommand(String[] commandArray, boolean needToLogCommand) {
		return executeCommand(commandArray, null, needToLogCommand, true, true);
	}

	/**
	 * execute command
	 * @param commandArray
	 * @param needToLogCommand
	 * @param needToLogNormal
	 * @param needToLogError
	 * @return int,exit code
	 */
	public static int executeCommand(String[] commandArray, boolean needToLogCommand, boolean needToLogNormal, boolean needToLogError) {
		return executeCommand(commandArray, null, needToLogCommand, needToLogNormal, needToLogError);
	}

	/**
	 * execute command
	 * @param commandArray
	 * @param environmentParameter
	 * @return int,exit code
	 */
	public static int executeCommand(String[] commandArray, String[] environmentParameter) {
		return executeCommand(commandArray, environmentParameter, true, true, true);
	}

	/**
	 * execute command
	 * @param commandArray
	 * @param environmentParameter
	 * @param needToLogCommand
	 * @return int,exit code
	 */
	public static int executeCommand(String[] commandArray, String[] environmentParameter, boolean needToLogCommand) {
		return executeCommand(commandArray, environmentParameter, needToLogCommand, true, true);
	}

	/**
	 * execute command
	 * @param command
	 * @param environmentParameter
	 * @param needToLogCommand
	 * @param needToLogNormal
	 * @param needToLogError
	 * @return int,exit code
	 */
	public static int executeCommand(String[] commandArray, String[] environmentParameter, boolean needToLogCommand, boolean needToLogNormal, boolean needToLogError) {
		int result=Integer.MIN_VALUE;
		if (commandArray != null) {
			try {
				Process process = null;
				if (commandArray.length == 1) {
					String command = commandArray[0];
					if(needToLogCommand){
						logger.verbose(command);
					}
					process = Runtime.getRuntime().exec(command,environmentParameter);
				} else {
					StringBuilder commandStringBuilder=new StringBuilder();
					for(String command:commandArray){
						commandStringBuilder.append(command.trim()+StringUtil.SPACE);
					}
					if(needToLogCommand){
						logger.verbose(commandStringBuilder);
					}
					process = Runtime.getRuntime().exec(commandArray,environmentParameter);
				}
				Thread errorInputThread = new Thread(new ProcessRunnable(ProcessRunnable.TAG_ERROR, process.getErrorStream(), needToLogError));
				errorInputThread.start();
				Thread inputThread = new Thread(new ProcessRunnable(ProcessRunnable.TAG_NORMAL, process.getInputStream(), needToLogNormal));
				inputThread.start();
				result=process.waitFor();
				errorInputThread.interrupt();
				inputThread.interrupt();
				process.destroy();
			} catch (Exception e) {
				logger.error(Constants.Base.EXCEPTION, e);
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	/**
	 * is window os
	 * @return boolean
	 */
	public static boolean isWindowsOS() {
		return isWindowsOS;
	}

	private static class ProcessRunnable implements Runnable{
		private static final String TAG_ERROR="error";
		private static final String TAG_NORMAL="normal";
		private InputStream inputStream=null;
		private String tag=null;
		private boolean needToLog=true;
		public ProcessRunnable(String tag,InputStream inputStream,boolean needToLog) {
			this.tag=tag;
			this.inputStream=inputStream;
			this.needToLog=needToLog;
		}
		public void run(){
			BufferedInputStream bufferedInputStream=null;
			try{
				bufferedInputStream=new BufferedInputStream(this.inputStream);
				BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(bufferedInputStream));
				String string=null;
				while((string=bufferedReader.readLine())!=null){
					if(this.needToLog){
						logger.verbose("["+this.tag+"]"+string);
					}
				}
			}catch(Exception e) {
				if(isWindowsOS()){//it has stream exception in linux
					logger.error(Constants.Base.EXCEPTION,e);
				}
			}finally{
				if(this.inputStream!=null){
					try {
						this.inputStream.close();
					} catch (Exception e) {
						if(isWindowsOS()){
							logger.error(Constants.Base.EXCEPTION,e);
						}
					}
				}
			}
		}
	}
}
