package model;

import java.util.List;

/** INHERITANCE: pengeluaran tidak tetap sehari-hari (makan, jajan, transportasi, hiburan, lainnya). */
public class PengeluaranHarian extends Pengeluaran {

    public static final String KATEGORI_HIBURAN = "Hiburan";
    public static final List<String> DAFTAR_KATEGORI =
            List.of("Makan", "Jajan", "Transportasi", KATEGORI_HIBURAN, "Lainnya");

    public PengeluaranHarian(String idPengeluaran, String namaPengeluaran, long nominal, String kategori) {
        super(idPengeluaran, namaPengeluaran, nominal, kategori);
    }

    /** POLYMORPHISM: pengeluaran harian selalu dibayar penuh. */
    @Override
    public long hitungBiaya() {
        return getNominal();
    }

    @Override
    public String getJenis() {
        return "Harian";
    }

    /** Kategori harian dibatasi pada daftar yang ditentukan spesifikasi. */
    @Override
    protected String validasiKategori(String kategori) {
        for (String k : DAFTAR_KATEGORI) {
            if (k.equalsIgnoreCase(kategori)) {
                return k;
            }
        }
        throw new IllegalArgumentException("Kategori transaksi tidak dikenal: " + kategori);
    }

    /** Hiburan masuk pos Budget Hiburan; kategori lain masuk pos Makan/Kebutuhan. */
    public boolean isHiburan() {
        return KATEGORI_HIBURAN.equals(getKategori());
    }
}
