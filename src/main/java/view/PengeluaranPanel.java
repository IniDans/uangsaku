package view;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.PengeluaranTetap;
import service.SistemKeuangan;
import util.RupiahFormatter;

/** Mengelola pengeluaran tetap bulanan (kos, kuota, bensin, servis, dll). */
public class PengeluaranPanel extends BasePanel {

    private final JCheckBox cekKos = new JCheckBox("Biaya kos ditanggung orang tua");
    private final JTextField inputNama = new JTextField(16);
    private final JComboBox<String> pilihKategori = new JComboBox<>();
    private final JTextField inputNominal = new JTextField(12);
    private final JLabel labelTotal = Tema.subjudul(" ");
    private final DefaultTableModel model = Tema.modelTabel("Nama", "Kategori", "Nominal", "Dihitung ke anggaran");
    private final JTable tabel = Tema.tabel(model, 2, 3);
    private final List<PengeluaranTetap> tampil = new ArrayList<>();
    private boolean sedangMemuat = false;

    public PengeluaranPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Pengeluaran Tetap",
                "Biaya rutin tiap periode. Mengurangi dana tersedia, tetapi tidak memotong saldo dompet.");

        cekKos.setOpaque(false);
        cekKos.setFont(Tema.FONT_BIASA);
        cekKos.addActionListener(e -> {
            if (!sedangMemuat) {
                boolean nilai = cekKos.isSelected();
                jalankan(() -> sistem.setStatusKosDitanggung(nilai));
            }
        });
        pilihKategori.setEditable(true);

        JButton tambah = Tema.tombolPrimer("Tambah");
        tambah.addActionListener(e -> {
            if (jalankan(() -> sistem.tambahPengeluaranTetap(inputNama.getText(), kategoriDipilih(),
                    RupiahFormatter.parse(inputNominal.getText(), "Nominal")))) {
                bersihkanForm();
            }
        });
        JButton ubah = Tema.tombolSekunder("Simpan perubahan");
        ubah.addActionListener(e -> {
            PengeluaranTetap p = terpilih();
            if (p == null) {
                tampilkanError("Pilih pengeluaran di tabel terlebih dahulu.");
                return;
            }
            if (jalankan(() -> sistem.ubahPengeluaranTetap(p.getIdPengeluaran(), inputNama.getText(),
                    kategoriDipilih(), RupiahFormatter.parse(inputNominal.getText(), "Nominal")))) {
                bersihkanForm();
            }
        });
        JButton hapus = Tema.tombolBahaya("Hapus");
        hapus.addActionListener(e -> {
            PengeluaranTetap p = terpilih();
            if (p == null) {
                tampilkanError("Pilih pengeluaran di tabel terlebih dahulu.");
                return;
            }
            if (konfirmasi("Hapus pengeluaran tetap \"" + p.getNamaPengeluaran() + "\"?", "Konfirmasi hapus")
                    && jalankan(() -> sistem.hapusPengeluaranTetap(p.getIdPengeluaran()))) {
                bersihkanForm();
            }
        });
        JButton bersih = Tema.tombolSekunder("Bersihkan");
        bersih.addActionListener(e -> bersihkanForm());

        Formulir form = new Formulir()
                .tambah("Nama", inputNama)
                .tambah("Kategori", pilihKategori, "Pilih atau ketik kategori baru")
                .tambah("Nominal (Rp)", inputNominal);
        form.tombol(tambah, ubah, hapus, bersih);

        JPanel kiri = new JPanel(new BorderLayout(0, 12));
        kiri.setOpaque(false);
        JPanel kos = Tema.kartu();
        kos.setLayout(new BorderLayout(0, 4));
        kos.add(cekKos, BorderLayout.NORTH);
        kos.add(Tema.keterangan("<html>Jika dicentang, pengeluaran kategori Kos<br>dihitung Rp0.</html>"),
                BorderLayout.CENTER);
        kiri.add(kos, BorderLayout.NORTH);
        kiri.add(bagian("Form pengeluaran tetap", form), BorderLayout.CENTER);

        tabel.getSelectionModel().addListSelectionListener(e -> {
            PengeluaranTetap p = terpilih();
            if (!e.getValueIsAdjusting() && p != null) {
                inputNama.setText(p.getNamaPengeluaran());
                pilihKategori.setSelectedItem(p.getKategori());
                inputNominal.setText(RupiahFormatter.formatAngka(p.getNominal()));
            }
        });

        JPanel kanan = new JPanel(new BorderLayout(0, 8));
        kanan.setOpaque(false);
        kanan.add(Tema.gulir(tabel), BorderLayout.CENTER);
        labelTotal.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        kanan.add(labelTotal, BorderLayout.SOUTH);

        getIsi().add(kiri, BorderLayout.WEST);
        getIsi().add(bagian("Daftar pengeluaran tetap", kanan), BorderLayout.CENTER);
    }

    private String kategoriDipilih() {
        Object item = pilihKategori.getEditor().getItem();
        return item == null ? "" : item.toString();
    }

    private PengeluaranTetap terpilih() {
        int baris = tabel.getSelectedRow();
        return baris < 0 || baris >= tampil.size() ? null : tampil.get(baris);
    }

    private void bersihkanForm() {
        tabel.clearSelection();
        inputNama.setText("");
        inputNominal.setText("");
        if (pilihKategori.getItemCount() > 0) {
            pilihKategori.setSelectedIndex(0);
        }
    }

    @Override
    public void muatUlang() {
        sedangMemuat = true;
        try {
            cekKos.setSelected(sistem.getMahasiswa().isStatusKosDitanggung());
            Object kategoriSekarang = pilihKategori.getEditor().getItem();
            pilihKategori.removeAllItems();
            for (String k : sistem.getKategoriPengeluaranTetap()) {
                pilihKategori.addItem(k);
            }
            pilihKategori.setSelectedItem(kategoriSekarang == null || kategoriSekarang.toString().isEmpty()
                    ? pilihKategori.getItemAt(0) : kategoriSekarang);

            tampil.clear();
            tampil.addAll(sistem.getDaftarPengeluaranTetap());
            model.setRowCount(0);
            for (PengeluaranTetap p : tampil) {
                long dihitung = p.hitungBiaya();
                model.addRow(new Object[] {
                    p.getNamaPengeluaran(),
                    p.getKategori(),
                    RupiahFormatter.format(p.getNominal()),
                    RupiahFormatter.format(dihitung) + (dihitung != p.getNominal() ? " (ditanggung)" : "")
                });
            }
            labelTotal.setText("Total dihitung: " + RupiahFormatter.format(sistem.getTotalPengeluaranTetap()));
        } finally {
            sedangMemuat = false;
        }
    }
}
