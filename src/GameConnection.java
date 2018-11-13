public class GameConnection {
	private ClientConnection left, right;
	private SpookyChessServer scs;
	
	public GameConnection(ClientConnection left, ClientConnection right, SpookyChessServer scs)
	{
		this.left = left;
		this.left.setInGame(true);
		this.left.setGC(this);
		
		this.right = right;
		this.right.setInGame(true);
		this.right.setGC(this);
		
		this.scs = scs;
	}
	
	// Close both of this game's Client Connections.
	public void close()
	{
		left.close();
		right.close();
	}
}

