//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.1
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.29 at 05:57:25 PM CEST
//


package org.taktik.icure.cin.saml.org.w3._2000._09.xmldsig_;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Element;


/**
 * <p>Java class for SPKIDataType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SPKIDataType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence maxOccurs="unbounded"&gt;
 *         &lt;element name="SPKISexp" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/&gt;
 *         &lt;any processContents='lax' namespace='##other' minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SPKIDataType", propOrder = {
    "spkiSexpsAndAnies"
})
@XmlRootElement(name = "SPKIData")
public class SPKIData implements Serializable
{

    private final static long serialVersionUID = 2L;
    @XmlElementRef(name = "SPKISexp", namespace = "http://www.w3.org/2000/09/xmldsig#", type = JAXBElement.class)
    @XmlAnyElement
    protected List<java.lang.Object> spkiSexpsAndAnies;

    /**
     * Gets the value of the spkiSexpsAndAnies property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the spkiSexpsAndAnies property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSPKISexpsAndAnies().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link byte[]}{@code >}
     * {@link Element }
     *
     *
     */
    public List<java.lang.Object> getSPKISexpsAndAnies() {
        if (spkiSexpsAndAnies == null) {
            spkiSexpsAndAnies = new ArrayList<java.lang.Object>();
        }
        return this.spkiSexpsAndAnies;
    }

}
