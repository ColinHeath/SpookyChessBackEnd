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
	private String userID;
	private Socket connectedSocket;
	private SpookyChessServer connectedServer;
	
	private BufferedReader bufferedInput;
	private PrintWriter outputWriter;
	
	public ClientConnection(Socket connectedSocket, SpookyChessServer spookyChessServer)
	{
		this.connectedServer = spookyChessServer;
		
		this.bufferedInput = new BufferedReader(new InputStreamReader(connectedSocket.getInputStream()));
		this.outputWriter = new PrintWriter(connectedSocket.getOutputStream(), true);
		
		this.start();
	}
	
	public boolean loginUser(String userID, String password)
	{
		//TODO: Use JDBC to login the user. Shouldn't be terribly difficult.
	}
	
	public String readData()
	{
		String totalInput = "";
		String inputLine;
		
		while((inputLine = this.bufferedInput.readLine()) != null)
		{
			totalInput += inputLine + "\n";
		}
		
		return totalInput;
	}

	public void writeData(String toWrite)
	{
		this.outputWriter.print(toWrite);
	}
	
	public boolean close()
	{
		this.connectedSocket.close();
		this.join();
	}
	
	public void run()
	{
		//When the reader has text, check if it's a login request. If so, do your thing.
		//Otherwise, ignore it. Let GameConnection take other values out.
		//Not sure how to check for one value while saving others for removal (buffering within the class?)
	}
}
