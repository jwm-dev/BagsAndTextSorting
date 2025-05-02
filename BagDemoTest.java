import java.io.*;
import java.util.*;

public class BagDemoTest {
    private static String runBagDemo(String[] args, String simulatedInput) throws Exception {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        if (simulatedInput != null) {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        }
        System.setOut(ps);

        try {
            BagDemo.main(args);
        } catch (SecurityException se) {
            // Ignore System.exit
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        return baos.toString().replaceAll("\r\n", "\n").trim();
    }

    private static String getExpectedLineForWord(String filename, String targetWord) throws IOException {
        String lower = targetWord.toLowerCase();
        int count = 0;
        boolean alwaysCap = true;
        boolean everLow = false;

        try (Scanner fileScanner = new Scanner(new File(filename))) {
            while (fileScanner.hasNext()) {
                String token = fileScanner.next();
                String word = token.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");
                if (word.length() == 0) continue;
                if (word.toLowerCase().equals(lower)) {
                    count++;
                    boolean isCap = Character.isUpperCase(word.charAt(0));
                    boolean isAllUpper = word.equals(word.toUpperCase());
                    if (!isCap) everLow = true;
                    if (!(isCap && !isAllUpper)) alwaysCap = false;
                }
            }
        }

        String display;
        if (alwaysCap && !everLow) {
            display = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        } else {
            display = lower;
        }
        return count > 1 ? display + " (" + count + ")" : display;
    }

    private static String findOutputLine(String output, String displayWord) {
        for (String line : output.split("\n")) {
            if (line.trim().startsWith(displayWord + " ") || line.trim().equals(displayWord)) {
                return line.trim();
            }
        }
        return null;
    }

    private static String readFile(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) throws Exception {
        String[][] tests = {
            {"txt/Sorting Input File.txt", "love"},
            {"txt/alice.txt", "alice"},
            {"txt/moby.txt", "moby-dick"}
        };

        int total = 0, passed = 0;
        List<String> failedTests = new ArrayList<>();

        for (String[] test : tests) {
            String file = test[0];
            String word = test[1];
            String expectedLine = getExpectedLineForWord(file, word);

            // PROMPT mode (simulate user input)
            System.out.println("Testing PROMPT mode with file: " + file);
            String output = runBagDemo(new String[]{}, file + "\n");
            String actualLine = findOutputLine(output, expectedLine.split(" ")[0]);
            boolean found = actualLine != null && actualLine.equals(expectedLine);
            System.out.println("Looking for: '" + expectedLine + "' ... " + (found ? "PASS" : "FAIL"));
            if (!found) failedTests.add("PROMPT: " + file + " (expected: " + expectedLine + ", actual: " + actualLine + ")");
            total++;
            if (found) passed++;

            // ONE ARG mode (output to .out file)
            System.out.println("Testing ONE ARG mode with file: " + file);
            runBagDemo(new String[]{file}, null);
            String outFile = file.replaceAll("\\.[^.]+$", ".out");
            String fileOutput = new File(outFile).exists() ? readFile(outFile) : "";
            actualLine = findOutputLine(fileOutput, expectedLine.split(" ")[0]);
            found = actualLine != null && actualLine.equals(expectedLine);
            System.out.println("Looking for: '" + expectedLine + "' ... " + (found ? "PASS" : "FAIL"));
            if (!found) failedTests.add("ONE ARG: " + file + " (expected: " + expectedLine + ", actual: " + actualLine + ")");
            total++;
            if (found) passed++;

            // TWO ARGS mode (explicit output file)
            String explicitOut = file + ".explicit.out";
            System.out.println("Testing TWO ARGS mode with file: " + file + " and output: " + explicitOut);
            runBagDemo(new String[]{file, explicitOut}, null);
            String explicitFileOutput = new File(explicitOut).exists() ? readFile(explicitOut) : "";
            actualLine = findOutputLine(explicitFileOutput, expectedLine.split(" ")[0]);
            found = actualLine != null && actualLine.equals(expectedLine);
            System.out.println("Looking for: '" + expectedLine + "' ... " + (found ? "PASS" : "FAIL"));
            if (!found) failedTests.add("TWO ARGS: " + file + " (expected: " + expectedLine + ", actual: " + actualLine + ")");
            total++;
            if (found) passed++;
        }

        // Summary
        System.out.println("\n========== TEST SUMMARY ==========");
        System.out.println("Total test cases: " + total);
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + (total - passed));
        if (!failedTests.isEmpty()) {
            System.out.println("\nFailed tests:");
            for (String fail : failedTests) {
                System.out.println(" - " + fail);
            }
        } else {
            System.out.println("\nAll tests passed!");
        }
        System.out.println("==================================");
    }
}