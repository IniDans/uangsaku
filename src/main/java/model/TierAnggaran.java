package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Tier kondisi anggaran berdasarkan dana tersedia per bulan.
 * Batas minimum dan maksimum bersifat inklusif.
 */
public class TierAnggaran {

    public static final String HEMAT = "Hemat";
    public static final String SEDANG = "Sedang";
    public static final String LUMAYAN = "Lumayan";
    public static final String NYAMAN = "Nyaman";
    public static final List<String> NAMA_TIER = List.of(HEMAT, SEDANG, LUMAYAN, NYAMAN);

    private final String namaTier;
    private final long batasMinimum;
    private final long batasMaksimum;

    public TierAnggaran(String namaTier, long batasMinimum, long batasMaksimum) {
        if (batasMinimum < 0 || batasMaksimum < batasMinimum) {
            throw new IllegalArgumentException("Batas tier " + namaTier + " tidak valid.");
        }
        this.namaTier = namaTier;
        this.batasMinimum = batasMinimum;
        this.batasMaksimum = batasMaksimum;
    }

    /**
     * Membuat 4 tier dari batas minimum Sedang, Lumayan, dan Nyaman milik kota acuan,
     * lalu diskalakan dengan faktor kota.
     */
    public static List<TierAnggaran> buatDaftarTier(long minSedang, long minLumayan, long minNyaman, double faktorKota) {
        if (minSedang <= 0 || minLumayan <= minSedang || minNyaman <= minLumayan) {
            throw new IllegalArgumentException("Batas tier harus naik: Sedang < Lumayan < Nyaman dan lebih dari Rp0.");
        }
        if (faktorKota <= 0) {
            throw new IllegalArgumentException("Faktor kota harus lebih dari 0.");
        }
        long sedang = Math.max(1, Math.round(minSedang * faktorKota));
        long lumayan = Math.max(sedang + 1, Math.round(minLumayan * faktorKota));
        long nyaman = Math.max(lumayan + 1, Math.round(minNyaman * faktorKota));

        List<TierAnggaran> daftar = new ArrayList<>();
        daftar.add(new TierAnggaran(HEMAT, 0, sedang - 1));
        daftar.add(new TierAnggaran(SEDANG, sedang, lumayan - 1));
        daftar.add(new TierAnggaran(LUMAYAN, lumayan, nyaman - 1));
        daftar.add(new TierAnggaran(NYAMAN, nyaman, Long.MAX_VALUE));
        return daftar;
    }

    /** Mengembalikan tier yang mencakup dana tersedia. */
    public static TierAnggaran tentukanTier(long danaTersedia, List<TierAnggaran> daftarTier) {
        for (TierAnggaran tier : daftarTier) {
            if (tier.mencakup(danaTersedia)) {
                return tier;
            }
        }
        return daftarTier.get(0);
    }

    public boolean mencakup(long dana) {
        return dana >= batasMinimum && dana <= batasMaksimum;
    }

    public String getNamaTier() {
        return namaTier;
    }

    public long getBatasMinimum() {
        return batasMinimum;
    }

    public long getBatasMaksimum() {
        return batasMaksimum;
    }
}
