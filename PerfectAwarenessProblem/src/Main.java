import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

class PairDeg implements Comparable<PairDeg>{
    int node;
    int deg;
    public PairDeg(int node, int deg){
        this.node = node;
        this.deg = deg;
    }


    @Override
    public int compareTo(PairDeg o) {
        return o.deg - this.deg;
    }
}

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
        String inPath = args[0];
        System.out.println("Path para guardar los resultados " + inPath);
        boolean bruteForce = false;
        boolean randomSolutionByDegree = false;
        boolean greedySolution = false;
        boolean incrementalRandomSolution = false;
        boolean decrementalRandomSolution = false;
        boolean incrementalMultipleRandomSolutions = false;
        boolean decrementalMultipleRandomSolutions = false;
        boolean lsIncrementalRandomSolution = false;
        boolean lsIncrementalMultipleRandomSolutions = false;
        boolean lsGreedySolution = false;

        switch (Integer.parseInt(args[1])) {
            case 1:
                bruteForce = true;
                System.out.println("Se ha seleccionado la generación de soluciones por fuerza bruta");
                break;
            case 2:
                randomSolutionByDegree = true;
                System.out.println("Se ha seleccionado la generación de soluciones por generación aleatoria");
                break;
            case 3:
                greedySolution = true;
                System.out.println("Se ha seleccionado la generación de soluciones por aproximación voraz");
                break;
            case 4:
                incrementalRandomSolution = true;
                System.out.println("Se ha seleccionado la generación de soluciones por generación aleatoria incremental");
                break;
            case 5:
                decrementalRandomSolution = true;
                System.out.println("Se ha seleccionado la generación de soluciones por generación aleatoria decremental");
                break;
            case 6:
                incrementalMultipleRandomSolutions = true;
                System.out.println("Se ha seleccionado la generación de soluciones por generación múltiple aleatoria incremental");
                break;
            case 7:
                decrementalMultipleRandomSolutions = true;
                System.out.println("Se ha seleccionado la generación de soluciones por generación múltiple aleatoria decremental");
                break;
            case 8:
                lsIncrementalRandomSolution = true;
                System.out.println("Se ha seleccionado la generación de soluciones por generación aleatoria incremental con búsqueda local");
                break;
            case 9:
                lsIncrementalMultipleRandomSolutions = true;
                System.out.println("Se ha seleccionado la generación de soluciones por generación múltiple aleatoria incremental con búsqueda local");
                break;
            case 10:
                lsGreedySolution = true;
                System.out.println("Se ha seleccionado la generación de soluciones por aproximación voraz con búsqueda local");
                break;
            case 0:
                System.out.println("No se ha seleccionado ningún generador de soluciones");
                break;
        }
        // Pruebas primera versión:
        String pathInstances = inPath + "/previous_work/instances";
        String pathSolutions = inPath + "/previous_work/solutions";
        File dirInstances = new File(pathInstances);

        // 1. Comprobar que las soluciones previas funcionan
        boolean checkPrevSols = false;
        if (checkPrevSols) {
            ArrayList<String> notSolved = new ArrayList<>();
            int correctlySolvedProblems = 0;
            for (File i : dirInstances.listFiles()) {
                //i = new File(i); // inPath + "/previous_work/instances/10_9_1_social_0.in"
                // i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
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

        if (bruteForce) {
            ArrayList<String> notSolved = new ArrayList<>();
            // Evidentemente, el metodo por fuerza bruta es computacionalmente irrisorio. Habría que plantear un metodo aleatorio y seguir
            File[] listFiles = dirInstances.listFiles();
            Arrays.sort(listFiles);
            for (File i : listFiles) {
                long initTime = System.nanoTime();
                Instance instance = new Instance(i);
                System.out.println("Instance " + i);
                Evaluation eval = new Evaluation(instance);
                Solution sol = Solution.GenerateBruteForce(instance, eval);
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutions/random_solutions/";
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
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + sol.getSolution().size() + " y con solución " + sol.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(sol.getSolution().size());
                    writer.println(endTime - initTime);
                    writer.close();
                }
            }
        }
        // 3. Generador voraz-aleatorio de soluciones
        /*
        No es trivial cómo generar soluciones para el problema. El PAP busca soluciones que sean perfect seed (es decir, que con el conjunto seleccionado de nodos consiga que todos los nodos estén aware)
        Está claro que soluciones triviales son todos los nodos, todos los nodos -1, todos los nodos -2,... La aproximación que voy a intertar tomar tiene un componente aleatorio,  pero también está dirigido.
        Si tomamos un conjunto aleatorio de los k primeros nodos con más centralidad por grado (es decir, mayor grado. Probablemente extensible a otros tipos de centralidades), podemos simplemente comprobar si,
        los subconjuntos aleatorios de distintos tamaños (tamaño de la semilla) son solución, entonces tendremos una solución pseudo-aleatoria. Esto habría que terminar de consultarlo con Isaac
         */
        if (randomSolutionByDegree) {
            for (File i : dirInstances.listFiles()) {
                //i = new File(inPath + "/previous_work/instances/800_799_1_social_0.in");
                // i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                long initTime = System.nanoTime();
                Evaluation eval = new Evaluation(instance);
                Solution sol = Solution.GenerateRandomSolutionDegree(instance, eval);
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutions/random_solutions/";
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
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + sol.getSolution().size() + " y con solución " + sol.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(sol.getSolution().size());
                    writer.println(endTime - initTime);
                    writer.close();
                }
            }
        }
        // 3.5. Generador voraz (centralidad por grado) de soluciones
        /*
        Análogo al anterior, quitando la componente aleatoria. TODO a esto añadir algo más de información: la CL en el GRASP sería útil considerarla como dg(v)*0.5 - n_i(v)
        es decir, el threshold del nodo menos el número de spreaders que tiene alrededor. Esto es más información que sería útil tener.
        */
        if(greedySolution) {
            for(File i: dirInstances.listFiles()) {
                //i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                long initTime = System.nanoTime();
                Evaluation eval = new Evaluation(instance);
                Solution.instance = instance;
                Solution sol = Solution.GenerateDegreeGreedySolution(instance, eval);
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutions/greedy_solutions/";
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
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + sol.getSolution().size() + " y con solución " + sol.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(sol.getSolution().size());
                    writer.println(endTime - initTime);
                    writer.close();
                }
            }
        }
        // 4. Generador aleatorio de soluciones (incremental)
        /*
        Vamos a desarrollar un generador aleatorio de soluciones. Lo que hará este metodo será coger nodos aleatorios, e ir metiendolos en una posible solución, hasta
        que esta sea feasible.
         */
        if(incrementalRandomSolution) {
            for (File i : dirInstances.listFiles()) {
                //i = new File(inPath + "/previous_work/instances/800_799_1_social_0.in");
                // i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                long initTime = System.nanoTime();
                Evaluation eval = new Evaluation(instance);
                Solution sol = Solution.GenerateIncrementalRandomSolution(instance, eval);
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutions/inc_random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if(sol == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(instance.getNumberNodes());
                    writer.println(endTime - initTime);
                    writer.close();
                }
                else{
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + sol.getSolution().size() + " y con solución " + sol.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(sol.getSolution().size());
                    writer.println(endTime - initTime);
                    writer.close();
                }
            }

        }
        // 5. Generador aleatorio de soluciones (decremental)
        /*
        Vamos a desarrollador un generador aleatorio de soluciones. Este metodo cogerá todos los nodos de la instancia, e irá quitando nodos aleatorios hasta que dejemos
        de tener una posible solución
         */
        if(decrementalRandomSolution) {
            for (File i : dirInstances.listFiles()) {
                //i = new File(inPath + "/previous_work/instances/800_799_1_social_0.in");
                // i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                long initTime = System.nanoTime();
                Evaluation eval = new Evaluation(instance);
                Solution sol = Solution.GenerateDecrementalRandomSolution(instance, eval);
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutions/dec_random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if(sol == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(instance.getNumberNodes());
                    writer.println(endTime - initTime);
                    writer.close();
                }
                else{
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + sol.getSolution().size() + " y con solución " + sol.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(sol.getSolution().size());
                    writer.println(endTime - initTime);
                    writer.close();
                }
            }

        }

        // MÚLTIPLES ITERACIONES PARA LOS GENERADORES ALEATORIOS
        /*
        Con el fin de hacer pruebas consistentes, vamos a lanzar múltiples ejecuciones de los dos generadores aleatorios de soluciones y ver qué resultados podemos
        alcanzar haciendo ejecuciones aleatorias
        */

        int nIterations = 100;
        if(incrementalMultipleRandomSolutions) {
            for (File i : dirInstances.listFiles()) {
                //i = new File(inPath + "/previous_work/instances/800_799_1_social_0.in");
                // i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                Evaluation eval = new Evaluation(instance);
                Solution bestSolution = null;
                long bestSolTime = 0;
                for (int j = 0; j < nIterations; j++) {
                    long initTime = System.nanoTime();
                    Solution sol = Solution.GenerateIncrementalRandomSolution(instance, eval);
                    long endTime = System.nanoTime();
                    if (bestSolution == null || sol != null && sol.solutionValue() < bestSolution.solutionValue()) {
                        bestSolution = sol;
                        bestSolTime = endTime - initTime;
                    }
                }
                String pathRandomSols = inPath + "/solutions/inc_random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if (bestSolution == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(instance.getNumberNodes());
                    writer.println(bestSolTime);
                    writer.close();
                } else {
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + bestSolution.getSolution().size() + " y con solución " + bestSolution.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(bestSolution.getSolution().size());
                    writer.println(bestSolTime);
                    writer.close();
                }
            }
        }
        if(decrementalMultipleRandomSolutions) {
            for (File i : dirInstances.listFiles()) {
                //i = new File(inPath + "/previous_work/instances/800_799_1_social_0.in");
                // i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                Evaluation eval = new Evaluation(instance);
                ArrayList<Integer> posSol = instance.getNodes();
                Solution bestSolution = null;
                long bestSolTime = 0;
                for (int j = 0; j < nIterations; j++) {
                    long initTime = System.nanoTime();
                    Solution sol = Solution.GenerateDecrementalRandomSolution(instance, eval);
                    long endTime = System.nanoTime();
                    if (bestSolution == null || sol != null && sol.solutionValue() < bestSolution.solutionValue()) {
                        bestSolution = sol;
                        bestSolTime = endTime - initTime;
                    }
                }
                String pathRandomSols = inPath + "/solutions/dec_random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if(bestSolution == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(instance.getNumberNodes());
                    writer.println(bestSolTime);
                    writer.close();
                }
                else{
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + bestSolution.getSolution().size() + " y con solución " + bestSolution.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(bestSolution.getSolution().size());
                    writer.println(bestSolTime);
                    writer.close();
                }
            }

        }

        /*
        Segunda fase de la generación de resultados. Ahora, vamos a generar soluciones: aleatorias, incrementales aleatorias y voraz por grado. Después de generar cada una de estas
        vamos a aplicarle la búsqueda local pertinente, para poder mejorar la solución obtenida.

         Generador de soluciones aleatorias, método incremental, con búsqueda local
        */
        if(lsIncrementalRandomSolution) {
            for (File i : dirInstances.listFiles()) {
                //i = new File(inPath + "/previous_work/instances/800_799_1_social_0.in");
                // i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                Solution.instance = instance;
                long initTime = System.nanoTime();
                Evaluation eval = new Evaluation(instance);
                Solution sol = Solution.GenerateIncrementalRandomSolution(instance, eval);
                LocalSearch ls = new LocalSearch(sol, eval, false);
                Solution improvedSolution = ls.bestSolutionFound;
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutions/ls_inc_random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if(improvedSolution == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(instance.getNumberNodes());
                    writer.println(endTime - initTime);
                    writer.close();
                }
                else{
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + improvedSolution.getSolution().size() + " y con solución " + improvedSolution.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(improvedSolution.getSolution().size());
                    writer.println(endTime - initTime);
                    writer.close();
                }
            }
        }
        nIterations = 100;
        if(lsIncrementalMultipleRandomSolutions) {
            for (File i : dirInstances.listFiles()) {
                Instance instance = new Instance(i);
                Solution.instance = instance;
                Evaluation eval = new Evaluation(instance);
                Solution bestSolution = null;
                long bestSolTime = 0;
                for (int j = 0; j < nIterations; j++) {
                    long initTime = System.nanoTime();
                    Solution sol = Solution.GenerateIncrementalRandomSolution(instance, eval);
                    long endTime = System.nanoTime();
                    if (bestSolution == null || sol != null && sol.solutionValue() < bestSolution.solutionValue()) {
                        bestSolution = sol;
                        bestSolTime = endTime - initTime;
                    }
                }
                long initLsTime = System.nanoTime();
                LocalSearch ls = new LocalSearch(bestSolution, eval, false);
                Solution improvedSolution = ls.bestSolutionFound;
                long endLsTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutions/ls_inc_random_solutions/";
                PrintWriter writer = new PrintWriter(pathRandomSols + i.getName() + ".txt", "UTF-8");
                if (improvedSolution == null) {
                    System.out.println("No se ha encontrado solución con este método para la instancia " + i.getName());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(instance.getNumberNodes());
                    writer.println(bestSolTime + (endLsTime - initLsTime));
                    writer.close();
                } else {
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + improvedSolution.getSolution().size() + " y con solución " + improvedSolution.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(improvedSolution.getSolution().size());
                    writer.println(bestSolTime + (endLsTime - initLsTime));
                    writer.close();
                }
            }
        }
        if(lsGreedySolution) {
            for(File i: dirInstances.listFiles()) {
                i = new File(inPath + "/previous_work/instances/10_18_2_social_0.in");
                Instance instance = new Instance(i);
                Solution.instance = instance;
                long initTime = System.nanoTime();
                Evaluation eval = new Evaluation(instance);
                Solution sol = Solution.GenerateDegreeGreedySolution(instance, eval);
                LocalSearch ls = new LocalSearch(sol, eval, true);
                Solution improvedSolution = new FilterUnnecesaryNodes(ls.bestSolutionFound, eval).bestSolutionFound;
                long endTime = System.nanoTime();
                String pathRandomSols = inPath + "/solutions/ls_greedy_solutions_ls+refined/";
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
                    System.out.println("Instancia " + i.getName() + " con valor de la FO " + improvedSolution.getSolution().size() + " y con solución " + improvedSolution.getSolution());
                    writer.println(i.getName().split("\\.")[0]);
                    writer.println(improvedSolution.getSolution().size());
                    writer.println(endTime - initTime);
                    writer.close();
                }
            }
        }
    }
}