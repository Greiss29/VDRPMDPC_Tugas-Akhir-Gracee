
import java.util.ArrayList;
import java.util.List;

public class Solution {

    // Rute van: urutan indeks titik yang dikunjungi
    // Contoh: 0 = Depot, 1 = Client 1
    public List<Integer> vanRoute;

    // Daftar misi drone
    public List<DroneMission> droneMissions;

    // Nilai objective dan fitness
    // Belum dihitung pada tahap ini
    public double objectiveValue;
    public double fitness;

    // Status kelayakan solusi
    public boolean feasible;

    public Solution() {
        vanRoute = new ArrayList<>();
        droneMissions = new ArrayList<>();

        objectiveValue = Double.POSITIVE_INFINITY;
        fitness = 0.0;
        feasible = false;
    }

    // Struktur untuk menyimpan satu misi drone
    public static class DroneMission {

        public int launchPoint;
        public int customerPoint;
        public int rendezvousPoint;

        public DroneMission(
                int launchPoint,
                int customerPoint,
                int rendezvousPoint) {

            this.launchPoint = launchPoint;
            this.customerPoint = customerPoint;
            this.rendezvousPoint = rendezvousPoint;
        }
    }
}
