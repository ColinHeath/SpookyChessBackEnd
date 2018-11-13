import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection extends Thread {
	private String userID;
	private Socket connectedSocket;
	private SpookyChessServer connectedServer;
	private boolean inGame; // flag that tracks whether this client is currently in a game or not
	
	private BufferedReader bufferedInput;
	private PrintWriter outputWriter;
	
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
		
		this.start();
	}
	
	public boolean loginUser(String userID, String password)
	{
		//TODO: Use JDBC to login the user. Shouldn't be terribly difficult.
		return false;
	}
	
	public String readData()
	{
		String totalInput = "";
		String inputLine;
		
		try {
			while((inputLine = this.bufferedInput.readLine()) != null)
			{
				totalInput += inputLine + "\n";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return totalInput;
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
	
	public boolean close()
	{
		
		try {
			this.connectedSocket.close();
			this.join();
			return true;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void run()
	{
		//When the reader has text, check if it's a login request. If so, do your thing.
		//Otherwise, ignore it. Let GameConnection take other values out.
		//Not sure how to check for one value while saving others for removal (buffering within the class?)
	}
}
