// Author : Sharmo, Sarita, Ashish, Yogi

package custom.mr;

import java.io.IOException;

public abstract class Reducer <KEYIN, VALUEIN, KEYOUT, VALUEOUT>
{
	public abstract void reduce(Text key, Iterable<VALUEIN> values, Context context) throws IOException, InterruptedException ;
}
