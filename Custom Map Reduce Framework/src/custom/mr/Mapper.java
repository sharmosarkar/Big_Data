// Author : Sharmo, Sarita, Ashish, Yogi

package custom.mr;
import java.io.IOException;


public abstract class Mapper <KEYIN, VALUEIN, KEYOUT, VALUEOUT>
{
	public abstract void map(Object key, Text value, Context context) throws IOException, InterruptedException; 
}
