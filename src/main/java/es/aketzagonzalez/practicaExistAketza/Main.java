package es.aketzagonzalez.practicaExistAketza;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
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
			generarXML(col);
			col.close(); //borramos
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void generarXML(Collection col) throws XMLDBException {
		XPathQueryService servicio = (XPathQueryService) col.getService("XPathQueryService", "1.0");
		try {
		 DocumentBuilderFactory docFactory=DocumentBuilderFactory.
         		newInstance();
			DocumentBuilder docBuilder;
			docBuilder = docFactory.newDocumentBuilder();
			Document doc=docBuilder.newDocument();
			//añadir el datosPrincipal para que luego pueda mantenerse la estuctura de <datos>...</datos> por cada uso
			Element datosPrincipal=doc.createElement("datosPrincipal");
			doc.appendChild(datosPrincipal);
			
			String query = "for $uso in /USO_GIMNASIO/fila_uso return $uso";
	        ResourceSet result = servicio.query(query);
			ResourceIterator i;
			i = result.getIterator();
			if (!i.hasMoreResources()) {
				System.out.println(" LA CONSULTA NO DEVUELVE NADA.");
				return;
			}
			while (i.hasMoreResources()) {
				Element datos=doc.createElement("datos");
				datosPrincipal.appendChild(datos);
				Resource r = i.nextResource();
				String contenido = (String) r.getContent();
				//codigo del socio
				String codigoSocio=contenido.split("<CODSOCIO>")[1].split("</CODSOCIO>")[0];
				//nombre del socio
				String querySocio = "/SOCIOS_GIM/fila_socios[COD="+codigoSocio+"]";
				ResourceSet resultSocio = servicio.query(query);
				ResourceIterator iSocio;
				iSocio = resultSocio.getIterator();
				Resource rSocio = iSocio.nextResource();
				String contenidoSocio = (String) rSocio.getContent();
				String NombreSocio=contenido.split("<NOMBRE>")[0].split("</NOMBRE>")[0];
				//codigo de la actividad
				String codigoActividad=contenido.split("<CODACTIV>")[1].split("</CODACTIV>")[0];
				//nombre de la actividad
				String queryActividad = "for $uso in /SOCIOS_GIM/fila_socios[cod="+codigoSocio+"] return $uso";
				ResourceSet resultActividad = servicio.query(query);
				ResourceIterator iActividad;
				iActividad = resultActividad.getIterator();
				Resource rActividad = iActividad.nextResource();
				String contenidoActividad = (String) rSocio.getContent();
				String NombreActividad=contenido.split("<NOMBRE>")[0].split("</NOMBRE>")[0];
				//horas que ha estado
				String horaInicio=contenido.split("<HORAINICIO>")[1].split("</HORAINICIO>")[0];
				String horaFin=contenido.split("<HORAFINAL>")[1].split("</HORAFINAL>")[0];
				int horas=Integer.parseInt(horaFin)-Integer.parseInt(horaInicio);
				//tipo de la actividad
				//cuota
				aniadeElemento(doc, datos, "COD", codigoSocio);
				aniadeElemento(doc, datos, "NOMBRESOCIO", NombreSocio);
				aniadeElemento(doc, datos, "CODACTIV", codigoActividad);
				aniadeElemento(doc, datos, "NOMBREACTIVIDAD", NombreActividad);
				aniadeElemento(doc, datos, "horas", horas+"");
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult streamResult = new StreamResult(new File("src/main/resources/archivos/archivo.xml"));
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.transform(source, streamResult);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Aniade elemento.
	 *
	 * @param doc El doc
	 * @param rowElement El elemento padre
	 * @param header El header
	 * @param texto El texto
	 */
	private static void aniadeElemento(Document doc, Element rowElement, String header, String texto) {
		Element elemento=doc.createElement(header);
		elemento.appendChild(doc.createTextNode(texto));
		rowElement.appendChild(elemento);
	}
	
}
