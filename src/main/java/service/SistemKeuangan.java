package service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import model.Anggaran;
import model.Dompet;
import model.Kiriman;
import model.Kota;
import model.Mahasiswa;
import model.Pengeluaran;
import model.PengeluaranHarian;
import model.PengeluaranTetap;
import model.Peringatan;
import model.TargetTabungan;
import model.TempatHiburan;
import model.TierAnggaran;
import model.Transaksi;
import repository.FileRepository;
import util.AppConfig;
import util.DateHelper;
import util.RupiahFormatter;
import util.Validasi;

/**
 * Pusat proses keuangan. GUI hanya memanggil method di kelas ini.
 *
 * Alur setiap perubahan data:
 *   validasi -> ubah data -> hitungUlangAnggaran() -> evaluasiPeringatan() -> simpanSemua()
 *
 * Aturan saldo (mencegah pengurangan ganda):
 *   - kiriman menambah saldo, transaksi mengurangi saldo, hapus transaksi mengembalikan saldo
 *   - pengeluaran tetap dan target tabungan TIDAK mengubah saldo, hanya mengurangi dana tersedia
 *   - sisa anggaran = dana tersedia - total transaksi periode aktif (selalu dihitung ulang)
 */
public class SistemKeuangan {

    /** UMK 2026 Kota Malang, dipakai jika kota acuan belum ada di kota.csv. */
    public static final long UMK_ACUAN_DEFAULT = 3_736_101;

    private final FileRepository repository;
    private final AppConfig config;
    private final Clock clock;

    private Mahasiswa mahasiswa;
    private Dompet dompet;
    private TargetTabungan targetTabungan;
    private final Anggaran anggaran = new Anggaran();

    private final List<Kiriman> daftarKiriman = new ArrayList<>();
    private final List<PengeluaranTetap> daftarPengeluaranTetap = new ArrayList<>();
    private final List<Transaksi> daftarTransaksi = new ArrayList<>();
    private final List<TempatHiburan> daftarTempatHiburan = new ArrayList<>();
    private final List<Peringatan> daftarPeringatan = new ArrayList<>();
    private final List<Kota> daftarKota = new ArrayList<>();

    public SistemKeuangan(FileRepository repository, AppConfig config) {
        this(repository, config, Clock.systemDefaultZone());
    }

    /** Clock bisa diganti saat pengujian agar "hari ini" tetap. */
    public SistemKeuangan(FileRepository repository, AppConfig config, Clock clock) {
        this.repository = repository;
        this.config = config;
        this.clock = clock;
    }

    // =====================================================================
    // MEMUAT DAN MENYIMPAN
    // =====================================================================

    /** Memuat seluruh data dari file. File yang hilang diganti seed atau nilai default. */
    public void muatData() {
        try {
            repository.siapkanFolder();
        } catch (IOException e) {
            System.err.println("[SistemKeuangan] Gagal menyiapkan folder data: " + e.getMessage());
        }
        daftarKota.clear();
        daftarKota.addAll(repository.muatKota());
        if (cariKota(config.getKotaAcuan()) == null) {
            daftarKota.add(0, new Kota(config.getKotaAcuan(), UMK_ACUAN_DEFAULT, null));
        }
        mahasiswa = repository.muatMahasiswa(daftarKota);
        if (mahasiswa == null) {
            mahasiswa = new Mahasiswa("MHS-001", "Mahasiswa", false, getKotaAcuan());
        }
        dompet = repository.muatDompet();
        if (dompet == null) {
            dompet = new Dompet("DMP-001", 0);
        }
        targetTabungan = repository.muatTabungan();
        if (targetTabungan == null) {
            targetTabungan = new TargetTabungan(0);
        }
        daftarKiriman.clear();
        daftarKiriman.addAll(repository.muatKiriman());
        perbaruiKirimanAktif();
        daftarPengeluaranTetap.clear();
        daftarPengeluaranTetap.addAll(repository.muatPengeluaranTetap());
        daftarTransaksi.clear();
        daftarTransaksi.addAll(repository.muatTransaksi());
        daftarTempatHiburan.clear();
        daftarTempatHiburan.addAll(repository.muatTempatHiburan());
        daftarPeringatan.clear();
        daftarPeringatan.addAll(repository.muatPeringatan());
        hitungUlangAnggaran();
    }

    /** Menyimpan semua data. Data di memori tetap aman walau penulisan file gagal. */
    private void simpanSemua() {
        try {
            repository.simpanKota(daftarKota);
            repository.simpanMahasiswa(mahasiswa);
            repository.simpanDompet(dompet);
            repository.simpanTabungan(targetTabungan);
            repository.simpanKiriman(daftarKiriman);
            repository.simpanPengeluaranTetap(daftarPengeluaranTetap);
            repository.simpanTransaksi(daftarTransaksi);
            repository.simpanTempatHiburan(daftarTempatHiburan);
            repository.simpanPeringatan(daftarPeringatan);
            config.simpan();
        } catch (IOException e) {
            throw new IllegalStateException("Perubahan tercatat, tetapi gagal disimpan ke file: " + e.getMessage(), e);
        }
    }

    /** Dipanggil setelah setiap perubahan data. */
    private void setelahPerubahan(LocalDate tanggalTransaksi) {
        hitungUlangAnggaran();
        evaluasiPeringatan(tanggalTransaksi);
        simpanSemua();
    }

    // =====================================================================
    // PERHITUNGAN ANGGARAN
    // =====================================================================

    /** Menghitung ulang anggaran. Dipanggil setiap kali kiriman, pengeluaran, tabungan, atau konfigurasi berubah. */
    private void hitungUlangAnggaran() {
        // status kos milik mahasiswa diteruskan ke setiap pengeluaran tetap
        for (PengeluaranTetap p : daftarPengeluaranTetap) {
            p.setDitanggungOrangTua(mahasiswa.isStatusKosDitanggung());
        }
        Kiriman aktif = mahasiswa.getKirimanAktif();
        int jumlahHari = aktif == null ? 1 : aktif.hitungJumlahHari();
        anggaran.hitungUlangAnggaran(mahasiswa.getNominalKiriman(), getTotalPengeluaranTetap(),
                targetTabungan.getTargetTabungan(), jumlahHari, config.getPersenMakan(), config.getPersenHiburan());
    }

    /**
     * POLYMORPHISM: daftar bisa berisi PengeluaranTetap maupun PengeluaranHarian.
     * Setiap objek menghitung biayanya sendiri lewat hitungBiaya().
     */
    public static long hitungTotalBiaya(List<? extends Pengeluaran> daftar) {
        long total = 0;
        for (Pengeluaran p : daftar) {
            total += p.hitungBiaya();
        }
        return total;
    }

    public long getTotalPengeluaranTetap() {
        return hitungTotalBiaya(daftarPengeluaranTetap);
    }

    /** Pengeluaran harian (dari transaksi) yang masuk periode kiriman aktif. */
    private List<PengeluaranHarian> getPengeluaranHarianPeriode() {
        List<PengeluaranHarian> hasil = new ArrayList<>();
        for (Transaksi t : getTransaksiPeriodeAktif()) {
            hasil.add(t.getPengeluaran());
        }
        return hasil;
    }

    public long getTotalTransaksiPeriode() {
        return hitungTotalBiaya(getPengeluaranHarianPeriode());
    }

    /** Gabungan pengeluaran tetap dan harian periode ini dalam satu List<Pengeluaran>. */
    public List<Pengeluaran> getSemuaPengeluaranPeriode() {
        List<Pengeluaran> semua = new ArrayList<>(daftarPengeluaranTetap);
        semua.addAll(getPengeluaranHarianPeriode());
        return semua;
    }

    public long getTotalPengeluaranPeriode() {
        return hitungTotalBiaya(getSemuaPengeluaranPeriode());
    }

    public long getSisaAnggaran() {
        return anggaran.getDanaTersedia() - getTotalTransaksiPeriode();
    }

    public long getTerpakaiHiburan() {
        long total = 0;
        for (PengeluaranHarian p : getPengeluaranHarianPeriode()) {
            if (p.isHiburan()) {
                total += p.hitungBiaya();
            }
        }
        return total;
    }

    public long getTerpakaiMakan() {
        return getTotalTransaksiPeriode() - getTerpakaiHiburan();
    }

    public long getSisaBudgetHiburan() {
        return Math.max(0, anggaran.getBudgetHiburan() - getTerpakaiHiburan());
    }

    public long getSisaBudgetMakan() {
        return Math.max(0, anggaran.getBudgetMakan() - getTerpakaiMakan());
    }

    /** Batas atas target tabungan = kiriman - total pengeluaran tetap. */
    public long getBatasMaksimalTabungan() {
        return Math.max(0, mahasiswa.getNominalKiriman() - getTotalPengeluaranTetap());
    }

    public boolean isAnggaranMenipis() {
        if (mahasiswa.getKirimanAktif() == null) {
            return false;
        }
        long dana = anggaran.getDanaTersedia();
        if (dana == 0) {
            return true;
        }
        return getSisaAnggaran() < Math.round(dana * config.getAmbangSisa());
    }

    /** Total transaksi per kategori pada periode aktif, urutan kategori tetap. */
    public Map<String, Long> getTotalPerKategoriPeriode() {
        Map<String, Long> hasil = new LinkedHashMap<>();
        for (String kategori : PengeluaranHarian.DAFTAR_KATEGORI) {
            hasil.put(kategori, 0L);
        }
        for (Transaksi t : getTransaksiPeriodeAktif()) {
            hasil.merge(t.getKategori(), t.getNominal(), Long::sum);
        }
        return hasil;
    }

    public long getTotalTransaksiPadaTanggal(LocalDate tanggal) {
        long total = 0;
        for (Transaksi t : daftarTransaksi) {
            if (t.getTanggal().equals(tanggal)) {
                total += t.getNominal();
            }
        }
        return total;
    }

    // =====================================================================
    // KOTA DAN TIER
    // =====================================================================

    public Kota getKotaAcuan() {
        return cariKota(config.getKotaAcuan());
    }

    public boolean isKotaAcuan(Kota kota) {
        return kota.getNamaKota().equalsIgnoreCase(config.getKotaAcuan());
    }

    /** Faktor kota mahasiswa terhadap kota acuan (acuan selalu 1,00). */
    public double getFaktorKota() {
        return hitungFaktor(mahasiswa.getKota());
    }

    public double hitungFaktor(Kota kota) {
        if (isKotaAcuan(kota)) {
            return 1.0;
        }
        return kota.hitungFaktor(getKotaAcuan().getUmk());
    }

    /** Batas tier kota acuan dikali faktor kota mahasiswa. */
    public List<TierAnggaran> getDaftarTierEfektif() {
        return TierAnggaran.buatDaftarTier(config.getTierMinSedang(), config.getTierMinLumayan(),
                config.getTierMinNyaman(), getFaktorKota());
    }

    public TierAnggaran getTierSaatIni() {
        return TierAnggaran.tentukanTier(anggaran.getDanaTersedia(), getDaftarTierEfektif());
    }

    public void tambahKota(String namaKota, long umk, Double persenKenaikanHarga) {
        if (cariKota(namaKota == null ? "" : namaKota.trim()) != null) {
            throw new IllegalArgumentException("Kota " + namaKota.trim() + " sudah ada.");
        }
        daftarKota.add(new Kota(namaKota, umk, persenKenaikanHarga));
        setelahPerubahan(null);
    }

    public void ubahKota(String namaKota, long umk, Double persenKenaikanHarga) {
        Kota kota = cariKotaWajib(namaKota);
        if (isKotaAcuan(kota) && persenKenaikanHarga != null) {
            throw new IllegalArgumentException("Kota acuan selalu berfaktor 1,00. Kosongkan persen kenaikan harga.");
        }
        // validasi dulu lewat objek sementara agar data tidak berubah sebagian
        new Kota(kota.getNamaKota(), umk, persenKenaikanHarga);
        kota.setUmk(umk);
        kota.setPersenKenaikanHarga(persenKenaikanHarga);
        setelahPerubahan(null);
    }

    public void hapusKota(String namaKota) {
        Kota kota = cariKotaWajib(namaKota);
        if (isKotaAcuan(kota)) {
            throw new IllegalArgumentException("Kota acuan tidak bisa dihapus.");
        }
        if (mahasiswa.getKota() == kota) {
            throw new IllegalArgumentException("Kota ini sedang dipakai sebagai kota rantau. Ganti kota rantau dulu.");
        }
        for (TempatHiburan t : daftarTempatHiburan) {
            if (t.getKota().equalsIgnoreCase(kota.getNamaKota())) {
                throw new IllegalArgumentException("Masih ada tempat hiburan di kota ini. Hapus tempatnya dulu.");
            }
        }
        daftarKota.remove(kota);
        setelahPerubahan(null);
    }

    private Kota cariKota(String namaKota) {
        for (Kota k : daftarKota) {
            if (k.getNamaKota().equalsIgnoreCase(namaKota)) {
                return k;
            }
        }
        return null;
    }

    private Kota cariKotaWajib(String namaKota) {
        Kota kota = cariKota(namaKota);
        if (kota == null) {
            throw new IllegalArgumentException("Kota " + namaKota + " tidak ditemukan.");
        }
        return kota;
    }

    // =====================================================================
    // PROFIL MAHASISWA
    // =====================================================================

    public void ubahProfil(String namaMahasiswa, String namaKota) {
        Kota kota = cariKotaWajib(namaKota);
        String nama = Validasi.teksWajib(namaMahasiswa, "Nama mahasiswa");
        mahasiswa.setNamaMahasiswa(nama);
        mahasiswa.setKota(kota);
        setelahPerubahan(null);
    }

    public void setStatusKosDitanggung(boolean ditanggung) {
        mahasiswa.setStatusKosDitanggung(ditanggung);
        setelahPerubahan(null);
    }

    // =====================================================================
    // UANG KIRIMAN
    // =====================================================================

    /** @param tanggalBerikutnya boleh null, otomatis tanggal terima + 1 bulan */
    public Kiriman tambahKiriman(long nominal, LocalDate tanggalTerima, LocalDate tanggalBerikutnya) {
        Validasi.wajibAda(tanggalTerima, "Tanggal terima");
        LocalDate berikutnya = tanggalBerikutnya != null ? tanggalBerikutnya : Kiriman.tanggalBerikutnyaDefault(tanggalTerima);
        cekTanggalKirimanUnik(tanggalTerima, null);
        Kiriman kiriman = new Kiriman(buatId("KRM"), nominal, tanggalTerima, berikutnya);
        dompet.tambahSaldo(nominal);
        daftarKiriman.add(kiriman);
        perbaruiKirimanAktif();
        setelahPerubahan(null);
        return kiriman;
    }

    /** Memperbarui kiriman aktif. Saldo hanya disesuaikan sebesar selisih nominal. */
    public void ubahKirimanAktif(long nominal, LocalDate tanggalTerima, LocalDate tanggalBerikutnya) {
        Kiriman aktif = mahasiswa.getKirimanAktif();
        if (aktif == null) {
            throw new IllegalArgumentException("Belum ada kiriman. Tambahkan kiriman terlebih dahulu.");
        }
        Validasi.wajibAda(tanggalTerima, "Tanggal terima");
        LocalDate berikutnya = tanggalBerikutnya != null ? tanggalBerikutnya : Kiriman.tanggalBerikutnyaDefault(tanggalTerima);
        cekTanggalKirimanUnik(tanggalTerima, aktif);
        new Kiriman(aktif.getIdKiriman(), nominal, tanggalTerima, berikutnya); // validasi tanpa mengubah data

        long selisih = nominal - aktif.getNominal();
        if (selisih > 0) {
            dompet.tambahSaldo(selisih);
        } else if (selisih < 0) {
            dompet.kurangiSaldo(-selisih); // ditolak jika saldo tidak cukup
        }
        aktif.setNominal(nominal);
        aktif.setPeriode(tanggalTerima, berikutnya);
        perbaruiKirimanAktif();
        setelahPerubahan(null);
    }

    private void cekTanggalKirimanUnik(LocalDate tanggalTerima, Kiriman kecuali) {
        for (Kiriman k : daftarKiriman) {
            if (k != kecuali && k.getTanggalTerima().equals(tanggalTerima)) {
                throw new IllegalArgumentException("Kiriman dengan tanggal terima "
                        + DateHelper.formatIndonesia(tanggalTerima) + " sudah ada.");
            }
        }
    }

    /** Kiriman aktif = kiriman dengan tanggal terima paling akhir. */
    private void perbaruiKirimanAktif() {
        daftarKiriman.sort(Comparator.comparing(Kiriman::getTanggalTerima));
        mahasiswa.setKirimanAktif(daftarKiriman.isEmpty() ? null : daftarKiriman.get(daftarKiriman.size() - 1));
    }

    // =====================================================================
    // PENGELUARAN TETAP
    // =====================================================================

    public PengeluaranTetap tambahPengeluaranTetap(String nama, String kategori, long nominal) {
        PengeluaranTetap baru = new PengeluaranTetap(buatId("PT"), nama, nominal, kategori);
        cekNamaPengeluaranUnik(baru.getNamaPengeluaran(), null);
        daftarPengeluaranTetap.add(baru);
        setelahPerubahan(null);
        return baru;
    }

    public void ubahPengeluaranTetap(String idPengeluaran, String nama, String kategori, long nominal) {
        PengeluaranTetap lama = cariPengeluaranTetap(idPengeluaran);
        PengeluaranTetap cek = new PengeluaranTetap(idPengeluaran, nama, nominal, kategori); // validasi dulu
        cekNamaPengeluaranUnik(cek.getNamaPengeluaran(), lama);
        lama.setNamaPengeluaran(nama);
        lama.setKategori(kategori);
        lama.setNominal(nominal);
        setelahPerubahan(null);
    }

    public void hapusPengeluaranTetap(String idPengeluaran) {
        daftarPengeluaranTetap.remove(cariPengeluaranTetap(idPengeluaran));
        setelahPerubahan(null);
    }

    private PengeluaranTetap cariPengeluaranTetap(String id) {
        for (PengeluaranTetap p : daftarPengeluaranTetap) {
            if (p.getIdPengeluaran().equals(id)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Pengeluaran tetap tidak ditemukan.");
    }

    private void cekNamaPengeluaranUnik(String nama, PengeluaranTetap kecuali) {
        for (PengeluaranTetap p : daftarPengeluaranTetap) {
            if (p != kecuali && p.getNamaPengeluaran().equalsIgnoreCase(nama)) {
                throw new IllegalArgumentException("Pengeluaran tetap dengan nama " + nama + " sudah ada.");
            }
        }
    }

    /** Daftar kategori bawaan ditambah kategori buatan pengguna yang sudah dipakai. */
    public List<String> getKategoriPengeluaranTetap() {
        List<String> hasil = new ArrayList<>(PengeluaranTetap.KATEGORI_BAWAAN);
        for (PengeluaranTetap p : daftarPengeluaranTetap) {
            boolean ada = false;
            for (String k : hasil) {
                if (k.equalsIgnoreCase(p.getKategori())) {
                    ada = true;
                    break;
                }
            }
            if (!ada) {
                hasil.add(p.getKategori());
            }
        }
        return hasil;
    }

    // =====================================================================
    // TARGET TABUNGAN
    // =====================================================================

    public void setTargetTabungan(long nominal) {
        Validasi.nominalTidakNegatif(nominal, "Target tabungan");
        if (nominal > 0 && mahasiswa.getKirimanAktif() == null) {
            throw new IllegalArgumentException("Isi uang kiriman dulu sebelum menentukan target tabungan.");
        }
        long batas = getBatasMaksimalTabungan();
        if (nominal > batas) {
            catatPeringatan(Peringatan.Jenis.TABUNGAN_TIDAK_TERPENUHI, hariIni(),
                    "Target tabungan " + RupiahFormatter.format(nominal) + " melebihi dana setelah pengeluaran tetap ("
                            + RupiahFormatter.format(batas) + ").");
            simpanSemua();
            throw new IllegalArgumentException("Target tabungan terlalu besar. Maksimal "
                    + RupiahFormatter.format(batas) + " (kiriman dikurangi pengeluaran tetap).");
        }
        targetTabungan.setTargetTabungan(nominal);
        setelahPerubahan(null);
    }

    // =====================================================================
    // TRANSAKSI HARIAN
    // =====================================================================

    /** Dipakai GUI untuk meminta konfirmasi sebelum transaksi yang melebihi sisa anggaran. */
    public boolean melebihiSisaAnggaran(long nominal, LocalDate tanggal) {
        Kiriman aktif = mahasiswa.getKirimanAktif();
        return aktif != null && tanggal != null && aktif.isDalamPeriode(tanggal) && nominal > getSisaAnggaran();
    }

    public Transaksi tambahTransaksi(LocalDate tanggal, String kategori, String keterangan, long nominal) {
        Validasi.wajibAda(tanggal, "Tanggal transaksi");
        if (tanggal.isAfter(hariIni())) {
            throw new IllegalArgumentException("Tanggal transaksi tidak boleh di masa depan.");
        }
        Transaksi transaksi = new Transaksi(buatId("TRX"), tanggal, kategori, keterangan, nominal);
        dompet.kurangiSaldo(transaksi.getNominal()); // ditolak jika saldo tidak cukup
        daftarTransaksi.add(transaksi);
        setelahPerubahan(tanggal);
        return transaksi;
    }

    public void hapusTransaksi(String idTransaksi) {
        Transaksi target = null;
        for (Transaksi t : daftarTransaksi) {
            if (t.getIdTransaksi().equals(idTransaksi)) {
                target = t;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Transaksi tidak ditemukan.");
        }
        daftarTransaksi.remove(target);
        dompet.tambahSaldo(target.getNominal()); // saldo dikembalikan
        setelahPerubahan(null);
    }

    public List<Transaksi> getTransaksiPeriodeAktif() {
        List<Transaksi> hasil = new ArrayList<>();
        Kiriman aktif = mahasiswa.getKirimanAktif();
        if (aktif == null) {
            return hasil;
        }
        for (Transaksi t : daftarTransaksi) {
            if (aktif.isDalamPeriode(t.getTanggal())) {
                hasil.add(t);
            }
        }
        return hasil;
    }

    // =====================================================================
    // REKOMENDASI HIBURAN
    // =====================================================================

    public void tambahTempatHiburan(String nama, String lokasi, String namaKota, long biayaMin, long biayaMaks, String tier) {
        Kota kota = cariKotaWajib(namaKota);
        TempatHiburan baru = new TempatHiburan(nama, lokasi, kota.getNamaKota(), biayaMin, biayaMaks, tier);
        for (TempatHiburan t : daftarTempatHiburan) {
            if (t.getNamaTempat().equalsIgnoreCase(baru.getNamaTempat()) && t.getKota().equalsIgnoreCase(baru.getKota())) {
                throw new IllegalArgumentException("Tempat " + baru.getNamaTempat() + " di " + baru.getKota() + " sudah ada.");
            }
        }
        daftarTempatHiburan.add(baru);
        setelahPerubahan(null);
    }

    public void hapusTempatHiburan(String nama, String namaKota) {
        boolean terhapus = daftarTempatHiburan.removeIf(
                t -> t.getNamaTempat().equalsIgnoreCase(nama) && t.getKota().equalsIgnoreCase(namaKota));
        if (!terhapus) {
            throw new IllegalArgumentException("Tempat hiburan tidak ditemukan.");
        }
        setelahPerubahan(null);
    }

    public List<TempatHiburan> getTempatHiburanKotaMahasiswa() {
        List<TempatHiburan> hasil = new ArrayList<>();
        for (TempatHiburan t : daftarTempatHiburan) {
            if (t.getKota().equalsIgnoreCase(mahasiswa.getKota().getNamaKota())) {
                hasil.add(t);
            }
        }
        return hasil;
    }

    /** Tempat di kota mahasiswa yang biaya minimumnya terjangkau sisa budget hiburan, termurah dulu. */
    public List<TempatHiburan> getRekomendasiHiburan() {
        long budget = getSisaBudgetHiburan();
        List<TempatHiburan> hasil = new ArrayList<>();
        for (TempatHiburan t : getTempatHiburanKotaMahasiswa()) {
            if (t.cocokkanBudget(budget)) {
                hasil.add(t);
            }
        }
        hasil.sort(Comparator.comparingLong(TempatHiburan::getBiayaMinimum).thenComparing(TempatHiburan::getNamaTempat));
        return hasil;
    }

    // =====================================================================
    // PERINGATAN
    // =====================================================================

    /** Mengecek seluruh kondisi peringatan. tanggalTransaksi diisi saat ada transaksi baru. */
    private void evaluasiPeringatan(LocalDate tanggalTransaksi) {
        Kiriman aktif = mahasiswa.getKirimanAktif();
        if (aktif == null) {
            return;
        }
        LocalDate hariIni = hariIni();

        if (tanggalTransaksi != null && aktif.isDalamPeriode(tanggalTransaksi)) {
            long totalHari = getTotalTransaksiPadaTanggal(tanggalTransaksi);
            if (totalHari > anggaran.getBudgetHarian()) {
                catatPeringatan(Peringatan.Jenis.BUDGET_HARIAN, tanggalTransaksi,
                        "Pengeluaran " + DateHelper.formatIndonesia(tanggalTransaksi) + " sebesar "
                                + RupiahFormatter.format(totalHari) + " melebihi budget harian "
                                + RupiahFormatter.format(anggaran.getBudgetHarian()) + ".");
            }
        }

        if (isAnggaranMenipis()) {
            catatPeringatan(Peringatan.Jenis.SISA_MENIPIS, hariIni,
                    "Sisa anggaran tinggal " + RupiahFormatter.format(getSisaAnggaran()) + " dari dana tersedia "
                            + RupiahFormatter.format(anggaran.getDanaTersedia()) + ". Saran: "
                            + String.join(" ", Peringatan.SARAN_HEMAT));
        }

        long totalTetap = getTotalPengeluaranTetap();
        if (totalTetap > Math.round(aktif.getNominal() * config.getAmbangPengeluaranTetap())) {
            catatPeringatan(Peringatan.Jenis.PENGELUARAN_TETAP_BESAR, hariIni,
                    "Pengeluaran tetap " + RupiahFormatter.format(totalTetap) + " lebih dari "
                            + Math.round(config.getAmbangPengeluaranTetap() * 100) + "% uang kiriman "
                            + RupiahFormatter.format(aktif.getNominal()) + ".");
        }

        if (targetTabungan.getTargetTabungan() > getBatasMaksimalTabungan()) {
            catatPeringatan(Peringatan.Jenis.TABUNGAN_TIDAK_TERPENUHI, hariIni,
                    "Target tabungan " + RupiahFormatter.format(targetTabungan.getTargetTabungan())
                            + " tidak dapat dipenuhi. Dana setelah pengeluaran tetap hanya "
                            + RupiahFormatter.format(getBatasMaksimalTabungan()) + ".");
        }
    }

    /** Menambah peringatan jika belum ada peringatan dengan jenis dan tanggal yang sama. */
    private void catatPeringatan(Peringatan.Jenis jenis, LocalDate tanggal, String pesan) {
        for (Peringatan p : daftarPeringatan) {
            if (p.samaDengan(jenis, tanggal)) {
                return;
            }
        }
        daftarPeringatan.add(Peringatan.buatPeringatan(buatId("PRG"), jenis, pesan, tanggal));
    }

    // =====================================================================
    // KONFIGURASI
    // =====================================================================

    public void ubahPersentaseBudget(double persenMakan, double persenHiburan) {
        config.setPersentaseBudget(persenMakan, persenHiburan);
        setelahPerubahan(null);
    }

    public void ubahBatasTier(long minSedang, long minLumayan, long minNyaman) {
        config.setBatasTier(minSedang, minLumayan, minNyaman);
        setelahPerubahan(null);
    }

    public void ubahAmbangPeringatan(double ambangSisa, double ambangPengeluaranTetap) {
        config.setAmbang(ambangSisa, ambangPengeluaranTetap);
        setelahPerubahan(null);
    }

    // =====================================================================
    // GETTER (mengembalikan salinan agar data internal tidak bisa diubah dari luar)
    // =====================================================================

    public Mahasiswa getMahasiswa() {
        return mahasiswa;
    }

    public long getSaldo() {
        return dompet.getSaldo();
    }

    public long getTargetTabungan() {
        return targetTabungan.getTargetTabungan();
    }

    public Anggaran getAnggaran() {
        return anggaran;
    }

    public AppConfig getConfig() {
        return config;
    }

    public long getTotalKiriman() {
        long total = 0;
        for (Kiriman k : daftarKiriman) {
            total += k.getNominal();
        }
        return total;
    }

    /** Riwayat kiriman, terbaru dulu. */
    public List<Kiriman> getDaftarKiriman() {
        List<Kiriman> hasil = new ArrayList<>(daftarKiriman);
        hasil.sort(Comparator.comparing(Kiriman::getTanggalTerima).reversed());
        return hasil;
    }

    public List<PengeluaranTetap> getDaftarPengeluaranTetap() {
        return List.copyOf(daftarPengeluaranTetap);
    }

    /** Riwayat transaksi, terbaru dulu. */
    public List<Transaksi> getDaftarTransaksi() {
        List<Transaksi> hasil = new ArrayList<>(daftarTransaksi);
        hasil.sort(Comparator.comparing(Transaksi::getTanggal).reversed());
        return hasil;
    }

    public List<TempatHiburan> getDaftarTempatHiburan() {
        return List.copyOf(daftarTempatHiburan);
    }

    /** Riwayat peringatan, terbaru dulu. */
    public List<Peringatan> getDaftarPeringatan() {
        List<Peringatan> hasil = new ArrayList<>(daftarPeringatan);
        hasil.sort(Comparator.comparing(Peringatan::getTanggal).reversed());
        return hasil;
    }

    public List<Kota> getDaftarKota() {
        return List.copyOf(daftarKota);
    }

    public LocalDate hariIni() {
        return LocalDate.now(clock);
    }

    private static String buatId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
