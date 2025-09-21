import java.math.BigInteger;
import java.util.*;

public class Solution {
    // In this works, solutions are composed as a number of nodes
    // Static (general) parameters
    public static double awareFactor;
    public static double betFactor;
    public static double degFactor;
    public static double eigFactor;
    public static Instance instance;
    // Structure - related parameters
    private BigInteger solutionBw;
    // Metrics parameters
    private int solutionValue = Integer.MAX_VALUE;
    // Diffusion parameters
    private int numberAware = 0;
    private int[] awareNeighs;

    // GRASP parameters
    public double minVal = Double.MAX_VALUE;
    public double maxVal = Double.MIN_VALUE;


    public Solution(BigInteger solution) {
        this.solutionBw = solution;
        this.solutionValue = solution.bitCount();
        this.numberAware = this.solutionValue;
    }

    public Solution() {
        this.solutionBw = BigInteger.ZERO;
        this.solutionValue = 0;
        this.numberAware = 0;
    }

    @Override
    public String toString() {
        String s = "(";
        BigInteger nextPossible = this.solutionBw;
        int index = nextPossible.getLowestSetBit();
        if (index != -1) {
            do {
                s += index + ", ";
                nextPossible = nextPossible.xor(BigInteger.ONE.shiftLeft(index));
                index = nextPossible.getLowestSetBit();
            } while (index != -1);
        }
        s += ")";
        return s;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public int solutionValue() {
        return solutionValue;
    }

    public void addNode(int node) {
        if(!this.isIn(node)) {
            this.solutionBw = this.solutionBw.add(BigInteger.ONE.shiftLeft(node));
            this.solutionValue = this.solutionBw.bitCount();
            this.numberAware = this.solutionValue;
            instance.setState(node, 2);
            for (int neigh : instance.graph.get(node)) {
                instance.setState(neigh, 1);
            }
        }
    }

    public void removeNode(int node) {
        this.solutionBw = this.solutionBw.clearBit(node);
        this.solutionValue = this.solutionBw.bitCount();
        this.numberAware = this.solutionValue;
        instance.setState(node, 0);
    }

    public boolean isIn(int node) {
        return this.solutionBw.testBit(node);
    }

    public LinkedList<Integer> nodesNotInSolution() {
        LinkedList<Integer> ll = new LinkedList<>();
        for (int j : instance.getNodes()) if (!this.isIn(j)) ll.add(j);
        return ll;
    }

    public ArrayList<PairVal> candidateList() {
        maxVal = Double.MIN_VALUE;
        minVal = Double.MAX_VALUE;
        ArrayList<PairVal> al = new ArrayList<>();
        for (int j : instance.getNodes()) {
            if (!this.isIn(j) && !instance.isLeaf(j)) {
                double awareValue = (double) (instance.graph.get(j).size() - getAwareNeighs(j)) / instance.graph.get(j).size();
                double nodeValue = 0;
                if(awareValue == 0) {
                    nodeValue = 0;
                }
                else {
                    double[] cen = instance.getCentrality(j);
                    nodeValue = awareValue*(betFactor * cen[0] + degFactor * cen[1] + eigFactor * cen[2]) / (betFactor + degFactor + eigFactor);
                }
                if(nodeValue > maxVal) maxVal = nodeValue;
                if(nodeValue < minVal) minVal = nodeValue;
                al.add(new PairVal(j, nodeValue));
            }
        }
        return al;
    }

    public BigInteger getBitwiseRepresentation() {
        return this.solutionBw;
    }

    public void removeUnnedeed() {
        BigInteger nextPossible = this.solutionBw;
        int index = nextPossible.getLowestSetBit();
        if (index != -1) {
            do {
                if(this.getSpreaderNeighs(index) >= instance.graph.get(index).size() * 0.5 && this.isIn(index)) {
                    this.removeNode(index);
                }
                nextPossible = nextPossible.xor(BigInteger.ONE.shiftLeft(index));
                index = nextPossible.getLowestSetBit();
            } while (index != -1);
        }
    }

    public int getAwareNeighs(int node) {
        /*int numberOf = 0;
        for(int i: instance.graph.get(node)) {
            if(instance.getNodeState(i) >= 1) numberOf++;
        }
        return numberOf;*/
        return awareNeighs[node];
    }

    public int getSpreaderNeighs(int node) {
        int numberOf = 0;
        for(int i: instance.graph.get(node)) {
            if(instance.getNodeState(i) == 2) numberOf++;
        }
        return numberOf;
    }

    public void setAwareCount(int[] aware) {
        this.awareNeighs = aware;
    }

    public void setAware(int nAware) { this.numberAware = nAware; }

    public int getAware() { return this.numberAware; }

    // STATIC METHODS

    public static Solution GenerateIncrementalRandomSolution(Instance instance, SpreadingProcessOptimize eval) {
        Solution sol = null;
        HashSet<Integer> visited = new HashSet<>();
        Solution posSol = new Solution();
        ArrayList<Integer> nodes = new ArrayList<>();
        for(int i: instance.getNodes()) {
            if(!instance.isLeaf(i)) {
                nodes.add(i);
            }
        }
        while(sol == null) {
            int randomIndex = Math.min((int) Math.ceil(Math.random()*nodes.size()), nodes.size() - 1);
            int posNode = nodes.get(randomIndex);
            if(!visited.contains(posNode) && !instance.isLeaf(posNode)) {
                nodes.remove(randomIndex);
                visited.add(posNode);
                posSol.addNode(posNode);
            }
            Solution auxSol = posSol;
            if(eval.isSolution(auxSol)) sol = auxSol;
        }
        return sol;
    }

    public static Solution GenerateDecrementalRandomSolution(Instance instance, SpreadingProcessOptimize eval) {
        Solution sol = null;
        HashSet<Integer> visited = new HashSet<>();
        ArrayList<Integer> nodes = instance.getNodes();
        Solution posSol = new Solution();
        for(int node: nodes) {
            posSol.addNode(node);
        }
        while(sol == null) {
            int randomIndex = Math.min((int) Math.ceil(Math.random()*nodes.size()), nodes.size() - 1);
            int posNode = nodes.get(randomIndex);
            posSol.removeNode(posNode);
            Solution auxSol = posSol;
            if(!eval.isSolution(auxSol)) {
                auxSol.addNode(posNode);
                sol = auxSol;
            }
        }
        return sol;
    }

    public static Solution GenerateGreedyDegreeSolution(Instance instance, SpreadingProcessOptimize eval) {
        Solution posSol = new Solution();
        ArrayList<Integer> nodesByDegree = instance.getNodes();
        nodesByDegree.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int res = 0;
                if(instance.getCentrality(o1)[1] < instance.getCentrality(o2)[1]){
                    res = 1;
                }
                else if(instance.getCentrality(o1)[1] > instance.getCentrality(o2)[1]) {
                    res = -1;
                }
                return res;
            }
        });
        while(!eval.isSolution(posSol)) {
            posSol.addNode(nodesByDegree.getFirst());
            nodesByDegree.removeFirst();
        }
        return posSol;
    }

    public static Solution GenerateGreedyBetweenessSolution(Instance instance, SpreadingProcessOptimize eval) {
        Solution posSol = new Solution();
        ArrayList<Integer> nodesByDegree = instance.getNodes();
        nodesByDegree.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int res = 0;
                if(instance.getCentrality(o1)[0] < instance.getCentrality(o2)[0]){
                    res = 1;
                }
                else if(instance.getCentrality(o1)[0] > instance.getCentrality(o2)[0]) {
                    res = -1;
                }
                return res;
            }
        });
        while(!eval.isSolution(posSol)) {
            posSol.addNode(nodesByDegree.getFirst());
            nodesByDegree.removeFirst();
        }
        return posSol;
    }

    public static Solution GenerateGreedyEigenvectorSolution(Instance instance, SpreadingProcessOptimize eval) {
        Solution posSol = new Solution();
        ArrayList<Integer> nodesByDegree = instance.getNodes();
        nodesByDegree.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int res = 0;
                if(instance.getCentrality(o1)[2] < instance.getCentrality(o2)[2]){
                    res = 1;
                }
                else if(instance.getCentrality(o1)[2] > instance.getCentrality(o2)[2]) {
                    res = -1;
                }
                return res;
            }
        });
        while(!eval.isSolution(posSol)) {
            posSol.addNode(nodesByDegree.getFirst());
            nodesByDegree.removeFirst();
        }
        return posSol;
    }
}
