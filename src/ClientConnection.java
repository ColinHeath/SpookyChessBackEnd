

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.util.Hashtable;

public class ClientConnection extends Thread {
	private int userID = -1;
	private Socket connectedSocket;
	private SpookyChessServer connectedServer;
	private boolean inGame; // flag that tracks whether this client is currently in a game or not
	
	private BufferedReader bufferedInput;
	private PrintWriter outputWriter;
	private boolean isRunning;
	
	public ClientConnection(Socket connectedSocket, SpookyChessServer spookyChessServer)
	{
		this.connectedServer = spookyChessServer;
		this.connectedSocket = connectedSocket;
		inGame = false;
		
		try {
			this.bufferedInput = new BufferedReader(new InputStreamReader(this.connectedSocket.getInputStream()));
			this.outputWriter = new PrintWriter(this.connectedSocket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.inGame = false;
		this.isRunning = true;
		
		this.start();
	}
	
	public boolean loginUser(String userName, String password)
	{
		//TODO: Make outputs realistic for Connor's server messaging scheme.
		
		int[] loginResult = this.connectedServer.verifyAccount(userName, password);
		
		if(loginResult != null)
		{
			this.userID = loginResult[2];
			
			this.outputWriter.write("Successfully logged in!");
			this.outputWriter.write("Wins: " + loginResult[0]);
			this.outputWriter.write("Losses: " + loginResult[1]);
		}
		else
		{
			this.outputWriter.write("Login Failed. Please create an account.");
		}
		
		return (this.userID != -1);
	}
	
	public String readData()
	{
		String totalInput = "";

		try
		{
			String inputLine;
			
			while((inputLine = this.bufferedInput.readLine()) != null)
			{
				totalInput += inputLine + "\n";
				System.out.println(inputLine);
			}
		}
		catch(IOException ioe)
		{
			System.out.println("An error occurred reading from socket: " + this.getName());
		}
		finally
		{
			return totalInput;	
		}
	}

	public void writeData(String toWrite)
	{
		this.outputWriter.print(toWrite);
	}
	
	// mutator method for inGame variable.
	public void setInGame(boolean inGame)
	{
		this.inGame = inGame;
	}
	
	public void close()
	{
		this.isRunning = false;
		
		try
		{
			this.connectedSocket.close();
			this.join();	
		}
		catch(IOException ioe)
		{
			System.out.println("An exception occurred while shutting down ClientConnection: " + ioe.getMessage());
		}
		catch(InterruptedException ie)
		{
			System.out.println("An exception occurred while shutting down ClientConnection: " + ie.getMessage());
		}
	}
	
	public void run()
	{

		//When the reader has text, check if it's a login request. If so, do your thing.
		//Otherwise, ignore it. Let GameConnection take other values out.
		//Not sure how to check for one value while saving others for removal (buffering within the class?)
		while(isRunning)
		{
			if(inGame)
			{
				//Do Game Things. Decided once GameConnection work starts for real.
			}
			else
			{
				String currentRequest = this.readData();
				
				/*
				 * Maybe parse currentRequest into an object using GSON or something of that ilk.
				 * Doesn't really matter, just find a way to get parameters out of it.
				 */
				
				String intent = ""; //TODO: Get a real value here, from the reader.
				if(intent.equals("login"))
				{
					String username = "";
					String password = "";
					
					loginUser(username, password);
				}
				else if(intent.equals("createAccount"))
				{
					String username = "";
					String password = "";
					
					this.userID = this.connectedServer.createUser(username, password);
				}
			}
		}
	}
}


// From https://stackoverflow.com/questions/13255622/parsing-raw-http-request
/**
 * Class for HTTP request parsing as defined by RFC 2612:
 * 
 * Request = Request-Line ; Section 5.1 (( general-header ; Section 4.5 |
 * request-header ; Section 5.3 | entity-header ) CRLF) ; Section 7.1 CRLF [
 * message-body ] ; Section 4.3
 * 
 * @author izelaya
 *
 */
class HttpRequestParser {

    private String _requestLine;
    private Hashtable<String, String> _requestHeaders;
    private StringBuffer _messagetBody;

    public HttpRequestParser() {
        _requestHeaders = new Hashtable<String, String>();
        _messagetBody = new StringBuffer();
    }

    /**
     * Parse and HTTP request.
     * 
     * @param request
     *            String holding http request.
     * @throws IOException
     *             If an I/O error occurs reading the input stream.
     * @throws IllegalArgumentException
     *             If HTTP Request is malformed
     */
    public void parseRequest(String request) throws IOException, IllegalArgumentException {
        BufferedReader reader = new BufferedReader(new StringReader(request));

        setRequestLine(reader.readLine()); // Request-Line ; Section 5.1

        String header = reader.readLine();
        while (header.length() > 0) {
            appendHeaderParameter(header);
            header = reader.readLine();
        }

        String bodyLine = reader.readLine();
        while (bodyLine != null) {
            appendMessageBody(bodyLine);
            bodyLine = reader.readLine();
        }

    }

    /**
     * 
     * 5.1 Request-Line The Request-Line begins with a method token, followed by
     * the Request-URI and the protocol version, and ending with CRLF. The
     * elements are separated by SP characters. No CR or LF is allowed except in
     * the final CRLF sequence.
     * 
     * @return String with Request-Line
     */
    public String getRequestLine() {
        return _requestLine;
    }

    private void setRequestLine(String requestLine) throws IllegalArgumentException {
        if (requestLine == null || requestLine.length() == 0) {
            throw new IllegalArgumentException("Invalid Request-Line: " + requestLine);
        }
        _requestLine = requestLine;
    }

    private void appendHeaderParameter(String header) throws IllegalArgumentException {
        int idx = header.indexOf(":");
        if (idx == -1) {
            throw new IllegalArgumentException("Invalid Header Parameter: " + header);
        }
        _requestHeaders.put(header.substring(0, idx), header.substring(idx + 1, header.length()));
    }

    /**
     * The message-body (if any) of an HTTP message is used to carry the
     * entity-body associated with the request or response. The message-body
     * differs from the entity-body only when a transfer-coding has been
     * applied, as indicated by the Transfer-Encoding header field (section
     * 14.41).
     * @return String with message-body
     */
    public String getMessageBody() {
        return _messagetBody.toString();
    }

    private void appendMessageBody(String bodyLine) {
        _messagetBody.append(bodyLine).append("\r\n");
    }

    /**
     * For list of available headers refer to sections: 4.5, 5.3, 7.1 of RFC 2616
     * @param headerName Name of header
     * @return String with the value of the header or null if not found.
     */
    public String getHeaderParam(String headerName){
        return _requestHeaders.get(headerName);
    }
}
