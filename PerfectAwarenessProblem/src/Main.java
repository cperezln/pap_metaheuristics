import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String path = args[0];
        int paramIter = Integer.parseInt(args[1]);
        float lambda = Float.parseFloat(args[2]);
        boolean leaf = false;
        File i = new File(path);
        // Ejecución del GRASP
        int nIterGrasp = paramIter;
        Instance instance = new Instance(i, i.getName());
        Solution bestSolutionFound = null;
        int bestValueFound = Integer.MAX_VALUE;
        long initTime = System.nanoTime();
        for (int j = 0; j < nIterGrasp; j++) {
            SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
            Solution.lambda = lambda;
            Solution.instance = instance;
            // Fase constructiva
            Solution graspSol = new Solution();
            while (!eval.isSolution(graspSol)) {
                ArrayList<PairVal> candidateList = graspSol.candidateList();
                ArrayList<Integer> restCandidateList = new ArrayList<>();
                float rclThresh = (float) (graspSol.minVal + Math.random() * (graspSol.maxVal - graspSol.minVal));
                for (PairVal pv : candidateList) {
                    if (pv.val >= rclThresh) {
                        if(leaf && instance.leafNodes.contains(pv.node)) {
                            continue;
                        }
                        else {
                            restCandidateList.add(pv.node);
                        }
                    }
                }
                int selectRand = (int) (Math.random() * restCandidateList.size());
                graspSol.addNode(restCandidateList.get(selectRand));
                instance.resetState(graspSol);
            }
            // Fase de mejora
            graspSol.removeUnnedeed();
            Solution improvedSol = graspSol;
            if (improvedSol.solutionValue() >= 2) {
                Solution graspSolImproved = new FilterUnnecesaryNodes(graspSol, eval).bestSolutionFound;
                LocalSearch ls = new LocalSearch(graspSolImproved, eval);
                improvedSol = ls.bestSolutionFound;
                improvedSol.removeUnnedeed();

            }
            if (improvedSol.solutionValue() < bestValueFound) {
                bestSolutionFound = improvedSol;
                bestValueFound = improvedSol.solutionValue();
            }
        }
        System.out.println(bestSolutionFound.solutionValue());

    }

}
