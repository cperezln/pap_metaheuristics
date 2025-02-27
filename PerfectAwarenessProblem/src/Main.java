import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String inPath = args[0];
        System.out.println("Path para guardar los resultados " + inPath);

        String pathInstances = inPath + "/previous_work/instances";
        String pathSolutions = inPath + "/previous_work/solutions";
        File dirInstances = new File(pathInstances);
        // Ejecución del GRASP
        for (File i : dirInstances.listFiles()) {
            int nIterGrasp = 100;
            for(int j = 0; j < nIterGrasp; j++) {
                // Fase constructiva
                /* Crear la RCL basándonos en el método propuesto en el artículo*/
                // Fase de mejora

            }
        }
    }
}
