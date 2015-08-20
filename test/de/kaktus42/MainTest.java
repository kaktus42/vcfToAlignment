package de.kaktus42;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public class MainTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(System.out);
        System.setErr(System.err);
    }

    public void resetStreams() {
        outContent.reset();
        errContent.reset();
    }

    @Test
    public void testBadParameters() {
        new Main();
        String [] bad_argv = {"/not_existent", "/not_existent", "/dev/null"};
        Main.main(bad_argv);
        assertEquals("ERROR: Cant open '/not_existent' to read.\n", errContent.toString());
        resetStreams();

        bad_argv[0] = "testdata/test.vcf";
        Main.main(bad_argv);
        assertEquals("ERROR: Cant open '/not_existent' to read.\n", errContent.toString());
    }

    @Test
    public void testGoodParameters() {
        String [] good_argv = {"testdata/test.vcf", "testdata/reference.fasta", "/dev/null"};
        Main.main(good_argv);
        //assertEquals("ERROR: Cant open '/not_existent' to read.", errContent.toString());
    }

}