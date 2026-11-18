package model;

import java.time.LocalDate;
import util.Validasi;

/** Data mahasiswa rantau. Nominal dan tanggal kiriman diambil dari kiriman yang sedang aktif. */
public class Mahasiswa {

    private final String idMahasiswa;
    private String namaMahasiswa;
    private boolean statusKosDitanggung;
    private Kota kota;
    private Kiriman kirimanAktif; // null jika belum pernah menerima kiriman

    public Mahasiswa(String idMahasiswa, String namaMahasiswa, boolean statusKosDitanggung, Kota kota) {
        this.idMahasiswa = Validasi.teksWajib(idMahasiswa, "ID mahasiswa");
        setNamaMahasiswa(namaMahasiswa);
        this.statusKosDitanggung = statusKosDitanggung;
        setKota(kota);
    }

    public long getNominalKiriman() {
        return kirimanAktif == null ? 0 : kirimanAktif.getNominal();
    }

    public LocalDate getTanggalKiriman() {
        return kirimanAktif == null ? null : kirimanAktif.getTanggalTerima();
    }

    public String getIdMahasiswa() {
        return idMahasiswa;
    }

    public String getNamaMahasiswa() {
        return namaMahasiswa;
    }

    public void setNamaMahasiswa(String namaMahasiswa) {
        this.namaMahasiswa = Validasi.teksWajib(namaMahasiswa, "Nama mahasiswa");
    }

    public boolean isStatusKosDitanggung() {
        return statusKosDitanggung;
    }

    public void setStatusKosDitanggung(boolean statusKosDitanggung) {
        this.statusKosDitanggung = statusKosDitanggung;
    }

    public Kota getKota() {
        return kota;
    }

    public void setKota(Kota kota) {
        this.kota = Validasi.wajibAda(kota, "Kota rantau");
    }

    public Kiriman getKirimanAktif() {
        return kirimanAktif;
    }

    public void setKirimanAktif(Kiriman kirimanAktif) {
        this.kirimanAktif = kirimanAktif;
    }
}
