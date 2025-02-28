import java.math.BigInteger;
import java.util.*;

public class Solution {
    // In this works, solutions are composed as a number of nodes
    // Static (general) parameters
    public static Instance instance;
    // Structure - related parameters
    private BigInteger solutionBw;
    // Metrics parameters
    private int cumCentrality = 0;
    private int solutionValue = Integer.MAX_VALUE;
    // Diffusion parameters
    private int numberAware = 0;

    // GRASP parameters
    public float minVal = Float.MAX_VALUE;
    public float maxVal = Float.MIN_VALUE;


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
        this.solutionBw = this.solutionBw.add(BigInteger.ONE.shiftLeft(node));
        this.solutionValue = this.solutionBw.bitCount();
        this.numberAware = this.solutionValue;
        instance.setState(node, 2);
        for(int neigh: instance.graph.get(node)) {
            instance.setState(neigh, 1);
        }
    }

    public void removeNode(int node) {
        this.solutionBw.clearBit(node);
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
        ArrayList<PairVal> al = new ArrayList<>();
        for (int j : instance.getNodes()) {
            if (!this.isIn(j)) {
                float nodeValue = instance.getCentrality(j)*(instance.graph.get(j).size() - getAwareNeighs(j));
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
        int numberOf = 0;
        for(int i: instance.graph.get(node)) {
            if(instance.getNodeState(i) >= 1) numberOf++;
        }
        return numberOf;
    }

    public int getSpreaderNeighs(int node) {
        int numberOf = 0;
        for(int i: instance.graph.get(node)) {
            if(instance.getNodeState(i) == 2) numberOf++;
        }
        return numberOf;
    }

    public void setAware(int nAware) { this.numberAware = nAware; }

    public int getAware() { return this.numberAware; }

    // STATIC METHODS

    public static Solution GenerateDegreeGreedySolution(Instance instance, SpreadingProcessOptimize eval) {
        Solution sol = new Solution();
        BigInteger posSol = BigInteger.ZERO;
        HashSet<Integer> inSolution = new HashSet<>();
        int[] spreadersCount = new int[instance.getNumberNodes()];
        int[] awareCount = new int[instance.getNumberNodes()];
        int awareSize = 0;
        eval.isSolution(sol);
        while (true) {
            int selectedNode = -1;
            double bestValue = Integer.MIN_VALUE;

            for (Integer j : instance.graph.keySet()) {
                if (Math.max(instance.getCentrality(j), 0.005)*Math.max(instance.graph.get(j).size() - sol.getAwareNeighs(j), 1) > bestValue && !sol.isIn(j)) {
                    selectedNode = j;
                    bestValue = Math.max(instance.getCentrality(j), 0.005)*Math.max(instance.graph.get(j).size() - sol.getAwareNeighs(j), 1);
                }
            }
            if (selectedNode != -1) {
                sol.addNode(selectedNode);
            }
            instance.resetState(sol);
            if (eval.isSolution(sol)) {
                break;
            }
        }
        return sol;
    }
}
