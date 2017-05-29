/*
This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

*/
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import es.us.isa.FAMA.models.FAMAfeatureModel.fileformats.SPLXReader;
import es.us.isa.FAMA.models.FAMAfeatureModel.fileformats.XMLWriter;
import main.Loader;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.MasonTagTypes;
import net.htmlparser.jericho.MicrosoftTagTypes;
import net.htmlparser.jericho.Source;

public class SPLOTLoader implements Loader{
	String sourceUrlString = "http://52.32.1.180:8080/SPLOT/SplotAnalysesServlet?action=select_model&enableSelection=false&&showModelDetails=true";
	
	
	@Override
	public Collection<File> loadFiles() {
		Collection<File> res = new LinkedList<File>();
		if (sourceUrlString.indexOf(':') == -1)
			sourceUrlString = "file:" + sourceUrlString;
		MicrosoftTagTypes.register();
		MasonTagTypes.register();
		Source source;
		try {
			source = new Source(new URL(sourceUrlString));
			List<Element> linkElements = source.getAllElements(HTMLElementName.A);
			
			for (Element linkElement : linkElements) {
				String href = linkElement.getAttributeValue("href");
				if (href == null)
					continue;
				// A element can contain other tags so need to extract the text from
				// it:
				//String label = linkElement.getContent().getTextExtractor().toString();
				if (!href.contains("javascript:")) {
					href = href.substring(href.indexOf("modelFile=") + 10);
					res.add(procesaDescarga(href));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
					
		return res;
	}

	@Override
	public String getFormat() {
		return "SPLX";
	}

	@Override
	public File getOriginData() {
		
		if (sourceUrlString.indexOf(':') == -1)
			sourceUrlString = "file:" + sourceUrlString;
		MicrosoftTagTypes.register();
		MasonTagTypes.register();
		Source source;
		File res = null;
		try {
			source = new Source(new URL(sourceUrlString));
			res= new File("out/data.csv");
			PrintWriter out = new PrintWriter(res);

			List<Element> tableElements = source.getAllElements(HTMLElementName.TR);
			for(Element tr:tableElements){
				List<Element> fila=tr.getChildElements();
				for(Element cell:fila){
					out.print(cell.getContent().getTextExtractor().toString().replaceAll(cell.getFirstStartTag().toString(), "").replaceAll(cell.getEndTag().toString(), "")+";");
				}
				out.println();
				
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return res;
	}
	
	

	private static File procesaDescarga(String href) throws Exception {
		URL url = new URL("http://52.32.1.180:8080/SPLOT/models/"+href);
		
		// establecemos conexion
		URLConnection urlCon = url.openConnection();

		// Sacamos por pantalla el tipo de fichero
//		System.out.println(urlCon.getContentType());

		// Se obtiene el inputStream de la web y se abre el fichero
		// local.
		InputStream is = urlCon.getInputStream();
		File file = new File("./out/"+href+".splx");
		FileOutputStream fos = new FileOutputStream(file);
		
		byte[] array = new byte[4*1024]; // buffer temporal de lectura.
		int leido = is.read(array);
		while (leido > 0) {
			fos.write(array, 0, leido);
			leido = is.read(array);
		}

		// cierre de conexion y fichero.
		is.close();
		fos.close();
		
		SPLXReader reader = new SPLXReader();
		XMLWriter writer = new XMLWriter();
		writer.writeFile("./splot/"+href, reader.parseFile("./out/"+href+".splx"));
		return file;
	}



}
