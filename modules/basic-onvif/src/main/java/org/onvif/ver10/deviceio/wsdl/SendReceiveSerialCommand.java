
package org.onvif.ver10.deviceio.wsdl;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Token" type="{http://www.onvif.org/ver10/schema}ReferenceToken" minOccurs="0"/&gt;
 *         &lt;element name="SerialData" type="{http://www.onvif.org/ver10/deviceIO/wsdl}SerialData" minOccurs="0"/&gt;
 *         &lt;element name="TimeOut" type="{http://www.w3.org/2001/XMLSchema}duration" minOccurs="0"/&gt;
 *         &lt;element name="DataLength" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/&gt;
 *         &lt;element name="Delimiter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "token",
    "serialData",
    "timeOut",
    "dataLength",
    "delimiter"
})
@XmlRootElement(name = "SendReceiveSerialCommand")
public class SendReceiveSerialCommand {

    @XmlElement(name = "Token")
    protected String token;
    @XmlElement(name = "SerialData")
    protected SerialData serialData;
    @XmlElement(name = "TimeOut")
    protected Duration timeOut;
    @XmlElement(name = "DataLength")
    protected BigInteger dataLength;
    @XmlElement(name = "Delimiter")
    protected String delimiter;

    /**
     * Gets the value of the token property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the value of the token property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToken(String value) {
        this.token = value;
    }

    /**
     * Gets the value of the serialData property.
     * 
     * @return
     *     possible object is
     *     {@link SerialData }
     *     
     */
    public SerialData getSerialData() {
        return serialData;
    }

    /**
     * Sets the value of the serialData property.
     * 
     * @param value
     *     allowed object is
     *     {@link SerialData }
     *     
     */
    public void setSerialData(SerialData value) {
        this.serialData = value;
    }

    /**
     * Gets the value of the timeOut property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getTimeOut() {
        return timeOut;
    }

    /**
     * Sets the value of the timeOut property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setTimeOut(Duration value) {
        this.timeOut = value;
    }

    /**
     * Gets the value of the dataLength property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getDataLength() {
        return dataLength;
    }

    /**
     * Sets the value of the dataLength property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setDataLength(BigInteger value) {
        this.dataLength = value;
    }

    /**
     * Gets the value of the delimiter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Sets the value of the delimiter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDelimiter(String value) {
        this.delimiter = value;
    }

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     * 
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, JAXBToStringStyle.DEFAULT_STYLE);
    }

}
