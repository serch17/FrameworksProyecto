package mx.gob.sct.dgaf.controller;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.signer.core.SignerServices;
import org.signer.vo.ContribuyenteVO;
import org.signer.vo.SignerVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.itextpdf.text.DocumentException;
import com.octo.captcha.service.image.ImageCaptchaService;

import mx.gob.sct.dgaf.model.Captcha;
import mx.gob.sct.dgaf.model.FotoContenedor;
import mx.gob.sct.dgaf.model.Perdireccion;
import mx.gob.sct.dgaf.model.TPerDatosPersona;
import mx.gob.sct.dgaf.model.TPersona;
import mx.gob.sct.dgaf.model.VUBitContrasena;
import mx.gob.sct.dgaf.model.VUPerLicDigital;
import mx.gob.sct.dgaf.services.IAdminPersonasService;
import mx.gob.sct.dgaf.services.IPersonaMedPrevenService;
import mx.gob.sct.dgaf.services.dao.IPersonaMedPrevenDao;
import mx.gob.sct.dgaf.services.impl.VUBitContrasenaServiceImpl;
import mx.gob.sct.dgaf.services.impl.VUPerLicDigitalImpl;
import mx.gob.sct.dgaf.util.CaptchaGenerator;
import mx.gob.sct.dgaf.util.CaptchaGeneratorGoogle;
import mx.gob.sct.dgaf.util.CorreoReenviarPasswordLF;
import mx.gob.sct.dgaf.util.EnviaCorreo;
import mx.gob.sct.dgaf.util.GenerarPdf;
import mx.gob.sct.dgaf.util.MsgBean;
import mx.gob.sct.dgaf.util.VUTramConstants;
import mx.gob.sct.dgaf.util.VUValoresSe;
import mx.gob.sct.lfd.services.dao.IPerDatosDao;

@Controller
public class LoginController implements Serializable {

	private static final Logger LOGVU = LoggerFactory.getLogger(LoginController.class);
	HttpSession session = null;
	String cExpediente = null;
	String curp = null;
	String nombre = null;
	Boolean redirecLogin=false; 
	 TPerDatosPersona perDatosPersona = null;
	 VUBitContrasena perDatosInsertaVIBIT = null;
	
	 public int length;
	 public byte[] bytes;
	 public String name;
	 public String type;
	 private static final long serialVersionUID = 1L;
	 private static final String _OCSP = "http://www.sat.gob.mx/ocsp";
	 DiskFileItemFactory factory = new DiskFileItemFactory(10 * 1024 * 1024, new File("."));
	 
	private CaptchaGenerator capGen;

	@Autowired
	protected MessageSource messageSource;
	
	@Autowired(required=true)
	private GenerarPdf generarPdf;
	
	@Autowired
	private IAdminPersonasService adminPersonasService;
	
	@Autowired
	private VUValoresSe valVUSe;
	
	@Autowired
	private EnviaCorreo enviaCorreo;
	
	@Autowired 
	private CorreoReenviarPasswordLF reenviarPwd;
	
	@Autowired
	VUPerLicDigitalImpl perLic;
	
	@Autowired
	private IPersonaMedPrevenService personaMedPrevenService;

	@Autowired
	private IPerDatosDao perDatosDaoLFD;
	
	@Autowired
	private mx.gob.sct.dgaf.services.dao.IPerdireccionDao perdireccionDao;
	
	@Autowired
	private mx.gob.sct.lfd.services.dao.IPerdireccionDao perdireccionDaoLFD;

	@Autowired
	private IPersonaMedPrevenDao personaMedPrevDao;
	
	@Autowired
	private VUBitContrasenaServiceImpl   vuBitcontrasenaService;
	
	@Autowired
	private ImageCaptchaService imageCaptchaService;
	
	@PostConstruct
	private void init() {
		capGen = new CaptchaGenerator(imageCaptchaService);
	}
	
	@RequestMapping(value = "/generaCaptcha.sct",method=RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Captcha> generaCaptcha(HttpServletRequest request) {
		ResponseEntity<Captcha> response = null;
		try {
			capGen.generateCaptchaImage(request);
			return new ResponseEntity<Captcha>(capGen.getCaptcha(),HttpStatus.CREATED);
		} catch (IOException e) {
			LOGVU.error("Error generando captcha: " + e.getMessage());
		}
		return response;
	}
	
	@RequestMapping(value = "/validaCaptcha.sct",method=RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Boolean> validaCaptcha(HttpServletRequest request) throws IOException {
		String valCaptcha = (String) request.getParameter("cCaptcha");
		boolean valido = CaptchaGeneratorGoogle.verificar(valCaptcha);
		/*LOGVU.debug("antes\n"+capGen.getCaptcha().getImage());
		capGen.getCaptcha().setValue(valCaptcha);   
		if (!capGen.isValidCaptcha()) {
			newCaptcha(request);
			LOGVU.debug("\ndespues\n"+capGen.getCaptcha().getImage());
			LOGVU.debug("se genero otro captcha...");
			return new ResponseEntity<Captcha>(capGen.getCaptcha(),HttpStatus.CREATED);
		}*/
		return new ResponseEntity<Boolean>(valido,HttpStatus.OK);
	}
	
	@RequestMapping(value = "/validaNumMedAndCurp.sct",method=RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Captcha> validaNumMedAndCurp(HttpServletRequest request) {
		
		String iCvePersonal = (String) request.getParameter("cExpediente"); 
		String cCurp = (String) request.getParameter("curp");
		
		List<TPerDatosPersona> listPerDatosPersona = new ArrayList<TPerDatosPersona>();
		
		/*se buscara al ciudadano por numero medico y CURP, los mismos que se capturaron en el login*/
		listPerDatosPersona=personaMedPrevenService.buscaDuplicidadExpediente(iCvePersonal,cCurp);
		
		if(listPerDatosPersona!=null &&
				listPerDatosPersona.size() > 1 || 
				(listPerDatosPersona.size() == 1 &&
				(listPerDatosPersona.get(0).getIcvePersonal() != Long.parseLong(iCvePersonal) ||
				!listPerDatosPersona.get(0).getcCurp().equals(cCurp)))){
			/*si existe mas de un registro, existe duplicidad, no se le permite el registro*/
			
			newCaptcha(request);
			return new ResponseEntity<Captcha>(capGen.getCaptcha(),HttpStatus.MULTI_STATUS); //207
		}else if(listPerDatosPersona!=null &&
				listPerDatosPersona.size() == 1 &&
				listPerDatosPersona.get(0).getIcvePersonal() == Long.parseLong(iCvePersonal) &&
				listPerDatosPersona.get(0).getcCurp().equals(cCurp)){
			/*si existe solo un registro, adelante con el registro*/
			
			return new ResponseEntity<Captcha>(capGen.getCaptcha(),HttpStatus.OK); //200
		}else {
			/*si es nula la busqueda, no se le permite el registro*/
			
			newCaptcha(request);
			return new ResponseEntity<Captcha>(capGen.getCaptcha(),HttpStatus.PRECONDITION_FAILED); //412
		}
	}
	
	
	public String newCaptcha(HttpServletRequest request) {
		try {
			capGen.generateCaptchaImage(request);
		} catch (IOException e) {
			LOGVU.error("Error generando captcha: " + e.getMessage());
		}
		return "";
	}

	@RequestMapping(value = "/login.sct")
	public void cargaPag(HttpServletRequest request,HttpServletResponse response) throws IOException, DocumentException {
		TPersona persona = null;
		String redirect=null;
		LOGVU.debug("Inicia login");
		
		try {
			session = null;
			session = request.getSession(true);
			
			cExpediente = request.getParameter("cExpediente")!= null ? request.getParameter("cExpediente"): null;
			curp = request.getParameter("curp")!=null ? request.getParameter("curp"): null ;
			nombre = request.getParameter("nombre")!=null ? request.getParameter("nombre") : null;
			
			String confirm = request.getParameter(VUTramConstants.VALSE_CONFIRM);
			String ConfirmaRFC = request.getParameter(VUTramConstants.VALSE_RFC_CONFIRM);
			
			if (cExpediente == null || curp == null) {
				LOGVU.debug("No existen datos proporcioandos por el APPLET del SAT");
				redirect= "/";
			} else {
				LOGVU.debug(new StringBuilder("Datos obtenidos por el APPLET Expediente:").append(cExpediente)
						.append(" curp:").append(curp)
						.append(" nombre:").append(nombre)
						.append(" confirm:").append(confirm)
						.append(" rfcConfirm:").append(ConfirmaRFC).toString());
				
				HashMap< String, String> params = new HashMap<String, String>();
				params.put("icvePersonal", cExpediente );
				params.put("curp", curp);
				TPerDatosPersona tp = personaMedPrevenService.getDatosPersonaByExpedienteCurp(params);
				
				/*solo la buscara por curp, ya que la clave de la persona en VU es autoincremental*/
				persona = adminPersonasService.getPersonaPorRC(params);

				redirecLogin=true;
				if (confirm.equals("true")) {
					LOGVU.debug("****Existe persona para confirmar: " + persona.toString());
					valVUSe.setVuPersona(persona);
					persona.setcExpediente(cExpediente);
					session.setAttribute(VUTramConstants.VALSE_TPERSONA, persona);
					session.setAttribute(VUTramConstants.VALSE_EXISTE_TPERSONA, true);
					session.setAttribute(VUTramConstants.VALSE_CONFIRMA_RFC, ConfirmaRFC);
					redirect="/home/confirm_sct.jsp";
				} else {
					if (persona != null && persona.getIdPersona()!=null) {
						LOGVU.debug("****Existe persona: " + persona.toString());
						LOGVU.debug("persona.getNombre(): " + persona.getNombre());
						LOGVU.debug("persona.getApMaterno(): " + persona.getApMaterno());
						LOGVU.debug("persona.getApPaterno(): " + persona.getApPaterno());
						valVUSe.setVuPersona(persona);

						persona.setEmail(tp.getcCorreoElectronico());
						persona.setcTelefono(tp.getcTelAviso());
						persona.setcExpediente(cExpediente);
						session.setAttribute(VUTramConstants.VALSE_TPERSONA, persona);
						session.setAttribute(VUTramConstants.VALSE_PERS, cExpediente);
						session.setAttribute(VUTramConstants.VALSE_EXISTE_TPERSONA, true);
						session.setAttribute("redirecLogin", redirecLogin);
						redirect="/home/home_sct.jsp";
					} else {
						LOGVU.debug("****No existe persona");
							
							FotoContenedor fotoContenedor= personaMedPrevenService.getFotoContenedor(Long.parseLong(cExpediente));
							if(fotoContenedor!=null){
								LOGVU.debug(" Foto contenedor: "+fotoContenedor.getInodocto());
								byte[] foto= generarPdf.bajaDocumento(fotoContenedor.getInodocto());
								session.setAttribute("foto","data:image/JPEG;base64,"+Base64.encodeBase64String(foto));
							}else {
								session.setAttribute("foto",null);
							}
							tp.setcCurp(curp);
							
						session.setAttribute("tDatosPer", tp);
						session.setAttribute(VUTramConstants.VALSE_PERS, cExpediente);
						session.setAttribute(VUTramConstants.VALSE_CURP, curp);
						session.setAttribute(VUTramConstants.VALSE_EXISTE_TPERSONA, false);
						session.setAttribute("redirecLogin", redirecLogin);
						redirect="/home/home_sct.jsp";
					}
				}
			}

		} catch (Exception e) {
			session = null;			
			redirect= "/";
			LOGVU.error("Error en loggin", e.getCause());
		}
		
		response.getOutputStream().println(redirect);
	}
	
	@RequestMapping(value = "/cargarApplet.sct")
	public void cargarAppet(){
		try {
			LOGVU.debug(" cargar applet ");
		} catch (Exception e) {
			LOGVU.error("Error en cargarApplet", e.getCause());
		}
	}
	
	@RequestMapping(value = "/obtenerCorreo.sct",method=RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ResponseEntity<TPersona> obtenerCorreo(HttpServletRequest request, HttpServletResponse response){
		TPersona persona = null;
		try {
//			session = request.getSession(false);
			persona= getPersonaSess(request);
		} catch (Exception e) {
			LOGVU.error("Error en obtenerCorreo", e.getCause());
		}
		return new ResponseEntity<TPersona>(persona, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/renvioDeCorreo.sct",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity<String> renvioDeCorreo(HttpServletRequest request, HttpServletResponse response){
		HttpStatus httpStatus = HttpStatus.OK;
		String numeroContentMan="";
		String strCorreo = "";
		String strMensaje = "";
		String strNombre="";
		String strCurp = "";
		TPersona persona = null;
		HashMap<String, String> hashMapParameter =new HashMap<String, String>();
		try {
			persona= getPersonaSess(request);
			
			strCurp = persona.getCurp()!=null?persona.getCurp():"";
			strCorreo=request.getParameter("correo");			
			
			strNombre=(persona.getNombre()!=null?persona.getNombre():"")
			          +" "+ 
			          (persona.getApPaterno()!=null?persona.getApPaterno():"")
					  +" "+
					  ( persona.getApMaterno()!=null? persona.getApMaterno():"");
			numeroContentMan=persona.getAcuseRegistroFolio();
			hashMapParameter.put("mail", strCorreo);
			hashMapParameter.put("curp", strCurp.toUpperCase());
			hashMapParameter.put("cTelefono", persona.getcTelefono());
			
				adminPersonasService.updateMailPersona(hashMapParameter);
				MsgBean correoEnviado = enviaCorreo.sendEMail(strCorreo, "Correo de Confirmaci\u00f3n", strMensaje, strCurp, strNombre, numeroContentMan);	
				
				if (!correoEnviado.isEstatus()) 
					httpStatus = HttpStatus.NOT_ACCEPTABLE;
				else
					correoEnviado.setMensaje(this.messageSource.getMessage("sct.mail.conf.success", null, Locale.getDefault()));

				//Se agrega el registro del reenvío a la bitácora
				VUBitContrasena constanciaVUBIT= new VUBitContrasena();
				Calendar calendario = Calendar.getInstance(VUTramConstants.LOCAL_MX);
				String cCurp =  persona.getCurp();
				String cNombre = persona.getNombre() + " " + persona.getApPaterno() + " " + persona.getApMaterno();
				Long iCVEPERSONAL=  personaMedPrevenService.buscaPersonaPorCurp(persona.getCurp()).getIcvePersonal();
				constanciaVUBIT.setCNOMBRE(cNombre); //Nombre persona
				constanciaVUBIT.setCURP(cCurp); //Curp persona
				constanciaVUBIT.setICVEPERSONAL(iCVEPERSONAL); //???
				constanciaVUBIT.setTSREGISTRO(calendario.getTime()); //Crear calendar
				constanciaVUBIT.setCTIPOCORREO("Reenv\u00edo-Confirmaci\u00f3n"); //Estatus
				vuBitcontrasenaService.insertVUBitContrasena(constanciaVUBIT);
					
				return new ResponseEntity<String>(correoEnviado.getMensaje(), httpStatus);
			
		} catch (Exception e) {
			LOGVU.error("Error en renvioDeCorreo", e.getCause());
			return new ResponseEntity<String>(this.messageSource.getMessage("sct.mail.error", null, Locale.getDefault()), HttpStatus.CONFLICT);
		}
	}
	
	
	@RequestMapping(value = "/TDLogin.sct", method = RequestMethod.POST)
    protected  void cargaPrueba(MultipartHttpServletRequest request, HttpServletResponse response) throws FileUploadException, IOException {
		String cPassword="";
		cPassword= request.getParameter("cPassword");
		byte [] bCertOut=new byte[0], bKeyOut=new byte[0];
		HashMap<String, byte[]> fileList = new HashMap<String, byte[]>();
	    ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setSizeMax(10 * 1024 * 1024);
		
		boolean isMultipartRequeridos=false;
		
		try {
			isMultipartRequeridos = ServletFileUpload.isMultipartContent(request);
			if (isMultipartRequeridos) {
		            Map<String, MultipartFile> fileMap = request.getFileMap();    
		            for(MultipartFile archivo:fileMap.values() ){
			             if (archivo.getName().equals("FileCert")) {
								bCertOut=archivo.getBytes();
							} 
			             if (archivo.getName().equals("FileKey")) {
								bKeyOut=archivo.getBytes();
							}
		            }  
	            }
			
			
			
			if (bCertOut.length!=0 && bKeyOut.length!=0) {
				SignerServices sigSrvc = new SignerServices(false,_OCSP,"encode","/store.properties");
				SignerVO Validate = sigSrvc.validate(bCertOut, bKeyOut, cPassword);
				
				
				if(!Validate.getcError().equals("")){
					LOGVU.error(Validate.getcError());
				}else{
					LOGVU.debug("Correcto");
				}
				
				SignerVO rs = sigSrvc.sign(bCertOut, bKeyOut, fileList, cPassword);
				ContribuyenteVO crb = rs.getContribuyenteVO();
				String cRFC="";
				String cNombre="";
				String cCurp="";
				String cError="";
				cRFC = crb.getRfc();
				cNombre = crb.getNombre();
				cCurp=crb.getCurp();
				cError=Validate.getcError();
				response.getWriter().write(cRFC + "/" + cNombre + "//" + cCurp + "///" + cError  );
				response.flushBuffer();
				//METODO PARA CARGAR ARCHIVOS
				/*SignerVO rs = sigSrvc.sign(bCertOut, bKeyOut, fileList, cPassword);
				FilesVO lst=rs.getFilesVO();
				
				if(lst!=null){
					HashMap<String,byte[]> hm=lst.getSignedFiles();
					Set<String> set = hm.keySet();
					FileOutputStream fso;
					for (String string : set) {
						fso=new FileOutputStream(new File("d:\\" + string.substring(string.lastIndexOf("\\",string.length()) )));
						fso.write(hm.get(string)); //hm.get(string) este es el arreglo que se va al content
						fso.close();
					}
				}*/
				
			}
			

		} catch (Exception e) {
			SignerServices sigSrvc = new SignerServices(false,_OCSP,"encode","/store.properties");
			SignerVO Validate = sigSrvc.validate(bCertOut, bKeyOut, cPassword);
			if(!Validate.getcError().equals("")){
				LOGVU.error(Validate.getcError(),e.getCause());
			}
			response.getWriter().write("///" + Validate.getcError());
			response.flushBuffer();
		}			
	}	

	@RequestMapping(value="/updateMailPanel.sct",method=RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Boolean> updateMailPanel(@RequestParam String mail, @RequestParam String tel,HttpServletRequest request) {		
		HttpHeaders headers = new HttpHeaders();
		TPersona persona = null;
//		session = request.getSession(false);

		try {
			persona= getPersonaSess(request);
			
			String strCurp = persona.getCurp()!=null?persona.getCurp():"";
			HashMap<String, String> hashMapParameter =new HashMap<String, String>();
			hashMapParameter.put("mail", mail);
			hashMapParameter.put("idPersona", persona.getIdPersona().toString());
			List<TPersona> lstPersona = adminPersonasService.existeEmail(hashMapParameter);
			if(lstPersona.isEmpty()){
				hashMapParameter =new HashMap<String, String>();
				hashMapParameter.put("mail", mail);
				hashMapParameter.put("cTelefono", tel);
				hashMapParameter.put("curp", strCurp.toUpperCase());
				adminPersonasService.updateMailPersona(hashMapParameter);
				persona.setcTelefono(tel);
				persona.setEmail(mail);
				Perdireccion pdir = new Perdireccion();
				pdir.setIcvepersonal(Long.valueOf(persona.getcExpediente()));
				pdir.setCtel(tel);
				personaMedPrevDao.updateCorreoPersona(persona);// por curp
				perdireccionDao.updateTelPerDireccion(pdir);
				
//				Actualiza Correo y telefono en la BD SIAF VUPERSONA y MEDPREV PERDATOS / PERDIRECCION
				mx.gob.sct.lfd.model.PerDatos tp = new mx.gob.sct.lfd.model.PerDatos();
				tp.setIcvepersonal(Long.valueOf(persona.getcExpediente()));
				tp.setCcorreoelec(mail);
				mx.gob.sct.lfd.model.Perdireccion pdirLfd = new mx.gob.sct.lfd.model.Perdireccion();
				pdirLfd.setIcvepersonal(Long.valueOf(persona.getcExpediente()));
				pdirLfd.setCtel(tel);	
				perdireccionDaoLFD.updateTelPerDireccionLfd(pdirLfd);
				perDatosDaoLFD.updateByPrimaryKeySelective(tp);
				
				headers.set("SCTResponse","Tus datos:<b> "+mail+"</b> y <b>"+ tel + "</b> se registraron correctamente.");
				return new ResponseEntity<Boolean>(true, headers,  HttpStatus.OK);

			}else{
				headers.set("SCTResponse","El correo: "+mail+" ya se encuentra registrado.");
				return new ResponseEntity<Boolean>(false, headers,  HttpStatus.OK);
			}		

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			headers.set("SCTResponse","Error de sistema.");
			return new ResponseEntity<Boolean>(false, headers,  HttpStatus.PARTIAL_CONTENT);
		}
	}
	
	

	@RequestMapping(value="/getDatosParaLicDigital2.sct",method=RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody TPerDatosPersona getDatosParaLicDigital2(HttpServletRequest request){
		TPersona persona =  getPersonaSess(request);
		  VUBitContrasena constanciaVUBIT= new VUBitContrasena();
        try {
        	perDatosPersona = personaMedPrevenService.buscaPersonaPorCurp(persona.getCurp());
//        	perDatosInsertaVIBIT = vuVontrasenaService.guardaEnVUBitContrasena(constancia);
        	
        	
    		String cCurp =  perDatosPersona.getcCurp();
    		String cNombre = perDatosPersona.getcNombre() +" " +perDatosPersona.getCapPaterno() +" " + perDatosPersona.getCapMaterno();
    		Long iCVEPERSONAL=  perDatosPersona.getIcvePersonal();
    		
    			final Calendar calendario = Calendar.getInstance(VUTramConstants.LOCAL_MX); 
    		  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
           
    		   constanciaVUBIT.setCNOMBRE(cNombre);
    		   constanciaVUBIT.setCURP(cCurp);
    		   
    		 constanciaVUBIT.setICVEPERSONAL(iCVEPERSONAL);
    		 constanciaVUBIT.setTSREGISTRO(calendario.getTime());
    		 constanciaVUBIT.setCTIPOCORREO("Reenvio");
    		 
			  vuBitcontrasenaService.insertVUBitContrasena(constanciaVUBIT);
        } catch (Exception e){
			LOGVU.error("error generaAccesoDigital.sct", e);
		}
        return perDatosPersona;
    }
	
	private TPersona getPersonaSess(final HttpServletRequest request) {
		TPersona persona = (TPersona)request.getSession(false).getAttribute(VUTramConstants.LABE_PERSONA);
		if(persona !=null && (persona.getcExpediente()==null || 
				persona.getcExpediente().equals("") ||
				persona.getcExpediente().equals("null"))) {
			String cExpediente = (request.getSession(false).getAttribute("cvePersonal")!=null &&
					!request.getSession(false).getAttribute("cvePersonal").equals(""))?(String)request.getSession(false).getAttribute("cvePersonal"):"";
			persona.setcExpediente(cExpediente);
		}
		return persona;
	}

	@RequestMapping(value = "/reenvioPassword.sct",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity<String> renvioPassword(HttpServletRequest request, HttpServletResponse response){
		HttpStatus httpStatus = HttpStatus.OK;
		String correo = "";
		String strNombre="";
		String rfc="";
		String curp="";
		TPersona persona = null;
		
		try {
//			session = request.getSession(false);
			persona= getPersonaSess(request);
			
			rfc= persona.getRfc()!=null?persona.getRfc():"";
			correo=persona.getEmail() != null ? persona.getEmail():"";
			curp=persona.getCurp();
			VUPerLicDigital perLic= this.perLic.getParametroByRfcCurp(rfc, curp);

			if(perLic==null) {
				return new ResponseEntity<String>(this.messageSource.getMessage("sct.mail.no.pwd", null, Locale.getDefault()), HttpStatus.PRECONDITION_FAILED);
			}
			strNombre=(persona.getNombre()!=null?persona.getNombre():"")
			          +" "+ 
			          (persona.getApPaterno()!=null?persona.getApPaterno():"")
					  +" "+
					  ( persona.getApMaterno()!=null? persona.getApMaterno():"");
			
				MsgBean correoEnviado = reenviarPwd.sendEMail(correo, perLic.getcContrasenaDigital(), rfc, strNombre);
				if (!correoEnviado.isEstatus()) 
					httpStatus = HttpStatus.NOT_ACCEPTABLE;
				return new ResponseEntity<String>(correoEnviado.getMensaje(), httpStatus);
		} 
		
		catch (Exception e) {
			LOGVU.error(this.messageSource.getMessage("sct.mail.error", null, Locale.getDefault()), e.getCause());
			return new ResponseEntity<String>(this.messageSource.getMessage("sct.mail.error", null, Locale.getDefault()), HttpStatus.CONFLICT);
			
		}
		
	}
}
