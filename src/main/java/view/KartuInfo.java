package view;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Kartu ringkasan berisi judul, angka utama, dan keterangan. */
public class KartuInfo extends JPanel {

    private final JLabel nilai = new JLabel("-");
    private final JLabel catatan = new JLabel(" ");

    public KartuInfo(String judul, Color aksen) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Tema.KARTU);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 4, 1, 1, aksen),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel labelJudul = Tema.keterangan(judul);
        nilai.setFont(Tema.FONT_ANGKA);
        nilai.setForeground(Tema.TEKS);
        catatan.setFont(Tema.FONT_KECIL);
        catatan.setForeground(Tema.TEKS_SEKUNDER);
        add(labelJudul);
        add(nilai);
        add(catatan);
    }

    public void setNilai(String teks, String keterangan) {
        setNilai(teks, keterangan, Tema.TEKS);
    }

    public void setNilai(String teks, String keterangan, Color warnaNilai) {
        nilai.setText(teks);
        nilai.setForeground(warnaNilai);
        catatan.setText(keterangan == null || keterangan.isEmpty() ? " " : keterangan);
    }
}
