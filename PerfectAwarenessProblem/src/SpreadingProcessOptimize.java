import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;


public class SpreadingProcessOptimize {

    private Instance instance;
    private Instant startTime;

    public SpreadingProcessOptimize(Instance i) {
        this.instance = i;
        this.startTime = null;
    }

    public SpreadingProcessOptimize(Instance i, Instant startTime) {
        this.instance = i;
        this.startTime = startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }


    public boolean isSolution(Solution sol) {
        int n = instance.getNumberNodes();
        int[] spreaderCount = new int[n];
        int[] awareCount = new int[n];
        int[][] checkSpreaders = new int[n][n];
        boolean[] visited = new boolean[n];
        boolean[] isSpreaderTaubw = new boolean[n];
        boolean[] isSpreaderTaupbw = new boolean[n];
        boolean[] isAware = new boolean[n];
        int[] qSpreaders = new int[n];
        int qSize = 0;

        // Auxiliar: para no contar spreaders de más
        /*---------------------------------------------------*/
        int awareSize = 0;
        BigInteger initialSeeds = sol.getBitwiseRepresentation();
        int index = initialSeeds.getLowestSetBit();
        if (index != -1) {
            do {
                isSpreaderTaubw[index] = true;
                isSpreaderTaupbw[index] = true;
                isAware[index] = true;
                awareSize++;
                qSpreaders[qSize++] = index;
                for(int neigh: instance.graph.get(index)) {
                    if(checkSpreaders[neigh][index] == 0) {
                        checkSpreaders[neigh][index] = 1;
                        spreaderCount[neigh]++;
                    }
                }
                initialSeeds = initialSeeds.xor(BigInteger.ONE.shiftLeft(index));
                index = initialSeeds.getLowestSetBit();
            } while (index != -1);
        }
        int[][] checkAware = checkSpreaders;
        awareCount = spreaderCount;
        /*---------------------------------------------------*/
        int qHead = 0;

        while(qHead < qSize && awareSize != n) {
            int node = qSpreaders[qHead++];
            if(!visited[node]) {
                visited[node] = true;
                for (Integer neigh : instance.graph.get(node)) {
                    // Become aware if your neighbor
                    if(!isAware[neigh]) {
                        isAware[neigh] = true;
                        awareSize++;
                        this.instance.setState(neigh, 1);
                    }
                    if(checkAware[node][neigh] == 0) {
                        checkAware[node][neigh] = 1;
                        awareCount[node]++;
                    }
                    // The number of spreaders around neigh increments, as node is a spreader
                    if(checkSpreaders[neigh][node] == 0) {
                        checkSpreaders[neigh][node] = 1;
                        spreaderCount[neigh]++;
                    }
                    if (!isSpreaderTaubw[neigh] && spreaderCount[neigh] >= instance.graph.get(neigh).size() * 0.5) {
                        isSpreaderTaupbw[neigh] = true;
                        isSpreaderTaubw[neigh] = true;
                        qSpreaders[qSize++] = neigh;
                        this.instance.setState(neigh, 2);
                    }
                }
            }
        }
        sol.setAware(awareSize);
        sol.setAwareCount(awareCount);
        return awareSize == n;
    }

    public static boolean checkEqualSets(boolean[] s, boolean[] sp) {
        boolean equals = true;
        for(int i = 0; i < s.length; i++) equals = equals && (s[i] == sp[i]);
        return equals;
    }
}
