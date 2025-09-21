import java.io.*;
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
        int configIrace = Integer.parseInt(args[1]);
        int algorithm = Integer.parseInt(args[2]);
        int identifier = Integer.parseInt(args[3]);
        System.out.println("Path para guardar los resultados " + inPath);

        String pathInstances = inPath + "/previous_work/instances";
        String pathSolutions = inPath + "/previous_work/solutions";
        File dirInstances = new File(pathInstances);

        boolean convergencNItersEvaluation = false;
        boolean graspEvaluation = false;
        boolean constructiveRandom = false;
        boolean constructiveGreedy = false;
        boolean instanceAnalysis = false;

        switch (algorithm) {
            case 1:
                convergencNItersEvaluation = true;
                break;
            case 2:
                graspEvaluation = true;
                break;
            case 3:
                constructiveRandom = true;
                break;
            case 4:
                instanceAnalysis = true;
                break;
            case 5:
                constructiveGreedy = true;
        }
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

        // Analysis of the instance sizes with the preprocessings
        if(instanceAnalysis){
            File[] dirSolved = new File(inPath + "/solutionsFinal/instanceAnalysisOfSizes/").listFiles();
            ArrayList<String> namesFiles = new ArrayList<>();
            for (File i : dirSolved) {
                namesFiles.add(i.getName().replace(".txt", ""));
            }
            HashSet<String> solved = new HashSet<>(namesFiles);
            for (File i : dirInstances.listFiles()) {
                if (solved.contains(i.getName())) {
                    System.out.println("Computed");
                    continue;
                }
                String pathRandomSols = inPath + "/solutionsFinal/instanceAnalysisOfSizes/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                Instance instance = new Instance(i, i.getName());
                double nodeReduction = instance.preproNodeReduction();
                double edgeReduction = instance.preproEdgeReduction();
                double leafReduction = instance.proportionLeaf();
                System.out.println("Instancia " + i.getName() + " con reducción de nodos " + nodeReduction + ", reducción de aristas " + edgeReduction + " y reducción en nodos hoja " + leafReduction);
                writer.println(i.getName().split("\\.")[0]);
                writer.println(nodeReduction);
                writer.println(edgeReduction);
                writer.println(leafReduction);
                writer.close();
            }
        }
        // Constructive random
        if(constructiveRandom) {
            int numberIters = 100;
            File[] dirSolved = null;
            if(numberIters > 1) {
                dirSolved = new File(String.format(inPath + "/solutionsFinal/%drandomConstructive/", numberIters)).listFiles();
            }
            else {
                dirSolved = new File(inPath + "/solutionsFinal/randomConstructive/").listFiles();
            }
            ArrayList<String> namesFiles = new ArrayList<>();
            for (File i : dirSolved) {
                namesFiles.add(i.getName().replace(".txt", ""));
            }
            File instances = new File("/home/cristian/Escritorio/TFM/sample.txt");
            Scanner fr = new Scanner(instances);
            ArrayList<File> sample = new ArrayList<>();
            while(fr.hasNext()) {
                String line = fr.nextLine();
                sample.add(new File(pathInstances + "/" + line));
            }
            boolean incremental = true;
            HashSet<String> solved = new HashSet<>(namesFiles);
            for (File i : sample) {
                if (solved.contains(i.getName())) {
                    System.out.println("Computed");
                    continue;
                }
                Instance instance = new Instance(i, i.getName());
                SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
                Solution.instance = instance;
                System.out.println(String.format("------------------------ INSTANCE %s ------------------------", i.getName()));
                Instant initTime = Instant.now();
                ArrayList<Solution> bestsSolutions = new ArrayList<>();
                Solution bestSolutionFound = null;
                for(int j = 0; j < numberIters; j++) {
                    if (incremental) {
                        bestSolutionFound = Solution.GenerateIncrementalRandomSolution(instance, eval);
                        bestsSolutions.add(bestSolutionFound);
                    } else {
                        bestSolutionFound = Solution.GenerateDecrementalRandomSolution(instance, eval);
                        bestsSolutions.add(bestSolutionFound);
                    }
                }
                if(numberIters == 1) {
                    bestSolutionFound = bestsSolutions.get(0);
                }
                Instant endTime = Instant.now();
                String pathRandomSols = "";
                if(numberIters > 1) {
                    pathRandomSols = String.format(inPath + "/solutionsFinal/%drandomConstructive/", numberIters);
                }
                else {
                    pathRandomSols = inPath + "/solutionsFinal/randomConstructive/";
                }
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if (bestSolutionFound == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    // Si no encontramos solución, usamos la solución trivial: semilla con todos los nodos del grafo
                    writer.println(instance.getNumberNodes());
                    writer.println(Duration.between(initTime, endTime).toMillis());
                    writer.close();
                } else {
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + bestSolutionFound.solutionValue() + " y con solución " + bestSolutionFound);
                    writer.println(i.getName().split("\\.")[0]);
                    if(numberIters > 1) {
                        float meanSolValue = 0;
                        for(Solution sol: bestsSolutions) meanSolValue += sol.solutionValue();
                        meanSolValue /= numberIters;
                        writer.println(meanSolValue);
                    }
                    else {
                        writer.println(bestSolutionFound.solutionValue());
                    }
                    writer.println(bestSolutionFound);
                    writer.println(Duration.between(initTime, endTime).toMillis());
                    writer.close();
                }
            }
        }

        // Constructive greedy
        if(constructiveGreedy) {
            String type = "";
            switch (configIrace) {
                case 1:
                    type = "Degree";
                    break;
                case 2:
                    type = "Betweeness";
                    break;
                case 3:
                    type = "Eigenvector";
                    break;
            }
            File[] dirSolved = new File(inPath + "/solutionsFinal/greedy" + type + "/").listFiles();
            ArrayList<String> namesFiles = new ArrayList<>();
            for (File i : dirSolved) {
                namesFiles.add(i.getName().replace(".txt", ""));
            }
            File instances = new File("/home/cristian/Escritorio/TFM/sample.txt");
            Scanner fr = new Scanner(instances);
            ArrayList<File> sample = new ArrayList<>();
            while(fr.hasNext()) {
                String line = fr.nextLine();
                sample.add(new File(pathInstances + "/" + line));
            }
            HashSet<String> solved = new HashSet<>(namesFiles);
            for (File i : sample) {
                if (solved.contains(i.getName())) {
                    System.out.println("Computed");
                    continue;
                }
                Instance instance = new Instance(i, i.getName());
                SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
                Solution.instance = instance;
                System.out.println(String.format("------------------------ INSTANCE %s ------------------------", i.getName()));
                Instant initTime = Instant.now();
                Solution bestSolutionFound = null;
                if(configIrace == 1){
                    bestSolutionFound = Solution.GenerateGreedyDegreeSolution(instance, eval);
                }
                else if(configIrace == 2) {
                    bestSolutionFound = Solution.GenerateGreedyBetweenessSolution(instance, eval);
                }
                else {
                    bestSolutionFound = Solution.GenerateGreedyEigenvectorSolution(instance, eval);
                }
                Instant endTime = Instant.now();
                String pathRandomSols = inPath + "/solutionsFinal/greedy" + type + "/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if (bestSolutionFound == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    // Si no encontramos solución, usamos la solución trivial: semilla con todos los nodos del grafo
                    writer.println(instance.getNumberNodes());
                    writer.println(Duration.between(initTime, endTime).toMillis());
                    writer.close();
                } else {
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + bestSolutionFound.solutionValue() + " y con solución " + bestSolutionFound);
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(bestSolutionFound.solutionValue());
                    writer.println(bestSolutionFound);
                    writer.println(Duration.between(initTime, endTime).toMillis());
                    writer.close();
                }
            }
        }
        // Measure time limit
        boolean measureTimeLimit = true;

        // GRASP nIters convergence evaluation
        if(convergencNItersEvaluation) {
            boolean finished = false;
            while(!finished) {
                File[] dirSolved = new File(inPath + String.format("/solutionsFinal/%d_convergenceGraspSolutionsConfig_%d/nIters_%d", identifier, configIrace, 151)).listFiles();
                ArrayList<String> namesFiles = new ArrayList<>();
                for (File i : dirSolved) {
                    namesFiles.add(i.getName().replace(".txt", ""));
                }
                File instances = new File("/home/cristian/Escritorio/TFM/sample.txt");
                Scanner fr = new Scanner(instances);
                ArrayList<File> sample = new ArrayList<>();
                while(fr.hasNext()) {
                    String line = fr.nextLine();
                    sample.add(new File(pathInstances + "/" + line));
                }
                HashSet<String> solved = new HashSet<>(namesFiles);
                for (File i : sample) {
                    if (solved.contains(i.getName())) {
                        System.out.println("Computed");
                        continue;
                    }
                    Solution.betFactor = betcent;
                    Solution.degFactor = degcent;
                    Solution.eigFactor = eigcent;
                    Instance instance = new Instance(i, i.getName());
                    SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
                    Solution.instance = instance;
                    measureTimeLimit = true;
                    GRASP.measureTimeLimit = measureTimeLimit;
                    GRASP graspExec = new GRASP(151, paramAlpha, instance, eval);
                    System.out.println(String.format("------------------------ INSTANCE %s ------------------------", i.getName()));
                    Instant initTime = Instant.now();
                    Solution bestSolutionFound = graspExec.run();
                    Instant endTime = Instant.now();
                    String pathRandomSols = inPath + String.format("/solutionsFinal/%d_convergenceGraspSolutionsConfig_%d/nIters_%d", identifier, configIrace, 151);
                    PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                    if (bestSolutionFound == null) {
                        System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                        writer.println(i.getName().split("\\.")[0]);
                        // Si no encontramos solución, usamos la solución trivial: semilla con todos los nodos del grafo
                        writer.println(instance.getNumberNodes());
                        writer.println(Duration.between(initTime, endTime).toMillis());
                        writer.close();
                    } else {
                        System.out.println("Instancia " + i.getName() + " con valor de la FO " + bestSolutionFound.solutionValue() + " y con solución " + bestSolutionFound);
                        writer.println(i.getName().split("\\.")[0]);
                        writer.println(bestSolutionFound.solutionValue());
                        writer.println(bestSolutionFound);
                        writer.println(Duration.between(initTime, endTime).toMillis());
                        writer.close();
                    }
                }
                finished = true;
            }
        }

        // GRASP evaluation
        if(graspEvaluation) {
            File[] dirSolved = new File(inPath + "/solutionPostTfm/biasedGrasp_0.35/").listFiles();
            ArrayList<String> namesFiles = new ArrayList<>();
            for (File i : dirSolved) {
                namesFiles.add(i.getName().replace(".txt", ""));
            }
            /*
            File instances = new File("/home/cristian/Escritorio/TFM/sample.txt");
            Scanner fr = new Scanner(instances);
            ArrayList<File> sample = new ArrayList<>();
            while(fr.hasNext()) {
                String line = fr.nextLine();
                sample.add(new File(pathInstances + "/" + line));
            }
            */
            File[] instances = new File(inPath + "/previous_work/instances").listFiles();
            HashSet<String> solved = new HashSet<>(namesFiles);
            for (File i : instances) {
                if (solved.contains(i.getName())) {
                    System.out.println("Computed");
                    continue;
                }
                // i = new File(pathInstances + "/10_9_1_social_0.in");
                int nIterGrasp = 80;
                Solution.betFactor = betcent;
                Solution.degFactor = degcent;
                Solution.eigFactor = eigcent;
                Instance instance = new Instance(i, i.getName());
                SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
                Solution.instance = instance;
                GRASP.measureTimeLimit = true;
                GRASP graspExec = new GRASP(nIterGrasp, paramAlpha, instance, eval);
                System.out.println(String.format("------------------------ INSTANCE %s ------------------------", i.getName()));
                Instant initTime = Instant.now();
                Solution bestSolutionFound = graspExec.run();
                Instant endTime = Instant.now();
                String pathRandomSols = inPath + "/solutionPostTfm/biasedGrasp_0.35/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if (bestSolutionFound == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    // Si no encontramos solución, usamos la solución trivial: semilla con todos los nodos del grafo
                    writer.println(instance.getNumberNodes());
                    writer.println(Duration.between(initTime, endTime).toMillis());
                    writer.close();
                } else {
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + bestSolutionFound.solutionValue() + " y con solución " + bestSolutionFound);
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(bestSolutionFound.solutionValue());
                    writer.println(bestSolutionFound);
                    writer.println(Duration.between(initTime, endTime).toMillis());
                    writer.close();
                }
            }
        }

    }
}
