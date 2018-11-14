import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;


public class SpookyChessServer {
	// Final Vars
	public static final int PORT = 8080;
	public static final String DBURL = "jdbc:mysql://localhost:3306/spookychess";
	public static final String DBUSERNAME = "root";
	public static final String DBPASSWORD = "root";
	
	// Instance Variables
	private Queue<ClientConnection> matchmakingQueue = new LinkedList<ClientConnection>();
	private Set<ClientConnection> openConnections = new HashSet<ClientConnection>();
	private Vector<GameConnection> games = new Vector<GameConnection>();
	
	// Constructor
	public SpookyChessServer(int port) {
		ServerSocket ss = null;
		try {
			System.out.println("Trying to bind to port " + port);
			ss = new ServerSocket(port);
			System.out.println("Bound to port " + port);
			// now continuously accept new connections while also reading messages from user (use threads)
			while (true) {
				Socket s = ss.accept();
				System.out.println("Connection from " + s.getInetAddress());
				ClientConnection cc = establishConnection(s);
				// addToMatchmakingQueue(cc); move to ClientConnection
			}
		} catch (IOException ioe) {
			System.out.println("ioe: "+ioe.getMessage());
		} finally {
			try {
				if(ss != null) ss.close();
			} catch (IOException ioe) {
				System.out.println("ioe: "+ioe.getMessage());
			}
		}
	}
	
	// Establishes a connection with a given socket
	public ClientConnection establishConnection(Socket s)
	{
		ClientConnection cc = new ClientConnection(s, this);
		openConnections.add(cc);
		return cc;
	}
	
	// Adds the given ClientConnection to the Matchmaking Queue.
	public void addToMatchmakingQueue(ClientConnection toPair)
	{
		matchmakingQueue.add(toPair);
		// If we now have at least two users in the Queue, we can create a game
		if(matchmakingQueue.size()>=2)
		{
			ClientConnection left = matchmakingQueue.remove();
			ClientConnection right = matchmakingQueue.remove();
			createGame(left, right);
		}
	}
	
	// Creates a user with the given credentials in the DB. Returns the userID
	public int createUser(String username, String password)
	{
		int userID = -1;
		
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		String connection = DBURL+"?user="+DBUSERNAME+"&password="+DBPASSWORD;
		try
		{
			Class.forName("com.mysql.cj.jdbc.Driver"); // Dynamically loads the class specified in the String
			conn = DriverManager.getConnection(connection); // Uses the last loaded Driver
			st = conn.createStatement();
			String update = "INSERT INTO Users(username, password, wins, losses) "
					+ "VALUES (\""+ username+"\",\""+password+"\", 0, 0);";
			st.executeUpdate(update);
			
			// Now get userID
			st = conn.createStatement();
			ps = conn.prepareStatement("SELECT * FROM Users WHERE username=? AND password=?");
			ps.setString(1, username); // question marks count from 1 up
			ps.setString(2, password);
			rs = ps.executeQuery();
			
			while(rs.next()) {
				userID = rs.getInt("userID");
			}
		} catch (SQLException sqle) {
			System.out.println("sqle: " + sqle.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println("cnfe: " + cnfe.getMessage());
		} finally {
			try {
				if(rs!=null) {rs.close();}
				if(st!=null) {st.close();}
				if(conn!=null) {conn.close();}
			} catch(SQLException sqle) {
				System.out.println("SQLE in SCS:createUser() finally: "+sqle.getMessage());
			}
		}
		
		return userID;
	}
	
	// Checks whether the given credentials already exists in the DB. If so, returns wins, losses, and userID as an int array
	// Otherwise, return null.
	public int[] verifyAccount(String username, String password)
	{
		int wins = -1;
		int losses = -1;
		int userID = -1;
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		String connection = DBURL+"?user="+DBUSERNAME+"&password="+DBPASSWORD;
		try
		{
			Class.forName("com.mysql.cj.jdbc.Driver"); // Dynamically loads the class specified in the String
			conn = DriverManager.getConnection(connection); // Uses the last loaded Driver
			st = conn.createStatement();
			ps = conn.prepareStatement("SELECT * FROM Users WHERE username=? AND password=?");
			ps.setString(1, username); // question marks count from 1 up
			ps.setString(2, password);
			rs = ps.executeQuery();
			
			while(rs.next()) {
				wins = rs.getInt("wins");
				losses = rs.getInt("losses");
				userID = rs.getInt("userID");
			}
		} catch (SQLException sqle) {
			System.out.println("sqle: " + sqle.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println("cnfe: " + cnfe.getMessage());
		} finally {
			// finally block always executes and the end of the try-catch block no matter what happens
			// usually used to CLOSE connections, streams, etc.
			try {
				if(rs!=null) {rs.close();}
				if(st!=null) {st.close();}
				if(conn!=null) {conn.close();}
			} catch(SQLException sqle) {
				System.out.println("OH OH");
			}
		}
		if(wins==-1 || losses==-1 || userID==-1)
		{
			// no account yet
			return null;
		}
		else
		{
			return new int[]{wins, losses, userID};
		}
	} 
	
	// Creates a game using the two given ClientConnections.
	public void createGame(ClientConnection left, ClientConnection right)
	{
		System.out.println("Creating a game");
		games.add(new GameConnection(left, right, this));
	}
	
	public static void main(String[] args) {
		new SpookyChessServer(PORT);
	}
}
