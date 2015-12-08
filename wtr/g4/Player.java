package wtr.g4;

import wtr.sim.Point;

import java.util.Random;

import java.lang.Math;

public class Player implements wtr.sim.Player {

	// your own id
	private int self_id = -1;

	// the remaining wisdom per player
	private int[] W = null;
	private int ticks = 0;
	private int soulmate = -1;
	private int tries = 0;
	boolean soulmate_present = false;
	HashMap<Integer, Integer> num_neighbors = null;

	// random generator
	private Random random = new Random();

<<<<<<< HEAD
    private int interferedChats = 0;
=======
	// friends
	ArrayList<Integer> friends = new ArrayList<>();

	ArrayList<ArrayList<Point>> priority = new ArrayList<>();

	// number of times failed initiating the talk
	private int fails = 0;

	// last player that we wanted to chat with
	private int lastPlayer = -1;

	// number of interfered chats
	private int interferedChats = 0;

>>>>>>> origin/master
	// init function called once
	public void init(int id, int[] friend_ids, int strangers)
	{
		self_id = id;
		// initialize the wisdom array
		int N = friend_ids.length + strangers + 2;
		W = new int [N];
		for (int i = 0 ; i != N ; ++i)
			W[i] = i == self_id ? 0 : -1;
		for (int friend_id : friend_ids) {
			friends.add(friend_id);
			W[friend_id] = 50;
<<<<<<< HEAD
=======
		}
		int nStrangers = strangers;
>>>>>>> origin/master
	}

	//decide where to move
	private Point move(Point[] players, int[] chat_ids, Point self)
	{
		//Move to a new position within the room anywhere within 6 meters of your current position. Any conversation you were previously engaged in is terminated: you have to be stationary to chat.
		//Initiate a conversation with somebody who is standing at a distance between 0.5 meters and 2 meters of you. In this case you do not move. You can only initiate a conversation with somebody on the next turn if they and you are both not currently engaged in a conversation with somebody else. 	
		int i = 0, target = 0;
		while (players[i].id != self_id) i++;
		//look through players who are within 6 meters => Point[] players
<<<<<<< HEAD
		Point p = self;
        for (target = 0; target<players.length; ++target) {
			//if we do not contain any information on them, they are our target => W[]
			if (W[p.id] != -1 || p.id == self.id) continue;
            p = players[target];
        }
        if (p.equals(self)) {
            for (target = 0; target < players.length; ++target) {
                if (W[p.id] == 0 || p.id == self.id) continue;
                p = players[target];
            }
        }
        if (!p.equals(self)) {
=======
		
		// if soulmate within 6 meters, talk/move to soulmate
		if (soulmate > 0 && soulmate_present && tries < 3) {
			int j = 0;
			while (players[j].id != soulmate) j++;
			double dx1 = self.x - players[j].x;
			double dy1 = self.y - players[j].y;
			double dd1 = dx1 * dx1 + dy1 * dy1;
			if (dd1 >= 0.25 && dd1 <= 4.0) {
				tries++;
				return new Point (0.0, 0.0, j);
			}
			else {
				dx1 = dx1-.5*(dx1/dd1);
				dy1 = dy1-.5*(dy1/dd1);
				tries++;
				return new Point(dx1, dy1, self.id);
			}
		}
		tries = 0;
		for (int target =0; target<players.length; ++target) {
			Point p = players[target];
			//if we do not contain any information on them, they are our target => W[]
			//if (ticks < 1200) {
			if (W[p.id] != -1 || p.id == self.id) {
				continue;
			}
			//}
			//else {
			//		if (W[p.id] == 0 || p.id == self.id) {
			//		continue;
			//}
			//}
>>>>>>> origin/master
			// compute squared distance
			double dx1 = self.x - p.x;
			double dy1 = self.y - p.y;
			double dd1 = dx1 * dx1 + dy1 * dy1;
			//check if they are engaged in conversations with someone
			int chatter = 0;
			while (chatter<players.length && players[chatter].id != chat_ids[target]) chatter++;

			//check if they are engaged in conversations with someone and whether that person is in our vicinity
			if(chat_ids[target]!=p.id && chatter!=players.length)
			{
				//if they are, we want to stand in front of them, .5 meters in the direction of who they're conversing with => check if result is within 6meters
				Point other = players[chatter];
				double dx2 = self.x - other.x;
				double dy2 = self.y - other.y;
				double dd2 = dx2 * dx2 + dy2 * dy2;

				double dx3 = dx2-dx1;
				double dy3 = dy2-dy1;
				double dd3 = Math.sqrt(dx3 * dx3 + dy3 * dy3);

				double dx4 = (dx2 - 0.5*(dx3/dd3)) * -1;
				double dy4 = (dy2 - 0.5*(dy3/dd3)) * -1;
				double dd4 = dx4 * dx4 + dy4 * dy4;

<<<<<<< HEAD
				if (dd4 <= 2.0)
					return new Point(dx4, dy4, self.id);
            }
			//if not chatting to someone or don't know position of chatter, just get as close to them as possible
			else return new Point((dx1-0.5*(dx1/dd1)) * -1, (dy1-0.5*(dy1/dd1)) * -1, self.id);				
		}

			double dir = random.nextDouble() * 2 * Math.PI;
			double dx = 6 * Math.cos(dir);
			double dy = 6 * Math.sin(dir);
			return new Point(dx, dy, self_id);
=======
				if (dd4 <= 2.0) {
					return new Point(dx4, dy4, self.id);
				}

			}
			//if not chatting to someone or don't know position of chatter, just get as close to them as possible
			else {
				dx1 = dx1-.5*(dx1/dd1);
				dy1 = dy1-.5*(dy1/dd1);
				return new Point(dx1, dy1, self.id);
			}
		}

		boolean found = false;
		double dir = 0.0, num = 0.0, dx = 0.0, dy = 0.0;

		return randomMove(self);
	}

	public class PlayerComparator implements Comparator<Point> {
		/**
		 * compare players by wisdom left
		 * expected wisdom of strangers are 12
		 */
		public int compare(Point p1, Point p2) {
			//weigh by number of interfering neighbors
			int d1 = num_neighbors.get(p1.id)+1; //so we don't have to divide by 0
			int d2 = num_neighbors.get(p2.id)+1;
			int s1 = W[p1.id] >= 0 ? W[p1.id] : 12;
			int s2 = W[p2.id] >= 0 ? W[p2.id] : 12;

			int r1 = s1/d1;
			int r2 = s2/d2;

			return r2 - r1;
			//return s2 - s1;
		}
>>>>>>> origin/master
	}

	public void getnumneighbors(Point self, Point[] players)
	{
		num_neighbors = new HashMap<Integer, Integer>();
		for(int i=0; i<players.length; ++i)
		{
			num_neighbors.put(players[i].id, 0);
		}

		for(int i=0; i<players.length; ++i)
		{
			Point p = players[i];
			double dx = self.x - p.x;
			double dy = self.y - p.y;
			double dist_to_us = dx * dx + dy * dy;
			for(int j=0; j<players.length; ++j)
			{
				if(i==j) continue;
				Point b = players[i];
				double dx1 = b.x - p.x;
				double dy1 = b.y - p.y;
				double dd = dx1 * dx1 + dy1 * dy1;
				if(dd<dist_to_us){
					int value = num_neighbors.get(p.id);
					num_neighbors.put(p.id, value++);
				}
			}
		}
	}
	// play function
	public Point play(Point[] players, int[] chat_ids,
			boolean wiser, int more_wisdom)
	{
		// find where you are and who you chat with
<<<<<<< HEAD
		int i = 0, j = 0, chatCount = 3;
=======
		int i = 0, j = 0, k = 0;
		ticks++;
>>>>>>> origin/master
		while (players[i].id != self_id) i++;
		while (players[j].id != chat_ids[i]) j++;
		
		Point self = players[i];
		Point chat = players[j];
		// record known wisdom
		W[chat.id] = more_wisdom;
<<<<<<< HEAD
        if (more_wisdom > 50) chatCount = 6;
		// attempt to continue chatting if there is more wisdom
		if (wiser) {
			interferedChats = 0;
            return new Point(0.0, 0.0, chat.id);
		}
        else if (interferedChats < chatCount) {
            interferedChats++;
            return new Point(0.0, 0.0, chat.id);
        }
		// try to initiate chat if previously not chatting
		if (i == j) {
            int id = -1;
            double max_distance = Double.MAX_VALUE;
			for (Point p : players) {
=======
		if (more_wisdom > 50) {
			soulmate = chat.id;
		}

		soulmate_present = false;
		if (soulmate != -1) {
			for (Point p : players) {
				if (p.id == soulmate) {
					soulmate_present = true;
				}
			}
		}
		// if (self_id == 1 && i != j)
		// 	System.err.println("Wiser " + wiser + ", more_wisdom " + more_wisdom + ", W " + W[chat.id]);


		// Handle the case where the player is chatting
		if (wiser) {
			interferedChats = 0;
			return new Point(0.0, 0.0, chat.id);
		} else if (interferedChats < 2 /*&& ticks > 1800*/) {
			interferedChats++;
			//return new Point(0.0, 0.0, chat.id);
			return new Point(0.0, 0.0, self.id);
		}

		getnumneighbors(self, players);
		// check if player is in a crowd
		int personCount = 0;
		for (Point p : players) {
			double dx = self.x - p.x;
			double dy = self.y - p.y;
			double dd = dx * dx + dy * dy;
			if (dd < 4.0) {
				personCount++;
			}
			if (personCount > 10 && personCount > W.length / 5) {
				return randomMove(self);
			}
		}

		// try to initiate chat if previously not chatting
		if (i == j) {
			priority.clear();
			priority.add(new ArrayList<Point>());
			priority.add(new ArrayList<Point>());
			

			for (int index = 0; index < players.length; index++) {
				Point p = players[index];
				int chattedByP = chat_ids[index];

>>>>>>> origin/master
				// skip if no more wisdom to gain
				if (W[p.id] == 0) continue;
				// compute squared distance
				double dx = self.x - p.x;
				double dy = self.y - p.y;
				double dd = dx * dx + dy * dy;
<<<<<<< HEAD
				// start chatting if in range
				if (dd >= 0.25 && dd <= 4.0 && dd < max_distance) {
                    id = p.id;
                }
=======

				// put qualified players into priority
				if (p.id == chattedByP) {
					if (dd >= 0.25 && dd <= 4.0) {

						priority.get(0).add(p);  // available, within talking distance
					}
				} else if (dd >= 0.25) {
					priority.get(1).add(p);  // within range
				}
			}

			ArrayList<Point> myList = priority.get(0);
			Collections.sort(myList, new PlayerComparator());

			// get the player with highest wisdom
			for (Point p : myList) {
				if (fails > 3 && p.id == lastPlayer) {
					continue;
				}

				fails++;
				lastPlayer = p.id;
				// if (self_id == 1)
				// 	System.err.println(self_id + " list0, talking to " + p.id + " wisdom " + W[p.id]);
				return new Point(0.0, 0.0, p.id);
			}
			fails = 0;

			myList = priority.get(1);
			if (!myList.isEmpty()) {
				Collections.sort(myList, new PlayerComparator());
				Point p = myList.get(0);

				// compute squared distance
				double dx = p.x - self.x;
				double dy = p.y - self.y;
				double dd = dx * dx + dy * dy;

				// get close to the player
				// if (self_id == 1)
				// 	System.err.println(self_id + " list1, talking to " + p.id + " wisdom " + W[p.id]);
				return new Point(0.8 * dx, 0.8 * dy, self_id);
>>>>>>> origin/master
			}
            if (id != -1) return new Point(0.0, 0.0, id);
		}
		return move(players, chat_ids, self);
	}

	// returns random move 6 meters
	public Point randomMove(Point self) {
		boolean found = false;
		double dir = 0.0, num = 0.0, dx = 0.0, dy = 0.0;

		while (!found) {
			dir = random.nextDouble() * 2 * Math.PI;
			dx = 6 * Math.cos(dir);
			dy = 6 * Math.sin(dir);
			if ((self.x + dx <= 20) && (self.x + dx >= 0) && (self.y + dy <= 20) && (self.y + dy >= 0) && 
					(dx * dx + dy * dy <= 36)) {
				found = true;
			}
		}
		return new Point(dx, dy, self_id);
	}
}
