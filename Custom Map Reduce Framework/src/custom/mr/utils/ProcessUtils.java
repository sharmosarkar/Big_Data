// Author : Sharmo, Sarita, Ashish, Yogi

package custom.mr.utils;

import java.io.File;
import java.io.IOException;

/**
 * Utility class that will hold the constants and  common methods that can be used as an utility.
 * 
 * @author Ashish Kalbhor, Yogiraj Awati
 *
 */
public class ProcessUtils 
{
	public static final int INIT_PORT = 10000;
	public static final int LISTEN_PORT = 9000;
	public static final String START_MAPPER = "StartMapper";
	public static final String MAPPER_SUCCESS = "MapperSuccess";
	public static final String KILL_SLAVE_DRIVER = "KillSlaveDriver";
	public static final String REDUCER_SUCCESS = "ReducerSuccess";
	public static final String LOCAL_INPUT = "input";
	public static final String LOCAL_INPUT_FOLDER = "input";
	public static final String IP_LIST_FILENAME = "ipList.txt";
	public static final String PEM_FILE_LOCATION = "/tmp/MyKeyPair1.pem";
	public static final String DNS_LIST_FILE_NAME = "/tmp/hostEntry.txt";
	public static final String MASTER_IP_FILENAME = "/tmp/master.txt";
	public static final String TO_DOWNLOAD_FILES_LOC = "/tmp/filename/";
	public static final String ALL_MAPPER_OUTPUT_LOC = "/tmp/allMapperOutput/";
	
	/**
	 * Utility method that will create a local folder of given name.
	 * <ul>
	 * NOTE: This command is for running on linux machines.
	 * 
	 * @param folderName
	 */
	public static void makeFolder(String folderName)
	{
		try 
		{
			String mkdirCmd = "mkdir " + folderName;
			Runtime rt = Runtime.getRuntime();
			Process  p = rt.exec(mkdirCmd);
			p.waitFor();
			// Uncomment below line, for Windows
			//new File(folderName).mkdir();
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void provideFullAccess(String folderName)
	{
		try 
		{
			String chmodCmd = "chmod 777 " + folderName;
			Runtime rt = Runtime.getRuntime();
			Process  p = rt.exec(chmodCmd);
			p.waitFor();
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Utility method that will the data from source to destination.
	 * <ul>
	 * NOTE: This command is for running on linux machines.
	 * 
	 * @param folderName
	 */
	public static void moveFolder(String src, String dest)
	{
		try 
		{
			String mvCmd = "mv " + src + " " + dest;
			Runtime rt = Runtime.getRuntime();
			Process  p = rt.exec(mvCmd);
			p.waitFor();
			System.out.println("Moved folder " + src + " to " + dest);
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Utility method that will send the given source file to destination using SCP.
	 * <ul>
	 * NOTE: Current implementation uses local PEM File.
	 * 
	 * @param src
	 * @param dest
	 */
	public static void sendDataBySCP(String src, String dest)
	{
		try 
		{
			String scpCmd = "scp -i " + PEM_FILE_LOCATION + " -o StrictHostKeyChecking=no -r " + src + " ec2-user@" + dest + ":/tmp/.";
			System.out.println(scpCmd);
			Runtime rt = Runtime.getRuntime();
			Process  p = rt.exec(scpCmd);
			p.waitFor();
			//Thread.sleep(5000);
		} catch (IOException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
	
}
