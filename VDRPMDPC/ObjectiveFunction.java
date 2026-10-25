
public class ObjectiveFunction {

    public static double calculateObjective(
            Solution solution,
            VDRPMDPCParameter parameter) {

        // 1. Validasi awal
        if (solution == null || parameter == null) {
            throw new IllegalArgumentException(
                    "Solusi dan parameter tidak boleh null.");
        }

        if (!solution.feasible) {
            throw new IllegalArgumentException(
                    "Solusi belum lolos pemeriksaan constraint.");
        }

        if (solution.vanRoute == null
                || solution.vanRoute.size() < 3) {
            throw new IllegalArgumentException(
                    "Rute van tidak valid.");
        }

        if (solution.droneMissions == null
                || solution.droneMissions.size()
                > VDRPMDPCParameter.M) {
            throw new IllegalArgumentException(
                    "Jumlah misi drone tidak valid.");
        }

        if (VDRPMDPCParameter.K != 1
                || VDRPMDPCParameter.M != 1) {
            throw new IllegalArgumentException(
                    "Implementasi ini mendukung satu van "
                    + "dan maksimal satu drone.");
        }

        if (VDRPMDPCParameter.VAN_SPEED_KMH <= 0
                || VDRPMDPCParameter.DRONE_SPEED_KMH <= 0) {
            throw new IllegalArgumentException(
                    "Kecepatan kendaraan harus positif.");
        }

        int routeSize = solution.vanRoute.size();
        int lastIndex = routeSize - 1;

        if (parameter.labels == null) {
            throw new IllegalArgumentException(
                    "Daftar label titik tidak tersedia.");
        }

        int numberOfPoints = parameter.labels.size();

        if (numberOfPoints < 2
                || parameter.demand == null
                || parameter.demand.length != numberOfPoints
                || !validMatrix(parameter.vanDistances,
                                numberOfPoints)
                || !validMatrix(parameter.droneDistances,
                                numberOfPoints)) {
            throw new IllegalArgumentException(
                    "Data demand atau matriks jarak tidak valid.");
        }

        if (solution.vanRoute.get(0) != 0
                || solution.vanRoute.get(lastIndex) != 0) {
            throw new IllegalArgumentException(
                    "Rute van harus dimulai dan berakhir di depot.");
        }

        for (int point : solution.vanRoute) {
            validatePoint(point, parameter);
        }

        // 2. Inisialisasi jadwal van.
        // Seluruh waktu perhitungan menggunakan detik.
        double[] arrival = new double[routeSize];
        double[] departure = new double[routeSize];

        double vanTravelTime = 0.0;
        double vanServiceTime = 0.0;
        double vanWaitingTime = 0.0;
        double droneWaitingTime = 0.0;
        double droneFlightTime = 0.0;
        double droneMissionDuration = 0.0;
        double rendezvousTime = 0.0;
        double launchWaiting = 0.0;

        arrival[0] = 0.0;
        departure[0] = 0.0;

        // 3. Jadwal awal van tanpa penundaan drone.
        for (int i = 1; i < routeSize; i++) {

            int from = solution.vanRoute.get(i - 1);
            int to = solution.vanRoute.get(i);

            double distance =
                    parameter.vanDistances[from][to];

            validateDistance(distance, from, to);

            double travelSeconds =
                    distance
                    / VDRPMDPCParameter.VAN_SPEED_KMH
                    * 3600.0;

            vanTravelTime += travelSeconds;

            arrival[i] =
                    departure[i - 1] + travelSeconds;

            departure[i] = arrival[i];

            // Depot tidak memerlukan waktu pelayanan.
            if (to != 0) {
                departure[i] += VDRPMDPCParameter.St;
                vanServiceTime += VDRPMDPCParameter.St;
            }
        }

        // 4. Evaluasi misi drone jika tersedia.
        if (!solution.droneMissions.isEmpty()) {

            Solution.DroneMission mission =
                    solution.droneMissions.get(0);

            validatePoint(mission.launchPoint, parameter);
            validatePoint(mission.customerPoint, parameter);
            validatePoint(mission.rendezvousPoint, parameter);

            if (mission.customerPoint == 0
                    || mission.launchPoint == mission.customerPoint
                    || mission.rendezvousPoint == mission.customerPoint) {
                throw new IllegalArgumentException(
                        "Titik pelanggan drone tidak valid.");
            }

            if (solution.vanRoute.contains(mission.customerPoint)) {
                throw new IllegalArgumentException(
                        "Pelanggan drone juga tercantum pada rute van.");
            }

            if (parameter.demand[mission.customerPoint]
                    > VDRPMDPCParameter.Cd) {
                throw new IllegalArgumentException(
                        "Demand pelanggan melebihi kapasitas drone.");
            }

            int launchIndex =
                    solution.vanRoute.indexOf(mission.launchPoint);

            int rendezvousIndex =
                    solution.vanRoute.indexOf(mission.rendezvousPoint);

            if (launchIndex < 0
                    || rendezvousIndex <= launchIndex) {
                throw new IllegalArgumentException(
                        "Rendezvous harus berada setelah peluncuran "
                        + "pada rute van.");
            }

            // 4A. Waktu persiapan dan peluncuran drone.
            // Persiapan dimulai ketika van tiba.
            // Persiapan berlangsung bersamaan dengan pelayanan van.
            double launchStartTime = arrival[launchIndex];

            double droneDeparture =
                    launchStartTime
                    + VDRPMDPCParameter.Prcd
                    + VDRPMDPCParameter.launchTime100m;

            double vanDepartureBeforeLaunch =
                    departure[launchIndex];

            // Van menunggu jika drone belum siap diluncurkan.
            launchWaiting = Math.max(
                    0.0,
                    droneDeparture - vanDepartureBeforeLaunch);

            vanWaitingTime += launchWaiting;

            departure[launchIndex] = Math.max(
                    vanDepartureBeforeLaunch,
                    droneDeparture);

            // Perbarui jadwal van setelah peluncuran.
            recalculateVanSchedule(
                    solution,
                    parameter,
                    arrival,
                    departure,
                    launchIndex + 1);

            // 4B. Hitung waktu penerbangan drone.
            double outboundDistance =
                    parameter.droneDistances
                            [mission.launchPoint]
                            [mission.customerPoint];

            double returnDistanceDrone =
                    parameter.droneDistances
                            [mission.customerPoint]
                            [mission.rendezvousPoint];

            validateDistance(
                    outboundDistance,
                    mission.launchPoint,
                    mission.customerPoint);

            validateDistance(
                    returnDistanceDrone,
                    mission.customerPoint,
                    mission.rendezvousPoint);

            double outboundTime =
                    outboundDistance
                    / VDRPMDPCParameter.DRONE_SPEED_KMH
                    * 3600.0;

            double returnFlightTime =
                    returnDistanceDrone
                    / VDRPMDPCParameter.DRONE_SPEED_KMH
                    * 3600.0;

            droneFlightTime =
                    outboundTime + returnFlightTime;

            // Total durasi operasi drone.
            // Mencakup persiapan, peluncuran, penerbangan,
            // pelayanan pelanggan, dan pendaratan.
            droneMissionDuration =
                    VDRPMDPCParameter.Prcd
                    + VDRPMDPCParameter.launchTime100m
                    + outboundTime
                    + VDRPMDPCParameter.cS
                    + returnFlightTime
                    + VDRPMDPCParameter.landingTime100m;

            if (!Double.isFinite(droneMissionDuration)
                    || droneMissionDuration
                    > VDRPMDPCParameter.maxB) {
                throw new IllegalArgumentException(
                        "Durasi misi drone melebihi batas baterai: "
                        + droneMissionDuration + " detik.");
            }

            // Waktu drone sampai di titik rendezvous.
            double droneArrival =
                    droneDeparture
                    + outboundTime
                    + VDRPMDPCParameter.cS
                    + returnFlightTime
                    + VDRPMDPCParameter.landingTime100m;

            // Van sudah tiba dan selesai melayani pelanggan
            // pada titik rendezvous.
            double vanReadyTime = departure[rendezvousIndex];

            // Sinkronisasi waktu van dan drone.
            rendezvousTime = Math.max(
                    vanReadyTime,
                    droneArrival);

            // Jika drone terlambat, van menunggu.
            double rendezvousVanWaiting = Math.max(
                    0.0,
                    droneArrival - vanReadyTime);

            vanWaitingTime += rendezvousVanWaiting;

            // Jika drone tiba lebih dahulu, drone mendarat
            // dan menunggu van untuk menghemat baterai.
            droneWaitingTime += Math.max(
                    0.0,
                    vanReadyTime - droneArrival);

            departure[rendezvousIndex] = rendezvousTime;

            // Perbarui jadwal setelah rendezvous.
            recalculateVanSchedule(
                    solution,
                    parameter,
                    arrival,
                    departure,
                    rendezvousIndex + 1);
        }

        // 5. Waktu penyelesaian pelayanan seluruh pelanggan.
        // Waktu ini belum mencakup perjalanan dari pelanggan
        // terakhir menuju depot.
        double deliveryTime = departure[lastIndex - 1];

        // 6. Waktu perjalanan dari pelanggan terakhir ke depot.
        int lastCustomer =
                solution.vanRoute.get(lastIndex - 1);

        double returnDistance =
                parameter.vanDistances[lastCustomer][0];

        validateDistance(returnDistance, lastCustomer, 0);

        double returnTime =
                returnDistance
                / VDRPMDPCParameter.VAN_SPEED_KMH
                * 3600.0;

        // 7. Fungsi objective untuk minimisasi waktu.
        // T_delivery adalah waktu penyelesaian pelayanan.
        // T_return adalah perjalanan kembali ke depot.
        double objective = deliveryTime + returnTime;

        if (!Double.isFinite(objective) || objective <= 0.0) {
            throw new IllegalArgumentException(
                    "Nilai objective tidak valid.");
        }

        // 8. Fitness untuk masalah minimisasi.
        double fitness = 1.0 / objective;

        solution.objectiveValue = objective;
        solution.fitness = fitness;

        // 9. Tampilkan hasil evaluasi.
        System.out.println("\n====================================");
        System.out.println("OBJECTIVE DAN FITNESS");
        System.out.println("====================================");

        System.out.printf(
                "Waktu tempuh van seluruh rute : %.2f detik%n",
                vanTravelTime);

        System.out.printf(
                "Waktu pelayanan van           : %.2f detik%n",
                vanServiceTime);

        System.out.printf(
                "Waktu tunggu peluncuran       : %.2f detik%n",
                launchWaiting);

        System.out.printf(
                "Waktu tunggu van               : %.2f detik%n",
                vanWaitingTime);

        System.out.printf(
                "Waktu tunggu drone             : %.2f detik%n",
                droneWaitingTime);

        System.out.printf(
                "Waktu terbang drone            : %.2f detik%n",
                droneFlightTime);

        System.out.printf(
                "Durasi operasi drone           : %.2f detik%n",
                droneMissionDuration);

        System.out.printf(
                "Waktu rendezvous               : %.2f detik%n",
                rendezvousTime);

        System.out.printf(
                "T_delivery                     : %.2f detik%n",
                deliveryTime);

        System.out.printf(
                "T_return                       : %.2f detik%n",
                returnTime);

        System.out.printf(
                "f(xi)                          : %.2f detik%n",
                objective);

        System.out.printf(
                "Objective                      : %.2f menit%n",
                objective / 60.0);

        System.out.printf(
                "Fitness = 1/f(xi)              : %.10f%n",
                fitness);

        return objective;
    }

    // Memperbarui jadwal van setelah terjadi penundaan.
    private static void recalculateVanSchedule(
            Solution solution,
            VDRPMDPCParameter parameter,
            double[] arrival,
            double[] departure,
            int startIndex) {

        for (int i = startIndex;
                i < solution.vanRoute.size();
                i++) {

            int from = solution.vanRoute.get(i - 1);
            int to = solution.vanRoute.get(i);

            double distance =
                    parameter.vanDistances[from][to];

            validateDistance(distance, from, to);

            double travelSeconds =
                    distance
                    / VDRPMDPCParameter.VAN_SPEED_KMH
                    * 3600.0;

            arrival[i] =
                    departure[i - 1] + travelSeconds;

            departure[i] = arrival[i];

            if (to != 0) {
                departure[i] += VDRPMDPCParameter.St;
            }
        }
    }

    private static boolean validMatrix(
            double[][] matrix,
            int size) {

        if (matrix == null || matrix.length != size) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            if (matrix[i] == null || matrix[i].length != size) {
                return false;
            }
        }

        return true;
    }

    private static void validatePoint(
            int point,
            VDRPMDPCParameter parameter) {

        if (parameter.labels == null
                || point < 0
                || point >= parameter.labels.size()) {
            throw new IllegalArgumentException(
                    "Indeks titik tidak valid: " + point);
        }
    }

    private static void validateDistance(
            double distance,
            int from,
            int to) {

        if (!Double.isFinite(distance) || distance < 0.0) {
            throw new IllegalArgumentException(
                    "Jarak tidak valid dari titik "
                    + from + " ke titik " + to + ".");
        }
    }
}
