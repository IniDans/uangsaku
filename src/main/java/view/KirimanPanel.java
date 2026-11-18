package view;

import java.awt.BorderLayout;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import model.Kiriman;
import service.SistemKeuangan;
import util.DateHelper;
import util.RupiahFormatter;

/** Mencatat uang kiriman orang tua. Kiriman dengan tanggal terima terbaru menjadi periode aktif. */
public class KirimanPanel extends BasePanel {

    private final JTextField inputNominal = new JTextField(14);
    private final JTextField inputTerima = new JTextField(10);
    private final JTextField inputBerikutnya = new JTextField(10);
    private final JLabel ringkasan = Tema.keterangan(" ");
    private final DefaultTableModel model = Tema.modelTabel(
            "Tanggal terima", "Kiriman berikutnya", "Jumlah hari", "Nominal", "Status");

    public KirimanPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Uang Kiriman",
                "Catat uang dari orang tua. Kiriman menambah saldo dan menentukan periode anggaran.");

        Formulir form = new Formulir()
                .tambah("Nominal kiriman (Rp)", inputNominal, "Contoh: 2000000 atau 2.000.000")
                .tambah("Tanggal terima", inputTerima, "Format dd/MM/yyyy")
                .tambah("Kiriman berikutnya", inputBerikutnya, "Kosongkan = otomatis 1 bulan setelah tanggal terima");
        form.tombol(tombolTambah(), tombolUbah(), tombolBersihkan());

        JPanel kiri = new JPanel(new BorderLayout(0, 10));
        kiri.setOpaque(false);
        kiri.add(bagian("Input kiriman", form), BorderLayout.NORTH);
        kiri.add(ringkasan, BorderLayout.CENTER);
        JPanel penahan = new JPanel(new BorderLayout());
        penahan.setOpaque(false);
        penahan.add(kiri, BorderLayout.NORTH);

        JTable tabel = Tema.tabel(model, 2, 3);
        getIsi().add(penahan, BorderLayout.WEST);
        getIsi().add(bagian("Riwayat kiriman", Tema.gulir(tabel)), BorderLayout.CENTER);
    }

    private JButton tombolTambah() {
        JButton b = Tema.tombolPrimer("Tambah kiriman baru");
        b.addActionListener(e -> {
            boolean ok = jalankan(() -> sistem.tambahKiriman(
                    RupiahFormatter.parse(inputNominal.getText(), "Nominal kiriman"),
                    DateHelper.parseInput(inputTerima.getText(), "Tanggal terima"),
                    tanggalOpsional(inputBerikutnya.getText())));
            if (ok) {
                isiForm(sistem.getMahasiswa().getKirimanAktif());
                info("Kiriman dicatat. Saldo dan anggaran sudah diperbarui.");
            }
        });
        return b;
    }

    private JButton tombolUbah() {
        JButton b = Tema.tombolSekunder("Simpan ke kiriman aktif");
        b.setToolTipText("Mengoreksi kiriman aktif. Saldo hanya disesuaikan sebesar selisih nominal.");
        b.addActionListener(e -> {
            boolean ok = jalankan(() -> sistem.ubahKirimanAktif(
                    RupiahFormatter.parse(inputNominal.getText(), "Nominal kiriman"),
                    DateHelper.parseInput(inputTerima.getText(), "Tanggal terima"),
                    tanggalOpsional(inputBerikutnya.getText())));
            if (ok) {
                isiForm(sistem.getMahasiswa().getKirimanAktif());
                info("Kiriman aktif diperbarui.");
            }
        });
        return b;
    }

    private JButton tombolBersihkan() {
        JButton b = Tema.tombolSekunder("Bersihkan");
        b.addActionListener(e -> {
            inputNominal.setText("");
            inputTerima.setText(DateHelper.formatInput(sistem.hariIni()));
            inputBerikutnya.setText("");
            inputNominal.requestFocusInWindow();
        });
        return b;
    }

    private static LocalDate tanggalOpsional(String teks) {
        return teks == null || teks.isBlank() ? null : DateHelper.parseInput(teks, "Tanggal kiriman berikutnya");
    }

    /** Mengisi form dengan kiriman aktif (atau kosong jika belum ada). */
    private void isiForm(Kiriman aktif) {
        if (aktif == null) {
            inputNominal.setText("");
            inputTerima.setText(DateHelper.formatInput(sistem.hariIni()));
            inputBerikutnya.setText("");
        } else {
            inputNominal.setText(RupiahFormatter.formatAngka(aktif.getNominal()));
            inputTerima.setText(DateHelper.formatInput(aktif.getTanggalTerima()));
            inputBerikutnya.setText(DateHelper.formatInput(aktif.getTanggalBerikutnya()));
        }
    }

    @Override
    public void muatUlang() {
        Kiriman aktif = sistem.getMahasiswa().getKirimanAktif();
        model.setRowCount(0);
        for (Kiriman k : sistem.getDaftarKiriman()) {
            model.addRow(new Object[] {
                DateHelper.formatIndonesia(k.getTanggalTerima()),
                DateHelper.formatIndonesia(k.getTanggalBerikutnya()),
                k.hitungJumlahHari(),
                RupiahFormatter.format(k.getNominal()),
                k == aktif ? "Aktif" : "Selesai"
            });
        }
        if (inputTerima.getText().isBlank()) {
            isiForm(aktif);
        }
        if (aktif == null) {
            ringkasan.setText("Belum ada kiriman.");
        } else {
            ringkasan.setText("<html>Kiriman aktif: <b>" + RupiahFormatter.format(aktif.getNominal()) + "</b><br>"
                    + "Total semua kiriman: " + RupiahFormatter.format(sistem.getTotalKiriman()) + "<br>"
                    + "Saldo dompet: " + RupiahFormatter.format(sistem.getSaldo()) + "</html>");
        }
    }
}
