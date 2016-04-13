package main;

import java.io.*;
import textsock.TextSocket;
import java.net.*;
// Original Author : Prof. Nat Tuck
// Modified by :: Sharmo , Sarita
public class WebClient {


    public static void main(String[] args) throws Exception {

                String scriptFilePaths = System.getProperty("user.dir");
                // input bucket name 
                String inputBucketName = args[1];
                // sorting column name
                String sortColumn = args[0];

                // retrieve number of servers available and the master server
                // master server is the first server in the instancesIPs.txt
                BufferedReader br = new BufferedReader(new FileReader("out.txt"));
                String ips = br.readLine();
                String[] ipList = ips.split(" ");

                // Data Split 
                int numberOfSplits = ipList.length;
                System.out.println (numberOfSplits);
                String target = new String(scriptFilePaths+"/splitFileList.sh "+numberOfSplits+" "+inputBucketName);
                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec(target);
                proc.waitFor();
                StringBuffer output = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line = "";                       
                while ((line = reader.readLine())!= null) {
                        output.append(line + "\n");
                }
                System.out.println("Data List Split for " + numberOfSplits + " servers ");


                // Send Split Data List  to the Servers using scp
                target = new String(scriptFilePaths+"/dataShipper.sh");
                rt = Runtime.getRuntime();
                proc = rt.exec(target);
                proc.waitFor();
                output = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                line = "";                       
                while ((line = reader.readLine())!= null) {
                        output.append(line + "\n");
                }
                System.out.println("Data List Sent to All the Servers ");

                long startTime = System.currentTimeMillis();
                // Start the algo on each of the nodes
                target = new String(scriptFilePaths+"/startsort.sh "+inputBucketName+" "+sortColumn);
                rt = Runtime.getRuntime();
                proc = rt.exec(target);
                proc.waitFor();
                output = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                line = "";                       
                while ((line = reader.readLine())!= null) {
                        output.append(line + "\n");
                }
                System.out.println("Sorting Starting on the Nodes ");


                // Client Call
                // server port
            	int port = 10001;
                // server ip
            	String ip = ipList[0];
		        System.out.println (ip);
                TextSocket conn = new TextSocket(ip, port);
		        String start_Signal = "Start Sort" ;
                conn.putln("GET / HTTP/1.1 "+start_Signal);
                conn.putln("Host: "+ip);
                conn.putln("");
                
                for (String resp : conn) {
                    System.out.println(resp);
                }

                conn.close();
                long endTime = System.currentTimeMillis();
                BufferedWriter writer = new BufferedWriter(new FileWriter("sortingTime.txt"));
                writer.write("Total time taken:: " + ((endTime - startTime)/60000.0) + " minutes");

    }
}
