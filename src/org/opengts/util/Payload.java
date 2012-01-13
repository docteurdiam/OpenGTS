// ----------------------------------------------------------------------------
// Copyright 2007-2011, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Read/Write binary fields
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2007/02/25  Martin D. Flynn
//     -Made 'decodeLong'/'encodeLong' public.
//     -Moved to 'org.opengts.util'.
//  2007/03/11  Martin D. Flynn
//     -Added check for remaining available read/write bytes
//  2008/02/04  Martin D. Flynn
//     -Added 'encodeDouble'/'decodeDouble' methods for encoding and decoding
//      32-bit and 64-bit IEEE 754 floating-point values.
//     -Added Big/Little-Endian flag
//     -Added 'writeZeroFill' method
//     -Fixed 'writeBytes' to proper blank fill written fields
//  2009/09/23  Martin D. Flynn
//     -Added methods "peekByte", "saveIndex", "restoreIndex"
//  2010/10/21  Martin D. Flynn
//     -Added little-endian support for writeLong/writeDouble
//  2010/11/29  Martin D. Flynn
//     -When parsing fixed-length String fields, terminate String at first
//      null-character found.
//  2011/04/06  Martin D. Flynn
//     -Change "overflow" error message to a warning instead
//  2011/06/16  Martin D. Flynn
//     -Added additional debug logging (displaying the parse hex and value).
//  2011/07/01  Martin D. Flynn
//     -Added "scanForPattern".  Added "peekBytes(offset, length)"
//  2011/08/21  Martin D. Flynn
//     -Added parsing debug message option
//     -Removed "readXXXXX(...)" that did not specify a parsng default value.
//  2011/10/03  Martin D. Flynn
//     -Added "readStringHex"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;

/**
*** For reading/writing binary fields
**/

public class Payload
{
    
    // ------------------------------------------------------------------------

    public  static final int        DEFAULT_MAX_PAYLOAD_LENGTH = 255;
    
    public  static final byte       EMPTY_BYTE_ARRAY[] = new byte[0];
    
    private static final boolean    DEFAULT_BIG_ENDIAN = true;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static boolean DebugLogging = false;
    
    /**
    *** Sets internal debug logging 
    **/
    public static void SetDebugLogging(boolean debug)
    {
        DebugLogging = debug;
    }
    
    /**
    *** Gets internal debug logging 
    **/
    public static boolean GetDebugLogging()
    {
        return DebugLogging;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Check for read overflow
    **/
    protected static void checkOverflow(int length, int maxLen, int frame)
    {
        if (maxLen < length) {
            String frameStr = Print._getStackFrame(frame + 1);
            Print.logWarn("Payload overflow at \"" + frameStr + "\"");
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private byte        payload[] = null;
    private int         size = 0;

    private int         index = 0;
    private int         indexSnapshot = -1;

    private boolean     bigEndian = DEFAULT_BIG_ENDIAN;

    // ------------------------------------------------------------------------

    /**
    *** Destination Constructor
    **/
    public Payload()
    {
        // DESTINATION: configure for creating a new binary payload
        this(DEFAULT_MAX_PAYLOAD_LENGTH, DEFAULT_BIG_ENDIAN);
    }

    /**
    *** Destination Constructor
    *** @param maxPayloadLen The maximum payload length
    **/
    public Payload(int maxPayloadLen)
    {
        // DESTINATION: configure for creating a new binary payload
        this(maxPayloadLen, DEFAULT_BIG_ENDIAN);
    }

    /**
    *** Destination Constructor
    *** @param maxPayloadLen The maximum payload length
    *** @param bigEndian If the payload uses big-endian byte ordering
    **/
    public Payload(int maxPayloadLen, boolean bigEndian)
    {
        // DESTINATION: configure for creating a new binary payload
        this.payload = (maxPayloadLen >= 0)? new byte[maxPayloadLen] : null;
        this.size    = 0; // no 'size' yet
        this.index   = 0; // start at index '0' for writing
        this.setBigEndian(bigEndian);
    }

    // ------------------------------------------------------------------------

    /**
    *** Source Constuctor
    *** @param b The payload (default big-endian byte ordering)
    **/
    public Payload(byte b[])
    {
        // SOURCE: configure for reading a binary payload
        this(b, DEFAULT_BIG_ENDIAN);
    }

    /**
    *** Source Constuctor
    *** @param b The payload
    *** @param bigEndian If the payload uses big-endian byte ordering
    **/
    public Payload(byte b[], boolean bigEndian)
    {
        // SOURCE: configure for reading a binary payload
        this(b, 0, ((b != null)? b.length : 0), bigEndian);
    }

    /**
    *** Source Constuctor
    *** @param b The byte array to copy the payload from (defalt big-endian byte ordering
    *** @param ofs The offset at which copying of <code>b</code> should begin
    *** @param len The length of the resultant payload
    **/
    public Payload(byte b[], int ofs, int len)
    {
        this(b, ofs, len, DEFAULT_BIG_ENDIAN);
    }
    
    /**
    *** Source Constuctor
    *** @param n   The byte array to copy the payload from (defalt big-endian byte ordering
    *** @param ofs The offset at which copying of <code>b</code> should begin
    *** @param len The length of the resultant payload
    *** @param bigEndian If the payload uses big-endian byte ordering
    **/
    public Payload(byte n[], int ofs, int len, boolean bigEndian)
    {
        // SOURCE: configure for reading a binary payload
        this();
        if ((n == null) || (ofs >= n.length)) {
            this.payload = new byte[0];
            this.size    = 0;
            this.index   = 0;
        } else
        if ((ofs == 0) && (n.length == len)) {
            this.payload = n;
            this.size    = n.length;
            this.index   = 0;
        } else {
            if (len > (n.length - ofs)) { len = n.length - ofs; }
            this.payload = new byte[len];
            System.arraycopy(n, ofs, this.payload, 0, len);
            this.index   = 0;
            this.index   = 0;
            this.size    = len;
        }
        this.setBigEndian(bigEndian);
        if (DebugLogging) { this.printDebug_source(1); }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the byte ordering of the payload to "Big-Endian"<br>
    *** Also called "Network Byte Order", "MSB first", ...
    *** ("Big-Endian" is the default format)
    **/
    public void setBigEndian()
    {
        this.setBigEndian(true);
    }

    /**
    *** Sets the byte ordering of the payload to "Little-Endian"<br>
    *** Also called "LSB first", ...
    **/
    public void setLittleEndian()
    {
        this.setBigEndian(false);
    }

    /**
    *** Sets the byte ordering of the payload
    *** @param bigEndFirst True for big-endian, false for little-endian numeric encoding
    **/
    public void setBigEndian(boolean bigEndFirst)
    {
        this.bigEndian = bigEndFirst;
    }
    
    /**
    *** Gets the byte ordering of the payload
    *** @return True if big-endian, false if little-endian
    **/
    public boolean getBigEndian()
    {
        return this.bigEndian;
    }

    /**
    *** Returns true if the payload is big-endian
    *** @return True if big-endian, false if little-endian
    **/
    public boolean isBigEndian()
    {
        return this.bigEndian;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** For an output/write Payload, returns the number of bytes written.
    *** For an input/read Payload, return the total number of bytes contained in this Payload.
    *** @return The current size of the payload
    **/
    public int getSize()
    {
        return this.size;
    }
    
    /**
    *** Resets the payload to an empty state
    **/
    public void clear()
    {
        this.size  = 0;
        this.index = 0;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Return the backing byte array (as-is)
    *** @return The backing byte array
    **/
    private byte[] _payloadBytes()
    {
        return this.payload; // may be null
    }

    /**
    *** Zero bytes in buffer
    **/
    private int _zeroBytes(int zLen)
    {
        if (zLen > 0) {
            byte b[] = this._payloadBytes();
            if (b != null) {
                for (int z = 0; z < zLen; z++) {
                    b[this.index + z] = (byte)0;
                }
            }
            this.index += zLen;
            if (this.size < this.index) { this.size = this.index; }
            return zLen;
        } else {
            return 0;
        }
    }

    /**
    *** Copy bytes to buffer
    **/
    private int _putBytes(byte n[], int nOfs, int nLen)
    {
        if (nLen > 0) {
            byte b[] = this._payloadBytes();
            if (b != null) {
                System.arraycopy(n, nOfs, b, this.index, nLen);
            }
            this.index += nLen;
            if (this.size < this.index) { this.size = this.index; }
            return nLen;
        } else {
            return 0;
        }
    }
    
    /**
    *** Copy bytes from buffer
    **/
    private byte[] _getBytes(int bOfs, int nLen)
    {
        byte b[] = this._payloadBytes();
        if (b == null) {
            return new byte[0];
        } else
        if (nLen == b.length) {
            return b;
        } else {
            byte n[] = new byte[nLen];
            System.arraycopy(b, bOfs, n, 0, nLen);
            return n;
        }
    }
    
    /**
    *** Return a byte array representing the data currently in the payload (may be a copy)
    *** @return The byte array currently in the payload (as-is)
    **/
    public byte[] getBytes()
    {
        // return the full payload (regardless of the state of 'this.index')
        byte b[] = this._getBytes(0, this.size);
        return b;
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Gets the current read/write index
    *** @return The index
    **/
    public int getIndex()
    {
        return this.index;
    }
    
    /**
    *** Resets the read/write index to '<code>0</code>'
    **/
    public void resetIndex()
    {
        // this makes Payload a data source
        this.resetIndex(0);
    }

    /**
    *** Resets the read/write index to the specified value
    *** @param ndx The value to set the index
    **/
    public void resetIndex(int ndx)
    {
        this.index = (ndx <= 0)? 0 : ndx;
    }

    /**
    *** Saves the current index.
    *** @return True if the operation was successful
    **/
    public boolean saveIndex()
    {
        this.indexSnapshot = this.getIndex();
        return true;
    }
    
    /**
    *** Restores the current index.
    *** @return True if the operation was successful
    **/
    public boolean restoreIndex()
    {
        if (this.indexSnapshot >= 0) {
            this.resetIndex(this.indexSnapshot);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the maximum allowed read length up to the specific length
    *** @param length   The preferred length
    *** @return The maximum length
    **/
    public int getMaximumReadLength(int length)
    {
        return ((this.index + length) <= this.size)? length : (this.size - this.index);
    }

    /**
    *** Gets the number of remaining available to read
    *** @return The number of availible bytes to read
    **/
    public int getAvailableReadLength()
    {
        return (this.size - this.index);
    }

    /**
    *** Returns true if there are at least <code>length</code> bytes that 
    *** can be read from the payload
    *** @return True if there at least <code>length</code> readable bytes
    ***         in the payload
    **/
    public boolean isValidReadLength(int length)
    {
        return ((this.index + length) <= this.size);
    }

    /**
    *** Returns true if there are bytes available for reading
    *** @return True if there are bytes availible for reading
    **/
    public boolean hasAvailableRead()
    {
        return (this.getAvailableReadLength() > 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Return the maximum allowed write length up to the specific length
    *** @param wrtLen   The preferred length
    *** @return The maximum length
    **/
    public int getMaximumWriteLength(int wrtLen)
    {
        byte b[] = this._payloadBytes();
        if (b == null) {
            return wrtLen; // special case, always allow writing
        } else {
            return ((this.index + wrtLen) <= b.length)? wrtLen : (b.length - this.index);
        }
    }

    /**
    *** Gets the nubmer of remaining availible bytes to write
    *** @return The remaining available bytes to write
    **/
    public int getAvailableWriteLength()
    {
        byte b[] = this._payloadBytes();
        if (b == null) {
            return 20000; // special case, always allow writing
        } else {
            return (b.length - this.index);
        }
    }

    /**
    *** Returns true if there are at least <code>length</code> bytes that 
    *** can be writen to the payload
    *** @return True if there at least <code>length</code> writeable bytes
    ***         in the payload
    **/
    public boolean isValidWriteLength(int length)
    {
        byte b[] = this._payloadBytes();
        if (b == null) {
            return true;
        } else {
            return ((this.index + length) <= b.length);
        }
    }

    /**
    *** Returns true if there are bytes available for writing
    **/
    public boolean hasAvailableWrite()
    {
        return (this.getAvailableWriteLength() > 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Skip a specified number of bytes
    *** @param length The number of bytes to skip
    **/
    public void readSkip(int length)
    {
        this.readSkip(length, null/*msg*/);
    }
    
    /**
    *** Skip a specified number of bytes
    *** @param length The number of bytes to skip
    *** @param msg    Debug message (used during "DebugLogging")
    **/
    public void readSkip(int length, String msg)
    {
        int maxLen = this.getMaximumReadLength(length);
        Payload.checkOverflow(length, maxLen, 1);
        if (maxLen <= 0) {
            // nothing to skip
            return;
        } else {
            if (DebugLogging) { this.printDebug(1, maxLen, (byte[])null, msg); }
            this.index += maxLen;
            return;
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Read the next byte without moving the read pointer
    *** @return The next byte (-1 if no bytes are available)
    **/
    public int peekByte()
    {
        if (this.index < this.size) {
            byte b[] = this._payloadBytes();
            if (b == null) {
                return -1; // reading not allowed when payload is null
            } else {
                return (int)b[this.index] & 0xFF;
            }
        } else {
            return -1;
        }
    }
    
    /** 
    *** Reads the specified number of bytes from the stream
    *** The read pointer is left as is
    *** @param length  The number of bytes to read
    *** @return The byte array
    **/
    public byte[] peekBytes(int length)
    {
        // This will read 'length' bytes, or the remaining bytes, whichever is less

        /* '0' offset */
        int peekStart = this.index;

        /* max length */
        int maxLen = ((length >= 0) && ((peekStart + length) <= this.size))? 
            length : 
            (this.size - peekStart);

        /* read */
        if (maxLen <= 0) {
            // no room left
            return new byte[0];
        } else {
            byte n[] = this._getBytes(peekStart, maxLen);
            return n;
        }

    }
    
    /** 
    *** Reads the specified number of bytes from the stream
    *** The read pointer is left as is
    *** @param offset  The offset from the current index
    *** @param length  The number of bytes to read
    *** @return The byte array
    **/
    public byte[] peekBytes(int offset, int length)
    {
        // This will read 'length' bytes, or the remaining bytes, whichever is less

        /* invalid offset */
        if (offset < 0) {
            return new byte[0];
        }
        int peekStart = this.index + offset;

        /* max length */
        int maxLen = ((length >= 0) && ((peekStart + length) <= this.size))? 
            length : 
            (this.size - peekStart);

        /* read */
        if (maxLen <= 0) {
            // no room left
            return new byte[0];
        } else {
            byte n[] = this._getBytes(peekStart, maxLen);
            return n;
        }

    }

    // ------------------------------------------------------------------------

    /** 
    *** Scans the remainder of the payload for the specified byte pattern
    *** and returns the offset from the current pointer to the beginning
    *** of the matching pattern.  Returns -1 if the pattern was not found.
    *** @param p The byte pattern array
    *** @return  The byte offset, or -1 if not found
    **/
    public int scanForPattern(byte p[])
    {
        return this.scanForPattern(p, 0);
    }
    
    /** 
    *** Scans the remainder of the payload for the specified byte pattern
    *** and returns the offset from the current pointer to the beginning
    *** of the matching pattern.  Returns -1 if the pattern was not found.
    *** @param p        The byte pattern array
    *** @param fromNdx  The the starting index relative to the current payload index
    *** @return  The byte offset, or -1 if not found
    **/
    public int scanForPattern(byte p[], int fromNdx)
    {

        /* no pattern */
        if ((p == null) || (p.length == 0)) {
            return -1;
        }

        /* bounds check 'fromNdx' */
        if (fromNdx < 0) {
            fromNdx = 0;
        }

        /* current index */
        byte b[] = this._payloadBytes();
        int lastNdx  = this.size - p.length;
        for (int ndx = (this.index + fromNdx); ndx <= lastNdx; ndx++) {
            // TODO: could be optimized
            if (p[0] == b[ndx]) {
                int m = 1;
                for (;(m < p.length) && (p[m] == b[ndx+m]); m++);
                if (m == p.length) {
                    // found pattern match
                    return ndx - this.index;
                }
            }
        }

        /* not found */
        return -1;

    }
 
    // ------------------------------------------------------------------------

    /**
    *** Read <code>length</code< of bytes from the payload
    *** @param length The number fo bytes to read from the payload
    *** @return The read byte array
    **/
    public byte[] readBytes(int length)
    {
        return this.readBytes(length, null/*msg*/);
    }
    
    /**
    *** Read <code>length</code< of bytes from the payload
    *** @param length The number fo bytes to read from the payload
    *** @param msg    Debug message (used during "DebugLogging")
    *** @return The read byte array
    **/
    public byte[] readBytes(int length, String msg)
    {
        // This will read 'length' bytes, or the remaining bytes, whichever is less
        int maxLen = (length >= 0)? this.getMaximumReadLength(length) : (this.size - this.index);
        Payload.checkOverflow(length, maxLen, 1);
        if (maxLen <= 0) {
            // no room left
            return new byte[0];
        } else {
            byte n[] = this._getBytes(this.index, maxLen);
            if (DebugLogging) { this.printDebug(1, maxLen, n, msg); }
            this.index += maxLen;
            return n;
        }
    }
   
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Reverses the byte order of the specified value
    *** @param value      The Long value to reverse
    *** @param byteCount  The number of bytes to reverse
    *** @return The reversed value
    **/
    public static long reverseByteOrder(long value, int byteCount)
    {
        long accum = 0L;
        for (int i = 0; i < byteCount; i++) {
            accum |= ((value >> (i * 8)) & 0xFF) << (((byteCount - 1) - i) * 8);
        }
        return accum;
    }
    
    /**
    *** Decodes a <code>long</code> value from bytes
    *** @param data The byte array to decode the value from
    *** @param ofs The offset into <code>data</code> to start decoding from
    *** @param len The number of bytes to decode the value from
    *** @param bigEndian True if the bytes are big-endian ordered, false if little-endian
    *** @param signed If the encoded bytes represent a signed value
    *** @param dft The default value if a value cannot be decoded
    *** @return The decoded value, or the default
    **/
    public static long decodeLong(byte data[], int ofs, int len, boolean bigEndian, boolean signed, long dft)
    {
        if ((data != null) && (data.length >= (ofs + len))) {
            if (bigEndian) {
                // Big-Endian order
                // { 0x01, 0x02, 0x03 } -> 0x010203
                long n = (signed && ((data[ofs] & 0x80) != 0))? -1L : 0L;
                for (int i = ofs; i < ofs + len; i++) {
                    n = (n << 8) | ((long)data[i] & 0xFF); 
                }
                return n;
            } else {
                // Little-Endian order
                // { 0x01, 0x02, 0x03 } -> 0x030201
                long n = (signed && ((data[ofs + len - 1] & 0x80) != 0))? -1L : 0L;
                for (int i = ofs + len - 1; i >= ofs; i--) {
                    n = (n << 8) | ((long)data[i] & 0xFF); 
                }
                return n;
            }
        } else {
            return dft;
        }
    }

    /**
    *** Read a <code>long</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @param frame     The current stackframe index
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    protected long _readLong(int length, long dft, boolean bigEndian, int frame, String msg)
    {
        int maxLen = this.getMaximumReadLength(length);
        Payload.checkOverflow(length, maxLen, frame + 1);
        if (maxLen <= 0) {
            // nothing to read
            return dft;
        } else {
            byte b[] = this._payloadBytes();
            long val = Payload.decodeLong(b, this.index, maxLen, bigEndian, true, dft);
            if (DebugLogging) { this.printDebug(frame + 1, maxLen, bigEndian, true, val, msg); }
            this.index += maxLen;
            return val;
        }
    }

    /**
    *** Read a <code>long</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @return The decoded value, or the default value
    **/
    public long readLong(int length, long dft, boolean bigEndian)
    {
        return this._readLong(length, dft, bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read a <code>long</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public long readLong(int length, long dft, boolean bigEndian, String msg)
    {
        return this._readLong(length, dft, bigEndian, 1, msg);
    }

    /**
    *** Read a <code>long</code> value from payload (with default)
    *** @param length The number of bytes to decode the value from
    *** @param dft The default value if a value could not be decoded
    *** @return The decoded value, or the default value
    **/
    public long readLong(int length, long dft)
    {
        return this._readLong(length, dft, this.bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read a <code>long</code> value from payload (with default)
    *** @param length   The number of bytes to decode the value from
    *** @param dft      The default value if a value could not be decoded
    *** @param msg      Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public long readLong(int length, long dft, String msg)
    {
        return this._readLong(length, dft, this.bigEndian, 1, msg);
    }

    /**
    *** Read a <code>long</code> value from payload
    *** @param length The number of bytes to decode the value from
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @return The decoded value
    **/
    //public long readLong(int length, boolean bigEndian)
    //{
    //    return this._readLong(length, 0L, bigEndian, 1, null/*msg*/);
    //}


    /**
    *** Read a <code>long</code> value from payload
    *** @param length The number of bytes to decode the value from
    *** @return The decoded value
    **/
    //public long readLong(int length)
    //{
    //    return this._readLong(length, 0L, this.bigEndian, 1, null/*msg*/);
    //}

    /**
    *** Read a <code>int</code> value from payload (with default)
    *** @param length The number of bytes to decode the value from
    *** @param dft The default value if a value could not be decoded
    *** @return The decoded value, or the default value
    **/
    public int readInt(int length, int dft)
    {
        return (int)this._readLong(length, (long)dft, this.bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read a <code>int</code> value from payload (with default)
    *** @param length   The number of bytes to decode the value from
    *** @param dft      The default value if a value could not be decoded
    *** @param msg      Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public int readInt(int length, int dft, String msg)
    {
        return (int)this._readLong(length, (long)dft, this.bigEndian, 1, msg);
    }

    /**
    *** Read a <code>int</code> value from payload
    *** @param length The number of bytes to decode the value from
    *** @return The decoded value, or '0' if no remaining bytes to read
    **/
    //public int readInt(int length)
    //{
    //    return (int)this._readLong(length, 0L, this.bigEndian, 1, null/*msg*/);
    //}

    // ------------------------------------------------------------------------

    /**
    *** Read an unsigned <code>long</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @param frame     The current stackframe index
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    protected long _readULong(int length, long dft, boolean bigEndian, int frame, String msg)
    {
        int maxLen = this.getMaximumReadLength(length);
        Payload.checkOverflow(length, maxLen, frame + 1);
        if (maxLen <= 0) {
            // nothing to read
            return dft;
        } else {
            byte b[] = this._payloadBytes();
            long val = Payload.decodeLong(b, this.index, maxLen, bigEndian, false, dft);
            if (DebugLogging) { this.printDebug(frame + 1, maxLen, bigEndian, false, val, msg); }
            this.index += maxLen;
            return val;
        }
    }

    /**
    *** Read an unsigned <code>long</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @return The decoded value, or the default value
    **/
    public long readULong(int length, long dft, boolean bigEndian)
    {
        return this._readULong(length, dft, bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read an unsigned <code>long</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public long readULong(int length, long dft, boolean bigEndian, String msg)
    {
        return this._readULong(length, dft, bigEndian, 1, msg);
    }

    /**
    *** Read an unsigned <code>long</code> value from payload (with default)
    *** @param length The number of bytes to decode the value from
    *** @param dft The default value if a value could not be decoded
    *** @return The decoded value, or the default value
    **/
    public long readULong(int length, long dft)
    {
        return this._readULong(length, dft, this.bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read an unsigned <code>long</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public long readULong(int length, long dft, String msg)
    {
        return this._readULong(length, dft, this.bigEndian, 1, msg);
    }

    /**
    *** Read an unsigned <code>long</code> value from payload
    *** @param length The number of bytes to decode the value from
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @return The decoded value
    **/
    //public long readULong(int length, boolean bigEndian)
    //{
    //    return this._readULong(length, 0L, bigEndian, 1, null/*msg*/);
    //}

    /**
    *** Read an unsigned <code>long</code> value from payload
    *** @param length The number of bytes to decode the value from
    *** @return The decoded value
    **/
    //public long readULong(int length)
    //{
    //    return this._readULong(length, 0L, this.bigEndian, 1, null/*msg*/);
    //}

    /**
    *** Read an unsigned <code>int</code> value from payload (with default)
    *** @param length The number of bytes to decode the value from
    *** @param dft The default value if a value could not be decoded
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @return The decoded value, or the default value
    **/
    public int readUInt(int length, int dft, boolean bigEndian)
    {
        return (int)this._readULong(length, (long)dft, bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read an unsigned <code>int</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public int readUInt(int length, int dft, boolean bigEndian, String msg)
    {
        return (int)this._readULong(length, (long)dft, bigEndian, 1, msg);
    }

    /**
    *** Read an unsigned <code>int</code> value from payload (with default)
    *** @param length The number of bytes to decode the value from
    *** @param dft The default value if a value could not be decoded
    *** @return The decoded value, or the default value
    **/
    public int readUInt(int length, int dft)
    {
        return (int)this._readULong(length, (long)dft, this.bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read an unsigned <code>int</code> value from payload (with default)
    *** @param length    The number of bytes to decode the value from
    *** @param dft       The default value if a value could not be decoded
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public int readUInt(int length, int dft, String msg)
    {
        return (int)this._readULong(length, (long)dft, this.bigEndian, 1, msg);
    }

    /**
    *** Read an unsigned <code>int</code> value from payload (with default)
    *** @param length The number of bytes to decode the value from
    *** @param bigEndian True to read bytes in big-endian order, otherwise little-endian
    *** @return The decoded value, or the default value
    **/
    //public int readUInt(int length, boolean bigEndian)
    //{
    //    return (int)this._readULong(length, 0L, bigEndian, 1, null/*msg*/);
    //}

    /**
    *** Read an unsigned <code>int</code> value from payload (with default)
    *** @param length The number of bytes from which to decode the value
    *** @return The decoded value, or the default value
    **/
    //public int readUInt(int length)
    //{
    //    return (int)this._readULong(length, 0L, this.bigEndian, 1, null/*msg*/);
    //}

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Read an unsigned BCD encoded <code>long</code> value from the payload.
    *** @param length    The number of bytes from which to decode the value
    *** @param dft       The default value if a value could not be decoded
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public long readLongBCD(int length, long dft, String msg)
    {
        int maxLen = this.getMaximumReadLength(length);
        Payload.checkOverflow(length, maxLen, 1);
        if (maxLen <= 0) {
            // nothing to read
            return dft;
        } else {
            byte b[] = this._payloadBytes();
            long val = 0L;
            for (int i = this.index; i < (this.index + maxLen); i++) {
                long N1 = (long)(b[i] >> 4) & 0xF;
                long N2 = (long)(b[i] >> 0) & 0xF;
                if ((N1 > 9) || (N2 > 9)) {
                    val = dft;
                    break;
                }
                val = (val * 10L) + N1;
                val = (val * 10L) + N2;
            }
            if (DebugLogging) { this.printDebug(1, maxLen, true, false, val, msg); }
            this.index += maxLen;
            return val;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Decodes a <code>double</code> value from bytes, using IEEE 754 format
    *** @param data The byte array from which to decode the <code>double</code> value
    *** @param ofs The offset into <code>data</code> to start decoding
    *** @param len The number of bytes from which the value is decoded
    *** @param bigEndian True if the bytes are in big-endian order, false if little-endian
    *** @param dft The default value if a value cannot be decoded
    *** @return The decoded value, or the default
    **/
    public static double decodeDouble(byte data[], int ofs, int len, boolean bigEndian, double dft)
    {
        // 'len' must be at lest 4
        if ((data != null) && (len >= 4) && (data.length >= (ofs + len))) {
            int flen = (len >= 8)? 8 : 4;
            long n = 0L;
            if (bigEndian) {
                // Big-Endian order
                // { 0x01, 0x02, 0x03, 0x04 } -> 0x01020304
                for (int i = ofs; i < ofs + flen; i++) {
                    n = (n << 8) | ((long)data[i] & 0xFF);
                }
            } else {
                // Little-Endian order
                // { 0x01, 0x02, 0x03, 0x04 } -> 0x04030201
                for (int i = ofs + flen - 1; i >= ofs; i--) {
                    n = (n << 8) | ((long)data[i] & 0xFF);
                }
            }
            if (flen == 8) {
                //Print.logInfo("Decoding 64-bit float " + n);
                return Double.longBitsToDouble(n);
            } else {
                //Print.logInfo("Decoding 32-bit float " + n);
                return (double)Float.intBitsToFloat((int)n);
            }
        } else {
            return dft;
        }
    }

    /**
    *** Read a <code>double</code> value from payload (with default), using IEEE 754 format
    *** @param length    The number of bytes from which the value is decoded
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True if the bytes are in big-endian order, false if little-endian
    *** @param frame     The current stackframe index
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    protected double _readDouble(int length, double dft, boolean bigEndian, int frame, String msg)
    {
        // 'length' must be at least 4
        int maxLen = this.getMaximumReadLength(length);
        Payload.checkOverflow(length, maxLen, frame + 1);
        if (maxLen <= 0) {
            // nothing to read
            return dft;
        } else {
            byte b[] = this._payloadBytes();
            double val = Payload.decodeDouble(b, this.index, maxLen, bigEndian, dft);
            if (DebugLogging) { this.printDebug(frame + 1, maxLen, bigEndian, val, msg); }
            this.index += maxLen;
            return val;
        }
    }

    /**
    *** Read a <code>double</code> value from payload (with default), using IEEE 754 format
    *** @param length The number of bytes from which the value is decoded
    *** @param dft The default value if a value could not be decoded
    *** @param bigEndian True if the bytes are in big-endian order, false if little-endian
    *** @return The decoded value, or the default value
    **/
    public double readDouble(int length, double dft, boolean bigEndian)
    {
        return this._readDouble(length, dft, bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read a <code>double</code> value from payload (with default), using IEEE 754 format
    *** @param length    The number of bytes from which the value is decoded
    *** @param dft       The default value if a value could not be decoded
    *** @param bigEndian True if the bytes are in big-endian order, false if little-endian
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public double readDouble(int length, double dft, boolean bigEndian, String msg)
    {
        return this._readDouble(length, dft, bigEndian, 1, msg);
    }

    /**
    *** Read a <code>double</code> value from payload (with default), using IEEE 754 format
    *** @param length The number of bytes from which the value is decoded
    *** @param dft The default value if a value could not be decoded
    *** @return The decoded value, or the default value
    **/
    public double readDouble(int length, double dft)
    {
        return this._readDouble(length, dft, this.bigEndian, 1, null/*msg*/);
    }

    /**
    *** Read a <code>double</code> value from payload (with default), using IEEE 754 format
    *** @param length    The number of bytes from which the value is decoded
    *** @param dft       The default value if a value could not be decoded
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The decoded value, or the default value
    **/
    public double readDouble(int length, double dft, String msg)
    {
        return this._readDouble(length, dft, this.bigEndian, 1, msg);
    }

    /**
    *** Read a <code>double</code> value from payload, using IEEE 754 format
    *** @param length The number of bytes from which the value is decoded
    *** @param bigEndian True if the bytes are in big-endian order, false if little-endian
    *** @return The decoded value
    **/
    //public double readDouble(int length, boolean bigEndian)
    //{
    //    // 'length' must be at least 4
    //    return this._readDouble(length, 0.0, bigEndian, 1, null/*msg*/);
    //}

    /**
    *** Read a <code>double</code> value from payload, using IEEE 754 format
    *** @param length The number of bytes from which the value is decoded
    *** @return The decoded value
    **/
    //public double readDouble(int length)
    //{
    //    // 'length' must be at least 4
    //    return this._readDouble(length, 0.0, this.bigEndian, 1, null/*msg*/);
    //}

    // ------------------------------------------------------------------------
    
    /**
    *** Read a string from the payload.
    *** The string is read until (whichever comes first):
    *** <ol><li><code>length</code> bytes have been read</li>
    ***     <li>a null (0x00) byte is found (if <code>varLength==true</code>)</li>
    ***     <li>end of data is reached</li></ol>
    *** @param length    The maximum length to read
    *** @param varLength If the string can be variable in length (stop on a null)
    *** @param frame     The current stackframe index
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The read String
    **/
    protected String _readString(int length, boolean varLength, int frame, String msg)
    {
        // Read until (whichever comes first):
        //  1) length bytes have been read
        //  2) a null (0x00) byte is found (if 'varLength==true')
        //  3) end of data is reached
        int maxLen = this.getMaximumReadLength(length);
        if (!varLength) {
            Payload.checkOverflow(length, maxLen, frame + 1);
        }
        if (maxLen <= 0) {
            // no room left
            return "";
        } else {
            byte b[] = this._payloadBytes();
            if (b == null) {
                return "";
            }
            // find end of string
            // (look for the end-of-data, or a terminating null (0x00))
            int st = 0;
            for (st = 0; (st < maxLen) && ((this.index + st) < this.size) && (b[this.index + st] != 0); st++);
            String str = (st > 0)? StringTools.toStringValue(b, this.index, st) : "";
            if (DebugLogging) { this.printDebug(frame + 1, (varLength?(st+1):maxLen), str, msg); }
            // move index
            if (varLength) {
                this.index += st;
                if (st < maxLen) { this.index++; } // skip past null terminator
            } else {
                this.index += ((this.index + maxLen) < this.size)? maxLen : (this.size - this.index);
            }
            // return String
            return str;
        }
    }
    
    /**
    *** Read a string from the payload.
    *** The string is read until (whichever comes first):
    *** <ol><li><code>length</code> bytes have been read</li>
    ***     <li>a null (0x00) byte is found (if <code>varLength==true</code>)</li>
    ***     <li>end of data is reached</li></ol>
    *** @param length    The maximum length to read
    *** @param varLength If the string can be variable in length (stop on a null)
    *** @return The read String
    **/
    public String readString(int length, boolean varLength)
    {
        return this._readString(length, varLength, 1/*frame*/, null/*msg*/);
    }
    
    /**
    *** Read a string from the payload.
    *** The string is read until (whichever comes first):
    *** <ol><li><code>length</code> bytes have been read</li>
    ***     <li>a null (0x00) byte is found (if <code>varLength==true</code>)</li>
    ***     <li>end of data is reached</li></ol>
    *** @param length    The maximum length to read
    *** @param varLength If the string can be variable in length (stop on a null)
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The read String
    **/
    public String readString(int length, boolean varLength, String msg)
    {
        return this._readString(length, varLength, 1/*frame*/, msg);
    }

    /**
    *** Reads a variable length string from the payload
    *** @param length The maximum length of the string to read
    *** @return The read String
    *** @see #readString(int length, boolean varLength)
    **/
    public String readString(int length)
    {
        return this._readString(length, true, 1/*frame*/, null/*msg*/);
    }

    /**
    *** Reads a variable length string from the payload
    *** @param length    The maximum length of the string to read
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The read String
    *** @see #readString(int length, boolean varLength)
    **/
    public String readString(int length, String msg)
    {
        return this._readString(length, true, 1/*frame*/, msg);
    }

    // ------------------------------------------------------------------------

    /**
    *** Reads a fixed length hex string from the payload bytes.  The bytes
    *** read will be returned as a hex string, so the length of the returned
    *** String will be twice that of the number of bytes specified.
    *** @param length    The maximum number of bytes to read into a hex string
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The read hex String
    **/
    protected String _readStringHex(int length, int frame, String msg)
    {
        int maxLen = this.getMaximumReadLength(length);
        if (maxLen <= 0) {
            // no room left
            return "";
        } else {
            byte b[] = this._payloadBytes();
            if (b == null) {
                return "";
            }
            String str = StringTools.toHexString(b, this.index, maxLen);
            if (DebugLogging) { this.printDebug(frame + 1, maxLen, str, msg); }
            this.index += maxLen;
            return str;
        }
    }

    /**
    *** Reads a fixed length hex string from the payload bytes.  The bytes
    *** read will be returned as a hex string, so the length of the returned
    *** String will be twice that of the number of bytes specified.
    *** @param length    The maximum number of bytes to read into a hex string
    *** @return The read hex String
    **/
    public String readStringHex(int length)
    {
        return this._readStringHex(length, 1/*frame*/, null);
    }

    /**
    *** Reads a fixed length hex string from the payload bytes.  The bytes
    *** read will be returned as a hex string, so the length of the returned
    *** String will be twice that of the number of bytes specified.
    *** @param length    The maximum number of bytes to read into a hex string
    *** @param msg       Debug message (used during "DebugLogging")
    *** @return The read hex String
    **/
    public String readStringHex(int length, String msg)
    {
        return this._readStringHex(length, 1/*frame*/, msg);
    }

    // ------------------------------------------------------------------------

    /**
    *** Reads an encoded GPS point (latitude,longitude) from the payload
    *** @param length The number of bytes to decode the GeoPoint from
    *** @return The decoded GeoPoint
    *** @see GeoPoint#decodeGeoPoint
    *** @see GeoPoint#encodeGeoPoint
    **/
    public GeoPoint readGPS(int length)
    {
        return this.readGPS(length, null/*msg*/);
    }
    
    /**
    *** Reads an encoded GPS point (latitude,longitude) from the payload
    *** @param length The number of bytes to decode the GeoPoint from
    *** @param msg    Debug message (used during "DebugLogging")
    *** @return The decoded GeoPoint
    *** @see GeoPoint#decodeGeoPoint
    *** @see GeoPoint#encodeGeoPoint
    **/
    public GeoPoint readGPS(int length, String msg)
    {
        int maxLen = this.getMaximumReadLength(length);
        Payload.checkOverflow(length, maxLen, 1);
        if (maxLen < 6) {
            // not enough bytes to decode GeoPoint
            GeoPoint gp = new GeoPoint();
            if (maxLen > 0) { this.index += maxLen; }
            return gp;
        } else
        if (maxLen < 8) {
            // 6 <= len < 8
            byte b[] = this._payloadBytes();
            GeoPoint gp = GeoPoint.decodeGeoPoint(b, this.index, maxLen);
            if (DebugLogging) { this.printDebug(1, maxLen, gp, msg); }
            this.index += maxLen; // 6
            return gp;
        } else {
            // 8 <= len
            byte b[] = this._payloadBytes();
            GeoPoint gp = GeoPoint.decodeGeoPoint(b, this.index, maxLen);
            if (DebugLogging) { this.printDebug(1, maxLen, gp, msg); }
            this.index += maxLen; // 8
            return gp;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Encodes a <code>long</code> value into bytes
    *** @param data The byte array to encode the value to
    *** @param ofs The offset into <code>data</code> to start encoding to
    *** @param len The number of bytes to encode the value to
    *** @param bigEndian True if the bytes are to be big-endian ordered, false if little-endian
    *** @param val The value to encode
    *** @return The number of bytes writen to <code>data</data>. 0 or 
    ***         <code>len</code>
    **/
    public static int encodeLong(byte data[], int ofs, int len, boolean bigEndian, long val)
    {
        if ((data != null) && (data.length >= (ofs + len))) {
            long n = val;
            if (bigEndian) {
                // Big-Endian order
                for (int i = (ofs + len - 1); i >= ofs; i--) {
                    data[i] = (byte)(n & 0xFF);
                    n >>>= 8;
                }
            } else {
                // Little-Endian order
                for (int i = ofs; i < ofs + len; i++) {
                    data[i] = (byte)(n & 0xFF);
                    n >>>= 8;
                }
            }
            return len;
        } else {
            return 0;
        }
    }

    /**
    *** Write a <code>long</code> value to the payload
    *** @param val The value to write
    *** @param wrtLen The number of bytes to write the value into
    *** @param bigEndian True if the bytes are to be big-endian ordered, false if little-endian
    *** @return The number of bytes written
    **/
    protected int _writeLong(long val, int wrtLen, boolean bigEndian, int frame)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* write float/double */
        int maxLen = this.getMaximumWriteLength(wrtLen);
        Payload.checkOverflow(wrtLen, maxLen, frame + 1);
        if (maxLen < wrtLen) {
            // not enough bytes to encode long
            return 0;
        }

        /* write long */
        byte b[] = this._payloadBytes();
        Payload.encodeLong(b, this.index, maxLen, bigEndian, val);
        this.index += maxLen;
        if (this.size < this.index) { this.size = this.index; }
        return maxLen;
        
    }

    /**
    *** Write a <code>long</code> value to the payload
    *** @param val The value to write
    *** @param wrtLen The number of bytes to write the value into
    *** @param bigEndian True if the bytes are to be big-endian ordered, false if little-endian
    *** @return The number of bytes written
    **/
    public int writeLong(long val, int wrtLen, boolean bigEndian)
    {
        return this._writeLong(val, wrtLen, bigEndian, 1);
    }

    /**
    *** Write a <code>long</code> value to the payload
    *** @param val The value to write
    *** @param wrtLen The number of bytes to write the value into
    *** @return The number of bytes written
    **/
    public int writeLong(long val, int wrtLen)
    {
        return this._writeLong(val, wrtLen, this.bigEndian, 1);
    }
    
    /**
    *** Write a unsigned <code>long</code> value to the payload.
    *** @param val       The value to write
    *** @param wrtLen    The number of bytes to write the value into
    *** @param bigEndian True if the bytes are to be big-endian ordered, false if little-endian
    *** @return The number of bytes written
    *** @see #writeLong
    **/
    public int writeULong(long val, int wrtLen, boolean bigEndian)
    {
        return this._writeLong(val, wrtLen, bigEndian, 1);
    }

    /**
    *** Write a unsigned <code>long</code> value to the payload.
    *** @param val    The value to write
    *** @param wrtLen The number of bytes to write the value into
    *** @return The number of bytes written
    *** @see #writeLong
    **/
    public int writeULong(long val, int wrtLen)
    {
        return this._writeLong(val, wrtLen, this.bigEndian, 1);
    }

    /**
    *** Write a <code>int</code> value to the payload
    *** @param val The value to write
    *** @param wrtLen The number of bytes to write the value into
    *** @return The number of bytes written
    **/
    public int writeInt(int val, int wrtLen)
    {
        return this._writeLong((long)val, wrtLen, this.bigEndian, 1);
    }

    /**
    *** Write a unsigned <code>int</code> value to the payload.
    *** @param val The value to write
    *** @param length The number of bytes to write the value into
    *** @return The number of bytes written
    *** @see #writeInt
    **/
    public int writeUInt(int val, int length)
    {
        return this._writeLong((long)val & 0xFFFFFFFFL, length, this.bigEndian, 1);
    }

    // ------------------------------------------------------------------------

    /**
    *** Encodes a <code>double</code> value into bytes
    *** @param data The byte array to encode the value to
    *** @param ofs The offset into <code>data</code> to start encoding to
    *** @param len The number of bytes to encode the value to
    *** @param bigEndian True if the bytes are to be big-endian ordered, 
    ***        false if little-endian
    *** @param val The value to encode
    *** @return The number of bytes writen to <code>data</data>. 0 or 
    ***         <code>len</code>
    **/
    public static int encodeDouble(byte data[], int ofs, int len, boolean bigEndian, double val)
    {
        // 'len' must be at least 4
        if ((data != null) && (len >= 4) && (data.length >= (ofs + len))) {
            int flen = (len >= 8)? 8 : 4;
            long n = (flen == 8)? Double.doubleToRawLongBits(val) : (long)Float.floatToRawIntBits((float)val);
            if (bigEndian) {
                // Big-Endian order
                for (int i = (ofs + flen - 1); i >= ofs; i--) {
                    data[i] = (byte)(n & 0xFF);
                    n >>>= 8;
                }
            } else {
                // Little-Endian order
                for (int i = ofs; i < ofs + flen; i++) {
                    data[i] = (byte)(n & 0xFF);
                    n >>>= 8;
                }
            }
            return len;
        } else {
            return 0;
        }
    }

    /**
    *** Write a <code>double</code> value to the payload
    *** @param val The value to write
    *** @param wrtLen The number of bytes to write the value into
    *** @param bigEndian True if the bytes are to be big-endian ordered, false if little-endian
    *** @return The number of bytes written
    **/
    protected int _writeDouble(double val, int wrtLen, boolean bigEndian, int frame)
    {
        // 'wrtLen' should be either 4 or 8

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* write float/double */
        int maxLen = this.getMaximumWriteLength(wrtLen);
        Payload.checkOverflow(wrtLen, maxLen, frame + 1);
        if (maxLen < 4) {
            // not enough bytes to encode float/double
            return 0;
        }

        /* write float/double */
        int len;
        byte b[] = this._payloadBytes();
        if (wrtLen < 8) {
            // 4 <= wrtLen < 8  [float]
            len = (b == null)? 4 : Payload.encodeDouble(b, this.index, 4, bigEndian, val);
        } else {
            // 8 <= wrtLen      [double]
            len = (b == null)? 8 : Payload.encodeDouble(b, this.index, 8, bigEndian, val);
        }
        this.index += len; // 4;
        if (this.size < this.index) { this.size = this.index; }
        return len;

    }

    /**
    *** Write a <code>double</code> value to the payload
    *** @param val The value to write
    *** @param wrtLen The number of bytes to write the value into
    *** @param bigEndian True if the bytes are to be big-endian ordered, false if little-endian
    *** @return The number of bytes written
    **/
    public int writeDouble(double val, int wrtLen, boolean bigEndian)
    {
        return this._writeDouble(val, wrtLen, bigEndian, 1);
    }
    
    /**
    *** Write a <code>double</code> value to the payload
    *** @param val The value to write
    *** @param wrtLen The number of bytes to write the value into
    *** @return The number of bytes written
    **/
    public int writeDouble(double val, int wrtLen)
    {
        return this._writeDouble(val, wrtLen, this.bigEndian, 1);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Write a zero fill to the payload
    *** @param wrtLen The number of bytes to write
    *** @param frame  The current stackframe index
    *** @return The number of bytes written
    **/
    protected int _writeZeroFill(int wrtLen, int frame)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* check for available space to write the data */
        int maxLen = this.getMaximumWriteLength(wrtLen);
        Payload.checkOverflow(wrtLen, maxLen, frame + 1);
        if (maxLen <= 0) {
            // no room left
            return 0;
        }

        /* fill field bytes with '0's, and adjust pointers */
        this._zeroBytes(maxLen);

        /* return number of bytes written */
        return maxLen;

    }

    /**
    *** Write a zero fill to the payload
    *** @param wrtLen The number of bytes to write
    *** @return The number of bytes written
    **/
    public int writeZeroFill(int wrtLen)
    {
        return this._writeZeroFill(wrtLen, 1);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Write an array of bytes to the payload
    *** @param n The bytes to write to the payload
    *** @param nOfs The offset into <code>n</code> to start reading from
    *** @param nLen The number of bytes to write from <code>n</code>
    *** @param wrtLen The total number of bytes to write. (remaining bytes
    ***        filled with '0' if greater than <code>nLen</code>
    *** @param frame  The current stackframe index
    *** @return The number of bytes writen
    **/
    protected int _writeBytes(byte n[], int nOfs, int nLen, int wrtLen, int frame)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* adjust nOfs/nLen to fit within the byte array */
        if ((nOfs < 0) || (nLen <= 0) || (n == null)) {
            // invalid offset/length, or byte array
            return this._writeZeroFill(wrtLen, frame + 1);
        } else
        if (nOfs >= n.length) {
            // 'nOfs' is outside the array, nothing to write
            return this._writeZeroFill(wrtLen, frame + 1);
        } else
        if ((nOfs + nLen) > n.length) {
            // 'nLen' would extend beyond the end of the array
            nLen = n.length - nOfs; // nLen will be > 0
        }

        /* check for available space to write the data */
        int maxLen = this.getMaximumWriteLength(wrtLen);
        Payload.checkOverflow(wrtLen, maxLen, frame + 1);
        if (maxLen <= 0) {
            // no room left
            return 0;
        }

        /* write byte field */
        // copy 'm' bytes to buffer at current index
        int m = (nLen < maxLen)? nLen : maxLen;
        this._putBytes(n, nOfs, m);
        if (m < maxLen) {
            this._zeroBytes(maxLen - m);    // fill remaining field bytes with '0's
        }

        /* return number of bytes written */
        return maxLen;

    }

    /**
    *** Write an array of bytes to the payload
    *** @param n The bytes to write to the payload
    *** @param nOfs The offset into <code>n</code> to start reading from
    *** @param nLen The number of bytes to write from <code>n</code>
    *** @param wrtLen The total number of bytes to write. (remaining bytes
    ***        filled with '0' if greater than <code>nLen</code>
    *** @return The number of bytes writen
    **/
    public int writeBytes(byte n[], int nOfs, int nLen, int wrtLen)
    {
        return this._writeBytes(n, nOfs, nLen, wrtLen, 1);
    }
    
    /**
    *** Write an array of bytes to the payload
    *** @param n The bytes to write to the payload
    *** @param wrtLen The total number of bytes to write. (remaining bytes
    ***        filled with '0' if greater than <code>n.length</code>
    *** @return The number of bytes writen
    **/
    public int writeBytes(byte n[], int wrtLen)
    {
        return (n == null)? 
            this._writeZeroFill(wrtLen, 1) :
            this._writeBytes(n, 0, n.length, wrtLen, 1);
    }

    /**
    *** Write an array of bytes to the payload
    *** @param n The bytes to write to the payload
    *** @return The number of bytes writen
    **/
    public int writeBytes(byte n[])
    {
        return (n == null)?
            0 :
            this._writeBytes(n, 0, n.length, n.length, 1);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Write a string to the payload. Writes until either <code>wrtLen</code>
    *** bytes are written or the string terminates
    *** @param s The string to write
    *** @param wrtLen The maximum number of bytes to write
    *** @return The number of bytes written
    **/
    public int writeString(String s, int wrtLen)
    {
        return this.writeString(s, wrtLen, true);
    }
    
    /**
    *** Write a string to the payload. Writes until either <code>wrtLen</code>
    *** bytes are written or the string terminates
    *** @param s The string to write
    *** @param wrtLen The maximum number of bytes to write
    *** @return The number of bytes written
    **/
    public int writeString(String s, int wrtLen, boolean varLength)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* check for available space to write the data */
        int maxLen = this.getMaximumWriteLength(wrtLen);
        Payload.checkOverflow(wrtLen, maxLen, 1);
        if (maxLen <= 0) {
            // no room left
            return 0;
        }

        /* empty string ('maxLen' is at least 1) */
        if ((s == null) || s.equals("")) { // do not use "StringTools.isBlank"
            int m = 0;
            if (varLength) {
                // terminate with single '0'
                m += this._zeroBytes(1);  // string terminator only
            } else {
                // '0'-fill remainder of field
                m += this._zeroBytes(maxLen - m);
            }
            return m;
        }

        /* write string bytes, and adjust pointers */
        byte n[] = StringTools.getBytes(s);
        int m = (n.length < maxLen)? n.length : maxLen;
        this._putBytes(n, 0, m);
        if (m < maxLen) {
            if (varLength) {
                // terminate with single '0'
                m += this._zeroBytes(1);  // string terminator only
            } else {
                // '0'-fill remainder of field
                m += this._zeroBytes(maxLen - m);
            }
        }
        return m;

    }

    // ------------------------------------------------------------------------

    /**
    *** Encode a new GPS point into the payload
    *** @param lat The latitude of the GPS point
    *** @param lon The longitude of the GPS point
    *** @param length The total number of bytes to write. 
    ***        Defaults to a minimum of 6
    *** @return The total number of bytes written
    *** @see GeoPoint#encodeGeoPoint
    *** @see GeoPoint#decodeGeoPoint
    **/
    public int writeGPS(double lat, double lon, int length)
    {
        return this.writeGPS(new GeoPoint(lat,lon), length);
    }
    
    /**
    *** Encode a GPS point into the payload
    *** @param gp The GPS point to encode
    *** @param wrtLen The total number of bytes to write. 
    ***        Defaults to a minimum of 6
    *** @return The total number of bytes written
    *** @see GeoPoint#encodeGeoPoint
    *** @see GeoPoint#decodeGeoPoint
    **/
    public int writeGPS(GeoPoint gp, int wrtLen)
    {

        /* check for nothing to write */
        if (wrtLen <= 0) {
            // nothing to write
            return 0;
        }

        /* check for available space to write the data */
        int maxLen = this.getMaximumWriteLength(wrtLen);
        Payload.checkOverflow(wrtLen, maxLen, 1);
        if (maxLen != wrtLen) {
            // not enough bytes to encode GeoPoint
            return 0;
        }

        /* write GPS point */
        int len;
        if (wrtLen < 8) {
            // 6 <= wrtLen < 8
            len = 6;
        } else {
            // 8 <= wrtLen
            len = 8;
        }
        byte b[] = this._payloadBytes();
        if (b != null) {
            GeoPoint.encodeGeoPoint(gp, b, this.index, len);
        }
        this.index += len;
        if (this.size < this.index) { this.size = this.index; }
        // TODO: zero-fill (wrtLen - len) bytes?
        return len;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a hex string representation of the payload
    *** @return A hex string representation
    **/
    public String toString()
    {
        return StringTools.toHexString(this.payload, 0, this.size);
    }

    // ------------------------------------------------------------------------

    /* debug */
    private void _printDebug(int frame, int maxLen, String valStr, String msg)
    {
        String frameStr = Print._getStackFrame(frame + 1);
        StringBuffer sb = new StringBuffer();
        sb.append("[PayloadDebug:" + frameStr + "] ");
        sb.append("0x");
        sb.append(StringTools.leftAlign(StringTools.toHexString(this._payloadBytes(),this.index,maxLen),16));
        sb.append(" ==> ").append(valStr);
        if (!StringTools.isBlank(msg)) {
            sb.append("  [").append(msg).append("]");
        }
        Print.sysPrintln(sb.toString());
    }

    /* debug */
    private void printDebug(int frame, int maxLen, byte n[], String msg)
    {
        String valStr = "(bytes) 0x" + StringTools.toHexString(n);
        this._printDebug(frame + 1, maxLen, valStr, msg);
    }

    /* debug */
    private void printDebug(int frame, int maxLen, boolean bigEndian, boolean signed, long val, String msg)
    {
        String valStr = (signed?"(long) ":"(ulong) ") + String.valueOf(val);
        this._printDebug(frame + 1, maxLen, valStr, msg);
    }

    /* debug */
    private void printDebug(int frame, int maxLen, boolean bigEndian, double val, String msg)
    {
        String valStr = "(double) " + String.valueOf(val);
        this._printDebug(frame + 1, maxLen, valStr, msg);
    }

    /* debug */
    private void printDebug(int frame, int maxLen, String val, String msg)
    {
        String valStr = "(String) " + ((val != null)? val : "null");
        this._printDebug(frame + 1, maxLen, valStr, msg);
    }

    /* debug */
    private void printDebug(int frame, int maxLen, GeoPoint val, String msg)
    {
        String valStr = "(GeoPoint) " + ((val != null)? ("\""+val.toString()+"\"") : "null");
        this._printDebug(frame + 1, maxLen, valStr, msg);
    }

    /* debug */
    private void printDebug_source(int frame)
    {
        String frameStr = Print._getStackFrame(frame + 1);
        StringBuffer sb = new StringBuffer();
        sb.append("[PayloadDebug:" + frameStr + "] ");
        sb.append("S=0x");
        StringTools.toHexString(this._payloadBytes(),sb);
        Print.sysPrintln(sb.toString());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}

