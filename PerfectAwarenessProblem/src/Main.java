import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
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


        for (File i : dirInstances.listFiles()) {
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
            long initTime = System.nanoTime();
            Solution bestSolutionFound = graspExec.run();
            long endTime = System.nanoTime();
            String pathRandomSols = inPath + "/solutionsv5/grasp_solutions/";
            PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
            if (bestSolutionFound == null) {
                System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                writer.println(i.getName().split("\\.")[0]);
                // Si no encontramos solución, usamos la solución trivial: semilla con todos los nodos del grafo
                writer.println(instance.getNumberNodes());
                writer.println(endTime - initTime);
                writer.close();
            } else {
                System.out.println("Instancia " + i.getName() + " con valor de la FO " + bestSolutionFound.solutionValue() + " y con solución " + bestSolutionFound);
                writer.println(i.getName().split("\\.")[0]);
                writer.println(bestSolutionFound.solutionValue());
                writer.println(bestSolutionFound);
                writer.println(endTime - initTime);
                writer.close();
            }
        }

    }
}
