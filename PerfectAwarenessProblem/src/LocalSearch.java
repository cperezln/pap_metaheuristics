import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LocalSearch {
    Solution bestSolutionFound;
    private Random random;

    public LocalSearch(Solution solution, SpreadingProcessOptimize e,  Instant startTime, int ilsIteration, Random random) {

        this.bestSolutionFound = solution;
        this.random = random;
        boolean improved = true;
        Instant initTime = Instant.now();

        ArrayList<Integer> nodesInSolution = new ArrayList<>();
        int n = Solution.instance.getNumberNodes();
        int m = Solution.instance.getNumberEdges();
        double density = (2.0 * m) / (n * (n - 1));
        boolean isDense = density > 0.15;
        while (improved) {
            solution = this.bestSolutionFound;
            improved = false;
            LinkedList<Integer> nodesNotInSolution = this.bestSolutionFound.nodesNotInSolution();
            if (nodesNotInSolution.isEmpty()) {
                break;
            }
            ArrayList<Integer> candidatesOut = new ArrayList<>(nodesNotInSolution);

            int steps = 80;
            if (!isDense && nodesNotInSolution.size() > steps) {
                // Paso 1: Top-K rápido (tomar top 3*steps)
                candidatesOut.sort((a, b) -> {
                    int degA = Solution.instance.graph.get(a).size();
                    int degB = Solution.instance.graph.get(b).size();
                    return Integer.compare(degB, degA);
                });
                if(n<=900) {
                    // Paso 2: Weighted sampling sobre top-K (Efraimidis & Spirakis)
                    int topK = Math.min(3 * steps, candidatesOut.size());
                    List<Integer> top = new ArrayList<>(candidatesOut.subList(0, topK));

                    // Clase auxiliar simple para guardar nodo y clave
                    class WeightedKey {
                        int node;
                        double key;

                        WeightedKey(int node, double key) {
                            this.node = node;
                            this.key = key;
                        }
                    }

                    // Generar clave aleatoria para cada nodo según su peso
                    List<WeightedKey> keys = new ArrayList<>(top.size());
                    for (int node : top) {
                        int degree = Solution.instance.graph.get(node).size();
                        double weight = Math.pow(degree, 4);  // puedes ajustar el exponente
                        double u = random.nextDouble();
                        double key = Math.pow(u, 1.0 / weight); // clave ponderada
                        keys.add(new WeightedKey(node, key));
                    }

                    // Ordenar por clave descendente (mayor key = más probabilidad)
                    keys.sort((a, b) -> Double.compare(b.key, a.key));

                    // Tomar los steps mejores
                    ArrayList<Integer> sampled = new ArrayList<>(steps);
                    for (int i = 0; i < Math.min(steps, keys.size()); i++) {
                        sampled.add(keys.get(i).node);
                    }

                    // Resultado final
                    candidatesOut = sampled;
                }else{
                    candidatesOut = new ArrayList<>(candidatesOut.subList(0, Math.min(steps, candidatesOut.size())));
                }

            } else {
                java.util.Collections.shuffle(candidatesOut, random);
            }

            nodesInSolution.clear();
            BigInteger solutionBits = solution.getBitwiseRepresentation();
            for (int i = 0; i < Solution.instance.getNumberNodes(); i++) {
                if (solutionBits.testBit(i)) {
                    nodesInSolution.add(i);
                }
            }

            java.util.Collections.shuffle(nodesInSolution, random);

            for (Integer node : candidatesOut) {

                if(improved) break;
                long currentTime = Instant.now().toEpochMilli();
                if(currentTime - initTime.toEpochMilli() >= TestRunner.LOCAL_SEARCH_TIME_LIMIT_MS || Duration.between(startTime, Instant.now()).toMillis() > TestRunner.TIME_LIMIT_MS) {
                    return;
                }

                // Efficient nested loop over nodes in solution
                for (int i = 0; i < nodesInSolution.size() && !improved; i++) {
                    int indexI = nodesInSolution.get(i);
                    for (int j = i + 1; j < nodesInSolution.size() && !improved; j++) {
                        int indexJ = nodesInSolution.get(j);

                        // Perform swap: remove indexI and indexJ, add node
                        BigInteger bwSol = solutionBits
                                .clearBit(indexI)
                                .clearBit(indexJ)
                                .setBit(node);

                        Solution finalSol = new Solution(bwSol);
                        Solution.instance.resetState(finalSol);
                        if(e.isSolution(finalSol)) {
                            //finalSol = new FilterUnnecesaryNodes(finalSol, e).bestSolutionFound;
                            if (finalSol.solutionValue() < this.bestSolutionFound.solutionValue()) {
                                improved = true;
                                //finalSol = new FilterUnnecesaryNodes(finalSol, e).bestSolutionFound;
                                int improvement = this.bestSolutionFound.solutionValue() - finalSol.solutionValue();
                                //System.out.println("LocalSearch: Improvement found! " + this.bestSolutionFound.solutionValue() + " -> " + finalSol.solutionValue() + " (gain: " + improvement + ")");
                                this.bestSolutionFound = finalSol;
                                nodesInSolution.remove((Integer)indexI);
                                nodesInSolution.remove((Integer)indexJ);
                                nodesInSolution.add(node);
                            }
                        }
                        currentTime = Instant.now().toEpochMilli();
                        if(currentTime - initTime.toEpochMilli() >= TestRunner.LOCAL_SEARCH_TIME_LIMIT_MS || Duration.between(startTime, Instant.now()).toMillis() > TestRunner.TIME_LIMIT_MS) {
                            return;
                        }
                    }
                }
            }
        }
    }
}