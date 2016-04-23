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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
/* Tests with the other dictionaries did not show improved results. */ 
/*import com.atilika.kuromoji.unidic.Token;
import com.atilika.kuromoji.unidic.Tokenizer;*/
/*import com.atilika.kuromoji.jumandic.Token;
import com.atilika.kuromoji.jumandic.Tokenizer;*/

 
public class MapTranslater extends DefaultHandler 
{
	// Temporary storage
    StringBuffer textBuffer;
    // Output writer
    private Writer out;
    // Further parameters
    private int verbose = 0;
    private boolean both = false; 
    private boolean advanced = false; 

    // Actual transliteration
    private Transliterator trl = null;

	// Stat file stuff
	private String statFile = null;
	private Map<String, Integer> stats; 
    
    public MapTranslater(Writer out)
    {
    	this.out = out; 
    	trl = new Transliterator();
    }
    
    public void setVerbose(int v)
    {
    	verbose = v;
    }
    
    public void setBoth(boolean b)
    {
    	both = b; 
    }
    
    public void setAdvanced(boolean b)
    {
    	advanced = b;
    }
    
    public void enableStats(String statFile)
    {
    	this.statFile = statFile;
    	stats = new HashMap<String, Integer>();
    }
    
    // Output statistics: 
    private int numSuccess = 0, numPartial = 0, numFailed = 0, numEnglish = 0; 
    
	public int getNumSuccess() {
		return numSuccess;
	}

	public int getNumPartial() {
		return numPartial;
	}

	public int getNumFailed() {
		return numFailed;
	}
	
	public int getNumEnglish() {
		return numEnglish; 
	}

    
    // Information about the current map element
    private String jaName = null; // both "name" or "name:ja"
    private String enName = null; // both "name:en" or "name:ja_rm"
    private String enNameOnly = null; // only "name:en"
    private String deName = null; // only "name:de"
    
    public boolean isMapElem(String elemName)
    {
    	return elemName.equals("way") || elemName.equals("node") || elemName.equals("relation");
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
        
        // Stats handling: 
        if(statFile != null) {
        	try {
        		Writer wr = new OutputStreamWriter(new FileOutputStream(statFile), "UTF-8");
        		for (String item : stats.keySet()) {
					wr.write(item);
					wr.write('\t');
					wr.write(stats.get(item).toString());
					wr.write('\n');
				}
        		wr.close();
        	}
        	catch(IOException e) {
        		throw new SAXException("Stat writing error", e);
        	}
        	
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
        	enNameOnly = null;
        	deName = null; 
        }
        else if(eName.equals("tag"))
        {
        	String key = attrs.getValue("k");
        	if(key.equals("name:en") || (key.equals("name:ja_rm") && enName == null))
        		enName = attrs.getValue("v");
        	else if(key.equals("name:de"))
        		deName = attrs.getValue("v");
        	else if(key.equals("name") || key.equals("name:ja"))
        		jaName = attrs.getValue("v");
        	if(key.equals("name:en"))
        		enNameOnly = attrs.getValue("v");
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
        	// The transliterated name. May be used by both english and advanced outputs. 
        	// Do the transliteration here once, if necessary. 
        	String trName = enName; 
        	if((enName == null) && jaName != null) {
        		// First check: are there kanji in the jaName? 
        		if(Transliterator.hasAsianChar(jaName)) {
					try {
						// Transliterate all writing systems
						trName = transliterate(jaName);
						
						// Check result
						boolean fail = (trName.equals(jaName));
						boolean partial = !fail && Transliterator.hasAsianChar(trName);
						if(fail) {
							numFailed++;
							trName = null; 
						}
						else if(partial) { 
							numPartial++;
						}
						else { 
							numSuccess++;
						}
						
						if(verbose > 0) {
							String result =      "success: ";
							if(partial) result = "partial: ";
							if(fail) result =    "FAILURE: ";
							if(fail || partial || verbose > 1)
								System.out.println(result + "generated english name: " + trName + " from japanese name: " + jaName);
						}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        	
        	if(trName != null) {
	        	// Write the english name
	        	if(enNameOnly == null && jaName != null) {
					// Output the transliterated name
	        		if(trName != null) {
	        			String finalName = trName;
	        			// TODO: additional translation of the finalName should be done here. For now, just do stats.
	        			if(statFile != null) {
		        			String[] words = finalName.trim().split("\\s+");
		        			for(String word : words) {
		        				String tok = word.toLowerCase();
		        				Integer val = stats.get(tok);
		        				int v = 0; 
		        				if(val != null) v = val.intValue();
		        				stats.put(tok, v+1);
		        			}
	        			}
	        			// Actual Output
	        			if(both) finalName = finalName + " (" + jaName + ")";
	        			String out = "<tag k=\"name:en\" v=\"" + StringEscapeUtils.escapeXml(finalName)
	        					+ "\" />\n";
	        			emit(out);
	        		}
	        	}
        	}
        	
        	// Write the "advanced" name also, if enabled
        	if(advanced && deName == null && jaName != null) {
        		String finalName = null; 
        		// Null check: can we use the english name instead? 
        		if(enName != null) {
        			// Output the english name
        			finalName = jaName; if(!enName.equals(jaName)) finalName = finalName + " (" + enName + ")";
        			numEnglish++;
        		}
        		// No, there is none. Use the trName instead, if exists.
        		if(trName != null) {
        			finalName = jaName; if(!trName.equals(jaName)) finalName = finalName + " (" + trName + ")";
        		}
        		if(finalName != null)
        		{
	        		// Finally output the advanced name.
	    			String out = "<tag k=\"name:de\" v=\"" + StringEscapeUtils.escapeXml(finalName)
	    					+ "\" />\n";
	    			emit(out);
        		}
        	}
        }

        emit("</" + eName + ">");
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
	private String transliterate(String s) throws IOException
	{
		return trl.transliterate(s);
	}
	
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
