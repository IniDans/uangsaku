package view;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import service.SistemKeuangan;
import util.RupiahFormatter;

/** Menentukan target tabungan yang disisihkan di awal periode. */
public class TabunganPanel extends BasePanel {

    private final JTextField inputTarget = new JTextField(14);
    private final JLabel rincian = new JLabel();

    public TabunganPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Target Tabungan",
                "Uang yang disisihkan di awal periode. Tidak boleh melebihi kiriman dikurangi pengeluaran tetap.");

        JButton simpan = Tema.tombolPrimer("Simpan target");
        simpan.addActionListener(e -> {
            if (jalankan(() -> sistem.setTargetTabungan(RupiahFormatter.parse(inputTarget.getText(), "Target tabungan")))) {
                info("Target tabungan disimpan. Anggaran sudah dihitung ulang.");
            }
        });
        JButton nol = Tema.tombolSekunder("Tanpa tabungan");
        nol.addActionListener(e -> {
            inputTarget.setText("0");
            jalankan(() -> sistem.setTargetTabungan(0));
        });

        Formulir form = new Formulir()
                .tambah("Target tabungan (Rp)", inputTarget, "Isi 0 jika tidak menabung periode ini");
        form.tombol(simpan, nol);

        rincian.setFont(Tema.FONT_BIASA);
        rincian.setForeground(Tema.TEKS);

        JPanel atas = new JPanel(new BorderLayout(16, 0));
        atas.setOpaque(false);
        atas.add(bagian("Atur target", form), BorderLayout.WEST);
        atas.add(bagian("Perhitungan", rincian), BorderLayout.CENTER);
        getIsi().add(atas, BorderLayout.NORTH);
    }

    @Override
    public void muatUlang() {
        if (inputTarget.getText().isBlank() || !inputTarget.hasFocus()) {
            inputTarget.setText(RupiahFormatter.formatAngka(sistem.getTargetTabungan()));
        }
        long kiriman = sistem.getMahasiswa().getNominalKiriman();
        long tetap = sistem.getTotalPengeluaranTetap();
        long batas = sistem.getBatasMaksimalTabungan();
        String sisaSetelahTabungan = RupiahFormatter.format(sistem.getAnggaran().getDanaTersedia());
        rincian.setText("<html><table cellpadding='3'>"
                + baris("Uang kiriman aktif", RupiahFormatter.format(kiriman))
                + baris("Total pengeluaran tetap", "− " + RupiahFormatter.format(tetap))
                + baris("<b>Batas maksimal tabungan</b>", "<b>" + RupiahFormatter.format(batas) + "</b>")
                + baris("Target tabungan saat ini", RupiahFormatter.format(sistem.getTargetTabungan()))
                + baris("Dana tersedia setelah menabung", sisaSetelahTabungan)
                + "</table><br><span style='color:#64748b'>Rumus: Dana tersedia = Kiriman − Pengeluaran tetap − "
                + "Target tabungan</span></html>");
    }

    private static String baris(String label, String nilai) {
        return "<tr><td>" + label + "</td><td align='right'>" + nilai + "</td></tr>";
    }
}
