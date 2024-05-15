//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.04.28 at 11:20:57 AM CEST 
//


package be.recipe.services.core;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PrescriptionAttribute.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="PrescriptionAttribute">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="prescription-date"/>
 *     &lt;enumeration value="reservation-modification-date"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "PrescriptionAttribute")
@XmlEnum
public enum PrescriptionAttribute {

    @XmlEnumValue("prescription-date")
    PRESCRIPTION_DATE("prescription-date"),
    @XmlEnumValue("reservation-modification-date")
    RESERVATION_MODIFICATION_DATE("reservation-modification-date");
    private final String value;

    PrescriptionAttribute(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PrescriptionAttribute fromValue(String v) {
        for (PrescriptionAttribute c: PrescriptionAttribute.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
