package view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import model.Anggaran;
import model.Kiriman;
import model.Mahasiswa;
import model.Peringatan;
import service.SistemKeuangan;
import util.DateHelper;
import util.RupiahFormatter;

/** Halaman ringkasan: kartu angka utama, grafik kategori, dan peringatan terbaru. */
public class DashboardPanel extends BasePanel {

    private final JLabel sapaan = Tema.subjudul("");
    private final JLabel infoPeriode = Tema.keterangan("");
    private final JTextArea banner = new JTextArea();

    private final KartuInfo kartuSaldo = new KartuInfo("Saldo dompet", Tema.PRIMER);
    private final KartuInfo kartuDana = new KartuInfo("Dana tersedia", Tema.PRIMER);
    private final KartuInfo kartuSisa = new KartuInfo("Sisa anggaran", Tema.SUKSES);
    private final KartuInfo kartuHarian = new KartuInfo("Budget harian", Tema.SUKSES);
    private final KartuInfo kartuMakan = new KartuInfo("Sisa budget makan & kebutuhan", Tema.WASPADA);
    private final KartuInfo kartuHiburan = new KartuInfo("Sisa budget hiburan", Tema.WASPADA);
    private final KartuInfo kartuTetap = new KartuInfo("Pengeluaran tetap", Tema.SEKUNDER);
    private final KartuInfo kartuTabungan = new KartuInfo("Target tabungan", Tema.SEKUNDER);

    private final GrafikBatang grafik = new GrafikBatang();
    private final JTextArea daftarPeringatan = new JTextArea();

    public DashboardPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Dashboard", "Ringkasan keuangan periode kiriman yang sedang berjalan.");

        JPanel atas = new JPanel();
        atas.setOpaque(false);
        atas.setLayout(new BoxLayout(atas, BoxLayout.Y_AXIS));
        rataKiri(sapaan);
        rataKiri(infoPeriode);
        atas.add(sapaan);
        atas.add(infoPeriode);

        banner.setEditable(false);
        banner.setLineWrap(true);
        banner.setWrapStyleWord(true);
        banner.setFont(Tema.FONT_BIASA);
        banner.setBackground(Tema.LATAR_WASPADA);
        banner.setForeground(Tema.TEKS);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, Tema.WASPADA),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JPanel wadahBanner = new JPanel(new BorderLayout());
        wadahBanner.setOpaque(false);
        wadahBanner.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        wadahBanner.add(banner);
        rataKiri(wadahBanner);
        atas.add(wadahBanner);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        grid.add(kartuSaldo);
        grid.add(kartuDana);
        grid.add(kartuSisa);
        grid.add(kartuHarian);
        grid.add(kartuMakan);
        grid.add(kartuHiburan);
        grid.add(kartuTetap);
        grid.add(kartuTabungan);
        rataKiri(grid);
        atas.add(grid);
        getIsi().add(atas, BorderLayout.NORTH);

        daftarPeringatan.setEditable(false);
        daftarPeringatan.setLineWrap(true);
        daftarPeringatan.setWrapStyleWord(true);
        daftarPeringatan.setFont(Tema.FONT_BIASA);
        daftarPeringatan.setForeground(Tema.TEKS);

        JPanel bawah = new JPanel(new GridLayout(1, 2, 16, 0));
        bawah.setOpaque(false);
        bawah.add(bagian("Pengeluaran per kategori (periode ini)", grafik));
        bawah.add(bagian("Peringatan terbaru", Tema.gulir(daftarPeringatan)));
        getIsi().add(bawah, BorderLayout.CENTER);
    }

    @Override
    public void muatUlang() {
        Mahasiswa m = sistem.getMahasiswa();
        Anggaran a = sistem.getAnggaran();
        Kiriman aktif = m.getKirimanAktif();

        sapaan.setText("Halo, " + m.getNamaMahasiswa() + "!");
        String kota = m.getKota().getNamaKota() + " (faktor biaya "
                + String.format(Locale.forLanguageTag("id-ID"), "%.2f", sistem.getFaktorKota()) + ")";
        if (aktif == null) {
            infoPeriode.setText("Kota rantau: " + kota + "  •  Belum ada uang kiriman.");
        } else {
            long sisaHari = Math.max(0, ChronoUnit.DAYS.between(sistem.hariIni(), aktif.getTanggalBerikutnya()));
            infoPeriode.setText("Kota rantau: " + kota + "  •  Tier: " + sistem.getTierSaatIni().getNamaTier()
                    + "  •  Periode " + DateHelper.formatIndonesia(aktif.getTanggalTerima()) + " – "
                    + DateHelper.formatIndonesia(aktif.getTanggalBerikutnya()) + " (" + a.getJumlahHari()
                    + " hari, sisa " + sisaHari + " hari)");
        }

        if (aktif == null) {
            banner.setText("Belum ada uang kiriman. Buka menu Uang Kiriman untuk mencatat kiriman pertama.");
            banner.getParent().setVisible(true);
        } else if (sistem.isAnggaranMenipis()) {
            banner.setText("Sisa anggaran menipis (" + RupiahFormatter.format(sistem.getSisaAnggaran()) + "). Saran: "
                    + String.join(" ", Peringatan.SARAN_HEMAT));
            banner.getParent().setVisible(true);
        } else {
            banner.getParent().setVisible(false);
        }

        kartuSaldo.setNilai(RupiahFormatter.format(sistem.getSaldo()), "Uang yang benar-benar dipegang");
        kartuDana.setNilai(RupiahFormatter.format(a.getDanaTersedia()), "Kiriman − tetap − tabungan");
        long sisa = sistem.getSisaAnggaran();
        kartuSisa.setNilai(RupiahFormatter.format(sisa),
                sisa < 0 ? "Melebihi anggaran!" : "Dana tersedia − transaksi periode ini",
                sisa < 0 ? Tema.BAHAYA : (sistem.isAnggaranMenipis() ? Tema.WASPADA : Tema.TEKS));
        LocalDate hariIni = sistem.hariIni();
        long hariIniTotal = sistem.getTotalTransaksiPadaTanggal(hariIni);
        kartuHarian.setNilai(RupiahFormatter.format(a.getBudgetHarian()),
                "Terpakai hari ini " + RupiahFormatter.format(hariIniTotal),
                hariIniTotal > a.getBudgetHarian() ? Tema.BAHAYA : Tema.TEKS);
        kartuMakan.setNilai(RupiahFormatter.format(sistem.getSisaBudgetMakan()),
                "dari " + RupiahFormatter.format(a.getBudgetMakan()) + " ("
                        + Tema.persen(sistem.getConfig().getPersenMakan()) + ")");
        kartuHiburan.setNilai(RupiahFormatter.format(sistem.getSisaBudgetHiburan()),
                "dari " + RupiahFormatter.format(a.getBudgetHiburan()) + " ("
                        + Tema.persen(sistem.getConfig().getPersenHiburan()) + ")");
        kartuTetap.setNilai(RupiahFormatter.format(sistem.getTotalPengeluaranTetap()),
                m.isStatusKosDitanggung() ? "Kos ditanggung orang tua" : "Termasuk biaya kos");
        kartuTabungan.setNilai(RupiahFormatter.format(sistem.getTargetTabungan()), "Disisihkan di awal periode");

        grafik.setData(sistem.getTotalPerKategoriPeriode());

        List<Peringatan> peringatan = sistem.getDaftarPeringatan();
        if (peringatan.isEmpty()) {
            daftarPeringatan.setText("Belum ada peringatan. Pengeluaran masih terkendali.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(5, peringatan.size()); i++) {
                Peringatan p = peringatan.get(i);
                sb.append("• ").append(DateHelper.formatIndonesia(p.getTanggal())).append(" — ")
                        .append(p.getJenis().getLabel()).append('\n').append("   ").append(p.getPesan()).append("\n\n");
            }
            daftarPeringatan.setText(sb.toString().trim());
            daftarPeringatan.setCaretPosition(0);
        }
    }
}
