package org.jmrtd.lds;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class LDS {

	private COMFile com;
	private Set<DataGroup> dataGroups;
	private CVCAFile cvca;
	private SODFile sod;
		
	public LDS(COMFile com, Collection<DataGroup> dataGroups, SODFile sod) {
		this(com, dataGroups, null, sod);
	}
	
	public LDS(COMFile com, Collection<DataGroup> dataGroups, CVCAFile cvca, SODFile sod) {
		this.com = com;
		this.dataGroups = new HashSet<DataGroup>(dataGroups);
		this.cvca = cvca;
		this.sod = sod;
	}
	
	/**
	 * Gets the EF.COM.
	 * 
	 * @return the EF.COM
	 */
	public COMFile getCOMFile() {
		return com;
	}
	
	/**
	 * Gets the data group specified by ICAO tag or <code>null</code> if it does not exist.
	 * 
	 * @param tag a tag for a datagroup
	 *
	 * @return a data group or <code>null</code>
	 */
	public DataGroup getDataGroup(int tag) {
		for (DataGroup dataGroup: dataGroups) {
			if (dataGroup != null && dataGroup.getTag() == tag) {
				return dataGroup;
			}
		}
		return null;
	}
	
	/**
	 * Gets a (spine-deep copy) of the collection of data groups in this LDS.
	 * 
	 * @return the data groups in this LDS
	 */
	public Collection<DataGroup> getDataGroups() {
		return new HashSet<DataGroup>(dataGroups);
	}
	
	/**
	 * Adds a new file. If the LDS already contained a file
	 * with the same tag, the old copy is replaced.
	 * 
	 * Note that EF.COM and EF.SOd will not be updated as a result of adding
	 * data groups.
	 * 
	 * @param file the new file to add
	 */
	public void add(LDSFile file) {
		if (file instanceof COMFile) {
			com = (COMFile)file;
		} else if (file instanceof SODFile) {
			sod = (SODFile)file;
		} else if (file instanceof CVCAFile) {
			cvca = (CVCAFile)file;
		} else if (file instanceof DataGroup) {
			dataGroups.add((DataGroup)file);
		} else {
			throw new IllegalArgumentException("Unsupported LDS file " + file.getClass().getCanonicalName());
		}
	}
	
	public void add(InputStream inputStream) throws IOException {
		LDSFile file = LDSFileUtil.getLDSFile(inputStream);
		add(file);
	}
	
	public CVCAFile getCVCAFile() {
		return cvca;
	}
	
	/**
	 * Gets the EF.SOd.
	 * 
	 * @return the EF.SOd
	 */
	public SODFile getSODFile() {
		return sod;
	}
}
