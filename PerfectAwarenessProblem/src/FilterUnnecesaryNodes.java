import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class FilterUnnecesaryNodes {
    Solution bestSolutionFound;
    public FilterUnnecesaryNodes(Solution sol, SpreadingProcessOptimize e) {
        bestSolutionFound = sol;
        boolean timeLimit = false;
        Instant initTime = Instant.now();
        Queue<Solution> qSols = new LinkedList<>();
        HashSet<BigInteger> visitedSolutions = new HashSet<>();
        qSols.add(sol);
        Solution bestSol = sol;
        int added = 0;
        while(!qSols.isEmpty() && !timeLimit) {
            Solution actSolution = qSols.poll();
            added = Math.max(0, added - 1);
            int maxQueueSize =  Math.min(actSolution.solutionValue(), (int) Math.ceil(actSolution.solutionValue() / (5*Math.log(actSolution.solutionValue()))));
            BigInteger nextPossible = actSolution.getBitwiseRepresentation();
            int index = nextPossible.getLowestSetBit();
            while(index!=-1 && !timeLimit) {
                if(Duration.between(initTime, Instant.now()).toMillis() >= 300000) {
                    timeLimit = true;
                    break;
                }
                BigInteger bwActSol = actSolution.getBitwiseRepresentation();
                BigInteger newSol = bwActSol.xor(BigInteger.ONE.shiftLeft(index));
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
                nextPossible = nextPossible.xor(BigInteger.ONE.shiftLeft(index));
                index = nextPossible.getLowestSetBit();
            }
        }
        bestSolutionFound = bestSol;
    }
}
