package view;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import model.Peringatan;
import service.SistemKeuangan;
import util.DateHelper;

/** Riwayat peringatan yang pernah muncul. Satu peringatan per jenis per tanggal. */
public class PeringatanPanel extends BasePanel {

    private final DefaultTableModel model = Tema.modelTabel("Tanggal", "Jenis", "Pesan");
    private final JLabel ringkasan = new JLabel();

    public PeringatanPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Riwayat Peringatan",
                "Muncul saat budget harian terlampaui, sisa menipis, pengeluaran tetap terlalu besar, "
                        + "atau target tabungan tidak bisa dipenuhi.");
        JTable tabel = Tema.tabel(model);
        tabel.getColumnModel().getColumn(0).setPreferredWidth(130);
        tabel.getColumnModel().getColumn(0).setMaxWidth(170);
        tabel.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabel.getColumnModel().getColumn(1).setMaxWidth(280);
        tabel.getColumnModel().getColumn(2).setPreferredWidth(600);
        ringkasan.setFont(Tema.FONT_BIASA);
        getIsi().add(ringkasan, BorderLayout.NORTH);
        getIsi().add(bagian("Semua peringatan (terbaru di atas)", Tema.gulir(tabel)), BorderLayout.CENTER);
    }

    @Override
    public void muatUlang() {
        model.setRowCount(0);
        int[] jumlah = new int[Peringatan.Jenis.values().length];
        for (Peringatan p : sistem.getDaftarPeringatan()) {
            model.addRow(new Object[] {DateHelper.formatIndonesia(p.getTanggal()), p.getJenis().getLabel(), p.getPesan()});
            jumlah[p.getJenis().ordinal()]++;
        }
        StringBuilder sb = new StringBuilder("<html>");
        for (Peringatan.Jenis j : Peringatan.Jenis.values()) {
            sb.append(j.getLabel()).append(": <b>").append(jumlah[j.ordinal()]).append("</b> &nbsp;&nbsp; ");
        }
        ringkasan.setText(sb.append("</html>").toString());
    }
}
