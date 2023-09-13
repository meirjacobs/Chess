package my.cool.projects;

import java.io.*;
import java.nio.file.Files;
import java.util.Locale;

import org.junit.jupiter.api.*;

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

    @Test
    public void unicode() {
        String string = "\u2654";
        System.out.println(string);
        string = "\u265A";
        System.out.println(string);
    }

    @Test
    public void run() throws FileNotFoundException {
        PlayChess.main(new String[] {});
    }

    @Test
    public void boardStartScore() {
        PlayChess.initializeBoard();
        int score = PlayChess.measureBoardScore(PlayChess.board);
        Assertions.assertEquals(0, score);
    }
}