
import java.net.*;
import java.io.*;
import java.util.LinkedList;

// Author: Nat Tuck
public class TextSocket  
{
    final Socket sock;
    final BufferedReader rdr;
    final BufferedWriter wtr;

    public TextSocket(String host, int port) throws IOException 
    {
        this(new Socket(host, port));
    }

    public TextSocket(Socket ss) throws IOException 
    { 
        sock = ss;
	sock.setReuseAddress(true);
        try {
            rdr = new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8"));
            wtr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
        }
        catch (UnsupportedEncodingException ee) {
            throw new IOException("Can't happen.");
        }
    }

    public void close() throws IOException 
    {
        sock.close();
    }

    public String getln() throws IOException 
    {
        String line = rdr.readLine();
        if (line == null) {
            return null;
        }
        else {
            return line.trim();
        }
    }

    public void putln(String line) throws IOException 
    {
        line += "\r\n";
        wtr.write(line, 0, line.length());
        wtr.flush();
    }
    
    /**
     * Put a list of serialized objects.
     * @author Ashish Kalbhor
     */
    public void putList(LinkedList<Record> list) throws IOException
    {
    	ObjectOutputStream objectOutput = new ObjectOutputStream(sock.getOutputStream());
    	objectOutput.writeObject(list);
    }
    
    /**
     * Get a list of serialized objects
     * @author Ashish Kalbhor
     */
    public LinkedList<Record> getList() throws IOException, ClassNotFoundException
    {
    	ObjectInputStream objectInput = new ObjectInputStream(sock.getInputStream());
        Object object = objectInput.readObject();
        return (LinkedList<Record>) object;
    }

    public static class Server {
        final ServerSocket server;

        public Server(int port) throws IOException {
            server = new ServerSocket(port);
	    server.setReuseAddress(true);
        }

        public TextSocket accept() throws IOException {
            Socket sock = server.accept();
            if (sock == null) {
                return null;
            }
            else {
                return new TextSocket(sock);
            }
        }
    }
}