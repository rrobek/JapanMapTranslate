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


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
 

public class JapanMapTranslate 
{
	private static int verbose = 0; 
	private static boolean both = false; 
	private static boolean advanced = false; 
	private static String stat = null; 

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		
		boolean assignStat = false;  
		for(String arg : args)
		{
			if(assignStat) {
				stat = arg; 
				assignStat = false;
				continue;
			}
			if(arg.equals("-v") || arg.equals("-verbose") || arg.equals("--verbose"))
				verbose = 1;
			else if(arg.equals("-vv"))
				verbose = 2; 
			else if(arg.equals("-both") || arg.equals("--both"))
				both = true; 
			else if(arg.equals("-adv") || arg.equals("--adv"))
				advanced = true; 
			else if(arg.equals("-stat") || arg.equals("--stat"))
				assignStat = true; 
			else if(arg.equals("-h") || arg.equals("-help") || arg.equals("--help"))
				doHelp();
			else
				translate(arg);
		}

	}
	
	private static void doHelp()
	{
		System.out.println("Usage: java JapanMapTranslate [OPTIONS] [FILES...]");
		System.out.println("Available OPTIONS: ");
		System.out.println("  -both       Include original Japanese name in English name");
		System.out.println("  -adv        Also write a 'de' name containing both");
		System.out.println("  -v          Verbose output.");
		System.out.println("  -vv         Even more verbose output.");
		System.out.println("  -stat FILE  Write word occurrency statistics to FILE.");
		System.out.println("  -h          Print help message.");
		System.out.println("FILES are map data files in the OSM XML format.");
		System.out.println("Each file is loaded, Japanese place names are transliterated to English");
		System.out.println("and added as English name tags. The result is saved as FILENAME.tr.osm,");
		System.out.println("where FILENAME is the original map data file name. ");
		System.out.println();
	}

	private static void translate(String arg) {
		System.out.println("Transliterate " + arg);
		
		String outfile = arg + ".tr.osm";
		
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		
		try {
			// Set up file output
			FileOutputStream fos = new FileOutputStream(outfile);
			OutputStreamWriter out = new OutputStreamWriter(fos, "UTF8");
			// Use an instance of ourselves as the SAX event handler
			Translater tr = new Translater(out);
			tr.setVerbose(verbose);
			tr.setBoth(both);
			tr.setAdvanced(advanced);
			if(stat != null) tr.enableStats(stat);
			
		
		    // Parse the input
		    SAXParser saxParser = factory.newSAXParser();
		    saxParser.parse(new File(arg), tr);
		    
		    out.flush();
		    out.close();
		    fos.close();
		    
		    // Output statistics. 
		    System.out.println("  Result: " + tr.getNumSuccess() + " names transliterated successfully, " 
		    		+ tr.getNumPartial() + " partial, " + tr.getNumFailed() + " failed, " + tr.getNumEnglish() + " English names used");
		    
		} catch (Throwable t) {
		    t.printStackTrace();
		}
	}

}
