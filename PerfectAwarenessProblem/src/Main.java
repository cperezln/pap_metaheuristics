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
        if (args.length < 1) {
            System.err.println("Usage: java Main <path_to_project_or_instance_file> [config_id] [algorithm]");
            System.err.println("  path_to_project_or_instance_file: Project root path OR specific .in instance file");
            System.err.println("  config_id: Configuration ID (1 or 2, default 1)");
            System.err.println("  algorithm: GRASP (default) or ILS");
            return;
        }

        String firstArg = args[0];
        int configIrace = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        String algorithm = args.length > 2 ? args[2] : "GRASP";

        // Check if first argument is a specific instance file (.in) or a project path
        File firstArgFile = new File(firstArg);
        boolean isInstanceFile = firstArg.endsWith(".in") && firstArgFile.isFile();

        if (isInstanceFile) {
            // Mode: Process single instance file
            processSingleInstance(firstArgFile, algorithm, configIrace);
        } else {
            // Mode: Process all instances in directory (original behavior)
            processAllInstances(firstArg, algorithm, configIrace);
        }
    }

    private static void processSingleInstance(File instanceFile, String algorithm, int configIrace)
            throws FileNotFoundException, UnsupportedEncodingException {

        // Get IRACE configuration parameters
        double paramAlpha = 0;
        double betcent = 0;
        double degcent = 0;
        double eigcent = 0;
         int reconstructionIterations = 1; // Default value

        // Best params IRACE
        if(configIrace == 1) {
            paramAlpha = 0.2412;
            betcent = 0.4915;
            degcent = 0.9034;
            eigcent = 0.5253;
            reconstructionIterations = 1;
        }
        if(configIrace == 2) {
            paramAlpha = 0.2412;
            betcent = 0.4915;
            degcent = 0.9034;
            eigcent = 0.5253;
            reconstructionIterations = 1;
        }

        // Determine solution directory - use current working directory with solutions subfolder
        String solutionDir = algorithm.equals("ILS") ? "solutions/ils" : "solutions/grasp";
        File solutionDirectory = new File(solutionDir);

        // Create solution directory if it doesn't exist
        if (!solutionDirectory.exists()) {
            boolean created = solutionDirectory.mkdirs();
            if (created) {
                System.out.println("Created solution directory: " + solutionDirectory.getAbsolutePath());
            } else {
                System.err.println("Warning: Could not create solution directory: " + solutionDirectory.getAbsolutePath());
            }
        }

        try {
            // Process the single instance file
            SolutionResult result = solveInstance(instanceFile, algorithm, paramAlpha, betcent, degcent, eigcent, reconstructionIterations);

            // Create output file name based on instance name and algorithm
            String instanceBaseName = instanceFile.getName().replaceFirst("\\.[^.]+$", ""); // Remove extension
            String outputFileName = algorithm.toLowerCase() + "_" + instanceBaseName + ".txt";
            String outputPath = solutionDir + "/" + outputFileName;

            // Write result to file
            PrintWriter writer = new PrintWriter(outputPath, "UTF-8");

            if (result.bestSolution == null) {
                System.out.println("No se ha encontrado solución con este método para la instancia " + result.fileName);
                writer.println(instanceBaseName);
                writer.println(result.trivialSolutionSize);
                writer.println(result.executionTime);
            } else {
                System.out.println("Instancia " + result.fileName + " con valor de la FO " + result.bestSolution.solutionValue() + " y con solución " + result.bestSolution);
                writer.println(instanceBaseName);
                writer.println(result.bestSolution.solutionValue());
                writer.println(result.bestSolution);
                writer.println(result.executionTime);
            }
            writer.close();

            System.out.println("Results written to: " + outputPath);

        } catch (Exception e) {
            System.err.println("Error processing instance " + instanceFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processAllInstances(String inPath, String algorithm, int configIrace)
            throws FileNotFoundException, UnsupportedEncodingException {

        String pathInstances = inPath + "/previous_work/instances";
        String pathSolutions = inPath + "/previous_work/solutions";
        File dirInstances = new File(pathInstances);

        // Get IRACE configuration parameters
        double paramAlpha = 0;
        double betcent = 0;
        double degcent = 0;
        double eigcent = 0;
        int reconstructionIterations = 1; // Default value

        // Best params IRACE
        if(configIrace == 1) {
            paramAlpha = 0.2412;
            betcent = 0.4915;
            degcent = 0.9034;
            eigcent = 0.5253;
            reconstructionIterations = 10; // Default for config 1
        }
        if(configIrace == 2) {
            paramAlpha = 0.2412;
            betcent = 0.4915;
            degcent = 0.9034;
            eigcent = 0.5253;
            reconstructionIterations = 10; // Default for config 2
        }

        String solutionDir = algorithm.equals("ILS") ? "/solutionsv7/ils_solutions" : "/solutionsv7/grasp_solutions";
        File solutionDirectory = new File(inPath + solutionDir);

        // Check if solution directory exists
        if (!solutionDirectory.exists()) {
            boolean created = solutionDirectory.mkdirs();
            if (created) {
                System.out.println("Created solution directory: " + solutionDirectory.getAbsolutePath());
            } else {
                System.err.println("Warning: Could not create solution directory: " + solutionDirectory.getAbsolutePath());
            }
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
            if(instanceFile.getName().contains(".DS_Store")) {
                continue;
            }
            try {
                SolutionResult result = solveInstance(instanceFile, algorithm, paramAlpha, betcent, degcent, eigcent, reconstructionIterations);
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
                                                       double betcent, double degcent, double eigcent, int reconstructionIterations) {
        // Create local copies to avoid any potential shared reference issues
        File localInstanceFile = new File(instanceFile.getAbsolutePath());
        String localAlgorithm = new String(algorithm);
        double localParamAlpha = paramAlpha;
        double localBetcent = betcent;
        double localDegcent = degcent;
        double localEigcent = eigcent;
        int localReconstructionIterations = reconstructionIterations;
        try {
            // Create NEW instance and evaluator for each thread - thread safe by isolation
            Instance instance = new Instance(localInstanceFile, localInstanceFile.getName());

            // Set the static instance for Solution class
            Solution.instance = instance;

            System.out.println(String.format("------------------------ INSTANCE %s using %s ------------------------", localInstanceFile.getName(), localAlgorithm));
            Instant initTime = Instant.now();
            SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance, initTime);
            Solution bestSolutionFound = null;

            // NO static variables - pass parameters directly to algorithms
            if (localAlgorithm.equals("ILS")) {
                IteratedLocalSearch ilsExec = new IteratedLocalSearch(localParamAlpha, instance, eval, localBetcent, localDegcent, localEigcent, localReconstructionIterations);
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
