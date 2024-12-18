import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // Pruebas primera versión:
        // 1. Comprobar que las soluciones previas funcionan
        String pathInstances = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances";
        String pathSolutions = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/solutions";
        File dirInstances = new File(pathInstances);
        ArrayList<String> notSolved = new ArrayList<>();
        int correctlySolvedProblems = 0;
        for (File i : dirInstances.listFiles()) {
            //i = new File(i); // "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/10_9_1_social_0.in"
            Instance instance = new Instance(i);
            String aux = i.getName();
            String[] auxs = aux.split("\\.");
            File solution = new File(pathSolutions + "/" + i.getName().split("\\.")[0] + ".sol");
            try {
                Scanner reader = new Scanner(solution);
                int value = reader.nextInt();
                ArrayList<Integer> nodes = new ArrayList<>();
                for (int numberNodes = 0; numberNodes < value; numberNodes++) {
                    nodes.add(reader.nextInt());
                }
                Evaluation eval = new Evaluation(instance);
                Solution possibleSolution = new Solution(nodes);
                if (eval.isSolution(possibleSolution) && possibleSolution.solutionValue() == value) {
                    correctlySolvedProblems++;
                }
                else {
                    notSolved.add(i.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(correctlySolvedProblems);
        if (correctlySolvedProblems == dirInstances.length()) {
            System.out.println("Todos los problemas tienen soluciones correctas");
        }
        else {
            for(String i: notSolved) System.out.println("Este problema no tiene solución " + i);
        }
    }
}