package custom.mr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import custom.mr.utils.ProcessUtils;

//Author : Sharmo, Sarita, Ashish, Yogi,
public class Context 
{
	private String outputFolder = null;
	boolean isMapperContext;
	
	public Context(String outputFolder, boolean isMapperContext) 
	{
		this.outputFolder = outputFolder;
		this.isMapperContext = isMapperContext;
		ProcessUtils.makeFolder(outputFolder);
	}
	
	public String getOutputPath()
	{
		return outputFolder;
	}

	public void write(Text key, IntWritable value)
	{
		if (isMapperContext) 
		{
			// Remove all the special characters form the Key
			// This is required to allow fileName according to the Key
			String keyString = key.toString().replaceAll("[:*?/<>|]", ""); 
			String keyFilePath = outputFolder + "/" + keyString + ".txt";
			try 
			{
				BufferedWriter keyValueWriter = new BufferedWriter(
						new FileWriter(keyFilePath, true));
				keyValueWriter.write(value.get() + " ");
				keyValueWriter.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}else
		{
			try 
			{
				BufferedWriter keyValueWriter = new BufferedWriter(new FileWriter(outputFolder + "/output.txt", true));
				keyValueWriter.write(key.toString() + "\t" + value.get() + "\n");
				keyValueWriter.close();
			} catch (IOException e1) 
			{
				e1.printStackTrace();
			}
		}
		
	}
	
	public void write(Text key, Text value)
	{
		if (isMapperContext) 
		{
			// Remove all the special characters form the Key
			// This is required to allow fileName according to the Key
			String keyString = key.toString().replaceAll("[:*?/<>|]", ""); //replaceAll("[^A-Za-z0-9 ]", "");
			String keyFilePath = outputFolder + "/" + keyString + ".txt";
			try 
			{
				BufferedWriter keyValueWriter = new BufferedWriter(
						new FileWriter(keyFilePath, true));
				keyValueWriter.write(value + " ");
				keyValueWriter.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}else
		{
			try 
			{
				BufferedWriter keyValueWriter = new BufferedWriter(new FileWriter(outputFolder + "/output.txt", true));
				keyValueWriter.write(key.toString() + "\t" + value + "\n");
				keyValueWriter.close();
			} catch (IOException e1) 
			{
				e1.printStackTrace();
			}
		}
		
	}
	
}

