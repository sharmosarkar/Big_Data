package custom.mr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import custom.mr.utils.ProcessUtils;
import custom.mr.utils.RunShellScript;
import custom.mr.utils.TextSocket;

/**
 * LocalJobClient is used in Pseudo Mode
 * Author : Sharmo, Sarita, Ashish, Yogi
 *
 */
public class LocalJobClient 
{
	public static String mapperClassName;
	public static String reducerClassName;
	public static String inputPath;
	public static String outputPath;
	public static int numReducerTasks;
	public static int numMapperTasks;
	
	public LocalJobClient(int numMapperTasks, int numReducerTasks, String inputPath, String outputPath, String mapperClassName, String reducerClassName)
	{
		this.mapperClassName = mapperClassName;
		this.reducerClassName = reducerClassName;
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.numReducerTasks = numReducerTasks;
		this.numMapperTasks = numMapperTasks;
	}

	
	public boolean waitForCompletion() 
	{
		try 
		{
			RunShellScript shellScriptObj = new RunShellScript();
			System.out.println("Starting the Mapper on pseudo mode");
			System.out.println(inputPath);
			System.out.println("Running data splitter..");
			numMapperTasks = Integer.parseInt(shellScriptObj.runScript("splitFileList_pseudo.sh " + inputPath));
			System.out.println("Mapppers to be spawned: " + numMapperTasks);
			Context context = new Context(ProcessUtils.ALL_MAPPER_OUTPUT_LOC, true);
			LocalMapperThread[] mapperThreads = new LocalMapperThread[numMapperTasks];
			//spawn mappers
			// Input should be of the format input0, input1..
			for(int i = 0; i < numMapperTasks; i++)
			{
				mapperThreads[i] = new LocalMapperThread(inputPath + "//input" + i, context);
				mapperThreads[i].start();
			}
			
			for(int i = 0; i < numMapperTasks; i++)
			{
				mapperThreads[i].join();
			}
			
			System.out.println("Mappers have finished their task.");
			ProcessUtils.moveFolder("/tmp/createPseudoReducerInput", ProcessUtils.ALL_MAPPER_OUTPUT_LOC);
			ProcessUtils.moveFolder("/tmp/mergeMapperOutput", ProcessUtils.ALL_MAPPER_OUTPUT_LOC);
			
			System.out.println("Generating summary of Mapper output");
			shellScriptObj.runScript("allMapperOutput/mergeMapperOutput");
			
			System.out.println("Generating reducer text files..");
			PartitionHelper.createPartitions(ProcessUtils.ALL_MAPPER_OUTPUT_LOC + "/summary", "/tmp/allMapperOutput/r", numReducerTasks);

			shellScriptObj.runScript("allMapperOutput/createPseudoReducerInput");
			
			//Reducer input is ready, now spawn reducer threads
			LocalReducerThread[] reducerThreads = new LocalReducerThread[numReducerTasks];
			for(int i = 0; i < numReducerTasks; i++)
			{
				reducerThreads[i] = new LocalReducerThread(i);
				reducerThreads[i].start();
			}
			
			for(int i = 0; i < numReducerTasks; i++)
			{
				reducerThreads[i].join();
			}
			
			System.out.println("All reducers have written their output.");
			
		} catch (InterruptedException | IOException e) 
		{
			e.printStackTrace();
		}
		
		return true;
	}
}

/**
 * 
 * LocalMapperThread: Spawns Mapper threads and calls map function of the provided jar
 *
 */
class LocalMapperThread extends Thread
{
	String mapperInputPath;
	Context context;
	
	public LocalMapperThread(String mapperInputCompletePath, Context context) 
	{
		this.mapperInputPath = mapperInputCompletePath;
		this.context = context;
	}
	
	@Override
	public void run() 
	{
		System.out.println("Starting slave driver thread on " + mapperInputPath);
		SlaveDriver.main(new String[]{mapperInputPath}, context);
	}
}
/**
 * LocalReducerThread : Spawns Reducer threads and call reducer of the provided jar
 *
 *
 */
class LocalReducerThread extends Thread
{
	int reducerId;
	Context context;
	
	public LocalReducerThread(int reducerId)
	{
		this.reducerId = reducerId;
	}
	
	@Override
	public void run() 
	{
		System.out.println("Starting reducer driver thread on " + reducerId);
		ReducerDriver.main(new String[]{reducerId + ""});
	}
}
