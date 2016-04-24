/*
 * JapanMapTranslate
 * Copyright (c) Florian Fischer, 2016 (florianfischer@gmx.de)
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import kanaconv.Kakasi;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

/** Transliterates japanese text to latin characters (romaji). */
public class Transliterator {

    // Two instances of the Kakasi service
    // are required to transliterate both kana scripts.
	private Kakasi trHiragana = null;
	private Kakasi trKatakana = null;
	// Use the Kumoroji tokenizer to split words and to read kanji.
	private Tokenizer tokKanji = null; 
	
	public Transliterator()
	{
    	tokKanji = new Tokenizer(); 
    	
    	trHiragana = new Kakasi();
    	trHiragana.setupHiraganaConverter(Kakasi.ASCII);
    	trHiragana.setRomajiCapitalizeMode(true);
    	
    	trKatakana = new Kakasi();
    	trKatakana.setupKatakanaConverter(Kakasi.ASCII);
    	trKatakana.setRomajiCapitalizeMode(true);		
	}
    
    public static boolean isAsianChar(char ch)
    {
		// very simplified range matching; this range
		// contains all japanese scripts (Hiragana, Katakana and Kanji)
		// surrogate pairs are also accepted because many CJK chars are extended
		return ((ch >= 0x2E80 && ch < 0xA000) || (ch >= 0xD800 && ch <= 0xDFFF));
    }
    
    // returns true if any of the east-asian scripts is used
    public static boolean hasAsianChar(String text)
    {
    	for(int c = 0; c < text.length(); c++) {
    		char ch = text.charAt(c);
    		if(isAsianChar(ch))
    			return true; 
    	}
    	return false; 
    }
    
    public static boolean isKanji(char ch)
    {
		// very simplified range matching; this range
		// contains excludes the Hiragana and Katakana, but includes
    	// all CJK chars and some other asian scripts
		// surrogate pairs are also accepted because many CJK chars are extended
		return ((ch >= 0x3200 && ch < 0xA000) || (ch >= 0xD800 && ch <= 0xDFFF)); 
    }
    
    // returns true ONLY if kanji are used
    public static boolean hasKanji(String text)
    {
    	for(int c = 0; c < text.length(); c++) {
    		char ch = text.charAt(c);
    		if(isKanji(ch))
    			return true; 
    	}
    	return false; 
    }	
    
    // -------------------
    // Prenormalization: replace some simple Asian character variants by their Latin equivalents 
    private Map<Character, Character> prenormTable = null;
    private synchronized Map<Character, Character> getPrenormTable()
    {
    	if(prenormTable == null) {
    		Map<Character, Character> t = new HashMap<>();
    		t.put('\u3000', ' '); // Full-width space
    		t.put('\uff10', '0'); // Full-width numbers
    		t.put('\uff11', '1');
    		t.put('\uff12', '2');
    		t.put('\uff13', '3');
    		t.put('\uff14', '4');
    		t.put('\uff15', '5');
    		t.put('\uff16', '6');
    		t.put('\uff17', '7');
    		t.put('\uff18', '8');
    		t.put('\uff19', '9');
    		t.put('\uff0c', '.'); // Full-width quotation marks
    		t.put('\uff0e', ',');
    		prenormTable = t; 
    	}
    	return prenormTable;
    }
    
    private String prenormalize(String str) {
    	Map<Character, Character> tbl = getPrenormTable();
    	char[] chars = str.toCharArray();
    	for(int i = 0; i < chars.length; i++) {
    		Character cse = chars[i];
    		Character rpl = tbl.get(cse);
    		if(rpl != null)
    			chars[i] = rpl.charValue();
    	}
    	return new String(chars);
    }
    
    // Postnormalization: Replace [aeiou]^ by the nice characters with macrons on top.  
    private String postnormalize(String str) {
    	if(str.indexOf('^') >= 0) {
    		str = str.replace("A^", "\u0100");
    		str = str.replace("E^", "\u0112");
    		str = str.replace("I^", "\u012A");
    		str = str.replace("O^", "\u014C");
    		str = str.replace("U^", "\u016A");
    		str = str.replace("a^", "\u0101");
    		str = str.replace("e^", "\u0113");
    		str = str.replace("i^", "\u012B");
    		str = str.replace("o^", "\u014D");
    		str = str.replace("u^", "\u016B");
    	}
    	// Sometimes, too many spaces are created.
    	str = str.replace("  ", " ");
    	str = str.trim();
    	return str; 
    }
    

    public String transliterate(String jaName) throws IOException {
    	String trName = jaName; 
    	
    	trName = prenormalize(trName);
    	
		// Use Kuromoji for preprocessing of kanji and for tokenization
		StringBuilder sb = new StringBuilder();
		for(Token t : tokKanji.tokenize(trName)) {
			if(hasKanji(t.getSurface()) && !(t.getReading().equals("*"))) {
				if(sb.length() > 0)
					sb.append(' ');
				sb.append(t.getReading());
			}
			/*if(hasKanji(t.getSurface()) && !(t.getLemmaReadingForm().equals("*"))) {
				if(sb.length() > 0)
					sb.append(' ');
				sb.append(t.getLemmaReadingForm());
			}*/
			else {
				String surf = t.getSurface();
				if(sb.length() > 0)
					sb.append(' ');
				sb.append(surf);
			}
			/*sb.append(t.getReading());
			sb.append('[');
			sb.append(t.getSurface());
			sb.append(']');
			sb.append(' ');*/
		}
		trName = sb.toString();
		
		// Use Kakasi for rest of conversion
		trName = trHiragana.doString(trName);
		trName = trKatakana.doString(trName);
		
		// Add spaces between words (but not within abbreviations)
		trName = trName.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
		trName = postnormalize(trName);
		
		/*// debugging option: marking remaining asian chars in the output.  
		StringBuilder sbOut = new StringBuilder();
		for(char c : trName.toCharArray()) {
			sbOut.append(c);
			if(c > 0x200) {
				sbOut.append(String.format("[\\u%04x]", (int)c));
			}
				
		} 

		return sbOut.toString(); */
		return trName; 
	}	
    
    public static void main(String[] args) throws IOException
    {
    	Transliterator tr = new Transliterator();
    	for(String arg : args)
    	{
    		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(arg), "UTF-8"));
    		Writer w = new OutputStreamWriter(new FileOutputStream(arg + ".tr"), "UTF-8");
    		String l = null;
    		while((l = r.readLine()) != null) {
    			l = tr.transliterate(l);
    			w.write(l);
    			w.write('\n');
    		}
    		r.close();
    		w.close();    		
    	}
    }
}
