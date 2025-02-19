import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class LocalSearch {
    Solution bestSolutionFound;

    public Solution exchange(Solution sol, int[] pair, int toExchange) {
        ArrayList<Integer> posSol = new ArrayList<>();
        for (Integer i : sol.getSolution()) {
            if (i != pair[0] && i != pair[1]) posSol.add(i);
        }
        if (!sol.isIn(toExchange)) {
            posSol.add(toExchange);
            return new Solution(posSol);
        }
        return sol;
    }

    public LocalSearch(Solution solution, Evaluation e, boolean evaluateSecondOF) {
        // Definimos la búsqueda local con el esquema habitual
        this.bestSolutionFound = solution;
        boolean improved = true;
        while (improved) {
            // Implementamos el movimiento de la solución. En este caso, es un swap de dos nodos de la solución por un nodo que no esté en la misma.
            solution = this.bestSolutionFound;
            improved = false;
            LinkedList<Integer> nodesNotInSolution = this.bestSolutionFound.nodesNotInSolution();
            ArrayList<Integer> solutionStructure = solution.getSolution();
            for (Integer node : nodesNotInSolution) {
                // El intercambio consiste en coger uno de los nodos que no está en la solución, e intentar intercambiarlo por un par de los nodos
                // que sí que están.
                for (int i = 0; i < solutionStructure.size(); i++) {
                    for (int j = i + 1; j < solutionStructure.size(); j++) {
                        int exchangeOne = solutionStructure.get(i);
                        int exchangeTwo = solutionStructure.get(j);
                        BigInteger bwSol = solution.bitwiseRepresentation().xor(BigInteger.ONE.shiftLeft(exchangeOne)).xor(BigInteger.ONE.shiftLeft(exchangeTwo)).add(BigInteger.ONE.shiftLeft(node));
                        Solution finalSol = Solution.SolutionFromBitwiseRepresentation(bwSol);
                        if(e.isSolution(finalSol)) {
                            Solution neighbor = new FilterUnnecesaryNodes(finalSol, e).bestSolutionFound;
                            if (neighbor.solutionValue() < this.bestSolutionFound.solutionValue()) {
                                this.bestSolutionFound = neighbor;
                                improved = true;
                            } else if (evaluateSecondOF && neighbor.solutionValue() == this.bestSolutionFound.solutionValue() && neighbor.getCumCentrality() < this.bestSolutionFound.getCumCentrality()) {
                                this.bestSolutionFound = neighbor;
                                improved = true;
                            }
                        }

                    }
                }
            }
        }
    }
}
