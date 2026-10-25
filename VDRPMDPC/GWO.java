import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class GWO {

    private final VDRPMDPCParameter parameter;
    private final Random random = new Random();

    private final int populationSize;
    private final int maxIterations;

    private Solution alpha;
    private Solution beta;
    private Solution delta;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public GWO(
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

        if (populationSize < 3) {
            throw new IllegalArgumentException(
                    "Ukuran populasi minimal 3.");
        }

        if (maxIterations < 1) {
            throw new IllegalArgumentException(
                    "Jumlah iterasi minimal 1.");
        }

        this.parameter = parameter;
        this.populationSize = populationSize;
        this.maxIterations = maxIterations;
    }

    // =========================================================
    // PROSES UTAMA GWO
    // =========================================================

    public Solution optimize() {

        List<Solution> population =
                new ArrayList<>();

        System.out.println();
        System.out.println("====================================");
        System.out.println("GREY WOLF OPTIMIZER - VDRPMDPC");
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
        // 1. POPULASI AWAL
        // =====================================================

        int attempts = 0;

        int maxAttempts =
                populationSize * 1000;

        while (population.size() < populationSize
                && attempts < maxAttempts) {

            attempts++;

            Solution candidate =
                    createRandomSolution();

            /*
             * Urutan evaluasi:
             *
             * 1. Constraint
             * 2. Objective
             * 3. Fitness
             */
            if (candidate != null
                    && evaluateSilently(candidate)) {

                population.add(
                        copySolution(candidate));
            }
        }

        if (population.size() < 3) {

            throw new IllegalStateException(
                    "Tidak ditemukan minimal tiga "
                            + "solusi feasible untuk memulai GWO.");
        }

        /*
         * Alpha, Beta, Delta awal ditentukan
         * dari populasi awal.
         *
         * Tidak perlu ditampilkan.
         */
        updateLeaders(population);

        // =====================================================
        // 2. ITERASI GWO
        // =====================================================

        for (int iteration = 1;
                iteration <= maxIterations;
                iteration++) {

            List<Solution> nextPopulation =
                    new ArrayList<>();

            // =================================================
            // ELITISME
            // =================================================

            nextPopulation.add(
                    copySolution(alpha));

            nextPopulation.add(
                    copySolution(beta));

            nextPopulation.add(
                    copySolution(delta));

            int failures = 0;

            int maxFailures =
                    populationSize * 100;

            // =================================================
            // PEMBENTUKAN GENERASI BARU
            // =================================================

            while (nextPopulation.size()
                    < populationSize
                    && failures < maxFailures) {

                Solution candidate;

                /*
                 * 15% kandidat dibuat secara random.
                 *
                 * Tujuan:
                 * menjaga keragaman populasi dan
                 * mengurangi premature convergence.
                 */
                if (random.nextDouble() < 0.15) {

                    candidate =
                            createRandomSolution();

                } else {

                    candidate =
                            createGuidedCandidate(
                                    iteration);
                }

                if (candidate != null
                        && evaluateSilently(candidate)) {

                    nextPopulation.add(
                            copySolution(candidate));

                    failures = 0;

                } else {

                    failures++;
                }
            }

            // =================================================
            // REFILL POPULASI JIKA BELUM PENUH
            // =================================================

            int refillAttempts = 0;

            int maxRefillAttempts =
                    populationSize * 100;

            while (nextPopulation.size()
                    < populationSize
                    && refillAttempts
                    < maxRefillAttempts) {

                refillAttempts++;

                Solution candidate =
                        createRandomSolution();

                if (candidate != null
                        && evaluateSilently(candidate)) {

                    nextPopulation.add(
                            copySolution(candidate));
                }
            }

            // =================================================
            // FALLBACK TERAKHIR
            // =================================================

            int emergencyAttempts = 0;

            while (nextPopulation.size()
                    < populationSize
                    && emergencyAttempts
                    < populationSize * 100) {

                emergencyAttempts++;

                /*
                 * Gunakan salah satu pemimpin,
                 * bukan Alpha terus-menerus.
                 */
                Solution candidate;

                int leaderChoice =
                        random.nextInt(3);

                if (leaderChoice == 0) {

                    candidate =
                            copySolution(alpha);

                } else if (leaderChoice == 1) {

                    candidate =
                            copySolution(beta);

                } else {

                    candidate =
                            copySolution(delta);
                }

                mutateRoute(candidate);

                if (random.nextDouble() < 0.40) {
                    mutateRoute(candidate);
                }

                if (random.nextDouble() < 0.30) {
                    mutateDroneMission(candidate);
                }

                candidate.feasible = false;
                candidate.objectiveValue =
                        Double.POSITIVE_INFINITY;
                candidate.fitness = 0.0;

                if (evaluateSilently(candidate)) {

                    nextPopulation.add(
                            copySolution(candidate));
                }
            }

            /*
             * Jika sangat sulit menemukan kandidat feasible,
             * gunakan Alpha sebagai fallback terakhir.
             */
            while (nextPopulation.size()
                    < populationSize) {

                nextPopulation.add(
                        copySolution(alpha));
            }

            population =
                    nextPopulation;

            // =================================================
            // UPDATE ALPHA, BETA, DELTA
            // =================================================

            updateLeaders(population);

            // =================================================
            // OUTPUT ITERASI
            // =================================================

            System.out.printf(
                    "Iterasi %3d | "
                            + "Objective: %.2f | "
                            + "Fitness: %.10f%n",
                    iteration,
                    alpha.objectiveValue,
                    alpha.fitness);
        }

        // =====================================================
        // 3. HASIL AKHIR
        // =====================================================

        printBestSolution();

        return copySolution(alpha);
    }

    // =========================================================
    // PEMBENTUKAN SOLUSI RANDOM
    // =========================================================

    private Solution createRandomSolution() {

        List<Integer> customers =
                new ArrayList<>();

        /*
         * Titik 0 adalah depot.
         * Pelanggan dimulai dari indeks 1.
         */
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
         * 50% kemungkinan kandidat mencoba
         * menggunakan drone.
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

        // Depot awal.
        solution.vanRoute.add(0);

        // Pelanggan van.
        solution.vanRoute.addAll(
                customers);

        // Depot akhir.
        solution.vanRoute.add(0);

        /*
         * Bentuk misi drone apabila memungkinkan.
         */
        if (droneCustomer >= 0
                && customers.size() >= 2) {

            addRandomDroneMission(
                    solution,
                    droneCustomer);

        } else if (droneCustomer >= 0) {

            /*
             * Jika misi drone tidak dapat dibuat,
             * pelanggan kembali dilayani van.
             */
            solution.vanRoute.add(
                    solution.vanRoute.size() - 1,
                    droneCustomer);
        }

        return solution;
    }

    // =========================================================
    // MEMBUAT MISI DRONE RANDOM
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
    // PEMBENTUKAN KANDIDAT GWO
    // =========================================================

    private Solution createGuidedCandidate(
            int iteration) {

        /*
         * Parameter a GWO.
         *
         * Awal iterasi mendekati 2.
         * Akhir iterasi mendekati 0.
         */
        double a =
                2.0
                        * (1.0
                        - (double) iteration
                        / maxIterations);

        /*
         * Nilai ini menentukan besarnya
         * eksplorasi pada suatu iterasi.
         */
        double explorationProbability =
                a / 2.0;

        Solution candidate;

        // =====================================================
        // PEMILIHAN PEMIMPIN AWAL
        // =====================================================

        /*
         * Pada awal iterasi:
         * peluang menggunakan Beta/Delta lebih tinggi.
         *
         * Pada akhir iterasi:
         * pencarian semakin fokus ke Alpha.
         */
        if (random.nextDouble()
                < explorationProbability) {

            if (random.nextBoolean()) {

                candidate =
                        copySolution(beta);

            } else {

                candidate =
                        copySolution(delta);
            }

        } else {

            candidate =
                    copySolution(alpha);
        }

        // =====================================================
        // PENGARUH ALPHA
        // =====================================================

        if (random.nextDouble() < 0.60) {

            candidate =
                    combineRoutes(
                            candidate,
                            alpha);
        }

        // =====================================================
        // PENGARUH BETA
        // =====================================================

        if (random.nextDouble() < 0.60) {

            candidate =
                    combineRoutes(
                            candidate,
                            beta);
        }

        // =====================================================
        // PENGARUH DELTA
        // =====================================================

        if (random.nextDouble() < 0.60) {

            candidate =
                    combineRoutes(
                            candidate,
                            delta);
        }

        // =====================================================
        // MUTASI ADAPTIF
        // =====================================================

        /*
         * Awal pencarian:
         * probabilitas mutasi lebih tinggi.
         *
         * Akhir pencarian:
         * probabilitas semakin rendah.
         */
        double mutationProbability =
                0.15
                        + 0.45
                        * (a / 2.0);

        // Mutasi pertama.
        if (random.nextDouble()
                < mutationProbability) {

            mutateRoute(candidate);
        }

        /*
         * Kadang dilakukan mutasi kedua.
         *
         * Ini mencegah populasi terlalu cepat
         * menjadi identik.
         */
        if (random.nextDouble()
                < mutationProbability * 0.50) {

            mutateRoute(candidate);
        }

        // =====================================================
        // MUTASI MISI DRONE
        // =====================================================

        if (random.nextDouble()
                < mutationProbability * 0.60) {

            mutateDroneMission(candidate);
        }

        /*
         * Kandidat harus dievaluasi ulang.
         */
        candidate.feasible = false;

        candidate.objectiveValue =
                Double.POSITIVE_INFINITY;

        candidate.fitness = 0.0;

        return candidate;
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

        // =====================================================
        // AMBIL SEBAGIAN STRUKTUR BASE
        // =====================================================

        for (int customer
                : baseCustomers) {

            if (customer != droneCustomer
                    && random.nextBoolean()
                    && !combined.contains(
                            customer)) {

                combined.add(customer);
            }
        }

        // =====================================================
        // MASUKKAN INFORMASI GUIDE
        // =====================================================

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

        // =====================================================
        // PASTIKAN SEMUA PELANGGAN TERCAKUP
        // =====================================================

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

        // =====================================================
        // VALIDASI STRUKTUR MISI DRONE
        // =====================================================

        if (!child.droneMissions.isEmpty()) {

            Solution.DroneMission mission =
                    child.droneMissions.get(0);

            int launchIndex =
                    child.vanRoute.indexOf(
                            mission.launchPoint);

            int rendezvousIndex =
                    child.vanRoute.indexOf(
                            mission.rendezvousPoint);

            /*
             * Jika launch/rendezvous tidak valid,
             * pelanggan drone kembali ke van.
             */
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
    // MUTASI RUTE VAN
    // =========================================================

    private void mutateRoute(
            Solution solution) {

        int routeSize =
                solution.vanRoute.size();

        /*
         * Depot pertama dan terakhir
         * tidak boleh dimutasi.
         */
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
        // SOLUSI SUDAH MEMILIKI DRONE
        // =====================================================

        if (!solution.droneMissions.isEmpty()) {

            /*
             * 25% kemungkinan menghapus drone.
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

            /*
             * Jika misi drone tetap digunakan,
             * ubah launch dan rendezvous.
             */
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
        // SOLUSI BELUM MEMILIKI DRONE
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

            /*
             * Seleksi awal berdasarkan kapasitas.
             *
             * Baterai tetap diperiksa oleh Constraint.
             */
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

        /*
         * Pelanggan drone dikeluarkan
         * dari rute van.
         */
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
            // 1. CEK CONSTRAINT
            // =================================================

            solution.feasible =
                    Constraint.check(
                            solution,
                            parameter);

            if (!solution.feasible) {

                return false;
            }

            // =================================================
            // 2. HITUNG OBJECTIVE
            // =================================================
            /*
             * Di dalam ObjectiveFunction:
             *
             * f(x) dihitung terlebih dahulu.
             *
             * Setelah objective diperoleh,
             * fitness = 1 / f(x).
             */

            ObjectiveFunction.calculateObjective(
                    solution,
                    parameter);

            // =================================================
            // 3. VALIDASI OBJECTIVE DAN FITNESS
            // =================================================

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
    // UPDATE ALPHA, BETA, DELTA
    // =========================================================

    private void updateLeaders(
            List<Solution> population) {

        /*
         * Karena objective adalah minimisasi,
         * objective terkecil adalah solusi terbaik.
         */
        population.sort(
                Comparator.comparingDouble(
                        s -> s.objectiveValue));

        alpha =
                copySolution(
                        population.get(0));

        beta =
                copySolution(
                        population.get(1));

        delta =
                copySolution(
                        population.get(2));
    }

    // =========================================================
    // MENGAMBIL PELANGGAN VAN
    // =========================================================

    private List<Integer> getVanCustomers(
            Solution solution) {

        List<Integer> customers =
                new ArrayList<>();

        /*
         * Indeks pertama dan terakhir
         * adalah depot.
         */
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
    // HASIL TERBAIK
    // =========================================================

    private void printBestSolution() {

        System.out.println();
        System.out.println("====================================");
        System.out.println("HASIL TERBAIK GWO");
        System.out.println("====================================");

        System.out.println(
                "Status constraint : "
                        + (alpha.feasible
                        ? "FEASIBLE"
                        : "TIDAK FEASIBLE"));

        System.out.println(
                "Rute van          : "
                        + alpha.vanRoute);

        if (alpha.droneMissions.isEmpty()) {

            System.out.println(
                    "Misi drone        : tidak digunakan");

        } else {

            Solution.DroneMission mission =
                    alpha.droneMissions.get(0);

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

        /*
         * Objective ditampilkan terlebih dahulu
         * sesuai tahapan penelitian.
         */
        System.out.printf(
                "Objective          : %.2f detik%n",
                alpha.objectiveValue);

        System.out.printf(
                "Objective          : %.2f menit%n",
                alpha.objectiveValue / 60.0);

        /*
         * Fitness berasal dari objective:
         *
         * Fitness = 1 / f(x)
         */
        System.out.printf(
                "Fitness            : %.10f%n",
                alpha.fitness);

        System.out.println("====================================");
    }
}