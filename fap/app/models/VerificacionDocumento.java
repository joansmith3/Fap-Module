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
import utils.AedUtils;

// === IMPORT REGION END ===

@Entity
public class VerificacionDocumento extends FapModel {
	// Código de los atributos

	public String uriDocumentoVerificacion;

	public String uriDocumento;

	public String uriTipoDocumento;

	@Transient
	public String nombreTipoDocumento;

	public String descripcion;

	@ValueFromTable("estadosDocumentoVerificacion")
	@FapEnum("enumerado.fap.gen.EstadosDocumentoVerificacionEnum")
	public String estadoDocumentoVerificacion;

	@org.hibernate.annotations.Columns(columns = { @Column(name = "fechaPresentacion"), @Column(name = "fechaPresentacionTZ") })
	@org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTimeWithZone")
	public DateTime fechaPresentacion;

	public String identificadorMultiple;

	public Integer version;

	@Column(columnDefinition = "LONGTEXT")
	public String motivoRequerimiento;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "verificaciondocumento_codigosrequerimiento")
	public List<CodigoRequerimiento> codigosRequerimiento;

	public Boolean existe;

	@Transient
	public String urlDescarga;

	@Transient
	public String linkUrlDescarga;

	public VerificacionDocumento() {
		init();
	}

	public void init() {

		if (codigosRequerimiento == null)
			codigosRequerimiento = new ArrayList<CodigoRequerimiento>();

		postInit();
	}

	// === MANUAL REGION START ===

	public VerificacionDocumento(Documento doc) {
		descripcion = doc.descripcionVisible;
		uriTipoDocumento = doc.tipo;
		fechaPresentacion = doc.fechaRegistro;
		uriDocumento = doc.uri;
	}

	/**
	 * Constructor de copia
	 */
	public VerificacionDocumento(VerificacionDocumento vDoc) {
		this.descripcion = vDoc.descripcion;
		this.estadoDocumentoVerificacion = vDoc.estadoDocumentoVerificacion;
		this.existe = vDoc.existe;
		this.fechaPresentacion = vDoc.fechaPresentacion;
		this.identificadorMultiple = vDoc.identificadorMultiple;
		this.motivoRequerimiento = vDoc.motivoRequerimiento;
		this.uriDocumento = vDoc.uriDocumento;
		this.uriDocumentoVerificacion = vDoc.uriDocumentoVerificacion;
		this.uriTipoDocumento = vDoc.uriTipoDocumento;
		this.codigosRequerimiento = new ArrayList<CodigoRequerimiento>();
		for (CodigoRequerimiento cReq : vDoc.codigosRequerimiento) {
			CodigoRequerimiento cRequerimiento = new CodigoRequerimiento();
			cRequerimiento.codigo = cReq.codigo;
			cRequerimiento.descripcion = cReq.descripcion;
			cRequerimiento.descripcionCorta = cReq.descripcionCorta;
			cRequerimiento.save();
			this.codigosRequerimiento.add(cRequerimiento);
		}
	}

	public String getUrlDescarga() {
		if ((uriDocumento != null) && (!uriDocumento.trim().isEmpty()))
			return AedUtils.crearUrl(uriDocumento);
		return "#";
	}

	public String disponibleDescarga() {
		if ((uriDocumento != null) && (!uriDocumento.trim().isEmpty()))
			return "SI";
		return "NO";
	}

	public String getLinkUrlDescarga() {
		String link = "";
		if ((uriDocumento != null) && (!uriDocumento.trim().isEmpty()))
			link = "<a href=\"" + AedUtils.crearUrl(uriDocumento) + "\" target=\"_blank\">Descarga</a>";
		return link;
	}

	public String getNombreTipoDocumento() {
		String etiqueta = TipoDocumento.find("select nombre from TipoDocumento tipo where tipo.uri=?", this.uriTipoDocumento).first();
		return etiqueta;
	}

	// === MANUAL REGION END ===

}
