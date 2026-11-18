package repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Dompet;
import model.Kiriman;
import model.Kota;
import model.Mahasiswa;
import model.PengeluaranTetap;
import model.Peringatan;
import model.TargetTabungan;
import model.TempatHiburan;
import model.Transaksi;
import util.DateHelper;

/**
 * Menyimpan dan membaca data dalam file CSV (delimiter ';', baris pertama header).
 * - File yang belum ada disalin dari folder seed (data dummy).
 * - Baris rusak dilewati dan dicatat ke stderr, aplikasi tetap berjalan.
 */
public class FileRepository {

    public static final String DELIMITER = ";";

    public static final String FILE_MAHASISWA = "mahasiswa.csv";
    public static final String FILE_DOMPET = "dompet.csv";
    public static final String FILE_KIRIMAN = "kiriman.csv";
    public static final String FILE_PENGELUARAN_TETAP = "pengeluaran_tetap.csv";
    public static final String FILE_TABUNGAN = "tabungan.csv";
    public static final String FILE_TRANSAKSI = "transaksi.csv";
    public static final String FILE_TEMPAT_HIBURAN = "tempat_hiburan.csv";
    public static final String FILE_PERINGATAN = "peringatan.csv";

    private static final int MAKS_PERCOBAAN_TULIS = 5;
    public static final String FILE_KOTA = "kota.csv";

    private static final List<String> SEMUA_FILE = List.of(FILE_MAHASISWA, FILE_DOMPET, FILE_KIRIMAN,
            FILE_PENGELUARAN_TETAP, FILE_TABUNGAN, FILE_TRANSAKSI, FILE_TEMPAT_HIBURAN, FILE_PERINGATAN, FILE_KOTA);

    private final Path folderData;
    private final Path folderSeed;

    public FileRepository(Path folderData, Path folderSeed) {
        this.folderData = folderData;
        this.folderSeed = folderSeed;
    }

    /** Membuat folder data dan menyalin file seed untuk file yang belum ada. */
    public void siapkanFolder() throws IOException {
        Files.createDirectories(folderData);
        for (String nama : SEMUA_FILE) {
            Path target = folderData.resolve(nama);
            Path seed = folderSeed.resolve(nama);
            if (!Files.exists(target) && Files.exists(seed)) {
                Files.copy(seed, target);
            }
        }
    }

    public Path getFolderData() {
        return folderData;
    }

    // ===================== MEMBACA =====================

    public List<Kota> muatKota() {
        List<Kota> hasil = new ArrayList<>();
        for (Baris b : bacaBaris(FILE_KOTA, 3)) {
            try {
                String persen = b.kolom[2].trim();
                Double nilaiPersen = persen.isEmpty() ? null : Double.valueOf(persen.replace(',', '.'));
                hasil.add(new Kota(b.kolom[0], Long.parseLong(b.kolom[1].trim()), nilaiPersen));
            } catch (RuntimeException e) {
                lewati(FILE_KOTA, b, e);
            }
        }
        return hasil;
    }

    /** @return mahasiswa pertama yang valid, atau null jika tidak ada. */
    public Mahasiswa muatMahasiswa(List<Kota> daftarKota) {
        for (Baris b : bacaBaris(FILE_MAHASISWA, 4)) {
            try {
                Kota kota = cariKota(daftarKota, b.kolom[3].trim());
                if (kota == null) {
                    throw new IllegalArgumentException("kota '" + b.kolom[3] + "' tidak ada di kota.csv");
                }
                return new Mahasiswa(b.kolom[0], b.kolom[1], parseBoolean(b.kolom[2]), kota);
            } catch (RuntimeException e) {
                lewati(FILE_MAHASISWA, b, e);
            }
        }
        return null;
    }

    public Dompet muatDompet() {
        for (Baris b : bacaBaris(FILE_DOMPET, 2)) {
            try {
                return new Dompet(b.kolom[0], Long.parseLong(b.kolom[1].trim()));
            } catch (RuntimeException e) {
                lewati(FILE_DOMPET, b, e);
            }
        }
        return null;
    }

    public TargetTabungan muatTabungan() {
        for (Baris b : bacaBaris(FILE_TABUNGAN, 1)) {
            try {
                return new TargetTabungan(Long.parseLong(b.kolom[0].trim()));
            } catch (RuntimeException e) {
                lewati(FILE_TABUNGAN, b, e);
            }
        }
        return null;
    }

    public List<Kiriman> muatKiriman() {
        List<Kiriman> hasil = new ArrayList<>();
        for (Baris b : bacaBaris(FILE_KIRIMAN, 4)) {
            try {
                hasil.add(new Kiriman(b.kolom[0], Long.parseLong(b.kolom[1].trim()),
                        DateHelper.dariIso(b.kolom[2]), DateHelper.dariIso(b.kolom[3])));
            } catch (RuntimeException e) {
                lewati(FILE_KIRIMAN, b, e);
            }
        }
        return hasil;
    }

    public List<PengeluaranTetap> muatPengeluaranTetap() {
        List<PengeluaranTetap> hasil = new ArrayList<>();
        for (Baris b : bacaBaris(FILE_PENGELUARAN_TETAP, 4)) {
            try {
                hasil.add(new PengeluaranTetap(b.kolom[0], b.kolom[1], Long.parseLong(b.kolom[3].trim()), b.kolom[2]));
            } catch (RuntimeException e) {
                lewati(FILE_PENGELUARAN_TETAP, b, e);
            }
        }
        return hasil;
    }

    public List<Transaksi> muatTransaksi() {
        List<Transaksi> hasil = new ArrayList<>();
        for (Baris b : bacaBaris(FILE_TRANSAKSI, 5)) {
            try {
                hasil.add(new Transaksi(b.kolom[0], DateHelper.dariIso(b.kolom[1]), b.kolom[2], b.kolom[3],
                        Long.parseLong(b.kolom[4].trim())));
            } catch (RuntimeException e) {
                lewati(FILE_TRANSAKSI, b, e);
            }
        }
        return hasil;
    }

    public List<TempatHiburan> muatTempatHiburan() {
        List<TempatHiburan> hasil = new ArrayList<>();
        for (Baris b : bacaBaris(FILE_TEMPAT_HIBURAN, 6)) {
            try {
                hasil.add(new TempatHiburan(b.kolom[0], b.kolom[1], b.kolom[2], Long.parseLong(b.kolom[3].trim()),
                        Long.parseLong(b.kolom[4].trim()), b.kolom[5].trim()));
            } catch (RuntimeException e) {
                lewati(FILE_TEMPAT_HIBURAN, b, e);
            }
        }
        return hasil;
    }

    public List<Peringatan> muatPeringatan() {
        List<Peringatan> hasil = new ArrayList<>();
        for (Baris b : bacaBaris(FILE_PERINGATAN, 4)) {
            try {
                hasil.add(new Peringatan(b.kolom[0], Peringatan.Jenis.valueOf(b.kolom[1].trim()),
                        b.kolom[3], DateHelper.dariIso(b.kolom[2])));
            } catch (RuntimeException e) {
                lewati(FILE_PERINGATAN, b, e);
            }
        }
        return hasil;
    }

    // ===================== MENYIMPAN =====================

    public void simpanKota(List<Kota> daftar) throws IOException {
        List<String> baris = new ArrayList<>();
        for (Kota k : daftar) {
            baris.add(gabung(k.getNamaKota(), k.getUmk(), formatPersen(k.getPersenKenaikanHarga())));
        }
        tulisFile(FILE_KOTA, "namaKota;umk;persenKenaikanHarga", baris);
    }

    public void simpanMahasiswa(Mahasiswa m) throws IOException {
        tulisFile(FILE_MAHASISWA, "idMahasiswa;namaMahasiswa;statusKosDitanggung;namaKota",
                List.of(gabung(m.getIdMahasiswa(), m.getNamaMahasiswa(), m.isStatusKosDitanggung(),
                        m.getKota().getNamaKota())));
    }

    public void simpanDompet(Dompet d) throws IOException {
        tulisFile(FILE_DOMPET, "idDompet;saldo", List.of(gabung(d.getIdDompet(), d.getSaldo())));
    }

    public void simpanTabungan(TargetTabungan t) throws IOException {
        tulisFile(FILE_TABUNGAN, "nominalTabungan", List.of(String.valueOf(t.getTargetTabungan())));
    }

    public void simpanKiriman(List<Kiriman> daftar) throws IOException {
        List<String> baris = new ArrayList<>();
        for (Kiriman k : daftar) {
            baris.add(gabung(k.getIdKiriman(), k.getNominal(), DateHelper.keIso(k.getTanggalTerima()),
                    DateHelper.keIso(k.getTanggalBerikutnya())));
        }
        tulisFile(FILE_KIRIMAN, "idKiriman;nominal;tanggalTerima;tanggalBerikutnya", baris);
    }

    public void simpanPengeluaranTetap(List<PengeluaranTetap> daftar) throws IOException {
        List<String> baris = new ArrayList<>();
        for (PengeluaranTetap p : daftar) {
            baris.add(gabung(p.getIdPengeluaran(), p.getNamaPengeluaran(), p.getKategori(), p.getNominal()));
        }
        tulisFile(FILE_PENGELUARAN_TETAP, "idPengeluaran;namaPengeluaran;kategori;nominal", baris);
    }

    public void simpanTransaksi(List<Transaksi> daftar) throws IOException {
        List<String> baris = new ArrayList<>();
        for (Transaksi t : daftar) {
            baris.add(gabung(t.getIdTransaksi(), DateHelper.keIso(t.getTanggal()), t.getKategori(),
                    t.getKeterangan(), t.getNominal()));
        }
        tulisFile(FILE_TRANSAKSI, "idTransaksi;tanggal;kategori;keterangan;nominal", baris);
    }

    public void simpanTempatHiburan(List<TempatHiburan> daftar) throws IOException {
        List<String> baris = new ArrayList<>();
        for (TempatHiburan t : daftar) {
            baris.add(gabung(t.getNamaTempat(), t.getLokasi(), t.getKota(), t.getBiayaMinimum(),
                    t.getBiayaMaksimum(), t.getTier()));
        }
        tulisFile(FILE_TEMPAT_HIBURAN, "namaTempat;lokasi;kota;biayaMinimum;biayaMaksimum;tier", baris);
    }

    public void simpanPeringatan(List<Peringatan> daftar) throws IOException {
        List<String> baris = new ArrayList<>();
        for (Peringatan p : daftar) {
            baris.add(gabung(p.getIdPeringatan(), p.getJenis().name(), DateHelper.keIso(p.getTanggal()), p.getPesan()));
        }
        tulisFile(FILE_PERINGATAN, "idPeringatan;jenis;tanggal;pesan", baris);
    }

    // ===================== HELPER =====================

    /** Satu baris data beserta nomor barisnya (untuk pesan error). */
    private static class Baris {
        final int nomor;
        final String[] kolom;

        Baris(int nomor, String[] kolom) {
            this.nomor = nomor;
            this.kolom = kolom;
        }
    }

    /** Membaca semua baris data (tanpa header). Baris dengan jumlah kolom salah dilewati. */
    private List<Baris> bacaBaris(String namaFile, int jumlahKolom) {
        Path file = folderData.resolve(namaFile);
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        List<String> semua;
        try {
            semua = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[FileRepository] Gagal membaca " + namaFile + ": " + e.getMessage());
            return Collections.emptyList();
        }
        List<Baris> hasil = new ArrayList<>();
        for (int i = 1; i < semua.size(); i++) { // i = 0 adalah header
            String teks = semua.get(i);
            if (teks.isBlank()) {
                continue;
            }
            String[] kolom = teks.split(DELIMITER, -1);
            if (kolom.length != jumlahKolom) {
                System.err.println("[FileRepository] " + namaFile + " baris " + (i + 1)
                        + " dilewati: butuh " + jumlahKolom + " kolom, ditemukan " + kolom.length + ".");
                continue;
            }
            hasil.add(new Baris(i + 1, kolom));
        }
        return hasil;
    }

    private void lewati(String namaFile, Baris b, RuntimeException e) {
        System.err.println("[FileRepository] " + namaFile + " baris " + b.nomor + " dilewati: " + e.getMessage());
    }

    /** Menulis ke file sementara lalu memindahkannya, agar file lama tidak rusak jika penulisan gagal. */
    private void tulisFile(String namaFile, String header, List<String> baris) throws IOException {
        Files.createDirectories(folderData);
        List<String> isi = new ArrayList<>();
        isi.add(header);
        isi.addAll(baris);
        Path target = folderData.resolve(namaFile);
        Path sementara = folderData.resolve(namaFile + ".tmp");
        Files.write(sementara, isi, StandardCharsets.UTF_8);
        // Di Windows file bisa terkunci sesaat (dibuka Excel, antivirus, indexer): coba ulang beberapa kali.
        for (int percobaan = 1; ; percobaan++) {
            try {
                pindahkan(sementara, target);
                return;
            } catch (FileSystemException e) {
                if (percobaan >= MAKS_PERCOBAAN_TULIS) {
                    Files.deleteIfExists(sementara);
                    throw e;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Files.deleteIfExists(sementara);
                    throw e;
                }
            }
        }
    }

    private static void pindahkan(Path sumber, Path target) throws IOException {
        try {
            Files.move(sumber, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(sumber, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String gabung(Object... nilai) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nilai.length; i++) {
            if (i > 0) {
                sb.append(DELIMITER);
            }
            sb.append(nilai[i]);
        }
        return sb.toString();
    }

    private static String formatPersen(Double persen) {
        if (persen == null) {
            return "";
        }
        if (persen == Math.rint(persen)) {
            return String.valueOf(persen.longValue());
        }
        return String.valueOf(persen);
    }

    private static boolean parseBoolean(String teks) {
        String t = teks.trim().toLowerCase();
        if (t.equals("true") || t.equals("ya")) {
            return true;
        }
        if (t.equals("false") || t.equals("tidak")) {
            return false;
        }
        throw new IllegalArgumentException("nilai boolean tidak dikenal: " + teks);
    }

    private static Kota cariKota(List<Kota> daftar, String nama) {
        for (Kota k : daftar) {
            if (k.getNamaKota().equalsIgnoreCase(nama)) {
                return k;
            }
        }
        return null;
    }
}
