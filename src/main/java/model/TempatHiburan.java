package model;

import util.Validasi;

/** Tempat hiburan beserta estimasi biaya (data dummy dari pengalaman pribadi). */
public class TempatHiburan {

    private final String namaTempat;
    private final String lokasi;
    private final String kota;
    private final long biayaMinimum;
    private final long biayaMaksimum;
    private final String tier;

    public TempatHiburan(String namaTempat, String lokasi, String kota,
                         long biayaMinimum, long biayaMaksimum, String tier) {
        this.namaTempat = Validasi.teksWajib(namaTempat, "Nama tempat");
        this.lokasi = Validasi.teksWajib(lokasi, "Lokasi");
        this.kota = Validasi.teksWajib(kota, "Kota");
        this.biayaMinimum = Validasi.nominalTidakNegatif(biayaMinimum, "Biaya minimum");
        this.biayaMaksimum = Validasi.nominalTidakNegatif(biayaMaksimum, "Biaya maksimum");
        if (biayaMaksimum < biayaMinimum) {
            throw new IllegalArgumentException("Biaya maksimum tidak boleh lebih kecil dari biaya minimum.");
        }
        if (!TierAnggaran.NAMA_TIER.contains(tier)) {
            throw new IllegalArgumentException("Tier harus salah satu dari " + TierAnggaran.NAMA_TIER + ".");
        }
        this.tier = tier;
    }

    /** Tempat cocok jika biaya minimumnya masih terjangkau oleh budget. */
    public boolean cocokkanBudget(long budgetHiburan) {
        return biayaMinimum <= budgetHiburan;
    }

    public String getNamaTempat() {
        return namaTempat;
    }

    public String getLokasi() {
        return lokasi;
    }

    public String getKota() {
        return kota;
    }

    public long getBiayaMinimum() {
        return biayaMinimum;
    }

    public long getBiayaMaksimum() {
        return biayaMaksimum;
    }

    public String getTier() {
        return tier;
    }
}
