package model;

import util.RupiahFormatter;
import util.Validasi;

/**
 * Menyimpan saldo uang yang benar-benar dimiliki mahasiswa.
 * Saldo hanya bisa berubah lewat tambahSaldo() dan kurangiSaldo(), tidak ada setSaldo().
 */
public class Dompet {

    private final String idDompet;
    private long saldo;

    public Dompet(String idDompet, long saldoAwal) {
        this.idDompet = Validasi.teksWajib(idDompet, "ID dompet");
        this.saldo = Validasi.nominalTidakNegatif(saldoAwal, "Saldo awal");
    }

    public void tambahSaldo(long nominal) {
        Validasi.nominalPositif(nominal, "Nominal");
        saldo += nominal;
    }

    public void kurangiSaldo(long nominal) {
        Validasi.nominalPositif(nominal, "Nominal");
        if (nominal > saldo) {
            throw new IllegalArgumentException("Saldo tidak cukup. Saldo saat ini " + RupiahFormatter.format(saldo) + ".");
        }
        saldo -= nominal;
    }

    public long getSaldo() {
        return saldo;
    }

    public String getIdDompet() {
        return idDompet;
    }
}
