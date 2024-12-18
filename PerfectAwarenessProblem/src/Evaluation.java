import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class Evaluation {

    private Instance instance;

    public Evaluation(Instance i) {
        this.instance = i;
    }

    public boolean isSolution(Solution sol) {
        int tau = 0;
        // We don't need to know the round we become aware/spreaders. If so, we can compute it just adding a null every time we insert every new neighbor in the queue
        Queue<Integer> qSpreaders = new LinkedList<>(sol.getSolution());
        HashSet<Integer> spreadersTau = new HashSet<>();
        HashSet<Integer> spreadersTaup = new HashSet<>(sol.getSolution());
        HashSet<Integer> aware = new HashSet<>(sol.getSolution());
        HashSet<Integer> visited = new HashSet<>();
        while(spreadersTau != spreadersTaup && qSpreaders.size() > 0) {
            spreadersTau.addAll(spreadersTaup);
            int node = qSpreaders.poll();
            if(!visited.contains(node)) {
                visited.add(node);
                for (Integer i : instance.graph.get(node)) {
                    // Become aware if your neigbor
                    if(!aware.contains(i)) aware.add(i);
                    if(!spreadersTaup.contains(i)) {
                        int countSpreader = 0;
                        for (Integer j : instance.graph.get(i)) {
                            if (spreadersTau.contains(j)) countSpreader += 1;
                        }
                        // Become spreader if the number of spreader neighbors is greater than threshold (which is 0.5 * d(node))
                        if (countSpreader >= instance.graph.get(i).size() * 0.5) {
                            spreadersTaup.add(i);
                            qSpreaders.add(i);
                        }
                    }
                }
            }
        }
        return aware.size() == instance.getNumberNodes();
    }


}
