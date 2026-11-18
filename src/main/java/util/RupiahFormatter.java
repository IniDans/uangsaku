package util;

import java.text.NumberFormat;
import java.util.Locale;

/** Mengubah angka menjadi teks Rupiah (Rp1.500.000) dan sebaliknya. */
public final class RupiahFormatter {

    private static final Locale INDONESIA = Locale.forLanguageTag("id-ID");

    private RupiahFormatter() {
    }

    public static String format(long nominal) {
        NumberFormat angka = NumberFormat.getIntegerInstance(INDONESIA);
        String hasil = "Rp" + angka.format(Math.abs(nominal));
        return nominal < 0 ? "-" + hasil : hasil;
    }

    /** Format angka tanpa "Rp", dipakai untuk mengisi kolom input. */
    public static String formatAngka(long nominal) {
        return NumberFormat.getIntegerInstance(INDONESIA).format(nominal);
    }

    /**
     * Membaca input pengguna seperti "1500000", "1.500.000", atau "Rp1.500.000".
     *
     * @throws IllegalArgumentException jika kosong, negatif, atau bukan angka
     */
    public static long parse(String input, String namaField) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(namaField + " wajib diisi.");
        }
        String bersih = input.trim().replace("Rp", "").replace("rp", "").replace(".", "").replace(" ", "");
        if (bersih.startsWith("-")) {
            throw new IllegalArgumentException(namaField + " tidak boleh negatif.");
        }
        if (!bersih.matches("\\d+")) {
            throw new IllegalArgumentException(namaField + " harus berupa angka bulat, contoh 1.500.000");
        }
        try {
            return Long.parseLong(bersih);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(namaField + " terlalu besar.");
        }
    }
}
