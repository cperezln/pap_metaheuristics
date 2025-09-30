import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;


public class SpreadingProcessOptimize {

    private Instance instance;
    private Instant startTime;

    // Reusable arrays to avoid allocation overhead
    private int[] spreaderCount;
    private int[] awareCount;
    private boolean[][] checkSpreaders;
    private boolean[] visited;
    private boolean[] isSpreaderTaubw;
    private boolean[] isSpreaderTaupbw;
    private boolean[] isAware;
    private int[] qSpreaders;

    public SpreadingProcessOptimize(Instance i) {
        this.instance = i;
        this.startTime = null;
        initializeArrays(10000);
    }

    public SpreadingProcessOptimize(Instance i, Instant startTime) {
        this.instance = i;
        this.startTime = startTime;
        initializeArrays(10000);
    }

    private void initializeArrays(int maxSize) {
        spreaderCount = new int[maxSize];
        awareCount = new int[maxSize];
        checkSpreaders = new boolean[maxSize][maxSize];
        visited = new boolean[maxSize];
        isSpreaderTaubw = new boolean[maxSize];
        isSpreaderTaupbw = new boolean[maxSize];
        isAware = new boolean[maxSize];
        qSpreaders = new int[maxSize];
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }


    public boolean isSolution(Solution sol) {
        int n = instance.getNumberNodes();

        // Clear arrays for reuse
        for (int i = 0; i < n; i++) {
            spreaderCount[i] = 0;
            awareCount[i] = 0;
            visited[i] = false;
            isSpreaderTaubw[i] = false;
            isSpreaderTaupbw[i] = false;
            isAware[i] = false;
            for (int j = 0; j < n; j++) {
                checkSpreaders[i][j] = false;
            }
        }

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
                    if(!checkSpreaders[neigh][index]) {
                        checkSpreaders[neigh][index] = true;
                        spreaderCount[neigh]++;
                    }
                }
                initialSeeds = initialSeeds.xor(BigInteger.ONE.shiftLeft(index));
                index = initialSeeds.getLowestSetBit();
            } while (index != -1);
        }
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
                    if(!checkSpreaders[node][neigh]) {
                        checkSpreaders[node][neigh] = true;
                        awareCount[node]++;
                    }
                    // The number of spreaders around neigh increments, as node is a spreader
                    if(!checkSpreaders[neigh][node]) {
                        checkSpreaders[neigh][node] = true;
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
        // Create a copy of awareCount for this solution
        int[] awareCountCopy = new int[n];
        System.arraycopy(awareCount, 0, awareCountCopy, 0, n);
        sol.setAwareCount(awareCountCopy);
        return awareSize == n;
    }

    public static boolean checkEqualSets(boolean[] s, boolean[] sp) {
        boolean equals = true;
        for(int i = 0; i < s.length; i++) equals = equals && (s[i] == sp[i]);
        return equals;
    }
}
