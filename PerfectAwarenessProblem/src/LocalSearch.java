import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class LocalSearch {
    Solution bestSolutionFound;

    public Solution exchange(Solution sol, int[] pair, int toExchange) {
        ArrayList<Integer> posSol = new ArrayList<>();
        for(Integer i: sol.getSolution()) {
            if(i != pair[0]) posSol.add(i);
        }
        if(!sol.isIn(toExchange)) {
            posSol.add(toExchange);
            return new Solution(posSol);
        }
        return sol;
    }


    public LocalSearch(Solution solution,  Evaluation e) {
        /* TODO darle una vuelta a la idea de esta búsqueda local. El problema es que cuando encuentras una solución mejor en el "vecindario"
        sigues teniendo el problema de que ya no vas a encontrar ninguna mejor en dicho vecindario. No vas a explorar todo el vecindario de una solución,
        sino dirigiendolo a partir de la primera mejora que encontramos
         */
        this.bestSolutionFound = solution;
        boolean improved = true;
        while(improved) {
            Queue<Integer> queueNotInSolution = this.bestSolutionFound.nodesNotInSolution();
            improved = false;
            ArrayList<Integer> solutionStructue = solution.getSolution();
            while(!queueNotInSolution.isEmpty() && !improved) {
                int toExchange = queueNotInSolution.poll();
                for (int i = 0; i < solutionStructue.size(); i++) {
                    for (int j = i + 1; j < solutionStructue.size(); j++) {
                        Solution neighbor = exchange(this.bestSolutionFound, new int[]{solutionStructue.get(i), solutionStructue.get(j)}, toExchange);
                        if(e.isSolution(neighbor)) {
                            if(neighbor.solutionValue() < this.bestSolutionFound.solutionValue()) {
                                this.bestSolutionFound = solution;
                                improved = true;
                            }
                            else if(neighbor.solutionValue() == this.bestSolutionFound.solutionValue() && neighbor.getCumCentrality() < this.bestSolutionFound.getCumCentrality()) {
                                this.bestSolutionFound = neighbor;
                                improved = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public void l1ocalSearch(Solution solution,  Evaluation e) {
        /* TODO darle una vuelta a la idea de esta búsqueda local. El problema es que cuando encuentras una solución mejor en el "vecindario"
        sigues teniendo el problema de que ya no vas a encontrar ninguna mejor en dicho vecindario. No vas a explorar todo el vecindario de una solución,
        sino dirigiendolo a partir de la primera mejora que encontramos
         */
        this.bestSolutionFound = solution;
        boolean improved = true;
        while(improved) {
            Queue<Integer> queueNotInSolution = this.bestSolutionFound.nodesNotInSolution();
            improved = false;
            ArrayList<Integer> solutionStructue = solution.getSolution();
            while(!queueNotInSolution.isEmpty() && !improved) {
                int toExchange = queueNotInSolution.poll();
                for (int i = 0; i < solutionStructue.size(); i++) {
                    Solution neighbor = exchange(this.bestSolutionFound, new int[]{solutionStructue.get(i)}, toExchange);
                    if(e.isSolution(neighbor)) {
                        if(neighbor.solutionValue() == this.bestSolutionFound.solutionValue() && neighbor.getCumCentrality() < this.bestSolutionFound.getCumCentrality()) {
                            this.bestSolutionFound = neighbor;
                            improved = true;
                        }
                    }
                }
            }
        }
    }

    public void localSearch(Solution solution, Evaluation e, Instance instance) {
        this.bestSolutionFound = solution;
        HashSet<Solution> visited = new HashSet<>();
        boolean improved = true;
        while(improved) {
            improved = false;
            for(Integer i: this.bestSolutionFound.getSolution()) {
                for(Integer neigh: instance.graph.get(i)) {
                    ArrayList<Integer> posSol = new ArrayList<>();
                    for(Integer k: this.bestSolutionFound.getSolution()) if(k != i) posSol.add(k);
                    posSol.add(neigh);
                    Solution newSol = new Solution(posSol);
                    if(e.isSolution(newSol)) {
                        if(newSol.solutionValue() < this.bestSolutionFound.solutionValue()) {
                            this.bestSolutionFound = newSol;
                            improved = true;
                        }
                        else if(newSol.getCumCentrality() < this.bestSolutionFound.getCumCentrality()) {
                            this.bestSolutionFound = newSol;
                            improved = true;
                        }
                    }
                }
            }
        }
    }
}
