// Author : Sharmo, Sarita, Ashish, Yogi

package custom.mr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import custom.mr.utils.FileIO;
import custom.mr.utils.ProcessUtils;
import custom.mr.utils.RunShellScript;
import custom.mr.utils.TextSocket;

public class Job 
{
	
	public Class<?> Mapper;
	public Class<?> Reducer;
	public Class<?> Combiner;
	public Class<?> OutputKeyClass;
	public Class<?> OutputValueClass;
	public String inputPath;
	public String outputPath;
	public int numReducerTasks;
	public int numMapperTasks;
	
	public static boolean isPseudoMode = false;
	
	public Job() 
	{
		numReducerTasks = 1;
	}
	
	public static Job getInstance(Configuration conf, String name)
	{
		Job jobInstance = new Job();
		System.out.println("Giving instance..");
		return jobInstance;
	}
	
	public void setJarByClass(Class<?> cls)
	{
	
	}
	
	public void setMapperClass(Class<?> cls)
	{
		System.out.println("Setting Mapper class..");
		this.Mapper = cls;
	}
	
	public String getMapperClassName()
	{
		return this.Mapper.getName();
	}
	
	public void setCombinerClass(Class<?> cls)
	{
		System.out.println("Setting Combiner class..");
		this.Combiner = cls;
	}
	
	public void setReducerClass(Class<?> cls)
	{
		System.out.println("Setting Reducer class..");
		this.Reducer = cls;
	}
	
	public String getReducerClassName()
	{
		return this.Reducer.getName();
	}
	
	public void setOutputKeyClass(Class<?> cls)
	{
		System.out.println("Setting OutputKey class..");
		this.OutputKeyClass = cls;
	}
	
	public void setOutputValueClass(Class<?> cls)
	{
		System.out.println("Setting OutputValue class..");
		this.OutputValueClass = cls;
	}
	
	public void setNumReducerTasks(int n)
	{
		this.numReducerTasks = n;
	}
	

	public Class<?> getOutputKeyClass()
	{
		return this.OutputKeyClass;
	}
	
	public Class<?> getOutputValueClass()
	{
		return this.OutputValueClass;
	}
	
	public String getInputPath()
	{
		return inputPath;
	}
	
	public String getOutputPath()
	{
		return outputPath;
	}

	//waitForCompletion() : This method call mappers, processes intermediate mapper output,
	// processes reducer input, send data to reducers, instantiates reducers, writes final output
	// to S3 bucket if in EC2 Mode
	public boolean waitForCompletion(boolean b) 
	{
		if(checkIsPseudoMode(this.inputPath))
		{
			isPseudoMode = true;
			LocalJobClient ljc = new LocalJobClient(this.numMapperTasks, this.numReducerTasks, inputPath, outputPath, getMapperClassName(), getReducerClassName());
			return ljc.waitForCompletion();
		}
		
		try 
		{
			RunShellScript shellScriptObj = new RunShellScript();
			System.out.println(inputPath);
			//splitting input data across Mappers
			System.out.println("Running data splitter..");
			int numMapperTasks = Integer.parseInt(shellScriptObj.runScript("splitFileList.sh " + inputPath));
			System.out.println("Number of Map Jobs ->" + numMapperTasks);
			
			// SCP the fileList to all the slaves
			//0.txt means mapper with cluster id 0 should download all the listed files in 0.txt from s3
			shellScriptObj.runScript("dataShipper.sh");
			System.out.println("x.txt successfully shipped");
			// Data has been shipped to the Slaves.
			System.out.println("Slave Starting" +shellScriptObj.runScript("startSlaves.sh " + inputPath));
			System.out.println("All slaves successfully started");
			// AT THIS POINT WITH SLAVE: x.txt, jar, master 
			// creating folder allMapperOutput on Master node, where each mapper will SCP its data 
			// to Master node
			ProcessUtils.makeFolder(ProcessUtils.ALL_MAPPER_OUTPUT_LOC);
			System.out.println("Starting the Mapper on slave instances");
			// Call the Slave Driver. Send them the MapperClass
			BufferedReader ec2IpReader = new BufferedReader(new FileReader(ProcessUtils.DNS_LIST_FILE_NAME)); 
			// Skip the Master IP Address (first Line)
			String ips[] = ec2IpReader.readLine().split(" ");
			int i = 0;
			ArrayList<ConnectionThread> connThread = new ArrayList<ConnectionThread>();
			//Start Mapper by sending start mapper request to SlaveDrivers
			for(i=1; i <= numMapperTasks; i++)
			{
				Thread.sleep(5000);
				String requestMsg = ProcessUtils.START_MAPPER + ":" + getMapperClassName() + ":" + getReducerClassName();	
				connThread.add(new ConnectionThread(ips[i], ProcessUtils.INIT_PORT + i, requestMsg));
				connThread.get(i-1).start();
			}
			
			ec2IpReader.close();
			i--;
			// Now, proceed ahead only when all the Mapper outputs are done.
			while(i > 0)
			{
				connThread.get(i-1).join();
				i--;
			}
			
			Thread.sleep(10000);
			System.out.println("Mappers have finished their task.");
			
			// Broadcast "kill-all" to idle SlaveDrivers
			for(i = numMapperTasks + 1; i < ips.length; i++)
			{
				String requestMsg = ProcessUtils.KILL_SLAVE_DRIVER;	
				int toPort = ProcessUtils.INIT_PORT + i;
				System.out.println("Creating socket at " + ips[i] + ":" + toPort);				
				TextSocket conn = new TextSocket(ips[i], toPort);
				conn.putln(requestMsg);
				System.out.println("Killing Idle SlaveDriver at " + ips[i] + ":" + toPort);
				conn.close();				
			}
			
			// Now all the mapper outputs are in /tmp/outputx/
			// Process all the key files and define partitions.
			// And send these partitions to reducers and wait for ACK from Reducers.
			// Move all the individual Mapper outputs in a combined folder
			for(int k = 0; k<ips.length-1;k++) 
			{
				ProcessUtils.moveFolder("/tmp/output" + k, ProcessUtils.ALL_MAPPER_OUTPUT_LOC);
			}
			
			// Move the scripts to Combined Mapper Output folder
			ProcessUtils.moveFolder("/tmp/createReducerInput", ProcessUtils.ALL_MAPPER_OUTPUT_LOC);
			ProcessUtils.moveFolder("/tmp/mergeMapperOutput", ProcessUtils.ALL_MAPPER_OUTPUT_LOC);
			
			//Summary : This files contains all the keys used to create partitions
			System.out.println("Generating summary of Mapper output");
			shellScriptObj.runScript("allMapperOutput/mergeMapperOutput");
			
			//CreatePartitions : Creates partitions based on Summary file and number of reducers
			System.out.println("Generating reducer text files..");
			PartitionHelper.createPartitions(ProcessUtils.ALL_MAPPER_OUTPUT_LOC + "/summary", "/tmp/allMapperOutput/r", numReducerTasks);
			
			//Before instantiating Redducers, Master goes in listening mode to receive ACKs
			System.out.println("Reducers are working.. Waiting for their ACK");
			
			try
			{
				ListenerThread[] listenerThreads = new ListenerThread[numReducerTasks];
				
				for(i=0;i<numReducerTasks;i++)
				{
					listenerThreads[i] = new ListenerThread(ProcessUtils.LISTEN_PORT+i);
					listenerThreads[i].start();
				}
				
				writeReducerInfo(getReducerClassName());
				//Following script merges all values against single key file.
				//Creates input for reducers, SCP that input to reducers and invokes ReducerDriver
				System.out.println("Merging similar key values");
				shellScriptObj.runScript("allMapperOutput/createReducerInput");
				
				//At this point all reducers finished there task, and a success message from all
				// reducers being received
				for(i=0;i<numReducerTasks;i++)
				{
					listenerThreads[i].join();
				}
				
				// Push the output to S3 bucket
				shellScriptObj.runScript("exportTos3.sh " + outputPath);
				System.out.println("Export to " + outputPath);
				
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
				return false;
			}
		} catch (IOException | InterruptedException e) 
		{
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean waitForReducers()
	{
		boolean hasReduced = false;;
		int i=0;
		return hasReduced;
	}
	
	//writeReducerInfo() : Writes reducerClass to reducerInfo.txt.
	//This file is used by reducer to get reducer class
	private static void writeReducerInfo(String reducerClassName)
	{
		try 
		{
			BufferedWriter redInfoWriter = new BufferedWriter(new FileWriter("/tmp/reducerInfo.txt"));
			redInfoWriter.write(reducerClassName + "\n");
			redInfoWriter.close();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	//checkIsPseudoMode(): returns false if input bucket doesnt contains "S3://"
	//                     else true
	public static boolean checkIsPseudoMode(String inputbucket)
	{
		if(inputbucket.startsWith("s3://"))
			return false;	
		return true;
	}
	/**
	 * getMapperOutputCounter (): counts the keys generated by mapper output, used for WordMedian Program
	 * @return
	 */
	
	public int getMapperOutputCounter()
	{
		int count = 0;
		
		try
		{
			BufferedReader summaryReader = new BufferedReader(new FileReader(ProcessUtils.ALL_MAPPER_OUTPUT_LOC + "/summary"));
			String inputLine = "";
			while(null != (inputLine = summaryReader.readLine()))
			{
				count++;
			}
			summaryReader.close();
		}catch(Exception e){
			System.out.println("Error reading summary");
		}
		System.out.println("Job says Mapper Output count is " + count);
		return count;
	}

}

/**
 * A simple server socket thread class that will run one instance.
 * 
 * It can either listen for a list of records or just a string, based on 
 * <b>isList</b> boolean value. 
 * @author Yogiraj Awati, Ashish Kalbhor
 *
 */
class ConnectionThread extends Thread
{
	int port;
	String ec2Address;
	String request;
	String response;
	
	public ConnectionThread(String ec2Address, int port, String requestMsg) 
	{
		this.ec2Address = ec2Address;
		this.port = port;
		this.request = requestMsg;
	}
	
	@Override
	public void run() 
	{
		try 
		{
			System.out.println("Sending Start Mapper request to " + ec2Address + " at Port " + port);
			TextSocket conn = new TextSocket(ec2Address, port);
			conn.putln(request);
			// Go in listening mode
			System.out.println("Listening mapper success at " + (port+100));
			TextSocket.Server successServer = new TextSocket.Server(port+100);
			TextSocket succConn;
			
			while(null != (succConn = successServer.accept()))
			{
				String rsp = succConn.getln();
				if(rsp.contains(ProcessUtils.MAPPER_SUCCESS))
				{
					System.out.println("Received success from the slave.. Now sending kill at " + (port + 200));
					Thread.sleep(10000);
					TextSocket killMapper = new TextSocket(ec2Address, port + 200);
					killMapper.putln(ProcessUtils.KILL_SLAVE_DRIVER);
					Thread.sleep(5000);
					killMapper.close();
					conn.close();
					break;
				}
			}
			
		} catch (IOException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
}

class ListenerThread extends Thread
{
	int port;
	
	public ListenerThread(int port) 
	{
		this.port = port;
	}
	
	@Override
	public void run() 
	{
		try 
		{
			System.out.println("Master is Listening at Port : "+port);
			TextSocket.Server  server = new TextSocket.Server(port);
			System.out.println("Created server at " + port);
			TextSocket c;
			while (null != (c = server.accept())) 
			{
				String rsp = c.getln();
				System.out.println("Received " + rsp);
				break;
			}
			
			c.close();
			
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}

