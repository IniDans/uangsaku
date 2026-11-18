import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Peringatan;
import model.Pengeluaran;
import model.PengeluaranHarian;
import model.PengeluaranTetap;
import model.TempatHiburan;
import model.TierAnggaran;
import model.Transaksi;
import service.SistemKeuangan;

/** Pengujian aturan bisnis: rumus anggaran, saldo, tier, kota adaptif, hiburan, peringatan. */
public final class LogikaAnggaranTest {

    private static final LocalDate TGL_1_NOV = LocalDate.of(2026, 11, 1);

    private LogikaAnggaranTest() {
    }

    /**
     * Skenario dummy (Malang): kiriman Rp2.000.000 tgl 1 Nov - 1 Des (30 hari),
     * pengeluaran tetap Rp945.000, target tabungan Rp200.000
     * => dana tersedia Rp855.000, harian Rp28.500, makan Rp598.500, hiburan Rp256.500.
     */
    private static SistemKeuangan skenarioDasar(Path folder) throws Exception {
        SistemKeuangan s = Uji.sistemKosong(folder);
        s.tambahKiriman(2_000_000, TGL_1_NOV, null);
        s.tambahPengeluaranTetap("Kos", PengeluaranTetap.KATEGORI_KOS, 700_000);
        s.tambahPengeluaranTetap("Kuota internet", "Kuota internet", 75_000);
        s.tambahPengeluaranTetap("Bensin", "Bensin", 120_000);
        s.tambahPengeluaranTetap("Servis motor", "Servis motor", 50_000);
        s.setTargetTabungan(200_000);
        return s;
    }

    private static int hitungPeringatan(SistemKeuangan s, Peringatan.Jenis jenis) {
        int n = 0;
        for (Peringatan p : s.getDaftarPeringatan()) {
            if (p.getJenis() == jenis) {
                n++;
            }
        }
        return n;
    }

    static void jalankan() {
        Uji.kasus("Rumus anggaran sesuai spesifikasi (skenario Malang)", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                Uji.sama(945_000L, s.getTotalPengeluaranTetap(), "total pengeluaran tetap");
                Uji.sama(855_000L, s.getAnggaran().getDanaTersedia(), "dana tersedia = kiriman - tetap - tabungan");
                Uji.sama(30, s.getAnggaran().getJumlahHari(), "jumlah hari 1 Nov - 1 Des");
                Uji.sama(28_500L, s.getAnggaran().getBudgetHarian(), "budget harian = dana / hari");
                Uji.sama(598_500L, s.getAnggaran().getBudgetMakan(), "budget makan = 70%");
                Uji.sama(256_500L, s.getAnggaran().getBudgetHiburan(), "budget hiburan = 30%");
                Uji.sama(2_000_000L, s.getSaldo(), "pengeluaran tetap & tabungan tidak mengurangi saldo");
                Uji.sama(TierAnggaran.SEDANG, s.getTierSaatIni().getNamaTier(), "tier Rp855.000 di Malang");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Kos ditanggung orang tua vs tidak", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                s.setStatusKosDitanggung(true);
                Uji.sama(245_000L, s.getTotalPengeluaranTetap(), "kos tidak dihitung saat ditanggung");
                Uji.sama(1_555_000L, s.getAnggaran().getDanaTersedia(), "dana tersedia naik Rp700.000");
                Uji.sama(TierAnggaran.LUMAYAN, s.getTierSaatIni().getNamaTier(), "tier naik ke Lumayan");
                s.setStatusKosDitanggung(false);
                Uji.sama(945_000L, s.getTotalPengeluaranTetap(), "kos dihitung lagi");
                Uji.sama(855_000L, s.getAnggaran().getDanaTersedia(), "dana tersedia kembali");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Ubah target tabungan langsung menghitung ulang anggaran", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                s.setTargetTabungan(0);
                Uji.sama(1_055_000L, s.getAnggaran().getDanaTersedia(), "dana tersedia tanpa tabungan");
                Uji.sama(35_166L, s.getAnggaran().getBudgetHarian(), "budget harian dihitung ulang");
                s.setTargetTabungan(500_000);
                Uji.sama(555_000L, s.getAnggaran().getDanaTersedia(), "dana tersedia tabungan 500rb");
                Uji.sama(TierAnggaran.HEMAT, s.getTierSaatIni().getNamaTier(), "tier turun ke Hemat");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Target tabungan terlalu besar ditolak dan dicatat", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                Uji.sama(1_055_000L, s.getBatasMaksimalTabungan(), "batas = kiriman - tetap");
                Uji.ditolak(() -> s.setTargetTabungan(1_100_000), "terlalu besar", "target > batas");
                Uji.sama(200_000L, s.getTargetTabungan(), "target lama tetap dipakai");
                Uji.sama(1, hitungPeringatan(s, Peringatan.Jenis.TABUNGAN_TIDAK_TERPENUHI), "peringatan tabungan");
                s.setTargetTabungan(1_055_000);
                Uji.sama(0L, s.getAnggaran().getDanaTersedia(), "target tepat di batas boleh, dana 0");
                Uji.ditolak(() -> s.setTargetTabungan(-1), "negatif", "target negatif");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Tambah lalu hapus transaksi mengembalikan saldo dan sisa", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                Transaksi t = s.tambahTransaksi(LocalDate.of(2026, 11, 10), "Makan", "Nasi padang", 20_000);
                Uji.sama(1_980_000L, s.getSaldo(), "saldo berkurang");
                Uji.sama(835_000L, s.getSisaAnggaran(), "sisa anggaran berkurang");
                Uji.sama(578_500L, s.getSisaBudgetMakan(), "sisa budget makan berkurang");
                Uji.sama(256_500L, s.getSisaBudgetHiburan(), "budget hiburan tidak terpengaruh makan");
                s.hapusTransaksi(t.getIdTransaksi());
                Uji.sama(2_000_000L, s.getSaldo(), "saldo kembali");
                Uji.sama(855_000L, s.getSisaAnggaran(), "sisa kembali");
                Uji.ditolak(() -> s.hapusTransaksi("TRX-TIDAKADA"), "tidak ditemukan", "hapus id asing");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Validasi transaksi: tanggal masa depan, nominal, kategori, saldo", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                Uji.ditolak(() -> s.tambahTransaksi(Uji.HARI_INI.plusDays(1), "Makan", "Besok", 10_000),
                        "masa depan", "tanggal besok");
                Uji.ditolak(() -> s.tambahTransaksi(Uji.HARI_INI, "Makan", "Nol", 0), "lebih dari Rp0", "nominal 0");
                Uji.ditolak(() -> s.tambahTransaksi(Uji.HARI_INI, "Belanja", "x", 1_000), "Kategori", "kategori asing");
                Uji.ditolak(() -> s.tambahTransaksi(Uji.HARI_INI, "Makan", "Mahal", 2_000_001), "Saldo tidak cukup",
                        "melebihi saldo");
                Uji.sama(2_000_000L, s.getSaldo(), "saldo tidak berubah setelah penolakan");
                Uji.sama(0, s.getDaftarTransaksi().size(), "tidak ada transaksi tersimpan");
                Uji.ditolak(() -> s.tambahTransaksi(Uji.HARI_INI, "Makan", "a;b", 1_000), ";", "titik koma");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Kategori Jajan/Transportasi/Lainnya masuk pos makan & kebutuhan", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                s.tambahTransaksi(LocalDate.of(2026, 11, 3), "Jajan", "Kopi", 18_000);
                s.tambahTransaksi(LocalDate.of(2026, 11, 4), "Transportasi", "Parkir", 2_000);
                s.tambahTransaksi(LocalDate.of(2026, 11, 7), "Hiburan", "Bioskop", 45_000);
                Uji.sama(45_000L, s.getTerpakaiHiburan(), "terpakai hiburan");
                Uji.sama(20_000L, s.getTerpakaiMakan(), "terpakai makan & kebutuhan");
                Uji.sama(211_500L, s.getSisaBudgetHiburan(), "sisa hiburan");
                Uji.sama(18_000L, s.getTotalPerKategoriPeriode().get("Jajan"), "total kategori Jajan");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Transaksi di luar periode kiriman tidak mengurangi sisa anggaran", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                s.tambahTransaksi(LocalDate.of(2026, 10, 20), "Makan", "Bulan lalu", 30_000);
                Uji.sama(1_970_000L, s.getSaldo(), "saldo tetap berkurang");
                Uji.sama(855_000L, s.getSisaAnggaran(), "sisa periode ini tidak berubah");
                Uji.benar(!s.melebihiSisaAnggaran(900_000, LocalDate.of(2026, 10, 20)),
                        "konfirmasi hanya untuk periode aktif");
                Uji.benar(s.melebihiSisaAnggaran(900_000, Uji.HARI_INI), "900rb > sisa 855rb perlu konfirmasi");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Batas tier Malang (faktor 1,00)", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = Uji.sistemKosong(f);
                Uji.kira(1.0, s.getFaktorKota(), 1e-12, "faktor Malang");
                List<TierAnggaran> tier = s.getDaftarTierEfektif();
                Uji.sama(TierAnggaran.HEMAT, TierAnggaran.tentukanTier(0, tier).getNamaTier(), "Rp0");
                Uji.sama(TierAnggaran.HEMAT, TierAnggaran.tentukanTier(799_999, tier).getNamaTier(), "799.999");
                Uji.sama(TierAnggaran.SEDANG, TierAnggaran.tentukanTier(800_000, tier).getNamaTier(), "800.000");
                Uji.sama(TierAnggaran.SEDANG, TierAnggaran.tentukanTier(1_399_999, tier).getNamaTier(), "1.399.999");
                Uji.sama(TierAnggaran.LUMAYAN, TierAnggaran.tentukanTier(1_400_000, tier).getNamaTier(), "1.400.000");
                Uji.sama(TierAnggaran.LUMAYAN, TierAnggaran.tentukanTier(2_199_999, tier).getNamaTier(), "2.199.999");
                Uji.sama(TierAnggaran.NYAMAN, TierAnggaran.tentukanTier(2_200_000, tier).getNamaTier(), "2.200.000");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Kota adaptif: faktor UMK, override persen, perubahan tier", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                s.tambahKota("Surabaya", 5_288_796, null);
                s.ubahProfil("Mahasiswa Rantau", "Surabaya");
                Uji.kira(5_288_796.0 / 3_736_101.0, s.getFaktorKota(), 1e-9, "faktor Surabaya = UMK / UMK Malang");
                Uji.kira(1.416, s.getFaktorKota(), 0.001, "faktor Surabaya ≈ 1,416");
                Uji.sama(TierAnggaran.HEMAT, s.getTierSaatIni().getNamaTier(),
                        "dana Rp855.000 di Surabaya turun ke Hemat (batas Sedang ≈ Rp1,13 jt)");
                Uji.sama(855_000L, s.getAnggaran().getDanaTersedia(), "rumus dana tidak bergantung kota");

                s.ubahKota("Surabaya", 5_288_796, 10.0);
                Uji.kira(1.10, s.getFaktorKota(), 1e-12, "override kenaikan harga 10%");
                Uji.sama(880_000L, s.getDaftarTierEfektif().get(1).getBatasMinimum(), "batas Sedang 800rb x 1,10");
                Uji.sama(TierAnggaran.HEMAT, s.getTierSaatIni().getNamaTier(), "855rb < 880rb tetap Hemat");

                s.ubahProfil("Mahasiswa Rantau", "Malang (Kota)");
                Uji.sama(TierAnggaran.SEDANG, s.getTierSaatIni().getNamaTier(), "kembali ke Malang = Sedang");

                Uji.ditolak(() -> s.ubahKota("Malang (Kota)", 3_736_101, 5.0), "acuan", "acuan tidak boleh override");
                Uji.ditolak(() -> s.hapusKota("Malang (Kota)"), "acuan", "acuan tidak boleh dihapus");
                Uji.ditolak(() -> s.tambahKota("surabaya", 1, null), "sudah ada", "kota duplikat");
                Uji.ditolak(() -> s.ubahKota("Surabaya", 5_288_796, 250.0), null, "persen di luar -50..200");
                s.ubahProfil("Mahasiswa Rantau", "Surabaya");
                Uji.ditolak(() -> s.hapusKota("Surabaya"), "sedang dipakai", "kota yang dipakai");
                s.ubahProfil("Mahasiswa Rantau", "Malang (Kota)");
                s.hapusKota("Surabaya");
                Uji.sama(1, s.getDaftarKota().size(), "Surabaya terhapus");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Rekomendasi hiburan disaring kota dan sisa budget hiburan", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                s.tambahKota("Batu", 3_562_484, null);
                s.tambahTempatHiburan("Matos", "Lowokwaru", "Malang (Kota)", 20_000, 100_000, TierAnggaran.SEDANG);
                s.tambahTempatHiburan("Alun-Alun Tugu", "Klojen", "Malang (Kota)", 0, 20_000, TierAnggaran.HEMAT);
                s.tambahTempatHiburan("Konser", "Stadion", "Malang (Kota)", 300_000, 500_000, TierAnggaran.NYAMAN);
                s.tambahTempatHiburan("Jatim Park 2", "Batu", "Batu", 100_000, 200_000, TierAnggaran.LUMAYAN);

                List<String> nama = namaTempat(s.getRekomendasiHiburan());
                Uji.sama(List.of("Alun-Alun Tugu", "Matos"), nama, "hanya Malang, terjangkau, urut termurah");

                s.tambahTransaksi(LocalDate.of(2026, 11, 7), "Hiburan", "Nonton", 240_000);
                Uji.sama(16_500L, s.getSisaBudgetHiburan(), "sisa hiburan setelah transaksi");
                Uji.sama(List.of("Alun-Alun Tugu"), namaTempat(s.getRekomendasiHiburan()), "Matos tidak terjangkau");

                Uji.ditolak(() -> s.tambahTempatHiburan("matos", "x", "Malang (Kota)", 0, 1, TierAnggaran.HEMAT),
                        "sudah ada", "tempat duplikat di kota sama");
                Uji.ditolak(() -> s.tambahTempatHiburan("X", "x", "Malang (Kota)", 50, 10, TierAnggaran.HEMAT),
                        "maksimum", "biaya maks < min");
                Uji.ditolak(() -> s.hapusKota("Batu"), "tempat hiburan", "kota masih punya tempat");
                s.hapusTempatHiburan("Jatim Park 2", "Batu");
                s.hapusKota("Batu");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Periode 1 hari: budget harian = dana tersedia", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = Uji.sistemKosong(f);
                s.tambahKiriman(300_000, Uji.HARI_INI, Uji.HARI_INI.plusDays(1));
                Uji.sama(1, s.getAnggaran().getJumlahHari(), "jumlah hari 1");
                Uji.sama(300_000L, s.getAnggaran().getBudgetHarian(), "harian = dana");
                Uji.ditolak(() -> s.tambahKiriman(100_000, Uji.HARI_INI, Uji.HARI_INI), "", "berikutnya = terima");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Kiriman: tanggal duplikat ditolak, ubah nominal hanya menggeser saldo sebesar selisih", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                Uji.ditolak(() -> s.tambahKiriman(1_000_000, TGL_1_NOV, null), "sudah ada", "tanggal sama");
                s.tambahTransaksi(LocalDate.of(2026, 11, 2), "Makan", "Bakso", 15_000);
                s.ubahKirimanAktif(2_500_000, TGL_1_NOV, null);
                Uji.sama(2_485_000L, s.getSaldo(), "saldo + selisih 500rb");
                Uji.sama(1_355_000L, s.getAnggaran().getDanaTersedia(), "dana tersedia ikut naik");
                Uji.ditolak(() -> s.ubahKirimanAktif(1, TGL_1_NOV, null), "Saldo tidak cukup",
                        "turun lebih dari saldo");
                Uji.sama(2_500_000L, s.getMahasiswa().getNominalKiriman(), "kiriman tidak berubah setelah ditolak");

                s.tambahKiriman(1_800_000, LocalDate.of(2026, 11, 15), null);
                Uji.sama(LocalDate.of(2026, 11, 15), s.getMahasiswa().getTanggalKiriman(), "kiriman terbaru jadi aktif");
                Uji.sama(4_285_000L, s.getSaldo(), "saldo bertambah kiriman baru");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Pengeluaran tetap: ubah, hapus, nama unik", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                Uji.ditolak(() -> s.tambahPengeluaranTetap("kos", "Kos", 1), "sudah ada", "nama duplikat");
                PengeluaranTetap bensin = cariTetap(s, "Bensin");
                s.ubahPengeluaranTetap(bensin.getIdPengeluaran(), "Bensin", "Bensin", 150_000);
                Uji.sama(975_000L, s.getTotalPengeluaranTetap(), "total setelah ubah");
                Uji.ditolak(() -> s.ubahPengeluaranTetap(bensin.getIdPengeluaran(), "Kos", "Bensin", 1), "sudah ada",
                        "ubah ke nama yang dipakai");
                s.hapusPengeluaranTetap(bensin.getIdPengeluaran());
                Uji.sama(825_000L, s.getTotalPengeluaranTetap(), "total setelah hapus");
                Uji.sama(975_000L, s.getAnggaran().getDanaTersedia(), "dana tersedia dihitung ulang");
                s.tambahPengeluaranTetap("Laundry", "Laundry", 40_000);
                Uji.benar(s.getKategoriPengeluaranTetap().contains("Laundry"), "kategori kustom muncul");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Polymorphism: total campuran PengeluaranTetap + PengeluaranHarian", () -> {
            PengeluaranTetap kos = new PengeluaranTetap("P1", "Kos", 700_000, PengeluaranTetap.KATEGORI_KOS);
            PengeluaranHarian makan = new PengeluaranHarian("H1", "Bakso", 15_000, "Makan");
            List<Pengeluaran> campur = new ArrayList<>(List.of(kos, makan));
            Uji.sama(715_000L, SistemKeuangan.hitungTotalBiaya(campur), "total normal");
            kos.setDitanggungOrangTua(true);
            Uji.sama(15_000L, SistemKeuangan.hitungTotalBiaya(campur), "kos ditanggung = 0");
            Uji.sama("Tetap", campur.get(0).getJenis(), "jenis tetap");
            Uji.sama("Harian", campur.get(1).getJenis(), "jenis harian");
        });

        Uji.kasus("Peringatan: budget harian, sisa menipis, pengeluaran tetap besar, tanpa duplikat", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                Uji.sama(0, s.getDaftarPeringatan().size(), "belum ada peringatan");
                s.tambahTransaksi(LocalDate.of(2026, 11, 5), "Makan", "Makan", 28_500);
                Uji.sama(0, hitungPeringatan(s, Peringatan.Jenis.BUDGET_HARIAN), "tepat sama budget harian belum");
                s.tambahTransaksi(LocalDate.of(2026, 11, 5), "Jajan", "Es", 1);
                Uji.sama(1, hitungPeringatan(s, Peringatan.Jenis.BUDGET_HARIAN), "lewat Rp1 -> peringatan");
                s.tambahTransaksi(LocalDate.of(2026, 11, 5), "Jajan", "Es lagi", 5_000);
                Uji.sama(1, hitungPeringatan(s, Peringatan.Jenis.BUDGET_HARIAN), "hari yang sama tidak dobel");

                Uji.benar(!s.isAnggaranMenipis(), "belum menipis");
                s.tambahTransaksi(Uji.HARI_INI, "Lainnya", "Laptop servis", 660_000);
                Uji.benar(s.isAnggaranMenipis(), "sisa < 20% dana");
                Uji.sama(1, hitungPeringatan(s, Peringatan.Jenis.SISA_MENIPIS), "peringatan sisa menipis");
                Uji.sama(2, hitungPeringatan(s, Peringatan.Jenis.BUDGET_HARIAN), "budget harian tanggal lain");

                Uji.sama(0, hitungPeringatan(s, Peringatan.Jenis.PENGELUARAN_TETAP_BESAR), "tetap masih < 60%");
                s.tambahPengeluaranTetap("Cicilan laptop", "Lainnya", 300_000);
                Uji.sama(1, hitungPeringatan(s, Peringatan.Jenis.PENGELUARAN_TETAP_BESAR), "tetap > 60% kiriman");
            } finally {
                Uji.hapusFolder(f);
            }
        });

        Uji.kasus("Konfigurasi persentase dan batas tier divalidasi", () -> {
            Path f = Uji.folderSementara();
            try {
                SistemKeuangan s = skenarioDasar(f);
                Uji.ditolak(() -> s.ubahPersentaseBudget(0.8, 0.3), "100%", "jumlah > 100%");
                s.ubahPersentaseBudget(0.6, 0.4);
                Uji.sama(513_000L, s.getAnggaran().getBudgetMakan(), "makan 60%");
                Uji.sama(342_000L, s.getAnggaran().getBudgetHiburan(), "hiburan 40%");
                Uji.ditolak(() -> s.ubahBatasTier(1_000_000, 900_000, 2_000_000), "naik", "batas tidak naik");
                s.ubahBatasTier(900_000, 1_400_000, 2_200_000);
                Uji.sama(TierAnggaran.HEMAT, s.getTierSaatIni().getNamaTier(), "855rb < 900rb jadi Hemat");
                Uji.ditolak(() -> s.ubahAmbangPeringatan(0, 0.6), "Ambang", "ambang 0");
            } finally {
                Uji.hapusFolder(f);
            }
        });
    }

    private static List<String> namaTempat(List<TempatHiburan> daftar) {
        List<String> hasil = new ArrayList<>();
        for (TempatHiburan t : daftar) {
            hasil.add(t.getNamaTempat());
        }
        return hasil;
    }

    private static PengeluaranTetap cariTetap(SistemKeuangan s, String nama) {
        for (PengeluaranTetap p : s.getDaftarPengeluaranTetap()) {
            if (p.getNamaPengeluaran().equals(nama)) {
                return p;
            }
        }
        throw new IllegalStateException("tidak ada " + nama);
    }
}
