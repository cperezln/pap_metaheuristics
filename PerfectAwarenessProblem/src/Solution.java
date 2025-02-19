import java.math.BigInteger;
import java.util.*;
import java.util.stream.IntStream;

public class Solution {
    // In this works, solutions are composed as a number of nodes
    static Instance instance;
    private ArrayList<Integer> solution;
    private BigInteger solutionBw;
    private boolean[] containSet;
    private int cumCentrality = 0;
    private int solutionValue = Integer.MAX_VALUE;

    public Solution(ArrayList<Integer> solution) {
        this.solution = solution;
        this.containSet = new boolean[instance.getNumberNodes()];
        BigInteger bitwiseRepresentation = BigInteger.ZERO;
        for(int i: this.solution) {
            this.containSet[i] = true;
            bitwiseRepresentation = bitwiseRepresentation.add(BigInteger.ONE.shiftLeft(i));
            this.cumCentrality += instance.getCentrality(i);
        }
        this.solutionBw = bitwiseRepresentation;
        this.solutionValue = this.solution.size();
    }

    public Solution(int[] solution) {
        this.solution =  new ArrayList<>();
        this.containSet = new boolean[instance.getNumberNodes()];
        BigInteger bitwiseRepresentation = BigInteger.ZERO;
        for(int i: solution) {
            this.solution.add(i);
            bitwiseRepresentation = bitwiseRepresentation.add(BigInteger.ONE.shiftLeft(i));
            this.containSet[i] = true;
            this.cumCentrality += instance.getCentrality(i);
        }
        this.solutionBw = bitwiseRepresentation;
        this.solutionValue = this.solution.size();
    }

    public Solution(BigInteger solution) {
        this.solutionBw = solution;
        char[] bitwiseRepresentation = this.solutionBw.toString(2).toCharArray();
        this.containSet = new boolean[instance.getNumberNodes()];
        this.solution = new ArrayList<>();
        for(int i = bitwiseRepresentation.length - 1; i >= 0; i--) {
            if(bitwiseRepresentation[i] == '1') {
                this.solution.add((bitwiseRepresentation.length - 1) - i);
                this.containSet[(bitwiseRepresentation.length - 1) - i] = true;
                this.cumCentrality += instance.getCentrality((bitwiseRepresentation.length - 1) - i);
            }
        }
        this.solutionValue = this.solution.size();
    }

    public boolean checkValidityOfSolution(Instance instance){
        return solution.size() < instance.getNumberNodes();
    }

    public ArrayList<Integer> getSolution() {
        return solution;
    }

    public int getCumCentrality() {
        return cumCentrality;
    }

    public int solutionValue() { return solutionValue; }

    public void addNode(int node) { this.solution.add(node); }

    public boolean isIn(int node) { return this.containSet[node]; }

    public LinkedList<Integer> nodesNotInSolution() {
        LinkedList<Integer> ll = new LinkedList<>();
        for(int j: instance.getNodes()) if(!this.isIn(j)) ll.add(j);
        return ll;
    }

    @Override
    public String toString() {
        String s = "(";
        for(int i: solution) {
            s += Integer.toString(i) + ",";
        }
        s += ")";
        return s;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public BigInteger getBitwiseRepresentation() { return this.solutionBw; }

    public static Solution GenerateBruteForce(Instance instance, Evaluation eval) {
        int[] arr = IntStream.range(0, instance.getNumberNodes()).toArray();
        boolean foundSol = false;
        Solution posSol = null;
        for (int solSize = 1; solSize <= instance.getNumberNodes(); solSize++) {
            ArrayList<int[]> possibleSolutions = new ArrayList<>();
            try {
                Main.computeCombination(arr, arr.length, solSize, possibleSolutions);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
            for (int[] tuple : possibleSolutions) {
                posSol = new Solution(tuple);
                if (eval.isSolution(posSol)) {
                    System.out.println("Instancia " + instance.name + " con solución " + posSol);
                    foundSol = true;
                    break;
                }
            }
            if(foundSol) break;
        }
        return posSol;
    }
    public static Solution GenerateRandomSolutionDegree(Instance instance, Evaluation eval) {
        PriorityQueue<PairDeg> nodeDeg = new PriorityQueue<>();
        for(Map.Entry<Integer, ArrayList<Integer>> entry: instance.graph.entrySet()) {
            nodeDeg.add(new PairDeg(entry.getKey(), entry.getValue().size()));
        }
        int k = (int) Math.ceil(instance.getNumberNodes()/2);
        int[] setToPick = new int[k];
        for(int j = 0; j < k; j++) setToPick[j] = nodeDeg.poll().node;
        Solution sol = null;
        HashSet<Integer> visited = new HashSet<>();
        while(sol == null) {
            int randomNumberOfNodes = (int) Math.ceil(Math.random()*k);
            visited.add(randomNumberOfNodes);
            ArrayList<Integer> posSol = new ArrayList<>();
            for(int j = 0; j < randomNumberOfNodes; j++) {
                int toAdd = setToPick[Integer.min(Integer.max((int) Math.floor(Math.random()*k) - 1, 0), k - 1)];
                if(!posSol.contains(toAdd)) posSol.add(toAdd);
            }
            if(eval.isSolution(new Solution(posSol))) {
                sol = new Solution(posSol);
                break;
            }
            else if(visited.size() == k) {
                break;
            }
        }
        return sol;
    }

    public static Solution GenerateIncrementalRandomSolution(Instance instance, Evaluation eval) {
        Solution sol = null;
        HashSet<Integer> visited = new HashSet<>();
        ArrayList<Integer> posSol = new ArrayList<>();
        ArrayList<Integer> nodes = instance.getNodes();
        while(sol == null) {
            int randomIndex = Math.min((int) Math.ceil(Math.random()*nodes.size()), nodes.size() - 1);
            int posNode = nodes.get(randomIndex);
            if(!visited.contains(posNode)) {
                visited.add(posNode);
                posSol.add(posNode);
            }
            Solution auxSol = new Solution(posSol);
            if(eval.isSolution(auxSol)) sol = auxSol;
        }
        return sol;
    }

    public static Solution GenerateDecrementalRandomSolution(Instance instance, Evaluation eval) {
        Solution sol = null;
        ArrayList<Integer> posSol = instance.getNodes();
        while(sol == null) {
            int randomIndex = Math.min((int) Math.ceil(Math.random()*posSol.size()), posSol.size() - 1);
            int posNode = posSol.get(randomIndex);
            posSol.remove(randomIndex);
            Solution auxSol = new Solution(posSol);
            if(!eval.isSolution(auxSol)) {
                auxSol.addNode(posNode);
                sol = auxSol;
            }
        }
        return sol;
    }

    public static Solution GenerateDegreeGreedySolution(Instance instance, Evaluation eval) {
        Solution sol = null;
        ArrayList<Integer> posSol = new ArrayList<>();
        HashSet<Integer> inSolution = new HashSet<>();
        while(sol == null) {
            int selectedNode = -1;
            double bestValue = Integer.MIN_VALUE;
            // TODO mejorar esto. Podemos hacer un prorityqueue que ordene directamente los nodos por
            // el factor que estamos seleccionando
            for(Integer j: instance.graph.keySet()) {
                if(instance.nodeValue(j) > bestValue && !inSolution.contains(j)) {
                    selectedNode = j;
                    bestValue = instance.nodeValue(j);
                }
            }
            if(selectedNode != -1) {
                posSol.add(selectedNode);
                inSolution.add(selectedNode);
                // Update the value of the node following the greedy rule
                for(Integer spreader: posSol) {
                    for(Integer neigh: instance.graph.get(spreader)) {
                        instance.nSpreaders.put(neigh, instance.nSpreaders.get(neigh) + 1);
                    }
                }
            }
            Solution newSol = new Solution(posSol);
            if(eval.isSolution(newSol)) {
                sol = newSol;
                break;
            }
        }
        return sol;
    }

}
