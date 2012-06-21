package de.kp.ames.webdav;
/**
 *	Copyright 2012 Dr. Krusche & Partner PartG
 *
 *	AMES-WebDAV is free software: you can redistribute it and/or 
 *	modify it under the terms of the GNU General Public License 
 *	as published by the Free Software Foundation, either version 3 of 
 *	the License, or (at your option) any later version.
 *
 *	AMES-WebDAV is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 *  See the GNU General Public License for more details. 
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this software. If not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataSource;

/**
 * @author Stefan Krusche (krusche@dr-kruscheundpartner.de)
 *
 */

public class WebDAVFile {

	/* 
	 * The predefined buffer size when retrieving repository items
	 */
	private static final int BUFFER_SIZE = 1024;
	
	private byte[] file;
	private InputStream inputStream;

	/**
	 * Constructor
	 */
	public WebDAVFile() {		
	}

	/**
	 * Constructor requires input stream
	 * 
	 * @param inputStream
	 * @param mimetype
	 */
	public WebDAVFile(InputStream inputStream) {
		setInputStream(inputStream);
	}	

	/**
	 * @return
	 */
	public InputStream getInputStream() {
		
		if (this.inputStream == null)
			return new ByteArrayInputStream(file);

		return this.inputStream;
	}

	/**
	 * @param inputStream
	 * @param mimetype
	 */
	public void setInputStream(InputStream inputStream) {
		
		this.inputStream = inputStream;
		this.file = getByteArrayFromInputStream(inputStream);
		
	}

    /**
     * @param is
     * @return
     */
    public static byte[] getByteArrayFromInputStream(InputStream is) {
    	
    	ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

    	byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        
        try {
        	while ((len = is.read(buffer, 0, buffer.length)) != -1) {
        		baos.write(buffer, 0, len);
        	}
        	//is.close();

        } catch (IOException e) {
        	e.printStackTrace();
        }
        
        return baos.toByteArray();
        //return content;
    
    }

    /**
     * @param bytes
     * @param mimetype
     * @return
     */
    public static ByteArrayDataSource createByteArrayDataSource(byte[] bytes, String mimetype) {
    	return new ByteArrayDataSource(bytes, mimetype);
    }
  
}

class ByteArrayDataSource implements DataSource {
   
	byte bytes[];
	String contentType;
   
	public ByteArrayDataSource(byte bytes[], String contentType) {
	   
		this.bytes = bytes;
		this.contentType = contentType;
       
	}
   
	public String getContentType() {
		return contentType;
	}
   
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(bytes);
	}
   
	public String getName() {
		// unknown
		throw new UnsupportedOperationException("ByteArrayDataSource.getName()");
	}
   
	public OutputStream getOutputStream() throws java.io.IOException {
		// not required, do not expose
		throw new UnsupportedOperationException("ByteArrayDataSource.getOutputStream()");
	}

}
