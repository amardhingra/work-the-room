package wtr.g2;

import wtr.sim.Point;
import java.util.Comparator;

/**
 * Comparator for making pq a min-heap
 */
public class TargetComparator implements Comparator<Point> {

    Point self;
    public TargetComparator(Point self) {
<<<<<<< HEAD
        this.self = new Point(self.x, self.y, self.id);
=======
        this.self = self;
>>>>>>> origin/master
    }

    @Override
    public int compare(Point o1, Point o2) {
        double o1Dist = Utils.dist(self, o1);
        double o2Dist = Utils.dist(self, o2);
<<<<<<< HEAD
        if (o1Dist > o2Dist)
            return 1;
        else if (o1Dist < o2Dist)
=======
        if (o1Dist < o2Dist)
            return 1;
        else if (o1Dist > o2Dist)
>>>>>>> origin/master
            return -1;
        return 0;
    }
}