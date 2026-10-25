
import java.io.IOException;
import java.util.List;

public class VDRPMDPCParameter {

    // ==========================================
    // 1. JUMLAH TITIK DAN KENDARAAN
    // ==========================================

    // 10 titik: 1 depot + 9 pelanggan
    public static final int NUMBER_OF_POINTS = 10;

    // Konfigurasi simulasi awal
    public static final int K = 1; // Jumlah van
    public static final int M = 1; // Jumlah drone

    // ==========================================
    // 2. KECEPATAN KENDARAAN
    // ==========================================

    // Asumsi skenario simulasi, perlu diverifikasi
    // terhadap referensi dan formulasi penelitian.
    public static final double VAN_SPEED_KMH = 40.0;
    public static final double DRONE_SPEED_KMH = 60.0;

    // ==========================================
    // 3. KAPASITAS KENDARAAN
    // ==========================================

    public static final double C = 705.0;  // Kapasitas van (kg)
    public static final double Cd = 6.0;   // Kapasitas drone (kg)

    // ==========================================
    // 4. PARAMETER WAKTU (DETIK)
    // ==========================================

    public static final double St = 300.0;
    // Waktu pelayanan pelanggan oleh van

    public static final double cS = 30.0;
    // Waktu pelayanan pelanggan oleh drone

    public static final double maxB = 1080.0;
    // Batas waktu operasi drone

    public static final double Prcd = 300.0;
    // Waktu persiapan drone sebelum peluncuran

    public static final double launchTime100m = 20.0;
    // Waktu peluncuran berdasarkan data parameter

    public static final double landingTime100m = 33.33;
    // Waktu pendaratan berdasarkan data parameter

    // ==========================================
    // 5. DEMAND PELANGGAN (KG)
    // Indeks 0 = Depot; indeks 1-9 = pelanggan
    // ==========================================

    public final double[] demand = {
        0.0,  // Depot
        3.0,  // Client 1
        4.0,  // Client 2
        3.0,  // Client 3
        3.0,  // Client 4
        1.0,  // Client 5
        3.0,  // Client 6
        5.0,  // Client 7
        5.0,  // Client 8
        19.0  // Client 9
    };

    // ==========================================
    // 6. DATASET
    // ==========================================

    public final List<String> labels;
    public final double[][] vanDistances;
    public final double[][] droneDistances;

    // ==========================================
    // 7. MEMUAT DAN MEMVALIDASI DATASET
    // ==========================================

    public VDRPMDPCParameter() throws IOException {

        DatasetLoader.DistanceMatrix van =
                DatasetLoader.loadMatrix(
                        "Data/van_distance.csv");

        DatasetLoader.DistanceMatrix drone =
                DatasetLoader.loadMatrix(
                        "Data/drone_distance.csv");

        // Pastikan label titik van dan drone sama.
        if (!van.labels.equals(drone.labels)) {
            throw new IOException(
                    "Label titik van dan drone tidak cocok.");
        }

        // Pastikan jumlah titik sesuai.
        if (van.labels.size() != NUMBER_OF_POINTS) {
            throw new IOException(
                    "Jumlah titik dataset tidak sesuai. "
                    + "Seharusnya "
                    + NUMBER_OF_POINTS + " titik.");
        }

        // Pastikan jumlah demand sesuai jumlah titik.
        if (demand.length != NUMBER_OF_POINTS) {
            throw new IOException(
                    "Jumlah data demand tidak sesuai "
                    + "dengan jumlah titik.");
        }

        // Pastikan ukuran matriks van dan drone benar.
        validateMatrix(van.distances, "van");
        validateMatrix(drone.distances, "drone");

        this.labels = van.labels;
        this.vanDistances = van.distances;
        this.droneDistances = drone.distances;
    }

    private static void validateMatrix(
            double[][] matrix,
            String vehicleName) throws IOException {

        if (matrix.length != NUMBER_OF_POINTS) {
            throw new IOException(
                    "Jumlah baris matriks " + vehicleName
                    + " tidak sesuai.");
        }

        for (int i = 0; i < matrix.length; i++) {

            if (matrix[i] == null
                    || matrix[i].length != NUMBER_OF_POINTS) {
                throw new IOException(
                        "Ukuran matriks " + vehicleName
                        + " tidak valid pada baris " + i + ".");
            }

            for (int j = 0; j < matrix[i].length; j++) {

                if (!Double.isFinite(matrix[i][j])
                        || matrix[i][j] < 0.0) {
                    throw new IOException(
                            "Nilai jarak " + vehicleName
                            + " tidak valid pada baris "
                            + i + ", kolom " + j + ".");
                }
            }
        }
    }

    // ==========================================
    // 8. MENAMPILKAN RINGKASAN PARAMETER
    // ==========================================

    public void printSummary() {

        System.out.println(
                "\n====================================");
        System.out.println(
                "PARAMETER SIMULASI VDRPMDPC");
        System.out.println(
                "====================================");

        System.out.println(
                "Jumlah titik       : " + NUMBER_OF_POINTS);
        System.out.println(
                "Jumlah pelanggan   : "
                + (NUMBER_OF_POINTS - 1));

        System.out.println("Jumlah van         : " + K);
        System.out.println("Jumlah drone       : " + M);

        System.out.println(
                "Kecepatan van      : "
                + VAN_SPEED_KMH + " km/jam (asumsi)");

        System.out.println(
                "Kecepatan drone    : "
                + DRONE_SPEED_KMH + " km/jam (asumsi)");

        System.out.println(
                "Kapasitas van      : " + C + " kg");
        System.out.println(
                "Kapasitas drone    : " + Cd + " kg");

        System.out.println(
                "Pelayanan van      : " + St + " detik");
        System.out.println(
                "Pelayanan drone    : " + cS + " detik");

        System.out.println(
                "Baterai drone      : " + maxB + " detik");
        System.out.println(
                "Persiapan drone    : " + Prcd + " detik");

        System.out.println(
                "Waktu peluncuran   : "
                + launchTime100m + " detik");

        System.out.println(
                "Waktu pendaratan   : "
                + landingTime100m + " detik");

        System.out.println("\nDemand pelanggan:");

        for (int i = 1; i < demand.length; i++) {
            System.out.println(
                    labels.get(i) + " = " + demand[i] + " kg");
        }

        System.out.println(
                "\nMatriks van        : "
                + vanDistances.length + " x "
                + vanDistances.length);

        System.out.println(
                "Matriks drone      : "
                + droneDistances.length + " x "
                + droneDistances.length);

        System.out.println(
                "Status             : konfigurasi berhasil dimuat");
    }
}
