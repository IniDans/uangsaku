package view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.Kota;
import model.TempatHiburan;
import model.TierAnggaran;
import service.SistemKeuangan;
import util.RupiahFormatter;

/** Rekomendasi hiburan sesuai kota rantau dan sisa budget hiburan, plus pengelolaan data tempat. */
public class HiburanPanel extends BasePanel {

    private final JLabel infoBudget = new JLabel();
    private final DefaultTableModel modelRekomendasi = Tema.modelTabel("Tempat", "Lokasi", "Estimasi biaya", "Tier");
    private final DefaultTableModel modelSemua = Tema.modelTabel("Tempat", "Lokasi", "Kota", "Estimasi biaya", "Tier");
    private final JTable tabelSemua = Tema.tabel(modelSemua, 3);
    private final List<TempatHiburan> tampil = new ArrayList<>();

    private final JTextField inputNama = new JTextField(14);
    private final JTextField inputLokasi = new JTextField(14);
    private final JComboBox<String> pilihKota = new JComboBox<>();
    private final JTextField inputMin = new JTextField(10);
    private final JTextField inputMaks = new JTextField(10);
    private final JComboBox<String> pilihTier = new JComboBox<>(TierAnggaran.NAMA_TIER.toArray(new String[0]));

    public HiburanPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Rekomendasi Hiburan",
                "Tempat di kota rantau yang biaya minimumnya masih terjangkau sisa budget hiburan. Biaya = estimasi dummy.");
        infoBudget.setFont(Tema.FONT_BIASA);

        JPanel rekomendasi = new JPanel(new BorderLayout(0, 8));
        rekomendasi.setOpaque(false);
        rekomendasi.add(infoBudget, BorderLayout.NORTH);
        rekomendasi.add(Tema.gulir(Tema.tabel(modelRekomendasi, 2)), BorderLayout.CENTER);

        JButton tambah = Tema.tombolPrimer("Tambah tempat");
        tambah.addActionListener(e -> {
            if (jalankan(() -> sistem.tambahTempatHiburan(inputNama.getText(), inputLokasi.getText(),
                    (String) pilihKota.getSelectedItem(), RupiahFormatter.parse(inputMin.getText(), "Biaya minimum"),
                    RupiahFormatter.parse(inputMaks.getText(), "Biaya maksimum"), (String) pilihTier.getSelectedItem()))) {
                inputNama.setText("");
                inputLokasi.setText("");
                inputMin.setText("");
                inputMaks.setText("");
            }
        });
        JButton hapus = Tema.tombolBahaya("Hapus terpilih");
        hapus.addActionListener(e -> {
            int baris = tabelSemua.getSelectedRow();
            if (baris < 0 || baris >= tampil.size()) {
                tampilkanError("Pilih tempat di tabel data tempat hiburan terlebih dahulu.");
                return;
            }
            TempatHiburan t = tampil.get(baris);
            if (konfirmasi("Hapus " + t.getNamaTempat() + " (" + t.getKota() + ")?", "Konfirmasi hapus")) {
                jalankan(() -> sistem.hapusTempatHiburan(t.getNamaTempat(), t.getKota()));
            }
        });
        Formulir form = new Formulir()
                .tambah("Nama tempat", inputNama)
                .tambah("Lokasi", inputLokasi)
                .tambah("Kota", pilihKota)
                .tambah("Biaya minimum (Rp)", inputMin, "Isi 0 untuk tempat gratis")
                .tambah("Biaya maksimum (Rp)", inputMaks)
                .tambah("Tier", pilihTier);
        form.tombol(tambah, hapus);

        JPanel kelola = new JPanel(new BorderLayout(16, 0));
        kelola.setOpaque(false);
        kelola.add(form, BorderLayout.WEST);
        kelola.add(Tema.gulir(tabelSemua), BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 16));
        grid.setOpaque(false);
        grid.add(bagian("Rekomendasi untuk Anda", rekomendasi));
        grid.add(bagian("Data tempat hiburan (semua kota)", kelola));
        getIsi().add(grid, BorderLayout.CENTER);
    }

    private static String biaya(TempatHiburan t) {
        if (t.getBiayaMaksimum() == 0) {
            return "Gratis";
        }
        return RupiahFormatter.format(t.getBiayaMinimum()) + " – " + RupiahFormatter.format(t.getBiayaMaksimum());
    }

    @Override
    public void muatUlang() {
        String kotaMahasiswa = sistem.getMahasiswa().getKota().getNamaKota();
        long sisaHiburan = sistem.getSisaBudgetHiburan();
        List<TempatHiburan> rekomendasi = sistem.getRekomendasiHiburan();
        String status;
        if (sistem.getTempatHiburanKotaMahasiswa().isEmpty()) {
            status = "Belum ada data tempat hiburan untuk " + kotaMahasiswa + ". Tambahkan lewat form di bawah.";
        } else if (rekomendasi.isEmpty()) {
            status = "Budget hiburan habis. Tahan dulu sampai kiriman berikutnya.";
        } else {
            status = rekomendasi.size() + " tempat terjangkau, diurutkan dari yang termurah.";
        }
        infoBudget.setText("<html>Kota: <b>" + kotaMahasiswa + "</b> &nbsp;•&nbsp; Sisa budget hiburan: <b>"
                + RupiahFormatter.format(sisaHiburan) + "</b> &nbsp;•&nbsp; Tier anggaran: <b>"
                + sistem.getTierSaatIni().getNamaTier() + "</b><br><span style='color:#64748b'>" + status
                + "</span></html>");
        modelRekomendasi.setRowCount(0);
        for (TempatHiburan t : rekomendasi) {
            modelRekomendasi.addRow(new Object[] {t.getNamaTempat(), t.getLokasi(), biaya(t), t.getTier()});
        }

        Object kotaDipilih = pilihKota.getSelectedItem();
        pilihKota.removeAllItems();
        for (Kota k : sistem.getDaftarKota()) {
            pilihKota.addItem(k.getNamaKota());
        }
        pilihKota.setSelectedItem(kotaDipilih != null ? kotaDipilih : kotaMahasiswa);

        tampil.clear();
        tampil.addAll(sistem.getDaftarTempatHiburan());
        modelSemua.setRowCount(0);
        for (TempatHiburan t : tampil) {
            modelSemua.addRow(new Object[] {t.getNamaTempat(), t.getLokasi(), t.getKota(), biaya(t), t.getTier()});
        }
    }
}
