package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import util.Validasi;

/**
 * Satu kali uang kiriman beserta periodenya.
 * Periode anggaran: dari tanggalTerima sampai SEBELUM tanggalBerikutnya.
 */
public class Kiriman {

    private final String idKiriman;
    private long nominal;
    private LocalDate tanggalTerima;
    private LocalDate tanggalBerikutnya;

    public Kiriman(String idKiriman, long nominal, LocalDate tanggalTerima, LocalDate tanggalBerikutnya) {
        this.idKiriman = Validasi.teksWajib(idKiriman, "ID kiriman");
        setNominal(nominal);
        setPeriode(tanggalTerima, tanggalBerikutnya);
    }

    /** Default tanggal kiriman berikutnya = tanggal terima + 1 bulan. */
    public static LocalDate tanggalBerikutnyaDefault(LocalDate tanggalTerima) {
        return tanggalTerima.plusMonths(1);
    }

    public int hitungJumlahHari() {
        long hari = ChronoUnit.DAYS.between(tanggalTerima, tanggalBerikutnya);
        return (int) Math.max(1, hari);
    }

    public boolean isDalamPeriode(LocalDate tanggal) {
        return !tanggal.isBefore(tanggalTerima) && tanggal.isBefore(tanggalBerikutnya);
    }

    public String getIdKiriman() {
        return idKiriman;
    }

    public long getNominal() {
        return nominal;
    }

    public void setNominal(long nominal) {
        this.nominal = Validasi.nominalPositif(nominal, "Nominal kiriman");
    }

    public LocalDate getTanggalTerima() {
        return tanggalTerima;
    }

    public LocalDate getTanggalBerikutnya() {
        return tanggalBerikutnya;
    }

    public void setPeriode(LocalDate tanggalTerima, LocalDate tanggalBerikutnya) {
        Validasi.wajibAda(tanggalTerima, "Tanggal terima");
        Validasi.wajibAda(tanggalBerikutnya, "Tanggal kiriman berikutnya");
        if (!tanggalBerikutnya.isAfter(tanggalTerima)) {
            throw new IllegalArgumentException("Tanggal kiriman berikutnya harus setelah tanggal terima.");
        }
        this.tanggalTerima = tanggalTerima;
        this.tanggalBerikutnya = tanggalBerikutnya;
    }
}
