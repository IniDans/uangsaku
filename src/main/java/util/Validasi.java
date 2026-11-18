package util;

/**
 * Kumpulan validasi yang dipakai berulang oleh banyak model.
 * Semua method melempar IllegalArgumentException dengan pesan berbahasa Indonesia
 * agar GUI cukup menampilkan getMessage() kepada pengguna.
 */
public final class Validasi {

    private Validasi() {
    }

    /** Teks wajib diisi dan tidak boleh mengandung ';' (delimiter file CSV). */
    public static String teksWajib(String teks, String namaField) {
        if (teks == null || teks.isBlank()) {
            throw new IllegalArgumentException(namaField + " wajib diisi.");
        }
        if (teks.contains(";")) {
            throw new IllegalArgumentException(namaField + " tidak boleh mengandung tanda ';'.");
        }
        return teks.trim();
    }

    public static long nominalTidakNegatif(long nominal, String namaField) {
        if (nominal < 0) {
            throw new IllegalArgumentException(namaField + " tidak boleh negatif.");
        }
        return nominal;
    }

    public static long nominalPositif(long nominal, String namaField) {
        if (nominal <= 0) {
            throw new IllegalArgumentException(namaField + " harus lebih dari Rp0.");
        }
        return nominal;
    }

    public static <T> T wajibAda(T objek, String namaField) {
        if (objek == null) {
            throw new IllegalArgumentException(namaField + " wajib diisi.");
        }
        return objek;
    }
}
