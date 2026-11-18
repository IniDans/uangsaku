package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Format tanggal:
 * - input pengguna  : dd/MM/yyyy (contoh 18/11/2026)
 * - tampilan        : 18 November 2026
 * - file CSV        : ISO yyyy-MM-dd (tidak ambigu)
 */
public final class DateHelper {

    private static final Locale INDONESIA = Locale.forLanguageTag("id-ID");
    // STRICT + "uuuu" menolak tanggal mustahil seperti 31/02/2026
    private static final DateTimeFormatter FORMAT_INPUT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMAT_TAMPIL =
            DateTimeFormatter.ofPattern("d MMMM yyyy", INDONESIA);

    private DateHelper() {
    }

    public static LocalDate parseInput(String input, String namaField) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(namaField + " wajib diisi.");
        }
        try {
            return LocalDate.parse(input.trim(), FORMAT_INPUT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(namaField + " tidak valid. Gunakan format dd/MM/yyyy, contoh 18/11/2026.");
        }
    }

    public static String formatInput(LocalDate tanggal) {
        return tanggal == null ? "" : tanggal.format(FORMAT_INPUT);
    }

    public static String formatIndonesia(LocalDate tanggal) {
        return tanggal == null ? "-" : tanggal.format(FORMAT_TAMPIL);
    }

    /** Untuk file CSV. Melempar DateTimeParseException jika rusak (ditangani repository). */
    public static LocalDate dariIso(String teks) {
        return LocalDate.parse(teks.trim());
    }

    public static String keIso(LocalDate tanggal) {
        return tanggal.toString();
    }
}
