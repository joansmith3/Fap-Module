package models;

import java.util.*;
import javax.persistence.*;
import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import play.data.validation.*;
import org.joda.time.DateTime;
import models.*;
import messages.Messages;
import validation.*;
import audit.Auditable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

// === IMPORT REGION START ===
import enumerado.fap.gen.EstadoNotificacionEnum;
import es.gobcan.aciisi.servicios.enotificacion.dominio.notificacion.DocumentoNotificacionEnumType;
import es.gobcan.aciisi.servicios.enotificacion.notificacion.NotificacionService;
import properties.FapProperties;
import utils.NotificacionUtils;

// === IMPORT REGION END ===

@Entity
public class Notificacion extends FapModel {
	// Código de los atributos

	public String uri;

	public String uriProcedimiento;

	public String descripcion;

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "notificacion_interesados")
	public List<Interesado> interesados;

	@Transient
	public String todosInteresados;

	@ValueFromTable("estadoNotificacion")
	@FapEnum("enumerado.fap.gen.EstadoNotificacionEnum")
	public String estado;

	@Transient
	public boolean activa;

	public String asunto;

	@org.hibernate.annotations.Columns(columns = { @Column(name = "fechaPuestaADisposicion"), @Column(name = "fechaPuestaADisposicionTZ") })
	@org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTimeWithZone")
	public DateTime fechaPuestaADisposicion;

	@org.hibernate.annotations.Columns(columns = { @Column(name = "fechaAcceso"), @Column(name = "fechaAccesoTZ") })
	@org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTimeWithZone")
	public DateTime fechaAcceso;

	@org.hibernate.annotations.Columns(columns = { @Column(name = "fechaLimite"), @Column(name = "fechaLimiteTZ") })
	@org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTimeWithZone")
	public DateTime fechaLimite;

	@org.hibernate.annotations.Columns(columns = { @Column(name = "fechaFinPlazo"), @Column(name = "fechaFinPlazoTZ") })
	@org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTimeWithZone")
	public DateTime fechaFinPlazo;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "notificacion_documentosanotificar")
	public List<DocumentoNotificacion> documentosANotificar;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "notificacion_documentosanexos")
	public List<DocumentoNotificacion> documentosAnexos;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "notificacion_documentosrespuesta")
	public List<DocumentoNotificacion> documentosRespuesta;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "notificacion_documentosauditoria")
	public List<Documento> documentosAuditoria;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Documento documentoPuestaADisposicion;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Documento documentoAnulacion;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Documento documentoRespondida;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Documento documentoAcuseRecibo;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Documento documentoNoAcceso;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Registro registro;

	public Integer plazoAcceso;

	public Integer frecuenciaRecordatorioAcceso;

	public Integer plazoRespuesta;

	public Integer frecuenciaRecordatorioRespuesta;

	public String idExpedienteAed;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Agente agente;

	public Boolean preparadaAnulacion;

	public Boolean preparadaRespondida;

	public Notificacion() {
		init();
	}

	public void init() {

		if (interesados == null)
			interesados = new ArrayList<Interesado>();

		if (documentosANotificar == null)
			documentosANotificar = new ArrayList<DocumentoNotificacion>();

		if (documentosAnexos == null)
			documentosAnexos = new ArrayList<DocumentoNotificacion>();

		if (documentosRespuesta == null)
			documentosRespuesta = new ArrayList<DocumentoNotificacion>();

		if (documentosAuditoria == null)
			documentosAuditoria = new ArrayList<Documento>();

		if (documentoPuestaADisposicion == null)
			documentoPuestaADisposicion = new Documento();
		else
			documentoPuestaADisposicion.init();

		if (documentoAnulacion == null)
			documentoAnulacion = new Documento();
		else
			documentoAnulacion.init();

		if (documentoRespondida == null)
			documentoRespondida = new Documento();
		else
			documentoRespondida.init();

		if (documentoAcuseRecibo == null)
			documentoAcuseRecibo = new Documento();
		else
			documentoAcuseRecibo.init();

		if (documentoNoAcceso == null)
			documentoNoAcceso = new Documento();
		else
			documentoNoAcceso.init();

		if (registro == null)
			registro = new Registro();
		else
			registro.init();

		if (preparadaAnulacion == null)
			preparadaAnulacion = false;

		if (preparadaRespondida == null)
			preparadaRespondida = false;

		postInit();
	}

	// === MANUAL REGION START ===

	public Notificacion(List<DocumentoNotificacion> documentosANotificar, List<Interesado> interesados, String idExpedienteAed) {
		init();
		this.estado = EstadoNotificacionEnum.creada.name();
		this.plazoAcceso = FapProperties.getInt("fap.notificacion.plazoacceso");
		this.plazoRespuesta = FapProperties.getInt("fap.notificacion.plazorespuesta");
		this.frecuenciaRecordatorioAcceso = FapProperties.getInt("fap.notificacion.frecuenciarecordatorioacceso");
		this.frecuenciaRecordatorioRespuesta = FapProperties.getInt("fap.notificacion.frecuenciarecordatoriorespuesta");
		this.fechaPuestaADisposicion = new DateTime();
		this.documentosANotificar.addAll(documentosANotificar);
		this.interesados.addAll(interesados);
		this.idExpedienteAed = idExpedienteAed;
	}

	public boolean getActiva() {
		if ((this.estado != null) && (!this.estado.equals(EstadoNotificacionEnum.puestaadisposicion.name()))) {
			return true;
		}
		return false;
	}

	public List<Firmante> getFirmantes() {
		List<Firmante> listFirmantes = new ArrayList<Firmante>();

		for (Interesado interesado : interesados) {
			Firmante firmante = new Firmante(interesado);
			listFirmantes.add(firmante);
		}
		return listFirmantes;
	}

	public void actualizarDocumentacion(Notificacion notificacion) {
		//Comprobación de que hay nueva documentación
		//LO QUE SE ALMACENAN EN BBDD SON LAS URIS
		//Se suben los expedientes al AED

		//List donde se almacenan los nuevos documentos a subir al AED
		List<DocumentoNotificacion> documentosNuevos = new ArrayList<DocumentoNotificacion>();

		//DocNotificacion (Requerimiento y/o más docs)
		for (DocumentoNotificacion doc : notificacion.documentosANotificar) {
			boolean encontrado = false;
			for (DocumentoNotificacion docDB : this.documentosANotificar) {
				if (doc.uri.equalsIgnoreCase(docDB.uri)) {
					encontrado = true;
					break;
				}
			}

			if (!encontrado) {
				this.documentosANotificar.add(doc);
				doc.save();
				this.save();
				// Preparar para almacenar en AED
				documentosNuevos.add(doc);
			}
		}

		// Sincronizar los documentos asociados
		for (DocumentoNotificacion doc : notificacion.documentosAnexos) {
			boolean encontrado = false;
			for (DocumentoNotificacion docDB : this.documentosAnexos) {
				if (doc.uri.equalsIgnoreCase(docDB.uri)) {
					encontrado = true;
					break;
				}
			}

			if (!encontrado) {
				this.documentosAnexos.add(doc);
				doc.save();
				this.save();
				// Preparar para almacenar en AED
				documentosNuevos.add(doc);
			}
		}

		//Doc Acuse de recibo -> Negativo o Positivo
		String uriAcuseDeRecibo = NotificacionUtils.obtenerUriDocumentos(this, DocumentoNotificacionEnumType.ACUSE_RECIBO);
		if ((uriAcuseDeRecibo != "") && (!uriAcuseDeRecibo.equals(this.documentoAcuseRecibo.uri))) {
			play.Logger.info("Nuevo fichero de AcuseRecibo para " + this.idExpedienteAed);
			NotificacionUtils.subirDocumentoNotificacionExpediente(uriAcuseDeRecibo, this);
			this.documentoAcuseRecibo.uri = uriAcuseDeRecibo;
			this.documentoAcuseRecibo.firmado = NotificacionUtils.obtenerFirmadoDocumentoNotificacion("", this.uri, DocumentoNotificacionEnumType.ACUSE_RECIBO);
			this.documentoAcuseRecibo.clasificado = true;
			try {
				this.documentoAcuseRecibo.descripcion = NotificacionUtils.obtenerDescripcionDocumento(uriAcuseDeRecibo);
				this.documentoAcuseRecibo.tipo = NotificacionUtils.obtenerTipoDocumento(uriAcuseDeRecibo);
			} catch (Exception ex) {
				play.Logger.error("Error obteniendo la descripción y el tipo de documento de la uri: " + uriAcuseDeRecibo + "Excepción: " + ex.getMessage());
			}
		}

		//Doc anulacion
		String uriAnulacion = NotificacionUtils.obtenerUriDocumentos(this, DocumentoNotificacionEnumType.ANULACION);
		if ((uriAnulacion != "") && (!uriAnulacion.equals(this.documentoAnulacion.uri))) {
			play.Logger.info("Nuevo fichero de Anulacion para " + this.idExpedienteAed);
			NotificacionUtils.subirDocumentoNotificacionExpediente(uriAnulacion, this);
			this.documentoAnulacion.uri = uriAnulacion;
			this.documentoAnulacion.firmado = NotificacionUtils.obtenerFirmadoDocumentoNotificacion("", this.uri, DocumentoNotificacionEnumType.ANULACION);
			this.documentoAnulacion.clasificado = true;
			try {
				this.documentoAnulacion.descripcion = NotificacionUtils.obtenerDescripcionDocumento(uriAnulacion);
				this.documentoAnulacion.tipo = NotificacionUtils.obtenerTipoDocumento(uriAnulacion);
			} catch (Exception ex) {
				play.Logger.error("Error obteniendo la descripción y el tipo de documento de la uri: " + uriAnulacion + "Excepción: " + ex.getMessage());
			}
		}

		//DocPuestaADisposicion
		String uriPuestaADisposicion = NotificacionUtils.obtenerUriDocumentos(this, DocumentoNotificacionEnumType.PUESTA_A_DISPOSICION);
		if ((uriPuestaADisposicion != "") && (!uriPuestaADisposicion.equals(this.documentoPuestaADisposicion.uri))) {
			play.Logger.info("Nuevo fichero de PuestaADisposicion para " + this.idExpedienteAed);
			NotificacionUtils.subirDocumentoNotificacionExpediente(uriPuestaADisposicion, this);
			this.documentoPuestaADisposicion.uri = uriPuestaADisposicion;
			this.documentoPuestaADisposicion.firmado = NotificacionUtils.obtenerFirmadoDocumentoNotificacion("", this.uri, DocumentoNotificacionEnumType.PUESTA_A_DISPOSICION);
			this.documentoPuestaADisposicion.clasificado = true;
			try {
				this.documentoPuestaADisposicion.descripcion = NotificacionUtils.obtenerDescripcionDocumento(uriPuestaADisposicion);
				this.documentoPuestaADisposicion.tipo = NotificacionUtils.obtenerTipoDocumento(uriPuestaADisposicion);
			} catch (Exception ex) {
				play.Logger.error("Error obteniendo la descripción y el tipo de documento de la uri: " + uriPuestaADisposicion + "Excepción: " + ex.getMessage());
			}
		}

		//DocRespondida
		String uriRespondida = NotificacionUtils.obtenerUriDocumentos(this, DocumentoNotificacionEnumType.MARCADA_RESPONDIDA);

		//Código temporal: ya hay notificaciones creadas, que no tienen este doc inicialiado (nuevo doc)
		if (this.documentoNoAcceso == null) {
			this.documentoNoAcceso = new Documento();
		}
		if ((uriRespondida != "") && (!uriRespondida.equals(this.documentoRespondida.uri))) {
			play.Logger.info("Nuevo fichero de Respondida para " + this.idExpedienteAed);
			NotificacionUtils.subirDocumentoNotificacionExpediente(uriRespondida, this);
			this.documentoRespondida.uri = uriRespondida;
			this.documentoRespondida.firmado = NotificacionUtils.obtenerFirmadoDocumentoNotificacion("", this.uri, DocumentoNotificacionEnumType.MARCADA_RESPONDIDA);
			this.documentoRespondida.clasificado = true;
			try {
				this.documentoRespondida.descripcion = NotificacionUtils.obtenerDescripcionDocumento(uriRespondida);
				this.documentoRespondida.tipo = NotificacionUtils.obtenerTipoDocumento(uriRespondida);
			} catch (Exception ex) {
				play.Logger.error("Error obteniendo la descripción y el tipo de documento de la uri: " + uriRespondida + "Excepción: " + ex.getMessage());
			}
		}

		//DocNoAcceso
		String uriNoAcceso = NotificacionUtils.obtenerUriDocumentos(this, DocumentoNotificacionEnumType.NO_ACCESO);
		if ((uriNoAcceso != "") && (!uriNoAcceso.equals(this.documentoNoAcceso.uri))) {
			play.Logger.info("Nuevo fichero de NoAcceso para " + this.idExpedienteAed);
			NotificacionUtils.subirDocumentoNotificacionExpediente(uriNoAcceso, this);
			this.documentoNoAcceso.uri = uriNoAcceso;
			this.documentoNoAcceso.firmado = NotificacionUtils.obtenerFirmadoDocumentoNotificacion("", this.uri, DocumentoNotificacionEnumType.NO_ACCESO);
			this.documentoNoAcceso.clasificado = true;
			try {
				this.documentoNoAcceso.descripcion = NotificacionUtils.obtenerDescripcionDocumento(uriNoAcceso);
				this.documentoNoAcceso.tipo = NotificacionUtils.obtenerTipoDocumento(uriNoAcceso);
			} catch (Exception ex) {
				play.Logger.error("Error obteniendo la descripción y el tipo de documento de la uri: " + uriNoAcceso + "Excepción: " + ex.getMessage());
			}
		}

		//Subida de los nuevos documentos de tipo DocumentoNotificacion (lista docs no es vacía)
		if ((documentosNuevos != null) && (!documentosNuevos.isEmpty())) {
			play.Logger.info("Nuevos Multiples Ficheros para " + this.idExpedienteAed);
			NotificacionUtils.subirDocumentosNotificacionExpediente(documentosNuevos, this);
		}
	}

	public void actualizar(Notificacion notificacion) {
		try {
			if ((this.estado == null && notificacion.estado != null) || (this.estado != null && !this.estado.equals(notificacion.estado)))
				this.estado = notificacion.estado;

			//if ((this.fechaAcceso == null && notificacion.fechaAcceso != null) || (!this.fechaAcceso.equals(notificacion.fechaAcceso)))
			//    this.fechaAcceso = notificacion.fechaAcceso;

			if ((this.fechaFinPlazo == null && notificacion.fechaFinPlazo != null) || (this.fechaFinPlazo != null && !this.fechaFinPlazo.equals(notificacion.fechaFinPlazo)))
				this.fechaFinPlazo = notificacion.fechaFinPlazo;

			if ((this.fechaLimite == null && notificacion.fechaLimite != null) || (this.fechaLimite != null && !this.fechaLimite.equals(notificacion.fechaLimite)))
				this.fechaLimite = notificacion.fechaLimite;

			//			play.Logger.info("Actualizando fechas y estado de Notificacion: " + this.uri + " para el expediente " + this.idExpedienteAed);
		} catch (Exception e) {
			play.Logger.error("Se ha producido un error actualizando la notificacion: " + this.uri + " error: " + e.getMessage());
		}

	}

	public String getTodosInteresados() {
		String todos = "";
		for (Interesado interesado : this.interesados) {
			todos += interesado.persona.numeroId + ", ";
		}
		if (!todos.isEmpty())
			todos = todos.substring(0, todos.length() - 2);
		return todos;
	}

	// === MANUAL REGION END ===

}
