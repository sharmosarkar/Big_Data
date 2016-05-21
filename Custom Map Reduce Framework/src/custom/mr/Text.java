// Author : Sharmo, Sarita, Ashish, Yogi

package custom.mr;
public class Text 
{
	String key;
	
	public Text(String ipLine) 
	{
		this.key = ipLine;
	}

	public Text() 
	{
	}

	public void set(String key)
	{
		this.key = key;
	}
	
	public String toString()
	{
		return key;
	}
	
}
