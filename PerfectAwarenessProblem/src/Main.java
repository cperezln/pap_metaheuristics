import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.IntStream;

 class Pair implements Comparable<Pair>{
    int node;
    int deg;
    public Pair(int node, int deg){
        this.node = node;
        this.deg = deg;
    }


    @Override
    public int compareTo(Pair o) {
        return o.deg - this.deg;
    }
}
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void combinationUtil(int arr[], int data[], int start,
                                int end, int index, int r, ArrayList<int[]> solution) {
        // El metodo funciona, pero habría que cambiarlo de recursivo a iterativo. Además, es evidentemente muy poco óptimo en cuanto el tamaño de la solución crece mucho.
        if (index == r) {
            int[] save = new int[r];
            for (int j = 0; j < r; j++)
                save[j] = data[j];
            solution.add(save);
            return;
        }

        // replace index with all possible elements. The condition
        // "end-i+1 >= r-index" makes sure that including one element
        // at index will make a combination with remaining elements
        // at remaining positions
        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            data[index] = arr[i];
            combinationUtil(arr, data, i + 1, end, index + 1, r, solution);
        }
    }

    static void computeCombination(int arr[], int n, int r, ArrayList<int[]> solution) {
        // A temporary array to store all combination one by one
        int data[] = new int[r];

        // Print all combination using temporary array 'data[]'
        combinationUtil(arr, data, 0, n - 1, 0, r, solution);
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        // Pruebas primera versión:
        String pathInstances = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances";
        String pathSolutions = "/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/solutions";
        File dirInstances = new File(pathInstances);
        // 1. Comprobar que las soluciones previas funcionan
        boolean checkPrevSols = false;
        if (checkPrevSols) {
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
        if (bruteForce) {
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
        // 3. Generador voraz de soluciones
        /*
        No es trivial cómo generar soluciones para el problema. El PAP busca soluciones que sean perfect seed (es decir, que con el conjunto seleccionado de nodos consiga que todos los nodos estén aware)
        Está claro que soluciones triviales son todos los nodos, todos los nodos -1, todos los nodos -2,... La aproximación que voy a intertar tomar tiene un componente aleatorio,  pero también está dirigido.
        Si tomamos un conjunto aleatorio de los k primeros nodos con más centralidad por grado (es decir, mayor grado. Probablemente extensible a otros tipos de centralidades), podemos simplemente comprobar si,
        los subconjuntos aleatorios de distintos tamaños (tamaño de la semilla) son solución, entonces tendremos una solución pseudo-aleatoria. Esto habría que terminar de consultarlo con Isaac
         */
        boolean randomSolutionByDegree = false;
        if (randomSolutionByDegree) {
            for (File i : dirInstances.listFiles()) {
                //i = new File("/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/800_799_1_social_0.in");
                // i = new File("/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                PriorityQueue<Pair> nodeDeg = new PriorityQueue<>();
                for(Map.Entry<Integer, ArrayList<Integer>> entry: instance.graph.entrySet()) {
                    nodeDeg.add(new Pair(entry.getKey(), entry.getValue().size()));
                }
                int k = (int) Math.ceil(instance.getNumberNodes()/2);
                int[] setToPick = new int[k];
                for(int j = 0; j < k; j++) setToPick[j] = nodeDeg.poll().node;
                Evaluation eval = new Evaluation(instance);
                Solution sol = null;
                HashSet<Integer> visited = new HashSet<>();
                while(sol == null) {
                    int randomNumberOfNodes = (int) Math.ceil(Math.random()*k);
                    visited.add(randomNumberOfNodes);
                    ArrayList<Integer> posSol = new ArrayList<>();
                    for(int j = 0; j < randomNumberOfNodes; j++) {
                        int toAdd = setToPick[Integer.min(Integer.max((int) Math.floor(Math.random()*k) - 1, 0), k - 1)];
                        if(!posSol.contains(toAdd)) posSol.add(toAdd);
                    }
                    if(eval.isSolution(new Solution(posSol))) {
                        sol = new Solution(posSol);
                        break;
                    }
                    else if(visited.size() == k) {
                        break;
                    }
                }
                String pathRandomSols = "/home/cristian/Escritorio/TFM/pap_metaheuristics/solutions/random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if(sol == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(0);
                    writer.close();
                }
                else{
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + sol.getSolution().size() + " y con solución " + sol.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(sol.getSolution().size());
                    writer.close();
                }
            }
        }
        // 4. Generador aleatorio de soluciones (incremental)
        /*
        Vamos a desarrollar un generador aleatorio de soluciones. Lo que hará este metodo será coger nodos aleatorios, e ir metiendolos en una posible solución, hasta
        que esta sea feasible.
         */
        boolean incrementalRandomSolution = false;
        if(incrementalRandomSolution) {
            for (File i : dirInstances.listFiles()) {
                //i = new File("/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/800_799_1_social_0.in");
                // i = new File("/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                Solution sol = null;
                HashSet<Integer> visited = new HashSet<>();
                ArrayList<Integer> posSol = new ArrayList<>();
                Evaluation eval = new Evaluation(instance);
                ArrayList<Integer> nodes = instance.getNodes();
                while(sol == null) {
                    int randomIndex = Math.min((int) Math.ceil(Math.random()*nodes.size()), nodes.size() - 1);
                    int posNode = nodes.get(randomIndex);
                    if(!visited.contains(posNode)) {
                        visited.add(posNode);
                        posSol.add(posNode);
                    }
                    Solution auxSol = new Solution(posSol);
                    if(eval.isSolution(auxSol)) sol = auxSol;
                }
                String pathRandomSols = "/home/cristian/Escritorio/TFM/pap_metaheuristics/solutions/inc_random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if(sol == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(0);
                    writer.close();
                }
                else{
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + sol.getSolution().size() + " y con solución " + sol.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(sol.getSolution().size());
                    writer.close();
                }
            }

        }
        // 5. Generador aleatorio de soluciones (decremental)
        /*
        Vamos a desarrollador un generador aleatorio de soluciones. Este metodo cogerá todos los nodos de la instancia, e irá quitando nodos aleatorios hasta que dejemos
        de tener una posible solución
         */
        boolean decrementalRandomSolution = true;
        if(decrementalRandomSolution) {
            for (File i : dirInstances.listFiles()) {
                //i = new File("/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/800_799_1_social_0.in");
                // i = new File("/home/cristian/Escritorio/TFM/pap_metaheuristics/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                Solution sol = null;
                Evaluation eval = new Evaluation(instance);
                ArrayList<Integer> posSol = instance.getNodes();
                while(sol == null) {
                    int randomIndex = Math.min((int) Math.ceil(Math.random()*posSol.size()), posSol.size() - 1);
                    int posNode = posSol.get(randomIndex);
                    posSol.remove(randomIndex);
                    Solution auxSol = new Solution(posSol);
                    if(!eval.isSolution(auxSol)) {
                        auxSol.addNode(posNode);
                        sol = auxSol;
                    }
                }
                String pathRandomSols = "/home/cristian/Escritorio/TFM/pap_metaheuristics/solutions/dec_random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if(sol == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(0);
                    writer.close();
                }
                else{
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + sol.getSolution().size() + " y con solución " + sol.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(sol.getSolution().size());
                    writer.close();
                }
            }

        }

    }
}