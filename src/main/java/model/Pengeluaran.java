package model;

import util.Validasi;

/**
 * ABSTRACTION: kelas dasar untuk semua jenis pengeluaran.
 * Tidak bisa dibuat langsung (abstract); setiap turunan wajib menentukan
 * cara menghitung biayanya sendiri lewat hitungBiaya().
 */
public abstract class Pengeluaran {

    // ENCAPSULATION: semua atribut private, diubah hanya lewat setter yang memvalidasi
    private final String idPengeluaran;
    private String namaPengeluaran;
    private long nominal;
    private String kategori;

    protected Pengeluaran(String idPengeluaran, String namaPengeluaran, long nominal, String kategori) {
        this.idPengeluaran = Validasi.teksWajib(idPengeluaran, "ID pengeluaran");
        setNamaPengeluaran(namaPengeluaran);
        setNominal(nominal);
        setKategori(kategori);
    }

    /**
     * POLYMORPHISM: setiap turunan punya aturan biaya berbeda.
     * Kode pemanggil cukup memanggil hitungBiaya() tanpa peduli jenis aslinya.
     */
    public abstract long hitungBiaya();

    /** Nama jenis pengeluaran untuk ditampilkan, misalnya "Tetap" atau "Harian". */
    public abstract String getJenis();

    public String getIdPengeluaran() {
        return idPengeluaran;
    }

    public String getNamaPengeluaran() {
        return namaPengeluaran;
    }

    public void setNamaPengeluaran(String namaPengeluaran) {
        this.namaPengeluaran = Validasi.teksWajib(namaPengeluaran, "Nama pengeluaran");
    }

    public long getNominal() {
        return nominal;
    }

    public void setNominal(long nominal) {
        this.nominal = Validasi.nominalPositif(nominal, "Nominal pengeluaran");
    }

    public String getKategori() {
        return kategori;
    }

    public void setKategori(String kategori) {
        this.kategori = validasiKategori(Validasi.teksWajib(kategori, "Kategori"));
    }

    /** Turunan boleh membatasi kategori yang diizinkan. Default: semua kategori boleh. */
    protected String validasiKategori(String kategori) {
        return kategori;
    }
}
