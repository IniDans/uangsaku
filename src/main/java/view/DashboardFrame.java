package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import service.SistemKeuangan;

/** Jendela utama: sidebar navigasi gelap di kiri, halaman (CardLayout) di kanan. */
public class DashboardFrame extends JFrame {

    private final CardLayout kartu = new CardLayout();
    private final JPanel konten = new JPanel(kartu);
    private final Map<String, BasePanel> halaman = new LinkedHashMap<>();
    private final List<JButton> tombolMenu = new ArrayList<>();
    private BasePanel halamanAktif;

    public DashboardFrame(SistemKeuangan sistem) {
        super("Uang Saku Mahasiswa Rantau");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);

        Runnable segarkan = this::segarkanSemua;
        daftar("Dashboard", new DashboardPanel(sistem, segarkan));
        daftar("Uang Kiriman", new KirimanPanel(sistem, segarkan));
        daftar("Pengeluaran Tetap", new PengeluaranPanel(sistem, segarkan));
        daftar("Target Tabungan", new TabunganPanel(sistem, segarkan));
        daftar("Anggaran", new AnggaranPanel(sistem, segarkan));
        daftar("Transaksi Harian", new TransaksiPanel(sistem, segarkan));
        daftar("Rekomendasi Hiburan", new HiburanPanel(sistem, segarkan));
        daftar("Riwayat Peringatan", new PeringatanPanel(sistem, segarkan));
        daftar("Pengaturan", new PengaturanPanel(sistem, segarkan));

        konten.setBackground(Tema.LATAR);
        add(buatSidebar(), BorderLayout.WEST);
        add(konten, BorderLayout.CENTER);

        segarkanSemua();
        tampilkan("Dashboard");
    }

    private void daftar(String nama, BasePanel panel) {
        halaman.put(nama, panel);
        konten.add(panel, nama);
    }

    private JPanel buatSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Tema.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 0, 16, 0));

        JLabel logo = new JLabel("Uang Saku");
        logo.setFont(new Font(Tema.FONT, Font.BOLD, 20));
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        JLabel sub = new JLabel("Mahasiswa Rantau");
        sub.setFont(Tema.FONT_KECIL);
        sub.setForeground(new Color(0x94A3B8));
        sub.setBorder(BorderFactory.createEmptyBorder(2, 20, 20, 0));
        sidebar.add(logo);
        sidebar.add(sub);

        for (String nama : halaman.keySet()) {
            JButton b = new JButton(nama);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(new Font(Tema.FONT, Font.PLAIN, 14));
            b.setForeground(Tema.SIDEBAR_TEKS);
            b.setBackground(Tema.SIDEBAR);
            b.setOpaque(true);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setContentAreaFilled(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBorder(BorderFactory.createEmptyBorder(11, 20, 11, 12));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.addActionListener(e -> tampilkan(nama));
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!b.getText().equals(namaAktif())) {
                        b.setBackground(Tema.SIDEBAR_HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!b.getText().equals(namaAktif())) {
                        b.setBackground(Tema.SIDEBAR);
                    }
                }
            });
            tombolMenu.add(b);
            sidebar.add(b);
        }
        sidebar.add(Box.createVerticalGlue());
        JLabel catatan = new JLabel("<html>Data tersimpan di<br>folder data/ (CSV)</html>");
        catatan.setFont(Tema.FONT_KECIL);
        catatan.setForeground(new Color(0x94A3B8));
        catatan.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        sidebar.add(catatan);
        return sidebar;
    }

    private String namaAktif() {
        for (Map.Entry<String, BasePanel> e : halaman.entrySet()) {
            if (e.getValue() == halamanAktif) {
                return e.getKey();
            }
        }
        return "";
    }

    private void tampilkan(String nama) {
        halamanAktif = halaman.get(nama);
        halamanAktif.muatUlang();
        kartu.show(konten, nama);
        for (JButton b : tombolMenu) {
            boolean aktif = b.getText().equals(nama);
            b.setBackground(aktif ? Tema.SIDEBAR_AKTIF : Tema.SIDEBAR);
            b.setFont(new Font(Tema.FONT, aktif ? Font.BOLD : Font.PLAIN, 14));
        }
    }

    /** Dipanggil setelah setiap aksi agar semua halaman menampilkan angka terbaru. */
    private void segarkanSemua() {
        for (BasePanel p : halaman.values()) {
            p.muatUlang();
        }
    }
}
