// Author : Sharmo, Sarita, Ashish, Yogi

package custom.mr.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.GZIPInputStream;

import custom.mr.Context;
import custom.mr.IntWritable;
import custom.mr.Job;
import custom.mr.Mapper;
import custom.mr.Reducer;
import custom.mr.Text;

public class FileIO<KEYIN, VALUEIN, KEYOUT, VALUEOUT>
{
	public Job jobObj = new Job();
	
	public void fetchMapperInput(String mainClassForRun, String ipLocalFolder, String outputFolderName)
	{
		Context context = new Context(outputFolderName, true);	// Output folder name
		File folder = new File(ipLocalFolder);
		for (final File fileEntry : folder.listFiles()) 
		{
			if(fileEntry.getName().equals("index.html"))
				continue;
			
	        try 
	        {
	        	String filePath = ipLocalFolder + "/" + fileEntry.getName();
	        	BufferedReader reader;
	        	if(filePath.contains(".gz") || filePath.contains(".tar"))
	        	{
	        		System.out.println("Reading a GZIP file");
	        		GZIPInputStream gzip = null;
	        		gzip = new GZIPInputStream(new FileInputStream(filePath));
	        		reader = new BufferedReader(new InputStreamReader(gzip));
	        	}else
	        	{
	        		reader = new BufferedReader(new FileReader(filePath));
	        	}
				System.out.println("Fetching from: " + filePath + " and running mapper on it.");
				String ipLine;
				
				while(null != (ipLine = reader.readLine()))
				{
					Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT> obj = (Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>)Class.forName(mainClassForRun).newInstance();
					obj.map(fileEntry.getName(), new Text(ipLine), context);					
				}
				
				System.out.println("Mapper finished on " + filePath);
				
				reader.close();
			} catch (Exception e) 
	        {
				e.printStackTrace();
			}  
	    }
	}
	
	
	
	public void reduceOnEachKey(String reducerClass, String keyFileName, String key, int clusterId)
	{
		try 
	    {
			ArrayList<IntWritable> intValList = new ArrayList<>();
			ArrayList<Text> textValList = new ArrayList<>();
			boolean isTextValue = false;
			Context context = new Context("/tmp/part-" + clusterId, false);	// Output folder name
			BufferedReader reader = new BufferedReader(new FileReader(keyFileName));
			String ipLine = reader.readLine().replaceAll("\\s+", " ");
			reader.close();
			String[] stringVals = ipLine.split(" ");
			if(stringVals[0].contains(",") || stringVals[0].contains("-"))
				isTextValue = true;
			
			for(String val : stringVals)
			{
				if (!isTextValue) 
				{
					IntWritable finalVal = new IntWritable(Integer.parseInt(val));
					intValList.add(finalVal);
				}else
				{
					textValList.add(new Text(val));
				}
			}
				
			Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT> obj = (Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT>)Class.forName(reducerClass).newInstance();
			if(!isTextValue)
			{
				obj.reduce( new Text(key), (Iterable<VALUEIN>)intValList, context);		      
			}else
			{
				obj.reduce( new Text(key), (Iterable<VALUEIN>)textValList, context);
			}
		} catch (Exception e) 
        {
			
		}  
	}
	
}
