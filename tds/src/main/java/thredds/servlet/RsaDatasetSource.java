package thredds.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.vpac.ndg.datamodel.RsaDatasetReader;

import ucar.nc2.NetcdfFile;

/**
 * When a special url specific to rsa is encountered.
 * such as 
 * 		/rsa/small_landsat/100m
 * 
 * Use this RsaDatasetSource provider class to
 * generate CDM for rsa data cube.
 * 
 * @author hsumanto
 *
 */
public class RsaDatasetSource implements thredds.servlet.DatasetSource {
  static final Logger logger = LoggerFactory.getLogger(RsaDatasetSource.class);	
	
  RsaDatasetReader rsaDatasetReader;
  
  public RsaDatasetSource() {
	ApplicationContext appContext = new ClassPathXmlApplicationContext(new String[] {"spring/config/BeanLocations.xml"});
	rsaDatasetReader = (RsaDatasetReader) appContext.getBean("rsaDatasetReader");
  }

  public boolean isMine(HttpServletRequest req) {
	/*
		THREDDS possible protocol URIs 
		OPENDAP: /thredds/dodsC/rsa/small_landsat/100m.html
		HTTPServer: /thredds/fileServer/rsa/small_landsat/100m
		WCS: /thredds/wcs/rsa/small_landsat/100m
		WMS: /thredds/wms/rsa/small_landsat/100m
		
		String path = req.getPathInfo();
		path = /rsa/small_landsat/100m
	*/	  
    String path = req.getPathInfo();
    if(path.startsWith("/")) {
    	path = path.substring(1);
    	// path = rsa/small_landsat/100m
    }
    
    logger.info("RsaDatasetSource path: {}", path);

	if(path.startsWith("rsa")) {
		return true;
	}
	
	return false;   
  }

  public NetcdfFile getNetcdfFile(HttpServletRequest req, HttpServletResponse res) throws IOException {
	// path = /rsa/small_landsat/100m
    String path = req.getPathInfo();
	path = path.substring(1);
	
	// path = rsa/small_landsat/100m
    if(path.startsWith("rsa/")) {
	  path = path.substring(4);
	  // path = small_landsat/100m
	}	  
	  
    int pos = path.lastIndexOf('.');
    if(pos != -1) {
    	// In OPENDAP path = small_landsat/100m.html 
    	// need to remove .html
    	path = path.substring(0, pos);
    }
	
	logger.debug("is rsaDatasetReader null? {}", rsaDatasetReader == null);
	    
    NetcdfFile ncfile = null;
	try {
		ncfile = rsaDatasetReader.open("rsa:" + path);
	} catch (IOException e) {
		logger.error("{}", e);
		res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
		return null;
	}
    
	if(ncfile != null) {
	    logger.info("rsa datacube: {}", ncfile);
	    ncfile.finish();
	}
    
    return ncfile;
  }

}
