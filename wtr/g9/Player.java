package wtr.g9;

import wtr.sim.Point;

import java.util.Random;

public class Player implements wtr.sim.Player {

	// your own id
	private int self_id = -1;

	// the remaining wisdom per player
	private int[] W = null;

	// random generator
	private Random random = new Random();

	// Who talk to next
	private int next_id = -1;
	
	// init function called once
	public void init(int id, int[] friend_ids, int strangers)
	{
		self_id = id;
		// initialize the wisdom array
		int N = friend_ids.length + strangers + 2;
		W = new int [N];
		for (int i = 0 ; i != N ; ++i)
			W[i] = i == self_id ? 0 : -1;
		for (int friend_id : friend_ids)
			W[friend_id] = 50;
	}
	
	
	private int pick_player(Point[] players, int[] chat_ids) {
		// Pick the closest not-chatting player with positive or unknown W
		// value by index in the array
		int self_idx = 0;
		while (players[self_idx].id != self_id) self_idx++;

		double dist = Double.POSITIVE_INFINITY;
		int index = -1;

		for (int i = 0; i < players.length; i++) {
			if (i == self_idx) continue; // Self
			if (chat_ids[i] == -1) continue; // Chatting to someone else
			if (W[players[i].id] == 0) continue; // No value
			
			double dd = getDistance(players[i], players[self_idx]);

			if (dd < dist && dd>=0.25 && dd<=4.0) {
				dist  = dd ;
				index = i;
			}
		}
		return index;
	}

	
	// play function
	public Point play(Point[] players, int[] chat_ids,
	                  boolean wiser, int more_wisdom)
	{
		// find where you are and who you chat with
		int i = 0, j = 0;
		// After this, players[i].id = self_id
		while (players[i].id != self_id) i++; 
		// After this, players[j] is someone whom I'm currently talking to
		while (players[j].id != chat_ids[i]) j++; 
		Point self = players[i];
		Point chat = players[j];
		// record known wisdom (more_wisdom is remaining wisdom)
		W[chat.id] = more_wisdom;
		
		
		
		// If there's somone closer, move on
		boolean closeEnoughToChatter = true;
		double distFromChatter = getDistance(self, chat); 
		double minDistFromOther = Double.MAX_VALUE;
		for(Point p : players){
			if(p == self || p == chat) continue;
			double dist = getDistance(self, p);
			if(dist < minDistFromOther){
				minDistFromOther = dist;
			}
		}
		if(minDistFromOther < distFromChatter) closeEnoughToChatter = false;

		
		if(wiser){
			if(closeEnoughToChatter){
				return new Point(0.0, 0.0, chat.id);
			}
			else{
				// If can move closer, move to min dist
				if(minDistFromOther > 0.25){
					double slope = Math.abs(Math.atan((chat.y - self.y)/(chat.x - self.x)));
					double xOffset = 0.5*Math.cos(slope);
					double yOffset = 0.5*Math.sin(slope);
					

					double newX = (self.x <=chat.x) ? (self.x + xOffset) : (self.x + xOffset);
					double newY = (self.y <=chat.y) ? (self.y + yOffset) : (self.y + yOffset);
					
					// Move to a new position 0.5 metres away from the current position
					Point newSelf = new Point(newX, newY, self_id);
					
					double dx = (chat.x - newSelf.x)*0.50;
					double dy = (chat.y - newSelf.y)*0.50;
					
					next_id = chat.id;
					return new Point(dx, dy, self_id);	
					
					/*double dx = self.x;
					double dy = self.y;
					int factorX = self.x < chat.x ? 1 : -1;
					int factorY = self.y < chat.y ? 1 : -1;
					Point newPoint = new Point(dx,dy,0);
					double newDist = getDistance(chat, newPoint);
					boolean useX = true;
					boolean useY = true;
					double increaseFactor = 0.1;
					while(newDist > 0.25){
						double updatedDist;
						Point updatedPoint;
						if(useX){
							dx += increaseFactor*factorX; 
							updatedPoint = new Point(dx,dy, 0);							
							updatedDist = getDistance(updatedPoint,chat);
							if(updatedDist > newDist || updatedDist < 0.25){
								dx -= increaseFactor*factorX; 
								useX = false;
							}
							else{
								newDist = updatedDist;
							}
						}
						else if(useY){
							dy += increaseFactor*factorY; 
							updatedPoint = new Point(dx,dy, 0);							
							updatedDist = getDistance(updatedPoint,chat);
							if(updatedDist > newDist || updatedDist < 0.25){
								dy -= increaseFactor*factorY; 
								useY = false;
							}
							else{
								newDist = updatedDist;
							}							
						}
						else{
							break;
						}
					}
					// Eventually, try to move in a circle around 
					next_id = chat.id;
					return new Point(dx, dy, self_id);	*/			
				}
			}
		}

		if(next_id != -1){
			// See if player is nearby and isn't chatting with anyone
			int index = 0;
			boolean playerNearby = false;
			for (Point p : players) {
				if(p.id == next_id){
					playerNearby = true;
					break;
				}
				index++;
			}
			next_id = -1;
			if(playerNearby == true && (chat_ids[index] == -1)){
				return new Point(0.0, 0.0, chat.id);				
			}
		}

		
		// try to initiate chat if previously not chatting
		if (i == j)
		{
			int ind = pick_player(players, chat_ids);
			if (ind != -1)
			{
				return new Point(0.0, 0.0, players[ind].id);
			}
		}
			
			/*for (Point p : players) {
				// skip if no more wisdom to gain
				if (W[p.id] == 0) continue;
				// compute squared distance
				double dx = self.x - p.x;
				double dy = self.y - p.y;
				double dd = dx * dx + dy * dy;
				// start chatting if in range
				if (dd >= 0.25 && dd <= 4.0)
					return new Point(0.0, 0.0, p.id);
			}*/
		// return a random move
		double dir = random.nextDouble() * 2 * Math.PI;
		double dx = 6 * Math.cos(dir);
		double dy = 6 * Math.sin(dir);
		return new Point(dx, dy, self_id);
	}
	
	public double getDistance(Point p1, Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		double dd = dx * dx + dy * dy;
		return dd;
		
	}
}
