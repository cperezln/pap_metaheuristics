import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class FilterUnnecesaryNodes {
    Solution bestSolutionFound;
    public FilterUnnecesaryNodes(Solution sol, SpreadingProcessOptimize e) {
        bestSolutionFound = sol;
        Queue<Solution> qSols = new LinkedList<>();
        HashSet<BitSet> visitedSolutions = new HashSet<>();
        qSols.add(sol);
        Solution bestSol = sol;
        int added = 0;
        while(!qSols.isEmpty()) {
            Solution actSolution = qSols.poll();
            added = Math.max(0, added - 1);
            int maxQueueSize =  Math.min(actSolution.solutionValue(), (int) Math.ceil(actSolution.solutionValue() / (5*Math.log(actSolution.solutionValue()))));
            BitSet bwActSol = actSolution.getBitwiseRepresentation();
            for (int index = bwActSol.nextSetBit(0); index >= 0; index = bwActSol.nextSetBit(index + 1)) {
                BitSet newSol = (BitSet) bwActSol.clone();
                newSol.clear(index);
                Solution newPossibleSolution = new Solution(newSol);
                Solution.instance.resetState(newPossibleSolution);
                if(e.isSolution(newPossibleSolution)) {
                    if(added <= maxQueueSize) {
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
