
package org.onvif.ver10.device.wsdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;


/**
 * <p>Java class for SystemCapabilities complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SystemCapabilities"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="DiscoveryResolve" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="DiscoveryBye" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="RemoteDiscovery" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="SystemBackup" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="SystemLogging" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="FirmwareUpgrade" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="HttpFirmwareUpgrade" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="HttpSystemBackup" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="HttpSystemLogging" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="HttpSupportInformation" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="StorageConfiguration" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="MaxStorageConfigurations" type="{http://www.w3.org/2001/XMLSchema}int" /&gt;
 *       &lt;attribute name="GeoLocationEntries" type="{http://www.w3.org/2001/XMLSchema}int" /&gt;
 *       &lt;attribute name="AutoGeo" type="{http://www.onvif.org/ver10/schema}StringAttrList" /&gt;
 *       &lt;attribute name="StorageTypesSupported" type="{http://www.onvif.org/ver10/schema}StringAttrList" /&gt;
 *       &lt;attribute name="DiscoveryNotSupported" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="NetworkConfigNotSupported" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="UserConfigNotSupported" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;anyAttribute processContents='lax'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SystemCapabilities")
public class SystemCapabilities {

    @XmlAttribute(name = "DiscoveryResolve")
    protected Boolean discoveryResolve;
    @XmlAttribute(name = "DiscoveryBye")
    protected Boolean discoveryBye;
    @XmlAttribute(name = "RemoteDiscovery")
    protected Boolean remoteDiscovery;
    @XmlAttribute(name = "SystemBackup")
    protected Boolean systemBackup;
    @XmlAttribute(name = "SystemLogging")
    protected Boolean systemLogging;
    @XmlAttribute(name = "FirmwareUpgrade")
    protected Boolean firmwareUpgrade;
    @XmlAttribute(name = "HttpFirmwareUpgrade")
    protected Boolean httpFirmwareUpgrade;
    @XmlAttribute(name = "HttpSystemBackup")
    protected Boolean httpSystemBackup;
    @XmlAttribute(name = "HttpSystemLogging")
    protected Boolean httpSystemLogging;
    @XmlAttribute(name = "HttpSupportInformation")
    protected Boolean httpSupportInformation;
    @XmlAttribute(name = "StorageConfiguration")
    protected Boolean storageConfiguration;
    @XmlAttribute(name = "MaxStorageConfigurations")
    protected Integer maxStorageConfigurations;
    @XmlAttribute(name = "GeoLocationEntries")
    protected Integer geoLocationEntries;
    @XmlAttribute(name = "AutoGeo")
    protected List<String> autoGeo;
    @XmlAttribute(name = "StorageTypesSupported")
    protected List<String> storageTypesSupported;
    @XmlAttribute(name = "DiscoveryNotSupported")
    protected Boolean discoveryNotSupported;
    @XmlAttribute(name = "NetworkConfigNotSupported")
    protected Boolean networkConfigNotSupported;
    @XmlAttribute(name = "UserConfigNotSupported")
    protected Boolean userConfigNotSupported;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the discoveryResolve property.
     * This getter has been renamed from isDiscoveryResolve() to getDiscoveryResolve() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getDiscoveryResolve() {
        return discoveryResolve;
    }

    /**
     * Sets the value of the discoveryResolve property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDiscoveryResolve(Boolean value) {
        this.discoveryResolve = value;
    }

    /**
     * Gets the value of the discoveryBye property.
     * This getter has been renamed from isDiscoveryBye() to getDiscoveryBye() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getDiscoveryBye() {
        return discoveryBye;
    }

    /**
     * Sets the value of the discoveryBye property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDiscoveryBye(Boolean value) {
        this.discoveryBye = value;
    }

    /**
     * Gets the value of the remoteDiscovery property.
     * This getter has been renamed from isRemoteDiscovery() to getRemoteDiscovery() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getRemoteDiscovery() {
        return remoteDiscovery;
    }

    /**
     * Sets the value of the remoteDiscovery property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRemoteDiscovery(Boolean value) {
        this.remoteDiscovery = value;
    }

    /**
     * Gets the value of the systemBackup property.
     * This getter has been renamed from isSystemBackup() to getSystemBackup() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getSystemBackup() {
        return systemBackup;
    }

    /**
     * Sets the value of the systemBackup property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSystemBackup(Boolean value) {
        this.systemBackup = value;
    }

    /**
     * Gets the value of the systemLogging property.
     * This getter has been renamed from isSystemLogging() to getSystemLogging() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getSystemLogging() {
        return systemLogging;
    }

    /**
     * Sets the value of the systemLogging property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSystemLogging(Boolean value) {
        this.systemLogging = value;
    }

    /**
     * Gets the value of the firmwareUpgrade property.
     * This getter has been renamed from isFirmwareUpgrade() to getFirmwareUpgrade() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getFirmwareUpgrade() {
        return firmwareUpgrade;
    }

    /**
     * Sets the value of the firmwareUpgrade property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFirmwareUpgrade(Boolean value) {
        this.firmwareUpgrade = value;
    }

    /**
     * Gets the value of the httpFirmwareUpgrade property.
     * This getter has been renamed from isHttpFirmwareUpgrade() to getHttpFirmwareUpgrade() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getHttpFirmwareUpgrade() {
        return httpFirmwareUpgrade;
    }

    /**
     * Sets the value of the httpFirmwareUpgrade property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setHttpFirmwareUpgrade(Boolean value) {
        this.httpFirmwareUpgrade = value;
    }

    /**
     * Gets the value of the httpSystemBackup property.
     * This getter has been renamed from isHttpSystemBackup() to getHttpSystemBackup() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getHttpSystemBackup() {
        return httpSystemBackup;
    }

    /**
     * Sets the value of the httpSystemBackup property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setHttpSystemBackup(Boolean value) {
        this.httpSystemBackup = value;
    }

    /**
     * Gets the value of the httpSystemLogging property.
     * This getter has been renamed from isHttpSystemLogging() to getHttpSystemLogging() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getHttpSystemLogging() {
        return httpSystemLogging;
    }

    /**
     * Sets the value of the httpSystemLogging property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setHttpSystemLogging(Boolean value) {
        this.httpSystemLogging = value;
    }

    /**
     * Gets the value of the httpSupportInformation property.
     * This getter has been renamed from isHttpSupportInformation() to getHttpSupportInformation() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getHttpSupportInformation() {
        return httpSupportInformation;
    }

    /**
     * Sets the value of the httpSupportInformation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setHttpSupportInformation(Boolean value) {
        this.httpSupportInformation = value;
    }

    /**
     * Gets the value of the storageConfiguration property.
     * This getter has been renamed from isStorageConfiguration() to getStorageConfiguration() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getStorageConfiguration() {
        return storageConfiguration;
    }

    /**
     * Sets the value of the storageConfiguration property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setStorageConfiguration(Boolean value) {
        this.storageConfiguration = value;
    }

    /**
     * Gets the value of the maxStorageConfigurations property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMaxStorageConfigurations() {
        return maxStorageConfigurations;
    }

    /**
     * Sets the value of the maxStorageConfigurations property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMaxStorageConfigurations(Integer value) {
        this.maxStorageConfigurations = value;
    }

    /**
     * Gets the value of the geoLocationEntries property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getGeoLocationEntries() {
        return geoLocationEntries;
    }

    /**
     * Sets the value of the geoLocationEntries property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setGeoLocationEntries(Integer value) {
        this.geoLocationEntries = value;
    }

    /**
     * Gets the value of the autoGeo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the autoGeo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAutoGeo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getAutoGeo() {
        if (autoGeo == null) {
            autoGeo = new ArrayList<String>();
        }
        return this.autoGeo;
    }

    /**
     * Gets the value of the storageTypesSupported property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the storageTypesSupported property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStorageTypesSupported().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getStorageTypesSupported() {
        if (storageTypesSupported == null) {
            storageTypesSupported = new ArrayList<String>();
        }
        return this.storageTypesSupported;
    }

    /**
     * Gets the value of the discoveryNotSupported property.
     * This getter has been renamed from isDiscoveryNotSupported() to getDiscoveryNotSupported() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getDiscoveryNotSupported() {
        return discoveryNotSupported;
    }

    /**
     * Sets the value of the discoveryNotSupported property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDiscoveryNotSupported(Boolean value) {
        this.discoveryNotSupported = value;
    }

    /**
     * Gets the value of the networkConfigNotSupported property.
     * This getter has been renamed from isNetworkConfigNotSupported() to getNetworkConfigNotSupported() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getNetworkConfigNotSupported() {
        return networkConfigNotSupported;
    }

    /**
     * Sets the value of the networkConfigNotSupported property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNetworkConfigNotSupported(Boolean value) {
        this.networkConfigNotSupported = value;
    }

    /**
     * Gets the value of the userConfigNotSupported property.
     * This getter has been renamed from isUserConfigNotSupported() to getUserConfigNotSupported() by cxf-xjc-boolean plugin.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getUserConfigNotSupported() {
        return userConfigNotSupported;
    }

    /**
     * Sets the value of the userConfigNotSupported property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setUserConfigNotSupported(Boolean value) {
        this.userConfigNotSupported = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * <p>
     * the map is keyed by the name of the attribute and 
     * the value is the string value of the attribute.
     * 
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     * 
     * 
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
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
