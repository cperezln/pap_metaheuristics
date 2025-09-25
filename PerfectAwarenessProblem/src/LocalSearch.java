import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.ArrayList;

public class LocalSearch {
    Solution bestSolutionFound;
    private long timeLimitMs;

    public LocalSearch(Solution solution, SpreadingProcessOptimize e, long timeLimitMs, Instant startTime) {
        this.timeLimitMs = timeLimitMs;
        // Definimos la búsqueda local con el esquema habitual
        this.bestSolutionFound = solution;
        boolean improved = true;
        Instant initTime = Instant.now();
        int iterations = 0;
        int totalNeighborsEvaluated = 0;
        int feasibleNeighbors = 0;
        int infeasibleNeighbors = 0;
        int initialValue = solution.solutionValue();

        //System.out.println("LocalSearch: Starting with solution value: " + initialValue);

        while (improved) {
            iterations++;
            // Implementamos el movimiento de la solución. En este caso, es un swap de dos nodos de la solución por un nodo que no esté en la misma.
            this.bestSolutionFound = new FilterUnnecesaryNodes(this.bestSolutionFound, e).bestSolutionFound;
            solution = this.bestSolutionFound;
            improved = false;
            LinkedList<Integer> nodesNotInSolution = this.bestSolutionFound.nodesNotInSolution();
            //System.out.println("LocalSearch: Iteration " + iterations + " - Current value: " + this.bestSolutionFound.solutionValue() + ", Nodes to try: " + nodesNotInSolution.size());

            if (nodesNotInSolution.isEmpty()) {
                //System.out.println("LocalSearch: No nodes outside solution, stopping.");
                break;
            }

            for (Integer node : nodesNotInSolution) {
                if(improved) break;
                long currentTime = Instant.now().toEpochMilli();
                if(currentTime - initTime.toEpochMilli() >= this.timeLimitMs || Duration.between(startTime, Instant.now()).toMillis() > TestRunner.TIME_LIMIT_MS) {
                    //System.out.println("LocalSearch: Time limit reached after " + (currentTime - initTime.toEpochMilli()) + "ms");
                    return; // Exit constructor early if time limit reached
                }
                // El intercambio consiste en coger uno de los nodos que no está en la solución, e intentar intercambiarlo por un par de los nodos
                // que sí que están.
                int iter = 0;

                // Create efficient list of nodes in solution to avoid BigInteger operations
                ArrayList<Integer> nodesInSolution = new ArrayList<>();
                BigInteger solutionBits = solution.getBitwiseRepresentation();
                for (int i = 0; i < Solution.instance.getNumberNodes(); i++) {
                    if (solutionBits.testBit(i)) {
                        nodesInSolution.add(i);
                    }
                }
                // Shuffle for more diversity in local search
                java.util.Collections.shuffle(nodesInSolution);

                // Efficient nested loop over nodes in solution
                for (int i = 0; i < nodesInSolution.size() && !improved; i++) {
                    int indexI = nodesInSolution.get(i);
                    for (int j = i + 1; j < nodesInSolution.size() && !improved; j++) {
                        int indexJ = nodesInSolution.get(j);
                        iter++;

                        // Perform swap: remove indexI and indexJ, add node
                        BigInteger bwSol = solutionBits
                            .clearBit(indexI)
                            .clearBit(indexJ)
                            .setBit(node);

                        Solution finalSol = new Solution(bwSol);
                        Solution.instance.resetState(finalSol);
                        totalNeighborsEvaluated++;
                        if(e.isSolution(finalSol)) {
                            feasibleNeighbors++;
                            Solution neighbor = new FilterUnnecesaryNodes(finalSol, e).bestSolutionFound;
                            if (neighbor.solutionValue() < this.bestSolutionFound.solutionValue()) {
                                int improvement = this.bestSolutionFound.solutionValue() - neighbor.solutionValue();
                                //System.out.println("LocalSearch: Improvement found! " + this.bestSolutionFound.solutionValue() + " -> " + neighbor.solutionValue() + " (gain: " + improvement + ") after " + totalNeighborsEvaluated + " evaluations");
                                this.bestSolutionFound = neighbor;
                                improved = true;
                            }
                        } else {
                            infeasibleNeighbors++;
                        }
                        if(Duration.between(startTime, Instant.now()).toMillis() > TestRunner.TIME_LIMIT_MS) {
                            return;
                        }
                    }
                }
            }
        }

        int finalValue = this.bestSolutionFound.solutionValue();
        long totalTime = Instant.now().toEpochMilli() - initTime.toEpochMilli();
        //System.out.println("LocalSearch: Completed in " + iterations + " iterations, " + totalNeighborsEvaluated + " neighbor evaluations, " + totalTime + "ms");
        //System.out.println("LocalSearch: Feasible: " + feasibleNeighbors + ", Infeasible: " + infeasibleNeighbors + " (" + String.format("%.1f", (100.0 * feasibleNeighbors / Math.max(1, totalNeighborsEvaluated))) + "% feasible)");
        //System.out.println("LocalSearch: Final improvement: " + initialValue + " -> " + finalValue + " (total gain: " + (initialValue - finalValue) + ")");

        /*if (totalNeighborsEvaluated == 0) {
            System.out.println("LocalSearch: WARNING - No neighbors were evaluated! Check if solution is already minimal or movement generation is broken.");
        }
        if (feasibleNeighbors == 0 && totalNeighborsEvaluated > 0) {
            System.out.println("LocalSearch: WARNING - No feasible neighbors found! All generated moves are infeasible.");
        }*/
    }
}