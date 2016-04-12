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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
/*import com.atilika.kuromoji.unidic.Token;
import com.atilika.kuromoji.unidic.Tokenizer;*/
/*import com.atilika.kuromoji.jumandic.Token;
import com.atilika.kuromoji.jumandic.Tokenizer;*/
import kanaconv.Kakasi;

 
public class Translater extends DefaultHandler 
{
	// Temporary storage
    StringBuffer textBuffer;
    // Output writer
    private Writer out;
    // Further parameters
    private int verbose = 0;
    private boolean both = false; 

    // Three instances of the Kakasi service
    // are required to transliterate all three
    // japanese scripts. 
    //private Kakasi trKanji = null;
	private Kakasi trHiragana = null;
	private Kakasi trKatakana = null;
	private Tokenizer tokKanji = null; 
	
    
    public Translater(Writer out)
    {
    	this.out = out; 
    	
    	tokKanji = new Tokenizer(); 
    	
    	trHiragana = new Kakasi();
    	trHiragana.setupHiraganaConverter(Kakasi.ASCII);
    	trHiragana.setRomajiCapitalizeMode(true);
    	
    	trKatakana = new Kakasi();
    	trKatakana.setupKatakanaConverter(Kakasi.ASCII);
    	trKatakana.setRomajiCapitalizeMode(true);
    }
    
    public void setVerbose(int v)
    {
    	verbose = v;
    }
    
    public void setBoth(boolean b)
    {
    	both = b; 
    }
    
    // Output statistics: 
    private int numSuccess = 0, numPartial = 0, numFailed = 0; 
    
	public int getNumSuccess() {
		return numSuccess;
	}

	public int getNumPartial() {
		return numPartial;
	}

	public int getNumFailed() {
		return numFailed;
	}

    
    // Information about the current map element
    private String jaName = null;
    private String enName = null; 
    
    
    public boolean isMapElem(String elemName)
    {
    	return elemName.equals("way") || elemName.equals("node") || elemName.equals("relation");
    }
    
    public boolean isAsianChar(char ch)
    {
		// very simplified range matching; this range
		// contains all japanese scripts (Hiragana, Katakana and Kanji)
		// surrogate pairs are also accepted because many CJK chars are extended
		return ((ch >= 0x2E80 && ch < 0xA000) || (ch >= 0xD800 && ch <= 0xDFFF));
    }
    
    // returns true if any of the east-asian scripts is used
    public boolean hasAsianChar(String text)
    {
    	for(int c = 0; c < text.length(); c++) {
    		char ch = text.charAt(c);
    		if(isAsianChar(ch))
    			return true; 
    	}
    	return false; 
    }
    
    public boolean isKanji(char ch)
    {
		// very simplified range matching; this range
		// contains excludes the Hiragana and Katakana, but includes
    	// all CJK chars and some other asian scripts
		// surrogate pairs are also accepted because many CJK chars are extended
		return ((ch >= 0x3200 && ch < 0xA000) || (ch >= 0xD800 && ch <= 0xDFFF)); 
    }
    
    // returns true ONLY if kanji are used
    public boolean hasKanji(String text)
    {
    	for(int c = 0; c < text.length(); c++) {
    		char ch = text.charAt(c);
    		if(isKanji(ch))
    			return true; 
    	}
    	return false; 
    }


    //===========================================================
    // SAX DocumentHandler methods
    //===========================================================
    public void startDocument() throws SAXException {
        emit("<?xml version='1.0' encoding='UTF-8'?>");
        nl();
    }

    public void endDocument() throws SAXException {
        try {
            nl();
            out.flush();
        } catch (IOException e) {
            throw new SAXException("I/O error", e);
        }
    }

    public void startElement(String namespaceURI, String sName, // simple name
        String qName, // qualified name
        Attributes attrs) throws SAXException {
        echoText();

        String eName = sName; // element name
        
        if ("".equals(eName)) {
            eName = qName; // not namespaceAware
        }

        if(isMapElem(eName)) {
        	// New map elem starts. Now we have to look for its names... 
        	jaName = null;
        	enName = null; 
        }
        else if(eName.equals("tag"))
        {
        	String key = attrs.getValue("k");
        	if(key.equals("name:en") || (key.equals("name:ja_rm") && enName == null))
        		enName = attrs.getValue("v");
        	else if(key.equals("name") || key.equals("name:ja"))
        		jaName = attrs.getValue("v");
        }

        emit("<" + eName);

        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                String aName = attrs.getLocalName(i); // Attr name 

                if ("".equals(aName)) {
                    aName = attrs.getQName(i);
                }

                emit(" ");
                emit(aName + "=\"" + StringEscapeUtils.escapeXml(attrs.getValue(i)) + "\"");
            }
        }

        emit(">");
    }

    public void endElement(String namespaceURI, String sName, // simple name
        String qName // qualified name
    ) throws SAXException {
        echoText();

        String eName = sName; // element name

        if ("".equals(eName)) {
            eName = qName; // not namespaceAware
        }
        
        if(isMapElem(eName)) {
        	// Todo: Generate an english name and write it here, if none exists
        	if(enName == null && jaName != null) {
        		// First check: are there kanji in the jaName? 
        		if(hasAsianChar(jaName)) {
        			String trName = null;
					try {
						// Transliterate all writing systems
						trName = transliterate(jaName);
						
						// Check result
						boolean fail = (trName.equals(jaName));
						boolean partial = !fail && hasAsianChar(trName);
						if(fail) numFailed++;
						else if(partial) numPartial++;
						else numSuccess++;
						
						if(verbose > 0) {
							String result =      "success: ";
							if(partial) result = "partial: ";
							if(fail) result =    "FAILURE: ";
							if(fail || partial || verbose > 1)
								System.out.println(result + "generated english name: " + trName + " from japanese name: " + jaName);
						}
						// Output the transliterated name
	            		if(trName != null && !fail) {
	            			String finalName = trName;
	            			if(both) finalName = finalName + " (" + jaName + ")";
	            			String out = "<tag k=\"name:en\" v=\"" + StringEscapeUtils.escapeXml(finalName)
	            					+ "\" />\n";
	            			emit(out);
	            		}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        }

        emit("</" + eName + ">");
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
    

    private String transliterate(String jaName) throws IOException {
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

	public void characters(char[] buf, int offset, int len)
        throws SAXException {
        String s = new String(buf, offset, len);

        if (textBuffer == null) {
            textBuffer = new StringBuffer(s);
        } else {
            textBuffer.append(s);
        }
    }

    //===========================================================
    // Utility Methods ...
    //===========================================================
    private void echoText() throws SAXException {
        if (textBuffer == null) {
            return;
        }

        emit(StringEscapeUtils.escapeXml(textBuffer.toString()));
        textBuffer = null;
    }

    // Wrap I/O exceptions in SAX exceptions, to
    // suit handler signature requirements
    private void emit(String s) throws SAXException {
        try {
            out.write(s);
            //out.flush();
        } catch (IOException e) {
            throw new SAXException("I/O error", e);
        }
    }

    // Start a new line
    private void nl() throws SAXException {
        String lineEnd = System.getProperty("line.separator");

        try {
            out.write(lineEnd);
        } catch (IOException e) {
            throw new SAXException("I/O error", e);
        }
    }

}
