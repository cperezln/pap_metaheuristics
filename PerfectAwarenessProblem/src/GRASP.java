import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;

public class GRASP {
    private LocalSearch ls;
    private int nIters;
    private double alphaValue;
    private Instance instance;
    private SpreadingProcessOptimize eval;
    private long timeToBestMs = -1; // Time when best solution was found
    private Random random;
    // Centrality factors for thread-safe solution creation
    private double betFactor;
    private double degFactor;
    private double eigFactor;

    public GRASP(int nIters, double alphaValue, Instance instance, SpreadingProcessOptimize eval, double betFactor, double degFactor, double eigFactor) {
        this.nIters = nIters;
        this.alphaValue = alphaValue;
        this.instance = instance;
        this.eval = eval;
        this.timeToBestMs = -1;
        this.random = new Random(123); // Semilla fija para reproducibilidad
        this.betFactor = betFactor;
        this.degFactor = degFactor;
        this.eigFactor = eigFactor;
    }

    public long getTimeToBestMs() {
        return timeToBestMs;
    }

    public Solution constructivePhase() {
        Solution graspSol = new Solution();
        Instant consStart = Instant.now();

        while (!eval.isSolution(graspSol)) {
            ArrayList<PairVal> candidateList = graspSol.candidateList();
            ArrayList<Integer> restCandidateList = new ArrayList<>();
            double rclThresh = graspSol.minVal + alphaValue * (graspSol.maxVal - graspSol.minVal);
            for (PairVal pv : candidateList) {
                if (pv.val >= rclThresh) restCandidateList.add(pv.node);
            }
            int selectRand = random.nextInt(restCandidateList.size());
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

    public Solution improvePhase(Solution sol, Instant startTime, int iteration) {
        sol.removeUnnedeed();
        Solution improvedSol = sol;
        double execTime = 0;
        if (improvedSol.solutionValue() >= 2) {
            Solution graspSolImproved = new FilterUnnecesaryNodes(improvedSol, eval).bestSolutionFound;
            LocalSearch ls = new LocalSearch(graspSolImproved, eval, startTime, iteration, random);
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
        this.timeToBestMs = -1;
        for (int j = 0; j < nIters; j++) {
            if(timeLimit) {
                break;
            }
            // Fase constructiva
            Solution graspSol = constructivePhase();
            // Fase de mejora
            Solution improvedSol = graspSol;
            double execTime = 0;
            improvedSol = improvePhase(improvedSol, start, j);
            if (Duration.between(start, Instant.now()).toMillis() >= TestRunner.TIME_LIMIT_MS) {
                timeLimit = true;
            }
            if (improvedSol.solutionValue() < bestValueFound) {
                bestSolutionFound = improvedSol;
                bestValueFound = improvedSol.solutionValue();
                this.timeToBestMs = Duration.between(start, Instant.now()).toMillis();
                System.out.println(String.format("Improved solution with value %d at %dms", bestValueFound, this.timeToBestMs));
            }
        }
        System.out.println("TIME RUNNING " + Duration.between(start, Instant.now()).toMillis());
        return bestSolutionFound;
    }
}
