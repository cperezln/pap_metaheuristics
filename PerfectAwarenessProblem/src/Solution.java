import java.util.ArrayList;

public class Solution {
    // In this works, solutions are composed as a number of nodes
    private ArrayList<Integer> solution;

    public Solution(ArrayList<Integer> solution) {
        this.solution = solution;
    }

    public boolean checkValidityOfSolution(Instance instance){
        return solution.size() < instance.getNumberNodes();
    }

    public ArrayList<Integer> getSolution() {
        return solution;
    }

    public int solutionValue() { return solution.size(); }
}
