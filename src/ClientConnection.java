import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection extends Thread {
	private int userID = -1;
	private Socket connectedSocket;
	private SpookyChessServer connectedServer;
	
	private BufferedReader bufferedInput;
	private PrintWriter outputWriter;
	private boolean isRunning;
	private boolean inGame;
	
	public ClientConnection(Socket connectedSocket, SpookyChessServer spookyChessServer)
	{
		this.connectedServer = spookyChessServer;
		
		this.bufferedInput = new BufferedReader(new InputStreamReader(connectedSocket.getInputStream()));
		this.outputWriter = new PrintWriter(connectedSocket.getOutputStream(), true);
		
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
