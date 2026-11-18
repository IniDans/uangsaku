package model;

import java.time.LocalDate;
import java.util.List;
import util.Validasi;

/** Satu peringatan keuangan yang disimpan sebagai riwayat. */
public class Peringatan {

    public enum Jenis {
        BUDGET_HARIAN("Budget harian terlampaui"),
        SISA_MENIPIS("Sisa anggaran menipis"),
        PENGELUARAN_TETAP_BESAR("Pengeluaran tetap terlalu besar"),
        TABUNGAN_TIDAK_TERPENUHI("Target tabungan tidak dapat dipenuhi");

        private final String label;

        Jenis(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** Saran yang ditampilkan ketika anggaran menipis. */
    public static final List<String> SARAN_HEMAT = List.of(
            "Kurangi pengeluaran hiburan.",
            "Pilih hiburan gratis atau murah.",
            "Kurangi pengeluaran yang tidak penting.",
            "Tahan pengeluaran sampai kiriman berikutnya.");

    private final String idPeringatan;
    private final Jenis jenis;
    private final String pesan;
    private final LocalDate tanggal;

    public Peringatan(String idPeringatan, Jenis jenis, String pesan, LocalDate tanggal) {
        this.idPeringatan = Validasi.teksWajib(idPeringatan, "ID peringatan");
        this.jenis = Validasi.wajibAda(jenis, "Jenis peringatan");
        this.pesan = Validasi.teksWajib(pesan, "Pesan peringatan");
        this.tanggal = Validasi.wajibAda(tanggal, "Tanggal peringatan");
    }

    /** Factory method agar pembuatan peringatan seragam. */
    public static Peringatan buatPeringatan(String idPeringatan, Jenis jenis, String pesan, LocalDate tanggal) {
        return new Peringatan(idPeringatan, jenis, pesan, tanggal);
    }

    /** Dipakai untuk mencegah peringatan ganda (jenis + tanggal sama). */
    public boolean samaDengan(Jenis jenisLain, LocalDate tanggalLain) {
        return jenis == jenisLain && tanggal.equals(tanggalLain);
    }

    public String getIdPeringatan() {
        return idPeringatan;
    }

    public Jenis getJenis() {
        return jenis;
    }

    public String getPesan() {
        return pesan;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }
}
