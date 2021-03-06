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

// === IMPORT REGION END ===

@Auditable
@Entity
public class Firmante extends FapModel {
	// Código de los atributos

	public String nombre;

	public String idtipo;

	public String idvalor;

	@org.hibernate.annotations.Columns(columns = { @Column(name = "fechaFirma"), @Column(name = "fechaFirmaTZ") })
	@org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTimeWithZone")
	public DateTime fechaFirma;

	public String tipo;

	public String cardinalidad;

	public void init() {

		postInit();
	}

	// === MANUAL REGION START ===
	public Firmante() {
		init();
	}

	public Firmante(Agente agente) {
		init();
		this.nombre = agente.name;
		this.cardinalidad = "unico";
		this.idvalor = agente.username;
		// Comprobamos el tipo
		StringBuilder texto = new StringBuilder();
		if (CifCheck.validaCif(agente.username, texto)) {
			this.tipo = "personajuridica";
		} else {
			Nip nip = new Nip();
			nip.valor = agente.username;
			/// Compruebo todos los posibles tipos
			nip.tipo = "nif";
			if (new NipCheck().validaNip(nip, texto)) {
				this.tipo = "personafisica";
			} else {
				nip.tipo = "nie";
				if (new NipCheck().validaNip(nip, texto)) {
					this.tipo = "personafisica";
				} else {
					this.tipo = "personafisica";
					play.Logger.error("El firmante creado a partir del Agente no tiene tipo (se le asigna \"personafisica\") (username: " + agente.username + ")");
				}
			}
		}
	}

	public Firmante(Persona persona, String cardinalidad) {
		String tipo = getTipoRepresentanteFromPersona(persona);
		constructor(persona, tipo, cardinalidad);
	}

	public Firmante(Persona persona, String tipo, String cardinalidad) {
		constructor(persona, tipo, cardinalidad);
	}

	public Firmante(Interesado interesado) {
		this.nombre = interesado.persona.getNombreCompleto();
		this.idvalor = interesado.persona.getNumeroId();
		this.cardinalidad = "unico";
		this.tipo = interesado.persona.tipo;
		// TODO: Le asignamos un ID tipo???
	}

	private void constructor(Persona persona, String tipo, String cardinalidad) {
		init();
		this.nombre = persona.getNombreCompleto();
		setIdentificador(persona);
		this.tipo = tipo;
		this.cardinalidad = cardinalidad;
		this.idvalor = persona.numeroId;
		this.idtipo = persona.tipoDeDocumento;
	}

	private static String getTipoRepresentanteFromPersona(Persona persona) {
		String tipo = null;
		if (persona.isPersonaFisica()) {
			tipo = "personafisica";
		} else if (persona.isPersonaJuridica()) {
			tipo = "personajuridica";
		}
		return tipo;
	}

	public void setIdentificador(Nip nip) {
		idtipo = nip.tipo;
		idvalor = nip.valor;
	}

	public void setIdentificador(String cif) {
		idtipo = "cif";
		idvalor = cif;
	}

	public void setIdentificador(Persona p) {
		if (p.isPersonaFisica())
			setIdentificador(p.fisica.nip);
		else if (p.isPersonaJuridica())
			setIdentificador(p.juridica.cif);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((idtipo == null) ? 0 : idtipo.hashCode());
		result = prime * result + ((idvalor == null) ? 0 : idvalor.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		Firmante other = (Firmante) obj;

		if (idtipo != null && other.idtipo != null && idvalor != null && other.idvalor != null) {
			// Por alguna razon los certificados no distingeun entre NIE Y NIF
			// y se ponen los dos en el mismo campo como NIF

			String tipo = idtipo;
			if (tipo.equalsIgnoreCase("nie"))
				tipo = "nif";
			String otherTipo = other.idtipo;
			if (otherTipo.equalsIgnoreCase("nie")) {
				otherTipo = "nif";
			}

			return tipo.equalsIgnoreCase(otherTipo) && idvalor.equalsIgnoreCase(other.idvalor);
		}
		return false;
	}

	@Override
	public String toString() {
		return "Firmante [nombre=" + nombre + ", idtipo=" + idtipo + ", idvalor=" + idvalor + ", fechaFirma=" + fechaFirma + ", tipo=" + tipo + "]";
	}

	public boolean esFuncionarioHabilitado() {
		Agente agente = Agente.find("byUsername", idvalor).first();
		if ((agente != null) && (agente.funcionario))
			return true;
		return false;
	}

	// === MANUAL REGION END ===

}
