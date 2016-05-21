package custom.mr.utils;

import java.net.*;
import java.io.*;
import java.util.Iterator;

// Author: Nat Tuck
public class TextSocket implements Iterable<String>
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
        try 
        {
            rdr = new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8"));
            wtr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), "UTF-8"));
        }
        catch (UnsupportedEncodingException ee) 
        {
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
        if (line == null) 
        {
            return null;
        }
        else 
        {
            return line.trim();
        }
    }

    public void putln(String line) throws IOException 
    {
        line += "\r\n";
        wtr.write(line, 0, line.length());
        wtr.flush();
    }

    public static class Server 
    {
        final ServerSocket server;

        public Server(int port) throws IOException 
        {
            server = new ServerSocket(port);
	    server.setReuseAddress(true);
        }

        public TextSocket accept() throws IOException 
        {
            Socket sock = server.accept();
            if (sock == null) 
            {
                return null;
            }
            else 
            {
                return new TextSocket(sock);
            }
        }
    }

	@Override
	public Iterator<String> iterator() 
	{
        return new BufferedReaderIterator(rdr); 
    }
}

class BufferedReaderIterator implements Iterator<String> 
{
    private BufferedReader br;
    private java.lang.String line;
	public BufferedReaderIterator(BufferedReader aBR) 
	{
	    (br = aBR).getClass();
	    advance();
	}
	
	public boolean hasNext() 
	{
	    return line != null;
	}
	
	public String next() 
	{
	    String retval = line;
	    advance();
	    return retval;
	}
	
	public void remove() 
	{
	    throw new UnsupportedOperationException("Remove not supported on BufferedReader iteration.");
	}
	
	private void advance() 
	{
	    try {
	        line = br.readLine();
	    }
	    catch (IOException e) { /* TODO */}
	}
}