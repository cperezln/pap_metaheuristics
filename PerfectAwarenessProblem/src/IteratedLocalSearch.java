import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;

public class IteratedLocalSearch {
    private double alphaValue;
    private Instance instance;
    private SpreadingProcessOptimize eval;
    private Random random;
    private long timeToBestMs = -1; // Time when best solution was found
    private boolean useBridgeSwap = true; // Toggle between perturbation methods
    private PrintWriter timeToBestWriter,contributionWriter; // Writer for time to best output
    // Centrality factors for thread-safe solution creation
    private double betFactor;
    private double degFactor;
    private double eigFactor;

    public IteratedLocalSearch(double alphaValue, Instance instance, SpreadingProcessOptimize eval, double betFactor, double degFactor, double eigFactor) {
        this.alphaValue = alphaValue;
        this.instance = instance;
        this.eval = eval;
        this.random = new Random();
        this.timeToBestMs = -1;
        this.betFactor = betFactor;
        this.degFactor = degFactor;
        this.eigFactor = eigFactor;
    }

    public long getTimeToBestMs() {
        return timeToBestMs;
    }

    private void addNodeFromCandidateList(Solution solution) {
        ArrayList<PairVal> candidateList = solution.candidateList();
        if (candidateList.isEmpty()) return;

        ArrayList<Integer> restCandidateList = new ArrayList<>();
        double rclThresh = solution.minVal + alphaValue * (solution.maxVal - solution.minVal);

        for (PairVal pv : candidateList) {
            if (pv.val >= rclThresh) {
                restCandidateList.add(pv.node);
            }
        }

        if (!restCandidateList.isEmpty()) {
            int selectRand = random.nextInt(restCandidateList.size());
            try {
                solution.addNode(restCandidateList.get(selectRand));
                instance.resetState(solution);
            } catch (Exception e) {
                System.out.println("Error adding node: " + e.getMessage());
            }
        }
    }

    /**
     * Add a random node to the solution instead of using candidate list
     * This provides more diversification during perturbation
     */
    private void addRandomNode(Solution solution) {
        // Get nodes not currently in the solution
        ArrayList<Integer> nodesNotInSolution = new ArrayList<>();
        for (int i = 0; i < instance.getNumberNodes(); i++) {
            if (!solution.isIn(i)) {
                nodesNotInSolution.add(i);
            }
        }

        if (nodesNotInSolution.isEmpty()) {
            System.out.println("ILS: Warning - No nodes available to add!");
            return;
        }

        // Select a completely random node
        int randomIndex = random.nextInt(nodesNotInSolution.size());
        int nodeToAdd = nodesNotInSolution.get(randomIndex);

        try {
            solution.addNode(nodeToAdd);
            instance.resetState(solution);
        } catch (Exception e) {
            System.out.println("Error adding random node " + nodeToAdd + ": " + e.getMessage());
        }
    }

    public Solution constructivePhase() {
        Solution solution = new Solution();
        while (!eval.isSolution(solution) || solution.solutionValue()==0) {
            addRandomNode(solution);
        }
        System.out.println("ILS: Constructive phase completed with value: " + solution.solutionValue());
        return solution;
    }

    public Solution localSearch(Solution solution, Instant startTime) {
        solution.removeUnnedeed();
        Solution improvedSol = solution;
        int initialValue = improvedSol.solutionValue();

        if (improvedSol.solutionValue() >= 2) {
            Solution filteredSol = new FilterUnnecesaryNodes(improvedSol, eval).bestSolutionFound;
            LocalSearch ls = new LocalSearch(filteredSol, eval, TestRunner.LOCAL_SEARCH_TIME_LIMIT_MS,startTime);
            improvedSol = ls.bestSolutionFound;
            improvedSol.removeUnnedeed();
        }

        int finalValue = improvedSol.solutionValue();
        //System.out.println("ILS: Local search: " + initialValue + " -> " + finalValue + " (improvement: " + (initialValue - finalValue) + ")");
        return improvedSol;
    }

    public Solution perturbation(Solution solution) {
        Instant perturbationStart = Instant.now();
        Solution perturbedSol = new Solution(solution.getBitwiseRepresentation());

        // Obtener nodos en la solución de forma más eficiente
        ArrayList<Integer> nodesInSolution = new ArrayList<>();
        for (int i = 0; i < instance.getNumberNodes(); i++) {
            if (perturbedSol.isIn(i)) {
                nodesInSolution.add(i);
            }
        }
        if (nodesInSolution.size()==0)
            return perturbedSol;
        //System.out.println("ILS: Nodes in solution: " + nodesInSolution.size());
        int initialSize = perturbedSol.solutionValue();

        // Remover nodos aleatoriamente - optimizado para evitar reindexación
        int nodesToRemove = 1 + random.nextInt(nodesInSolution.size());
        //System.out.println("ILS: Removing: " + nodesToRemove + " nodes from solution of size " + initialSize);

        for (int i = 0; i < nodesToRemove; i++) {
            int randomIndex = random.nextInt(nodesInSolution.size());
            int nodeToRemove = nodesInSolution.get(randomIndex);
            perturbedSol.removeNodeForPerturbation(nodeToRemove);
            // Swap and pop para evitar shifting en ArrayList
            nodesInSolution.set(randomIndex, nodesInSolution.get(nodesInSolution.size() - 1));
            nodesInSolution.remove(nodesInSolution.size() - 1);
        }

        int finalSize = perturbedSol.solutionValue();
        //System.out.println("ILS: Perturbation - After removal: " + initialSize + " -> " + finalSize + " (removed " + (initialSize - finalSize) + " nodes)");

        // Reconstruir la solución añadiendo nodos aleatorios
        instance.resetState(perturbedSol);
        while (!eval.isSolution(perturbedSol)) {
            // Check time limit - if exceeded, return original solution
            if (Duration.between(perturbationStart, Instant.now()).toMillis() > TestRunner.TIME_LIMIT_MS) {
                //System.out.println("ILS: Perturbation timeout - returning original solution");
                return solution;
            }
            addRandomNode(perturbedSol);
        }

        //System.out.println("ILS: Perturbation completed - Final value: " + perturbedSol.solutionValue());
        return perturbedSol;
    }

    /**
     * Alternative perturbation method: Bridge Swap Perturbation
     * Based on graph connectivity and centrality measures
     * More effective for coverage problems like PAP
     */
    public Solution bridgeSwapPerturbation(Solution solution) {
        Solution perturbedSol = new Solution(solution.getBitwiseRepresentation());
        System.out.println("ILS: Bridge Swap Perturbation - Initial solution value: " + solution.solutionValue());

        // Get nodes currently in solution
        ArrayList<Integer> nodesInSolution = new ArrayList<>();
        for (int i = 0; i < instance.getNumberNodes(); i++) {
            if (perturbedSol.isIn(i)) {
                nodesInSolution.add(i);
            }
        }

        if (nodesInSolution.size() <= 1) {
            return perturbedSol; // Can't perturb with only one node
        }

        // Calculate perturbation strength (25-50% of solution size)
        int perturbationStrength = Math.max(1,
            (int)(nodesInSolution.size() * (0.25 + random.nextDouble() * 0.25)));

        // Phase 1: Remove nodes with lowest "criticality" (bridge importance)
        ArrayList<NodeCriticality> nodeCriticality = new ArrayList<>();
        for (Integer node : nodesInSolution) {
            double criticality = calculateNodeCriticality(node, perturbedSol);
            nodeCriticality.add(new NodeCriticality(node, criticality));
        }

        // Sort by criticality (ascending - remove least critical first)
        nodeCriticality.sort((a, b) -> Double.compare(a.criticality, b.criticality));

        // Remove least critical nodes
        for (int i = 0; i < Math.min(perturbationStrength, nodeCriticality.size() - 1); i++) {
            int nodeToRemove = nodeCriticality.get(i).node;
            perturbedSol.removeNodeForPerturbation(nodeToRemove);
        }

        // Phase 2: Add random nodes for diversification
        instance.resetState(perturbedSol);
        Instant reconstructionStart = Instant.now();
        while (!eval.isSolution(perturbedSol)) {
            // Check time limit - if exceeded, return original solution
            if (Duration.between(reconstructionStart, Instant.now()).toMillis() > TestRunner.TIME_LIMIT_MS) {
                //System.out.println("ILS: Bridge Swap Perturbation timeout - returning original solution");
                return solution;
            }
            addRandomNode(perturbedSol);
        }

        System.out.println("ILS: Bridge Swap Perturbation completed - Final value: " + perturbedSol.solutionValue());
        return perturbedSol;
    }

    /**
     * Calculate node criticality based on its connectivity and coverage contribution
     */
    private double calculateNodeCriticality(int node, Solution solution) {
        // Factors that make a node critical:
        // 1. Node degree (connectivity)
        // 2. Number of unique neighbors it covers
        // 3. Centrality measures if available

        double degree = instance.graph.get(node).size();

        // Count unique coverage contribution
        int uniqueCoverage = 0;
        for (Integer neighbor : instance.graph.get(node)) {
            boolean coveredByOthers = false;
            for (int i = 0; i < instance.getNumberNodes(); i++) {
                if (i != node && solution.isIn(i)) {
                    if (instance.graph.get(i).contains(neighbor)) {
                        coveredByOthers = true;
                        break;
                    }
                }
            }
            if (!coveredByOthers) {
                uniqueCoverage++;
            }
        }

        // Criticality = weighted combination of factors
        // Higher unique coverage = higher criticality (less likely to be removed)
        return degree * 0.3 + uniqueCoverage * 0.7;
    }

    /**
     * Add the node that maximizes coverage improvement
     */
    private void addBestCoverageNode(Solution solution) {
        ArrayList<PairVal> candidateList = solution.candidateList();
        if (candidateList.isEmpty()) return;

        // Find node with highest coverage value
        PairVal bestCandidate = candidateList.get(0);
        for (PairVal candidate : candidateList) {
            if (candidate.val > bestCandidate.val) {
                bestCandidate = candidate;
            }
        }

        try {
            solution.addNode(bestCandidate.node);
            instance.resetState(solution);
        } catch (Exception e) {
            System.out.println("Error adding best coverage node: " + e.getMessage());
        }
    }

    /**
     * Helper class to store node criticality information
     */
    private static class NodeCriticality {
        int node;
        double criticality;

        NodeCriticality(int node, double criticality) {
            this.node = node;
            this.criticality = criticality;
        }
    }

    public Solution run() {
        Instant startTime = Instant.now();

        // Initialize PrintWriter for time to best output
        String instanceName = instance.name;
        // Create time_to_best directory if it doesn't exist
        java.io.File timeToBestDir = new java.io.File("time_to_best");
        if (!timeToBestDir.exists()) {
            timeToBestDir.mkdirs();
        }
        String timeToBestFileName = "time_to_best/time_to_best_" + instanceName + ".txt";
        String methodContributionName = "time_to_best/contribution_" + instanceName + ".txt";
        try {
            timeToBestWriter = new PrintWriter(new FileWriter(timeToBestFileName));
            timeToBestWriter.println("Time_to_Best(ms)\tSolution_Value\tIteration");
        } catch (IOException e) {
            System.err.println("Error creating time to best file: " + e.getMessage());
            timeToBestWriter = null;
        }

        // Generar solución inicial
        System.out.println("ILS: Starting initial construction and local search...");
        Solution bestSolution = new Solution();
        if(instance.graph.size()==0) {
            bestSolution.addNodeUnique(instance.getUniqueNode());
            // Record initial solution as first improvement
            long initialTimeToBest = Duration.between(startTime, Instant.now()).toMillis();
            timeToBestMs = initialTimeToBest;
            timeToBestWriter.println(initialTimeToBest + "\t" + 1 + "\t0");
            timeToBestWriter.flush();
            try {
                contributionWriter = new PrintWriter(new FileWriter(methodContributionName));
                contributionWriter.println("Method\tSolution_Value");
                contributionWriter.println("1\t1\t1");
                contributionWriter.flush();
                contributionWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            Solution currentSolution = constructivePhase();
            int constructiveOF = currentSolution.solutionValue();
            currentSolution = localSearch(currentSolution,startTime);
            int lsOF = currentSolution.solutionValue();
            bestSolution = new Solution(currentSolution.getBitwiseRepresentation());
            int bestValue = bestSolution.solutionValue();

            // Record initial solution as first improvement
            long initialTimeToBest = Duration.between(startTime, Instant.now()).toMillis();
            timeToBestMs = initialTimeToBest;
            timeToBestWriter.println(initialTimeToBest + "\t" + bestValue + "\t0");
            timeToBestWriter.flush();
            System.out.println("ILS: Initial time to best: " + initialTimeToBest + " ms");

            System.out.println(String.format("ILS: Initial solution value: %d", bestValue));

            int iteration = 0;
            int iterationsWithoutImprovement = 0;
            final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 50000;

            while (Duration.between(startTime, Instant.now()).toMillis() < TestRunner.TIME_LIMIT_MS) {
                iteration++;

                // Perturbación - alternar entre métodos
                Solution perturbedSolution;
                //if (useBridgeSwap && iteration % 3 == 0) { // Use bridge swap every 3rd iteration
                //    perturbedSolution = bridgeSwapPerturbation(currentSolution);
                //} else {
                    perturbedSolution = perturbation(currentSolution);
                //}
                //System.out.println("ILS: Iteration " + iteration + " - After perturbation: " + perturbedSolution.solutionValue());

                // Búsqueda local
                Solution localOptimum = localSearch(perturbedSolution,startTime);

                // Criterio de aceptación: aceptar si es mejor o igual
                if (localOptimum.solutionValue() <= currentSolution.solutionValue()) {
                    currentSolution = localOptimum;
                    iterationsWithoutImprovement = 0;

                    // Actualizar mejor solución si es necesario
                    if (localOptimum.solutionValue() < bestValue) {
                        bestSolution = new Solution(localOptimum.getBitwiseRepresentation());
                        bestValue = bestSolution.solutionValue();

                        // Record time to best for this improvement
                        long currentTimeToBest = Duration.between(startTime, Instant.now()).toMillis();
                        timeToBestMs = currentTimeToBest;

                        timeToBestWriter.println(currentTimeToBest + "\t" + bestValue + "\t" + iteration);
                        timeToBestWriter.flush();


                        System.out.println("ILS: New best solution found at iteration " + iteration + " with value: " + bestValue);
                        System.out.println("ILS: Time to best: " + currentTimeToBest + " ms");
                    }
                } else {
                    iterationsWithoutImprovement++;
                }

                // Reinicio si hay demasiadas iteraciones sin mejora
                if (iterationsWithoutImprovement >= MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                    //System.out.println(String.format("ILS: Restart at iteration %d - reconstructing solution...", iteration));
                    currentSolution = constructivePhase();
                    currentSolution = localSearch(currentSolution,startTime);
                    iterationsWithoutImprovement = 0;
                    //System.out.println(String.format("ILS: After restart, current solution value: %d", currentSolution.solutionValue()));
                }
            }
            long totalTime = Duration.between(startTime, Instant.now()).toMillis();
            System.out.println(String.format("ILS: Completed %d iterations in %d ms", iteration, totalTime));
            System.out.println(String.format("ILS: Best solution value: %d", bestValue));

            try {
                contributionWriter = new PrintWriter(new FileWriter(methodContributionName));
                contributionWriter.println("Method\tSolution_Value");
                contributionWriter.println(constructiveOF + "\t" + lsOF + "\t" + bestValue);
                contributionWriter.flush();
                contributionWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        timeToBestWriter.close();

        // Verify solution feasibility using SpreadingProcessCheck
        SpreadingProcessCheck checker = new SpreadingProcessCheck(instance);
        ArrayList<Integer> solutionNodes = new ArrayList<>();
        for (int i = 0; i < instance.getNumberNodes(); i++) {
            if (bestSolution.isIn(i)) {
                solutionNodes.add(i);
            }
        }

        boolean isFeasible = checker.isSolution(solutionNodes);
        System.out.println(String.format("ILS: Final solution feasibility check: %s", isFeasible ? "FEASIBLE" : "INFEASIBLE"));
        if (!isFeasible) {
            System.err.println("WARNING: ILS returned an infeasible solution!");
        }

        return bestSolution;
    }
}