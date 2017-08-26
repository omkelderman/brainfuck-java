package nl.omk;

import nl.omk.bf.Brainfuck;
import nl.omk.bf.BrainfuckSyntaxException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Provide a program as commandline argument");
            return;
        }

        Brainfuck bf = new Brainfuck();

        try {
            bf.compile(args[0]);
        } catch (IOException | BrainfuckSyntaxException e) {
            System.err.println("Error compiling program");
            e.printStackTrace();
            return;
        }

        try {
            bf.run(0xFFFF);
        } catch (IOException e) {
            System.err.println("Error running program");
            e.printStackTrace();
        }
    }
}
