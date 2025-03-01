import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
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
                //i = new File(pathInstances + "/1000_999_1_social_0.in");
                Instance instance = new Instance(i, i.getName());
                System.out.println(i.getName());
                Solution.instance = instance;
                long initTime = System.nanoTime();
                SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
                Solution sol = Solution.GenerateDegreeGreedySolution(instance, eval);
                System.out.println("First solution found: " + sol.solutionValue());
                sol.removeUnnedeed();
                System.out.println("Solution after unneeded: " + sol.solutionValue());
                sol = new FilterUnnecesaryNodes(sol, eval).bestSolutionFound;
                System.out.println("Solution after redundant: " + sol.solutionValue());
                LocalSearch ls = new LocalSearch(sol, eval);
                System.out.println("Solution after LS: " + ls.bestSolutionFound.solutionValue());
                //Solution improvedSolution = new FilterUnnecesaryNodes(ls.bestSolutionFound, eval).bestSolutionFound;
                Solution improvedSolution = ls.bestSolutionFound;
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutionsv2/betweeness_deg_ls_greedy_solutions/";
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

        boolean grasp = true;
        if(grasp) {
            // Ejecución del GRASP
            for (File i : dirInstances.listFiles()) {
                int nIterGrasp = 50;
                Solution finalSol = null;
                Instance instance = new Instance(i, i.getName());
                System.out.println(String.format("------------------------ INSTANCE %s ------------------------", i.getName()));
                Solution bestSolutionFound = null;
                int bestValueFound = Integer.MAX_VALUE;
                long initTime = System.nanoTime();
                boolean breakFor = false;
                for (int j = 0; j < nIterGrasp; j++) {
                    if(breakFor) break;
                    SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
                    Solution.instance = instance;
                    // Fase constructiva
                    Solution graspSol = new Solution();
                    long constructedTime = System.nanoTime();
                    while (!eval.isSolution(graspSol)) {
                        // REVISAR UNA SOLUCIÓN PARECIDA A LA QUE TIENEN ELLOS PARA EL CANDIDATE LIST, PORQUE NO PUEDE SER QUE DEL ALGORITMO SE LLEVE LA MITAD DEL TIEMPO
                        // DARLE UNA VUELTA A LAS APROXIMACIONES QUE MEJORAN EL ALPHA EN CADA ITERACIÓN, PUEDEN SER INTERESANTES
                        // TAMBIÉN ES POSIBLE PLANTEAR LA ENCAPSULACIÓN DEL GRASP EN UN ITERATED GREEDY, PARA TENER LÍMITES PARA LOS TIEMPOS. HABLARLO CON ISAAC

                        
                        ArrayList<PairVal> candidateList = graspSol.candidateList();
                        ArrayList<Integer> restCandidateList = new ArrayList<>();
                        float rclThresh = (float) (graspSol.minVal + Math.random() * (graspSol.maxVal - graspSol.minVal));
                        for (PairVal pv : candidateList) {
                            if (pv.val >= rclThresh) restCandidateList.add(pv.node);
                        }
                        int selectRand = (int) (Math.random() * restCandidateList.size() + 0.5) % instance.getNumberNodes();
                        graspSol.addNode(selectRand);
                        if (finalSol == null || finalSol.solutionValue() > graspSol.solutionValue())
                            finalSol = graspSol;
                        instance.resetState(finalSol);
                    }
                    double graspTime = (System.nanoTime() - constructedTime)/Math.pow(10,9);
                    breakFor = graspTime >= 60;
                    System.out.println(String.format("GRASP constructing time %f with value %d", graspTime, finalSol.solutionValue()));
                    long innerInitTime = System.nanoTime();
                    // Fase de mejora
                    graspSol.removeUnnedeed();
                    Solution improvedSol = graspSol;
                    double execTime = 0;
                    if (improvedSol.solutionValue() >= 2) {
                        Solution graspSolImproved = new FilterUnnecesaryNodes(graspSol, eval).bestSolutionFound;
                        LocalSearch ls = new LocalSearch(graspSolImproved, eval);
                        improvedSol = ls.bestSolutionFound;
                        improvedSol.removeUnnedeed();
                        long innerEndTime = System.nanoTime();
                        execTime = (innerEndTime - innerInitTime)/Math.pow(10, 9);
                        if(execTime >= 60) {
                            breakFor = true;
                        }

                    }
                    if (improvedSol.solutionValue() < bestValueFound) {
                        bestSolutionFound = improvedSol;
                        bestValueFound = improvedSol.solutionValue();
                        System.out.println(String.format("Improved solution with value %d and time %f", bestValueFound, execTime));
                    }
                }
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutionsv2/grasp_solutions/";
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
                    writer.println(endTime - initTime);
                    writer.close();
                }

            }
        }

        boolean checkSolution = false;
        if(checkSolution) {
            File i = new File(pathInstances + "/1000_999_1_social_0.in");
            Instance ins = new Instance(i, i.getName());
            String sol1 = "(0, 2, 3, 4, 5, 10, 11, 12, 14, 17, 20, 23, 26, 33, 42, 49, 52, 54, 59, 60, 67, 69, 71, 75, 82, 85, 88, 92, 94, 99, 112, 121, 132, 150, 154, 157, 158, 171, 174, 176, 202, 205, 215, 218, 223, 232, 233, 234, 241, 242, 253, 255, 258, 272, 298, 312, 314, 315, 320, 321, 322, 328, 349, 364, 373, 386, 421, 436, 444, 447, 477, 478, 506, 530, 540, 560, 581, )";
            String sol = "(0, 2, 3, 4, 5, 10, 11, 12, 14, 17, 20, 23, 26, 33, 37, 42, 49, 52, 54, 56, 59, 60, 65, 67, 69, 71, 75, 82, 85, 88, 92, 94, 99, 108, 112, 121, 132, 150, 154, 157, 158, 167, 171, 174, 176, 202, 205, 215, 218, 223, 232, 233, 234, 241, 242, 253, 255, 258, 272, 298, 312, 314, 315, 320, 321, 322, 328, 349, 364, 373, 386, 421, 436, 444, 447, 478, 506, 530, 540, 560, 573, 581, )";
            String[] solSplt = sol.replace("(", "").replace(")", "").split(", ");
            ArrayList<Integer> posSol = new ArrayList<>();
            for(String s: solSplt) {
                posSol.add(Integer.parseInt(s));
            }
            BigInteger solutionbw = BigInteger.ZERO;
            for(int node: posSol) solutionbw = solutionbw.setBit(node);
            SpreadingProcessCheck eval = new SpreadingProcessCheck(ins);
            SpreadingProcessOptimize eval2 = new SpreadingProcessOptimize(ins);
            System.out.println(eval.isSolution(posSol));
            System.out.println(eval2.isSolution(new Solution(solutionbw)));
        }
    }
}
