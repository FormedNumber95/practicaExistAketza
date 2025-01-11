package es.aketzagonzalez.practicaExistAketza;

import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.modules.XPathQueryService;

public class Main {

	public static void main(String[] args) {
		try {
			String driver = "org.exist.xmldb.DatabaseImpl"; //Driver para eXist
			Collection col = null; // Colección
			String URI="xmldb:exist://localhost:8080/exist/xmlrpc/db/GIMNASIO"; //URI colección
			String usu="admin"; //Usuario
			String usuPwd=""; //Clave
			try {
				Class cl = Class.forName(driver); //Cargar del driver
				Database database = (Database) cl.newInstance(); //Instancia de la BD
				DatabaseManager.registerDatabase(database); //Registro del driver
			} catch (Exception e) {
				System.out.println("Error al inicializar la BD eXist");
				e.printStackTrace(); 
			}
			col = DatabaseManager.getCollection(URI, usu, usuPwd);
			if(col == null)
			System.out.println(" *** LA COLECCION NO EXISTE. ***");
			XPathQueryService servicio = (XPathQueryService) col.getService("XPathQueryService", "1.0");
			ResourceSet result = servicio.query ("for $em in /EMPLEADOS/EMP_ROW[DEPT_NO=10] return $em");
			// recorrer los datos del recurso.
			ResourceIterator i;
			i = result.getIterator();
			if (!i.hasMoreResources())
				System.out.println(" LA CONSULTA NO DEVUELVE NADA.");
			while (i.hasMoreResources()) {
				Resource r = i.nextResource();
				System.out.println((String) r.getContent());
			}
			col.close(); //borramos
		}catch(Exception e) {
			e.printStackTrace();
		}
	}// FIN verempleados10
}
