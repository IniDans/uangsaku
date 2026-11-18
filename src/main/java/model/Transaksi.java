package model;

import java.time.LocalDate;
import util.Validasi;

/**
 * Satu catatan transaksi harian.
 * KOMPOSISI: Transaksi membuat dan memiliki sebuah PengeluaranHarian;
 * PengeluaranHarian itu tidak hidup di luar transaksinya.
 * Penambahan/penghapusan transaksi dari daftar dilakukan oleh SistemKeuangan.
 */
public class Transaksi {

    private final String idTransaksi;
    private final LocalDate tanggal;
    private final PengeluaranHarian pengeluaran;

    public Transaksi(String idTransaksi, LocalDate tanggal, String kategori, String keterangan, long nominal) {
        this.idTransaksi = Validasi.teksWajib(idTransaksi, "ID transaksi");
        this.tanggal = Validasi.wajibAda(tanggal, "Tanggal transaksi");
        this.pengeluaran = new PengeluaranHarian(idTransaksi, keterangan, nominal, kategori);
    }

    public String getIdTransaksi() {
        return idTransaksi;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public PengeluaranHarian getPengeluaran() {
        return pengeluaran;
    }

    public long getNominal() {
        return pengeluaran.hitungBiaya();
    }

    public String getKategori() {
        return pengeluaran.getKategori();
    }

    public String getKeterangan() {
        return pengeluaran.getNamaPengeluaran();
    }
}
