
// Author : Sharmo, Sarita, Ashish, Yogi

package custom.mr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PartitionHelper 
{

 /**
  * 
  * @param input : Summary file
  * @param output : the output path where r0, r1 files are created. The content implies the filename
  *                 which should be processed by the respective reducers
  * @param numReducers : Number of reducers used by the hash function
  */
	public static void createPartitions(String input, String output, int numReducers) 
	{
		String filePath = input;
		BufferedReader reader;
		try 
		{
			reader = new BufferedReader(new FileReader(filePath));
			String ipLine;
			while (null != (ipLine = reader.readLine())) {
			       int i= getReducer(ipLine,numReducers);
			       BufferedWriter wr = new BufferedWriter(new FileWriter(output+i+"", true));
			       wr.write(ipLine+"\n");
			       wr.close();
			}
		} catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * 
	 * @param ipLine : the filename, which is key in our case
	 * @param numReducers : Number of reducers 
	 * @return : the reducer number to which this key file should be processed. This is 
	 *           determined by getOptimumHash()
	 */
	private static int getReducer(String ipLine,int numReducers) 
	{
		return getOptimumHash((Math.abs(ipLine.hashCode())))%numReducers;
	}
	
	/**
	 * 
	 * @param hash
	 * @return : new hash for the input hash
	 */
	static int getOptimumHash(int hash) {
		hash ^= (hash >>> 20) ^ (hash >>> 12);
		return hash ^ (hash >>> 7) ^ (hash >>> 4);
	}
}
