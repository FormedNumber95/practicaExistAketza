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
			//consulta
			String query = "for $uso in /USO_GIMNASIO/fila_uso return $uso";
	        ResourceSet result = servicio.query(query);
			ResourceIterator i;
			i = result.getIterator();
			if (!i.hasMoreResources()) {
				System.out.println(" LA CONSULTA NO DEVUELVE NADA.");
				return;
			}
			while (i.hasMoreResources()) {
				String codigoSocio="";
				String nombreSocio="";
				String codigoActividad="";
				String nombreActividad="";
				String tipoActividad="";
				int cantidad;
				Element datos=doc.createElement("datos");
				datosPrincipal.appendChild(datos);
				Resource r = i.nextResource();
				String contenido = (String) r.getContent();
				//codigo del socio
				codigoSocio=contenido.split("<CODSOCIO>")[1].split("</CODSOCIO>")[0];
				//nombre del socio
				String querySocio = "/SOCIOS_GIM/fila_socios[COD='"+codigoSocio+"']/NOMBRE/text()";
				ResourceSet resultSocio = servicio.query(querySocio);
				ResourceIterator iSocio = resultSocio.getIterator();
				if (iSocio.hasMoreResources()) {
				    Resource rSocio = iSocio.nextResource();
				    String contenidoSocio = (String) rSocio.getContent();
				    nombreSocio = contenidoSocio.trim();
				}
				//codigo de la actividad
				codigoActividad=contenido.split("<CODACTIV>")[1].split("</CODACTIV>")[0];
				//nombre de la actividad);
				String queryActividad = "/ACTIVIDADES_GIM/fila_actividades[@cod="+codigoActividad+"]/NOMBRE/text()";
				ResourceSet resultActividad = servicio.query(queryActividad);
				ResourceIterator iActividad=resultActividad.getIterator();
				if (iActividad.hasMoreResources()) {
					Resource rActividad = iActividad.nextResource();
				    String contenidoActividad = (String) rActividad.getContent();
				    nombreActividad = contenidoActividad.trim();
				}
				//horas que ha estado
				String horaInicio=contenido.split("<HORAINICIO>")[1].split("</HORAINICIO>")[0];
				String horaFin=contenido.split("<HORAFINAL>")[1].split("</HORAFINAL>")[0];
				int horas=Integer.parseInt(horaFin)-Integer.parseInt(horaInicio);
				//tipo de la actividad
				String queryActividadTipo = "/ACTIVIDADES_GIM/fila_actividades[@cod='"+codigoActividad+"']/string(@tipo)";
				ResourceSet resultActividadTipo = servicio.query(queryActividadTipo);
				ResourceIterator iActividadTipo = resultActividadTipo.getIterator();
				if (iActividadTipo.hasMoreResources()) {
				    Resource rActividad = iActividadTipo.nextResource();
				    tipoActividad = rActividad.getContent().toString().trim();
				}
				//añadir
				aniadeElemento(doc, datos, "COD", codigoSocio);
				aniadeElemento(doc, datos, "NOMBRESOCIO", nombreSocio);
				aniadeElemento(doc, datos, "CODACTIV", codigoActividad);
				aniadeElemento(doc, datos, "NOMBREACTIVIDAD", nombreActividad);
				aniadeElemento(doc, datos, "horas", horas+"");
				switch (Integer.parseInt(tipoActividad)) {
				case 1:
					cantidad=0;
					aniadeElemento(doc, datos, "tipoact", "libre horario");
					break;
				case 2:
					cantidad=2;
					aniadeElemento(doc, datos, "tipoact", "grupo");
					break;
				default:
					cantidad=4;
					aniadeElemento(doc, datos, "tipoact", "alquila un espacio");
					break;
				}
				//añadir la cuota
				aniadeElemento(doc, datos, "cuota_adicional", (cantidad*horas)+"€");
				
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
