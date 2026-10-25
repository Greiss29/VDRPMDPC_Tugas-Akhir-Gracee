
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DatasetLoader {

    public static class DistanceMatrix {
        public List<String> labels;
        public double[][] distances;

        public DistanceMatrix(List<String> labels,
                              double[][] distances) {
            this.labels = labels;
            this.distances = distances;
        }
    }

    public static DistanceMatrix loadMatrix(String filePath)
            throws IOException {

        List<String> lines = Files.readAllLines(
                Path.of(filePath), StandardCharsets.UTF_8);

        if (lines.isEmpty()) {
            throw new IOException("File CSV kosong: " + filePath);
        }

        // Baris pertama berisi judul dan nama titik.
        String[] header = lines.get(0).split(";", -1);

        // Nama titik dimulai dari kolom ketiga.
        List<String> labels = new ArrayList<>();

        for (int i = 2; i < header.length; i++) {
            labels.add(header[i].trim());
        }

        int n = labels.size();

        if (n == 0) {
            throw new IOException(
                    "Nama titik tidak ditemukan pada header.");
        }

        double[][] distances = new double[n][n];
        List<String> rowLabels = new ArrayList<>();

        // Baris kedua berisi alamat, jadi dilewati.
        // Data jarak dimulai dari baris ketiga.
        int rowIndex = 0;

        for (int i = 2; i < lines.size(); i++) {

            String line = lines.get(i);

            if (line.trim().isEmpty()) {
                continue;
            }

            // Dataset menggunakan titik koma sebagai pemisah.
            String[] cells = line.split(";", -1);

            if (cells.length < n + 2) {
                throw new IOException(
                        "Kolom tidak lengkap pada baris " + (i + 1)
                        + " di file " + filePath);
            }

            if (rowIndex >= n) {
                throw new IOException(
                        "Jumlah baris jarak melebihi jumlah titik.");
            }

            // Kolom pertama adalah nama titik.
            rowLabels.add(cells[0].trim());

            // Kolom kedua adalah alamat dan tidak digunakan
            // dalam perhitungan matriks jarak.
            for (int j = 0; j < n; j++) {

                String value = cells[j + 2].trim();

                try {
                    // Mengubah desimal berkoma menjadi desimal bertitik.
                    value = value.replace(',', '.');

                    distances[rowIndex][j] =
                            Double.parseDouble(value);

                } catch (NumberFormatException e) {
                    throw new IOException(
                            "Nilai jarak tidak valid pada baris "
                            + (i + 1) + ", kolom " + (j + 3)
                            + ": " + cells[j + 2], e);
                }
            }

            rowIndex++;
        }

        if (rowIndex != n) {
            throw new IOException(
                    "Jumlah baris jarak: " + rowIndex
                    + ", seharusnya: " + n);
        }

        if (!rowLabels.equals(labels)) {
            throw new IOException(
                    "Label baris dan kolom tidak cocok."
                    + "\nHeader: " + labels
                    + "\nBaris: " + rowLabels);
        }

        return new DistanceMatrix(labels, distances);
    }

    public static void printMatrix(
            String title, DistanceMatrix matrix) {

        System.out.println("\n" + title);
        System.out.println("Jumlah titik: " + matrix.labels.size());
        System.out.println("Label titik : " + matrix.labels);
    }
}
