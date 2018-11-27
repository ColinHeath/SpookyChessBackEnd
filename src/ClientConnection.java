
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class ClientConnection extends Thread {
	private int userID = -1;
	private String username = "Guest"; // will get updated on successful login or account creation
	private Socket connectedSocket;
	private SpookyChessServer connectedServer;
	private GameConnection connectedGame;
	private boolean inGame; // flag that tracks whether this client is currently in a game or not
	
	private BufferedReader bufferedInput;
	private PrintWriter outputWriter;
	private boolean isRunning;
	
	public ClientConnection(Socket connectedSocket, SpookyChessServer spookyChessServer)
	{
		this.connectedServer = spookyChessServer;
		this.connectedSocket = connectedSocket;
		this.connectedGame = null;
		this.inGame = false;
		this.isRunning = true;
		
		try
		{
			this.bufferedInput = new BufferedReader(new InputStreamReader(this.connectedSocket.getInputStream()));
			this.outputWriter = new PrintWriter(this.connectedSocket.getOutputStream(), true);
		}
		catch (IOException e) //TODO: Consider closing the connection here, because there's almost no point in persisting a readless, writeless connection.
		{
			e.printStackTrace();
		}
		
		this.start();
	}
	
	public boolean loginUser(String userName, String password)
	{
		int[] record = connectedServer.verifyAccount(username, password);
		
		if(record == null)
		{
			String response = "valid=false";
			sendToClient(response);
			return false;
		}
		else
		{
			this.userID = record[2];
			String response = "valid=true&userID=" + record[2] + "&wins=" + record[0] + "&losses=" + record[1];
			this.username = userName;
			sendToClient(response);
			return true;
		}
	}
	
	public boolean createAccount(String userName, String password)
	{
		int result = this.connectedServer.createUser(username, password);
		if(result == -1)
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
			while(this.bufferedInput.ready())
			{
				String inputLine = this.bufferedInput.readLine();
				totalInput += inputLine + "\n";
			}
			
			if(!totalInput.equals("")) System.out.println("Message from client: " + totalInput);
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
	public String getUsername() {return username;}
	
	// mutator method for inGame variable.
	public void setInGame(boolean inGame)
	{
		this.inGame = inGame;
	}
	
	// mutator method for gc variable.
	public void setGC(GameConnection gc)
	{
		this.connectedGame = gc;
	}
	
	// send board state to this client
	public void sendBoardState(String boardState)
	{
		sendToClient("UpdateBoard="+boardState);
	}
	
	// message the Client
	private void sendToClient(String response)
	{
		/*String OUTPUT = response;
		String OUTPUT_HEADERS = "HTTP/1.1 200 OK\r\n" +
		    "Content-Type: text/plain\r\n" + 
		    "Content-Length: ";
		String OUTPUT_END_OF_HEADERS = "\r\n\r\n";
		String httpResponse = OUTPUT_HEADERS + OUTPUT.length() + OUTPUT_END_OF_HEADERS + OUTPUT;*/
		this.outputWriter.println(response);
		this.outputWriter.flush();
		System.out.println("Sent: "+ response);
	}
	
	// Calls SpookyChessServer's updateStats method as long as this Client isn't a guest
	void updateStats(boolean isWinner)
	{
		if(userID != -1)
		{
			connectedServer.updateStats(isWinner, userID);
		}
	}
	
	// sets inGame, sets GC, and notifies client
	public void joinGame(GameConnection gc)
	{
		inGame = true;
		this.connectedGame = gc;
		// send response to client that we've joined a game
		// include opponent name and whether they're moving first
		String opponentName = gc.opponentName(this);
		boolean movingFirst = gc.movingFirst(this);
		this.sendToClient("StartGame");
		this.sendToClient((movingFirst) ? "SetPlayerOne" : "SetPlayerTwo");
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
		try
		{
			String strippedRequest = request.split(" ")[1].split("\\?")[1];
			// Use regex to split on = and &
			String[]tokens = strippedRequest.split("=|\\&");
			assert tokens.length % 2 == 0 : " Number of tokens is odd"; // keys and values must be equal, thus there must be an even number total
			for(int i = 0; i < tokens.length; i+=2)
			{
				System.out.println(tokens[i]+": "+tokens[i+1]);
				params.put(tokens[i], tokens[i+1]);
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			return null;
		}
		
		return params;
	}
	
	public void run()
	{

		//When the reader has text, check if it's a login request. If so, do your thing.
		//Otherwise, ignore it. Let GameConnection take other values out.
		while(isRunning)
		{
			// Parse our current request.
			String currentRequest = this.readData();
			
			if(!currentRequest.equals("")) this.sendToClient("Message Received: " + currentRequest);
			
			HashMap<String, String> params = parseParameters(currentRequest);
			String intent = "";
			
			try
			{
				intent = params.get("function");
			}
			catch(NullPointerException e)
			{
				continue;
			}
			
			if(inGame)
			{
				// Do Game Things.
				if(intent.equals("updateboard"))
				{
					String boardState = params.get("move");
					connectedGame.transmitBoardState(this, boardState);
				}
				else if(intent.equals("updateLeaderboard"))
				{
					String isWinnerStr = params.get("result");
					
					if(isWinnerStr.equals("win"))
						connectedGame.updateLeaderboard(this, true);
					else if(isWinnerStr.equals("lose"))
						connectedGame.updateLeaderboard(this, false);
					else // if it's neither "win" nor "lose", there has been a mistake
						throw new IllegalArgumentException("'result' must be either 'win' or 'lose'! Received: '"+isWinnerStr+"'");
				}
				else
				{
					throw new IllegalArgumentException("Unsupported function request: '"+intent+"'");
				}
			}
			else
			{
				// Handle non-game Things.
				if(intent.equals("login"))
				{
					String username = params.get("username");
					String password = params.get("password");
					
					if(loginUser(username, password))
					{
						this.sendToClient("LoggedIn");
					}
				}
				else if(intent.equals("register"))
				{
					String username = params.get("username");
					String password = params.get("password");
					
					if(createAccount(username, password))
					{
						this.sendToClient("Registered");
					}
				}
				else if(intent.equals("joinmatchmaking"))
				{
					connectedServer.addToMatchmakingQueue(this); // when the queue pairs this user, a GC will be made,
						//	whose constructor will call joinGame() on this ClientConnection.
				}
				else
				{
					this.sendToClient("error=unknownfunction");
					throw new IllegalArgumentException("Unsupported function request: '"+intent+"'");
				}
			}
		}
	}
	
	public HashMap<String, String> parseParameters(String inputString)
	{
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		String[] splitInput = inputString.split("\n");
		
		for(int i = 0; i < splitInput.length; i++)
		{
			if(splitInput[i].matches("^.*=.*$"))
			{
				String[] keyValue = splitInput[i].split("=");
				parameters.put(keyValue[0], keyValue[1]);
			}
		}
		
		return parameters;
	}
}
