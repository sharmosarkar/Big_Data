// Author : Sharmo, Sarita, Ashish, Yogi

package custom.mr;
import java.io.Serializable;



public class IntWritable implements Serializable
{
	int val;
	
	public IntWritable()
	{
		this.val = 0;
	}
	
	public IntWritable(int val) 
	{
		this.val = val;
	}
	
	public int get()
	{
		return val;
	}
	
	public void set(int val)
	{
		this.val = val;
	}

}
