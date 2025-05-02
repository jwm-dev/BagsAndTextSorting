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

    private static void analyzeZipfDistribution(String bagDemoOutput, String distributionName) {
        // Parse output: word [ (count) ]
        List<Integer> frequencies = new ArrayList<>();
        List<String> words = new ArrayList<>();
        String[] lines = bagDemoOutput.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int count = 1;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+)\\)\\s*$").matcher(line);
            if (m.find()) {
                count = Integer.parseInt(m.group(1));
                words.add(line.substring(0, m.start()).trim());
            } else {
                words.add(line);
            }
            frequencies.add(count);
        }
        if (frequencies.isEmpty()) {
            System.out.println("No words found for Zipf analysis.");
            return;
        }
    
        // Pair words and frequencies, then sort descending by frequency
        List<Map.Entry<String, Integer>> wordFreq = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            wordFreq.add(new AbstractMap.SimpleEntry<>(words.get(i), frequencies.get(i)));
        }
        wordFreq.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
    
        System.out.println("========== ZIPF'S LAW ANALYSIS: " + distributionName + " ==========");
    
        // Print top 5 most frequent words
        System.out.println("Top 5 most frequent words:");
        System.out.println("--------------------------");
        for (int i = 0; i < Math.min(5, wordFreq.size()); i++) {
            Map.Entry<String, Integer> entry = wordFreq.get(i);
            System.out.printf("%d. %s (%d)\n", i + 1, entry.getKey(), entry.getValue());
        }
        System.out.println();
    
        // Sort frequencies descending (rank 1 = most frequent)
        frequencies.sort(Collections.reverseOrder());
    
        // Compute best-fit slope in log-log space
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        int n = frequencies.size();
        for (int i = 0; i < n; i++) {
            int rank = i + 1;
            int freq = frequencies.get(i);
            double logRank = Math.log10(rank);
            double logFreq = Math.log10(freq);
            sumX += logRank;
            sumY += logFreq;
            sumXY += logRank * logFreq;
            sumXX += logRank * logRank;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    
        // Kolmogorov–Smirnov test for Zipf's Law
        double[] empiricalCDF = new double[n];
        double[] zipfCDF = new double[n];
        double total = 0;
        for (int f : frequencies) total += f;
        double running = 0;
        for (int i = 0; i < n; i++) {
            running += frequencies.get(i);
            empiricalCDF[i] = running / total;
        }
        double zipfSum = 0;
        for (int i = 1; i <= n; i++) zipfSum += 1.0 / i;
        double runningZipf = 0;
        for (int i = 0; i < n; i++) {
            runningZipf += 1.0 / (i + 1) / zipfSum;
            zipfCDF[i] = runningZipf;
        }
        double ks = 0;
        for (int i = 0; i < n; i++) {
            ks = Math.max(ks, Math.abs(empiricalCDF[i] - zipfCDF[i]));
        }
    
        System.out.printf("Best-fit slope (should be close to -1 for Zipf): %.4f\n", slope);
        System.out.printf("Kolmogorov–Smirnov statistic (lower is better fit): %.4f\n", ks);
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

        // Zipf's Law analysis
        String aliceOut = readFile("txt/alice.out");
        analyzeZipfDistribution(aliceOut, "alice.out");
        String mobyOut = readFile("txt/moby.out");
        analyzeZipfDistribution(mobyOut, "moby.out");
    }
}