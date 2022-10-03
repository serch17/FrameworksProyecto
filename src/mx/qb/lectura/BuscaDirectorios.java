package mx.qb.lectura;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BuscaDirectorios {
	
	public List<String> buscaDirectorios(String directorio){
		BuscaDirectorios la= new BuscaDirectorios();
		List<String> directorios = new ArrayList<String>();
		la.buscaDirectorios(directorio,directorios);
		return directorios;
		
	}
	
	private void buscaDirectorios(String directorio, List<String> directorios){
		File carpetaIni= new File(directorio);
		
		File[] fs = carpetaIni.listFiles();
		for(File f :fs) {
			if(f.isDirectory()) {
				//System.out.println(f.getPath());
				directorios.add(directorio);
				buscaDirectorios(f.getPath(),directorios);
			}
		}
		
	}
}
