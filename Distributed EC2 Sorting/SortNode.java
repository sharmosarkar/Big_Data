import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

/**
 * Sample Sorting Node that takes the input set of files and sorts them
 * by using Sample Sorting Algorithm.
 * This implementation involves Socket connections between multiple EC2 instances,
 * wherein each of the EC2 instances have just once jar deployed.
 * We have taken care of dynamic use of ports for creating sockets.
 * While sending a chunk from one instance to another, we have used a Serialized Record Object.
 * 
 * @author Yogiraj Awati, Ashish Kalbhor, Sharmodeep Sarkar, Sarita Joshi
 * 
 * NOTE : cluster id =0 implies this is Master Node else its a slave
 *
 */
public class SortNode 
{
	// CONSTANTS
	final static String SEPERATOR = ",";
	final static String WBAN_COLOUMN = "Wban Number";
	final static String DATE_COLOUMN = "YearMonthDay";
	final static String TIME_COLOUMN = "Time";
	final static int INIT_DATA_PORT = 5000;
	final static int INIT_ACK_PORT = 6000;
	final static int INIT_FINISH_ACK_PORT = 7000;
	
	// STATIC DATA MEMBERS
	static String TEMPERATURE_COLOUMN;
	static LinkedList<Record> recordList = new LinkedList<>();
	static LinkedList<Record> splitterList = new LinkedList<>();
	static LinkedList<Record> receivedSplitterList = new LinkedList<>();
	static LinkedList<Record> finalList = new LinkedList<>();
	static ArrayList<String> fileColoumns = new ArrayList<String>();
	static LinkedList<Integer> insertIndexList = new LinkedList<>();
	static int[] ports = null;
	static int[] ackPorts = null;
	static int[] finishAckPorts = null;
	static String[] ec2IpAddrs = null;
	static int clusterId = 0;
	static int clusters = 0;

	/**
	 * Driver method that starts the processing on given input set of files. 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException 
	{
		Record r;
		GZIPInputStream gzip = null;
		if (args.length != 5)
		{
			System.out.println("Please input <Sorting Field> : <input path> : <Number of clusters> : <Cluster Id> : <DNS File Path> ");
			System.exit(0);
		}

		String sortingField = args[0];
		String inputPath = args[1];
		clusters = Integer.parseInt(args[2]);
		ec2IpAddrs = new String[clusters];
		clusterId = Integer.parseInt(args[3]);
		String dnsFilePath = args[4];

		TEMPERATURE_COLOUMN = sortingField;
		
		int i = 0;
		int j = 0;
		
		// Load all the EC2 Ip Addresses from the file send by client to this EC2 Instance
		loadEc2IpAddrs(dnsFilePath);
		// Load all the Ports (both data ports and acknowledgement ports)
		loadPortArrays(clusters);

		// Read each of the given input file.
		final File folder = new File(inputPath);
		for (final File fileEntry : folder.listFiles())
		{
			String singleFile = fileEntry.getName();
			System.out.println(singleFile);

			try
			{
				gzip = new GZIPInputStream(new FileInputStream(inputPath + "/"+ singleFile));

				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
				String line;
				line = br.readLine();
				String newLine = line.replaceAll("\"", "");
				String correctLine = newLine.replaceAll(", ", ",");
				String[] columns = correctLine.split(SEPERATOR);
				columns[0] = WBAN_COLOUMN;
				fileColoumns = new ArrayList<>(Arrays.asList(columns));
				// Reading each line from the file
				while ((line = br.readLine()) != null)
				{
					j++;
					String[] data = line.split(SEPERATOR);
					if (data.length < 21)
					{
						continue;
					}

					String wban = data[fileColoumns.indexOf(WBAN_COLOUMN)].trim();
					String date = data[fileColoumns.indexOf(DATE_COLOUMN)].trim();
					String time = data[fileColoumns.indexOf(TIME_COLOUMN)].trim();
					String temp = data[fileColoumns.indexOf(TEMPERATURE_COLOUMN)].trim();

					//Take only valid records
					if (isRecordValid(wban, date, time, temp))
					{
						r = new Record(wban, date, time, Double.parseDouble(temp));
						i++;
						recordList.add(r);
					}
				}

				br.close();

			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
			System.out.println("Records in list  till now: " + recordList.size());

		} // Loaded all the files - data stored in recordList

		System.out.println("started sorting...");
        //sort list by "Dry Bulb Temp"
		recordList = sortListByTemperature(recordList);

		// select p-1 (CLUSTERS - 1) splitters from this data set
		splitterList = getSplitters(recordList, clusters);
		
		System.out.println("\n SPLITTERS : ");

		for (Record rec : splitterList)
		{
			System.out.print(" Splitter " + rec.getBulbTemp());
		}

		// send the splitters of this node to master node
		if(clusterId != 0)
		{
			System.out.println("Sending splitters from slave to Master");
			sendSplitters(splitterList, new String[]{ec2IpAddrs[0]});
		
			//Receive new p-1 splitters from Master - HAVE TO WAIT - Master broadcasts to all slaves new splitters
			System.out.println("Waiting to receive new splitters from Master Node");
			splitterList = receiveSplitters(false);
			System.out.println("Recieved new splitters " + splitterList);
		}
		
		if(clusterId == 0)	// MASTER first receives then sends to all
		{
			//  Wait to receive p-1 splitters from each Slave.
			System.out.println("Master will now be receiving splitters");
			splitterList.addAll(receiveSplitters(true));
			System.out.println("splitters received by master.. now sort them");
			//sort the new splitters
			Collections.sort(splitterList, Record.BulbTempComparator);
			System.out.println("Get more splitters on these");
			
			//after sorting get new splitters
			splitterList = getSplitters(splitterList, clusters);
			
			// TODO Here send is for ALL the Slaves - New splitters are broadcasted to all slaves by Master
			String[] tempAddr = new String[ec2IpAddrs.length-1];
			for(int ip = 1; ip < ec2IpAddrs.length; ip++)
				tempAddr[ip-1] = ec2IpAddrs[ip];
			
			// Send the new splitters to all the slaves (exclude the master).
			System.out.println("Now master sends splitters to all the slaves (exclude itself)..");
			sendSplitters(splitterList, tempAddr);
		}
		
		// insert new splitters received by the master node into this recordlist
		System.out.println("Inserting the splitters into record list");
		insertSplitters(recordList, splitterList);
		System.out.println("Distributing the buckets");
		
		//Master : Receives its bucket from all the slaves and then distributes buckets to slave
		//Slave : All slaves start distributing first bucket to Master and after that to respective slaves
		
		if(clusterId == 0) // Master
		{
			// clusterId is excluded from receiving. This means receive from everyone except this
			finalList.addAll(receiveList(clusterId)); 
			distributeBuckets(recordList, insertIndexList);
		}
		else // slave
		{ 
			distributeBuckets(recordList, insertIndexList);
		}
		
		//At this point we have finalList which contains the bucket of this Node
		System.out.println("Sorting the final list..");
		//Sort the Bucket
		Collections.sort(finalList, Record.BulbTempComparator);

		//Write the data to file
		String outputPath = "output" + clusterId + ".txt";
		writeOutputToFile(finalList, outputPath);

		System.out.println("\n Total Records: " + j + " Valid Records: " + i);

		// If Master, wait for FINISH ACK from all the slaves
		if(clusterId ==0)
		{
			System.out.println("Master waiting for all slaves to finish up");
			//receive acks from all slaves
			receiveFinishAck();
			// TODO Send the client an acknowledgement that all is done
			sendAckToClient();
		}else
		{	// If Slave, tell the Master that you are done
			Thread.sleep(5000);
			System.out.println(clusterId + " telling Master that he is done");
			sendFinishAck(clusterId);
		}
		
	}
	
	/**
	 * Loads the Data and Acknowledgement Ports in the port array.
	 * 
	 * @param size
	 */
	public static void loadPortArrays(int size)
	{
		ports = new int[clusters];
		ackPorts = new int[clusters];
		finishAckPorts = new int[clusters];

		for(int i=0;i<clusters;i++)
		{
			ports[i] = INIT_DATA_PORT + i;
			ackPorts[i] = INIT_ACK_PORT + i;
			finishAckPorts[i] = INIT_FINISH_ACK_PORT + i;
		}
	}
	
	/**
	 * Loads all the IP Addresses from the dnsFile into a static array.
	 * @param dnsFile
	 * @throws IOException
	 */
	public static void loadEc2IpAddrs(String dnsFile) throws IOException
	{
		BufferedReader read = new BufferedReader(new FileReader(dnsFile));
		String ipLine = read.readLine();
		ec2IpAddrs = ipLine.split(" ");	
		read.close();
	}

	/**
	 * write the data to file
	 * @author : Ashish
	 */
	private static void writeOutputToFile(LinkedList<Record> recordList2, String ouputPath) 
	{
		BufferedWriter recordWrite = null;

		try 
		{
			recordWrite = new BufferedWriter(new FileWriter(ouputPath));

			for (Record rec : recordList2)
			{
				recordWrite.write(rec.toString());
			}

			recordWrite.close();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 *Inserts new splitters broadcasted by Master Node to current record list using binary search 
	 * @author : Yogiraj Awati
	 */
	private static void insertSplitters(LinkedList<Record> recordList2,	LinkedList<Record> receivedSplitterList2) 
	{
		double t;
		int insertIndex;
		int offset = 0;

		// for each splitter
		for (Record r : receivedSplitterList2)
		{
			t = r.getBulbTemp();
			
			// get the correct index that the splitter needs to be inserted into
			// the list
			insertIndex = getSplitterPosForInsertion(recordList2, t);

			// Keep track of the index of the splitters that are going to be
			// inserted in the sorted list
			// offset is used to manage insertions
			insertIndexList.add(insertIndex + offset);
			offset++;
		}

		int j = 0;
		// for each splitter index
		for (int i : insertIndexList)
		{
			//get the splitter and insert at index i
			recordList2.add(i, receivedSplitterList2.get(j));
			j++;
		}
	}


	/**
	 * getSplitterPosForInsertion : returns the index of the splitter t that is going to
	   be inserted into sorted recordlist using Binary Search
	 * @author : Yogiraj Awati
	 */
	private static int getSplitterPosForInsertion(LinkedList<Record> recordList2, Double t) 
	{
		int lo = 0;
		int hi = recordList2.size() - 1;
		while (lo <= hi) 
		{
			int mid = lo + (hi - lo) / 2;

			if (t < recordList2.get(mid).getBulbTemp())
				hi = mid - 1;
			else if (t > recordList2.get(mid).getBulbTemp())
				lo = mid + 1;
			else
				return mid;
		}
		return lo;
	}

	
	/**
	 * getSplitters : This function returns array of splitters on recordlist
	   with number of splitters = clusters - 1
	 * @author : Yogiraj Awati
	 */
	private static LinkedList<Record> getSplitters(LinkedList<Record> recordList, int clusters) 
	{
		int sizeOfList = recordList.size();

		// Following maths used to pick splitters at equal distance from record list
		int quotient = sizeOfList / (clusters);
		int increment = quotient;
		int index = increment;

		LinkedList<Record> splitterList = new LinkedList<Record>();

		for (int k = 0; k < clusters - 1 && k < sizeOfList; k++)
		{
			splitterList.add(recordList.get(index));
			index = index + increment;
		}

		return splitterList;
	}

	/**
	 * Sorts the given List of Records based on the Dry Bulb Temperature column.
	 * @param recordList
	 * @return : LinkedList<Record>
	 */
	private static LinkedList<Record> sortListByTemperature(LinkedList<Record> recordList) 
	{
		Collections.sort(recordList, Record.BulbTempComparator);
		return recordList;
	}

	/**
	 * 
	// sends splitters of this EC2 Node to ec2IpAddrs
	 * @authors : Ashish Kalbhor
	 */
	private static void sendSplitters(LinkedList<Record> splitterList, String[] ec2IpAddrs) throws IOException 
	{
		for(String eachIp : ec2IpAddrs)
		{
			System.out.println("Sending Splitters to " + eachIp);
			sendData(eachIp,splitterList);
		}
	}
	
	/**
	 * 	creates sockets and send data to the port
	 * @author
	 */
	private static void sendData(String ipAddr, LinkedList<Record> data) throws IOException 
	{
		int port = 0;
		//if slave
		if(clusterId != 0)
			port = 3000;
		else //if master
			port = 3002;
		
		TextSocket conn = new TextSocket(ipAddr, port);
		conn.putList(data);
        conn.close();
	}
	
	/**
	 * Sends record list
	 * @author : Ashish Kalbhor
	 */
	private static void sendList(String ipAddr, LinkedList<Record> data, int port2) throws IOException, InterruptedException
	{
		System.out.println("Waiting before sending..");
		Thread.sleep(10000);
		System.out.println("...Sending Chunk to " + ipAddr + ":" + port2);
		TextSocket conn = new TextSocket(ipAddr, port2);
        conn.putList(data);
        System.out.println("Finished sending list of size " + data.size() + " now closing");
        conn.close();
	}
	
	/**
	 * receives finish processing acks from all slaves 
	 * @author : Yogiraj Awati
	 */
	private static void receiveFinishAck() throws InterruptedException 
	{
		ListenerThread finishAckThreads[] = new ListenerThread[clusters-1];
		int c = 0;
		for(int i=1 ; i < clusters ; i++)
		{
			finishAckThreads[c] = new ListenerThread(finishAckPorts[i], false);
			System.out.println("Spawning finish ack listener at " + finishAckPorts[i]);
			finishAckThreads[c].start();				
			c++;
		}
		
		c--;
		while (c >= 0) 
		{
			finishAckThreads[c].join();
			c--;
		}
		System.out.println("All Slaves are done now");
		
	}
	
	/**
	 * sends ack to client running on local computer
	 * @author : Yogiraj Awati
	 */
	private static void sendAckToClient() throws IOException 
	{
		TextSocket conn = new TextSocket("localhost", 10002);
		conn.putln("DONE");
		conn.close();
	}
	
	/**
	 * sends ack to ec2IpAddrs
	 * @author :Ashish Kalbhor
	 */
	private static void sendAck(int ackPort) throws IOException 
	{
		int c = 0;
		for(String ipAddr : ec2IpAddrs)
		{
			if(clusterId != c)
			{
				TextSocket conn = new TextSocket(ipAddr, ackPort);
				
				conn.putln(clusterId + "");
				System.out.println("Sent ack " + clusterId + " to " + ipAddr + " : " + ackPort);
				conn.close();
		        c++;
			}
			else{
				c++;
			}
			
		}        
	}
	
	/**
	 * receive acks from ackPort
	 * @author : Ashish kalbhor
	 */
	private static int receiveAck(int ackPort) throws IOException, ClassNotFoundException
	{
		TextSocket.Server server = new TextSocket.Server(ackPort);
        TextSocket conn;
        String cId = null;
        int count = 1;
        System.out.println("Entering receiveAck for port" + ackPort + ".. next comes loop");
        while (null != (conn = server.accept())) 
        {
        	count++;
            System.out.println("Received acknowledgement..");
            cId = conn.getln();
            conn.close();
            
            if(count == 2)
            	break;  
        }
        
        return Integer.parseInt(cId);
	}
	
	/**
	 * Receives record list from multiple port : Threads are spawned to listen from multiple ports except negCluster id
	 * @author : Ashish Kalbhor
	 */
	private static LinkedList<Record> receiveList(int negCluster) throws IOException, ClassNotFoundException, InterruptedException
	{
		System.out.println(negCluster + " cluster now listening to others");
		LinkedList<Record> finalRcvdList = new LinkedList<Record>();
		
		ListenerThread lThreadArray[] = new ListenerThread[clusters-1];
		int c = 0;
		for(int i=0 ; i < clusters ; i++)
		{
			if(i != negCluster)
			{
				lThreadArray[c] = new ListenerThread(ports[i], true);
				System.out.println("Spawning listener at " + ports[i]);
				lThreadArray[c].start();				
				c++;
			}
		}
		
		c--;
		while (c >= 0) 
		{
			lThreadArray[c].join();
			c--;
		}
		System.out.println("All threads finished..");
		// All listener threads have been started, now accumulate the list
		for(int threadCount = 0; threadCount < lThreadArray.length ; threadCount++)
		{
			finalRcvdList.addAll(lThreadArray[threadCount].rcvdList);
		}		
		System.out.println("Final received list size..... " + finalRcvdList.size());
		return finalRcvdList;		
	}
	
	/**
	 * Listener method for RecordList chunk from a Slave at given port.
	 * This method is called for each of the Slaves.
	 * It just returns the chunk of sorted records that was sent by the slave.
	 *  
	 * @author Yogiraj Awati
	 */
	public static LinkedList<Record> listenListFromSlave(int port) throws IOException, ClassNotFoundException
	{
		LinkedList<Record> rcvdList = new LinkedList<Record>();
		TextSocket.Server server = new TextSocket.Server(port);
        TextSocket conn;
        int count = 1;

        while (null != (conn = server.accept())) 
        {
        	rcvdList.addAll(conn.getList());   
        	count++;
            System.out.println("Received a chunk at Port:" + port);
            conn.close();
            
            if(count == 2)
            	break;  
        }
		     
        return rcvdList;
	}
	
	/**
	 * Sends acknowledgement over the network to Master Instance.
	 * This method is called when current instance has finished with 
	 * Author : Yogiraj Awati
	 * @param clusterId
	 * @throws IOException
	 */
	private static void sendFinishAck(int clusterId) throws IOException 
	{
		TextSocket conn = new TextSocket(ec2IpAddrs[0], finishAckPorts[clusterId]);
		conn.putln(clusterId + "");
		conn.close();
	}
	
	/**
	 * Listener method for the Last Acknowledgement from the Slave.
	 * This method is called for each of the Slaves.
	 *  
	 * @author Yogiraj Awati
	 */
	public static void listenLastAckFromSlave(int port) throws IOException, ClassNotFoundException
	{
		TextSocket.Server server = new TextSocket.Server(port);
        TextSocket conn;
        int count = 1;

        // Keep listening on given port.
        while (null != (conn = server.accept())) 
        {
        	count++;
            conn.close();
            
            if(count == 2) // Exit once received.
            	break;  
        }
	}
	
	/**
	 * Listener method for splitters from other instances.
	 * <li>
	 * If current instance is a Master, it will keep on listening at 3000,
	 * till all the slaves send their splitter lists.
	 * We accumulate these splitters and then send the whole list.
	 * <li>
	 * If current instance is a Slave, it will keep listening at 3002,
	 * just once till it receives a single list of splitters.
	 * 
	 * @author Ashish Kalbhor 
	 */
	private static LinkedList<Record> receiveSplitters(boolean isMaster) throws IOException, ClassNotFoundException
	{
		int port = 0;
		if(clusterId == 0)	// Master receives at Port 3000 
			port = 3000;
		else				// Slave receives at Port 3002
			port = 3002;
		
		LinkedList<Record> finalRcvdSplitters = new LinkedList<Record>();
		LinkedList<Record> rcvdSplittersFromSingleNode = new LinkedList<Record>();
		TextSocket.Server server = new TextSocket.Server(port);
        TextSocket conn;
        int count = 1;

        while (null != (conn = server.accept())) 
        {
        	rcvdSplittersFromSingleNode= conn.getList();   
            count++;
            System.out.println("Received Splitter " + rcvdSplittersFromSingleNode);
            finalRcvdSplitters.addAll(rcvdSplittersFromSingleNode);
            conn.close();
            
            if(isMaster)
            {
            	// Master listens the splitters for all the slaves.
            	if(count == clusters)
                	break;
            }else
            {
            	// Slave just listens the splitters once.
            	if(count == 2)
                	break;
            }
            
        }
		
        return finalRcvdSplitters;
	}
	
	/**
	 * Sanity check for the given fields of Record object.
	 * Author : Yogiraj Awati
	 * 
	 */
	private static boolean isRecordValid(String wban, String date, String time,	String temp) 
	{
		if(wban.isEmpty() || date.isEmpty() || time.isEmpty() || temp.isEmpty() || temp.equals("-"))
		{
			return false;
		}
		try
		{
			Double.parseDouble(temp);
			return true;
		}catch (Exception e)
		{
			return false;
		}
	}

	/**
	 *distributeBuckets : Distributes buckets to respective EC2 Instances 
	 * @author Yogiraj Awati
	 */
	private static void distributeBuckets(LinkedList<Record> recordList2, LinkedList<Integer> insertIndexList) throws IOException, ClassNotFoundException, InterruptedException 
	{
		int i = 0, j = 0;
		int ipCount = 0;
		int k = 1;

		LinkedList<Record> sendRecordChunk = new LinkedList<>();

		System.out.print("\nChunk = " + k++ + " : ");

		for (i = 0; i < recordList2.size(); i++)
		{
            //populate chunk
			if (i < insertIndexList.get(j))
			{
				sendRecordChunk.add(recordList2.get(i));
			}
			else
			{
				// send recordChunk to EC2 instance .....
				System.out.println("Chunk size is " + sendRecordChunk.size());
				if(ipCount != clusterId) 	// Dont send the chunk to yourself
				{
					sendList(ec2IpAddrs[ipCount], sendRecordChunk, ports[clusterId]);
					// TODO Wait for acknowledgement
					// receive ack from the node to which data was send. This ack is received only after the Node to which 
					// data is send receives buckets from all nodes....util this time, the current node waits
					int ack = receiveAck(ackPorts[ipCount]);
					System.out.println("Ack received by " + clusterId + " is " + ack);
					
					//if the ack was received by Node 1 and this is Node 2, then its time for this node to receive bucket
					// from rest of the nodes. So enter in receiving mode
					if(clusterId != 0 && ack == clusterId-1)
					{
						finalList.addAll(receiveList(clusterId));
					}
					ipCount++;
				}else{
					// TODO Store my this current chunk - dont broadcast to any other
					finalList.addAll(sendRecordChunk);		
					sendAck(ackPorts[clusterId]);
					
					System.out.println("populating final list for " + clusterId + " and ipcount " + ipCount +
							" final list size " + finalList.size());
					ipCount++;
				}
				sendRecordChunk.clear();
				sendRecordChunk = null;
				sendRecordChunk = new LinkedList<>();
				System.out.print("\nChunk = " + k++ + " : ");
				i--;
				j++;
				if (j == insertIndexList.size())
				{
					break;
				}
			}
		}

		i++;

		// send last bucket to node
		while (i < recordList2.size())
		{
			sendRecordChunk.add(recordList2.get(i));			
			i++;
		}

		System.out.println("Size of send record chunk after while loop " + sendRecordChunk.size());
		//if there is any last bucket remaining send to respective IP address
		if(ipCount != clusterId && sendRecordChunk.size() > 0) 	
		{
			System.out.println("Final sending of distributeBucket.. ipCount " + ipCount + " and cid " + clusterId);
			sendList(ec2IpAddrs[ipCount], sendRecordChunk, ports[clusterId]);	
		}
		sendRecordChunk.clear();
		sendRecordChunk = null;
	}
}

/**
 * A serializable Record class is the container for data,
 * that is read from the given input of Climate data.
 * It consists of {Wban, Date, Time, Temperature}
 * 
 * @author Yogiraj Awati, Ashish Kalbhor
 *
 */
class Record implements Serializable
{
	/**
	 * Default serialVersionUID for the serializable Record object.
	 */
	private static final long serialVersionUID = 1L;
	private String wban;
	private String date;
	private String time;
	private Double temp;
	
	public Double getBulbTemp() 
	{
		return temp;
	}

	public Record(String wban, String date, String time, Double temp) 
	{
		super();
		this.wban = wban;
		this.date = date;
		this.time = time;
		this.temp = temp;
	}

	@Override
	public String toString() 
	{
		return "Record [wban=" + wban + ", date=" + date + ", time=" + time
				+ ", temp=" + temp + "]" + "\n";
	}

	/**
	 * Explicit comparator for Temperature field of the <b>Record</b> class.
	 */
	public static final Comparator<Record> BulbTempComparator = new Comparator<Record>() {
		@Override
		public int compare(Record o1, Record o2) 
		{
			if (o1.getBulbTemp() > o2.getBulbTemp())
			{
				return 1;
			}
			else if (o1.getBulbTemp() < o2.getBulbTemp())
			{
				return -1;
			}
			else
				return 0;
		}
	};
}

/**
 * A simple server socket thread class that will run one instance.
 * 
 * It can either listen for a list of records or just a string, based on 
 * <b>isList</b> boolean value. 
 * @author Yogiraj Awati, Ashish Kalbhor
 *
 */
class ListenerThread extends Thread
{
	int port;
	LinkedList<Record> rcvdList;
	boolean isList = true;
	
	public ListenerThread(int port, boolean isList) 
	{
		this.port = port;
		rcvdList = new LinkedList<Record>();
		this.isList = isList;
	}
	
	@Override
	public void run() 
	{
		try 
		{
			if(isList)
			{
				// Listen for a List of Records on given port.
				rcvdList = SortNode.listenListFromSlave(port);
			}else
			{
				// Else, just listen for an acknowledgement.
				SortNode.listenLastAckFromSlave(port);
			}
			
		} catch (ClassNotFoundException | IOException e) 
		{
			e.printStackTrace();
		}
	}
}