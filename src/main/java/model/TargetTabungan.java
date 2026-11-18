package model;

import util.Validasi;

/**
 * Target tabungan bulanan. Boleh Rp0.
 * Batas atas (tidak melebihi dana setelah pengeluaran tetap) dicek oleh SistemKeuangan
 * karena butuh data kiriman dan pengeluaran tetap.
 */
public class TargetTabungan {

    private long nominalTabungan;

    public TargetTabungan(long nominalTabungan) {
        setTargetTabungan(nominalTabungan);
    }

    public long getTargetTabungan() {
        return nominalTabungan;
    }

    public void setTargetTabungan(long nominalTabungan) {
        this.nominalTabungan = Validasi.nominalTidakNegatif(nominalTabungan, "Target tabungan");
    }
}
