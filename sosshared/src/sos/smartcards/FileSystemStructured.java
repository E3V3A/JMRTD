package sos.smartcards;

/**
 * Implement this interface to tell {@link CardFileInputStream}
 * how to deal with card files.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
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
	 * @param length the number of bytes to read (the result may be shorter, though)
	 * @return contents of currently selected file, contains at least 1 byte, at most length.
	 * @throws CardServiceException on error (for instance: end of file)
	 */
	byte[] readBinary(int offset, int length) throws CardServiceException;
	
	/**
	 * Identifies the currently selected file.
	 * 
	 * @return a file identifier or 0 if no file is selected.
	 */
	short getSelectedFID();
}
