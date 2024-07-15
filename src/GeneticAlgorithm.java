import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeneticAlgorithm {
    // Population parameters
    ArrayList<Graph> population = new ArrayList<>();
    private Graph initialGraph;
    public int populationSize;
    int generation = 1;
    private volatile boolean running = true; // Flag to control the loop
    // List to store the best graph's fitness score and generation

    // Synchronization tools & other parameters
    static Random random = new Random();
    static final double MUTATION_PROBABILITY = 0.1;
    int processorCount;
    private ExecutorService executor;
    private Semaphore semaphore;
    private String logFile = "generation_log.csv";

    public GeneticAlgorithm(int nodeSize,ArrayList<Edge> edges, int populationSize, int processorCount) {
        this.initialGraph = new Graph(nodeSize,edges,1000,1000);
        this.populationSize = populationSize;
        initialGraphPopulation(initialGraph);
        this.processorCount = processorCount;
        this.executor = Executors.newFixedThreadPool(processorCount);
        this.semaphore = new Semaphore(0);

        // Add a shutdown hook to properly shut down the executor service
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            running = false; // Set the flag to false to stop the loop
            shutdownAndAwaitTermination();
            System.out.println("Shutdown complete.");
        }));

        // Initialize log file
        initializeLogFile();
    }

    // Function to create the population of graphs according to the first one
    private void initialGraphPopulation(Graph initialGraph) {
        this.population.add(initialGraph);
        for (int i = 0; i < populationSize - 1; i++) {
            this.population.add(new Graph(initialGraph.getNodes(), initialGraph.getEdges(), initialGraph.getH(), initialGraph.getW()));
        }
    }

    public void selection() {
        population.sort(Comparator.comparingDouble(Graph::getFitnessScore));
        Collections.reverse(population);
        ArrayList<Graph> selectedGraphs = new ArrayList<>();
        population.subList(0, (populationSize / 2)).
                forEach(graph -> selectedGraphs.add
                        (new Graph(graph.getNodes(), graph.getEdges(), graph.getH(), graph.getW())));
        this.population = selectedGraphs;
    }

    public void crossoverOnePoint() {
        int size = population.size();
        int limit = size - (size % 2); // Ensuring we only process pairs

        List<Graph> children = IntStream.range(0, limit)
                .boxed()
                .parallel()
                .filter(i -> i % 2 == 0)
                .flatMap(i -> {
                    Graph parent1 = population.get(i);
                    Graph parent2 = population.get(i + 1);

                    int separator = random.nextInt(parent1.getNodes().size());

                    ArrayList<Node> firstChildNodes = new ArrayList<>(parent2.getNodes().subList(0, separator + 1));
                    firstChildNodes.addAll(parent1.getNodes().subList(separator + 1, parent1.getNodes().size()));

                    ArrayList<Node> secondChildNodes = new ArrayList<>(parent1.getNodes().subList(0, separator + 1));
                    secondChildNodes.addAll(parent2.getNodes().subList(separator + 1, parent2.getNodes().size()));

                    Graph child1 = new Graph(firstChildNodes, parent1.getEdges(), parent1.getH(), parent1.getW());
                    Graph child2 = new Graph(secondChildNodes, parent2.getEdges(), parent2.getH(), parent2.getW());

                    return Arrays.stream(new Graph[]{child1, child2});
                })
                .collect(Collectors.toList());

        // If population size was odd
        if (size % 2 != 0) {
            children.add(population.get(size - 1));
        }
        population.addAll(children);
    }

    public void mutation(double mutationRate) {
        Random random = new Random();
        for (Graph g : this.population) {
            double randomValue = random.nextDouble();
            if (randomValue <= mutationRate) {
                // Perform a coin flip to decide which mutation to apply
                double mutationChoice = random.nextDouble();
                if (mutationChoice < 0.5) {
                    g.circularMutation();
                } else {
                    g.mutationFlipCoordinates();
                }
            }
        }
    }

    public void calculateFitness() {
        semaphore.drainPermits();
        for (Graph graph : this.population) {
            executor.submit(() -> {
                try {
                    graph.fitnessEvaluation(); // Perform fitness evaluation
                } catch (Exception e) {
                } finally {
                    semaphore.release(); // Increase it
                }
            });
        }
        try {
            semaphore.acquire(populationSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void compute() {
        int generation = 1; // Reset generation
        long totalComputationTime = 0;

        int nodesAtStart = initialGraph.getNodes().size();
        int edgesAtStart = initialGraph.getEdges().size();

        while (generation <= 10) {
            long startTime = System.currentTimeMillis();

            long fitnessStartTime = System.currentTimeMillis();
            calculateFitness();
            long fitnessEndTime = System.currentTimeMillis();
            long fitnessTime = fitnessEndTime - fitnessStartTime;

            long selectionStartTime = System.currentTimeMillis();
            selection();
            long selectionEndTime = System.currentTimeMillis();
            long selectionTime = selectionEndTime - selectionStartTime;

            long crossoverStartTime = System.currentTimeMillis();
            crossoverOnePoint();
            long crossoverEndTime = System.currentTimeMillis();
            long crossoverTime = crossoverEndTime - crossoverStartTime;

            long mutationStartTime = System.currentTimeMillis();
            mutation(MUTATION_PROBABILITY);
            long mutationEndTime = System.currentTimeMillis();
            long mutationTime = mutationEndTime - mutationStartTime;

            long endTime = System.currentTimeMillis();
            long timeForGeneration = endTime - startTime;
            totalComputationTime += timeForGeneration;

            logGeneration(generation, nodesAtStart, edgesAtStart, timeForGeneration, fitnessTime, selectionTime, crossoverTime, mutationTime);
            generation++;
        }
        logTotalTime(totalComputationTime, 10); // Log total and average time after 10 generations
        shutdownAndAwaitTermination();
    }

    public void logGeneration(int generation, int nodes, int edges, long time, long fitnessTime, long selectionTime, long crossoverTime, long mutationTime) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(new File(logFile), true))) {
            writer.printf("%d,%d,%d,%d ms,%d ms,%d ms,%d ms,%d ms\n", generation, nodes, edges, time, fitnessTime, selectionTime, crossoverTime, mutationTime);
            writer.flush(); // Ensure data is written to disk
        } catch (FileNotFoundException e) {
            System.out.println("Error logging generation: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed error analysis
        }
    }

    public void logTotalTime(long totalComputationTime, int totalGenerations) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(new File(logFile), true))) {
            writer.printf("Total Time,%d ms,Average Time,%d ms\n", totalComputationTime, totalComputationTime / totalGenerations);
            writer.flush(); // Ensure data is written to disk
        } catch (FileNotFoundException e) {
            System.out.println("Error logging total time: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed error analysis
        }
    }

    public void initializeLogFile() {
        File logFileObject = new File(logFile);
        // Check if the log file already exists
        if (!logFileObject.exists()) {
            try (PrintWriter writer = new PrintWriter(logFileObject)) {
                writer.println("Generation,Nodes,Edges,Total Time (ms),Fitness Time (ms),Selection Time (ms),Crossover Time (ms),Mutation Time (ms)");
            } catch (FileNotFoundException e) {
                System.out.println("Error initializing log file: " + e.getMessage());
            }
        }
    }



    public void shutdownAndAwaitTermination() {
        executor.shutdown();
        try {
            // Wait for existing tasks to terminate
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Executor did not terminate.");
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


}
