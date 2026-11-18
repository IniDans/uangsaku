import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.stream.Stream;
import repository.FileRepository;
import service.SistemKeuangan;
import util.AppConfig;

/** Helper assertion sederhana (tanpa JUnit, agar tidak butuh dependency). */
public final class Uji {

    static int lulus = 0;
    static int gagal = 0;
    static String kasus = "";

    /** "Hari ini" tetap saat pengujian. */
    static final LocalDate HARI_INI = LocalDate.of(2026, 11, 18);

    private Uji() {
    }

    interface Aksi {
        void jalankan() throws Exception;
    }

    static void kasus(String nama, Aksi aksi) {
        kasus = nama;
        int gagalSebelum = gagal;
        try {
            aksi.jalankan();
        } catch (Throwable t) {
            gagal++;
            System.out.println("  GAGAL  " + nama + " -> exception: " + t);
            return;
        }
        if (gagal == gagalSebelum) {
            System.out.println("  OK     " + nama);
        }
    }

    static void sama(Object harapan, Object aktual, String pesan) {
        if (harapan == null ? aktual == null : harapan.equals(aktual)) {
            lulus++;
        } else {
            gagal++;
            System.out.println("  GAGAL  " + kasus + " | " + pesan + " | harapan=" + harapan + " aktual=" + aktual);
        }
    }

    static void benar(boolean kondisi, String pesan) {
        sama(true, kondisi, pesan);
    }

    static void kira(double harapan, double aktual, double toleransi, String pesan) {
        benar(Math.abs(harapan - aktual) <= toleransi, pesan + " (harapan≈" + harapan + ", aktual=" + aktual + ")");
    }

    /** Memastikan aksi melempar IllegalArgumentException dan pesannya mengandung potongan teks. */
    static void ditolak(Aksi aksi, String potonganPesan, String pesan) {
        try {
            aksi.jalankan();
            gagal++;
            System.out.println("  GAGAL  " + kasus + " | " + pesan + " | seharusnya ditolak");
        } catch (IllegalArgumentException e) {
            if (potonganPesan == null || e.getMessage().contains(potonganPesan)) {
                lulus++;
            } else {
                gagal++;
                System.out.println("  GAGAL  " + kasus + " | " + pesan + " | pesan error: " + e.getMessage());
            }
        } catch (Exception e) {
            gagal++;
            System.out.println("  GAGAL  " + kasus + " | " + pesan + " | exception tak terduga: " + e);
        }
    }

    static Clock jam() {
        ZoneId zona = ZoneId.of("Asia/Jakarta");
        return Clock.fixed(HARI_INI.atTime(10, 0).atZone(zona).toInstant(), zona);
    }

    static Path folderSementara() throws IOException {
        return Files.createTempDirectory("uangsaku-test");
    }

    /** Sistem kosong (tanpa seed) di folder sementara. */
    static SistemKeuangan sistemKosong(Path folder) throws IOException {
        Path seedKosong = folder.resolve("seed-kosong");
        Files.createDirectories(seedKosong);
        return sistemDari(folder.resolve("data"), seedKosong);
    }

    static SistemKeuangan sistemDari(Path folderData, Path folderSeed) {
        AppConfig config = AppConfig.muat(folderData.resolve("config.properties"));
        SistemKeuangan sistem = new SistemKeuangan(new FileRepository(folderData, folderSeed), config, jam());
        sistem.muatData();
        return sistem;
    }

    static void hapusFolder(Path folder) {
        if (folder == null || !Files.exists(folder)) {
            return;
        }
        try (Stream<Path> semua = Files.walk(folder)) {
            semua.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            System.out.println("  (gagal menghapus folder sementara " + folder + ")");
        }
    }
}
