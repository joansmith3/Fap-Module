package generator.utils;

import es.fap.simpleled.led.Accion
import es.fap.simpleled.led.Attribute;
import es.fap.simpleled.led.Boton
import es.fap.simpleled.led.Campo;
import es.fap.simpleled.led.CampoAtributos;
import es.fap.simpleled.led.Entity
import es.fap.simpleled.led.Form
import es.fap.simpleled.led.MenuEnlace
import es.fap.simpleled.led.Enlace
import es.fap.simpleled.led.Pagina
import es.fap.simpleled.led.PaginaAccion
import es.fap.simpleled.led.Permiso
import es.fap.simpleled.led.Popup
import es.fap.simpleled.led.Tabla
import es.fap.simpleled.led.util.ModelUtils;
import es.fap.simpleled.led.LedPackage;
import es.fap.simpleled.led.LedFactory;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource

import templates.GForm
import templates.GPagina
import templates.GPopup
import es.fap.simpleled.led.util.LedEntidadUtils;
import es.fap.simpleled.led.util.LedCampoUtils;

public class Controller {
	
	// Lista de atributos que tienen que recibirse como parámetros
	public EObject container; // Elemento Popup, Pagina o Form
	public boolean index;
	public boolean crear;
	public boolean editar;
	public boolean borrar;
	public List<Object> saveController;
	public CampoUtils campo;
	public String renderView;
	public Permiso permiso;
	public String controllerGenName;
	public String controllerName;
	public String controllerGenFullName;
	public String controllerFullName;
	public String url;
	public String packageName;
	public String packageGenName;
	public boolean noBorrarEntidad;
	public boolean noAutenticar;
	public String name;
	public List<String> saveExtra;
	public List<String> saveCode;
	public List<String> saveBoton;
	public List<EntidadUtils> saveEntities;
	public List<EntidadUtils> indexEntities;
	public EObject elementoGramatica;
	
	// Lista de atributos que son calculados en esta clase
	private EntidadUtils almacen;
	private EntidadUtils almacenNoSingle;  // nulo() retorna true cuando el almacen es Singleton
	private EntidadUtils entidad;
	private EntidadUtils entidadNoSingle;  // nulo() retorna true cuando la entidad es Singleton
	private EntidadUtils entidadPagina;		// Si no hay campos guardables nulo() sera true
	private List<EntidadUtils> intermedias;
	private boolean hayTabla;
	private boolean hayAnterior;
	private boolean xToMany;
	private EntidadUtils entidadSiTabla;  // entidad vale null si no hay una tabla en la pagina o popup
	private String nameEditar;
	private String sufijoPermiso;
	private String sufijoBoton;
	private List<AlmacenEntidad> campos;  // No incluye el ultimo
	private List<AlmacenEntidad> camposTodos;
	private Accion accionCrear;
	private Accion accionEditar;
	private Accion accionBorrar;
	
	public Controller initialize(){
		campos = calcularSubcampos(campo?.campo);
		camposTodos = new ArrayList<AlmacenEntidad>(campos);
		if (campos.size() > 0){
			campo = campos.get(campos.size() - 1).campo;
			campos.remove(campos.size() - 1);
		}
		intermedias = new ArrayList<EntidadUtils>();
		for (AlmacenEntidad subcampo: campos)
			intermedias.add(subcampo.almacen);
		
		hayTabla = hayTabla(container);
		hayAnterior = hayAnterior(container);
			
		entidad = EntidadUtils.create(campo?.getUltimaEntidad());
		entidadPagina = EntidadUtils.create(entidad.entidad);
		if (!LedCampoUtils.hayCamposGuardables(container))
			entidadPagina = EntidadUtils.create((Entity)null);
		almacen = EntidadUtils.create((Entity)null);
		
		entidadSiTabla = EntidadUtils.create((Entity)null);
		
		if (campo?.campo?.atributos != null){
			almacen = EntidadUtils.create(campo.getEntidad());
			if (LedEntidadUtils.xToMany(campo?.getUltimoAtributo()))
				xToMany = true;
			else
				xToMany = false;
		}
		
		nameEditar = "editar";
		sufijoPermiso = "";
		sufijoBoton = "";
		if (isForm()){
			nameEditar = name;
			sufijoPermiso = StringUtils.firstUpper(name);
			sufijoBoton = StringUtils.firstUpper(name);
		}
			
		if (hayTabla)
			entidadSiTabla = EntidadUtils.create(entidad.entidad);
			
		for (int i = 0; i < saveEntities.size(); i++){
			if (saveEntities.get(i).equals(entidad)){
				saveEntities.remove(i);
				break;
			}
		}
		for (int i = 0; i < indexEntities.size(); i++){
			if (indexEntities.get(i).equals(entidad)){
				indexEntities.remove(i);
				break;
			}
		}
		almacenNoSingle = EntidadUtils.create(almacen.entidad);
		if (almacen.isSingleton()){
			almacenNoSingle.entidad = null;
		}
		entidadNoSingle = EntidadUtils.create(entidad.entidad);
		if (entidad.isSingleton()){
			entidadNoSingle.entidad = null;
		}
		return this;
	}
	
	public void controller(){
		String withSecure = "";
		if (!noAutenticar)
			withSecure = "@With(CheckAccessController.class)"
			
		String controllerGen = """
package ${packageGenName};

import play.*;
import play.mvc.*;
import play.db.jpa.Model;
import controllers.fap.*;
import validation.*;
import messages.Messages;
import messages.Messages.MessageType;

import tables.TableRecord;
import models.*;
import tags.ReflectionUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.MalformedURLException;
import java.net.URL;

${withSecure}
public class ${controllerGenName} extends GenericController {

${metodoIndex()}

${metodoEditar()}

${metodoCrear()}

${metodoBorrar()}

${validateCopyMethod()}

${metodoPermiso()}

${metodoEditarRender()}

${metodoCrearRender()}

${metodoBorrarRender()}

${metodoEditarValidateRules()}

${metodoCrearValidateRules()}

${metodoBorrarValidateRules()}

${botonesMethods()}

${getters()}

${metodoCheckRedirigir()}

${metodosHashStack()}

${beforeMethod()}

}
"""
		
		FileUtils.overwrite(FileUtils.getRoute('CONTROLLER_GEN'),controllerGenFullName.replaceAll("\\.", "/") + ".java", controllerGen);
			
		String controller = """
package ${packageName};

import ${packageGenName}.${controllerGenName};
			
public class ${controllerName} extends ${controllerGenName} {

}
		"""
		FileUtils.write(FileUtils.getRoute('CONTROLLER'), controllerFullName.replaceAll("\\.", "/") + ".java", controller);
	}
	
	public String generateRoutes(){
		StringBuffer sb = new StringBuffer();
		if (index)
			StringUtils.appendln sb, Route.to("GET", url, controllerFullName + ".index");
		if (editar || isForm())
			StringUtils.appendln sb, Route.to("POST", url + "/${nameEditar}", controllerFullName + ".${nameEditar}");
		if (borrar)
			StringUtils.appendln sb, Route.to("POST", url + "/borrar", controllerFullName + ".borrar");
		if (crear)
			StringUtils.appendln sb, Route.to("POST", url + "/crear", controllerFullName + ".crear");
		return sb.toString();
	}
	
	private String getters(){
		String getters = "";
		if(almacen.entidad != null){
			for (AlmacenEntidad subcampo: camposTodos)
				getters += ControllerUtils.complexGetter(subcampo.almacen, subcampo.entidad, subcampo.campo);
			getters += ControllerUtils.simpleGetter(camposTodos.get(0).almacen, true);
			if(hayTabla && crear && !almacen.isSingleton())
				getters += ControllerUtils.simpleGetter(entidad, true);
		}
		else{
			getters += ControllerUtils.simpleGetter(entidad, true);
		}
		if (!hayTabla && !entidad.isSingleton())
			getters += ControllerUtils.simpleGetter(entidad, false); // get sin parametros, que dentro hace un new()
		for (EntidadUtils entity: saveEntities)
			getters += ControllerUtils.simpleGetter(entity, false);
		for (EntidadUtils entity: indexEntities)
			getters += ControllerUtils.simpleGetter(entity, false);
		return getters;
	}
	
	private String metodoIndex(){
		String guardarAlCrear = "";
		if (hayTabla && !entidadNoSingle.nulo()){
			guardarAlCrear = """
				${entidad.variable}.save();
				${entidad.id} = ${entidad.variable}.id;
				((Map<String, Long>)tags.TagMapStack.top("idParams")).put("${entidad.id}", ${entidad.id});
			"""
		}
		EntidadUtils primerAlmacen = camposTodos.size() > 0? camposTodos.get(0).almacen : EntidadUtils.create((Entity)null);
		return """
			public static void index(${StringUtils.params(
				"String accion",
				intermedias.collect{it.typeId},
				almacenNoSingle.typeId,
				entidadNoSingle.typeId
			)}){
				if (!secure.checkAction(accion)){
					if (accion != null)
						Messages.warning("La acción especificada en la url no es válida. Se utilizará la acción por defecto.");
					accion = "editar";
				}
				${hayAnterior? "checkRedirigir();" : ""}
				${entidad.entidad? "$entidad.clase $entidad.variable = null;" : ""}
				if(accion.equals("crear")){
					${ControllerUtils.newCall(entidad)}
					${guardarAlCrear}
				}
				else if (!accion.equals("borrado")){
					${entidad.entidad? "$entidad.variable = ${ControllerUtils.complexGetterCall(almacen, entidad)};" : ""}
				}
				${primerAlmacen.entidad? "$primerAlmacen.clase $primerAlmacen.variable = ${ControllerUtils.simpleGetterCall(primerAlmacen, true)};" : ""}
				${campos.collect{"$it.entidad.clase $it.entidad.variable = ${ControllerUtils.complexGetterCall(it.almacen, it.entidad)};"}.join("\n")}
				${indexEntities.collect{"$it.clase $it.variable = ${ControllerUtils.simpleGetterCall(it, false)};"}.join("\n")}
				${saveEntities.collect{"$it.clase $it.variable = ${ControllerUtils.simpleGetterCall(it, false)};"}.join("\n")}
				if (!permiso(accion)){
					Messages.fatal("${permiso?.mensaje? permiso.mensaje : "No tiene permisos suficientes para realizar esta acción"}");
				}
				renderTemplate(${StringUtils.params(
					renderView,
					"accion",
					intermedias.collect{it.variable},
					almacenNoSingle.id,
					almacen.variable,
					entidadNoSingle.id,
					entidad.variable,
					indexEntities.collect{it.variable},
					saveEntities.collect{it.variable}
				)});
			}
		"""
	}
	
	private String metodoEditar(){
		String metodoEditar = "";
		if (editar || isForm()) {
			String editarRenderCall = "${nameEditar}Render(${StringUtils.params(intermedias.collect{it.id}, almacenNoSingle.id, entidadNoSingle.id)});";
			String botonCode = "";
			List<String> saveBotones = new ArrayList<String>(saveBoton);
			if (isForm() && saveBotones.size() == 1){
				saveBotones.clear();
			}
			else if (saveBotones.size() > 0){
				boolean primero = true;
				for (String boton : saveBotones) {
					if (primero == true) {
						botonCode += "if (${boton} != null) {";
						primero = false;
					}
					else
						botonCode += "else if (${boton} != null) {"
						botonCode += """
							${StringUtils.firstLower(boton)}${sufijoBoton}(${StringUtils.params(
								intermedias.collect{it.id}, almacenNoSingle.id, entidad.id,
								entidadPagina.variable, saveEntities.collect{it.variable}, saveExtra.collect{it.split(" ")[1]}
							)});
							${editarRenderCall}
						}
					""";
				}
			}
			metodoEditar = """
				public static void ${nameEditar}(${StringUtils.params(
					intermedias.collect{it.typeId},
					almacenNoSingle.typeId,
					entidadNoSingle.typeId,
					entidadPagina.typeVariable,
					saveEntities.collect{it.typeVariable},
					saveExtra,
					saveBotones.collect{"String ${it}"}
				)}){
					checkAuthenticity();
					if(!permiso${sufijoPermiso}("editar")){
						Messages.error("${permiso?.mensaje? permiso.mensaje : "No tiene permisos suficientes para realizar la acción"}");
					}
					${botonCode}
					${!entidad.nulo()? "${entidad.clase} $entidad.variableDb = ${ControllerUtils.complexGetterCall(almacen, entidad)};":""}
					${ControllerUtils.fullGetterCall(EntidadUtils.create(null), EntidadUtils.create(null), saveEntities)}
					if(!Messages.hasErrors()){
						${editar && !entidadPagina.nulo()? ControllerUtils.validateCopyCall("\"editar\"", this, entidad, saveEntities) : ""}
					}
					if(!Messages.hasErrors()){
						${nameEditar}ValidateRules(${StringUtils.params(
							entidad.variableDb, saveEntities.collect{it.variableDb}, entidadPagina.variable,
							saveEntities.collect{it.variable}, saveExtra.collect{it.split(" ")[1]}
						)});
					}
					if(!Messages.hasErrors()){
						${entidadPagina.entidad? "${entidad.variableDb}.save();" : ""}
						${saveEntities.collect{"${it.variableDb}.save();"}.join("\n")}
					}
					${editarRenderCall}
				}
			"""
		}
		return metodoEditar;
	}
	
	private String metodoCrear(){
		String metodoCrear = "";
		if (crear) {
			AlmacenEntidad ultimoAlmacen;
			if (camposTodos.size() > 0)
				ultimoAlmacen = camposTodos.get(camposTodos.size() - 1);
			else
				ultimoAlmacen = new AlmacenEntidad();
			String crearSaveCall = """
				${entidad.variableDb}.save();
				${entidad.id} = ${entidad.variableDb}.id;
			""";
			if(almacen.entidad != null){
				if (xToMany)
					crearSaveCall += "db${campo.str}.add(${entidad.variableDb});\n";
				else
					crearSaveCall += "db${campo.str} = ${entidad.variableDb};\n";
				crearSaveCall += "${almacen.variableDb}.save();\n";
			}
			metodoCrear = """
				public static void crear(${StringUtils.params(
					intermedias.collect{it.typeId},
					almacenNoSingle.typeId,
					entidadPagina.typeVariable, 
					entidadSiTabla.typeId,
					saveEntities.collect{it.typeVariable},
					saveExtra
				)}){
					checkAuthenticity();
					if(!permiso("crear")){
						Messages.error("${permiso?.mensaje? permiso.mensaje : "No tiene permisos suficientes para realizar la acción"}");
					}
					${entidad.clase} ${entidad.variableDb} = null;
					if(!Messages.hasErrors()){
						${entidad.variableDb} = ${ControllerUtils.simpleGetterCall(entidad, hayTabla)};
					}
					${ControllerUtils.almacenGetterCall(ultimoAlmacen);}
					${ControllerUtils.fullGetterCall(EntidadUtils.create(null), EntidadUtils.create(null), saveEntities)}
					if(!Messages.hasErrors()){
						${!entidadPagina.nulo()? ControllerUtils.validateCopyCall("\"crear\"", this, entidad, saveEntities) : ""}
					}
					if(!Messages.hasErrors()){
						crearValidateRules(${StringUtils.params(
							entidad.variableDb, saveEntities.collect{it.variableDb}, entidadPagina.variable,
							saveEntities.collect{it.variable}, saveExtra.collect{it.split(" ")[1]}
						)});
					}
					${entidadSiTabla.nulo()? "${entidad.typeId} = null;" : ""}
					if(!Messages.hasErrors()){
						$crearSaveCall
						${saveEntities.collect{"${it.variableDb}.save();"}.join("\n")}
					}
					crearRender(${StringUtils.params(intermedias.collect{it.id}, almacenNoSingle.id, entidad.id)});
				}
			"""
		}
		return metodoCrear;
	}
	
	private String metodoBorrar(){
		String metodoBorrar = "";
		if (borrar){
			AlmacenEntidad ultimoAlmacen;
			if (camposTodos.size() > 0)
				ultimoAlmacen = camposTodos.get(camposTodos.size() - 1);
			else
				ultimoAlmacen = new AlmacenEntidad();
			def borrarEntidad = "";
			if(almacen.entidad != null){
				if (xToMany)
					borrarEntidad += "db${campo.str}.remove(${entidad.variableDb});\n";
				else
					borrarEntidad += "db${campo.str} = null;\n";
				borrarEntidad += "${almacen.variableDb}.save();\n";
			}
			if (!noBorrarEntidad) {
				borrarEntidad += """
					${borrarListasXToMany()}
					${entidad.variableDb}.delete();
				""";
			}
			metodoBorrar = """
				public static void borrar(${StringUtils.params(intermedias.collect{it.typeId}, almacenNoSingle.typeId, entidad.typeId)}){
					checkAuthenticity();
					if(!permiso("borrar")){
						Messages.error("${permiso?.mensaje? permiso.mensaje : "No tiene permisos suficientes para realizar la acción"}");
					}

					${ControllerUtils.almacenGetterCall(ultimoAlmacen);}
					${entidad.clase} ${entidad.variableDb} = ${ControllerUtils.complexGetterCall(almacen, entidad)};
					if(!Messages.hasErrors()){
						borrarValidateRules(${StringUtils.params(entidad.variableDb)});
					}
					if(!Messages.hasErrors()){
						$borrarEntidad
					}

					borrarRender(${StringUtils.params(intermedias.collect{it.id}, almacenNoSingle.id, entidad.id)});
				}
			"""
		}
		return metodoBorrar;
	}
	
	private String metodoEditarRender(){
		String metodoEditarRender = "";
		if (editar || isForm()) {
			String redirectMethod = "\"${controllerFullName}.index\"";
			String redirectMethodOk = redirectMethod;
			String redirectActionOk = "\"editar\"";
			if (accionEditar.redirigir != null){
				redirectMethodOk = "\"${new GPagina(accionEditar.redirigir.pagina).controllerName()}.index\"";
				redirectActionOk = "\"${Actions.getAccion(accionEditar.redirigir.accion)}\"";
			}
			String redirigirOk = "redirect(${StringUtils.params(redirectMethodOk, redirectActionOk, intermedias.collect{it.id}, almacenNoSingle.id, entidadNoSingle.id)});";
			if (accionEditar.redirigirUrl)
				redirigirOk = "redirect(\"${accionEditar.redirigirUrl}\");";
			else if (accionEditar.anterior)
				redirigirOk = redirigirOkCode(redirigirOk);
			String mensajeEditadoOk;
			if (isPopup())
				mensajeEditadoOk = """renderJSON(utils.RestResponse.ok("${accionEditar.mensajeOk}"));""";
			else
				mensajeEditadoOk = """Messages.ok("${accionEditar.mensajeOk}");""";
			metodoEditarRender = """
				@Util
				public static void ${nameEditar}Render(${StringUtils.params(intermedias.collect{it.typeId}, almacenNoSingle.typeId, entidadNoSingle.typeId)}){
					if (!Messages.hasMessages()) {
						${mensajeEditadoOk}
						Messages.keep();
						${redirigirOk}
					}
					Messages.keep();
					redirect(${StringUtils.params(redirectMethod, "\"editar\"", intermedias.collect{it.id}, almacenNoSingle.id, entidadNoSingle.id)});
				}
			"""
		}
		return metodoEditarRender;
	}

	private String metodoCrearRender(){
		String metodoCrearRender = "";
		if (crear) {
			String redirectMethod = "\"${controllerFullName}.index\"";
			String redirectMethodOk = redirectMethod;
			String redirectActionOk = "\"editar\"";
			if (accionCrear.redirigir != null){
				redirectMethodOk = "\"${new GPagina(accionCrear.redirigir.pagina).controllerName()}.index\"";
				redirectActionOk = "\"${Actions.getAccion(accionCrear.redirigir.accion)}\"";
			}
			String redirigirOk = "redirect(${StringUtils.params(redirectMethodOk, redirectActionOk, intermedias.collect{it.id}, almacenNoSingle.id, entidad.id)});";
			if (accionCrear.redirigirUrl)
				redirigirOk = "redirect(\"${accionCrear.redirigirUrl}\");";
			else if (accionCrear.anterior)
				redirigirOk = redirigirOkCode(redirigirOk);
			String mensajeCreadoOk;
			if (isPagina()){
				mensajeCreadoOk = """
					Messages.ok("${accionCrear.mensajeOk}");
					Messages.keep();
					${redirigirOk}
				""";
			}
			else if (isPopup())
				mensajeCreadoOk = """renderJSON(utils.RestResponse.ok("${accionCrear.mensajeOk}"));""";
			metodoCrearRender = """
				@Util
				public static void crearRender(${StringUtils.params(intermedias.collect{it.typeId}, almacenNoSingle.typeId, entidad.typeId)}){
					if (!Messages.hasMessages()) {
						${mensajeCreadoOk}
					}
					Messages.keep();
					redirect(${StringUtils.params(redirectMethod, '"crear"', intermedias.collect{it.id}, almacenNoSingle.id, entidad.id)});
				}
			"""
		}
		return metodoCrearRender;
	}
	
	private String metodoBorrarRender(){
		String metodoBorrarRender = "";
		if (borrar) {
			String redirectMethod = "\"${controllerFullName}.index\"";
			String redirectMethodOk = redirectMethod;
			String redirectActionOk = "\"borrado\"";
			if (accionBorrar.redirigir != null){
				redirectMethodOk = "\"${new GPagina(accionBorrar.redirigir.pagina).controllerName()}.index\"";
				redirectActionOk = "\"${Actions.getAccion(accionBorrar.redirigir.accion)}\"";
			}
			String redirigirOk = "redirect(${StringUtils.params(redirectMethodOk, redirectActionOk, intermedias.collect{it.id}, almacenNoSingle.id)});";
			if (accionBorrar.redirigirUrl)
				redirigirOk = "redirect(\"${accionBorrar.redirigirUrl}\");";
			else if (accionBorrar.anterior)
				redirigirOk = redirigirOkCode(redirigirOk);
			String mensajeBorradoOk;
			if (isPagina())
				mensajeBorradoOk = """Messages.ok("${accionBorrar.mensajeOk}");""";
			else if (isPopup())
				mensajeBorradoOk = """renderJSON(utils.RestResponse.ok("${accionBorrar.mensajeOk}"));""";
			metodoBorrarRender = """
				@Util
				public static void borrarRender(${StringUtils.params(intermedias.collect{it.typeId}, almacenNoSingle.typeId, entidad.typeId)}){
					if (!Messages.hasMessages()) {
						${mensajeBorradoOk}
						Messages.keep();
						${redirigirOk}
					}
					Messages.keep();
					redirect(${StringUtils.params(redirectMethod, '"borrar"', intermedias.collect{it.id}, almacenNoSingle.id, entidad.id)});
				}
			"""
		}
		return metodoBorrarRender;
	}
		
	private String redirigirOkCode(String redirigirOk){
		return """
			if (request.cookies.containsKey("redirigir${name}"))
				redirect(request.cookies.get("redirigir${name}").value);
			else
				${redirigirOk}
		""";
	} 
	
	private String metodoCheckRedirigir(){
		if (!hayAnterior)
			return "";
		return """
			@Util
			public static void checkRedirigir(){
				renderArgs.put("container", "${name}");
				if (request.params.data.containsKey("redirigir") && "anterior".equals(request.params.data.get("redirigir")[0])){
					if (request.headers.get("referer") != null){
						String referer = request.headers.get("referer").value();
						try {
							URL url = new URL(referer);
							String refererHost = url.getHost();
							if (url.getPort() != -1)
								refererHost += ":" + url.getPort();
							else
								refererHost += ":80";
							String host = request.host;
							if (host.indexOf(":") == -1)
								host += ":80";
							if (refererHost.equals(host))
								response.setCookie("redirigir${name}", referer.replaceFirst("redirigir=anterior", "redirigir=no"));
						} catch (MalformedURLException e) {}
					}
				}
			}
		""";
	}
	
	private String metodoEditarValidateRules(){
		String metodoEditarValidateRules = "";
		if (editar || isForm()) {
			metodoEditarValidateRules = """
				@Util
				protected static void ${nameEditar}ValidateRules(${StringUtils.params(
					entidad.typeDb,
					saveEntities.collect{it.typeDb},
					entidadPagina.typeVariable,
					saveEntities.collect{it.typeVariable},
					saveExtra
				)}){
					//Sobreescribir para validar las reglas de negocio
				}
			""";
		}
		return metodoEditarValidateRules;
	}
	
	private String metodoCrearValidateRules(){
		String metodoCrearValidateRules = "";
		if (crear) {
			metodoCrearValidateRules = """
				@Util
				protected static void crearValidateRules(${StringUtils.params(
					entidad.typeDb,
					saveEntities.collect{it.typeDb},
					entidadPagina.typeVariable,
					saveEntities.collect{it.typeVariable},
					saveExtra
				)}){
					//Sobreescribir para validar las reglas de negocio
				}
			""";
		}
		return metodoCrearValidateRules;
	}
	
	private String metodoBorrarValidateRules(){
		String metodoBorrarValidateRules = "";
		if (borrar) {
			metodoBorrarValidateRules = """
				@Util
				protected static void borrarValidateRules(${StringUtils.params(entidad.typeDb)}){
					//Sobreescribir para validar las reglas de negocio
				}
			""";
		}
		return metodoBorrarValidateRules;
	}
	
	private String metodoPermiso(){
		return """
			@Util
			protected static boolean permiso${sufijoPermiso}(String accion) {
				${ControllerUtils.permisoContent(permiso)}
			}
		"""
	}
	
	private String metodosHashStack(){
		String controllerHS = "";
		for(elemento in saveController){
			controllerHS += elemento.controller();
		}
		return controllerHS;
	}
	
	private String beforeMethod(){
		return """
			@Before
			static void beforeMethod() {
				renderArgs.put("controllerName", "${controllerGenName}");
			}
		""";
	}	
	
	private String validateCopyMethod(){
		if ((editar || crear) && !entidadPagina.nulo()){
			return "${ControllerUtils.validateCopyMethod(this, entidad, saveEntities)}";
		}
		return "";
	}
	
	private String botonesMethods(){
		String botonesMethod = "";
		List<String> saveBotones = new ArrayList<String>(saveBoton);
		if (isForm() && saveBotones.size() == 1){
			saveBotones.clear();
		}
		for (String boton: saveBotones){
			botonesMethod += """
				@Util
				public static void ${StringUtils.firstLower(boton)}${sufijoBoton}(${StringUtils.params(
					intermedias.collect{it.typeId},
					almacenNoSingle.typeId,
					entidad.typeId,
					entidad.typeVariable,
					saveEntities.collect{it.typeVariable},
					saveExtra
				)}){
					//Sobreescribir este método para asignar una acción
				}
			""";
		}
		return botonesMethod;
	}
	
	private static boolean hayAnterior(Object o){
		if(o instanceof Popup || o instanceof Pagina){
			if (o.eContainer().menu){
				if (hayAnterior(o.eContainer().menu))
					return true;
			}
		}
		if(o.metaClass.respondsTo(o,"getElementos")){
			for (Object elemento: o.elementos){
				if (hayAnterior(elemento))
					return true;
			}
			return false;
		}
		if(o instanceof Accion || o instanceof Boton || o instanceof Enlace || o instanceof MenuEnlace){
			if (o.anterior)
				return true;
		}
	}
	
	private static boolean hayTabla(Object o){
		if(o.metaClass.respondsTo(o,"getElementos")){
			for (Object elemento: o.elementos){
				if (hayTabla(elemento))
					return true;
			}
			return false;
		}
		if(o instanceof Tabla)
			return true;
	}
	
	public boolean isPopup(){
		return container instanceof Popup;
	}
	
	public boolean isPagina(){
		return container instanceof Pagina;
	}
	
	public boolean isForm(){
		return container instanceof Form;
	}
	
	public static Controller fromPagina(Pagina pagina){
		GPagina pag = new GPagina(pagina);
		Controller controller = new Controller();
		controller.createOpcionesAccion(pagina);
		controller.container = pagina;
		controller.index = true;
		controller.findPaginaReferencias(pagina);
		controller.crear = controller.crear && (pag.hasForm || controller.accionCrear.crearSiempre);
		controller.editar = (controller.editar || controller.crear) && (pag.hasForm || controller.accionEditar.crearSiempre);
		controller.borrar = controller.borrar && (pag.hasForm || controller.accionBorrar.crearSiempre);
		controller.saveController = [];
		controller.campo = pag.campo;
		controller.renderView = "\"gen/${pagina.name}/${pagina.name}.html\"";
		controller.permiso = pagina.permiso;
		controller.controllerGenName = pag.controllerGenName();
		controller.controllerName = pag.controllerName();
		controller.controllerGenFullName = pag.controllerGenFullName();
		controller.controllerFullName = pag.controllerFullName();
		controller.url = pag.url();
		controller.packageName = "controllers";
		controller.packageGenName = "controllers.gen";
		controller.noBorrarEntidad = controller.accionBorrar?.noBorrarEntidad? true : false;
		controller.noAutenticar = pagina.noAutenticar;
		controller.name = pagina.name;
		controller.saveExtra = [];
		controller.saveCode = [];
		controller.saveEntities = [];
		controller.indexEntities = [];
		controller.elementoGramatica = pagina;
		return controller;
	}
	
	public static Controller fromPopup(Popup popup){
		GPopup gpopup = new GPopup(popup);
		Controller controller = new Controller();
		controller.createOpcionesAccion(popup);
		controller.container = popup;
		controller.index = true;
		controller.findPopupReferencias(popup);
		controller.crear = controller.crear || controller.accionCrear.crearSiempre;
		controller.editar = controller.editar || controller.accionEditar.crearSiempre;
		controller.borrar = controller.borrar || controller.accionBorrar.crearSiempre;
		controller.saveController = [];
		controller.campo = gpopup.campo;
		controller.renderView = "\"gen/popups/${gpopup.viewName()}\"";
		controller.permiso = popup.permiso;
		controller.controllerGenName = gpopup.controllerGenName();
		controller.controllerName = gpopup.controllerName();
		controller.controllerGenFullName = gpopup.controllerGenFullName();
		controller.controllerFullName = gpopup.controllerFullName();
		controller.url = gpopup.url();
		controller.packageName = "controllers.popups";
		controller.packageGenName = "controllers.gen.popups";
		controller.noBorrarEntidad = controller.accionBorrar?.noBorrarEntidad? true : false;
		controller.noAutenticar = false;
		controller.name = popup.name;
		controller.saveExtra = [];
		controller.saveCode = [];
		controller.saveEntities = [];
		controller.indexEntities = [];
		controller.elementoGramatica = popup;
		return controller;
	}
	
	public static Controller fromForm(Form form){
		GForm gform = new GForm(form);
		EObject container = LedCampoUtils.getElementosContainer(form);
		Controller containerController;
		if (container instanceof Pagina)
			containerController = Controller.fromPagina(container);
		else if (container instanceof Popup)
			containerController = Controller.fromPopup(container);
		Controller controller = new Controller();
		controller.createOpcionesAccion(null);
		controller.container = form;
		controller.editar = true;
		controller.saveController = [];
		controller.campo = containerController.campo;
		controller.renderView = containerController.renderView;
		controller.permiso = form.permiso;
		controller.controllerGenName = containerController.controllerGenName;
		controller.controllerName = containerController.controllerName;
		controller.controllerGenFullName = containerController.controllerGenFullName;
		controller.controllerFullName = containerController.controllerFullName;
		controller.url = containerController.url;
		controller.packageName = containerController.packageName;
		controller.packageGenName = containerController.packageGenName;
		controller.noBorrarEntidad = containerController.noBorrarEntidad;
		controller.noAutenticar = containerController.noAutenticar;
		controller.name = StringUtils.firstLower(form.name);
		controller.saveExtra = [];
		controller.saveCode = [];
		controller.saveEntities = [];
		controller.indexEntities = [];
		controller.elementoGramatica = form;
		return controller;
	}
	
	public void createOpcionesAccion(Object o){
		if (o != null)
			findOpcionesAccion(o);
		if (accionCrear == null)
			accionCrear = LedFactory.eINSTANCE.createAccion();
		if (accionCrear.boton == null)
			accionCrear.boton = "Crear";
		if (accionCrear.mensajeOk == null){
			if (o instanceof Popup)
				accionCrear.mensajeOk = "Registro creado correctamente";
			else
				accionCrear.mensajeOk = "Página creada correctamente";
		}
		if (accionEditar == null)
			accionEditar = LedFactory.eINSTANCE.createAccion();
		if (accionEditar.boton == null)
			accionEditar.boton = "Guardar";
		if (accionEditar.mensajeOk == null){
			if (o instanceof Popup)
				accionEditar.mensajeOk = "Registro actualizado correctamente";
			else
				accionEditar.mensajeOk = "Página editada correctamente";
		}
		if (accionBorrar == null)
			accionBorrar = LedFactory.eINSTANCE.createAccion();
		if (accionBorrar.boton == null)
			accionBorrar.boton = "Borrar";
		if (accionBorrar.mensajeOk == null){
			if (o instanceof Popup)
				accionBorrar.mensajeOk = "Registro borrado correctamente";
			else
				accionBorrar.mensajeOk = "Página borrada correctamente";
		}
	}
	
	private void findOpcionesAccion(Object o){
		if(o.metaClass.respondsTo(o,"getElementos")){
			for (Object elemento: o.elementos)
				findOpcionesAccion(elemento);
		}
		if(o instanceof Accion){
			Accion accion = (Accion)o;
			if ("crear".equals(accion.name))
				accionCrear = accion;
			if ("editar".equals(accion.name))
				accionEditar = accion;
			if ("borrar".equals(accion.name))
				accionBorrar = accion;
		}
	}

	public void checkReferencia(String accion){
		accion = Actions.getAccion(accion);
		if ("editar".equals(accion)) editar = true;
		if ("crear".equals(accion)) crear = true;
		if ("borrar".equals(accion)) borrar = true;
	}
		
	public void findPaginaReferencias(Pagina pagina){
		editar = crear = borrar = false;
		for (MenuEnlace enlace: LedUtils.getNodes(LedPackage.Literals.MENU_ENLACE))
			if (enlace.pagina != null && enlace.pagina.pagina.name.equals(pagina.name))
				checkReferencia(enlace.pagina.accion);
		for (Enlace enlace: LedUtils.getNodes(LedPackage.Literals.ENLACE))
			if (enlace.pagina != null && enlace.pagina.pagina.name.equals(pagina.name))
				checkReferencia(enlace.pagina.accion);
		for (Boton boton: LedUtils.getNodes(LedPackage.Literals.BOTON))
			if (boton.pagina != null && boton.pagina.pagina.name.equals(pagina.name))
				checkReferencia(boton.pagina.accion);
		for (Pagina p: LedUtils.getNodes(LedPackage.Literals.PAGINA)){
			Controller c = new Controller();
			c.createOpcionesAccion(pagina);
			if (c.accionCrear.redirigir?.pagina?.name.equals(pagina.name))
				checkReferencia(c.accionCrear.redirigir.accion);
			if (c.accionEditar.redirigir?.pagina?.name.equals(pagina.name))
				checkReferencia(c.accionEditar.redirigir.accion);
			if (c.accionBorrar.redirigir?.pagina?.name.equals(pagina.name))
				checkReferencia(c.accionBorrar.redirigir.accion);
		}
		for (Tabla tabla: LedUtils.getNodes(LedPackage.Literals.TABLA)){
			if (tabla.pagina != null && tabla.pagina.name.equals(pagina.name))
				crear = borrar = editar = true;
			if (tabla.paginaCrear != null && tabla.paginaCrear.name.equals(pagina.name))
				crear = true;
			if (tabla.paginaEditar != null && tabla.paginaEditar.name.equals(pagina.name))
				editar = true;
			if (tabla.paginaBorrar != null && tabla.paginaBorrar.name.equals(pagina.name))
				borrar = true;
		}
		for (Form form: LedUtils.getNodes(LedPackage.Literals.FORM)){
			if (form.redirigir != null && form.redirigir.pagina.name.equals(pagina.name))
				checkReferencia(form.redirigir.accion);
		}
	}
	
	public void findPopupReferencias(Popup popup){
		editar = crear = borrar = false;
		for (MenuEnlace enlace: LedUtils.getNodes(LedPackage.Literals.MENU_ENLACE))
			if (enlace.popup != null && enlace.popup.popup.name.equals(popup.name))
				checkReferencia(enlace.popup.accion);
		for (Enlace enlace: LedUtils.getNodes(LedPackage.Literals.ENLACE))
			if (enlace.popup != null && enlace.popup.popup.name.equals(popup.name))
				checkReferencia(enlace.popup.accion);
		for (Boton boton: LedUtils.getNodes(LedPackage.Literals.BOTON))
			if (boton.popup != null && boton.popup.popup.name.equals(popup.name))
				checkReferencia(boton.popup.accion);
		for (Tabla tabla: LedUtils.getNodes(LedPackage.Literals.TABLA)){
			if (tabla.popup != null && tabla.popup.name.equals(popup.name))
				crear = borrar = editar = true;
			if (tabla.popupCrear != null && tabla.popupCrear.name.equals(popup.name))
				crear = true;
			if (tabla.popupEditar != null && tabla.popupEditar.name.equals(popup.name))
				editar = true;
			if (tabla.popupBorrar != null && tabla.popupBorrar.name.equals(popup.name))
				borrar = true;
		}
	}
	
	public Accion getAccion(String accion){
		if ("editar".equals(accion)) return accionEditar;
		if ("crear".equals(accion)) return accionCrear;
		if ("borrar".equals(accion)) return accionBorrar;
		return null;
	}
	
	/*
	 * Devuelve un String con la llamada a play.mvc.Router.reverse que a su vez retorna la URL asociada a este controlador y a la acción especificada.
	 * @param forTabla	Si es true, en el parámetro que corresponde al ID de la entidad pone un String, debido a que para su uso en tablas, el identificador
	 * de la entidad no se conoce hasta que el usuario selecciona una fila en la tabla.  
	 */
	public String getRouteIndex(String accion, boolean forTabla = false){
		accion = Actions.getAccion(accion);
		String accionParam = "'accion':'${accion}'";
		String almacenId = (almacen.nulo() || almacen.isSingleton())? "" : "'${almacen.id}':${almacen.idCheck}";
		String entidadId = "";
		if (!entidad.nulo() && !entidad.isSingleton() && !accion.equals("crear")){
			if (forTabla){
				entidadId = "'${entidad.id}':'_${entidad.id}_'";
			}
			else{
				if (xToMany)
					entidadId = "'${entidad.id}':${entidad.idCheck}";
				else
					entidadId = "'${entidad.id}':${campo.idWithNullCheck()}";
			}
		}
		String redirigirAnterior = "";
		if (getAccion(accion)?.anterior)
			redirigirAnterior = "'redirigir': 'anterior'";
		List<String> intermediasStr = intermedias.collect {"'${it.id}':${it.idCheck}"};
		String ids = ", [${StringUtils.params(accionParam, almacenId, entidadId, intermediasStr, redirigirAnterior)}]";
		String link = """play.mvc.Router.reverse("${controllerFullName}.index" ${ids})""";
		return link;
	}
	
	public String getRouteAccion(String accion){
		String almacenId = (almacen.nulo() || almacen.isSingleton())? "" : "'${almacen.id}':${almacen.idCheck}";
		String entidadId = "";
		if (!entidad.nulo() && !entidad.isSingleton() && (!accion.equals("crear") || hayTabla)){
			if (xToMany)
				entidadId = "'${entidad.id}':${entidad.idCheck}";
			else
				entidadId = "'${entidad.id}':${campo.idWithNullCheck()}";
		}
		List<String> intermediasStr = intermedias.collect {"'${it.id}':${it.idCheck}"};
		String params = StringUtils.params(almacenId, entidadId, intermediasStr);
		String ids = "";
		if (params != "")
			ids = ", [${params}]";
		String link = "play.mvc.Router.reverse('${controllerFullName}.${accion}' ${ids})";
		return link;
	}
	
	private static List<AlmacenEntidad> calcularSubcampos(Campo campo){
		List<AlmacenEntidad> lista = new ArrayList<AlmacenEntidad>();
		if (campo == null || campo.atributos == null) return lista;
		String campoStr = campo.getEntidad().getName();
		EntidadUtils almacen = EntidadUtils.create(campo.getEntidad());
		EntidadUtils almacenAnterior = EntidadUtils.create((Entity)null);
		CampoAtributos atributos = campo.atributos;
		while (atributos != null){
			Attribute attr = atributos.getAtributo();
			campoStr += "." + attr.getName();
			if (LedEntidadUtils.xToMany(attr) || atributos.getAtributos() == null){
				EntidadUtils entidad = EntidadUtils.create(LedEntidadUtils.getEntidad(attr));
				lista.add(new AlmacenEntidad(CampoUtils.create(campoStr), almacenAnterior, almacen, entidad));
				campoStr = entidad.getClase();
				almacenAnterior = almacen;
				almacen = entidad;
			}
			atributos = atributos.getAtributos();
		}
		return lista;
	}
	
	/*
	 * Recorre los atributos de la entidad, y en aquellos que sean OneToMany o ManyToMany,
	 * hace un clear() para vaciar la lista y que Hibernate no cante error al borrar la entidad.
	 */
	private String borrarListasXToMany(){
		String code = "";
		for (Attribute attr: LedEntidadUtils.getAllDirectAttributes(entidad.entidad)){
			if (LedEntidadUtils.xToMany(attr)){
				code += "${entidad.variableDb}.${attr.name}.clear();\n";
			}
		}
		return code;
	}
			
}