package wtr.g3;

import wtr.sim.Point;

import java.util.*;

public class Player implements wtr.sim.Player {

    public static final int MAX_RETRIES = 5;

    // your own id
    private int self_id = -1;


    // the remaining wisdom per player
    private int[] wisdom = null;

    // random generator
    private Random random = new Random();

    // previous move
    Point prevMove = null;
    int numberOfRetries = 0;

    // init function called once
    public void init(int id, int[] friend_ids, int strangers) {
        self_id = id;
        // initialize the wisdom array
        int N = friend_ids.length + strangers + 2;
        wisdom = new int[N];
        for (int i = 0; i != N; ++i)
            wisdom[i] = i == self_id ? 0 : -1;
        for (int friend_id : friend_ids)
            wisdom[friend_id] = 50;
    }

    // play function
    public Point play(Point[] players, int[] chat_ids,
                      boolean wiser, int more_wisdom) {
        // find where you are and who you chat with
        int i = 0, j = 0;
        while (players[i].id != self_id) i++;
        while (players[j].id != chat_ids[i]) j++;
        Point self = players[i];
        Point chat = players[j];

        // record known wisdom
        wisdom[chat.id] = more_wisdom;

        // attempt to continue chatting if there is more wisdom
        if (wiser) {
            return new Point(0.0, 0.0, chat.id);
        }

        if (prevMove != null && numberOfRetries < MAX_RETRIES) {
            if (playerIsWithinTalkingDistance(prevMove.id, players, self)) {
                numberOfRetries++;
                return prevMove;
            }
        }

        prevMove = null;
        numberOfRetries = 0;
        if (i == j) {
            Arrays.sort(players, new Comparator<Point>() {
                @Override
                public int compare(Point player1, Point player2) {
                    double player1distance = squareDistance(player1, self);
                    double player2distance = squareDistance(player2, self);

                    return player1distance < player2distance ? -1 : 1;
                }
            });
            for(Point p : players){
                if(wisdom[p.id] != 0 && isWithinTalkingDitance(squareDistance(self, p))){
                    numberOfRetries = 0;
                    prevMove = new Point(0.0, 0.0, p.id);
                    return prevMove;
                }
            }
        }
        // return a random move
        double dir = random.nextDouble() * 2 * Math.PI;
        double dx = 6 * Math.cos(dir);
        double dy = 6 * Math.sin(dir);
        prevMove = null;
        return new Point(dx, dy, self_id);

    }

    public ArrayList<Point> sortPlayers(Point[] players, Point self) {
        ArrayList<Point> playersWithWisdom = new ArrayList<>();
        for (Point p : players) {
            if (wisdom[p.id] != 0) {
                playersWithWisdom.add(p);
            }
        }

        Collections.sort(playersWithWisdom, new Comparator<Point>() {
            @Override
            public int compare(Point player1, Point player2) {
                double player1distance = squareDistance(player1, self);
                double player2distance = squareDistance(player2, self);

                return player1distance < player2distance ? -1 : 1;
            }
        });

        return playersWithWisdom;
        /*Arrays.sort(players, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {
                if(p1 == self) return 1;
                if(p2 == self) return -1;

                if(wisdom[p1.id] == 0 && wisdom[p2.id] == 0){
                    return 0;
                } else if(wisdom[p1.id] == 0 && wisdom[p2.id] != 0){
                    return 1;
                } else if(wisdom[p1.id] != 0 && wisdom[p2.id] == 0){
                    return -1;
                }

                double p1dist = squareDistance(self, p1);
                double p2dist = squareDistance(self, p2);

                if(isWithinTalkingDitance(p1dist) && !isWithinTalkingDitance(p2dist)){
                    return -1;
                } else if(!isWithinTalkingDitance(p1dist) && isWithinTalkingDitance(p2dist)){
                    return 1;
                } else if (!isWithinTalkingDitance(p1dist) && !isWithinTalkingDitance(p2dist)){
                    return 0;
                }

                if(wisdom[p1.id] == -1 && wisdom[p1.id] != -1){
                    return -1;
                } else if(wisdom[p1.id] != -1 && wisdom[p2.id] == -1){
                    return 1;
                } else {
                    return wisdom[p1.id] - wisdom[p2.id];
                }

            }
        });*/
    }

    public boolean playerIsWithinTalkingDistance(int id, Point[] players, Point self) {
        for (Point player : players) {
            if (player.id == id && isWithinTalkingDitance(squareDistance(self, player)))
                return true;
        }
        return false;
    }

    public boolean isWithinTalkingDitance(double distance) {
        return distance >= 0.25 && distance <= 4.0;
    }

    public double computeAverageWisdom(int[] wisdoms) {
        int total = 0;
        int denom = 0;
        for (int wisdom : wisdoms) {
            if (wisdom > 0) {
                total += wisdom;
                denom++;
            }
        }

        return denom > 0 ? total / denom : 0;
    }

    public double distance(Point a, Point b) {
        return Math.sqrt(squareDistance(a, b));
    }

    public double squareDistance(Point a, Point b) {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
    }

    public double angle(Point origin, Point target) {
        return Math.atan2(target.y - origin.y, target.x - origin.x);
    }
}
