
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestSolutions {

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println("PENGUJIAN BEBERAPA SOLUSI VDRPMDPC");
        System.out.println("====================================");

        try {
            VDRPMDPCParameter parameter =
                    new VDRPMDPCParameter();

            List<Solution> solutions = new ArrayList<>();

            // SOLUSI 1: Semua pelanggan dilayani van
            Solution s1 = new Solution();

            s1.vanRoute.add(0);

            for (int i = 1;
                    i < VDRPMDPCParameter.NUMBER_OF_POINTS;
                    i++) {
                s1.vanRoute.add(i);
            }

            s1.vanRoute.add(0);
            solutions.add(s1);

            // SOLUSI 2: Drone melayani Client 1
            // Launch: Client 2, Rendezvous: Client 3
            Solution s2 = new Solution();

            s2.vanRoute.add(0);
            s2.vanRoute.add(2);

            for (int i = 3;
                    i < VDRPMDPCParameter.NUMBER_OF_POINTS;
                    i++) {
                s2.vanRoute.add(i);
            }

            s2.vanRoute.add(0);

            s2.droneMissions.add(
                    new Solution.DroneMission(2, 1, 3));

            solutions.add(s2);

            // SOLUSI 3: Drone melayani Client 4
            // Launch: Client 2, Rendezvous: Client 5
            Solution s3 = new Solution();

            s3.vanRoute.add(0);
            s3.vanRoute.add(1);
            s3.vanRoute.add(2);
            s3.vanRoute.add(3);

            for (int i = 5;
                    i < VDRPMDPCParameter.NUMBER_OF_POINTS;
                    i++) {
                s3.vanRoute.add(i);
            }

            s3.vanRoute.add(0);

            s3.droneMissions.add(
                    new Solution.DroneMission(2, 4, 5));

            solutions.add(s3);

            double bestObjective =
                    Double.POSITIVE_INFINITY;

            int bestSolution = -1;

            // Evaluasi seluruh solusi
            for (int i = 0; i < solutions.size(); i++) {

                Solution solution = solutions.get(i);

                System.out.println("\n------------------------------------");
                System.out.println("SOLUSI " + (i + 1));
                System.out.println("------------------------------------");

                System.out.println(
                        "Rute van: " + solution.vanRoute);

                for (Solution.DroneMission mission
                        : solution.droneMissions) {

                    System.out.println(
                            "Drone: "
                            + parameter.labels.get(mission.launchPoint)
                            + " -> "
                            + parameter.labels.get(mission.customerPoint)
                            + " -> "
                            + parameter.labels.get(mission.rendezvousPoint));
                }

                boolean feasible =
                        Constraint.check(solution, parameter);

                solution.feasible = feasible;

                if (!feasible) {
                    System.out.println(
                            "Status: TIDAK LOLOS CONSTRAINT");
                    continue;
                }

                try {
                    double objective =
                            ObjectiveFunction.calculateObjective(
                                    solution, parameter);

                    System.out.printf(
                            "Objective: %.2f detik%n",
                            objective);

                    System.out.printf(
                            "Fitness: %.10f%n",
                            solution.fitness);

                    if (objective < bestObjective) {
                        bestObjective = objective;
                        bestSolution = i + 1;
                    }

                } catch (IllegalArgumentException e) {
                    System.out.println(
                            "Evaluasi gagal: " + e.getMessage());
                }
            }

            System.out.println("\n====================================");
            System.out.println("RINGKASAN PERBANDINGAN");
            System.out.println("====================================");

            if (bestSolution != -1) {
                System.out.println(
                        "Solusi dengan objective terkecil: Solusi "
                        + bestSolution);

                System.out.printf(
                        "Objective terkecil: %.2f detik%n",
                        bestObjective);
            } else {
                System.out.println(
                        "Belum ada solusi yang berhasil dievaluasi.");
            }

            System.out.println(
                    "\nCatatan: hasil ini merupakan pengujian awal, "
                    + "bukan hasil optimasi GWO atau BA.");

        } catch (IOException e) {
            System.out.println(
                    "Gagal memuat dataset: " + e.getMessage());
        }
    }
}
