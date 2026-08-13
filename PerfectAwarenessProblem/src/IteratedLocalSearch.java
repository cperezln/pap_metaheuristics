import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
    ArrayList<Integer> RCL = null;
    private int reconstructionIterations; // Number of iterations for reconstruction
    private int[] nodeFrequency; // Array to track how many times each node appears in solution after local search
    ArrayList<Integer> nodesNotInSolution = null;

    public IteratedLocalSearch(double alphaValue, Instance instance, SpreadingProcessOptimize eval, double betFactor, double degFactor, double eigFactor, int reconstructionIterations) {
        this.alphaValue = alphaValue;
        this.instance = instance;
        this.eval = eval;
        this.random = new Random(1111);
        this.timeToBestMs = -1;
        this.betFactor = betFactor;
        this.degFactor = degFactor;
        this.eigFactor = eigFactor;
        this.reconstructionIterations = reconstructionIterations;
        this.nodeFrequency = new int[instance.getNumberNodes()]; // Initialize frequency array

        // Asignar los factores de centralidad a Solution (campos estáticos)
        Solution.betFactor = betFactor;
        Solution.degFactor = degFactor;
        Solution.eigFactor = eigFactor;
    }

    public long getTimeToBestMs() {
        return timeToBestMs;
    }

    private void addNodeFromCandidateList(Solution solution) {
        recalculateRCL(solution);
        if (!RCL.isEmpty()) {
            int selectRand = random.nextInt(RCL.size());
            try {
                solution.addNode(RCL.get(selectRand));
                instance.resetState(solution);
            } catch (Exception e) {
                System.out.println("Error adding node: " + e.getMessage());
            }
        }
    }

    private void recalculateRCL(Solution solution) {
        if(RCL==null) {
            eval.isSolution(solution);
            ArrayList<Integer> restCandidateListLocal = new ArrayList<>();
            for (PairVal pv : solution.candidateList()) {
                double rclThresh = solution.minVal + alphaValue * (solution.maxVal - solution.minVal);
                if (!solution.isIn(pv.node) && pv.val >= rclThresh) {
                    restCandidateListLocal.add(pv.node);
                }
            }
            RCL=restCandidateListLocal;
        }
    }

    /**
     * Add a random node to the solution instead of using candidate list
     * This provides more diversification during perturbation
     */
    private void addRandomNode(Solution solution) {
        // Select a completely random node
        int nodeToAdd = nodesNotInSolution.removeLast();
        try {
            solution.addNode(nodeToAdd);
            instance.resetState(solution);
        } catch (Exception e) {
            System.out.println("Error adding random node " + nodeToAdd + ": " + e.getMessage());
        }
    }

    public Solution constructivePhase() {
        Instant constructiveStart = Instant.now();
        final long CONSTRUCTIVE_TIMEOUT_MS = 3000; // 30 seconds

        Solution solution = new Solution();
        // Get nodes not currently in the solution
        prepareNodesnotInSolutionforAddRandomNode(solution);
        final int CHECK_EVERY = 200000;
        boolean feasible = false;
        while (!feasible || solution.solutionValue() == 0) {
            for (int i = 0; i < CHECK_EVERY && !nodesNotInSolution.isEmpty(); i++) {
                addRandomNode(solution);
            }
            System.out.println("Size: " + solution.solutionValue());
            feasible = eval.isSolution(solution);
        }
        //System.out.println("ILS: Constructive phase completed with value: " + solution.solutionValue());
        return solution;
    }

    /**
     * Greedy constructive phase: at each step adds the node that maximises the
     * number of currently-unaware nodes that would become aware, accounting for
     * first-order spread (direct neighbours) and second-order cascade (neighbours
     * that reach the spreader threshold and activate their own neighbourhood).
     */
    public Solution greedyConstructivePhase() {
        Solution solution = new Solution();
        instance.resetState(solution);
        boolean feasible = eval.isSolution(solution); // populate awareNeighs + instance state

        while (!feasible) {
            //System.out.println(solution.solutionValue());
            if (solution.solutionValue() >= 15) { //1500
                //System.out.println(solution.solutionValue());
                // Batch mode: score all candidates and add top 2500 at once
                ArrayList<PairVal> scored = new ArrayList<>();
                for (int node : instance.getNodes()) {
                    if (solution.isIn(node)) continue;
                    scored.add(new PairVal(node, scoreForNode(node, solution)));
                }
                scored.sort((a, b) -> Double.compare(b.val, a.val));
                int batchSize = Math.min(1500, scored.size());
                for (int i = 0; i < batchSize; i++) {
                    solution.addNode(scored.get(i).node);
                }
                instance.resetState(solution);
                feasible = eval.isSolution(solution);
                System.out.println("Batch add, size now: " + solution.solutionValue());
            } else {
                int bestNode  = -1;
                int bestScore = -1;

                for (int node : instance.getNodes()) {
                    if (solution.isIn(node)) continue;
                    int score = scoreForNode(node, solution);
                    if (score > bestScore) {
                        bestScore = score;
                        bestNode  = node;
                    }
                }

                if (bestNode == -1) break; // should never happen on a connected graph
                solution.addNode(bestNode);
                instance.resetState(solution);
                feasible = eval.isSolution(solution); // re-evaluate and refresh state
            }
        }

        System.out.println("ILS: Greedy constructive completed with value: " + solution.solutionValue());
        return solution;
    }

    /**
     * Estimates the number of currently-unaware nodes that would become aware
     * if {@code node} were added as a seed.
     *
     * First order:  node itself (if unaware) + every unaware direct neighbour.
     * Second order: for each neighbour that is not yet a spreader, if adding
     *               {@code node} as a spreader pushes its spreader-count to the
     *               activation threshold (>= degree/2), we also count that
     *               neighbour's unaware neighbours.
     *
     * Requires {@code eval.isSolution(solution)} to have been called beforehand
     * so that {@code solution.getAwareNeighs()} and the instance state are fresh.
     */
    private int scoreForNode(int node, Solution solution) {
        int score = 0;

        // Node itself is unaware → becomes aware when seeded
        if (instance.getNodeState(node) == 0) score++;

        for (int neigh : instance.graph.get(node)) {
            // Direct activation: every unaware neighbour of 'node' becomes aware
            if (instance.getNodeState(neigh) == 0) score++;

            // Second-order cascade: if 'neigh' would now reach the spreader threshold,
            // count its own unaware neighbours as additional gain
            if (!solution.isIn(neigh) && instance.getNodeState(neigh) != 2) {
                int spreaderNeighs = solution.getAwareNeighs(neigh); // spreaders around neigh
                if (spreaderNeighs + 1 >= instance.graph.get(neigh).size() * 0.5) {
                    for (int nn : instance.graph.get(neigh)) {
                        if (instance.getNodeState(nn) == 0) score++;
                    }
                }
            }
        }

        return score;
    }

    public Solution localSearch(Solution solution, Instant startTime, int ilsIteration) {
        solution.removeUnnedeed();
        Solution improvedSol = solution;
        int initialValue = improvedSol.solutionValue();

        if (improvedSol.solutionValue() >= 2) {
            //Solution filteredSol = new FilterUnnecesaryNodes(improvedSol, eval).bestSolutionFound;
            LocalSearch ls = new LocalSearch(improvedSol, eval, startTime, ilsIteration, random);
            improvedSol = ls.bestSolutionFound;
            improvedSol.removeUnnedeed();
        }

        // Update node frequency array after local search
        for (int i = 0; i < instance.getNumberNodes(); i++) {
            if (improvedSol.isIn(i)) {
                nodeFrequency[i]++;
            }
        }

        int finalValue = improvedSol.solutionValue();
        //System.out.println("ILS: Local search: " + initialValue + " -> " + finalValue + " (improvement: " + (initialValue - finalValue) + ")");
        return improvedSol;
    }

    public Solution perturbation(Solution solution,int nodesToRemove) {
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

        destructRandom(nodesToRemove, nodesInSolution,perturbedSol);
        //destructGreedy(nodesToRemove, nodesInSolution, perturbedSol);
        instance.resetState(perturbedSol);

        while (!eval.isSolution(perturbedSol)) {
            if (Duration.between(perturbationStart, Instant.now()).toMillis() > TestRunner.TIME_LIMIT_MS) {
                return solution;
            }
            /*int bestNode  = -1;
            int bestScore = -1;
            for (int node : instance.getNodes()) {
                if (perturbedSol.isIn(node)) continue;
                int score = scoreForNode(node, perturbedSol);
                if (score > bestScore) {
                    bestScore = score;
                    bestNode  = node;
                }
            }
            if (bestNode == -1) break;
            perturbedSol.addNode(bestNode);*/
            prepareNodesnotInSolutionforAddRandomNode(perturbedSol); //RND
            addRandomNode(perturbedSol); //RND
            //instance.resetState(perturbedSol);
        }
        return perturbedSol;
    }

    private void prepareNodesnotInSolutionforAddRandomNode(Solution solution) {
        nodesNotInSolution.clear();
        // Use getNodes() to ensure we iterate over actual nodes in deterministic order
        for (int i : instance.getNodes()) {
            if (!solution.isIn(i)) {
                nodesNotInSolution.add(i);
            }
        }

        if (nodesNotInSolution.isEmpty()) {
            System.out.println("ILS: Warning - No nodes available to add!");
        }
        Collections.shuffle(nodesNotInSolution, random);
    }

    private void destructRandom(int nodesToRemove, ArrayList<Integer> nodesInSolution, Solution perturbedSol) {
        nodesToRemove %=nodesInSolution.size()+1;
        for (int i = 0; i < nodesToRemove; i++) {
            int randomIndex = random.nextInt(nodesInSolution.size());
            int nodeToRemove = nodesInSolution.get(randomIndex);
            perturbedSol.removeNodeForPerturbation(nodeToRemove);
            nodesInSolution.set(randomIndex, nodesInSolution.get(nodesInSolution.size() - 1));
            nodesInSolution.remove(nodesInSolution.size() - 1);
        }
    }

    /**
     * Greedy destruction method that removes nodes with highest frequency
     * (nodes that have appeared most often in solutions after local search)
     */
    private void destructGreedy(int nodesToRemove, ArrayList<Integer> nodesInSolution, Solution perturbedSol) {
        // Ajustar nodesToRemove para no eliminar más de la mitad de los nodos
        nodesToRemove %= nodesInSolution.size()  + 1;

        // Ordenar los nodos por frecuencia (de mayor a menor)
        nodesInSolution.sort((a, b) -> Integer.compare(nodeFrequency[b], nodeFrequency[a]));

        // Eliminar los primeros k nodos (los de mayor frecuencia)
        for (int i = 0; i < nodesToRemove && i < nodesInSolution.size(); i++) {
            int nodeToRemove = nodesInSolution.get(i);
            perturbedSol.removeNodeForPerturbation(nodeToRemove);
            nodeFrequency[nodeToRemove]-=2;
        }
    }

    /**
     * Write solution to file in specified directory
     */
    private void writeSolutionToFile(Solution solution, String instanceName, String directoryName, Instant startTime) {
        try {
            // Remove extension from instance name if present
            String instanceBaseName = instanceName.replaceFirst("\\.[^.]+$", "");
            String outputFileName = instanceBaseName + ".txt";
            String outputPath = directoryName + "/" + outputFileName;

            PrintWriter writer = new PrintWriter(outputPath, "UTF-8");

            long executionTime = Duration.between(startTime, Instant.now()).toMillis();

            writer.println(instanceBaseName);
            writer.println(solution.solutionValue());
            writer.println(solution);
            writer.println(executionTime);

            writer.close();

            System.out.println("Saved solution to " + outputPath + " with value: " + solution.solutionValue());
        } catch (IOException e) {
            System.err.println("Error writing solution to file: " + e.getMessage());
        }
    }

    public Solution run() {
        Instant startTime = Instant.now();

        // Initialize PrintWriter for time to best output
        String instanceName = instance.name;
        nodesNotInSolution = new ArrayList<>(instance.getNumberNodes());
        // Create time_to_best directory if it doesn't exist
        java.io.File timeToBestDir = new java.io.File("time_to_best");
        if (!timeToBestDir.exists()) {
            timeToBestDir.mkdirs();
        }

        // Create directories for constructive and local search solutions
        java.io.File constructiveDir = new java.io.File("Constructivo");
        if (!constructiveDir.exists()) {
            constructiveDir.mkdirs();
        }
        java.io.File localSearchDir = new java.io.File("LocalSearch");
        if (!localSearchDir.exists()) {
            localSearchDir.mkdirs();
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
            // Execute N iterations of constructive + local search before ILS
            Solution currentSolution = null;
            int constructiveOF = Integer.MAX_VALUE;
            int lsOF = Integer.MAX_VALUE;
            int bestValue = Integer.MAX_VALUE;

            for (int initIter = 0; initIter < 1; initIter++) {
                // Reset RCL for each construction
                RCL = null;

                // Constructive phase
                Solution tempSolution = greedyConstructivePhase();
                int tempConstructiveOF = tempSolution.solutionValue();

                // Write constructive solution to file
                writeSolutionToFile(tempSolution, instanceName, "Constructivo", startTime);
                tempSolution = new FilterUnnecesaryNodes(tempSolution, eval).bestSolutionFound;
                // Local search phase
                tempSolution = localSearch(tempSolution, startTime, initIter);
                int tempLsOF = tempSolution.solutionValue();

                // Write local search solution to file
                writeSolutionToFile(tempSolution, instanceName, "LocalSearch", startTime);

                System.out.println(String.format("ILS: Init iteration %d - Constructive: %d, After LS: %d",
                        initIter + 1, tempConstructiveOF, tempLsOF));

                // Keep the best solution found (or first solution if it's the first iteration)
                if (currentSolution == null || tempLsOF < bestValue) {
                    currentSolution = tempSolution;
                    constructiveOF = tempConstructiveOF;
                    lsOF = tempLsOF;
                    bestValue = tempLsOF;
                    System.out.println(String.format("ILS: New best in initial phase: %d", bestValue));
                }

                // Check time limit
                if (Duration.between(startTime, Instant.now()).toMillis() >= TestRunner.TIME_LIMIT_MS) {
                    break;
                }
            }

            // Ensure we have a valid solution
            if (currentSolution == null) {
                System.err.println("ERROR: No solution generated in initial phase!");
                return null;
            }

            bestSolution = new Solution(currentSolution.getBitwiseRepresentation());

            // Record initial solution as first improvement
            long initialTimeToBest = Duration.between(startTime, Instant.now()).toMillis();
            timeToBestMs = initialTimeToBest;
            timeToBestWriter.println(initialTimeToBest + "\t" + bestValue + "\t0");
            timeToBestWriter.flush();
            System.out.println("ILS: Initial time to best: " + initialTimeToBest + " ms");

            System.out.println(String.format("ILS: Initial solution value: %d", bestValue));

            int iteration = 0;
            int iterationsWithoutImprovement = 0;
            final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 20;

            while (Duration.between(startTime, Instant.now()).toMillis() < TestRunner.TIME_LIMIT_MS) {
                iteration++;
                if(bestValue==1) break;
                Solution perturbedSolution = perturbation(currentSolution,iteration);
                Solution localOptimum = localSearch(perturbedSolution, startTime, iteration-1);

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
                        if(bestValue==1) break;
                    }
                } else {
                    iterationsWithoutImprovement++;
                }

                // Reinicio si hay demasiadas iteraciones sin mejora
                if (iterationsWithoutImprovement >= MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                    //System.out.println(String.format("ILS: Restart at iteration %d - reconstructing solution...", iteration));
                    currentSolution = greedyConstructivePhase();
                    currentSolution = localSearch(currentSolution, startTime, iteration);
                    iterationsWithoutImprovement = 0;
                    //System.out.println(String.format("ILS: After restart, current solution value: %d", currentSolution.solutionValue()));
                    if (currentSolution.solutionValue() < bestValue) {
                        bestSolution = new Solution(currentSolution.getBitwiseRepresentation());
                        bestValue = bestSolution.solutionValue();

                        // Record time to best for this improvement
                        long currentTimeToBest = Duration.between(startTime, Instant.now()).toMillis();
                        timeToBestMs = currentTimeToBest;

                        timeToBestWriter.println(currentTimeToBest + "\t" + bestValue + "\t" + iteration);
                        timeToBestWriter.flush();


                        System.out.println("ILS: New best solution found at iteration " + iteration + " with value: " + bestValue);
                        System.out.println("ILS: Time to best: " + currentTimeToBest + " ms");
                        if(bestValue==1) break;
                    }
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