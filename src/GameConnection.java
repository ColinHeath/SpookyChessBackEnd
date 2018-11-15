public class GameConnection {
	private ClientConnection left, right;
	private SpookyChessServer scs;
	
	public GameConnection(ClientConnection left, ClientConnection right, SpookyChessServer scs)
	{
		this.left = left;
		this.left.joinGame(this); // sets GC, sets inGame, and notifies Client
		this.right.joinGame(this);
		
		this.scs = scs;
	}
	
	// request method for ClientConnection
	public String opponentName(ClientConnection requester)
	{
		if(requester.equals(left)) return right.username();
		else return left.username();
	}
	
	public boolean movingFirst(ClientConnection requester)
	{
		if(requester.equals(left)) return true;
		else return false;
	}
	
	// Close both of this game's Client Connections.
	public void close()
	{
		left.close();
		right.close();
	}
}

