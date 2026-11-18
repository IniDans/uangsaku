package view;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.Kiriman;
import model.PengeluaranHarian;
import model.Transaksi;
import service.SistemKeuangan;
import util.DateHelper;
import util.RupiahFormatter;

/** Mencatat pengeluaran harian. Setiap transaksi memotong saldo dan sisa anggaran. */
public class TransaksiPanel extends BasePanel {

    private final JTextField inputTanggal = new JTextField(10);
    private final JComboBox<String> pilihKategori = new JComboBox<>(PengeluaranHarian.DAFTAR_KATEGORI.toArray(new String[0]));
    private final JTextField inputKeterangan = new JTextField(16);
    private final JTextField inputNominal = new JTextField(12);
    private final JLabel ringkasan = new JLabel();
    private final DefaultTableModel model = Tema.modelTabel("Tanggal", "Kategori", "Keterangan", "Nominal", "Periode");
    private final JTable tabel = Tema.tabel(model, 3);
    private final List<Transaksi> tampil = new ArrayList<>();

    public TransaksiPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Transaksi Harian",
                "Catat setiap pengeluaran harian. Makan, jajan, transportasi, dan lainnya masuk pos makan & kebutuhan.");

        JButton tambah = Tema.tombolPrimer("Tambah transaksi");
        tambah.addActionListener(e -> tambahTransaksi());
        JButton hapus = Tema.tombolBahaya("Hapus terpilih");
        hapus.addActionListener(e -> hapusTransaksi());

        Formulir form = new Formulir()
                .tambah("Tanggal", inputTanggal, "Format dd/MM/yyyy, tidak boleh di masa depan")
                .tambah("Kategori", pilihKategori)
                .tambah("Keterangan", inputKeterangan)
                .tambah("Nominal (Rp)", inputNominal);
        form.tombol(tambah, hapus);
        inputTanggal.setText(DateHelper.formatInput(sistem.hariIni()));

        ringkasan.setFont(Tema.FONT_BIASA);
        ringkasan.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        JPanel kiri = new JPanel(new BorderLayout(0, 12));
        kiri.setOpaque(false);
        kiri.add(bagian("Transaksi baru", form), BorderLayout.NORTH);
        kiri.add(bagian("Ringkasan", ringkasan), BorderLayout.CENTER);
        JPanel penahan = new JPanel(new BorderLayout());
        penahan.setOpaque(false);
        penahan.add(kiri, BorderLayout.NORTH);

        getIsi().add(penahan, BorderLayout.WEST);
        getIsi().add(bagian("Riwayat transaksi (terbaru di atas)", Tema.gulir(tabel)), BorderLayout.CENTER);
    }

    private void tambahTransaksi() {
        final LocalDate tanggal;
        final long nominal;
        try {
            tanggal = DateHelper.parseInput(inputTanggal.getText(), "Tanggal");
            nominal = RupiahFormatter.parse(inputNominal.getText(), "Nominal");
        } catch (RuntimeException ex) {
            tampilkanError(ex.getMessage());
            return;
        }
        if (sistem.melebihiSisaAnggaran(nominal, tanggal)) {
            String pesan = "Transaksi " + RupiahFormatter.format(nominal) + " melebihi sisa anggaran "
                    + RupiahFormatter.format(sistem.getSisaAnggaran()) + ".\nTetap catat transaksi ini?";
            if (!konfirmasi(pesan, "Melebihi sisa anggaran")) {
                return;
            }
        }
        String kategori = (String) pilihKategori.getSelectedItem();
        String keterangan = inputKeterangan.getText();
        if (jalankan(() -> sistem.tambahTransaksi(tanggal, kategori, keterangan, nominal))) {
            inputKeterangan.setText("");
            inputNominal.setText("");
            inputKeterangan.requestFocusInWindow();
        }
    }

    private void hapusTransaksi() {
        int baris = tabel.getSelectedRow();
        if (baris < 0 || baris >= tampil.size()) {
            tampilkanError("Pilih transaksi di tabel terlebih dahulu.");
            return;
        }
        Transaksi t = tampil.get(baris);
        String pesan = "Hapus transaksi \"" + t.getKeterangan() + "\" " + RupiahFormatter.format(t.getNominal())
                + "?\nSaldo akan dikembalikan.";
        if (konfirmasi(pesan, "Konfirmasi hapus")) {
            jalankan(() -> sistem.hapusTransaksi(t.getIdTransaksi()));
        }
    }

    @Override
    public void muatUlang() {
        Kiriman aktif = sistem.getMahasiswa().getKirimanAktif();
        tampil.clear();
        tampil.addAll(sistem.getDaftarTransaksi());
        model.setRowCount(0);
        for (Transaksi t : tampil) {
            boolean periodeIni = aktif != null && aktif.isDalamPeriode(t.getTanggal());
            model.addRow(new Object[] {
                DateHelper.formatIndonesia(t.getTanggal()), t.getKategori(), t.getKeterangan(),
                RupiahFormatter.format(t.getNominal()), periodeIni ? "Aktif" : "Lalu"
            });
        }
        long sisa = sistem.getSisaAnggaran();
        long hariIni = sistem.getTotalTransaksiPadaTanggal(sistem.hariIni());
        long budgetHarian = sistem.getAnggaran().getBudgetHarian();
        ringkasan.setText("<html><table cellpadding='2'>"
                + "<tr><td>Saldo dompet</td><td align='right'><b>" + RupiahFormatter.format(sistem.getSaldo()) + "</b></td></tr>"
                + "<tr><td>Sisa anggaran</td><td align='right' style='color:" + (sisa < 0 ? "#b91c1c" : "#0f172a") + "'><b>"
                + RupiahFormatter.format(sisa) + "</b></td></tr>"
                + "<tr><td>Budget harian</td><td align='right'>" + RupiahFormatter.format(budgetHarian) + "</td></tr>"
                + "<tr><td>Terpakai hari ini</td><td align='right' style='color:"
                + (hariIni > budgetHarian ? "#b91c1c" : "#0f172a") + "'>" + RupiahFormatter.format(hariIni) + "</td></tr>"
                + "<tr><td>Sisa budget hiburan</td><td align='right'>"
                + RupiahFormatter.format(sistem.getSisaBudgetHiburan()) + "</td></tr>"
                + "</table></html>");
    }
}
