package model;

/**
 * Hasil perhitungan anggaran untuk satu periode kiriman.
 *
 * Dana Tersedia  = Kiriman - Total Pengeluaran Tetap - Target Tabungan (minimal 0)
 * Budget Harian  = Dana Tersedia / jumlah hari periode
 * Budget Makan   = Dana Tersedia x persen makan
 * Budget Hiburan = Dana Tersedia x persen hiburan
 */
public class Anggaran {

    private long danaTersedia;
    private long budgetHarian;
    private long budgetMakan;
    private long budgetHiburan;
    private int jumlahHari = 1;

    /** Menghitung ulang semua nilai sekaligus agar pembagian selalu konsisten. */
    public void hitungUlangAnggaran(long nominalKiriman, long totalPengeluaranTetap, long targetTabungan,
                                    int jumlahHari, double persenMakan, double persenHiburan) {
        if (persenMakan < 0 || persenHiburan < 0 || persenMakan + persenHiburan > 1.0 + 1e-9) {
            throw new IllegalArgumentException("Persentase makan + hiburan harus antara 0% dan 100%.");
        }
        this.jumlahHari = Math.max(1, jumlahHari);
        this.danaTersedia = hitungDanaTersedia(nominalKiriman, totalPengeluaranTetap, targetTabungan);
        this.budgetHarian = hitungBudgetHarian();
        this.budgetMakan = hitungBudgetMakan(persenMakan);
        this.budgetHiburan = hitungBudgetHiburan(persenHiburan);
    }

    /** Tidak pernah negatif: kekurangan dana ditangani lewat peringatan, bukan angka minus. */
    public long hitungDanaTersedia(long nominalKiriman, long totalPengeluaranTetap, long targetTabungan) {
        return Math.max(0, nominalKiriman - totalPengeluaranTetap - targetTabungan);
    }

    public long hitungBudgetHarian() {
        return danaTersedia / jumlahHari;
    }

    public long hitungBudgetMakan(double persenMakan) {
        return Math.round(danaTersedia * persenMakan);
    }

    public long hitungBudgetHiburan(double persenHiburan) {
        return Math.round(danaTersedia * persenHiburan);
    }

    public long getDanaTersedia() {
        return danaTersedia;
    }

    public long getBudgetHarian() {
        return budgetHarian;
    }

    public long getBudgetMakan() {
        return budgetMakan;
    }

    public long getBudgetHiburan() {
        return budgetHiburan;
    }

    public int getJumlahHari() {
        return jumlahHari;
    }
}
