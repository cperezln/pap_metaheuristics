import java.math.BigInteger;
import java.util.LinkedList;

public class LocalSearch {
    Solution bestSolutionFound;

    public LocalSearch(Solution solution, SpreadingProcessOptimize e) {
        // Definimos la búsqueda local con el esquema habitual
        this.bestSolutionFound = solution;
        boolean improved = true;
        while (improved) {
            // Implementamos el movimiento de la solución. En este caso, es un swap de dos nodos de la solución por un nodo que no esté en la misma.
            this.bestSolutionFound = new FilterUnnecesaryNodes(this.bestSolutionFound, e).bestSolutionFound;
            solution = this.bestSolutionFound;
            improved = false;
            LinkedList<Integer> nodesNotInSolution = this.bestSolutionFound.nodesNotInSolution();
            for (Integer node : nodesNotInSolution) {
                if(improved) break;
                // El intercambio consiste en coger uno de los nodos que no está en la solución, e intentar intercambiarlo por un par de los nodos
                // que sí que están.
                int iter = 0;
                BigInteger nextPossibleI = solution.getBitwiseRepresentation();
                int indexI = nextPossibleI.getLowestSetBit();
                while (indexI != -1) {
                    if (improved) break;
                    BigInteger nextPossibleJ = nextPossibleI.xor(BigInteger.ONE.shiftLeft(indexI));
                    int indexJ = nextPossibleJ.getLowestSetBit();
                    while (indexJ != -1) {
                        if(improved) break;
                        iter++;
                        int exchangeOne = indexI;
                        int exchangeTwo = indexJ;
                        BigInteger bwSol = solution.getBitwiseRepresentation().xor(BigInteger.ONE.shiftLeft(exchangeOne)).xor(BigInteger.ONE.shiftLeft(exchangeTwo)).add(BigInteger.ONE.shiftLeft(node));
                        Solution finalSol = new Solution(bwSol);
                        Solution.instance.resetState(finalSol);
                        if(e.isSolution(finalSol)) {
                            Solution neighbor = new FilterUnnecesaryNodes(finalSol, e).bestSolutionFound;
                            if (neighbor.solutionValue() < this.bestSolutionFound.solutionValue()) {
                                // System.out.println("Iteraciones para mejorar " + iter + " con resultado de FO " + neighbor.solutionValue());
                                this.bestSolutionFound = neighbor;
                                improved = true;
                            }
                        }
                        nextPossibleJ = nextPossibleJ.xor(BigInteger.ONE.shiftLeft(indexJ));
                        indexJ = nextPossibleJ.getLowestSetBit();
                    }
                    nextPossibleI = nextPossibleI.xor(BigInteger.ONE.shiftLeft(indexI));
                    indexI = nextPossibleI.getLowestSetBit();
                }
            }
        }
    }
}
