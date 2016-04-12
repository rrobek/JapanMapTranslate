package kanaconv;
/*
 * $Id: Kakasi.java,v 1.15 2003/03/01 12:52:26 kawao Exp $
 *
 * KAKASI/JAVA
 *  Copyright (C) 2002-2003  KAWAO, Tomoyuki (kawao@kawao.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */



import java.io.Reader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.io.IOException;

/**
 * This class is the KAKASI/JAVA main class.
 * 
 * @author  Kawao, Tomoyuki (kawao@kawao.com)
 * @version $Revision: 1.15 $ $Date: 2003/03/01 12:52:26 $
 */
public class Kakasi {

    /** Character Set ID of ASCII */
    public static final String ASCII = "ascii";

    /** Character Set ID of Kanji */
    public static final String KANJI = "kanji";

    /** Character Set ID of Hiragana */
    public static final String HIRAGANA = "hiragana";

    /** Character Set ID of Katakana */
    public static final String KATAKANA = "katakana";

    /** Romaji type of Hepburn */
    public static final int HEPBURN = KanaToRomaConverterImpl.HEPBURN;

    /** Romaji type of Kunrei */
    public static final int KUNREI = KanaToRomaConverterImpl.KUNREI;

    private static final Converter defaultConverter = new DefaultConverter();

    private final KanjiInput input = new KanjiInput();
    private final KanjiOutput output = new KanjiOutput();

    private final HiraganaConverterImpl hiraganaConverterImpl;
    private final KatakanaConverterImpl katakanaConverterImpl;
    private final KanaToRomaConverterImpl kanaToRomaConverterImpl;

    private Converter kanjiConverter;
    private Converter hiraganaConverter;
    private Converter katakanaConverter;

    private boolean wakachigakiMode;

    /**
     * Constructs a Kakasi object.
     */
    public Kakasi() {
        hiraganaConverterImpl = new HiraganaConverterImpl();
        katakanaConverterImpl = new KatakanaConverterImpl();
        kanaToRomaConverterImpl = new KanaToRomaConverterImpl();
    }

    /**
     * Prepares the hiragana converter.
     *
     * @param  characterSet  the destination character set ID.
     * @see #ASCII
     * @see #HIRAGANA
     * @see #KATAKANA
     */
    public void setupHiraganaConverter(String characterSet) {
        hiraganaConverter = characterSet == null ?
            null : createHiraganaConverter(characterSet);
    }

    /**
     * Creates the hiragana converter that converts hiragana to the specified
     * character set.
     *
     * @param  characterSet  the destination character set ID.
     */
    private Converter createHiraganaConverter(String characterSet) {
        if (characterSet.equals(ASCII)) {
            return new Converter() {
                public boolean convert(KanjiInput input, Writer output)
                    throws IOException {
                    return kanaToRomaConverterImpl.convertHiragana(input,
                                                                   output);
                }
            };
        } else if (characterSet.equals(HIRAGANA)) {
            return new Converter() {
                public boolean convert(KanjiInput input, Writer output)
                    throws IOException {
                    return hiraganaConverterImpl.toHiragana(input, output);
                }
            };
        } else if (characterSet.equals(KATAKANA)) {
            return new Converter() {
                public boolean convert(KanjiInput input, Writer output)
                    throws IOException {
                    return hiraganaConverterImpl.toKatakana(input, output);
                }
            };
        } else {
            String message =
                "HiraganaConverter does not support character set: " +
                characterSet;
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Prepares the katakana converter.
     *
     * @param  characterSet  the destination character set ID.
     * @see #ASCII
     * @see #HIRAGANA
     * @see #KATAKANA
     */
    public void setupKatakanaConverter(String characterSet) {
        katakanaConverter = characterSet == null ?
            null : createKatakanaConverter(characterSet);
    }

    /**
     * Creates the katakana converter that converts katakana to the specified
     * character set.
     *
     * @param  characterSet  the destination character set ID.
     */
    private Converter createKatakanaConverter(String characterSet) {
        if (characterSet.equals(ASCII)) {
            return new Converter() {
                public boolean convert(KanjiInput input, Writer output)
                    throws IOException {
                    return kanaToRomaConverterImpl.convertKatakana(input,
                                                                   output);
                }
            };
        } else  if (characterSet.equals(HIRAGANA)) {
            return new Converter() {
                public boolean convert(KanjiInput input, Writer output)
                    throws IOException {
                    return katakanaConverterImpl.toHiragana(input, output);
                }
            };
        } else if (characterSet.equals(KATAKANA)) {
            return new Converter() {
                public boolean convert(KanjiInput input, Writer output)
                    throws IOException {
                    return katakanaConverterImpl.toKatakana(input, output);
                }
            };
        } else {
            String message =
                "KatakanaConverter does not support character set: " +
                characterSet;
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Gets the input object.
     */
    public KanjiInput getInput() {
        return input;
    }

    /**
     * Gets the output object.
     */
    public KanjiOutput getOutput() {
        return output;
    }

    /**
     * Sets the Romaji type property value. The default value is HEPBURN.
     *
     * @param newType  new romaji type.
     * @see #HEPBURN
     * @see #KUNREI
     */
    public void setRomajiType(int newType) {
        kanaToRomaConverterImpl.setType(newType);
    }
    
    /**
     * Gets the Romaji type property value.
     *
     * @see #HEPBURN
     * @see #KUNREI
     */
    public int getRomajiType() {
        return kanaToRomaConverterImpl.getType();
    }

    /**
     * Sets the romaji capitalize mode property. The default value is false.
     *
     * @param newMode  new romaji capitalize mode value.
     */
    public void setRomajiCapitalizeMode(boolean newMode) {
        kanaToRomaConverterImpl.setCapitalizeMode(newMode);
    }

    /**
     * Gets the romaji capitalize mode property value.
     */
    public boolean isRomajiCapitalizeMode() {
        return kanaToRomaConverterImpl.isCapitalizeMode();
    }

    /**
     * Sets the romaji upper case mode property. The default value is false.
     *
     * @param newMode  new romaji upper case mode value.
     */
    public void setRomajiUpperCaseMode(boolean newMode) {
        kanaToRomaConverterImpl.setUpperCaseMode(newMode);
    }

    /**
     * Gets the romaji upper case mode property value.
     */
    public boolean isRomajiUpperCaseMode() {
        return kanaToRomaConverterImpl.isUpperCaseMode();
    }

    /**
     * Processes the specified string.
     *
     * @param string  the input string to process.
     * @return  the result string.
     * @exception  IOException  if an I/O error occurred.
     */
    public synchronized String doString(String string) throws IOException {
        input.setInputString(string);
        StringWriter writer = new StringWriter(string.length() * 2);
        output.setWriter(writer);
        run();
        return writer.toString();
    }

    /**
     * Runs the conversion process.
     *
     * @exception  IOException  if an I/O error occurred.
     */
    public synchronized void run() throws IOException {
        while (true) {
            int ch = input.get();
            if (ch < 0) {
                break;
            }
            Converter converter = null;
            Character.UnicodeBlock block = Character.UnicodeBlock.of((char)ch);
            if (block.equals(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)) {
                converter = kanjiConverter;
            } else if (block.equals(Character.UnicodeBlock.HIRAGANA)) {
                converter = hiraganaConverter;
            } else if (block.equals(Character.UnicodeBlock.KATAKANA)) {
                converter = katakanaConverter;
            }
            if (converter == null) {
                converter = defaultConverter;
            }
            output.putSeparator();
            if (!converter.convert(input, output)) {
                input.consume(1);
                if (wakachigakiMode) {
                    output.write((char)ch);
                }
            }
        }
        output.flush();
    }

}
