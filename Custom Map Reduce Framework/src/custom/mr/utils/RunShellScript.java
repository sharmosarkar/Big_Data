package custom.mr.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/* This class runs a shell script which is 
 *  specified in it's runScript method and 
 *  return the output of the script (if any) on the console
 */

// Author : Sharmo, Sarita, Ashish, Yogi

public class RunShellScript 
{
	
	// scriptRunDef => "scriptName "+arguments
	
	public String runScript (String scriptRunDef) throws InterruptedException, IOException
	{
		
		String scriptFilePaths = "/tmp"; //System.getProperty("user.dir");
		
		String target = new String(scriptFilePaths+"/"+scriptRunDef);
		Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(target);
        proc.waitFor();
        StringBuffer output = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line = "";                       
        while ((line = reader.readLine())!= null) {
                output.append(line);
        }
        
        return (output.toString());
	}
	

	

}
