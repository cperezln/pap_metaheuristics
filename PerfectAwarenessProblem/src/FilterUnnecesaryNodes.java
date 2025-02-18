import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class FilterUnnecesaryNodes {
    Solution bestSolutionFound;
    public FilterUnnecesaryNodes(Solution sol, Evaluation e) {
        Queue<Solution> qSols = new LinkedList<>();
        // Método optimizable
        qSols.add(sol);
        HashSet<Solution> visitedSolutions = new HashSet<>();
        Solution bestSol = sol;
        while(!qSols.isEmpty()) {
            Solution actSolution = qSols.poll();
            int maxQueueSize = Math.min(actSolution.solutionValue(), (int) Math.ceil(actSolution.solutionValue() / Math.log(actSolution.solutionValue())));
            int added = 0;
            ArrayList actArrSol = actSolution.getSolution();
            for(int i = 0; i < actArrSol.size(); i++) {
                ArrayList<Integer> newArrSol = new ArrayList<>();
                for(int j = 0; j < actArrSol.size(); j++) {
                    if(i != j) newArrSol.add((Integer) actArrSol.get(j));
                }
                Solution newPossibleSolution = new Solution(newArrSol);
                if(e.isSolution(newPossibleSolution)) {
                    if(added < maxQueueSize) {
                        if(visitedSolutions.contains(newPossibleSolution)) {
                            qSols.add(newPossibleSolution);
                        }
                        added++;
                        if (newPossibleSolution.solutionValue() < bestSol.solutionValue()) {
                            bestSol = newPossibleSolution;
                        }
                    }
                    else break;
                }
            }
        }
        bestSolutionFound = bestSol;
    }
}
