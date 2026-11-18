import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import repository.FileRepository;
import service.SistemKeuangan;
import util.AppConfig;
import view.DashboardFrame;

/**
 * Titik masuk aplikasi. Jalankan dari folder proyek (run.bat / run.sh)
 * agar folder data/ dan data/seed/ ditemukan.
 */
public class Main {

    public static void main(String[] args) {
        Path folderData = Path.of("data");
        Path folderSeed = folderData.resolve("seed");

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Tema bawaan tidak bisa dipakai: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                error.printStackTrace();
                JOptionPane.showMessageDialog(null, "Terjadi kesalahan tak terduga:\n" + error,
                        "Kesalahan", JOptionPane.ERROR_MESSAGE);
            });
            try {
                AppConfig config = AppConfig.muat(folderData.resolve("config.properties"));
                SistemKeuangan sistem = new SistemKeuangan(new FileRepository(folderData, folderSeed), config);
                sistem.muatData();
                new DashboardFrame(sistem).setVisible(true);
            } catch (RuntimeException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Aplikasi gagal dibuka:\n" + e.getMessage(),
                        "Kesalahan", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
