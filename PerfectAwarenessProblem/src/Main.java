import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
        String path = args[0];
        float paramAlpha = Float.parseFloat(args[1]);
        float betcent = Float.parseFloat(args[2]);
        float degcent = Float.parseFloat(args[3]);
        float eigcent = Float.parseFloat(args[4]);

        File i = new File(path);
        int nIterGrasp = 75;
        Solution.betFactor = betcent;
        Solution.degFactor = degcent;
        Solution.eigFactor = eigcent;
        Instance instance = new Instance(i, i.getName());
        SpreadingProcessOptimize eval = new SpreadingProcessOptimize(instance);
        Solution.instance = instance;
        GRASP graspExec = new GRASP(nIterGrasp, paramAlpha, instance, eval);
        Solution bestSolutionFound = graspExec.run();
        System.out.println(bestSolutionFound.solutionValue());

    }
}
