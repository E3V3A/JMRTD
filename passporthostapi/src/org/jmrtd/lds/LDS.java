package org.jmrtd.lds;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LDS {

	private Map<Short, InputStream> streams;

	private COMFile com;
	private InputStream comInputStream;

	private Map<Short, DataGroup> dataGroups;
	private Map<Short, InputStream> dataGroupInputStreams;

	private CVCAFile cvca;
	private InputStream cvcaInputStream;

	private SODFile sod;
	private InputStream sodInputStream;
	
	public LDS(COMFile com, Collection<DataGroup> dataGroups, SODFile sod) {
		this(com, dataGroups, null, sod);
	}

	public LDS(COMFile com, Collection<DataGroup> dataGroups, CVCAFile cvca, SODFile sod) {
		this.com = com;
		this.dataGroups = new HashMap<Short, DataGroup>();
		this.streams = new HashMap<Short, InputStream>();
		for (DataGroup dataGroup: dataGroups) {
			int tag = dataGroup.getTag();
			short fid = LDSFileUtil.lookupFIDByTag(tag);
			this.dataGroups.put(fid, dataGroup);
		}
		this.cvca = cvca;
		this.sod = sod;
	}

	/**
	 * Gets the EF.COM.
	 * 
	 * @return the EF.COM
	 */
	public COMFile getCOMFile() {
		if (com != null) { return com; }
		if (comInputStream == null) { return null; }
		try {
			com = new COMFile(comInputStream);
			return com;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	public List<Short> getDataGroupList() {
		SODFile sod = getSODFile();
		List<Short> result = new ArrayList<Short>();
		Set<Integer> dgNumbers = sod.getDataGroupHashes().keySet();
		for (int dgNumber: dgNumbers) {
			short fid = LDSFileUtil.lookupFIDByDataGroupNumber(dgNumber);
			result.add(fid);
		}
		return result;		
	}

	/**
	 * Gets a (spine-deep copy) of the collection of data groups in this LDS.
	 * 
	 * @return the data groups in this LDS
	 */
	public Map<Short, DataGroup> getDataGroups() {
		List<Short> dgFids = getDataGroupList();
		for (short fid: dgFids) {
			DataGroup dataGroup = dataGroups.get(fid);
			if (dataGroup == null) {
				try {
					InputStream inputStream = dataGroupInputStreams.get(fid);
					if (inputStream == null) { continue; } // Skip this fid, we don't have it
					dataGroup = (DataGroup)LDSFileUtil.getLDSFile(inputStream);
					dataGroups.put(fid, dataGroup);
				} catch (IOException ioe) {
					continue; // Skip this fid
				}
			}
		}
		return dataGroups;
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
			int tag = ((DataGroup)file).getTag();
			short fid = LDSFileUtil.lookupFIDByTag(tag);
			dataGroups.put(fid, (DataGroup)file);
		} else {
			throw new IllegalArgumentException("Unsupported LDS file " + file.getClass().getCanonicalName());
		}
	}

	public void add(short fid, InputStream inputStream) throws IOException {
		streams.put(fid, inputStream);
	}

	public CVCAFile getCVCAFile() {
		if (cvca != null) { return cvca; }
		if (cvcaInputStream == null) { return null; }
		try {
			cvca = new CVCAFile(cvcaInputStream);
			return cvca;
		} catch (IOException ioe) {
			return null;
		}
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
