package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.time.DateTimeException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import service.SistemKeuangan;

/**
 * INHERITANCE + ABSTRACTION untuk GUI: semua halaman turunan BasePanel.
 * Panel tidak menghitung apa pun; semua proses lewat SistemKeuangan.
 */
public abstract class BasePanel extends JPanel {

    /** Aksi pengguna yang boleh melempar error validasi. */
    @FunctionalInterface
    protected interface Aksi {
        void jalankan();
    }

    protected final SistemKeuangan sistem;
    private final Runnable penyegar;
    private final JPanel isi = new JPanel();

    protected BasePanel(SistemKeuangan sistem, Runnable penyegar, String judul, String deskripsi) {
        super(new BorderLayout());
        this.sistem = sistem;
        this.penyegar = penyegar;
        setBackground(Tema.LATAR);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JPanel kepala = new JPanel();
        kepala.setOpaque(false);
        kepala.setLayout(new BoxLayout(kepala, BoxLayout.Y_AXIS));
        kepala.add(Tema.judul(judul));
        kepala.add(Tema.keterangan(deskripsi));
        kepala.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        add(kepala, BorderLayout.NORTH);

        isi.setOpaque(false);
        isi.setLayout(new BorderLayout(16, 16));
        add(isi, BorderLayout.CENTER);
    }

    /** Area konten di bawah judul (BorderLayout). */
    protected JPanel getIsi() {
        return isi;
    }

    /** Membaca ulang data dari SistemKeuangan ke komponen. Dipanggil setiap data berubah. */
    public abstract void muatUlang();

    /**
     * Menjalankan aksi; error validasi ditampilkan sebagai dialog.
     * Semua halaman disegarkan setelahnya (juga saat gagal, karena peringatan bisa tercatat).
     * @return true jika aksi berhasil
     */
    protected boolean jalankan(Aksi aksi) {
        try {
            aksi.jalankan();
            return true;
        } catch (IllegalArgumentException | DateTimeException e) {
            tampilkanError(e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Gagal menyimpan", JOptionPane.WARNING_MESSAGE);
            return false;
        } finally {
            penyegar.run();
        }
    }

    protected void tampilkanError(String pesan) {
        JOptionPane.showMessageDialog(this, pesan, "Input tidak valid", JOptionPane.ERROR_MESSAGE);
    }

    protected void info(String pesan) {
        JOptionPane.showMessageDialog(this, pesan, "Berhasil", JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean konfirmasi(String pesan, String judul) {
        return JOptionPane.showConfirmDialog(this, pesan, judul, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Kartu berjudul untuk mengelompokkan isi halaman. */
    protected static JPanel bagian(String judul, Component konten) {
        JPanel kartu = Tema.kartu();
        kartu.setLayout(new BorderLayout(0, 10));
        kartu.add(Tema.subjudul(judul), BorderLayout.NORTH);
        kartu.add(konten, BorderLayout.CENTER);
        return kartu;
    }

    protected static void rataKiri(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}
