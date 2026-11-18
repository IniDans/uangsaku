/**
 * Menjalankan seluruh pengujian. Exit code 0 jika semua lulus, 1 jika ada yang gagal.
 * Jalankan lewat test.bat / test.sh.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("== Uji logika anggaran (SistemKeuangan) ==");
        LogikaAnggaranTest.jalankan();
        System.out.println();
        System.out.println("Hasil: " + Uji.lulus + " assertion lulus, " + Uji.gagal + " gagal.");
        System.exit(Uji.gagal == 0 ? 0 : 1);
    }
}
