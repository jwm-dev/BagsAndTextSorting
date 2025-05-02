import java.util.*;
import java.io.*;

public class BagDemo {

   public static String fixExtension(String s) {
      if (s.lastIndexOf('.') == -1) {
         s += ".";
      }
      String result = s.substring(0, s.lastIndexOf('.')) + ".out";
      return result;
   }

   // Capitalize first letter, rest lower
   private static String capitalize(String s) {
      if (s.length() == 0) return s;
      return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
   }

   public static void main(String[] args) {
      String inputFile = "";
      String outputFile = "";
      boolean console = false;

      if (args.length == 0) {
         try (Scanner s = new Scanner(System.in)) {
            System.out.print("Please enter the file path: ");
            inputFile = s.nextLine();
         }
         console = true;
      } else if (args.length == 1) {
         inputFile = args[0];
         outputFile = fixExtension(inputFile);
         console = false;
      } else if (args.length == 2) {
         inputFile = args[0];
         outputFile = args[1];
         console = false;
      } else {
         System.out.println("I did not understand your command, please try again.");
         System.exit(0);
      }

      if (!console && inputFile.equalsIgnoreCase(outputFile)) {
         System.out.println("File paths cannot be the same!");
         System.exit(0);
      }

      // Output file/console info
      System.out.println();
      System.out.println("The input file path is: " + inputFile);
      if (console) {
         System.out.println("Output will be to the console.");
      } else {
         System.out.println("The output file path is: " + outputFile);
      }

      // Bag logic
      Map<String, Integer> counts = new HashMap<>();
      Map<String, Boolean> alwaysCap = new HashMap<>();
      Map<String, Boolean> everLow = new HashMap<>();

      try (Scanner fileScanner = new Scanner(new File(inputFile))) {
         while (fileScanner.hasNext()) {
            String token = fileScanner.next();
            // Strip all leading/trailing punctuation and whitespace
            String word = token.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");
            if (word.length() == 0) continue;
            String lower = word.toLowerCase();
            counts.put(lower, counts.getOrDefault(lower, 0) + 1);

            boolean isCap = Character.isUpperCase(word.charAt(0));
            boolean isAllUpper = word.equals(word.toUpperCase());
            if (!isCap) everLow.put(lower, true);
            if (!(isCap && !isAllUpper)) alwaysCap.put(lower, false);
            else if (!alwaysCap.containsKey(lower)) alwaysCap.put(lower, true);
         }
      } catch (FileNotFoundException e) {
         System.out.println("Could not open input file: " + inputFile);
         System.exit(1);
      }

      // Prepare sorted output: alphabetical, upper-case version first if identical
      TreeSet<String> sortedWords = new TreeSet<>((a, b) -> {
         String la = a.toLowerCase();
         String lb = b.toLowerCase();
         int cmp = la.compareTo(lb);
         if (cmp != 0) return cmp;
         if (a.equals(b)) return 0;
         if (Character.isUpperCase(a.charAt(0)) && Character.isLowerCase(b.charAt(0))) return -1;
         if (Character.isLowerCase(a.charAt(0)) && Character.isUpperCase(b.charAt(0))) return 1;
         return a.compareTo(b);
      });

      // Build display words and their counts
      Map<String, Integer> displayCounts = new HashMap<>();
      for (String lower : counts.keySet()) {
         boolean cap = alwaysCap.getOrDefault(lower, false);
         boolean everL = everLow.getOrDefault(lower, false);
         String display;
         if (cap && !everL) {
            display = capitalize(lower);
         } else {
            display = lower;
         }
         displayCounts.put(display, counts.get(lower));
         sortedWords.add(display);
      }

      PrintStream out = System.out;
      if (!console) {
         try {
            out = new PrintStream(outputFile);
         } catch (FileNotFoundException e) {
            System.out.println("Could not open output file: " + outputFile);
            System.exit(1);
         }
      }

      for (String word : sortedWords) {
         int count = displayCounts.get(word);
         if (count > 1) {
            out.println(word + " (" + count + ")");
         } else {
            out.println(word);
         }
      }

      if (!console) out.close();
   }
}