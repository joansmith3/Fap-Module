package controllers;

import java.util.List;
import java.util.Map;

import play.mvc.Util;
import services.RegistroServiceException;
import tramitacion.TramiteBase;

import messages.Messages;
import models.Firmante;
import models.SolicitudGenerica;
import controllers.fap.PresentacionFapController;
import controllers.gen.SolicitudPresentarFAPControllerGen;
import emails.Mails;

public class SolicitudPresentarFAPController extends SolicitudPresentarFAPControllerGen {
	
	public static void tablatablaFirmantesHecho(Long idRegistro) {

		java.util.List<Firmante> rows =  Firmante
				.find("select firmante from SolicitudGenerica solicitud join solicitud.registro.firmantes firmante where solicitud.id=? and firmante.tipo=? and firmante.fechaFirma is not null",
						idRegistro, "representante").fetch();


		Map<String, Long> ids = (Map<String, Long>) tags.TagMapStack.top("idParams");
		List<Firmante> rowsFiltered = rows; //Tabla sin permisos, no filtra

		tables.TableRenderResponse<Firmante> response = new tables.TableRenderResponse<Firmante>(rowsFiltered, false, false, false, "", "", "", getAccion(), ids);

		renderJSON(response.toJSON("idvalor", "nombre", "fechaFirma", "id"));
	}

	
	public static void tablatablaFirmantesEspera(Long idRegistro) {

		java.util.List<Firmante> rows = Firmante
				.find("select firmante from SolicitudGenerica solicitud join solicitud.registro.firmantes firmante where solicitud.id=? and firmante.tipo=? and firmante.fechaFirma is null", idRegistro, "representante").fetch();

		Map<String, Long> ids = (Map<String, Long>) tags.TagMapStack.top("idParams");
		List<Firmante> rowsFiltered = rows; //Tabla sin permisos, no filtra

		tables.TableRenderResponse<Firmante> response = new tables.TableRenderResponse<Firmante>(rowsFiltered, false, false, false, "", "", "", getAccion(), ids);

		renderJSON(response.toJSON("idvalor", "nombre", "id"));
	}
	
	@Util
	// Este @Util es necesario porque en determinadas circunstancias crear(..) llama a editar(..).
	public static void formFirmaPF(Long idSolicitud, Long idRegistro, String firma, String firmarRegistrarNif) {
		checkAuthenticity();
		if (!permisoFormFirmaPF("editar")) {
			Messages.error("No tiene permisos suficientes para realizar la acción");
		}
		try {
			PresentacionFapController.invoke("beforeFirma", idSolicitud);
		} catch (Throwable e1) {
			log.error("Hubo un problema al invocar los métodos beforeFirma: "+e1.getMessage());
			Messages.error("Error al validar elementos previos a la firma");
		}
		if (!Messages.hasErrors()) {
			try {
				TramiteBase tramite = PresentacionFapController.invoke("getTramiteObject", idSolicitud);
				tramite.firmar(firma);
				if (!Messages.hasErrors()) {
					try {
						tramite.registrar();
						tramite.cambiarEstadoSolicitud();
						try {
							PresentacionFapController.invoke("afterRegistro", idSolicitud);
						} catch (Throwable e1) {
							log.error("Hubo un problema al invocar los métodos afterRegistro: "+e1.getMessage());
							Messages.error("Error al validar elementos posteriores al registro");
						}
						Messages.ok("Solicitud registrada correctamente");
					} catch (Exception e) {
						log.error("Hubo un error al registrar la solicitud: "+ e.getMessage());
						Messages.error("No se pudo registrar la solicitud");
					}
				}
			} catch (Throwable e1) {
				log.error("Hubo un problema al invocar el metodo que devuelve la clase TramiteBase en la firma: "+e1.getMessage());
				Messages.error("Error al intentar firmar antes de registrar");
			}
		}
		
		if (!Messages.hasErrors()) {
			if (firmarRegistrarNif != null) {
				SolicitudPresentarFAPController.firmarRegistrarNifFormFirmaPF(idSolicitud, idRegistro, firma);
				SolicitudPresentarFAPController.formFirmaPFRender(idSolicitud, idRegistro);
			}
		}

		if (!Messages.hasErrors()) {
			SolicitudPresentarFAPController.formFirmaPFValidateRules(firma);
		}
		if (!Messages.hasErrors()) {
			log.info("Acción Editar de página: " + "gen/SolicitudPresentarFAP/SolicitudPresentarFAP.html" + " , intentada con éxito");
			redirect("PresentarFAPController.index", idSolicitud, idRegistro);
		} else
			log.info("Acción Editar de página: " + "gen/SolicitudPresentarFAP/SolicitudPresentarFAP.html" + " , intentada sin éxito (Problemas de Validación)");
		SolicitudPresentarFAPController.formFirmaPFRender(idSolicitud, idRegistro);
	}
	
	@Util
	public static void formFirmaPFRender(Long idSolicitud, Long idRegistro) {
		if (!Messages.hasMessages()) {
			Messages.ok("Página editada correctamente");
			Messages.keep();
			redirect("PresentarFAPController.index", "editar", idSolicitud, idRegistro);
		}
		Messages.keep();
		redirect("PresentarFAPController.index", "editar", idSolicitud, idRegistro);
	}
	
	@Util
	// Este @Util es necesario porque en determinadas circunstancias crear(..) llama a editar(..).
	public static void formFirmaRepresentante(Long idSolicitud, Long idRegistro, String firma, String firmarRepresentante) {
		checkAuthenticity();
		if (!permisoFormFirmaRepresentante("editar")) {
			Messages.error("No tiene permisos suficientes para realizar la acción");
		}

		try {
			PresentacionFapController.invoke("beforeFirma", idSolicitud);
		} catch (Throwable e1) {
			log.error("Hubo un problema al invocar los métodos beforeFirma, en la firma con representantes: "+e1.getMessage());
			Messages.error("Error al validar elementos previos a la firma de representante");
		}
		
		if (!Messages.hasErrors()) {
			try {
				TramiteBase tramite = PresentacionFapController.invoke("getTramiteObject", idSolicitud);
				tramite.firmar(firma);
			} catch (Throwable e1) {
				log.error("Hubo un problema al firmar con representante en presentacion: "+e1.getMessage());
				Messages.error("Error al intentar firmar el representante");
			}
		}
		
		if (firmarRepresentante != null) {
			SolicitudPresentarFAPController.firmarRepresentanteFormFirmaRepresentante(idSolicitud, idRegistro, firma);
			SolicitudPresentarFAPController.formFirmaRepresentanteRender(idSolicitud, idRegistro);
		}

		if (!Messages.hasErrors()) {
			SolicitudPresentarFAPController.formFirmaRepresentanteValidateRules(firma);
		}
		if (!Messages.hasErrors()) {
			log.info("Acción Editar de página: " + "gen/SolicitudPresentarFAP/SolicitudPresentarFAP.html" + " , intentada con éxito");
		} else
			log.info("Acción Editar de página: " + "gen/SolicitudPresentarFAP/SolicitudPresentarFAP.html" + " , intentada sin éxito (Problemas de Validación)");
		SolicitudPresentarFAPController.formFirmaRepresentanteRender(idSolicitud, idRegistro);
	}
	
	@Util
	// Este @Util es necesario porque en determinadas circunstancias crear(..) llama a editar(..).
	public static void formFirmaCif(Long idSolicitud, Long idRegistro, String firma, String firmarRegistrarCif) {
		checkAuthenticity();
		if (!permisoFormFirmaCif("editar")) {
			Messages.error("No tiene permisos suficientes para realizar la acción");
		}
		
		try {
			PresentacionFapController.invoke("beforeFirma", idSolicitud);
		} catch (Throwable e1) {
			log.error("Hubo un problema al invocar los métodos beforeFirma, antes de registrar: "+e1.getMessage());
			Messages.error("Error al validar elementos previos al registro");
		}
		
		if (!Messages.hasErrors()) {
			try {
				TramiteBase tramite = PresentacionFapController.invoke("getTramiteObject", idSolicitud);
				tramite.firmar(firma);
				if (!Messages.hasErrors()) {
					try {
						tramite.registrar();
						tramite.cambiarEstadoSolicitud();
						try {
							PresentacionFapController.invoke("afterRegistro", idSolicitud);
						} catch (Throwable e1) {
							log.error("Hubo un problema al invocar los métodos afterRegistro: "+e1.getMessage());
							Messages.error("Error al validar elementos posteriores al registro");
						}
						Messages.ok("Solicitud registrada correctamente");
					} catch (Exception e) {
						log.error("Hubo un problema al intentar registrar en la presentación: "+e.getMessage());
					}
				}
			} catch (Throwable e1) {
				log.error("Hubo un problema al firmar con representante en presentacion antes de registrar: "+e1.getMessage());
				Messages.error("Error al intentar firmar el representante antes del registro");
			}
		}

		if (firmarRegistrarCif != null) {
			SolicitudPresentarFAPController.firmarRegistrarCifFormFirmaCif(idSolicitud, idRegistro, firma);
			SolicitudPresentarFAPController.formFirmaCifRender(idSolicitud, idRegistro);
		}

		if (!Messages.hasErrors()) {
			SolicitudPresentarFAPController.formFirmaCifValidateRules(firma);
		}
		if (!Messages.hasErrors()) {
			log.info("Acción Editar de página: " + "gen/SolicitudPresentarFAP/SolicitudPresentarFAP.html" + " , intentada con éxito");
			redirect("PresentarFAPController.index", idSolicitud);
		} else
			log.info("Acción Editar de página: " + "gen/SolicitudPresentarFAP/SolicitudPresentarFAP.html" + " , intentada sin éxito (Problemas de Validación)");
		SolicitudPresentarFAPController.formFirmaCifRender(idSolicitud, idRegistro);
	}
	
	@Util
	public static void formFirmaCifRender(Long idSolicitud, Long idRegistro) {
		if (!Messages.hasMessages()) {
			Messages.ok("Página editada correctamente");
			Messages.keep();
			redirect("PresentarFAPController.index", "editar", idSolicitud, idRegistro);
		}
		Messages.keep();
		redirect("PresentarFAPController.index", "editar", idSolicitud, idRegistro);
	}
	
	@Util
	// Este @Util es necesario porque en determinadas circunstancias crear(..) llama a editar(..).
	public static void frmRegistrar(Long idSolicitud, Long idRegistro, String botonRegistrar) {
		checkAuthenticity();
		if (!permisoFrmRegistrar("editar")) {
			Messages.error("No tiene permisos suficientes para realizar la acción");
		}

		SolicitudGenerica dbSolicitud = SolicitudPresentarFAPController.getSolicitudGenerica(idSolicitud);
		
		if (!Messages.hasErrors()) {
			
			try {
				TramiteBase tramite = PresentacionFapController.invoke("getTramiteObject", idSolicitud);
				try {
					dbSolicitud.registro.fasesRegistro.borrador = true;
					dbSolicitud.registro.fasesRegistro.firmada = true;
					tramite.registrar();
					tramite.cambiarEstadoSolicitud();
					try {
						PresentacionFapController.invoke("afterRegistro", idSolicitud);
					} catch (Throwable e1) {
						log.error("Hubo un problema al invocar los métodos afterRegistro: "+e1.getMessage());
						Messages.error("Error al validar elementos posteriores al registro");
					}
					Messages.ok("Solicitud registrada correctamente");
				} catch (RegistroServiceException e) {
					log.error("Error al intentar registrar en la presentacion en frmRegistrar: "+e.getMessage());
					Messages.error("Error al intentar sólo registrar");
				}
			} catch (Throwable e1) {
				log.error("Error al invocar al TramiteBase en frmRegistrar de SolicitudPresentarFAPController: "+e1.getMessage());
				Messages.error("Error al intentar sólo registrar");
			}
		}

		if (!Messages.hasErrors()) {
			dbSolicitud.save();
			SolicitudPresentarFAPController.frmRegistrarValidateRules();
		}
		if (!Messages.hasErrors()) {
			log.info("Acción Editar de página: " + "gen/SolicitudPresentarFAP/SolicitudPresentarFAP.html" + " , intentada con éxito");
			redirect("PresentarFAPController.index", idSolicitud);
		} else
			log.info("Acción Editar de página: " + "gen/SolicitudPresentarFAP/SolicitudPresentarFAP.html" + " , intentada sin éxito (Problemas de Validación)");
		SolicitudPresentarFAPController.frmRegistrarRender(idSolicitud, idRegistro);
	}
}