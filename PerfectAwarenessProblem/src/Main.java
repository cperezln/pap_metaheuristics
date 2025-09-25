import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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

class SolutionResult {
    String fileName;
    Solution bestSolution;
    long executionTime;
    int trivialSolutionSize;

    public SolutionResult(String fileName, Solution bestSolution, long executionTime, int trivialSolutionSize) {
        this.fileName = fileName;
        this.bestSolution = bestSolution;
        this.executionTime = executionTime;
        this.trivialSolutionSize = trivialSolutionSize;
    }
}

public class Main {
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String inPath = args[0];
        int configIrace = Integer.parseInt(args[1]);
        String algorithm = args.length > 2 ? args[2] : "GRASP"; // Default to GRASP if not specified
        System.out.println("Path para guardar los resultados " + inPath);

        String pathInstances = inPath + "/previous_work/instance_test";
        String pathSolutions = inPath + "/previous_work/solutions";
        File dirInstances = new File(pathInstances);

        double paramAlpha = 0;
        double betcent = 0;
        double degcent = 0;
        double eigcent = 0;
        // Best params IRACE
        if(configIrace == 1) {
            paramAlpha = 0.2412;
            betcent = 0.4915;
            degcent = 0.9034;
            eigcent = 0.5253;
        }

        if(configIrace == 2) {
            paramAlpha = 0.2412;
            betcent = 0.4915;
            degcent = 0.9034;
            eigcent = 0.5253;
        }

        String solutionDir = algorithm.equals("ILS") ? "/solutionsv7/ils_solutions" : "/solutionsv7/grasp_solutions";
        File solutionDirectory = new File(inPath + solutionDir);

        // Check if solution directory exists
        if (!solutionDirectory.exists()) {
            System.err.println("Error: Solution directory does not exist: " + solutionDirectory.getAbsolutePath());
            System.err.println("Please create the directory or check the path configuration.");
            return;
        }

        File[] dirSolved = solutionDirectory.listFiles();
        ArrayList<String> namesFiles = new ArrayList<>();
        if (dirSolved != null) {
            for(File i: dirSolved) {
                namesFiles.add(i.getName().replace(".txt", ""));
            }
        }
        HashSet<String> solved = new HashSet<>(namesFiles);

        // Sort instances alphabetically for consistent execution order
        File[] instanceFiles = dirInstances.listFiles();
        if (instanceFiles != null) {
            Arrays.sort(instanceFiles, Comparator.comparing(File::getName));
        } else {
            System.err.println("No instance files found in directory: " + pathInstances);
            return;
        }



        // Launch each instance file sequentially
        List<SolutionResult> results = new ArrayList<>();
        for (File instanceFile : instanceFiles) {
            //if(solved.contains(instanceFile.getName())) {
            //   continue;
            //}
            if(instanceFile.getName().contains(".DS_Store")) {
                continue;
            }
            try {
                SolutionResult result = solveInstance(instanceFile, algorithm, paramAlpha, betcent, degcent, eigcent);
                results.add(result);
            } catch (Exception e) {
                System.err.println("Error processing instance " + instanceFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Print results using PrintWriter
        for (SolutionResult result : results) {
            String pathRandomSols = inPath + solutionDir + "/";
            PrintWriter writer = new PrintWriter(pathRandomSols + result.fileName + ".txt", "UTF-8");

            if (result.bestSolution == null) {
                System.out.println("No se ha encontrado solución con este método para la instancia " + result.fileName);
                writer.println(result.fileName.split("\\.")[0]);
                writer.println(result.trivialSolutionSize);
                writer.println(result.executionTime);
                writer.close();
            } else {
                System.out.println("Instancia " + result.fileName + " con valor de la FO " + result.bestSolution.solutionValue() + " y con solución " + result.bestSolution);
                writer.println(result.fileName.split("\\.")[0]);
                writer.println(result.bestSolution.solutionValue());
                writer.println(result.bestSolution);
                writer.println(result.executionTime);
                writer.close();
            }
        }

    }

    private static SolutionResult solveInstance(File instanceFile, String algorithm, double paramAlpha,
                                                       double betcent, double degcent, double eigcent) {
        // Create local copies to avoid any potential shared reference issues
        File localInstanceFile = new File(instanceFile.getAbsolutePath());
        String localAlgorithm = new String(algorithm);
        double localParamAlpha = paramAlpha;
        double localBetcent = betcent;
        double localDegcent = degcent;
        double localEigcent = eigcent;
        try {
            // Create NEW instance and evaluator for each thread - thread safe by isolation
            Instance instance = new Instance(localInstanceFile, localInstanceFile.getName());
            SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);

            // Set the static instance for Solution class
            Solution.instance = instance;

            System.out.println(String.format("------------------------ INSTANCE %s using %s ------------------------", localInstanceFile.getName(), localAlgorithm));
            Instant initTime = Instant.now();
            Solution bestSolutionFound = null;

            // NO static variables - pass parameters directly to algorithms
            if (localAlgorithm.equals("ILS")) {
                IteratedLocalSearch ilsExec = new IteratedLocalSearch(localParamAlpha, instance, eval, localBetcent, localDegcent, localEigcent);
                bestSolutionFound = ilsExec.run();
            } else {
                int nIterGrasp = 150;
                GRASP graspExec = new GRASP(nIterGrasp, localParamAlpha, instance, eval, localBetcent, localDegcent, localEigcent);
                bestSolutionFound = graspExec.run();
            }

            Instant endTime = Instant.now();
            long executionTime = Duration.between(initTime, endTime).toMillis();

            return new SolutionResult(localInstanceFile.getName(), bestSolutionFound, executionTime, instance.getNumberNodes());
        } catch (Exception e) {
            System.err.println("Error processing instance " + localInstanceFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new SolutionResult(localInstanceFile.getName(), null, 0, 0);
        }
    }


}
