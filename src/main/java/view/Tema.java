package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/** Warna, font, dan pembuat komponen yang dipakai semua panel agar tampilan seragam. */
public final class Tema {

    public static final Color SIDEBAR = new Color(0x1E293B);
    public static final Color SIDEBAR_HOVER = new Color(0x334155);
    public static final Color SIDEBAR_AKTIF = new Color(0x0F766E);
    public static final Color SIDEBAR_TEKS = new Color(0xE2E8F0);
    public static final Color LATAR = new Color(0xF1F5F9);
    public static final Color KARTU = Color.WHITE;
    public static final Color GARIS = new Color(0xE2E8F0);
    public static final Color TEKS = new Color(0x0F172A);
    public static final Color TEKS_SEKUNDER = new Color(0x64748B);
    public static final Color PRIMER = new Color(0x0F766E);
    public static final Color SEKUNDER = new Color(0x475569);
    public static final Color BAHAYA = new Color(0xB91C1C);
    public static final Color WASPADA = new Color(0xB45309);
    public static final Color SUKSES = new Color(0x15803D);
    public static final Color LATAR_WASPADA = new Color(0xFEF3C7);

    public static final String FONT = Font.SANS_SERIF;
    public static final Font FONT_JUDUL = new Font(FONT, Font.BOLD, 22);
    public static final Font FONT_SUBJUDUL = new Font(FONT, Font.BOLD, 15);
    public static final Font FONT_BIASA = new Font(FONT, Font.PLAIN, 13);
    public static final Font FONT_KECIL = new Font(FONT, Font.PLAIN, 12);
    public static final Font FONT_ANGKA = new Font(FONT, Font.BOLD, 19);

    private Tema() {
    }

    public static JLabel judul(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(FONT_JUDUL);
        label.setForeground(TEKS);
        return label;
    }

    public static JLabel subjudul(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(FONT_SUBJUDUL);
        label.setForeground(TEKS);
        return label;
    }

    public static JLabel keterangan(String teks) {
        JLabel label = new JLabel(teks);
        label.setFont(FONT_KECIL);
        label.setForeground(TEKS_SEKUNDER);
        return label;
    }

    public static JButton tombol(String teks, Color warna) {
        JButton tombol = new JButton(teks);
        tombol.setFont(new Font(FONT, Font.BOLD, 12));
        tombol.setBackground(warna);
        tombol.setForeground(Color.WHITE);
        tombol.setOpaque(true);
        tombol.setBorderPainted(false);
        tombol.setFocusPainted(false);
        tombol.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tombol.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return tombol;
    }

    public static JButton tombolPrimer(String teks) {
        return tombol(teks, PRIMER);
    }

    public static JButton tombolSekunder(String teks) {
        return tombol(teks, SEKUNDER);
    }

    public static JButton tombolBahaya(String teks) {
        return tombol(teks, BAHAYA);
    }

    /** Panel putih dengan garis tepi tipis. */
    public static JPanel kartu() {
        JPanel panel = new JPanel();
        panel.setBackground(KARTU);
        panel.setBorder(borderKartu());
        return panel;
    }

    public static Border borderKartu() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GARIS),
                BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    /** Model tabel yang tidak bisa diedit langsung (perubahan hanya lewat form). */
    public static DefaultTableModel modelTabel(String... kolom) {
        return new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Tabel bergaya seragam.
     * @param kolomKanan indeks kolom yang rata kanan (angka/rupiah)
     */
    public static JTable tabel(DefaultTableModel model, int... kolomKanan) {
        JTable tabel = new JTable(model);
        tabel.setFont(FONT_BIASA);
        tabel.setRowHeight(28);
        tabel.setShowVerticalLines(false);
        tabel.setGridColor(GARIS);
        tabel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabel.setSelectionBackground(new Color(0xCCFBF1));
        tabel.setSelectionForeground(TEKS);
        tabel.setFillsViewportHeight(true);
        tabel.getTableHeader().setReorderingAllowed(false);
        JTableHeader header = tabel.getTableHeader();
        header.setFont(new Font(FONT, Font.BOLD, 12));
        header.setBackground(new Color(0xF8FAFC));
        header.setForeground(TEKS_SEKUNDER);
        DefaultTableCellRenderer kanan = new DefaultTableCellRenderer();
        kanan.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int k : kolomKanan) {
            tabel.getColumnModel().getColumn(k).setCellRenderer(kanan);
        }
        return tabel;
    }

    public static JScrollPane gulir(Component isi) {
        JScrollPane scroll = new JScrollPane(isi);
        scroll.setBorder(BorderFactory.createLineBorder(GARIS));
        scroll.getViewport().setBackground(KARTU);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /** Persentase untuk ditampilkan: 0.7 -> "70%". */
    public static String persen(double nilai) {
        double p = nilai * 100;
        if (Math.abs(p - Math.rint(p)) < 1e-9) {
            return Math.round(p) + "%";
        }
        return String.format(java.util.Locale.forLanguageTag("id-ID"), "%.1f%%", p);
    }
}
