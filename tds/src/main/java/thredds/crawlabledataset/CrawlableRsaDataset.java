package thredds.crawlabledataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.vpac.ndg.ApplicationContextProvider;
import org.vpac.ndg.storage.dao.DatasetDao;
import org.vpac.ndg.storage.model.Dataset;

/**
 * This class is an implementation of CrawlableDataset interface
 * in order to automatically generate rsa catalog in THREDDS catalog. 
 * @author hsumanto
 *
 */
public class CrawlableRsaDataset implements CrawlableDataset {
	static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrawlableRsaDataset.class);
	
	private String path;
	private Object configObj;
	private boolean crawlable;
	
	public CrawlableRsaDataset() {
	}

	public CrawlableRsaDataset(String path, Object configObj) {
		this(path, configObj, true);
	}

	public CrawlableRsaDataset(String path, Object configObj, boolean crawlable) {
		// path = C:\work\workspace\thredds\an-eclipse-tomcat-server\content\thredds\public\rsa
		// the special rsa root
		if(path.endsWith("rsa")) {
			path = "rsa";
		}		
		
		this.path = path;
		this.configObj = configObj;
		this.crawlable = crawlable;
	}	
	
	private CrawlableRsaDataset( CrawlableRsaDataset parent, String childPath ) {
		if(!childPath.isEmpty()) {
			this.path = parent.getPath() + "/" + childPath;			
		}
		else {
			this.path = parent.getPath();
		}
		this.configObj = null;
		this.crawlable = parent.crawlable;
	}	
	
	@Override
	public Object getConfigObject() {
		return configObj;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getName() {
		return path;
	}

	@Override
	public CrawlableDataset getParentDataset() {
		if(path.indexOf("/") == -1) {
			return null;
		}
		  
	    return new CrawlableRsaDataset("rsa", null);
	}

	@Override
	public boolean exists() {
		if(path.startsWith("rsa")) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isCollection() {
		if(getParentDataset() == null) {
			return true;
		}
		return false;
	}

	@Override
	public CrawlableDataset getDescendant(String relativePath) {
		if ( relativePath.startsWith( "/" ) )
			throw new IllegalArgumentException( "Path must be relative <" + relativePath + ">." );
		
		return new CrawlableRsaDataset( this, relativePath );
	}
	
	@Override
	public List<CrawlableDataset> listDatasets() throws IOException {
		if ( ! this.exists() ) {
			String tmpMsg = "This dataset <" + this.getPath() + "> does not exist.";
		    log.error( "listDatasets(): " + tmpMsg);
		    throw new IllegalStateException( tmpMsg );
		}

		if ( ! this.isCollection() ) {
			String tmpMsg = "This dataset <" + this.getPath() + "> is not a collection dataset.";
		    log.error( "listDatasets(): " + tmpMsg);
		    throw new IllegalStateException( tmpMsg );
		}

	    List<CrawlableDataset> list = new ArrayList<CrawlableDataset>();
		    
	    if(!crawlable) {
	        log.debug( "listDatasets(): the underlying rsa path not crawlable thus listFiles() returns null.");
	        return Collections.emptyList();
	    }
		    
		ApplicationContext appContext = ApplicationContextProvider.getApplicationContext();
		DatasetDao datasetDao = (DatasetDao) appContext.getBean("datasetDao");
	    log.debug("is datasetDao null? {}", datasetDao == null);
	    List<Dataset> datasetList = datasetDao.getAll();
	    log.debug("dataset list = {}", datasetList.size());    
		    
	    for(Dataset dataset: datasetList) {
	    	String datasetId = String.format("rsa/%s/%s", dataset.getName(), dataset.getResolution().toHumanString());
		    CrawlableDataset crDs = new CrawlableRsaDataset(datasetId, null, false);
		    if ( crDs.exists())
		        list.add( crDs );
	    }
		return list;
	}
	
	@Override
	public List<CrawlableDataset> listDatasets(CrawlableDatasetFilter filter)
			throws IOException {
		return listDatasets();
	}

	@Override
	public long length() {
		return -1;
	}

	@Override
	public Date lastModified() {
		return null;
	}
}
