public class GameConnection {
	private ClientConnection left, right;
	private SpookyChessServer scs;
	
	public GameConnection(ClientConnection left, ClientConnection right, SpookyChessServer scs)
	{
		this.left = left;
		this.left.setInGame(true);
		this.right = right;
		this.right.setInGame(true);
		this.scs = scs;
	}
	
	// Close both of this game's Client Connections.
	public boolean close()
	{
		return (left.close() && right.close());
	}
}

