package wtr.g3;

import wtr.sim.Point;

import java.util.*;

public class Player implements wtr.sim.Player {

    // your own id
    private int self_id = -1;

    // the remaining wisdom per player
    private int[] W = null;

    // random generator
    private Random random = new Random();

    // init function called once
    public void init(int id, int[] friend_ids, int strangers) {
        self_id = id;
        // initialize the wisdom array
        int N = friend_ids.length + strangers + 2;
        W = new int[N];
        for (int i = 0; i != N; ++i)
            W[i] = i == self_id ? 0 : -1;
        for (int friend_id : friend_ids)
            W[friend_id] = 50;
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
        W[chat.id] = more_wisdom;

        // attempt to continue chatting if there is more wisdom
        if (wiser) {
            return new Point(0.0, 0.0, chat.id);
        }

        double averageWisdom = 0;//computeAverageWisdom(W);

        Point target = null;
        int targetId = this.self_id;
        double targetDistance = Double.MAX_VALUE;
        int targetIndex = -1;

        if (i == j) {

            sortPlayers(players, self);
            target = players[0];
            targetDistance = squareDistance(target, self);
            if (targetDistance >= 0.25 && targetDistance <= 4.0) {
                return new Point(0.0, 0.0, target.id);
            }

        }
        // return a random move
        double dir = random.nextDouble() * 2 * Math.PI;
        double dx = 6 * Math.cos(dir);
        double dy = 6 * Math.sin(dir);
        return new Point(dx, dy, self_id);
/*
for (int k = 0; k < players.length; k++) {
                if (W[players[k].id] < 0 || W[players[k].id] > averageWisdom) {
                    if (squareDistance(self, players[k]) < targetDistance &&
                            squareDistance(self, players[k]) > 0) {
                        targetDistance = squareDistance(self, players[k]);
                        targetId = players[k].id;
                        targetIndex = k;
                    }
                }
            }
 */

//        if(targetIndex == -1){
//
//        }
//        if (targetDistance >= 0.25 && targetDistance <= 4.0) {
//            //System.out.println(self_id + " trying to talk to " + targetId);
//            return new Point(0.0, 0.0, targetId);
//        }
//        else {
//            double theta = angle(self, players[targetIndex]);
//            //System.out.println(self_id + " moving to " + players[targetIndex].id + " " + theta);
//            return new Point((Math.sqrt(targetDistance) - 0.5) * Math.cos(theta), (Math.sqrt(targetDistance) - 0.5) * Math.sin(theta), self_id);
//        }
    }

    public void sortPlayers(Point[] players, Point self) {
        Arrays.sort(players, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {
                if(p1 == self) return 1;
                if(p2 == self) return -1;

                if(W[p1.id] == 0 && W[p2.id] == 0){
                    return 0;
                } else if(W[p1.id] == 0 && W[p2.id] != 0){
                    return 1;
                } else if(W[p1.id] != 0 && W[p2.id] == 0){
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

                if(W[p1.id] == -1 && W[p1.id] != -1){
                    return -1;
                } else if(W[p1.id] != -1 && W[p2.id] == -1){
                    return 1;
                } else {
                    return W[p1.id] - W[p2.id];
                }

            }
        });
    }


    public boolean isWithinTalkingDitance(double distance){
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
