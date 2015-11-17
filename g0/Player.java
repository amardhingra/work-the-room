package wtr.g0;

import wtr.sim.Point;

import java.util.Random;

public class Player implements wtr.sim.Player {

	// your own id
	private int id = -1;

	// the remaining wisdom per player
	private int[] W = null;

	// random generator
	private Random random = new Random();

	// init function called once
	public void init(int id, int[] friend_ids, int strangers)
	{
		this.id = id;
		// initialize the wisdom array
		int N = friend_ids.length + strangers + 2;
		W = new int [N];
		for (int i = 0 ; i != N ; ++i)
			W[i] = i == id ? 0 : -1;
		for (int friend_id : friend_ids)
			W[friend_id] = 50;
	}

	// play function
	public Point play(Point[] players, int[] chat_ids,
	                  boolean wiser, int more_wisdom)
	{
		// find where you are and who you chat with
		int i = 0, j = 0;
		while (players[i].id != id) i++;
		while (players[j].id != chat_ids[i]) j++;
		Point self = players[i];
		Point chat = players[j];
		// record known wisdom
		if (more_wisdom >= 0)
			W[chat.id] = more_wisdom;
		// record unknown wisdom
		else if (self != chat && !wiser) {
			// must be closest to draw conclusion
			boolean c = true;
			double d = self.distance(chat);
			for (Point p : players)
				if (p != self && p != chat && p.distance(self) < d) {
					c = false;
					break;
				}
			if (c) W[chat.id] = 0;
		}
		// attempt to continue chatting if there is more wisdom
		if (wiser) return new Point(0.0, 0.0, chat.id);
		// try to initiate chat if previously not chatting
		if (self == chat)
			for (Point p : players) {
				double d = self.distance(p);
				if (W[p.id] != 0 && d >= 0.5 && d <= 2.0)
					return new Point(0.0, 0.0, p.id);
			}
		// return a random move
		double dir = random.nextDouble() * 2 * Math.PI;
		double dx = 6 * Math.cos(dir);
		double dy = 6 * Math.sin(dir);
		return new Point(dx, dy, id);
	}
}
