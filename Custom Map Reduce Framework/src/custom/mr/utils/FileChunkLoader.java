package custom.mr.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Class responsible for downloading the required files from S3 bucket.
 * This makes sure that not all the input is downloaded,
 * only the chunk which is input for the current EC2 Instance.
 * 
 * @author Yogiraj Awati, Ashish Kalbhor, Sharmodeep Sarkar, Sarita Joshi
 *
 */
public class FileChunkLoader 
{
	
	public static void main(String[] args) 
	{
	      try 
	      {
	    	  // Bucket List of format: s3://cs6240/climate/
	    	  String bucketPath = args[0];
	    	  String inputFileListPath = ProcessUtils.TO_DOWNLOAD_FILES_LOC + args[1];	// 0.txt, 1.txt...
	    	  
	          BufferedReader reader = new BufferedReader(new FileReader(inputFileListPath));
	          ArrayList<String> activeFileList = new ArrayList<String>();
	          String line;
	 
	          System.out.println("Downloading input files from S3 as given in " + inputFileListPath);
	          
	          while ((line = reader.readLine()) != null) 
	          {
	              activeFileList.add(line.trim());
	          }
	          reader.close();
	 
	          for (String oneFile : activeFileList) 
	          {
	        	  String mkdirCmd = "mkdir " + ProcessUtils.LOCAL_INPUT;
	        	  String wgetCmd = "aws s3 cp " + bucketPath + oneFile + " " + "/tmp/" + ProcessUtils.LOCAL_INPUT_FOLDER+"/";
	        	  Runtime rt = Runtime.getRuntime();
	        	  Process  p = rt.exec(mkdirCmd);
	              p.waitFor();
	        	  p = rt.exec(wgetCmd);
	              p.waitFor();      
	          }
	 
	      } catch (Exception e) 
	      { 
	    	  e.printStackTrace(); 
	      }
	  }
}
