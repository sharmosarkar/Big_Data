
// Author : Sharmo, Sarita, Ashish, Yogi
package custom.mr; 
public class FileOutputFormat 
{

	public static void setOutputPath(Job job, Path path) 
	{
		System.out.println("Path: " + path.getPath());
		job.outputPath = path.getPath();
	}

}
