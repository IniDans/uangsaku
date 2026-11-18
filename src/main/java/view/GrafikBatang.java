package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;
import util.RupiahFormatter;

/** Grafik batang sederhana (Graphics2D) untuk total pengeluaran per kategori. */
public class GrafikBatang extends JComponent {

    private static final Color[] WARNA = {
        new Color(0x0F766E), new Color(0xD97706), new Color(0x2563EB), new Color(0xDB2777), new Color(0x64748B)
    };

    private Map<String, Long> data = new LinkedHashMap<>();

    public GrafikBatang() {
        setPreferredSize(new Dimension(420, 240));
        setOpaque(false);
    }

    public void setData(Map<String, Long> data) {
        this.data = new LinkedHashMap<>(data);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int lebar = getWidth();
            int tinggi = getHeight();
            int atas = 24;
            int bawah = 30;
            int areaTinggi = Math.max(10, tinggi - atas - bawah);
            int dasar = atas + areaTinggi;

            g2.setColor(Tema.GARIS);
            g2.drawLine(0, dasar, lebar, dasar);

            long maks = 0;
            for (long v : data.values()) {
                maks = Math.max(maks, v);
            }
            if (data.isEmpty() || maks == 0) {
                g2.setFont(Tema.FONT_BIASA);
                g2.setColor(Tema.TEKS_SEKUNDER);
                String kosong = "Belum ada transaksi pada periode ini.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(kosong, (lebar - fm.stringWidth(kosong)) / 2, tinggi / 2);
                return;
            }

            int jumlah = data.size();
            int slot = lebar / jumlah;
            int lebarBatang = Math.max(12, Math.min(64, slot - 24));
            int i = 0;
            for (Map.Entry<String, Long> e : data.entrySet()) {
                int tinggiBatang = (int) Math.round((double) e.getValue() / maks * areaTinggi);
                int x = i * slot + (slot - lebarBatang) / 2;
                g2.setColor(WARNA[i % WARNA.length]);
                g2.fillRoundRect(x, dasar - tinggiBatang, lebarBatang, tinggiBatang, 6, 6);

                g2.setFont(Tema.FONT_KECIL);
                FontMetrics fm = g2.getFontMetrics();
                String nilai = RupiahFormatter.format(e.getValue());
                g2.setColor(Tema.TEKS);
                g2.drawString(nilai, i * slot + (slot - fm.stringWidth(nilai)) / 2, dasar - tinggiBatang - 6);
                g2.setColor(Tema.TEKS_SEKUNDER);
                g2.drawString(e.getKey(), i * slot + (slot - fm.stringWidth(e.getKey())) / 2, dasar + 18);
                i++;
            }
        } finally {
            g2.dispose();
        }
    }
}
