
import java.io.IOException;

public class RunGWO {

    public static void main(String[] args) {

        try {
            VDRPMDPCParameter parameter =
                    new VDRPMDPCParameter();

            GWO gwo = new GWO(
                    parameter,
                    20,
                    100
            );

            gwo.optimize();

        } catch (IOException e) {
            System.out.println(
                    "Gagal memuat dataset: "
                    + e.getMessage());

        } catch (IllegalArgumentException
                | IllegalStateException e) {

            System.out.println(
                    "GWO gagal dijalankan: "
                    + e.getMessage());
        }
    }
}
