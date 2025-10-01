import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

/**
 * Test runner for easier execution from IDE
 */
public class TestRunner {
    public static final long TIME_LIMIT_MS = 300000; // 300000 - 300 seconds - centralized time limit
    public static final long LOCAL_SEARCH_TIME_LIMIT_MS = 300000;
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        // Set default parameters for easy testing
        String projectPath = "/home/isaac/pap"; //"/home/isaac/pap" /Users/isaaclozano/Documents/GitHub/pap_metaheuristics
        String configId = "1";
        String algorithm = "ILS"; // Can be changed to "GRASP"

        System.out.println("=== Test Runner for PAP ===");
        System.out.println("Project Path: " + projectPath);
        System.out.println("Config ID: " + configId);
        System.out.println("Algorithm: " + algorithm);
        System.out.println("===========================");

        // Create arguments array and call Main
        String[] mainArgs = {projectPath, configId, algorithm};
        Main.main(mainArgs);
    }
}