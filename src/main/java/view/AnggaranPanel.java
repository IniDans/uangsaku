package view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import model.Anggaran;
import model.Peringatan;
import model.TierAnggaran;
import service.SistemKeuangan;
import util.RupiahFormatter;

/** Rincian perhitungan anggaran, tier, dan saran hemat. Halaman ini hanya menampilkan. */
public class AnggaranPanel extends BasePanel {

    private final DefaultTableModel modelRincian = Tema.modelTabel("Komponen", "Nilai", "Terpakai", "Sisa");
    private final DefaultTableModel modelTier = Tema.modelTabel("Tier", "Dana tersedia minimum", "Dana tersedia maksimum", "");
    private final JLabel infoTier = new JLabel();
    private final JLabel saran = new JLabel();

    public AnggaranPanel(SistemKeuangan sistem, Runnable penyegar) {
        super(sistem, penyegar, "Anggaran",
                "Dihitung ulang otomatis setiap kiriman, pengeluaran tetap, tabungan, atau pengaturan berubah.");

        JTable tabelRincian = Tema.tabel(modelRincian, 1, 2, 3);
        JTable tabelTier = Tema.tabel(modelTier, 1, 2);
        infoTier.setFont(Tema.FONT_BIASA);
        saran.setFont(Tema.FONT_BIASA);

        JPanel tier = new JPanel(new BorderLayout(0, 8));
        tier.setOpaque(false);
        tier.add(infoTier, BorderLayout.NORTH);
        tier.add(Tema.gulir(tabelTier), BorderLayout.CENTER);
        tier.add(saran, BorderLayout.SOUTH);

        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 0));
        grid.setOpaque(false);
        grid.add(bagian("Rincian anggaran periode ini", Tema.gulir(tabelRincian)));
        grid.add(bagian("Tier anggaran", tier));
        getIsi().add(grid, BorderLayout.CENTER);
    }

    @Override
    public void muatUlang() {
        Anggaran a = sistem.getAnggaran();
        String persenMakan = Tema.persen(sistem.getConfig().getPersenMakan());
        String persenHiburan = Tema.persen(sistem.getConfig().getPersenHiburan());
        modelRincian.setRowCount(0);
        modelRincian.addRow(baris("Uang kiriman", sistem.getMahasiswa().getNominalKiriman()));
        modelRincian.addRow(baris("− Pengeluaran tetap", sistem.getTotalPengeluaranTetap()));
        modelRincian.addRow(baris("− Target tabungan", sistem.getTargetTabungan()));
        modelRincian.addRow(new Object[] {
            "= Dana tersedia", RupiahFormatter.format(a.getDanaTersedia()),
            RupiahFormatter.format(sistem.getTotalTransaksiPeriode()), RupiahFormatter.format(sistem.getSisaAnggaran())
        });
        modelRincian.addRow(new Object[] {"Jumlah hari periode", a.getJumlahHari() + " hari", "", ""});
        modelRincian.addRow(new Object[] {
            "Budget harian (dana ÷ hari)", RupiahFormatter.format(a.getBudgetHarian()),
            RupiahFormatter.format(sistem.getTotalTransaksiPadaTanggal(sistem.hariIni())) + " hari ini", ""
        });
        modelRincian.addRow(new Object[] {
            "Budget makan & kebutuhan (" + persenMakan + ")", RupiahFormatter.format(a.getBudgetMakan()),
            RupiahFormatter.format(sistem.getTerpakaiMakan()), RupiahFormatter.format(sistem.getSisaBudgetMakan())
        });
        modelRincian.addRow(new Object[] {
            "Budget hiburan (" + persenHiburan + ")", RupiahFormatter.format(a.getBudgetHiburan()),
            RupiahFormatter.format(sistem.getTerpakaiHiburan()), RupiahFormatter.format(sistem.getSisaBudgetHiburan())
        });
        modelRincian.addRow(baris("Saldo dompet", sistem.getSaldo()));

        TierAnggaran sekarang = sistem.getTierSaatIni();
        String namaKota = sistem.getMahasiswa().getKota().getNamaKota();
        String faktor = String.format(Locale.forLanguageTag("id-ID"), "%.2f", sistem.getFaktorKota());
        infoTier.setText("<html>Tier Anda: <b>" + sekarang.getNamaTier() + "</b><br>"
                + "<span style='color:#64748b'>Batas tier kota acuan " + sistem.getConfig().getKotaAcuan()
                + " dikali faktor " + namaKota + " = " + faktor + "</span></html>");
        modelTier.setRowCount(0);
        for (TierAnggaran t : sistem.getDaftarTierEfektif()) {
            modelTier.addRow(new Object[] {
                t.getNamaTier(),
                RupiahFormatter.format(t.getBatasMinimum()),
                t.getBatasMaksimum() == Long.MAX_VALUE ? "tanpa batas" : RupiahFormatter.format(t.getBatasMaksimum()),
                t.getNamaTier().equals(sekarang.getNamaTier()) ? "◀ tier Anda" : ""
            });
        }

        if (sistem.isAnggaranMenipis()) {
            StringBuilder sb = new StringBuilder("<html><b style='color:#b45309'>Anggaran menipis. Saran:</b><ul>");
            for (String s : Peringatan.SARAN_HEMAT) {
                sb.append("<li>").append(s).append("</li>");
            }
            saran.setText(sb.append("</ul></html>").toString());
        } else {
            saran.setText("<html><span style='color:#15803d'>Anggaran masih aman.</span></html>");
        }
    }

    private static Object[] baris(String label, long nilai) {
        return new Object[] {label, RupiahFormatter.format(nilai), "", ""};
    }
}
