import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String inPath = args[0];
        System.out.println("Path para guardar los resultados " + inPath);

        String pathInstances = inPath + "/previous_work/instances";
        String pathSolutions = inPath + "/previous_work/solutions";
        File dirInstances = new File(pathInstances);

        // Pruebas
        boolean lsGreedySolution = false;
        if(lsGreedySolution) {
            for(File i: dirInstances.listFiles()) {
                Instance instance = new Instance(i, i.getName());
                System.out.println(i.getName());
                Solution.instance = instance;
                long initTime = System.nanoTime();
                SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
                Solution sol = Solution.GenerateDegreeGreedySolution(instance, eval);
                instance.resetState(sol);
                System.out.println("First solution found: " + sol.solutionValue());
                sol.removeUnnedeed();
                System.out.println("Solution after unneeded: " + sol.solutionValue());
                sol = new FilterUnnecesaryNodes(sol, eval).bestSolutionFound;
                System.out.println("Solution after redundant: " + sol.solutionValue());
                LocalSearch ls = new LocalSearch(sol, eval);
                System.out.println("Solution after LS: " + ls.bestSolutionFound.solutionValue());
                // Solution improvedSolution = new FilterUnnecesaryNodes(ls.bestSolutionFound, eval).bestSolutionFound;
                Solution improvedSolution = ls.bestSolutionFound;
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutionsv2/betweeness_ls_greedy_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if(sol == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    // Si no encontramos solución, usamos la solución trivial: semilla con todos los nodos del grafo
                    writer.println(instance.getNumberNodes());
                    writer.println(endTime - initTime);
                    writer.close();
                }
                else{
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + improvedSolution.solutionValue() + " y con solución " + improvedSolution);
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(improvedSolution.solutionValue());
                    writer.println(endTime - initTime);
                    writer.close();
                }
            }
        }


        // Ejecución del GRASP
        for (File i : dirInstances.listFiles()) {
            int nIterGrasp = 100;
            Solution finalSol = null;
            Instance instance = new Instance(i, i.getName());
            Solution bestSolutionFound = null;
            int bestValueFound = Integer.MAX_VALUE;
            long initTime = System.nanoTime();
            for(int j = 0; j < nIterGrasp; j++) {
                SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
                Solution.instance = instance;
                // Fase constructiva
                Solution graspSol =  new Solution();
                while(!eval.isSolution(graspSol)) {
                    ArrayList<PairVal> candidateList = graspSol.candidateList();
                    ArrayList<Integer> restCandidateList = new ArrayList<>();
                    float rclThresh = (float) (graspSol.minVal + Math.random() * (graspSol.maxVal - graspSol.minVal));
                    for(PairVal pv: candidateList) {
                        if(pv.val >= rclThresh) restCandidateList.add(pv.node);
                    }
                    int selectRand = (int) (Math.random()*restCandidateList.size() + 0.5) % instance.getNumberNodes();
                    graspSol.addNode(selectRand);
                    if(finalSol == null || finalSol.solutionValue() > graspSol.solutionValue()) finalSol = graspSol;
                    instance.resetState(finalSol);
                }
                // Fase de mejora
                graspSol.removeUnnedeed();
                Solution improvedSol = graspSol;
                if(improvedSol.solutionValue() >= 2) {
                    Solution graspSolImproved = new FilterUnnecesaryNodes(graspSol, eval).bestSolutionFound;
                    LocalSearch ls = new LocalSearch(graspSolImproved, eval);
                    improvedSol = ls.bestSolutionFound;
                }
                if(improvedSol.solutionValue() < bestValueFound) {
                    bestSolutionFound = improvedSol;
                    bestValueFound = improvedSol.solutionValue();
                    System.out.println("Improved solution with value " + bestValueFound);
                }
            }
            long endTime = System.nanoTime();
            String pathRandomSols = inPath + "/solutionsv2/grasp_solutions/";
            PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
            if(bestSolutionFound == null) {
                System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                writer.println(i.getName().split("\\.")[0]);
                // Si no encontramos solución, usamos la solución trivial: semilla con todos los nodos del grafo
                writer.println(instance.getNumberNodes());
                writer.println(endTime - initTime);
                writer.close();
            }
            else{
                System.out.println("Instancia " + i.getName() + " con valor de la FO " + bestSolutionFound.solutionValue() + " y con solución " + bestSolutionFound);
                writer.println(i.getName().split("\\.")[0]);
                writer.println(bestSolutionFound.solutionValue());
                writer.println(endTime - initTime);
                writer.close();
            }

        }
    }
}
