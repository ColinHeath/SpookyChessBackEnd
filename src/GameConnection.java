public class GameConnection {
	private ClientConnection left, right;
	private SpookyChessServer scs;
	private boolean alreadyUpdatedScores = false;
	private boolean leftTurnToMove = false;
	
	public GameConnection(ClientConnection left, ClientConnection right, SpookyChessServer scs)
	{
		this.left = left;
		this.left.joinGame(this); // sets GC, sets inGame, and notifies Client
		
		this.right = right;
		this.right.joinGame(this);
		
		this.scs = scs;
	}
	
	// request method for ClientConnection (gives info on the other player's username)
	public String opponentName(ClientConnection requester)
	{
		if(requester.equals(left)) return right.getUsername();
		else return left.getUsername();
	}
	
	// request method for ClientConnection (tells requesting Client if they're going first)
	public boolean movingFirst(ClientConnection requester)
	{
		if(requester.equals(left)) return true;
		else return false;
	}
	
	//TODO: Establish a lock or something to keep turn order in game.
	// sends the given board state to the opponent
	public void transmitBoardState(ClientConnection requester, String boardState)
	{
		if(this.leftTurnToMove)
		{
			if(requester.equals(left))
			{
				right.sendBoardState(boardState);
				left.sendBoardState(boardState);
				this.leftTurnToMove = !this.leftTurnToMove;
			}
			else return;
		}
		else
		{
			if(requester.equals(left)) return;
			else
			{
				right.sendBoardState(boardState);
				left.sendBoardState(boardState);
				this.leftTurnToMove = !this.leftTurnToMove;
			}
		}
	}
	
	// Update the wins/losses for both the requesting client and their opponent.
	// (assuming that exactly one player must win and the other player must lose.)
	public void updateLeaderboard(ClientConnection requester, boolean isWinner)
	{
		if(!alreadyUpdatedScores)
		{
			if(requester.equals(left))
			{
				if(isWinner)
				{
					left.updateStats(true);
					right.updateStats(false);
				}
				else
				{
					left.updateStats(false);
					right.updateStats(true);
				}
			}
			else
			{
				if(isWinner)
				{
					left.updateStats(false);
					right.updateStats(true);
				}
				else
				{
					left.updateStats(true);
					right.updateStats(false);
				}
			}
			alreadyUpdatedScores = true; // set this flag so we don't accidentally duplicate the update
		}
	}
	
	// Close both of this game's Client Connections.
	public void close()
	{
		left.close();
		right.close();
	}
	
	//TODO: Write a game-end method.
}

