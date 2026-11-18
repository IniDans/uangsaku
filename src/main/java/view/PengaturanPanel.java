package view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.Kota;
import service.SistemKeuangan;
import util.AppConfig;
import util.RupiahFormatter;

/** Pengaturan profil, persentase budget, batas tier, ambang peringatan, dan data kota. */
public class PengaturanPanel extends BasePanel {

    private static final Locale ID = Locale.forLanguageTag("id-ID");

    private final JTextField inputNama = new JTextField(16);
    private final JComboBox<String> pilihKota = new JComboBox<>();

    private final JTextField inputPersenMakan = new JTextField(6);
    private final JTextField inputPersenHiburan = new JTextField(6);

    private final JTextField inputTierSedang = new JTextField(10);
    private final JTextField inputTierLumayan = new JTextField(10);
    private final JTextField inputTierNyaman = new JTextField(10);

    private final JTextField inputAmbangSisa = new JTextField(6);
    private final JTextField inputAmbangTetap = new JTextField(6);

    private final DefaultTableModel modelKota = Tema.modelTabel("Kota", "UMK", "Kenaikan harga", "Faktor");
    private final JTable tabelKota = Tema.tabel(modelKota, 1, 2, 3);
    private final List<Kota> tampilKota = new ArrayList<>();
    private final JTextField inputNamaKota = new JTextField(14);
    private final JTextField inputUmk = new JTextField(10);
    private final JTextField inputPersenKota = new JTextField(6);

    private boolean formTerisi = false;

    public PengaturanPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Pengaturan",
                "Semua nilai disimpan ke data/config.properties dan file CSV, lalu anggaran dihitung ulang.");

        JButton simpanProfil = Tema.tombolPrimer("Simpan profil");
        simpanProfil.addActionListener(e -> simpan(() -> sistem.ubahProfil(inputNama.getText(),
                (String) pilihKota.getSelectedItem())));
        Formulir profil = new Formulir()
                .tambah("Nama mahasiswa", inputNama)
                .tambah("Kota rantau", pilihKota, "Menentukan faktor biaya dan rekomendasi hiburan");
        profil.tombol(simpanProfil);

        JButton simpanPersen = Tema.tombolPrimer("Simpan persentase");
        simpanPersen.addActionListener(e -> simpan(() -> sistem.ubahPersentaseBudget(
                parsePersen(inputPersenMakan.getText(), "Persentase makan"),
                parsePersen(inputPersenHiburan.getText(), "Persentase hiburan"))));
        JButton bawaanPersen = Tema.tombolSekunder("Kembalikan 70/30");
        bawaanPersen.addActionListener(e -> simpan(() -> sistem.ubahPersentaseBudget(
                AppConfig.DEFAULT_PERSEN_MAKAN, AppConfig.DEFAULT_PERSEN_HIBURAN)));
        Formulir persen = new Formulir()
                .tambah("Makan & kebutuhan (%)", inputPersenMakan)
                .tambah("Hiburan (%)", inputPersenHiburan, "Jumlah keduanya maksimal 100%");
        persen.tombol(simpanPersen, bawaanPersen);

        JButton simpanTier = Tema.tombolPrimer("Simpan batas tier");
        simpanTier.addActionListener(e -> simpan(() -> sistem.ubahBatasTier(
                RupiahFormatter.parse(inputTierSedang.getText(), "Batas Sedang"),
                RupiahFormatter.parse(inputTierLumayan.getText(), "Batas Lumayan"),
                RupiahFormatter.parse(inputTierNyaman.getText(), "Batas Nyaman"))));
        Formulir tier = new Formulir()
                .tambah("Sedang mulai dari (Rp)", inputTierSedang)
                .tambah("Lumayan mulai dari (Rp)", inputTierLumayan)
                .tambah("Nyaman mulai dari (Rp)", inputTierNyaman, "Batas untuk kota acuan; kota lain dikali faktor");
        tier.tombol(simpanTier);

        JButton simpanAmbang = Tema.tombolPrimer("Simpan ambang");
        simpanAmbang.addActionListener(e -> simpan(() -> sistem.ubahAmbangPeringatan(
                parsePersen(inputAmbangSisa.getText(), "Ambang sisa anggaran"),
                parsePersen(inputAmbangTetap.getText(), "Ambang pengeluaran tetap"))));
        Formulir ambang = new Formulir()
                .tambah("Sisa anggaran menipis di bawah (%)", inputAmbangSisa, "Persen dari dana tersedia")
                .tambah("Pengeluaran tetap besar di atas (%)", inputAmbangTetap, "Persen dari uang kiriman");
        ambang.tombol(simpanAmbang);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);
        grid.add(bagian("Profil", profil));
        grid.add(bagian("Persentase budget", persen));
        grid.add(bagian("Batas tier anggaran", tier));
        grid.add(bagian("Ambang peringatan", ambang));

        JPanel isiHalaman = new JPanel(new BorderLayout(0, 16));
        isiHalaman.setOpaque(false);
        isiHalaman.add(grid, BorderLayout.NORTH);
        isiHalaman.add(bagianKota(), BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(isiHalaman);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Tema.LATAR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        getIsi().add(scroll, BorderLayout.CENTER);
    }

    private JPanel bagianKota() {
        JButton tambah = Tema.tombolPrimer("Tambah kota");
        tambah.addActionListener(e -> simpan(() -> sistem.tambahKota(inputNamaKota.getText(),
                RupiahFormatter.parse(inputUmk.getText(), "UMK"), persenKotaOpsional())));
        JButton ubah = Tema.tombolSekunder("Simpan perubahan");
        ubah.addActionListener(e -> simpan(() -> sistem.ubahKota(inputNamaKota.getText(),
                RupiahFormatter.parse(inputUmk.getText(), "UMK"), persenKotaOpsional())));
        JButton hapus = Tema.tombolBahaya("Hapus");
        hapus.addActionListener(e -> {
            String nama = inputNamaKota.getText().trim();
            if (nama.isEmpty()) {
                tampilkanError("Pilih kota di tabel terlebih dahulu.");
                return;
            }
            if (konfirmasi("Hapus kota " + nama + "?", "Konfirmasi hapus")) {
                simpan(() -> sistem.hapusKota(nama));
            }
        });
        Formulir form = new Formulir()
                .tambah("Nama kota", inputNamaKota, "Untuk mengubah, pilih kota di tabel")
                .tambah("UMK (Rp)", inputUmk)
                .tambah("Kenaikan harga (%)", inputPersenKota, "Kosongkan = faktor dari UMK ÷ UMK kota acuan");
        form.tombol(tambah, ubah, hapus);

        tabelKota.getSelectionModel().addListSelectionListener(e -> {
            int baris = tabelKota.getSelectedRow();
            if (!e.getValueIsAdjusting() && baris >= 0 && baris < tampilKota.size()) {
                Kota k = tampilKota.get(baris);
                inputNamaKota.setText(k.getNamaKota());
                inputUmk.setText(RupiahFormatter.formatAngka(k.getUmk()));
                inputPersenKota.setText(k.getPersenKenaikanHarga() == null ? ""
                        : formatAngka(k.getPersenKenaikanHarga()));
            }
        });

        JPanel isi = new JPanel(new BorderLayout(16, 0));
        isi.setOpaque(false);
        isi.add(form, BorderLayout.WEST);
        JScrollPane gulir = Tema.gulir(tabelKota);
        gulir.setPreferredSize(new java.awt.Dimension(400, 200));
        isi.add(gulir, BorderLayout.CENTER);
        return bagian("Kota dan faktor biaya hidup (acuan: " + sistem.getConfig().getKotaAcuan() + " = 1,00)", isi);
    }

    private void simpan(Aksi aksi) {
        if (jalankan(aksi)) {
            isiForm();
            info("Pengaturan disimpan.");
        }
    }

    private Double persenKotaOpsional() {
        String teks = inputPersenKota.getText().trim().replace("%", "").replace(',', '.');
        if (teks.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(teks);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Kenaikan harga harus berupa angka persen, contoh 10 atau 7,5.");
        }
    }

    /** "70" atau "70%" atau "7,5" -> 0.70 / 0.075 */
    private static double parsePersen(String teks, String namaField) {
        String bersih = teks == null ? "" : teks.trim().replace("%", "").replace(',', '.');
        if (bersih.isEmpty()) {
            throw new IllegalArgumentException(namaField + " wajib diisi.");
        }
        try {
            return Double.parseDouble(bersih) / 100.0;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(namaField + " harus berupa angka persen, contoh 70.");
        }
    }

    private static String formatAngka(double nilai) {
        if (Math.abs(nilai - Math.rint(nilai)) < 1e-9) {
            return String.valueOf(Math.round(nilai));
        }
        return String.format(ID, "%.2f", nilai).replaceAll("0+$", "");
    }

    private void isiForm() {
        AppConfig c = sistem.getConfig();
        inputNama.setText(sistem.getMahasiswa().getNamaMahasiswa());
        inputPersenMakan.setText(formatAngka(c.getPersenMakan() * 100));
        inputPersenHiburan.setText(formatAngka(c.getPersenHiburan() * 100));
        inputTierSedang.setText(RupiahFormatter.formatAngka(c.getTierMinSedang()));
        inputTierLumayan.setText(RupiahFormatter.formatAngka(c.getTierMinLumayan()));
        inputTierNyaman.setText(RupiahFormatter.formatAngka(c.getTierMinNyaman()));
        inputAmbangSisa.setText(formatAngka(c.getAmbangSisa() * 100));
        inputAmbangTetap.setText(formatAngka(c.getAmbangPengeluaranTetap() * 100));
        pilihKota.setSelectedItem(sistem.getMahasiswa().getKota().getNamaKota());
        formTerisi = true;
    }

    @Override
    public void muatUlang() {
        pilihKota.removeAllItems();
        for (Kota k : sistem.getDaftarKota()) {
            pilihKota.addItem(k.getNamaKota());
        }
        pilihKota.setSelectedItem(sistem.getMahasiswa().getKota().getNamaKota());
        if (!formTerisi) {
            isiForm();
        }

        tampilKota.clear();
        tampilKota.addAll(sistem.getDaftarKota());
        modelKota.setRowCount(0);
        for (Kota k : tampilKota) {
            String kenaikan = k.getPersenKenaikanHarga() == null ? "(dari UMK)"
                    : formatAngka(k.getPersenKenaikanHarga()) + "%";
            modelKota.addRow(new Object[] {
                k.getNamaKota() + (sistem.isKotaAcuan(k) ? " (acuan)" : ""),
                RupiahFormatter.format(k.getUmk()),
                sistem.isKotaAcuan(k) ? "-" : kenaikan,
                String.format(ID, "%.2f", sistem.hitungFaktor(k))
            });
        }
    }
}
