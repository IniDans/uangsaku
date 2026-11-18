package util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Konfigurasi yang mudah diubah tanpa mengubah kode (data/config.properties).
 * Jika file belum ada, dibuat dengan nilai default. Nilai yang rusak diganti default.
 */
public class AppConfig {

    private static final String KEY_PERSEN_MAKAN = "budget.persenMakan";
    private static final String KEY_PERSEN_HIBURAN = "budget.persenHiburan";
    private static final String KEY_TIER_SEDANG = "tier.sedang.min";
    private static final String KEY_TIER_LUMAYAN = "tier.lumayan.min";
    private static final String KEY_TIER_NYAMAN = "tier.nyaman.min";
    private static final String KEY_AMBANG_SISA = "peringatan.ambangSisa";
    private static final String KEY_AMBANG_TETAP = "peringatan.ambangPengeluaranTetap";
    private static final String KEY_KOTA_ACUAN = "kota.acuan";

    // Nilai default (data dummy pengalaman pribadi, kota acuan Malang)
    public static final double DEFAULT_PERSEN_MAKAN = 0.70;
    public static final double DEFAULT_PERSEN_HIBURAN = 0.30;
    public static final long DEFAULT_TIER_SEDANG = 800_000;
    public static final long DEFAULT_TIER_LUMAYAN = 1_400_000;
    public static final long DEFAULT_TIER_NYAMAN = 2_200_000;
    public static final double DEFAULT_AMBANG_SISA = 0.20;
    public static final double DEFAULT_AMBANG_TETAP = 0.60;
    public static final String DEFAULT_KOTA_ACUAN = "Malang (Kota)";

    private final Path file;
    private double persenMakan = DEFAULT_PERSEN_MAKAN;
    private double persenHiburan = DEFAULT_PERSEN_HIBURAN;
    private long tierMinSedang = DEFAULT_TIER_SEDANG;
    private long tierMinLumayan = DEFAULT_TIER_LUMAYAN;
    private long tierMinNyaman = DEFAULT_TIER_NYAMAN;
    private double ambangSisa = DEFAULT_AMBANG_SISA;
    private double ambangPengeluaranTetap = DEFAULT_AMBANG_TETAP;
    private String kotaAcuan = DEFAULT_KOTA_ACUAN;

    private AppConfig(Path file) {
        this.file = file;
    }

    /** Membaca konfigurasi. File yang belum ada dibuat dengan nilai default. */
    public static AppConfig muat(Path file) {
        AppConfig config = new AppConfig(file);
        if (!Files.exists(file)) {
            try {
                config.simpan();
            } catch (IOException e) {
                System.err.println("[AppConfig] Gagal membuat " + file + ": " + e.getMessage());
            }
            return config;
        }
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException e) {
            System.err.println("[AppConfig] Gagal membaca " + file + ", memakai default: " + e.getMessage());
            return config;
        }
        // Setiap kelompok divalidasi lewat setter; jika gagal, kelompok itu tetap default
        try {
            config.setPersentaseBudget(bacaDouble(p, KEY_PERSEN_MAKAN, DEFAULT_PERSEN_MAKAN),
                    bacaDouble(p, KEY_PERSEN_HIBURAN, DEFAULT_PERSEN_HIBURAN));
        } catch (IllegalArgumentException e) {
            System.err.println("[AppConfig] Persentase budget tidak valid, memakai default: " + e.getMessage());
        }
        try {
            config.setBatasTier(bacaLong(p, KEY_TIER_SEDANG, DEFAULT_TIER_SEDANG),
                    bacaLong(p, KEY_TIER_LUMAYAN, DEFAULT_TIER_LUMAYAN),
                    bacaLong(p, KEY_TIER_NYAMAN, DEFAULT_TIER_NYAMAN));
        } catch (IllegalArgumentException e) {
            System.err.println("[AppConfig] Batas tier tidak valid, memakai default: " + e.getMessage());
        }
        try {
            config.setAmbang(bacaDouble(p, KEY_AMBANG_SISA, DEFAULT_AMBANG_SISA),
                    bacaDouble(p, KEY_AMBANG_TETAP, DEFAULT_AMBANG_TETAP));
        } catch (IllegalArgumentException e) {
            System.err.println("[AppConfig] Ambang peringatan tidak valid, memakai default: " + e.getMessage());
        }
        String acuan = p.getProperty(KEY_KOTA_ACUAN, DEFAULT_KOTA_ACUAN).trim();
        config.kotaAcuan = acuan.isEmpty() ? DEFAULT_KOTA_ACUAN : acuan;
        return config;
    }

    public void simpan() throws IOException {
        Properties p = new Properties();
        p.setProperty(KEY_PERSEN_MAKAN, String.valueOf(persenMakan));
        p.setProperty(KEY_PERSEN_HIBURAN, String.valueOf(persenHiburan));
        p.setProperty(KEY_TIER_SEDANG, String.valueOf(tierMinSedang));
        p.setProperty(KEY_TIER_LUMAYAN, String.valueOf(tierMinLumayan));
        p.setProperty(KEY_TIER_NYAMAN, String.valueOf(tierMinNyaman));
        p.setProperty(KEY_AMBANG_SISA, String.valueOf(ambangSisa));
        p.setProperty(KEY_AMBANG_TETAP, String.valueOf(ambangPengeluaranTetap));
        p.setProperty(KEY_KOTA_ACUAN, kotaAcuan);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            p.store(writer, "Konfigurasi Sistem Keuangan Mahasiswa Rantau (persen ditulis 0.70 = 70%)");
        }
    }

    private static double bacaDouble(Properties p, String key, double bawaan) {
        try {
            return Double.parseDouble(p.getProperty(key, String.valueOf(bawaan)).trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            System.err.println("[AppConfig] Nilai " + key + " bukan angka, memakai default.");
            return bawaan;
        }
    }

    private static long bacaLong(Properties p, String key, long bawaan) {
        try {
            return Long.parseLong(p.getProperty(key, String.valueOf(bawaan)).trim());
        } catch (NumberFormatException e) {
            System.err.println("[AppConfig] Nilai " + key + " bukan angka bulat, memakai default.");
            return bawaan;
        }
    }

    /** Persen dalam bentuk desimal: 0.70 = 70%. Jumlah keduanya tidak boleh lebih dari 100%. */
    public void setPersentaseBudget(double persenMakan, double persenHiburan) {
        if (persenMakan < 0 || persenHiburan < 0 || persenMakan > 1 || persenHiburan > 1) {
            throw new IllegalArgumentException("Persentase harus antara 0% dan 100%.");
        }
        if (persenMakan + persenHiburan > 1.0 + 1e-9) {
            throw new IllegalArgumentException("Jumlah persentase makan dan hiburan tidak boleh lebih dari 100%.");
        }
        this.persenMakan = persenMakan;
        this.persenHiburan = persenHiburan;
    }

    public void setBatasTier(long minSedang, long minLumayan, long minNyaman) {
        if (minSedang <= 0 || minLumayan <= minSedang || minNyaman <= minLumayan) {
            throw new IllegalArgumentException("Batas tier harus naik: Sedang < Lumayan < Nyaman dan lebih dari Rp0.");
        }
        this.tierMinSedang = minSedang;
        this.tierMinLumayan = minLumayan;
        this.tierMinNyaman = minNyaman;
    }

    public void setAmbang(double ambangSisa, double ambangPengeluaranTetap) {
        if (ambangSisa <= 0 || ambangSisa > 1 || ambangPengeluaranTetap <= 0 || ambangPengeluaranTetap > 1) {
            throw new IllegalArgumentException("Ambang peringatan harus lebih dari 0% dan paling besar 100%.");
        }
        this.ambangSisa = ambangSisa;
        this.ambangPengeluaranTetap = ambangPengeluaranTetap;
    }

    public double getPersenMakan() {
        return persenMakan;
    }

    public double getPersenHiburan() {
        return persenHiburan;
    }

    public long getTierMinSedang() {
        return tierMinSedang;
    }

    public long getTierMinLumayan() {
        return tierMinLumayan;
    }

    public long getTierMinNyaman() {
        return tierMinNyaman;
    }

    public double getAmbangSisa() {
        return ambangSisa;
    }

    public double getAmbangPengeluaranTetap() {
        return ambangPengeluaranTetap;
    }

    public String getKotaAcuan() {
        return kotaAcuan;
    }
}
