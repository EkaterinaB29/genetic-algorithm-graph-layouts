import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        int populationSize = 100;  // Assuming a fixed population size

        // Loop to increase graph size from 100 nodes and edges to 10,000, doubling each iteration
        for (int graphSize = 100; graphSize <= 10000; graphSize *= 2) {
            System.out.println("Configuring genetic algorithm for graph size: " + graphSize);

            ArrayList<Edge> allPossibleEdges = new ArrayList<>();
            for (int i = 0; i < graphSize; i++) {
                for (int j = i + 1; j < graphSize; j++) {
                    allPossibleEdges.add(new Edge(i, j));
                }
            }
            Collections.shuffle(allPossibleEdges);
            List<Edge> selectedEdges = allPossibleEdges.subList(0, Math.min(graphSize, allPossibleEdges.size()));
            ArrayList<Edge> edges = new ArrayList<>(selectedEdges);

            System.out.println("Generated " + edges.size() + " edges for " + graphSize + " nodes.");

            //Graph initialGraph = new Graph(graphSize, edges, 1000, 1000);  // Ensure Graph constructor correctly sets sizes
            System.out.println("Starting Genetic Algorithm for " + graphSize + " nodes and " + edges.size() + " edges.");
            GeneticAlgorithm ga = new GeneticAlgorithm(graphSize,edges, populationSize, Runtime.getRuntime().availableProcessors());
            Runtime runtime = Runtime.getRuntime();
            long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
            ga.compute();
            long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Memory used by this run: " + (usedMemoryAfter - usedMemoryBefore) + " bytes");

            if (graphSize == 10000) break;  // Prevent unnecessary doubling
        }
    }
}
