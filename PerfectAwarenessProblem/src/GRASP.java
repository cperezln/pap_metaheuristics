import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class GRASP {
    private LocalSearch ls;
    private int nIters;
    private double alphaValue;
    private Instance instance;
    private SpreadingProcessOptimize eval;

    public GRASP(int nIters, double alphaValue, Instance instance, SpreadingProcessOptimize eval) {
        this.nIters = nIters;
        this.alphaValue = alphaValue;
        this.instance = instance;
        this.eval = eval;
    }

    public Solution constructivePhase() {
        Solution graspSol = new Solution();
        Instant consStart = Instant.now();

        while (!eval.isSolution(graspSol)) {
            ArrayList<PairVal> candidateList = graspSol.candidateList();
            ArrayList<Integer> restCandidateList = new ArrayList<>();
            double rclThresh = (double) (graspSol.minVal + alphaValue * (graspSol.maxVal - graspSol.minVal));
            for (PairVal pv : candidateList) {
                if (pv.val >= rclThresh) restCandidateList.add(pv.node);
            }
            int selectRand = (int) (Math.random() * restCandidateList.size());
            try {
                graspSol.addNode(restCandidateList.get(selectRand));
            }
            catch (Exception e){
                System.out.println("nani");
            }
            instance.resetState(graspSol);
        }

        double graspTime = (Duration.between(consStart, Instant.now())).toMillis();
        System.out.println(String.format("GRASP constructing time %f with value %d", graspTime, graspSol.solutionValue()));
        return graspSol;
    }

    public Solution improvePhase(Solution sol) {
        sol.removeUnnedeed();
        Solution improvedSol = sol;
        double execTime = 0;
        if (improvedSol.solutionValue() >= 2) {
            Solution graspSolImproved = new FilterUnnecesaryNodes(improvedSol, eval).bestSolutionFound;
            LocalSearch ls = new LocalSearch(graspSolImproved, eval);
            improvedSol = ls.bestSolutionFound;
            improvedSol.removeUnnedeed();
        }
        return improvedSol;
    }
    public Solution run() {
        boolean timeLimit = false;
        int bestValueFound = Integer.MAX_VALUE;
        Solution bestSolutionFound = null;
        Instant start = Instant.now();
        for (int j = 0; j < nIters; j++) {
            if(timeLimit) {
                break;
            }
            // Fase constructiva
            Solution graspSol = constructivePhase();
            // Fase de mejora
            Solution improvedSol = graspSol;
            double execTime = 0;
            improvedSol = improvePhase(improvedSol);
            if (Duration.between(start, Instant.now()).toMillis() >= 300000) {
                timeLimit = true;
            }
            if (improvedSol.solutionValue() < bestValueFound) {
                bestSolutionFound = improvedSol;
                bestValueFound = improvedSol.solutionValue();
                System.out.println(String.format("Improved solution with value %d and time %f", bestValueFound, execTime));
            }
        }
        System.out.println("TIME RUNNING " + Duration.between(start, Instant.now()).toMillis());
        return bestSolutionFound;
    }
}
