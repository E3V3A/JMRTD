package sos.smartcards;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import sos.util.Hex;

/**
 * This class implements some basic file selection / reading / writing 
 * routines from the  ISO7816-4 standard.
 * 
 * TODO: Work in heavy progress
 *  
 * @author woj
 *
 */
public abstract class FileSystemStructuredAbstract implements
        FileSystemStructured {
    
    public static short MF_ID = 0x3F00;
    
    private CardService service = null;
    private short selectedFID = 0;
    private int length = -1;
    private int p2 = 0;
    private int selectLe = 256;
    private FileInfo fileInfo = null;
    
    public FileSystemStructuredAbstract(CardService service) {
        this.service = service;
    }

    public FileSystemStructuredAbstract(CardService service, boolean fileInfo) {
        this.service = service;
        this.p2 = fileInfo ? 0x00 : 0x0C;
        this.selectLe = fileInfo ? 256 : 0;
    }

    public int getFileLength() throws CardServiceException {
        return length;
    }

    public short getSelectedFID() {
        return selectedFID;
    }

    public byte[] readBinary(int offset, int length)
            throws CardServiceException {
        byte[] p1p2 = Hex.hexStringToBytes(Hex.shortToHexString((short)offset));        
        new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_READ_BINARY, p1p2[0], p1p2[1], length);
        return null;
    }

    private void selectFile(byte[] data, int p1) throws CardServiceException {
        CommandAPDU command = createSelectFileAPDU(p1, p2, data, selectLe);
        ResponseAPDU response = service.transmit(command);
        if(response.getSW() != ISO7816.SW_NO_ERROR) {
            throw new CardServiceException("File could not be selected. (File ID "+Hex.bytesToHexString(data)+", SW: "+Hex.shortToHexString((short)response.getSW())+")");
        }
        // store selected fid:
        // 0, 4, 8 absolute
        // 1, 2, 9, relative
        // 3 parent
        this.fileInfo = new FileInfo(response.getData());
        if(this.fileInfo.fid != -1) {
            selectedFID = this.fileInfo.fid;
        }
        if(this.fileInfo.fileLength != -1) {
            length = this.fileInfo.fileLength;
        }
    }

    private void selectFile(short fid, int p1) throws CardServiceException {
        byte[] fidbytes = (fid == 0) ? new byte[0] : new byte[]{ (byte) ((fid >> 8) & 0x000000FF), (byte) (fid & 0x000000FF) };
        selectFile(fidbytes, p1);
    }
    
    public void selectFile(short fid) throws CardServiceException {
        selectFile(fid, 0x00);
    }

    public void selectMF() throws CardServiceException {
        selectFile((short)0, 0);
    }

    public void selectParent() throws CardServiceException {
        selectFile((short)0, 0x03);
    }
    
    public void selectEFRelative(short fid) throws CardServiceException {
        selectFile(fid, 0x02);
    }
    
    public void selectDFRelative(short fid) throws CardServiceException {
        selectFile(fid, 0x01);        
    }

    public void selectAID(byte[] aid) throws CardServiceException {
        selectFile(aid, 0x04);
    } 
    
    public void selectPath(byte[] path) throws CardServiceException {
        selectFile(path, 0x08);
    } 

    public void selectPathRelative(byte[] path) throws CardServiceException {
        selectFile(path, 0x09);        
    } 
    
    private CommandAPDU createSelectFileAPDU(int p1, int p2, byte[] data, int le) {
        return (le == 0) ? 
                new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_SELECT_FILE, p1, p2, data)
        : new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_SELECT_FILE, p1, p2, data, le);
    }

}
