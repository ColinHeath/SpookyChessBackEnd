

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class ClientConnection extends Thread {
	private int userID = -1;
	private String username = "Guest"; // will get updated on successful login or account creation
	private Socket connectedSocket;
	private SpookyChessServer connectedServer;
	private GameConnection gc;
	private boolean inGame; // flag that tracks whether this client is currently in a game or not
	
	private BufferedReader bufferedInput;
	private PrintWriter outputWriter;
	private boolean isRunning;
	
	public ClientConnection(Socket connectedSocket, SpookyChessServer spookyChessServer)
	{
		this.connectedServer = spookyChessServer;
		this.connectedSocket = connectedSocket;
		inGame = false;
		gc = null;
		
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
		int[] record = connectedServer.verifyAccount(username, password);
		if(record==null)
		{
			String response = "valid=false";
			sendToClient(response);
			return false;
		}
		else
		{
			this.userID=record[2];
			String response = "valid=true&userID="+record[2]+"&wins="+record[0]+"&losses="+record[1];
			this.username = userName;
			sendToClient(response);
			return true;
		}
	}
	
	public boolean createAccount(String userName, String password)
	{
		int result = this.connectedServer.createUser(username, password);
		if(result==-1)
		{
			String response = "valid=false";
			sendToClient(response);
			return false;
		}
		else
		{
			this.userID = result;
			String response = "valid=true&userID="+result;
			this.username = userName;
			sendToClient(response);
			return true;
		}
	}
	
	/* Note: to demo this, run a SpookyChessServer on port 8080 then paste the following into your web browser's URL bar:
	 * 	http://localhost:8080/?function=login&username=ben&password=sponge
	 */
	public String readData()
	{
		String totalInput = "";

		try
		{
			String inputLine;
			inputLine = this.bufferedInput.readLine();
			while( !inputLine.equals("") ) // HTTP requests end in an empty line
			{
				if(inputLine.length() >= 3 && inputLine.substring(0, 3).equals("GET")) // only keep GET request line
				{
					totalInput += inputLine + "\n";
					System.out.println(inputLine);
				}
				inputLine = this.bufferedInput.readLine();
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
	
	// Accessors
	public String username() {return username;}

	public void writeData(String toWrite)
	{
		this.outputWriter.print(toWrite);
	}
	
	// mutator method for inGame variable.
	public void setInGame(boolean inGame)
	{
		this.inGame = inGame;
	}
	
	public void setGC(GameConnection gc)
	{
		this.gc = gc;
	}
	
	// message the Client
	private void sendToClient(String response)
	{
		this.outputWriter.print(response);
		this.outputWriter.flush();
	}
	
	// sets inGame, sets GC, and notifies client
	public void joinGame(GameConnection gc)
	{
		inGame = true;
		this.gc = gc;
		// send response to client that we've joined a game
		// include opponent name and whether they're moving first
		String opponentName = gc.opponentName(this);
		boolean movingFirst = gc.movingFirst(this);
		sendToClient("opponentName="+opponentName+"&moveFirst="+movingFirst);
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
	
	public Map<String, String> parseRequest(String request)
	{
		System.out.println("In parseRequest");
		Map<String, String> params = new HashMap<String, String>();
		String strippedRequest = request.split(" ")[1].split("\\?")[1];
		// Use regex to split on = and &
		String[]tokens = strippedRequest.split("=|\\&");
		
		assert tokens.length % 2 == 0 : " Number of tokens is odd"; // keys and values must be equal, thus there must be an even number total
		for(int i=0; i<tokens.length; i+=2)
		{
			System.out.println(tokens[i]+": "+tokens[i+1]);
			params.put(tokens[i], tokens[i+1]);
		}
		return params;
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
				Map<String, String> params = parseRequest(currentRequest);
				
				String intent = params.get("function");
				if(intent.equals("login"))
				{
					String username = params.get("username");
					String password = params.get("password");
					
					loginUser(username, password);
				}
				else if(intent.equals("createAccount"))
				{
					String username = params.get("username");
					String password = params.get("password");
					
					createAccount(username, password);
				}
				else if(intent.equals("joinMatchmaking"))
				{
					connectedServer.addToMatchmakingQueue(this);
				}
			}
		}
	}
}
