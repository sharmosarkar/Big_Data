package custom.mr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import custom.mr.utils.FileIO;
import custom.mr.utils.ProcessUtils;
import custom.mr.utils.FileChunkLoader;
import custom.mr.utils.TextSocket;

/**
 * Driver class which acts as Server on each of the Slave EC2 instances.
 * Listens for instruction from Master EC2 instance to perform tasks like:
 * <ul>
 * <li> Fetch Mapper input from S3.
 * <li> Run map method on the Mapper input.
 * <li> Send the Master an acknowledgement that Mapper output is ready.
 *</ul>
 *
 *PARAMLIST: <S3 Input Bucket>
 *
 *Author : Sharmo, Sarita, Ashish, Yogi
 *
 */
public class SlaveDriver 
{
	public static String MasterIPAddress = "0.0.0.0";
	public static int clusterId = -1; 
	public static String inputBucket = "";
	/**
	 * Main driver method that will be called by the Master in Pseudo mode.
	 * @param args
	 */
	public static void main(Object... args) 
	{
		if(args.length != 2)
		{
			System.out.println("SlaveDriver Usage: <S3 Input Bucket> <Cluster Id>");
			System.exit(0);
		}
		inputBucket = ((String[])args[0])[0];
		if(!Job.isPseudoMode)
		{
			MasterIPAddress = getMasterIPAddress();
			clusterId = getClusterIdFromLocalFile();
			SlaveListener();		
		}else
		{
			Context context = (Context)args[1];
			//call local mapper
			LocalMapper(context);
		}
	}
	
	/**
	 * Main driver method that will be called by the Master in EC2 mode.
	 * @param args
	 */
	public static void main(String[] args) 
	{
		if(args.length != 2)
		{
			System.out.println("SlaveDriver Usage: <S3 Input Bucket> <Cluster Id>");
			System.exit(0);
		}

		System.out.println("First Argument" + args[0]);
		System.out.println("Second Argument" + args[1]);
		inputBucket = args[0];
		MasterIPAddress = getMasterIPAddress();
		clusterId = Integer.parseInt(args[1]);
		SlaveListener();		
	}
	
	/**
	 * SlaveListener() : Starts SlaveDriver in Listening mode
	 *                   Once it receives start mapper message, it loads data from the S3 input
	 *                   bucket, runs mapper on the data and SCP output to Master node 
	 */
	public static void SlaveListener()
	{
		int port = ProcessUtils.INIT_PORT + clusterId + 1;
		boolean shouldKill = false;
		String request = "";
		String inputFilePath = clusterId + ".txt";
		try 
		{
			TextSocket.Server server = new TextSocket.Server(port);
			TextSocket conn;
			String outputFolderName = "output" + clusterId;
			System.out.println("Slave at cluster " + clusterId + " listening at " + port + " for a request.");
			while (null != (conn = server.accept())) 
			{
				request = conn.getln();		// StartMapper:TokenizerMapper
				if(request.split(":")[0].equals(ProcessUtils.START_MAPPER))
				{
					String mapperClassName = request.split(":")[1];//mainClassName + '$' + request.split(":")[1];
					// Call the FileChunkLoader for <clusterId>.txt to input/
					FileChunkLoader.main(new String[]{inputBucket, inputFilePath});
					System.out.println("Downloaded the input.");
					// Run Mapper on it.
					System.out.println("MapperClass for Name: " + mapperClassName);
					Class<?> MapperClass = Class.forName(mapperClassName).getClass();	// WordCount$TokenizerMapper
					System.out.println("Mapper class loaded:: " + MapperClass.getName());
					FileIO<Object, Object, Object, Object> fileIO = new FileIO<>();
					fileIO.fetchMapperInput(mapperClassName, "/tmp/" + ProcessUtils.LOCAL_INPUT_FOLDER, "/tmp/" + outputFolderName);
					System.out.println("Mapper output ready for this instance.. Doing SCP to Master.");
					// Send the output file to Master
					ProcessUtils.sendDataBySCP("/tmp/" + outputFolderName, MasterIPAddress);
					// Tell Master once Mapper output has been written.
					System.out.println("Sending success on port " + (port + 100));
					conn.close();
					TextSocket succConn = new TextSocket(MasterIPAddress, port+100);
					succConn.putln(ProcessUtils.MAPPER_SUCCESS);
					System.out.println("Mapper success has been sent to master at " + MasterIPAddress);
					succConn.close();
					TextSocket.Server killServer = new TextSocket.Server(port + 200);
					TextSocket killConn;
					System.out.println("Now listening for kill command at port " + (port +200));
					while(null != (killConn = killServer.accept()))
					{
						String killRsp = killConn.getln();
						if(killRsp.equals(ProcessUtils.KILL_SLAVE_DRIVER))
						{
							System.out.println("Slave Driver murdered at " + (port+200));
							shouldKill = true;
							break;
						}
					}
					killConn.close();
				}
				else if(request.equals(ProcessUtils.KILL_SLAVE_DRIVER))
				{
					System.out.println("Slave Driver murdered at " + port);
					shouldKill = true;
				}
			    
			    if(shouldKill)
			    {
			    	conn.close();
			    	break;  
			    }
			}
		} catch (IOException | ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
		System.out.println("SlaveListener is now closing connection.");
	}
	
	public static void LocalMapper(Context context)
	{
		int mapperId = Integer.parseInt(inputBucket.substring(inputBucket.length()-1));  // input0 => 0
		String outputFolderName = ProcessUtils.ALL_MAPPER_OUTPUT_LOC + "/output" + mapperId;
		System.out.println("Starting with fileIO by Mapper");
		FileIO<Object, Text, Text, IntWritable> fileIO = new FileIO<Object, Text, Text, IntWritable>();
		System.out.println("MapperClass for Name: " + LocalJobClient.mapperClassName);
		fileIO.fetchMapperInput(LocalJobClient.mapperClassName, inputBucket + "/", outputFolderName);
	}
	
	/**
	 * Returns the current ClusterId based on the input file present in "filename" folder.
	 * Reads the TEXT file present compulsorily in the "filename" folder.
	 * <ul>
	 * EXAMPLE: /filename/1.txt present locally on instance will set clusterID = 1.
	 * @return
	 */
	private static int getClusterIdFromLocalFile()
	{
		String cId = "";
		File[] files = new File(ProcessUtils.TO_DOWNLOAD_FILES_LOC).listFiles();
		for (File file : files) 
		{
		    if (file.isFile()) 
		    {
		        cId = file.getName().substring(0, 1);
		    }
		}		
		return Integer.parseInt(cId);
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
