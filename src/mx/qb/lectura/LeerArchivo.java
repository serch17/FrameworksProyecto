package mx.qb.lectura;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class LeerArchivo {
	
	
	public static void main(String [] args) {
		LeerArchivo la = new LeerArchivo();
		
		HashSet leerArchivo= la.leerArchivos("C:\\sergio\\SISEC");
		List listaImports = new ArrayList(leerArchivo);
		Collections.sort(listaImports);
		for(int i=0;i<listaImports.size();i++) {
			System.out.println(listaImports.get(i));
		}
		
	}
	
	public HashSet leerArchivos(String directorio) {
		HashSet<String> hset=null;
		hset= new HashSet<String>();
		
		try {
			BuscaDirectorios buscaDir= new BuscaDirectorios();
		
			List<String> directorios = buscaDir.buscaDirectorios(directorio);
		
			for (int i=0; i< directorios.size(); i++ ) {
				File f = new File(directorios.get(i));
				File[] archivos = f.listFiles();
				Scanner scan =null;
				for(File archivo:archivos) {
					
					try {
						if(archivo.isFile() && (archivo.getPath().lastIndexOf(".java")>0) ) {
							scan = new Scanner(archivo);
							while (scan.hasNextLine()) {
								String linea =scan.nextLine();
								if (linea.contains("import ")) {
									//System.out.println(linea);
									hset.add(linea);
								}
								
							}
						}else if(archivo.isFile() && (archivo.getName().equals("pom.xml" ))) {
							hset.add("existe un POM :: "+archivo.getPath());
						}else if (archivo.isFile() && (archivo.getName().equals("pom.xml" ))) {
							hset.add("existe un classpath :: "+archivo.getPath());
						}
						scan.close();
					}catch(Exception e) {
					}finally {
						
					}
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			
		}
		return hset;
		
	}
	
	

	
	

}
