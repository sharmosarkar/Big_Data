

package custom.mr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import custom.mr.utils.FileIO;
import custom.mr.utils.ProcessUtils;
import custom.mr.utils.TextSocket;

/**
 * Driver class for Reducer phase of the framework.
 * This class will be run be the Master once the input file for Reducer has been
 * send to the EC2 instance.
 * Once this class is called, the current instance acts as a Reducer. 
 * After successfully writing the Reducer output, it will send an acknowledgement
 * to the Master that the "part-files" are ready to be pulled.
 * <ul>
 * USAGE: java -jar xxx.jar ReducerDriver <clusterId>
 * 
 * @author Ashish Kalbhor, Yogiraj Awati, Sarita Joshi, Sharmodep Sarkar
 *
 */
public class ReducerDriver 
{
	public static int reducerId = 0; 
	public static String reducerClassName = "";
	
	public int pseudoReducerId;
	/**
	 * Driver Method.
	 * 
	 * ARG: clusterId
	 * @param args
	 */
	public static void main(String[] args) 
	{
		String reducerInputFolder;
		int localReducerId;
		
		if(!Job.isPseudoMode)
		{
			reducerId = Integer.parseInt(args[0]);
			reducerInputFolder = "/tmp/r" + reducerId + "folder";
			reducerClassName = getReducerClassName();
			System.out.println("Reducer " + reducerId + " for Name: " + reducerClassName + " on " + reducerInputFolder);
			localReducerId = reducerId;
		}else
		{
			localReducerId = Integer.parseInt(args[0]);
			reducerInputFolder= "/tmp/allMapperOutput/r" + localReducerId + "folder";
			reducerClassName = LocalJobClient.reducerClassName;
			System.out.println("Reducer " + localReducerId + " for Name: " + reducerClassName + " on " + reducerInputFolder);
		}
		
		// File processing on Reducer inputs starts from here.	
		FileIO<Object, Object, Object, Object> fileIO = new FileIO<>();
		//Read the input
		File folder = new File(reducerInputFolder);
		for (final File fileEntry : folder.listFiles()) 
		{	
			// Iterate each key file.
	        try 
	        {
	        	String keyFilePath = reducerInputFolder + "/" + fileEntry.getName();
	        	//call reducer per key
				fileIO.reduceOnEachKey(reducerClassName, keyFilePath, fileEntry.getName(), localReducerId);	        	
	        	
			} catch (Exception e) 
	        {
				e.printStackTrace();
			}  
	    }
		
		//if the mode is pseudo, you don't need to send any acknowledgement.
		if(Job.isPseudoMode)
		{
			ProcessUtils.provideFullAccess("/tmp/part-" + localReducerId);
			ProcessUtils.moveFolder("/tmp/part-" + localReducerId, LocalJobClient.outputPath);
			System.out.println("Reducer " + localReducerId + " success");
		}else{
			// Once done, send the output as well as ACK to Master
			try 
			{
				int toPort = ProcessUtils.LISTEN_PORT+localReducerId;
				String masterIPAddr = getMasterIPAddress();
				TextSocket conn = new TextSocket(masterIPAddr, toPort);
				//SCP output to reducer
				System.out.println("Sending data to Master : "+ toPort);
				ProcessUtils.sendDataBySCP("/tmp/part-" + localReducerId, masterIPAddr);
				conn.putln(localReducerId + ":" + ProcessUtils.REDUCER_SUCCESS);
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Reads the Reducer Class Name from info text file sent from the Master.
	 * @return
	 */
	private static String getReducerClassName()
	{
		String redClassName = "";
		try 
		{
			BufferedReader redInfoReader = new BufferedReader(new FileReader("/tmp/reducerInfo.txt"));
			redClassName = redInfoReader.readLine(); 
			redInfoReader.close();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		return redClassName;
	}
	
	/**
	 * Returns the Master Node IP Address that is written in Master.txt
	 * 
	 * @return masterIpAddress
	 */
	private static String getMasterIPAddress()
	{
		String masterIp = null;
		try 
		{
			BufferedReader masterReader = new BufferedReader(new FileReader(ProcessUtils.MASTER_IP_FILENAME));
			masterIp = masterReader.readLine();
			masterReader.close();
			return masterIp;
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		return masterIp;
	}
	
}
