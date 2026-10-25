import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class BatAlgorithm {

    private final VDRPMDPCParameter parameter;
    private final Random random = new Random();

    private final int populationSize;
    private final int maxIterations;

    /*
     * Parameter Bat Algorithm.
     */
    private final double frequencyMin = 0.0;
    private final double frequencyMax = 2.0;

    private final double alpha = 0.9;
    private final double gamma = 0.9;

    /*
     * Populasi kelelawar.
     */
    private final List<Bat> bats =
            new ArrayList<>();

    private Solution globalBest;

    // =========================================================
    // INNER CLASS BAT
    // =========================================================

    private static class Bat {

        Solution solution;

        double frequency;
        double velocity;

        double loudness;
        double pulseRate;

        Bat(Solution solution) {

            this.solution = solution;

            this.frequency = 0.0;
            this.velocity = 0.0;

            this.loudness = 1.0;
            this.pulseRate = 0.5;
        }
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public BatAlgorithm(
            VDRPMDPCParameter parameter,
            int populationSize,
            int maxIterations) {

        if (parameter == null) {
            throw new IllegalArgumentException(
                    "Parameter tidak boleh null.");
        }

        if (parameter.labels == null
                || parameter.demand == null) {

            throw new IllegalArgumentException(
                    "Data label atau demand tidak tersedia.");
        }

        if (parameter.demand.length
                != parameter.labels.size()) {

            throw new IllegalArgumentException(
                    "Jumlah demand tidak sesuai jumlah titik.");
        }

        if (populationSize < 1) {
            throw new IllegalArgumentException(
                    "Populasi minimal 1.");
        }

        if (maxIterations < 1) {
            throw new IllegalArgumentException(
                    "Iterasi minimal 1.");
        }

        this.parameter = parameter;
        this.populationSize = populationSize;
        this.maxIterations = maxIterations;
    }

    // =========================================================
    // PROSES UTAMA BAT ALGORITHM
    // =========================================================

    public Solution optimize() {

        System.out.println();
        System.out.println("====================================");
        System.out.println("BAT ALGORITHM - VDRPMDPC");
        System.out.println("====================================");

        System.out.println(
                "Jumlah titik : "
                        + parameter.labels.size());

        System.out.println(
                "Populasi     : "
                        + populationSize);

        System.out.println(
                "Iterasi      : "
                        + maxIterations);

        System.out.println();

        // =====================================================
        // 1. INISIALISASI POPULASI
        // =====================================================

        initializePopulation();

        updateGlobalBest();

        // =====================================================
        // 2. ITERASI BAT ALGORITHM
        // =====================================================

        for (int iteration = 1;
                iteration <= maxIterations;
                iteration++) {

            for (int i = 0;
                    i < bats.size();
                    i++) {

                Bat bat = bats.get(i);

                // =============================================
                // UPDATE FREKUENSI
                // =============================================

                bat.frequency =
                        frequencyMin
                                + (frequencyMax
                                - frequencyMin)
                                * random.nextDouble();

                /*
                 * Pada BA kontinu:
                 *
                 * v_i = v_i + (x_i - x_best) * f_i
                 *
                 * Karena solusi kita berupa urutan diskret,
                 * velocity diterjemahkan menjadi intensitas
                 * perubahan terhadap rute.
                 */
                bat.velocity =
                        bat.velocity
                                + bat.frequency;

                // =============================================
                // MEMBENTUK KANDIDAT
                // =============================================

                Solution candidate =
                        createCandidate(
                                bat,
                                iteration);

                if (candidate == null) {
                    continue;
                }

                // Constraint -> Objective -> Fitness
                if (!evaluateSilently(candidate)) {
                    continue;
                }

                // =============================================
                // ACCEPTANCE RULE
                // =============================================

                /*
                 * Karena objective diminimalkan:
                 * kandidat diterima jika lebih baik
                 * dan memenuhi probabilitas loudness.
                 */
                if (candidate.objectiveValue
                        < bat.solution.objectiveValue
                        && random.nextDouble()
                        < bat.loudness) {

                    bat.solution =
                            copySolution(candidate);

                    /*
                     * Loudness menurun.
                     */
                    bat.loudness =
                            alpha * bat.loudness;

                    /*
                     * Pulse rate meningkat.
                     */
                    bat.pulseRate =
                            0.5
                                    * (1.0
                                    - Math.exp(
                                    -gamma
                                            * iteration));
                }

                // =============================================
                // UPDATE GLOBAL BEST
                // =============================================

                if (candidate.objectiveValue
                        < globalBest.objectiveValue) {

                    globalBest =
                            copySolution(candidate);
                }
            }

            /*
             * Pastikan global best juga dibandingkan
             * dengan seluruh populasi.
             */
            updateGlobalBest();

            // =============================================
            // OUTPUT ITERASI
            // =============================================

            System.out.printf(
                    "Iterasi %3d | "
                            + "Objective: %.2f detik | "
                            + "Fitness: %.10f%n",
                    iteration,
                    globalBest.objectiveValue,
                    globalBest.fitness);
        }

        // =====================================================
        // 3. HASIL AKHIR
        // =====================================================

        printBestSolution();

        return copySolution(globalBest);
    }

    // =========================================================
    // INISIALISASI POPULASI
    // =========================================================

    private void initializePopulation() {

        bats.clear();

        int attempts = 0;

        int maxAttempts =
                populationSize * 1000;

        while (bats.size() < populationSize
                && attempts < maxAttempts) {

            attempts++;

            Solution solution =
                    createRandomSolution();

            if (solution != null
                    && evaluateSilently(solution)) {

                Bat bat =
                        new Bat(
                                copySolution(solution));

                /*
                 * Loudness awal.
                 */
                bat.loudness =
                        1.0;

                /*
                 * Pulse rate awal.
                 */
                bat.pulseRate =
                        0.5;

                bats.add(bat);
            }
        }

        if (bats.isEmpty()) {

            throw new IllegalStateException(
                    "Tidak ditemukan solusi feasible "
                            + "untuk populasi awal BA.");
        }

        while (bats.size() < populationSize) {

            Bat best =
                    getBestBat();

            Bat copy =
                    new Bat(
                            copySolution(
                                    best.solution));

            copy.loudness =
                    best.loudness;

            copy.pulseRate =
                    best.pulseRate;

            bats.add(copy);
        }
    }

    // =========================================================
    // MEMBENTUK KANDIDAT BA
    // =========================================================

    private Solution createCandidate(
            Bat bat,
            int iteration) {

        Solution candidate =
                copySolution(
                        bat.solution);

        /*
         * Besarnya perubahan disesuaikan
         * dengan frequency dan velocity.
         */
        int numberOfMoves =
                1
                        + (int) Math.min(
                        3,
                        Math.round(
                                bat.frequency
                                        + bat.velocity * 0.1));

        // =====================================================
        // GLOBAL SEARCH
        // =====================================================

        for (int move = 0;
                move < numberOfMoves;
                move++) {

            /*
             * Mendekat ke global best.
             */
            if (random.nextDouble() < 0.60) {

                candidate =
                        combineRoutes(
                                candidate,
                                globalBest);

            } else {

                /*
                 * Eksplorasi dengan mutasi.
                 */
                mutateRoute(candidate);
            }
        }

        // =====================================================
        // LOCAL SEARCH
        // =====================================================

        /*
         * Pada BA:
         *
         * jika random > pulse rate,
         * lakukan pencarian lokal di sekitar solusi terbaik.
         */
        if (random.nextDouble()
                > bat.pulseRate) {

            candidate =
                    copySolution(
                            globalBest);

            /*
             * Local random walk.
             */
            mutateRoute(candidate);

            if (random.nextDouble() < 0.40) {
                mutateRoute(candidate);
            }

            if (random.nextDouble() < 0.30) {
                mutateDroneMission(candidate);
            }
        }

        // =====================================================
        // RANDOM EXPLORATION
        // =====================================================

        /*
         * Sedikit random restart agar BA
         * tidak terlalu cepat stagnan.
         */
        if (random.nextDouble() < 0.10) {

            candidate =
                    createRandomSolution();
        }

        if (candidate != null) {

            candidate.feasible = false;

            candidate.objectiveValue =
                    Double.POSITIVE_INFINITY;

            candidate.fitness = 0.0;
        }

        return candidate;
    }

    // =========================================================
    // PEMBENTUKAN SOLUSI RANDOM
    // =========================================================

    private Solution createRandomSolution() {

        List<Integer> customers =
                new ArrayList<>();

        for (int i = 1;
                i < parameter.labels.size();
                i++) {

            customers.add(i);
        }

        Collections.shuffle(
                customers,
                random);

        Solution solution =
                new Solution();

        int droneCustomer = -1;

        /*
         * 50% kemungkinan mencoba menggunakan drone.
         */
        if (random.nextDouble() < 0.50) {

            List<Integer> eligible =
                    new ArrayList<>();

            for (int customer : customers) {

                if (parameter.demand[customer]
                        <= VDRPMDPCParameter.Cd) {

                    eligible.add(customer);
                }
            }

            if (!eligible.isEmpty()) {

                droneCustomer =
                        eligible.get(
                                random.nextInt(
                                        eligible.size()));

                customers.remove(
                        Integer.valueOf(
                                droneCustomer));
            }
        }

        solution.vanRoute.add(0);

        solution.vanRoute.addAll(
                customers);

        solution.vanRoute.add(0);

        if (droneCustomer >= 0
                && customers.size() >= 2) {

            addRandomDroneMission(
                    solution,
                    droneCustomer);

        } else if (droneCustomer >= 0) {

            solution.vanRoute.add(
                    solution.vanRoute.size() - 1,
                    droneCustomer);
        }

        return solution;
    }

    // =========================================================
    // MISI DRONE RANDOM
    // =========================================================

    private void addRandomDroneMission(
            Solution solution,
            int droneCustomer) {

        List<Integer> vanCustomers =
                getVanCustomers(solution);

        if (vanCustomers.size() < 2) {

            solution.vanRoute.add(
                    solution.vanRoute.size() - 1,
                    droneCustomer);

            return;
        }

        int launchIndex =
                random.nextInt(
                        vanCustomers.size() - 1);

        int rendezvousIndex =
                launchIndex
                        + 1
                        + random.nextInt(
                        vanCustomers.size()
                                - launchIndex
                                - 1);

        solution.droneMissions.add(
                new Solution.DroneMission(

                        vanCustomers.get(
                                launchIndex),

                        droneCustomer,

                        vanCustomers.get(
                                rendezvousIndex)));
    }

    // =========================================================
    // KOMBINASI RUTE
    // =========================================================

    private Solution combineRoutes(
            Solution base,
            Solution guide) {

        List<Integer> baseCustomers =
                getVanCustomers(base);

        List<Integer> guideCustomers =
                getVanCustomers(guide);

        List<Integer> combined =
                new ArrayList<>();

        int droneCustomer = -1;

        if (!base.droneMissions.isEmpty()) {

            droneCustomer =
                    base.droneMissions
                            .get(0)
                            .customerPoint;
        }

        for (int customer
                : baseCustomers) {

            if (customer != droneCustomer
                    && random.nextBoolean()
                    && !combined.contains(
                            customer)) {

                combined.add(customer);
            }
        }

        for (int customer
                : guideCustomers) {

            if (customer != droneCustomer
                    && !combined.contains(
                            customer)) {

                int position =
                        random.nextInt(
                                combined.size() + 1);

                combined.add(
                        position,
                        customer);
            }
        }

        /*
         * Pastikan tidak ada pelanggan hilang.
         */
        for (int customer = 1;
                customer < parameter.labels.size();
                customer++) {

            if (customer != droneCustomer
                    && !combined.contains(
                            customer)) {

                combined.add(customer);
            }
        }

        Solution child =
                copySolution(base);

        child.vanRoute.clear();

        child.vanRoute.add(0);
        child.vanRoute.addAll(combined);
        child.vanRoute.add(0);

        /*
         * Validasi struktur misi drone.
         */
        if (!child.droneMissions.isEmpty()) {

            Solution.DroneMission mission =
                    child.droneMissions.get(0);

            int launchIndex =
                    child.vanRoute.indexOf(
                            mission.launchPoint);

            int rendezvousIndex =
                    child.vanRoute.indexOf(
                            mission.rendezvousPoint);

            if (launchIndex < 1
                    || rendezvousIndex
                    <= launchIndex) {

                child.droneMissions.clear();

                if (!child.vanRoute.contains(
                        mission.customerPoint)) {

                    child.vanRoute.add(
                            child.vanRoute.size() - 1,
                            mission.customerPoint);
                }
            }
        }

        return child;
    }

    // =========================================================
    // MUTASI RUTE
    // =========================================================

    private void mutateRoute(
            Solution solution) {

        int routeSize =
                solution.vanRoute.size();

        if (routeSize <= 4) {
            return;
        }

        int first =
                1
                        + random.nextInt(
                        routeSize - 2);

        int second =
                1
                        + random.nextInt(
                        routeSize - 2);

        while (first == second) {

            second =
                    1
                            + random.nextInt(
                            routeSize - 2);
        }

        Collections.swap(
                solution.vanRoute,
                first,
                second);
    }

    // =========================================================
    // MUTASI MISI DRONE
    // =========================================================

    private void mutateDroneMission(
            Solution solution) {

        // =====================================================
        // SUDAH MENGGUNAKAN DRONE
        // =====================================================

        if (!solution.droneMissions.isEmpty()) {

            /*
             * Peluang menghapus misi drone.
             */
            if (random.nextDouble() < 0.25) {

                Solution.DroneMission old =
                        solution.droneMissions
                                .remove(0);

                if (!solution.vanRoute.contains(
                        old.customerPoint)) {

                    solution.vanRoute.add(
                            solution.vanRoute.size() - 1,
                            old.customerPoint);
                }

                return;
            }

            Solution.DroneMission old =
                    solution.droneMissions
                            .get(0);

            List<Integer> vanCustomers =
                    getVanCustomers(solution);

            if (vanCustomers.size() < 2) {
                return;
            }

            int launchIndex =
                    random.nextInt(
                            vanCustomers.size() - 1);

            int rendezvousIndex =
                    launchIndex
                            + 1
                            + random.nextInt(
                            vanCustomers.size()
                                    - launchIndex
                                    - 1);

            solution.droneMissions.set(
                    0,
                    new Solution.DroneMission(

                            vanCustomers.get(
                                    launchIndex),

                            old.customerPoint,

                            vanCustomers.get(
                                    rendezvousIndex)));

            return;
        }

        // =====================================================
        // BELUM MENGGUNAKAN DRONE
        // =====================================================

        List<Integer> vanCustomers =
                getVanCustomers(solution);

        if (vanCustomers.size() < 3) {
            return;
        }

        List<Integer> eligible =
                new ArrayList<>();

        for (int customer
                : vanCustomers) {

            if (parameter.demand[customer]
                    <= VDRPMDPCParameter.Cd) {

                eligible.add(customer);
            }
        }

        if (eligible.isEmpty()) {
            return;
        }

        int droneCustomer =
                eligible.get(
                        random.nextInt(
                                eligible.size()));

        solution.vanRoute.remove(
                Integer.valueOf(
                        droneCustomer));

        vanCustomers =
                getVanCustomers(solution);

        if (vanCustomers.size() < 2) {

            solution.vanRoute.add(
                    solution.vanRoute.size() - 1,
                    droneCustomer);

            return;
        }

        int launchIndex =
                random.nextInt(
                        vanCustomers.size() - 1);

        int rendezvousIndex =
                launchIndex
                        + 1
                        + random.nextInt(
                        vanCustomers.size()
                                - launchIndex
                                - 1);

        solution.droneMissions.add(
                new Solution.DroneMission(

                        vanCustomers.get(
                                launchIndex),

                        droneCustomer,

                        vanCustomers.get(
                                rendezvousIndex)));
    }

    // =========================================================
    // EVALUASI:
    // CONSTRAINT -> OBJECTIVE -> FITNESS
    // =========================================================

    private boolean evaluateSilently(
            Solution solution) {

        PrintStream originalOut =
                System.out;

        ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();

        PrintStream silentOut =
                new PrintStream(buffer);

        try {

            System.setOut(silentOut);

            // =================================================
            // 1. CONSTRAINT
            // =================================================

            solution.feasible =
                    Constraint.check(
                            solution,
                            parameter);

            if (!solution.feasible) {
                return false;
            }

            // =================================================
            // 2. OBJECTIVE
            // 3. FITNESS
            // =================================================

            ObjectiveFunction.calculateObjective(
                    solution,
                    parameter);

            return Double.isFinite(
                    solution.objectiveValue)

                    && solution.objectiveValue > 0.0

                    && Double.isFinite(
                            solution.fitness)

                    && solution.fitness > 0.0;

        } catch (IllegalArgumentException e) {

            solution.feasible = false;
            return false;

        } catch (IndexOutOfBoundsException e) {

            solution.feasible = false;
            return false;

        } finally {

            System.setOut(
                    originalOut);

            silentOut.close();
        }
    }

    // =========================================================
    // UPDATE GLOBAL BEST
    // =========================================================

    private void updateGlobalBest() {

        Bat bestBat =
                getBestBat();

        if (globalBest == null
                || bestBat.solution.objectiveValue
                < globalBest.objectiveValue) {

            globalBest =
                    copySolution(
                            bestBat.solution);
        }
    }

    // =========================================================
    // GET BEST BAT
    // =========================================================

    private Bat getBestBat() {

        return Collections.min(
                bats,
                Comparator.comparingDouble(
                        b ->
                                b.solution.objectiveValue));
    }

    // =========================================================
    // PELANGGAN VAN
    // =========================================================

    private List<Integer> getVanCustomers(
            Solution solution) {

        List<Integer> customers =
                new ArrayList<>();

        for (int i = 1;
                i < solution.vanRoute.size() - 1;
                i++) {

            customers.add(
                    solution.vanRoute.get(i));
        }

        return customers;
    }

    // =========================================================
    // COPY SOLUTION
    // =========================================================

    private Solution copySolution(
            Solution source) {

        Solution copy =
                new Solution();

        copy.vanRoute.addAll(
                source.vanRoute);

        for (Solution.DroneMission mission
                : source.droneMissions) {

            copy.droneMissions.add(
                    new Solution.DroneMission(

                            mission.launchPoint,

                            mission.customerPoint,

                            mission.rendezvousPoint));
        }

        copy.objectiveValue =
                source.objectiveValue;

        copy.fitness =
                source.fitness;

        copy.feasible =
                source.feasible;

        return copy;
    }

    // =========================================================
    // OUTPUT HASIL TERBAIK
    // =========================================================

    private void printBestSolution() {

        System.out.println();
        System.out.println("====================================");
        System.out.println("HASIL TERBAIK BAT ALGORITHM");
        System.out.println("====================================");

        System.out.println(
                "Status constraint : "
                        + (globalBest.feasible
                        ? "FEASIBLE"
                        : "TIDAK FEASIBLE"));

        System.out.println(
                "Rute van          : "
                        + globalBest.vanRoute);

        if (globalBest.droneMissions.isEmpty()) {

            System.out.println(
                    "Misi drone        : tidak digunakan");

        } else {

            Solution.DroneMission mission =
                    globalBest.droneMissions.get(0);

            System.out.println(
                    "Launch            : "
                            + parameter.labels.get(
                            mission.launchPoint));

            System.out.println(
                    "Customer drone    : "
                            + parameter.labels.get(
                            mission.customerPoint));

            System.out.println(
                    "Rendezvous        : "
                            + parameter.labels.get(
                            mission.rendezvousPoint));
        }

        System.out.println();

        System.out.printf(
                "Objective          : %.2f detik%n",
                globalBest.objectiveValue);

        System.out.printf(
                "Objective          : %.2f menit%n",
                globalBest.objectiveValue / 60.0);

        System.out.printf(
                "Fitness            : %.10f%n",
                globalBest.fitness);

        System.out.println("====================================");
    }
}