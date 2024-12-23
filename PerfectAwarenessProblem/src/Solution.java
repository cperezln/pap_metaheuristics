import java.util.ArrayList;
import java.util.Arrays;

public class Solution {
    // In this works, solutions are composed as a number of nodes
    private ArrayList<Integer> solution;

    public Solution(ArrayList<Integer> solution) {
        this.solution = solution;
    }

    public Solution(int[] solution) {
        this.solution =  new ArrayList<>();
        for(int i: solution) this.solution.add(i);
    }

    public boolean checkValidityOfSolution(Instance instance){
        return solution.size() < instance.getNumberNodes();
    }

    public ArrayList<Integer> getSolution() {
        return solution;
    }

    public int solutionValue() { return solution.size(); }

    public void addNode(int node) { this.solution.add(node); }

    @Override
    public String toString() {
        String s = "(";
        for(int i: solution) {
            s += Integer.toString(i) + ",";
        }
        s += ")";
        return s;
    }
}
