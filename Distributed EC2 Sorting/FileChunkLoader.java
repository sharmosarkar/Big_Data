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
	    	  String bucketPath = "https://s3.amazonaws.com/" + args[0].substring(5) + "/";
	    	  String inputFileListPath = args[1];
	    	  
	          BufferedReader reader = new BufferedReader(new FileReader(inputFileListPath));
	          ArrayList<String> activeFileList = new ArrayList<String>();
	          String line;
	 
	          while ((line = reader.readLine()) != null) 
	          {
	              activeFileList.add(line.trim());
	          }
	          reader.close();
	 
	          for (String oneFile : activeFileList) 
	          {
	        	  String cmdString = "/usr/bin/wget " + bucketPath + oneFile;	        
	        	  Runtime rt = Runtime.getRuntime();
	              Process  p = rt.exec(cmdString);
	              p.waitFor();      
	          }
	 
	      } catch (Exception e) 
	      { 
	    	  e.printStackTrace(); 
	      }
	  }
}
