package model;

import java.util.List;

/**
 * INHERITANCE: pengeluaran rutin bulanan (kos, kuota, bensin, servis motor, dll.).
 * Kategori bebas: selain kategori bawaan, pengguna boleh membuat kategori sendiri.
 */
public class PengeluaranTetap extends Pengeluaran {

    public static final String KATEGORI_KOS = "Kos";
    public static final List<String> KATEGORI_BAWAAN =
            List.of(KATEGORI_KOS, "Kuota internet", "Bensin", "Servis motor", "Lainnya");

    // Diisi oleh SistemKeuangan dari status Mahasiswa, tidak disimpan ke file
    private boolean ditanggungOrangTua;

    public PengeluaranTetap(String idPengeluaran, String namaPengeluaran, long nominal, String kategori) {
        super(idPengeluaran, namaPengeluaran, nominal, kategori);
    }

    /** POLYMORPHISM: biaya kos menjadi 0 jika kos ditanggung orang tua. */
    @Override
    public long hitungBiaya() {
        if (isKos() && ditanggungOrangTua) {
            return 0;
        }
        return getNominal();
    }

    @Override
    public String getJenis() {
        return "Tetap";
    }

    public boolean isKos() {
        return KATEGORI_KOS.equalsIgnoreCase(getKategori());
    }

    public boolean isDitanggungOrangTua() {
        return ditanggungOrangTua;
    }

    public void setDitanggungOrangTua(boolean ditanggungOrangTua) {
        this.ditanggungOrangTua = ditanggungOrangTua;
    }
}
