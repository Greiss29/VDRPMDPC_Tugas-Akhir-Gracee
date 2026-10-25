import java.io.IOException;

public class RunBA {

    public static void main(String[] args) {

        try {

            VDRPMDPCParameter parameter =
                    new VDRPMDPCParameter();

            int populationSize = 20;
            int maxIterations = 100;

            BatAlgorithm ba =
                    new BatAlgorithm(
                            parameter,
                            populationSize,
                            maxIterations);

            ba.optimize();

        } catch (IOException e) {

            System.out.println(
                    "Gagal membaca dataset: "
                    + e.getMessage());

            e.printStackTrace();
        }
    }
}