
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println("SIMULASI VDRPMDPC");
        System.out.println("UJI MISI DRONE DAN FITNESS");
        System.out.println("====================================");

        try {
            // 1. Memuat dataset dan parameter.
            VDRPMDPCParameter parameter =
                    new VDRPMDPCParameter();

            parameter.printSummary();

            // 2. Membuat solusi uji manual.
            Solution solution = new Solution();

            // Van mengunjungi Client 2 sampai Client 9.
            solution.vanRoute.add(0);

            for (int i = 2;
                    i < VDRPMDPCParameter.NUMBER_OF_POINTS;
                    i++) {
                solution.vanRoute.add(i);
            }

            // Van kembali ke depot.
            solution.vanRoute.add(0);

            // Misi drone:
            // launchPoint = Client 2
            // customerPoint = Client 1
            // rendezvousPoint = Client 3
            solution.droneMissions.add(
                    new Solution.DroneMission(2, 1, 3));

            // 3. Menampilkan solusi uji.
            System.out.println("\n====================================");
            System.out.println("RANCANGAN SOLUSI UJI");
            System.out.println("====================================");

            System.out.println(
                    "Rute van: " + solution.vanRoute);

            for (Solution.DroneMission mission
                    : solution.droneMissions) {

                System.out.println(
                        "Titik peluncuran drone : "
                        + parameter.labels.get(mission.launchPoint));

                System.out.println(
                        "Pelanggan yang dilayani: "
                        + parameter.labels.get(mission.customerPoint));

                System.out.println(
                        "Titik rendezvous       : "
                        + parameter.labels.get(mission.rendezvousPoint));
            }

            // 4. Memeriksa constraint.
            System.out.println("\n====================================");
            System.out.println("UJI CONSTRAINT");
            System.out.println("====================================");

            boolean feasible =
                    Constraint.check(solution, parameter);

            solution.feasible = feasible;

            if (!feasible) {
                System.out.println(
                        "Solusi ditolak. Objective dan fitness "
                        + "tidak dihitung.");
                return;
            }

            System.out.println(
                    "Solusi lolos pemeriksaan constraint "
                    + "yang telah diterapkan.");

            // 5. Menghitung objective dan fitness.
            System.out.println("\n====================================");
            System.out.println("PERHITUNGAN OBJECTIVE DAN FITNESS");
            System.out.println("====================================");

            double objective =
                    ObjectiveFunction.calculateObjective(
                            solution, parameter);

            System.out.println("\n====================================");
            System.out.println("RINGKASAN SOLUSI UJI");
            System.out.println("====================================");

            System.out.printf(
                    "Objective : %.2f detik%n",
                    objective);

            System.out.printf(
                    "Fitness   : %.10f%n",
                    solution.fitness);

            System.out.println(
                    "Status    : solusi uji berhasil dievaluasi");

            System.out.println(
                    "Catatan   : validasi sinkronisasi kronologis "
                    + "dan kesesuaian formulasi jurnal masih diperlukan.");

            System.out.println(
                    "GWO       : belum dijalankan");

            System.out.println(
                    "BA        : belum dijalankan");

        } catch (IOException e) {
            System.out.println(
                    "Gagal memuat dataset: " + e.getMessage());

        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Kesalahan simulasi: " + e.getMessage());
        }
    }
}
