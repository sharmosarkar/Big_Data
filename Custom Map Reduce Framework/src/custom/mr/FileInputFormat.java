
// Author : Sharmo, Sarita, Ashish, Yogi
package custom.mr;
public class FileInputFormat {

	public static void addInputPath(Job job, Path path) 
	{
		job.inputPath = path.getPath();
	}

}
