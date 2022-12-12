import javax.swing.*;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Test {
    public static void main(String[] args) throws Exception{
        viewHex("src/data.dat");
    }

    static void viewHex(String filename) {
        try (FileInputStream in = new FileInputStream(filename)) {
            int b;
            int count = 0;
            while ((b = in.read()) != -1) {
                String s = Integer.toHexString(b);
                if (s.length() ==1)
                    s = "0" + s;
                System.out.print(s + " ");
                count++;
                if (count % 16 == 0)
                    System.out.println();
                else if (count % 8 == 0)
                    System.out.print("| ");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
