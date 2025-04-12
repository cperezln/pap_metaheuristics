import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String path = args[0];
        float param_alpha = Float.parseFloat(args[1]);
        int paramIter = Integer.parseInt(args[2]);
        float bet_fact = Float.parseFloat(args[3]);
        float deg_factor = Float.parseFloat(args[4]);
        float eig_factor = Float.parseFloat(args[5]);
        float aware_factor = Float.parseFloat(args[6]);
        String leafS = args[7];
        boolean leaf = false;
        if (leafS.equals("1")) {
            leaf = true;
        }
        File i = new File(path);
        // Ejecución del GRASP
        int nIterGrasp = paramIter;
        Instance.betFact = bet_fact;
        Instance.degFact = deg_factor;
        Instance.eigFact = eig_factor;
        Instance instance = new Instance(i, i.getName());
        Solution bestSolutionFound = null;
        int bestValueFound = Integer.MAX_VALUE;
        long initTime = System.nanoTime();
        for (int j = 0; j < nIterGrasp; j++) {
            SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
            Solution.instance = instance;
            Solution.awareFactor = aware_factor;
            // Fase constructiva
            Solution graspSol = new Solution();
            while (!eval.isSolution(graspSol)) {
                ArrayList<PairVal> candidateList = graspSol.candidateList();
                ArrayList<Integer> restCandidateList = new ArrayList<>();
                float rclThresh = (float) (graspSol.minVal + param_alpha * (graspSol.maxVal - graspSol.minVal));
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
        if((System.nanoTime() - initTime) / Math.pow(10, 9) > 310) {
            System.out.println(Float.MAX_VALUE);
        }
        else {
            System.out.println(bestSolutionFound.solutionValue());
        }
    }

}
