import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import model.Kota;
import model.Peringatan;
import model.TierAnggaran;
import model.Transaksi;
import repository.FileRepository;
import service.SistemKeuangan;
import util.AppConfig;

/** Pengujian penyimpanan file: seed, round-trip, baris rusak, kolom kosong. */
public final class RepositoryTest {

    /** Folder seed asli di repo (test dijalankan dari root proyek). */
    private static final Path SEED = Path.of("data", "seed");

    private RepositoryTest() {
    }

    private static void tulis(Path file, String... baris) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, List.of(baris), StandardCharsets.UTF_8);
    }

    static void jalankan() {
        Uji.kasus("Folder data kosong -> seed disalin dan data dummy termuat", () -> {
            Uji.benar(Files.isDirectory(SEED), "folder data/seed ada (jalankan dari root proyek)");
            Path f = Uji.folderSementara();
            try {
                Path data = f.resolve("data");
                SistemKeuangan s = Uji.sistemDari(data, SEED);
                Uji.benar(Files.exists(data.resolve(FileRepository.FILE_TRANSAKSI)), "transaksi.csv disalin");
                Uji.benar(Files.exists(data.resolve("config.properties")), "config.properties dibuat");
                Uji.sama("Mahasiswa Rantau", s.getMahasiswa().getNamaMahasiswa(), "nama dummy");
                Uji.sama("Malang (Kota)", s.getMahasiswa().getKota().getNamaKota(), "kota dummy");
                Uji.sama(6, s.getDaftarKota().size(), "6 kota");
                Uji.sama(12, s.getDaftarTransaksi().size(), "12 transaksi");
                Uji.sama(12, s.getDaftarTempatHiburan().size(), "12 tempat hiburan");
                Uji.sama(945_000L, s.getTotalPengeluaranTetap(), "total tetap");
                Uji.sama(855_000L, s.getAnggaran().getDanaTersedia(), "dana tersedia");
                Uji.sama(208_000L, s.getTotalTransaksiPeriode(), "total transaksi November");
                Uji.sama(647_000L, s.getSisaAnggaran(), "sisa anggaran");
                Uji.sama(1_792_000L, s.getSaldo(), "saldo dompet seed");
                Uji.sama(s.getTotalKiriman() - 208_000L, s.getSaldo(), "saldo konsisten = kiriman - transaksi");
                Uji.sama(TierAnggaran.SEDANG, s.getTierSaatIni().getNamaTier(), "tier Sedang");
                Uji.sama(176_500L, s.getSisaBudgetHiburan(), "sisa hiburan 256.500 - 80.000");
                Uji.sama(10, s.getRekomendasiHiburan().size(), "10 tempat Malang terjangkau, Batu tidak ikut");
                Uji.sama(0, s.getDaftarPeringatan().size(), "belum ada peringatan");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("File data yang sudah ada tidak ditimpa seed", () -> {
            Path f = Uji.folderSementara();
            try {
                Path data = f.resolve("data");
                tulis(data.resolve(FileRepository.FILE_TABUNGAN), "nominalTabungan", "50000");
                SistemKeuangan s = Uji.sistemDari(data, SEED);
                Uji.sama(50_000L, s.getTargetTabungan(), "tabungan milik pengguna dipakai");
                Uji.sama(12, s.getDaftarTransaksi().size(), "file lain tetap dari seed");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Round-trip: perubahan tersimpan dan terbaca ulang sama persis", () -> {
            Path f = Uji.folderSementara();
            try {
                Path data = f.resolve("data");
                SistemKeuangan s = Uji.sistemDari(data, SEED);
                Transaksi t = s.tambahTransaksi(LocalDate.of(2026, 11, 18), "Makan", "Soto & es jeruk", 40_000);
                s.tambahKota("Jakarta", 5_729_876, 7.5);
                s.ubahProfil("Mahasiswa Uji", "Surabaya");
                s.setStatusKosDitanggung(true);
                s.setTargetTabungan(300_000);
                s.tambahTempatHiburan("Taman Bungkul", "Wonokromo", "Surabaya", 0, 20_000, TierAnggaran.HEMAT);
                s.ubahPersentaseBudget(0.65, 0.35);

                SistemKeuangan baru = Uji.sistemDari(data, SEED);
                Uji.sama(s.getSaldo(), baru.getSaldo(), "saldo");
                Uji.sama(1_752_000L, baru.getSaldo(), "saldo setelah transaksi 40rb");
                Uji.sama(13, baru.getDaftarTransaksi().size(), "jumlah transaksi");
                Uji.sama(t.getIdTransaksi(), baru.getDaftarTransaksi().get(0).getIdTransaksi(), "transaksi terbaru di atas");
                Uji.sama("Soto & es jeruk", baru.getDaftarTransaksi().get(0).getKeterangan(), "keterangan");
                Uji.sama("Mahasiswa Uji", baru.getMahasiswa().getNamaMahasiswa(), "nama");
                Uji.sama("Surabaya", baru.getMahasiswa().getKota().getNamaKota(), "kota");
                Uji.benar(baru.getMahasiswa().isStatusKosDitanggung(), "status kos");
                Uji.sama(300_000L, baru.getTargetTabungan(), "target tabungan");
                Uji.sama(0.65, baru.getConfig().getPersenMakan(), "persen makan dari config");
                Uji.sama(s.getAnggaran().getDanaTersedia(), baru.getAnggaran().getDanaTersedia(), "dana tersedia");
                Uji.sama(s.getAnggaran().getBudgetMakan(), baru.getAnggaran().getBudgetMakan(), "budget makan");
                Uji.sama(s.getTierSaatIni().getNamaTier(), baru.getTierSaatIni().getNamaTier(), "tier");
                Uji.sama(1, baru.getRekomendasiHiburan().size(), "rekomendasi Surabaya");
                Kota jakarta = null;
                for (Kota k : baru.getDaftarKota()) {
                    if (k.getNamaKota().equals("Jakarta")) {
                        jakarta = k;
                    }
                }
                Uji.benar(jakarta != null, "kota baru tersimpan");
                Uji.sama(7.5, jakarta.getPersenKenaikanHarga(), "persen desimal tersimpan");
                Uji.sama(null, baru.getDaftarKota().get(0).getPersenKenaikanHarga(), "persen kosong tetap null");
                Uji.sama(s.getDaftarPeringatan().size(), baru.getDaftarPeringatan().size(), "riwayat peringatan");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Baris rusak dilewati, baris valid tetap terbaca", () -> {
            Path f = Uji.folderSementara();
            try {
                Path data = f.resolve("data");
                tulis(data.resolve(FileRepository.FILE_TRANSAKSI),
                        "idTransaksi;tanggal;kategori;keterangan;nominal",
                        "TRX-A;2026-11-02;Makan;Valid;10000",
                        "TRX-B;2026-11-03;Makan;Kolom kurang",
                        "TRX-C;2026-11-04;Makan;Nominal huruf;sepuluh",
                        "TRX-D;03/11/2026;Makan;Tanggal salah;5000",
                        "TRX-E;2026-11-05;Belanja;Kategori asing;5000",
                        "TRX-F;2026-11-06;Makan;Nominal negatif;-5000",
                        "",
                        "TRX-G;2026-11-07;jajan;Huruf kecil dinormalkan;7000");
                tulis(data.resolve(FileRepository.FILE_KOTA),
                        "namaKota;umk;persenKenaikanHarga",
                        "Malang (Kota);3736101;",
                        "Kota Rusak;abc;",
                        "Kota Persen Rusak;3000000;x",
                        "Surabaya;5288796;10");
                tulis(data.resolve(FileRepository.FILE_PERINGATAN),
                        "idPeringatan;jenis;tanggal;pesan",
                        "PRG-1;JENIS_ASING;2026-11-01;x",
                        "PRG-2;SISA_MENIPIS;2026-11-02;Sisa menipis");
                SistemKeuangan s = Uji.sistemDari(data, SEED);
                List<Transaksi> trx = s.getDaftarTransaksi();
                Uji.sama(2, trx.size(), "hanya 2 baris valid");
                Uji.sama("Jajan", trx.get(0).getKategori(), "kategori dinormalkan");
                Uji.sama(2, s.getDaftarKota().size(), "kota rusak dilewati");
                Uji.sama(1.10, s.hitungFaktor(s.getDaftarKota().get(1)), "persen 10 terbaca");
                Uji.sama(1, s.getDaftarPeringatan().size(), "peringatan jenis asing dilewati");
                Uji.sama(Peringatan.Jenis.SISA_MENIPIS, s.getDaftarPeringatan().get(0).getJenis(), "jenis peringatan");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Tanpa seed dan tanpa data -> aplikasi tetap jalan dengan nilai awal", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = Uji.sistemKosong(f);
                Uji.sama(0L, s.getSaldo(), "saldo 0");
                Uji.sama(0L, s.getAnggaran().getDanaTersedia(), "dana 0 tanpa kiriman");
                Uji.sama(1, s.getAnggaran().getJumlahHari(), "jumlah hari minimal 1");
                Uji.sama(AppConfig.DEFAULT_KOTA_ACUAN, s.getMahasiswa().getKota().getNamaKota(), "kota acuan default");
                Uji.benar(!s.isAnggaranMenipis(), "tidak menipis tanpa kiriman");
                Uji.ditolak(() -> s.setTargetTabungan(1), "kiriman", "tabungan tanpa kiriman");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Mahasiswa dengan kota yang tidak dikenal -> profil default", () -> {
            Path f = Uji.folderSementara();
            try {
                Path data = f.resolve("data");
                tulis(data.resolve(FileRepository.FILE_MAHASISWA),
                        "idMahasiswa;namaMahasiswa;statusKosDitanggung;namaKota",
                        "MHS-9;Nama;mungkin;Malang (Kota)",
                        "MHS-1;Andi;true;Kota Antah");
                SistemKeuangan s = Uji.sistemDari(data, SEED);
                Uji.sama("Malang (Kota)", s.getMahasiswa().getKota().getNamaKota(), "fallback kota acuan");
                Uji.benar(!s.getMahasiswa().isStatusKosDitanggung(), "fallback status kos");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("config.properties rusak -> nilai default", () -> {
            Path f = Uji.folderSementara();
            try {
                Path file = f.resolve("config.properties");
                tulis(file,
                        "budget.persenMakan=0,9",
                        "budget.persenHiburan=0.5",
                        "tier.sedang.min=dua juta",
                        "peringatan.ambangSisa=0.25",
                        "peringatan.ambangPengeluaranTetap=0.5");
                AppConfig c = AppConfig.muat(file);
                Uji.sama(AppConfig.DEFAULT_PERSEN_MAKAN, c.getPersenMakan(), "jumlah > 100% -> default");
                Uji.sama(AppConfig.DEFAULT_TIER_SEDANG, c.getTierMinSedang(), "angka rusak -> default");
                Uji.sama(0.25, c.getAmbangSisa(), "nilai valid tetap dipakai");
                Uji.sama(AppConfig.DEFAULT_KOTA_ACUAN, c.getKotaAcuan(), "kota acuan default");
            } finally {
                Uji.hapusFolder(f);
            }
        });
    }
}
