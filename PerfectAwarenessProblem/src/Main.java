import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.ArrayList;
import java.util.HashSet;

class PairVal implements Comparable<PairVal>{
    int node;
    double val;
    public PairVal(int node, double val){
        this.node = node;
        this.val = val;
    }


    @Override
    public int compareTo(PairVal o) {
        if(o.val - this.val < 0) return -1;
        else return 1;
    }
}

public class Main {
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String inPath = args[0];
        System.out.println("Path para guardar los resultados " + inPath);

        String pathInstances = inPath + "/previous_work/instances";
        String pathSolutions = inPath + "/previous_work/solutions";
        File dirInstances = new File(pathInstances);

        // Best params IRACE
        double paramAlpha = 0.6725;
        int graspIters = 72;
        double betcent = 0.4636;
        double degcent = 0.0043;
        double eigcent = 0.9650;
        double awareFact = 0.7777;

        File[] dirSolved = new File(inPath + "/solutionsv6/grasp_solutions").listFiles();
        ArrayList<String> namesFiles = new ArrayList<>();
        for(File i: dirSolved) {
            namesFiles.add(i.getName().replace(".txt", ""));
        }
        HashSet<String> solved = new HashSet<>(namesFiles);
        for (File i : dirInstances.listFiles()) {
            if(solved.contains(i.getName())) {
                System.out.println("Computed");
               continue;
            }
            // i = new File(pathInstances + "/10_9_1_social_0.in");
            int nIterGrasp = graspIters;
            Solution.awareFactor = awareFact;
            Solution.betFactor = betcent;
            Solution.degFactor = degcent;
            Solution.eigFactor = eigcent;
            Instance instance = new Instance(i, i.getName());
            SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
            Solution.instance = instance;
            GRASP graspExec = new GRASP(nIterGrasp, paramAlpha, instance, eval);
            System.out.println(String.format("------------------------ INSTANCE %s ------------------------", i.getName()));
            Instant initTime = Instant.now();
            Solution bestSolutionFound = graspExec.run();
            Instant endTime = Instant.now();
            String pathRandomSols = inPath + "/solutionsv6/grasp_solutions/";
            PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
            System.out.println(bestSolutionFound.solutionValue());

        }
    }
}
