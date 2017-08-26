package nl.omk.bf;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Brainfuck {
    private List<Byte> program;
    private Map<Integer, Integer> forwardJump;
    private Map<Integer, Integer> backJump;

    // list of valid characters in a brainfuck program, stored as bytes, since everything else in this program uses bytes for simplicity
    private static final HashSet<Byte> VALID_CHARS = new HashSet<>(Arrays.asList((byte) '>', (byte) '<', (byte) '+', (byte) '-', (byte) '.', (byte) ',', (byte) '[', (byte) ']'));

    // load or "compile" a program into this thing
    public void compile(String filename) throws IOException, BrainfuckSyntaxException {
        // initialize the things, effectively overwriting whatever was their so a single Brainfuck object can be reused.
        program = new ArrayList<>();
        forwardJump = new HashMap<>();
        backJump = new HashMap<>();

        // loop through the file byte by byte
        try (InputStream in = new BufferedInputStream(new FileInputStream(filename))) {
            int position = 0;
            Stack<Integer> loopStack = new Stack<>();
            for (int i = in.read(); i != -1; i = in.read()) {
                byte b = (byte) i;
                // ignore everything thats not a valid character
                if (VALID_CHARS.contains(b)) {
                    program.add(b);
                    if (b == '[') {
                        // found a '[' add the current position to the loopStack
                        loopStack.push(position);
                    } else if (b == ']') {
                        // found a ']' and also pop the last position of the loopStack
                        // we now have a corresponding pair of positions
                        int otherPosition;
                        try {
                            otherPosition = loopStack.pop();
                        } catch (EmptyStackException e) {
                            // if the stack was empty, program is invalid, "compile error :D", matching [ didnt exist
                            throw new BrainfuckSyntaxException("Messed up loops: could not find a matching [ for a ]");
                        }

                        // save the positions in our lookup list. One for jumping forward, one for jumping back
                        forwardJump.put(otherPosition, position);
                        backJump.put(position, otherPosition);
                    }
                    ++position;
                }
            }

            // if stack is not empty at the end, program is invalid, not every [ has a matching ]
            if (!loopStack.isEmpty()) {
                throw new BrainfuckSyntaxException("Messed up loops: could not find a matching ] for a [");
            }
        }
    }

    public void run(int tapeSize) throws IOException {
        run(tapeSize, System.in, System.out);
    }

    public void run(int tapeSize, InputStream in, OutputStream out) throws IOException {
        // the "memory" of the brainfuck program
        int[] tape = new int[tapeSize];
        int pointer = 0;

        for (int programCounter = 0; programCounter < program.size(); ++programCounter) {
            switch (program.get(programCounter)) {
                case '>':
                    ++pointer;
                    break;
                case '<':
                    --pointer;
                    break;
                case '+':
                    ++tape[pointer];
                    if (tape[pointer] > 0xFF) tape[pointer] = 0;
                    break;
                case '-':
                    --tape[pointer];
                    if (tape[pointer] < 0) tape[pointer] = 0xFF;
                    break;
                case '.':
                    out.write(tape[pointer]);
                    out.flush();
                    break;
                case ',':
                    tape[pointer] = in.read();
                    break;
                case '[':
                    if (tape[pointer] == 0) {
                        // jump to the matching ], the programCounter will ++ at the end of the loop anyway
                        // so we end up at the right place (the thing after the ])
                        programCounter = forwardJump.get(programCounter);
                    }
                    break;
                case ']':
                    if (tape[pointer] != 0) {
                        // jump to the matching [, the programCounter will ++ at the end of the loop anyway
                        // so we end up at the right place (the thing after the [)
                        programCounter = backJump.get(programCounter);
                    }
                    break;
            }
        }
    }
}
