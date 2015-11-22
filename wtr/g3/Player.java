package wtr.g3;

import wtr.sim.Point;

import java.util.Random;

public class Player implements wtr.sim.Player {

  // your own id
  private int self_id = -1;

  // the remaining wisdom per player
  private int[] W = null;

  // random generator
  private Random random = new Random();

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

  // play function
  public Point play(Point[] players, int[] chat_ids,
      boolean wiser, int more_wisdom)
  {
    // find where you are and who you chat with
    int i = 0, j = 0;
    while (players[i].id != self_id) i++;
    while (players[j].id != chat_ids[i]) j++;
    Point self = players[i];
    Point chat = players[j];
    // record known wisdom
    W[chat.id] = more_wisdom;

    double averageWisdom = computeAverageWisdom(W);
    int targetId = id;
    double targetDistance = Double.MAX_VALUE;
    for (int k = 0; k < players.length; k++) {
      if (W[players[k].id] == 0 || W[players[k].id] > averageWisdom) {
        if (squareDistance(self, players[k]) < targetDistance && squareDistance(self, players[k]) > 0) {
          distance = squareDistance(self, players[k]);
          id = players[k].id;
        }
      }
    }

    if (dd >= 0.25 && dd <= 4.0)
      return new Point(0.0, 0.0, p.id);
    else {
      double theta = angle(self, points[targetId]);
      return new Point(6 * Math.cos(theta), 6 * Math.sin(theta), 0);
    }
  }
}

public double computeAverageWisdom(int[] wisdoms) {
  int total = 0;
  for (int wisdom : wisdoms) {
    if (wisdom > 0) {
      total += wisdom;
    }
  }
  return total / wisdoms.length;
}

public double squareDistance(Point a, Point b) {
  return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
}

public double angle(Point origin, Point target) {
  return Math.atan2(target.y - origin.y, target.x - origin.x);
}
