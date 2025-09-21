import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class GRASP {
    private static int getRandomPosGeom(double beta, Random rng, int n) {
        // Returns random position between 0 and n-1 based on Geometric(beta)
        int pos = (int) (Math.log(rng.nextDouble()) / Math.log(1 - beta));
        pos = pos % n;
        return pos;
    }
    public static boolean measureTimeLimit;
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
            ArrayList<PairVal> restCandidateList = new ArrayList<>();
            double rclThresh = (graspSol.minVal + alphaValue * (graspSol.maxVal - graspSol.minVal));
            for (PairVal pv : candidateList) {
                if (pv.val >= rclThresh) restCandidateList.add(pv);
            }
            restCandidateList.sort(new Comparator<PairVal>() {
                @Override
                public int compare(PairVal o1, PairVal o2) {
                    if(o1.val < o2.val) {
                        return 1;
                    } else if (o1.val > o2.val) {
                        return -1;
                    }
                    else {
                        return 0;
                    }
                }
            });
            int selectRand = getRandomPosGeom(0.35, new Random(), restCandidateList.size());
            try {
                graspSol.addNode(restCandidateList.get(selectRand).node);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            instance.resetState(graspSol);
        }

        double graspTime = (Duration.between(consStart, Instant.now())).toMillis();
        System.out.println(String.format("GRASP constructing time %f with value %d", graspTime, graspSol.solutionValue()));
        return graspSol;
    }

    public Solution improvePhase(Solution sol) {
        //sol.removeUnnedeed();
        Solution improvedSol = sol;
        if (improvedSol.solutionValue() >= 2) {
            Solution graspSolImproved = new FilterUnnecesaryNodes(improvedSol, eval).bestSolutionFound;
            LocalSearch.measureTimeLimit = measureTimeLimit;
            LocalSearch ls = new LocalSearch(graspSolImproved, eval);
            improvedSol = ls.bestSolutionFound;
            //improvedSol.removeUnnedeed();
        }
        return improvedSol;
    }
    public Solution run() {
        boolean timeLimit = false;
        int bestValueFound = Integer.MAX_VALUE;
        Solution bestSolutionFound = null;
        Instant start = Instant.now();
        for (int j = 0; j < nIters; j++) {
            if(timeLimit && measureTimeLimit) {
                break;
            }
            // Fase constructiva
            Solution graspSol = constructivePhase();
            // Fase de mejora
            instance.resetState(graspSol);
            Solution improvedSol = graspSol;
            improvedSol = improvePhase(improvedSol);
            if (Duration.between(start, Instant.now()).toMillis() >= 300000 && measureTimeLimit) {
                timeLimit = true;
            }
            if (improvedSol.solutionValue() < bestValueFound) {
                if(improvedSol.solutionValue() == 0) {
                    System.out.println("una polla");
                }
                bestSolutionFound = improvedSol;
                bestValueFound = improvedSol.solutionValue();
                //System.out.println(String.format("Improved solution with value %d and time %d", bestValueFound, Duration.between(start, Instant.now()).toMillis()));
            }

            if(j % 10 == 1) {
                System.out.println(String.format("Improved solution with with nIters %d, value %d and time %d", j, bestValueFound, Duration.between(start, Instant.now()).toMillis()));
            }
        }
        //System.out.println("TIME RUNNING " + Duration.between(start, Instant.now()).toMillis());
        return bestSolutionFound;
    }
}
