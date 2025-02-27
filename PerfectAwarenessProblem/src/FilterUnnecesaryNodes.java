import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class FilterUnnecesaryNodes {
    Solution bestSolutionFound;
    public FilterUnnecesaryNodes(Solution sol, SpreadingProcess e) {
        bestSolutionFound = sol;
        Queue<Solution> qSols = new LinkedList<>();
        // Método optimizable
        HashSet<BigInteger> visitedSolutions = new HashSet<>();
        qSols.add(sol);
        Solution bestSol = sol;
        int added = 0;
        while(!qSols.isEmpty()) {
            Solution actSolution = qSols.poll();
            added = Math.max(0, added - 1);
            int maxQueueSize = Math.min(actSolution.solutionValue(), (int) Math.ceil(actSolution.solutionValue() / (5*Math.log(actSolution.solutionValue()))));
            ArrayList<Integer> actArrSol = actSolution.getSolution();
            for(int i = 0; i < actArrSol.size(); i++) {
                BigInteger bwActSol = actSolution.getBitwiseRepresentation();
                BigInteger newSol = bwActSol.xor(BigInteger.ONE.shiftLeft(actArrSol.get(i)));
                Solution newPossibleSolution = new Solution(newSol);
                if(e.isSolution(newPossibleSolution)) {
                    if(added < maxQueueSize) {
                        if (!visitedSolutions.contains(newSol)) {
                            visitedSolutions.add(newSol);
                            qSols.add(newPossibleSolution);
                            added++;
                            if (newPossibleSolution.solutionValue() < bestSol.solutionValue()) {
                                bestSol = newPossibleSolution;
                            }
                        }
                    }
                    else break;
                }
            }
        }
        bestSolutionFound = bestSol;
    }
}
