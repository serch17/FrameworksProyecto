/*     */ package mx.gob.sct.dgaf.documentos;
/*     */ 
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.rmi.Remote;
/*     */ import java.util.HashSet;
/*     */ import java.util.Iterator;
/*     */ import javax.xml.namespace.QName;
/*     */ import javax.xml.rpc.ServiceException;
/*     */ import mx.gob.sct.dgaf.util.ContentManagerDocument;
/*     */ import org.apache.axis.AxisFault;
/*     */ import org.apache.axis.EngineConfiguration;
/*     */ import org.apache.axis.client.Service;
/*     */ import org.apache.axis.client.Stub;
/*     */ import org.springframework.beans.factory.annotation.Autowired;
/*     */ 
/*     */ public class DigitalizarDocumentoServiceLocator extends Service
/*     */   implements DigitalizarDocumentoService
/*     */ {
/*     */   private static final long serialVersionUID = 1719533286829612998L;
/*     */ 
/*     */   @Autowired
/*     */   ContentManagerDocument contentManagerDocument;
/*  18 */   private String DigitalizarDocumentoSoapPort_address = "http://aplicaciones7.sct.gob.mx/Documentos/DigitalizarDocumento";
/*     */ 
/*  39 */   private String DigitalizarDocumentoSoapPortWSDDServiceName = "DigitalizarDocumentoSoapPort";
/*     */ 
/* 119 */   private HashSet ports = null;
/*     */ 
/*     */   public DigitalizarDocumentoServiceLocator()
/*     */   {
/*     */   }
/*     */ 
/*     */   public DigitalizarDocumentoServiceLocator(EngineConfiguration config)
/*     */   {
/*  25 */     super(config);
/*     */   }
/*     */ 
/*     */   public DigitalizarDocumentoServiceLocator(String wsdlLoc, QName sName) throws ServiceException {
/*  29 */     super(wsdlLoc, sName);
/*     */   }
/*     */ 
/*     */   public String getDigitalizarDocumentoSoapPortAddress()
/*     */   {
/*  35 */     return this.DigitalizarDocumentoSoapPort_address;
/*     */   }
/*     */ 
/*     */   public String getDigitalizarDocumentoSoapPortWSDDServiceName()
/*     */   {
/*  42 */     return this.DigitalizarDocumentoSoapPortWSDDServiceName;
/*     */   }
/*     */ 
/*     */   public void setDigitalizarDocumentoSoapPortWSDDServiceName(String name) {
/*  46 */     this.DigitalizarDocumentoSoapPortWSDDServiceName = name;
/*     */   }
/*     */ 
/*     */   public DigitalizarDocumento getDigitalizarDocumentoSoapPort() throws ServiceException
/*     */   {
/*     */     URL endpoint;
/*     */     try {
/*  53 */       endpoint = new URL(this.DigitalizarDocumentoSoapPort_address);
/*     */     }
/*     */     catch (MalformedURLException e) {
/*  56 */       throw new ServiceException(e);
/*     */     }
/*  58 */     return getDigitalizarDocumentoSoapPort(endpoint);
/*     */   }
/*     */ 
/*     */   public DigitalizarDocumento getDigitalizarDocumentoSoapPort(URL portAddress) throws ServiceException {
/*     */     try {
/*  63 */       DigitalizarDocumentoServiceSoapBindingStub _stub = new DigitalizarDocumentoServiceSoapBindingStub(portAddress, this);
/*  64 */       _stub.setPortName(getDigitalizarDocumentoSoapPortWSDDServiceName());
/*  65 */       return _stub;
/*     */     } catch (AxisFault e) {
/*     */     }
/*  68 */     return null;
/*     */   }
/*     */ 
/*     */   public void setDigitalizarDocumentoSoapPortEndpointAddress(String address)
/*     */   {
/*  73 */     this.DigitalizarDocumentoSoapPort_address = address;
/*     */   }
/*     */ 
/*     */   public Remote getPort(Class serviceEndpointInterface)
/*     */     throws ServiceException
/*     */   {
/*     */     try
/*     */     {
/*  83 */       if (DigitalizarDocumento.class.isAssignableFrom(serviceEndpointInterface)) {
/*  84 */         DigitalizarDocumentoServiceSoapBindingStub _stub = new DigitalizarDocumentoServiceSoapBindingStub(new URL(this.DigitalizarDocumentoSoapPort_address), this);
/*  85 */         _stub.setPortName(getDigitalizarDocumentoSoapPortWSDDServiceName());
/*  86 */         return _stub;
/*     */       }
/*     */     }
/*     */     catch (Throwable t) {
/*  90 */       throw new ServiceException(t);
/*     */     }
/*  92 */     throw new ServiceException("There is no stub implementation for the interface:  " + ((serviceEndpointInterface == null) ? "null" : serviceEndpointInterface.getName()));
/*     */   }
/*     */ 
/*     */   public Remote getPort(QName portName, Class serviceEndpointInterface)
/*     */     throws ServiceException
/*     */   {
/* 101 */     if (portName == null) {
/* 102 */       return getPort(serviceEndpointInterface);
/*     */     }
/* 104 */     String inputPortName = portName.getLocalPart();
/* 105 */     if ("DigitalizarDocumentoSoapPort".equals(inputPortName)) {
/* 106 */       return getDigitalizarDocumentoSoapPort();
/*     */     }
/*     */ 
/* 109 */     Remote _stub = getPort(serviceEndpointInterface);
/* 110 */     ((Stub)_stub).setPortName(portName);
/* 111 */     return _stub;
/*     */   }
/*     */ 
/*     */   public QName getServiceName()
/*     */   {
/* 116 */     return new QName("http://gob/sct/documentos", "DigitalizarDocumentoService");
/*     */   }
/*     */ 
/*     */   public Iterator getPorts()
/*     */   {
/* 122 */     if (this.ports == null) {
/* 123 */       this.ports = new HashSet();
/* 124 */       this.ports.add(new QName("http://gob/sct/documentos", "DigitalizarDocumentoSoapPort"));
/*     */     }
/* 126 */     return this.ports.iterator();
/*     */   }
/*     */ 
/*     */   public void setEndpointAddress(String portName, String address)
/*     */     throws ServiceException
/*     */   {
/* 134 */     if ("DigitalizarDocumentoSoapPort".equals(portName)) {
/* 135 */       setDigitalizarDocumentoSoapPortEndpointAddress(address);
/*     */     }
/*     */     else
/*     */     {
/* 139 */       throw new ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void setEndpointAddress(QName portName, String address)
/*     */     throws ServiceException
/*     */   {
/* 147 */     setEndpointAddress(portName.getLocalPart(), address);
/*     */   }
/*     */ }

/* Location:           D:\Archivos\SCT\RESPALDO DE CAMBIOS\VUN\2018\Septiembre\27-09-2018\VentanillaUnicaS\war de julio\Fecha de ultima act 10072018\DECOMPILADO.zip
 * Qualified Name:     DECOMPILADO.WEB-INF.classes.mx.gob.sct.dgaf.documentos.DigitalizarDocumentoServiceLocator
 * JD-Core Version:    0.5.3
 */