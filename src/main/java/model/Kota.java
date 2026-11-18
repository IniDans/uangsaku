package model;

import util.Validasi;

/**
 * Kota rantau beserta data untuk menyesuaikan batas tier terhadap kota acuan (Malang).
 * persenKenaikanHarga boleh null: artinya faktor dihitung dari perbandingan UMK.
 */
public class Kota {

    public static final double PERSEN_MIN = -50;
    public static final double PERSEN_MAX = 200;

    private final String namaKota;
    private long umk;
    private Double persenKenaikanHarga;

    public Kota(String namaKota, long umk, Double persenKenaikanHarga) {
        this.namaKota = Validasi.teksWajib(namaKota, "Nama kota");
        setUmk(umk);
        setPersenKenaikanHarga(persenKenaikanHarga);
    }

    /**
     * Faktor penyesuaian terhadap kota acuan.
     * - persen diisi  : 1 + persen/100 (perkiraan pengguna dari pengalaman)
     * - persen kosong : umkKota / umkAcuan (pendekatan kasar)
     */
    public double hitungFaktor(long umkAcuan) {
        if (persenKenaikanHarga != null) {
            return 1 + persenKenaikanHarga / 100.0;
        }
        if (umkAcuan <= 0) {
            return 1.0;
        }
        return (double) umk / umkAcuan;
    }

    public String getNamaKota() {
        return namaKota;
    }

    public long getUmk() {
        return umk;
    }

    public void setUmk(long umk) {
        this.umk = Validasi.nominalPositif(umk, "UMK");
    }

    public Double getPersenKenaikanHarga() {
        return persenKenaikanHarga;
    }

    public void setPersenKenaikanHarga(Double persen) {
        if (persen != null && (persen.isNaN() || persen < PERSEN_MIN || persen > PERSEN_MAX)) {
            throw new IllegalArgumentException("Persen kenaikan harga harus antara -50 dan 200.");
        }
        this.persenKenaikanHarga = persen;
    }

    @Override
    public String toString() {
        return namaKota;
    }
}
