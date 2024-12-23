import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.IntStream;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void combinationUtil(int arr[], int data[], int start,
                                int end, int index, int r, ArrayList<int[]> solution) {
        // El metodo funciona, pero habría que cambiarlo de recursivo a iterativo. Además, es evidentemente muy poco óptimo en cuanto el tamaño de la solución crece mucho.
        if (index == r)
        {
            int[] save = new int[r];
            for (int j=0; j<r; j++)
                save[j] = data[j];
            solution.add(save);
            return;
        }

        // replace index with all possible elements. The condition
        // "end-i+1 >= r-index" makes sure that including one element
        // at index will make a combination with remaining elements
        // at remaining positions
        for (int i=start; i<=end && end-i+1 >= r-index; i++)
        {
            data[index] = arr[i];
            combinationUtil(arr, data, i+1, end, index+1, r, solution);
        }
    }

    static void computeCombination(int arr[], int n, int r, ArrayList<int[]> solution)
    {
        // A temporary array to store all combination one by one
        int data[]=new int[r];

        // Print all combination using temporary array 'data[]'
        combinationUtil(arr, data, 0, n-1, 0, r, solution);
    }
    public static void main(String[] args) {
        // Pruebas primera versión:
        String pathInstances = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances";
        String pathSolutions = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/solutions";
        File dirInstances = new File(pathInstances);
        // 1. Comprobar que las soluciones previas funcionan
        boolean checkPrevSols = false;
        if(checkPrevSols) {
            ArrayList<String> notSolved = new ArrayList<>();
            int correctlySolvedProblems = 0;
            for (File i : dirInstances.listFiles()) {
                //i = new File(i); // "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/10_9_1_social_0.in"
                // i = new File("/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/10_18_2_social_0.in");
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
                    } else {
                        notSolved.add(i.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println(correctlySolvedProblems);
            if (correctlySolvedProblems == dirInstances.length()) {
                System.out.println("Todos los problemas tienen soluciones correctas");
            } else {
                for (String i : notSolved) System.out.println("Este problema no tiene solución " + i);
            }
        }
        // 2. Generador de soluciones por fuerza bruta.
        /* Como primera aproximación, vamos a hacerlo por fuerza bruta. Para esto, iteraremos por todas las posibilidades de tamaño de semilla perfecta
         (como puede ser un grafo completamente disconexo, i.e, |E| = 0, hay que comprobar hasta semillas perfecta de tamaño |V|.
         si supiésemos a ciencia cierta que son grafos no disconexos (o que tienen componentes conexas considerables, sería interesante estudiar las propiedades
         del mundo pequeño, y el análisis de los grados de los grafos. En este caso, es probable que hubiese que probar, como mucho, con log(|V|) nodos).
         En este caso, será probar todas las posibles combinaciones desde 1 hasta |V| de nodos, estudiando si alguna es solución, (n sobre i) con i en (1...|V|).
        */

        boolean bruteForce = false;
        if(bruteForce) {
            ArrayList<String> notSolved = new ArrayList<>();
            // Evidentemente, el metodo por fuerza bruta es computacionalmente irrisorio. Habría que plantear un metodo aleatorio y seguir
            File[] listFiles = dirInstances.listFiles();
            Arrays.sort(listFiles);
            for (File i : listFiles) {
                Instance instance = new Instance(i);
                System.out.println("Instance " + i);
                //Instance instance = new Instance(new File("/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/85_263_3_social_0.in"));
                int[] arr = IntStream.range(0, instance.getNumberNodes()).toArray();
                boolean foundSol = false;
                for (int solSize = 1; solSize <= instance.getNumberNodes(); solSize++) {
                    ArrayList<int[]> possibleSolutions = new ArrayList<>();
                    try {
                        computeCombination(arr, arr.length, solSize, possibleSolutions);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    for (int[] tuple : possibleSolutions) {
                        Solution posSol = new Solution(tuple);
                        Evaluation eval = new Evaluation(instance);
                        if (eval.isSolution(posSol)) {
                            System.out.println("Instancia " + i.getName() + " con solución " + posSol);
                            foundSol = true;
                            break;
                        }
                    }
                    if (foundSol) break;
                }
                break;
            }
        }
        // 3. Generador aleatorio de soluciones
        /*
        No es trivial cómo generar soluciones para el problema. El PAP busca soluciones que sean perfect seed (es decir, que con el conjunto seleccionado de nodos consiga que todos los nodos estén aware)
        Está claro que soluciones triviales son todos los nodos, todos los nodos -1, todos los nodos -2,... La aproximación que voy a intertar tomar tiene un componente aleatorio,  pero también está dirigido.
        Si tomamos un conjunto aleatorio de los k primeros nodos con más centralidad por grado (es decir, mayor grado. Probablemente extensible a otros tipos de centralidades), podemos simplemente comprobar si,
        los subconjuntos aleatorios de distintos tamaños (tamaño de la semilla) son solución, entonces tendremos una solución pseudo-aleatoria. Esto habría que terminar de consultarlo con Isaac
         */
        boolean randomSolutionByDegree = true;
        if(randomSolutionByDegree) {

        }
    }
}