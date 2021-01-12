package my.cool.projects;
import org.junit.*;

import java.io.*;
import java.nio.file.Files;

public class PlayChessTest {

    @Test
    public void kingTakingAProtectedPiece() throws IOException {
        File file = new File("C:\\Users\\meirj\\MyGit\\Chess\\src\\test\\java\\my\\cool\\projects\\testFile.txt");
        PrintWriter writer = new PrintWriter(file);
        writer.print("");
        writer.close();
        String[] moves = {"h4", "g5", "hxg5", "Nh6", "g6", "e6", "g7", "Be7", "gxh8=Q", "Bf8", "Qxf8", "Kxf8", "Rxh6", "Kg7", "d3", "Kxh6"};
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (String s : moves) {
                bw.write(s);
                bw.write(System.lineSeparator()); // new line
            }
        }
        String[] array = {file.getAbsolutePath()};
        PlayChess.main(array);
    }
}