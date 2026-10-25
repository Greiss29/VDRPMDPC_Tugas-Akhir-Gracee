
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constraint {

    public static boolean check(
            Solution solution,
            VDRPMDPCParameter parameter) {

        // 1. Validasi awal
        if (solution == null || parameter == null) {
            return reject("Solusi atau parameter kosong.");
        }

        if (parameter.labels == null
                || parameter.demand == null
                || parameter.vanDistances == null
                || parameter.droneDistances == null) {
            return reject("Data parameter belum lengkap.");
        }

        int n = parameter.labels.size();

        if (n < 2
                || parameter.demand.length != n
                || parameter.vanDistances.length != n
                || parameter.droneDistances.length != n) {
            return reject("Ukuran dataset tidak konsisten.");
        }

        if (solution.vanRoute == null
                || solution.vanRoute.size() < 3
                || solution.droneMissions == null) {
            return reject("Rute van atau daftar misi tidak valid.");
        }

        if (VDRPMDPCParameter.K != 1
                || solution.droneMissions.size()
                > VDRPMDPCParameter.M
                || solution.droneMissions.size() > 1) {
            return reject(
                    "Implementasi ini mendukung satu van "
                    + "dan maksimal satu misi drone.");
        }

        if (!positive(VDRPMDPCParameter.VAN_SPEED_KMH)
                || !positive(VDRPMDPCParameter.DRONE_SPEED_KMH)
                || !positive(VDRPMDPCParameter.maxB)
                || !nonNegative(VDRPMDPCParameter.C)
                || !nonNegative(VDRPMDPCParameter.Cd)
                || !nonNegative(VDRPMDPCParameter.St)
                || !nonNegative(VDRPMDPCParameter.Prcd)
                || !nonNegative(VDRPMDPCParameter.cS)
                || !nonNegative(
                        VDRPMDPCParameter.launchTime100m)
                || !nonNegative(
                        VDRPMDPCParameter.landingTime100m)) {
            return reject("Parameter waktu, kecepatan, "
                    + "kapasitas, atau baterai tidak valid.");
        }

        int routeSize = solution.vanRoute.size();
        int lastIndex = routeSize - 1;

        if (solution.vanRoute.get(0) != 0
                || solution.vanRoute.get(lastIndex) != 0) {
            return reject(
                    "Rute van harus dimulai dan berakhir di depot.");
        }

        // 2. Validasi demand dan matriks jarak
        for (int i = 0; i < n; i++) {

            if (!nonNegative(parameter.demand[i])) {
                return reject(
                        "Demand tidak valid pada titik " + i);
            }

            if (parameter.vanDistances[i] == null
                    || parameter.droneDistances[i] == null
                    || parameter.vanDistances[i].length != n
                    || parameter.droneDistances[i].length != n) {
                return reject(
                        "Matriks jarak tidak konsisten pada titik "
                        + i);
            }

            for (int j = 0; j < n; j++) {
                if (!nonNegative(parameter.vanDistances[i][j])
                        || !nonNegative(
                                parameter.droneDistances[i][j])) {
                    return reject(
                            "Jarak tidak valid dari titik "
                            + i + " ke titik " + j);
                }
            }
        }

        // 3. Validasi rute van dan posisi setiap titik
        Set<Integer> servedCustomers = new HashSet<>();

        int[] vanPosition = new int[n];
        Arrays.fill(vanPosition, -1);

        for (int i = 0; i < routeSize; i++) {

            Integer pointObject = solution.vanRoute.get(i);

            if (pointObject == null) {
                return reject("Rute mengandung titik null.");
            }

            int point = pointObject;

            if (!isValidPoint(point, n)) {
                return reject(
                        "Indeks titik van tidak valid: " + point);
            }

            if (point == 0) {
                if (i != 0 && i != lastIndex) {
                    return reject(
                            "Depot tidak boleh muncul di tengah rute.");
                }
                continue;
            }

            if (!servedCustomers.add(point)) {
                return reject(
                        "Pelanggan van terduplikasi: "
                        + parameter.labels.get(point));
            }

            vanPosition[point] = i;
        }

        // 4. Validasi misi drone
        for (Solution.DroneMission mission
                : solution.droneMissions) {

            if (mission == null) {
                return reject("Misi drone bernilai null.");
            }

            int launch = mission.launchPoint;
            int customer = mission.customerPoint;
            int rendezvous = mission.rendezvousPoint;

            if (!isValidPoint(launch, n)
                    || !isValidPoint(customer, n)
                    || !isValidPoint(rendezvous, n)) {
                return reject(
                        "Indeks titik pada misi drone tidak valid.");
            }

            if (launch == 0 || rendezvous == 0) {
                return reject(
                        "Peluncuran dan rendezvous harus "
                        + "berada pada titik pelanggan.");
            }

            if (customer == 0
                    || launch == customer
                    || rendezvous == customer
                    || launch == rendezvous) {
                return reject(
                        "Titik peluncuran, pelanggan drone, "
                        + "dan rendezvous harus berbeda.");
            }

            if (vanPosition[launch] < 0
                    || vanPosition[rendezvous] < 0) {
                return reject(
                        "Peluncuran dan rendezvous harus "
                        + "berada pada rute van.");
            }

            if (vanPosition[rendezvous]
                    <= vanPosition[launch]) {
                return reject(
                        "Rendezvous harus terjadi setelah peluncuran.");
            }

            if (solution.vanRoute.contains(customer)) {
                return reject(
                        "Pelanggan drone juga terdapat pada rute van.");
            }

            if (!servedCustomers.add(customer)) {
                return reject(
                        "Pelanggan dilayani lebih dari sekali: "
                        + parameter.labels.get(customer));
            }

            if (parameter.demand[customer]
                    > VDRPMDPCParameter.Cd) {
                return reject(
                        "Kapasitas drone terlampaui pada "
                        + parameter.labels.get(customer));
            }
        }

        // 5. Pastikan semua pelanggan dilayani
        for (int point = 1; point < n; point++) {
            if (!servedCustomers.contains(point)) {
                return reject(
                        parameter.labels.get(point)
                        + " belum dilayani.");
            }
        }

        // 6. Validasi kapasitas van
        double vanLoad = 0.0;

        for (int point : solution.vanRoute) {
            if (point != 0) {
                vanLoad += parameter.demand[point];
            }
        }

        if (!Double.isFinite(vanLoad)
                || vanLoad > VDRPMDPCParameter.C) {
            return reject(
                    "Kapasitas van terlampaui. Muatan: "
                    + vanLoad + " kg.");
        }

        // 7. Buat jadwal awal van
        double[] arrival = new double[routeSize];
        double[] departure = new double[routeSize];

        double totalVanTravel = 0.0;
        double totalVanService = 0.0;

        for (int i = 1; i < routeSize; i++) {

            int from = solution.vanRoute.get(i - 1);
            int to = solution.vanRoute.get(i);

            double travelTime =
                    parameter.vanDistances[from][to]
                    / VDRPMDPCParameter.VAN_SPEED_KMH
                    * 3600.0;

            totalVanTravel += travelTime;

            arrival[i] = departure[i - 1] + travelTime;
            departure[i] = arrival[i];

            if (to != 0) {
                departure[i] += VDRPMDPCParameter.St;
                totalVanService += VDRPMDPCParameter.St;
            }
        }

        // 8. Validasi baterai dan sinkronisasi drone
        for (Solution.DroneMission mission
                : solution.droneMissions) {

            int launchIndex =
                    vanPosition[mission.launchPoint];

            int rendezvousIndex =
                    vanPosition[mission.rendezvousPoint];

            double outboundDistance =
                    parameter.droneDistances
                    [mission.launchPoint]
                    [mission.customerPoint];

            double returnDistance =
                    parameter.droneDistances
                    [mission.customerPoint]
                    [mission.rendezvousPoint];

            double outboundTime =
                    outboundDistance
                    / VDRPMDPCParameter.DRONE_SPEED_KMH
                    * 3600.0;

            double returnFlightTime =
                    returnDistance
                    / VDRPMDPCParameter.DRONE_SPEED_KMH
                    * 3600.0;

            double missionDuration =
                    VDRPMDPCParameter.Prcd
                    + VDRPMDPCParameter.launchTime100m
                    + outboundTime
                    + VDRPMDPCParameter.cS
                    + returnFlightTime
                    + VDRPMDPCParameter.landingTime100m;

            if (!Double.isFinite(missionDuration)
                    || missionDuration > VDRPMDPCParameter.maxB) {
                return reject(
                        "Misi drone melampaui batas baterai pada "
                        + parameter.labels.get(
                                mission.customerPoint)
                        + ". Durasi: "
                        + String.format(
                                "%.2f", missionDuration)
                        + " detik.");
            }

            // Waktu peluncuran drone.
            // Persiapan drone diasumsikan dapat berlangsung
            // bersamaan dengan pelayanan van.
            double vanDepartureBeforeLaunch =
                    departure[launchIndex];

            double droneLaunchTime =
                    arrival[launchIndex]
                    + VDRPMDPCParameter.Prcd
                    + VDRPMDPCParameter.launchTime100m;

            double launchWaiting = Math.max(
                    0.0,
                    droneLaunchTime - vanDepartureBeforeLaunch);

            departure[launchIndex] = Math.max(
                    vanDepartureBeforeLaunch,
                    droneLaunchTime);

            // Perbarui jadwal van setelah peluncuran.
            recalculateVanSchedule(
                    solution, parameter,
                    arrival, departure,
                    launchIndex + 1);

            // Perkiraan waktu kedatangan drone di rendezvous.
            double droneArrival =
                    droneLaunchTime
                    + outboundTime
                    + VDRPMDPCParameter.cS
                    + returnFlightTime
                    + VDRPMDPCParameter.landingTime100m;

            // Van siap setelah tiba dan menyelesaikan pelayanan
            // pada titik rendezvous.
            double vanReady = departure[rendezvousIndex];

            double vanWaiting = Math.max(
                    0.0, droneArrival - vanReady);

            double droneWaiting = Math.max(
                    0.0, vanReady - droneArrival);

            double synchronizedTime =
                    Math.max(vanReady, droneArrival);

            if (!Double.isFinite(synchronizedTime)
                    || !Double.isFinite(launchWaiting)
                    || !Double.isFinite(vanWaiting)
                    || !Double.isFinite(droneWaiting)) {
                return reject(
                        "Jadwal sinkronisasi van-drone tidak valid.");
            }

            // Van tidak boleh melanjutkan rute sebelum
            // kedua kendaraan siap di rendezvous.
            departure[rendezvousIndex] = synchronizedTime;

            recalculateVanSchedule(
                    solution, parameter,
                    arrival, departure,
                    rendezvousIndex + 1);

            System.out.println(
                    "Validasi misi drone: "
                    + parameter.labels.get(mission.launchPoint)
                    + " -> "
                    + parameter.labels.get(mission.customerPoint)
                    + " -> "
                    + parameter.labels.get(mission.rendezvousPoint));

            System.out.printf(
                    "Durasi misi drone : %.2f detik%n",
                    missionDuration);

            System.out.printf(
                    "Waktu tunggu peluncuran : %.2f detik%n",
                    launchWaiting);

            System.out.printf(
                    "Waktu tunggu van       : %.2f detik%n",
                    vanWaiting);

            System.out.printf(
                    "Waktu tunggu drone     : %.2f detik%n",
                    droneWaiting);

            System.out.printf(
                    "Waktu rendezvous       : %.2f detik%n",
                    synchronizedTime);
        }

        // 9. Validasi jadwal akhir
        for (int i = 0; i < routeSize; i++) {

            if (!Double.isFinite(arrival[i])
                    || !Double.isFinite(departure[i])
                    || departure[i] < arrival[i]) {
                return reject(
                        "Jadwal van tidak valid pada indeks "
                        + i + ".");
            }
        }

        double totalElapsedTime = departure[lastIndex];

        System.out.printf(
                "Total waktu tempuh van : %.2f detik%n",
                totalVanTravel);

        System.out.printf(
                "Total waktu pelayanan  : %.2f detik%n",
                totalVanService);

        System.out.printf(
                "Waktu selesai rute     : %.2f detik%n",
                totalElapsedTime);

        System.out.println(
                "Pemeriksaan struktur rute, pelayanan pelanggan, "
                + "kapasitas, baterai, dan jadwal berhasil.");

        return true;
    }

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

            double travelTime =
                    parameter.vanDistances[from][to]
                    / VDRPMDPCParameter.VAN_SPEED_KMH
                    * 3600.0;

            arrival[i] = departure[i - 1] + travelTime;
            departure[i] = arrival[i];

            if (to != 0) {
                departure[i] += VDRPMDPCParameter.St;
            }
        }
    }

    private static boolean isValidPoint(int point, int n) {
        return point >= 0 && point < n;
    }

    private static boolean nonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }

    private static boolean positive(double value) {
        return Double.isFinite(value) && value > 0.0;
    }

    private static boolean reject(String message) {
        System.out.println("Solusi ditolak: " + message);
        return false;
    }
}
