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
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Manages and applies a word list for simple translation.
 * The word list must be a tab-separated file with three columns:<br>
 * 		<code>sourceword	translation	usage</code><br>
 * The line is only used if the "usage" field is not empty.
 */
public class Wordlist {
	
	static final String FIELD_SEP = "\t";
	static final int COL_SRC = 0;
	static final int COL_TRL = 1;
	static final int COL_USAGE = 2;

	private Map<String, String> wordList = null;
	
	/** Reads a word list from a file. */
	public Wordlist(String file)
	{
		wordList = new HashMap<String, String>();

		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String l = null;
			while((l = r.readLine()) != null) {
				String[] fields = l.split(FIELD_SEP);
				if(fields.length < 3)
					continue; // incomplete line
				if(fields[COL_USAGE].trim().isEmpty())
					continue; // unused line 
				String srcLower = fields[COL_SRC].toLowerCase().trim();
				String trlLower = fields[COL_TRL].toLowerCase().trim();
				if(!srcLower.isEmpty() && !trlLower.isEmpty())
					wordList.put(srcLower, trlLower);
			}
			r.close();
		}
		catch(IOException ex)
		{
			System.err.println("error while reading wordlist: " + ex.getMessage());
		}
	}
	
	/** Attempts to translate a text using the word list. */
	public String translate(String text)
	{
		StringBuilder res = new StringBuilder();
		StringTokenizer tk = new StringTokenizer(text, " \t\n\r\f", true);
		while(tk.hasMoreTokens()) {
			String t = tk.nextToken();
			if(Character.isWhitespace(t.charAt(0))) // just copy the whitespace tokens
				res.append(t);
			else {
				// process a word token
				String tLower = t.toLowerCase();
				boolean isTitle = Character.isUpperCase(t.charAt(0));
				String tr = wordList.get(tLower);
				if(tr != null) {
					// use translated word. handle case of first char.
					char first = tr.charAt(0);
					if(isTitle) first = Character.toUpperCase(first);
					else first = Character.toLowerCase(first);
					res.append(first);
					// just copy rest of translated word. 
					res.append(tr.substring(1));
				}
				else // just copy original word
					res.append(t);
			}
		}
		
		return res.toString();
	}

}
