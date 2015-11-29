package wtr.g6;

import wtr.sim.Point;

import java.util.*;


public class Player implements wtr.sim.Player {

    int tick = 0;

	// your own id
	private int self_id = -1;
    private int soulmate_id = -1;

    // map of all people in the room
    Map<Integer, Person> people;

	// random generator
	private Random random = new Random();

	PriorityQueue<Person> maximum_wisdom_queue;
    WisdomComparator comparator;
//	int[] spoken; //0 = not spoken, 1=hello, 2 = zero wisdom left

    // global override of who to talk to
    int talk_to_id = -1;
	int count = 0;
    int k_turn = 3;

    Map<Integer, Turn> turns;

	private boolean exhaust = false;

	// init function called once
	public void init(int id, int[] friend_ids, int strangers)
	{
        turns = new HashMap<Integer, Turn>();
        turns.put(0, new Turn(0, self_id));
        people = new HashMap<Integer, Person>();
		self_id = id;
		// initialize the wisdom array
		int N = friend_ids.length + strangers + 2;
		for (int i = 0 ; i != N ; ++i) {
            int wisdom = i == self_id ? 0 : -1;
            Person person = new Person(i, wisdom);
            people.put(i, person);
        }
		for (int friend_id : friend_ids) {
            people.get(friend_id).wisdom = 50;
        }

        comparator = new WisdomComparator(people.get(self_id));

//		spoken = new int[N];
//		Arrays.fill(spoken, 0); //0 = not spoken, 1=hello, 2 = zero wisdom left

	}

	// play function
	public Point play(Point[] players, int[] chat_ids,
	                  boolean wiser, int more_wisdom)
	{
        Point response = new Point(0, 0, self_id);

        try {

            // update tracking parameters
            tick++;
            turns.put(tick, new Turn(tick));
            // find where you are and who you chat with
            int i = 0, j = 0;
            while (players[i].id != self_id) i++; // Find myself in the players array.
            while (players[j].id != chat_ids[i]) j++; // Find my chat-buddy (who I'm currently talking to)
            Point self = players[i];
            Point chat = players[j];
            // update wisdom of the person chatting with
            people.get(chat.id).wisdom = more_wisdom;
            if(more_wisdom > 50) {
                soulmate_id = chat.id;
            }

            Turn prev_turn = turns.get(tick - 1);
            prev_turn.spoke = prev_turn.chat_id_tried == chat_ids[i];
            prev_turn.wiser = wiser;

            // update people's info
            updatePeopleAndQueue(players, chat_ids);

            if(wiser && more_wisdom > 0) {
                response = new Point(0, 0, chat.id);
                return response;
            }

            // look for soulmate
            for(Point player: players) {
                if(player.id == soulmate_id) {
                    if(people.get(soulmate_id).wisdom > 0) {
                        if(Utils.inTalkRange(self, player) && lastKTurnsSuccessful(k_turn, soulmate_id)) {
                            response = new Point(0, 0, soulmate_id);
                            return response;
                        } else {
                            response = moveCloserToPerson(self, player);
                            return response;
                        }
                    }
                }
            }

//            if (talk_to_id != -1) {
//                Point result = new Point(0, 0, talk_to_id);
//                talk_to_id = -1;
//                response = result;
//                return response;
//            }

            // System.out.println("Player "+self.id+" now talking to "+chat.id);
//		spoken[chat.id] = wiser==true? 1:2; //wiser = more wisdom left

            if (exhaust) {
                if (more_wisdom > 0 && chat_ids[j] == self.id && lastKTurnsSuccessful(k_turn, chat_ids[i])) {
                    System.out.println("EXHAUST: I, " + self.id + ", am talking to " + chat.id);
                    response = new Point(0, 0, chat.id);
                    return response;
                } else {
                    exhaust = false;
                }
            }

            //Say hello!
//            if (false) {
//            if (true) {
//            if (!exhaust) {
                // keep track of person who we haven't talked to and that person is talking to someone else
//            int jj = -1;
//            for (int ii = 0; ii < players.length; ii++) {
//                Point p = players[ii];
//                if (p.id == self_id) {
//                    continue;
//                }
//                if (people.get(p.id).wisdom != -1) {
//                    continue;
//                }
//                if (!Utils.inTalkRange(self, p)) {
//                    continue;
//                }
//
//                // TODO: see what to do if other person is talking to someone else, try to talk to them for 2 turns or move closer?
//                // if we don't talk to them, we might not find people to talk to
//                // if we try to talk to them, we might get stuck because they might keep talking for minutes and we will try talking to them without any gain
//                if (chat_ids[ii] != p.id) {
//                        jj = ii;
//                    continue;
//                }
//                System.out.println(self.id + " trying to saying hello to " + p.id);
//                response = new Point(0.0, 0.0, p.id);
//                exhaust = false;
//                return response;
//            }

//            if (jj != -1) {
//                // move closer to that person
//                Point result = moveCloserToPerson(people.get(self_id).cur_position, players[jj]);
//                talk_to_id = players[jj].id;
//                response = result;
//                return response;
//            }

            while(!maximum_wisdom_queue.isEmpty()) {
                Person person = maximum_wisdom_queue.poll();
                if(lastKTurnsSuccessful(k_turn, person.id)) {
                    response = new Point(0, 0, person.id);
                    exhaust = true;
                    return response;
                }
            }

            // otherwise move to a new location
            response = moveToANewLocation(players);
            return response;
        } finally {
            turns.get(tick).chat_id_tried = response.id;
        }
    }

    private boolean lastKTurnsSuccessful(int k, int chat_id_tried) {
        for(int i = 1; i <= k; i++) {
            if(!turns.containsKey(tick - i)) {
                return true;
            }
            Turn turn = turns.get(tick - i);
            if(turn.chat_id_tried != chat_id_tried) {
                return true;
            }
            if(turn.spoke && turn.wiser) {
                return true;
            }
        }
        return false;
    }

    //
	private Point moveToANewLocation(Point[] players) {
        Point self = people.get(self_id).cur_position;
        for(Point player: players) {
            double distance = Utils.distance(self, player);
            // if the person is not in talking range and has wisdom to offer, move to that person's location
            if(distance > 2 && distance <= 6 && people.get(player.id).wisdom != 0) {
                return moveCloserToPerson(self, player);
            }
        }
        // if no one found, move to a random position
        double dir = random.nextDouble() * 2 * Math.PI;
        double dx = 6 * Math.cos(dir);
        double dy = 6 * Math.sin(dir);
        return new Point(dx, dy, self_id);
    }

    private Point moveCloserToPerson(Point self, Point player) {
        double theta = Math.atan2(player.y - self.y, player.x - self.x);
        double distance = Utils.distance(self, player);
        double new_distance = distance - 0.52;
        double dx = Math.abs(new_distance * Math.sin(theta));
        double dy = Math.abs(new_distance * Math.cos(theta));
        if(player.x - self.x < 0) {
            dx = -dx;
        }
        if(player.y - self.y < 0) {
            dy = -dy;
        }
        return new Point(dx, dy, self_id);
    }

    private void updatePeopleAndQueue(Point[] players, int[] chat_ids) {
        maximum_wisdom_queue = new PriorityQueue<Person>(comparator);
        Set<Integer> visibleIds = new HashSet<Integer>();
        for (int ii = 0; ii < players.length; ii++) {
            Point player = players[ii];
            int id = player.id;
            visibleIds.add(id);
            Person person = people.get(id);
            person.setNewPosition(player);
            person.chat_id = chat_ids[ii];
            // if position of the person change, that person is moving
//            if (Utils.distance(person.prev_position, person.cur_position) != 0) {
//                person.setNewStatus(Status.moving);
//            } else {
//                // if not talking to himself, then talking to someone, otherwise just stayed there
//                if(player.id != chat_ids[i]) {
//                    person.setNewStatus(Status.talking);
//                } else {
//                    person.setNewStatus(Status.stayed);
//                }
//            }
        }
        Person self = people.get(self_id);
        for(Point player: players) {
            Person other = people.get(player.id);
            if(Utils.inTalkRange(self.cur_position, other.cur_position) && other.wisdom != 0) {
                maximum_wisdom_queue.add(people.get(player.id));
            }
        }
        for(Integer id: people.keySet()) {
            if(visibleIds.contains(id)) {
                continue;
            }
            people.get(id).chat_id = -1;
        }
    }

    public void debug_queue(PriorityQueue<Person> maximum_wisdom_queue)
    {
    	if (count++ == 100)
		{	System.out.println("----------------->"+count);
			while(!maximum_wisdom_queue.isEmpty()){
				Person p = maximum_wisdom_queue.poll();
				System.out.println(p.id+" | "+p.wisdom);}
			System.out.println("----------------->"+count);
		}
    }

}
