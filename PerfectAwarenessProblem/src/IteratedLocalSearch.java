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

    public IteratedLocalSearch(double alphaValue, Instance instance, SpreadingProcessOptimize eval, double betFactor, double degFactor, double eigFactor, int reconstructionIterations) {
        this.alphaValue = alphaValue;
        this.instance = instance;
        this.eval = eval;
        this.random = new Random();
        this.timeToBestMs = -1;
        this.betFactor = betFactor;
        this.degFactor = degFactor;
        this.eigFactor = eigFactor;
        this.reconstructionIterations = reconstructionIterations;

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
     * Optimized method to add nodes from RCL using shuffled indices
     * Avoids cycles by iterating through shuffled indices instead of random selection
     */
    private void addNodeFromCandidateListOptimized(Solution solution) {
        recalculateRCL(solution);
        if (RCL.isEmpty()) return;

        // Crear lista de índices
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < RCL.size(); i++) {
            indices.add(i);
        }

        // Hacer shuffle de los índices
        Collections.shuffle(indices, random);

        // Intentar añadir nodos hasta que sea factible
        for (Integer index : indices) {
            int nodeToAdd = RCL.get(index);
            if (!solution.isIn(nodeToAdd)) {
                try {
                    solution.addNode(nodeToAdd);
                    instance.resetState(solution);

                    // Si ya es factible, salir
                    if (eval.isSolution(solution)) {
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("Error adding node: " + e.getMessage());
                }
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
            addNodeFromCandidateListOptimized(solution);
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

    public Solution perturbation(Solution solution,int nodesToRemove) {
        Instant perturbationStart = Instant.now();

        // Eliminar nodos una sola vez
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
        nodesToRemove%=nodesInSolution.size()+1;
        for (int i = 0; i < nodesToRemove; i++) {
            int randomIndex = random.nextInt(nodesInSolution.size());
            int nodeToRemove = nodesInSolution.get(randomIndex);
            perturbedSol.removeNodeForPerturbation(nodeToRemove);
            nodesInSolution.set(randomIndex, nodesInSolution.get(nodesInSolution.size() - 1));
            nodesInSolution.remove(nodesInSolution.size() - 1);
        }

        // Ahora reconstruir N veces y quedarse con la mejor
        Solution bestPerturbedSol = null;
        int bestValue = Integer.MAX_VALUE;

        for (int iter = 0; iter < reconstructionIterations; iter++) {
            // Crear una copia de la solución perturbada para cada iteración de reconstrucción
            Solution reconstructedSol = new Solution(perturbedSol.getBitwiseRepresentation());
            instance.resetState(reconstructedSol);

            while (!eval.isSolution(reconstructedSol)) {
                if (Duration.between(perturbationStart, Instant.now()).toMillis() > TestRunner.TIME_LIMIT_MS) {
                    return solution;
                }
                addNodeFromCandidateListOptimized(reconstructedSol);
            }

            // Quedarse con la mejor solución
            if (reconstructedSol.solutionValue() < bestValue) {
                int previousBest = bestValue;
                bestValue = reconstructedSol.solutionValue();
                bestPerturbedSol = reconstructedSol;
                if(bestValue==1) break;
                //System.out.println("ILS: Reconstruction iteration " + iter + " improved: " + previousBest + " -> " + bestValue);
            }
        }

        //System.out.println("ILS: Perturbation completed - Final value: " + bestPerturbedSol.solutionValue());
        return bestPerturbedSol;
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
                if(bestValue==1) break;
                Solution perturbedSolution = perturbation(currentSolution,iteration);
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
                        if(bestValue==1) break;
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