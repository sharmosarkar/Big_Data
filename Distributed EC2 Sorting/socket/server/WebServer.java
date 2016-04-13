package main;

import java.io.*;
import textsock.TextSocket;
import java.net.*;

/**
 * Web Server that runs only on the Master EC2 Instance.
 * It makes sure that once a finish acknowledgement is received from EC2,
 * the client on Local Machine should get to know that the processes are done,
 * and client to good to go ahead and read the output files.
 * 
 * Original Author : Nat Tuck
 * // Modified by :: Sharmo , Sarita
 */
public class WebServer 
{
	/**
	 * Driver method that starts the server socket.
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException 
    {
        TextSocket.Server serverForLocal = new TextSocket.Server(10001);
      
        TextSocket conn;
        while (null != (conn = serverForLocal.accept())) 
        {
            ServerThread tt = new ServerThread(conn);
            tt.start();
        }

    }
}

/**
 * Server Thread to do the server socket programming.
 * 
 * @author Sharmodeep Sarkar
 *
 */
class ServerThread extends Thread 
{
    final TextSocket conn;

    public ServerThread(TextSocket conn) 
    {
        this.conn = conn;
    }

    @Override 
    public void run() 
    {
        try 
        {
            handleConn();
        }
        catch (IOException ee) 
        {
            System.err.println("IO Error: " + ee.toString());
        }
    }

    void handleConn() throws IOException 
    {

        String req = conn.getln();
        while (!conn.getln().equals("")) 
        {
            // Skip empty lines
        }
        
        String[] parts = req.split(" ");
        if (!parts[0].equals("GET")) {
            throw new IOException("Expected GET");
        }
        if (!parts[2].equals("HTTP/1.1")) {
            throw new IOException("Expected HTTP/1.1");
        }

        String path = parts[1];
        String client_ip = parts[3];
        String msgToClient = "";

        try 
        {
        	Thread.sleep(10);            
        	// Now start the server for EC2 Master Instance
        	TextSocket.Server serverForEc2 = new TextSocket.Server(10002);
            TextSocket conn2;
            String finalEc2Message = null;
            int count = 1;
            while (null != (conn2 = serverForEc2.accept())) 
            {
            	count++;
                finalEc2Message = conn2.getln();
                msgToClient = "Acknowledgement from Ec2 Master: " + finalEc2Message;
                conn2.close();
                
                if(count == 2)	// Breaks after one iteration
                	break;  
            }
            
        } catch(InterruptedException ex)
        {
        	Thread.currentThread().interrupt();
        }

        conn.putln(msgToClient);
        conn.close();
   }
}
