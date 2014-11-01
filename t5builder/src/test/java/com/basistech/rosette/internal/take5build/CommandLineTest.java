/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2014 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.rosette.internal.take5build;

import com.basistech.rosette.internal.take5.Take5Exception;
import com.basistech.rosette.internal.take5.Take5Match;
import com.basistech.rosette.internal.take5.Take5Dictionary;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

/**
 * Tests that run the command-line class {@link Take5Build}.
 */
public class CommandLineTest extends Assert {

    public static final String COPYRIGHT_C_2014_ELMER_FUDD = "Copyright (c) 2014 elmer fudd.";

    //CHECKSTYLE:OFF
    @Rule
    public ExpectedException exception = ExpectedException.none();
    //CHECKSTYLE:ON

    @Test
    public void perfhashKeysOnly() throws Exception {
        File t5File = File.createTempFile("t5btest.", ".bin");
        t5File.deleteOnExit();
        File inputFile = new File("src/test/data/hex-keys.txt");
        File mdFile = File.createTempFile("md.", ".tsv");
        mdFile.deleteOnExit();
        Files.write("k1\tvalue 1\nk2\tvalue 2\n", mdFile, Charsets.UTF_8);
        File copyrightFile = File.createTempFile("copyright.", ".txt");
        Files.write(COPYRIGHT_C_2014_ELMER_FUDD, copyrightFile, Charsets.UTF_8);
        copyrightFile.deleteOnExit();
        Take5Build cmd = new Take5Build();
        cmd.noPayloads = true;
        cmd.engine = Engine.PERFHASH;
        cmd.outputFile = t5File;
        cmd.keyFormat = KeyFormat.HASH_STRING;
        cmd.commandInputFile = inputFile;
        cmd.metadataFile = mdFile;
        cmd.copyrightFile = copyrightFile;
        cmd.checkOptionConsistency();
        cmd.build();

        ByteBuffer resultT5 = Files.map(t5File);
        Take5Dictionary dict = new Take5Dictionary(resultT5, resultT5.capacity());

        assertEquals(Take5Format.ENGINE_PERFHASH, dictSpyInt(dict, "fsaEngine"));

        Take5Match match = new Take5Match();
        assertTrue(dict.matchExact("bedded".toCharArray(), 0, "bedded".length(), match));
        assertEquals(COPYRIGHT_C_2014_ELMER_FUDD, dict.getCopyright());
        assertEquals("value 1", dict.getMetadata().get("k1"));
        assertEquals("value 2", dict.getMetadata().get("k2"));
    }

    @Test
    public void simpleFormatMissingValues() throws Exception {
        File t5File = File.createTempFile("t5btest.", ".bin");
        t5File.deleteOnExit();
        File inputFile = new File("src/test/data/hex-keys.txt");
        Take5Build cmd = new Take5Build();
        cmd.engine = Engine.FSA;
        cmd.outputFile = t5File;
        cmd.commandInputFile = inputFile;
        cmd.simplePayloads = true;
        cmd.checkOptionConsistency();
        cmd.build();

        ByteBuffer resultT5 = Files.map(t5File);
        Take5Dictionary dict = new Take5Dictionary(resultT5, resultT5.capacity());
        Take5Match match = new Take5Match();
        assertTrue(dict.matchExact("bedded".toCharArray(), 0, "bedded".length(), match));
        int valueOffset = match.getOffsetValue();
        CharBuffer dictAsChars = resultT5.asCharBuffer();
        assertThat(dictAsChars.get(valueOffset / 2), equalTo((char)0));
    }

    @Test
    public void defaultFormat() throws Exception {
        File t5File = File.createTempFile("t5btest.", ".bin");
        t5File.deleteOnExit();
        File inputFile = new File("src/test/data/default-format-test.txt");
        t5File.deleteOnExit();
        Take5Build cmd = new Take5Build();
        cmd.engine = Engine.FSA;
        cmd.outputFile = t5File;
        cmd.commandInputFile = inputFile;
        cmd.defaultPayloadFormat = "#2!i";
        cmd.alignment = 2;
        cmd.checkOptionConsistency();
        cmd.build();

        ByteBuffer resultT5 = Files.map(t5File);
        Take5Dictionary dict = new Take5Dictionary(resultT5, resultT5.capacity());
        assertEquals(Take5Format.ENGINE_SEARCH, dictSpyInt(dict, "fsaEngine"));
        Take5Match match = new Take5Match();
        assertThat(dict.matchExact("key1".toCharArray(), 0, "key1".length(), match), is(equalTo(true)));
        int valueOffset = match.getOffsetValue();
        resultT5.order(ByteOrder.nativeOrder());
        ShortBuffer dictAsShorts = resultT5.asShortBuffer();
        assertThat(dictAsShorts.get(valueOffset / 2), equalTo((short)0x4e56));
    }

    // make sure that the above test means something because the default is not #2!i
    @Test
    public void defaultFormatContrast() throws Exception {
        File t5File = File.createTempFile("t5btest.", ".bin");
        t5File.deleteOnExit();
        File inputFile = new File("src/test/data/default-format-test.txt");
        t5File.deleteOnExit();
        Take5Build cmd = new Take5Build();
        cmd.engine = Engine.FSA;
        cmd.outputFile = t5File;
        cmd.commandInputFile = inputFile;
        cmd.defaultPayloadFormat = "#4!i";
        cmd.alignment = 4;
        cmd.checkOptionConsistency();
        cmd.build();

        ByteBuffer resultT5 = Files.map(t5File);
        Take5Dictionary dict = new Take5Dictionary(resultT5, resultT5.capacity());
        assertEquals(Take5Format.ENGINE_SEARCH, dictSpyInt(dict, "fsaEngine"));
        Take5Match match = new Take5Match();
        assertThat(dict.matchExact("key1".toCharArray(), 0, "key1".length(), match), is(equalTo(true)));
        int valueOffset = match.getOffsetValue();
        ShortBuffer dictAsShorts = resultT5.asShortBuffer();
        assertThat(dictAsShorts.get(valueOffset / 2), not(equalTo((short) 0x4e56)));
    }

    @Test
    public void controlFileNoMain() throws Exception {
        File t5File = File.createTempFile("t5btest.", ".bin");
        t5File.deleteOnExit();
        Take5Build cmd = new Take5Build();
        cmd.engine = Engine.FSA;
        cmd.outputFile = t5File;
        cmd.alignment = 4;
        cmd.controlFile = new File("src/test/data/mixed-two-entry-points.ctl.txt");
        cmd.checkOptionConsistency();
        cmd.build();

        exception.expect(Take5Exception.class);

        ByteBuffer resultT5 = Files.map(t5File);
        new Take5Dictionary(resultT5, resultT5.capacity());
    }

    @Test
    public void controlFileByteOrder() throws Exception {
        /*
         * This test, it turns out, is a test of default-mode handling. There was a bug:
         * if a control file does not bother to specify default-mode, the code was not
         * using the command-line default as a fallback.
         */
        // we only need to test this one way.
        Assume.assumeTrue(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
        File t5File = File.createTempFile("t5btest.", ".bin");
        t5File.deleteOnExit();
        Take5Build cmd = new Take5Build();
        cmd.engine = Engine.FSA;
        cmd.outputFile = t5File;
        cmd.alignment = 4;
        cmd.defaultPayloadFormat = "#2!i";
        cmd.byteOrder = ByteOrder.BIG_ENDIAN;

        cmd.controlFile = new File("src/test/data/mixed-two-entry-points.ctl.txt");
        cmd.checkOptionConsistency();
        cmd.build();

        ByteBuffer resultT5 = Files.map(t5File);
        Take5Dictionary dict1 = new Take5Dictionary(resultT5, resultT5.capacity(), "mixed1");
        String key = "key9";
        String value = "some 2-byte string";
        Take5Match match = new Take5Match();
        assertThat(dict1.matchExact(key.toCharArray(), 0, key.length(), match), is(equalTo(true)));
        int valueOffset = match.getOffsetValue();
        resultT5.order(ByteOrder.BIG_ENDIAN);

        short number = resultT5.getShort(valueOffset);
        assertEquals(9, number);

        CharBuffer dictAsChars = resultT5.asCharBuffer();
        char[] dictChars = new char[value.length()];
        dictAsChars.position((2 + valueOffset) / 2);
        dictAsChars.get(dictChars);
        assertThat(new String(dictChars, 0, dictChars.length), is(equalTo(value)));
    }

    @Test
    public void controlFileBasic() throws Exception {
        File t5File = File.createTempFile("t5btest.", ".bin");
        t5File.deleteOnExit();
        Take5Build cmd = new Take5Build();
        cmd.engine = Engine.FSA;
        cmd.outputFile = t5File;
        cmd.alignment = 4;
        cmd.controlFile = new File("src/test/data/mixed-two-entry-points.ctl.txt");
        cmd.checkOptionConsistency();
        cmd.build();

        ByteBuffer resultT5 = Files.map(t5File);
        Take5Dictionary dict1 = new Take5Dictionary(resultT5, resultT5.capacity(), "mixed1");
        assertThat(dict1.getContentFlags(), is(equalTo(0xdeadbeef)));
        //key2	"some 2-byte string"
        String key = "key2";
        String value = "some 2-byte string";
        Take5Match match = new Take5Match();
        assertThat(dict1.matchExact(key.toCharArray(), 0, key.length(), match), is(equalTo(true)));
        int valueOffset = match.getOffsetValue();
        resultT5.order(ByteOrder.nativeOrder());

        CharBuffer dictAsChars = resultT5.asCharBuffer();
        char[] dictChars = new char[value.length()];
        dictAsChars.position(valueOffset / 2);
        dictAsChars.get(dictChars);
        assertThat(new String(dictChars, 0, dictChars.length), is(equalTo(value)));

        Take5Dictionary dict2 = new Take5Dictionary(resultT5, resultT5.capacity(), "mixed2");
        //@key\u4e00	"key with an escape"
        key = "@key\u4e00";
        value = "key with an escape";
        assertThat(dict2.matchExact(key.toCharArray(), 0, key.length(), match), is(equalTo(true)));
        valueOffset = match.getOffsetValue();
        dictAsChars = resultT5.asCharBuffer();
        dictChars = new char[value.length()];
        dictAsChars.position(valueOffset / 2);
        dictAsChars.get(dictChars);
        assertThat(new String(dictChars, 0, dictChars.length), is(equalTo(value)));
        assertThat(dict2.getMinimumContentVersion(), is(equalTo(1)));
        assertThat(dict2.getMaximumContentVersion(), is(equalTo(3)));
    }

    /*
     * See COMN-139. A payload consisting of a \ and a PU character misfires.
     * The input file doubles the \ correctly, so this is going to spawn more
     * tests further down.
     */
    @Test
    public void slashPrivateUsePayload() throws Exception {
        File t5File = File.createTempFile("t5btest.", ".bin");
        t5File.deleteOnExit();
        Take5Build cmd = new Take5Build();
        cmd.engine = Engine.FSA;
        cmd.outputFile = t5File;
        cmd.alignment = 2;
        cmd.byteOrder = ByteOrder.LITTLE_ENDIAN;
        cmd.defaultPayloadFormat = "#2!i";
        cmd.commandInputFile = new File("src/test/data/slash-pu.txt");
        cmd.checkOptionConsistency();
        cmd.build();
        ByteBuffer resultT5 = Files.map(t5File);
        Take5Dictionary dict = new Take5Dictionary(resultT5, resultT5.capacity(), "main");
        Take5Match match = new Take5Match();
        assertThat(dict.matchExact("\\".toCharArray(), 0, "\\".length(), match), is(equalTo(true)));
        resultT5.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer dictAsShorts = resultT5.asShortBuffer();
        int valueOffset = match.getOffsetValue(); // indicated the '3' as a short and the two chars
        short[] dictShorts = new short[3];
        dictAsShorts.position(valueOffset / 2);
        dictAsShorts.get(dictShorts);
        assertEquals(3, dictShorts[0]);
        assertEquals('\\', (char)dictShorts[1]);
        assertEquals('\ue018', (char)dictShorts[2]);
    }

    private int dictSpyInt(Take5Dictionary dict, String fieldName) {
        try {
            Field field = Take5Dictionary.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(dict);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
