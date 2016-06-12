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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

/**
 * Manages and applies a translation list for simple translation.
 * The word list must be a tab-separated file with three columns:<br>
 * 		<code>Japanese	translation	usage</code><br>
 * For the usage field, three values are supported: 
 * - 1 translates the text anywhere
 * - 2 translates the text only at the beginning
 * - 3 translates the text only at the end
 * If anything else (or nothing) is found in the usage column, the line is ignored. 
 */
public class TranslationList {
	
	static final String FIELD_SEP = "\t";
	static final int COL_SRC = 0;
	static final int COL_TRL = 1;
	static final int COL_USAGE = 2;

	private String[] srcEverywhere, trlEverywhere;
	private String[] srcPrefix, trlPrefix;
	private String[] srcSuffix, trlSuffix; 
	
	private int verbose = 0; 
	
	/** Reads a word list from a file. */
	public TranslationList(String file)
	{
		ArrayList<String> sEverywhere = new ArrayList<String>(); 
		ArrayList<String> tEverywhere = new ArrayList<String>(); 
		ArrayList<String> sPrefix = new ArrayList<String>(); 
		ArrayList<String> tPrefix = new ArrayList<String>(); 
		ArrayList<String> sSuffix = new ArrayList<String>(); 
		ArrayList<String> tSuffix = new ArrayList<String>(); 

		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String l = null;
			while((l = r.readLine()) != null) {
				String[] fields = l.split(FIELD_SEP);
				if(fields.length < 3)
					continue; // incomplete line
				String src = fields[COL_SRC].trim();
				String trl = fields[COL_TRL].trim();
				String usage = fields[COL_USAGE].trim();
				
				if(!src.isEmpty() && !trl.isEmpty())
				{
					switch(usage) {
						case "1": sEverywhere.add(src); tEverywhere.add(trl); break;
						case "2": sPrefix.add(src); tPrefix.add(trl); break;
						case "3": sSuffix.add(src); tSuffix.add(trl); break;
						default: break;
					}
				}
			}
			r.close();
		}
		catch(IOException ex)
		{
			System.err.println("error while reading translation list: " + ex.getMessage());
		}
		
		srcEverywhere = (String[]) sEverywhere.toArray(new String[sEverywhere.size()]);
		trlEverywhere = (String[]) tEverywhere.toArray(new String[tEverywhere.size()]);
		srcPrefix = (String[]) sPrefix.toArray(new String[sPrefix.size()]);
		trlPrefix = (String[]) tPrefix.toArray(new String[tPrefix.size()]);
		srcSuffix = (String[]) sSuffix.toArray(new String[sSuffix.size()]);
		trlSuffix = (String[]) tSuffix.toArray(new String[tSuffix.size()]);
	}
	
	/** Attempts to translate a text using the word list. */
	public String translate(Tokenizer tok, String text)
	{
		StringBuilder res = new StringBuilder();
		
		for(Token t : tok.tokenize(text)) {
			if(Transliterator.hasAsianChar(t.getSurface())) {
				// attempt (partial) translation
				String s = t.getSurface();
				String r = StringUtils.replaceEach(s, srcEverywhere, trlEverywhere);
				r = replaceAnyOfAtStart(r, srcPrefix, trlPrefix);
				r = replaceAnyOfAtEnd(r, srcSuffix, trlSuffix);
				if(r != s && verbose > 1)
					System.out.println("translation list: translated " + s + " to " + r);
				res.append(r);				
			}
			else // tokens without asian chars can be ignored here and will be copied directly
				res.append(t.getSurface());
		}
		return res.toString();
	}
	
	public static String replaceAnyOfAtStart(String s, String[] search, String[] replace)
	{
		for(int i = 0; i < search.length; i++) {
			if(s.startsWith(search[i]))
				s = replace[i] + s.substring(search[i].length());
		}
		return s; 
	}

	public static String replaceAnyOfAtEnd(String s, String[] search, String[] replace)
	{
		for(int i = 0; i < search.length; i++) {
			if(s.endsWith(search[i]))
				s = s.substring(0, s.length() - search[i].length()) + replace[i]; 
		}
		return s; 
	}

	public void setVerbose(int v) {
		verbose = v; 
	}
}
