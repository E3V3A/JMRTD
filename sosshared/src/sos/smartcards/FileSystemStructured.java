package sos.smartcards;

public interface FileSystemStructured
{
	/**
	 * Selects a file.
	 * 
	 * @param fid indicates which file to select
	 * @throws CardServiceException in case of error
	 */
	void selectFile(short fid) throws CardServiceException;

	/**
	 * Gets the length of the currently selected file.
	 * 
	 * @return the length of the currently selected file.
	 * @throws CardServiceException 
	 */
	int getFileLength() throws CardServiceException;
	
	/**
	 * Reads a fragment of the currently selected file.
	 * 
	 * @param offset offset
	 * @param length length
	 * @return contents of currently selected file.
	 * @throws CardServiceException
	 */
	byte[] readBinary(int offset, int length) throws CardServiceException;
	
	/**
	 * Identifies the currently selected file.
	 * 
	 * @return a file identifier or 0 if no file is selected.
	 */
	short getSelectedFID();
}
