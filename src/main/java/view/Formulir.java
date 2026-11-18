package view;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Pembantu tata letak form: label di kiri, input di kanan, tombol di baris bawah. */
public class Formulir extends JPanel {

    private int baris = 0;

    public Formulir() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    public Formulir tambah(String label, JComponent input) {
        return tambah(label, input, null);
    }

    /** @param petunjuk teks kecil di bawah input, boleh null */
    public Formulir tambah(String label, JComponent input, String petunjuk) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = baris;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, petunjuk == null ? 4 : 0, 12);
        JLabel teks = new JLabel(label);
        teks.setFont(Tema.FONT_BIASA);
        teks.setForeground(Tema.TEKS);
        add(teks, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, petunjuk == null ? 4 : 0, 0);
        input.setFont(Tema.FONT_BIASA);
        add(input, c);
        baris++;

        if (petunjuk != null) {
            GridBagConstraints p = new GridBagConstraints();
            p.gridx = 1;
            p.gridy = baris;
            p.anchor = GridBagConstraints.WEST;
            p.insets = new Insets(0, 0, 6, 0);
            add(Tema.keterangan(petunjuk), p);
            baris++;
        }
        return this;
    }

    public Formulir tombol(JComponent... tombol) {
        JPanel deret = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deret.setOpaque(false);
        for (int i = 0; i < tombol.length; i++) {
            if (i > 0) {
                deret.add(javax.swing.Box.createHorizontalStrut(8));
            }
            deret.add(tombol[i]);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = baris++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 0, 0, 0);
        add(deret, c);

        // Pengisi kosong yang menyerap sisa tinggi agar form menempel di atas, tidak melayang di tengah.
        GridBagConstraints isi = new GridBagConstraints();
        isi.gridx = 0;
        isi.gridy = baris++;
        isi.weighty = 1;
        add(javax.swing.Box.createVerticalGlue(), isi);
        return this;
    }
}
