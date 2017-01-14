/*
 *     The contents of this file are subject to the Netscape Public
 *     License Version 1.1 (the "License"); you may not use this file
 *     except in compliance with the License. You may obtain a copy of
 *     the License at http://www.mozilla.org/NPL/
 *    
 *     Software distributed under the License is distributed on an "AS
 *     IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *     implied. See the License for the specific language governing
 *     rights and limitations under the License.
 *    
 *     The Original Code is the Netscape Messaging Access SDK Version 3.5 code, 
 *    released on or about June 15, 1998.  *    
 *     The Initial Developer of the Original Code is Netscape Communications 
 *     Corporation. Portions created by Netscape are
 *     Copyright (C) 1998 Netscape Communications Corporation. All
 *     Rights Reserved.
 */

/*
 * Copyright (c) 1997 and 1998 Netscape Communications Corporation
 * (http://home.netscape.com/misc/trademarks.html)
 */

package com.fmm.ds.agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.text.TextUtils;

import com.fmm.ds.core.XDSMimeException;

public class XDSQP
{
    // error messages
    public static final String  ERROR_BAD_PARAMETER               = "Error: Bad parameter";
    public static final String  ERROR_OUT_OF_MEMORY               = "Error: Out of memory";
    public static final String  ERROR_EMPTY_MESSAGE               = "Error: Empty message";
    public static final String  ERROR_BAD_MIME_MESSAGE            = "Error: Bad mime message";
    public static final String  ERROR_BAD_EXTERNAL_MESSAGE_PART   = "Error:  No External headers in Message/external-body";
    public static final String  ERROR_UNSUPPORTED_PARTIAL_SUBTYPE = "Error: Unsupported Partial SubType";
    public static final String  INVALID_CODE                      = "is an invalid code";

    private static final byte   CR                                = '\r';
    private static final byte   LF                                = '\n';
    private static final byte   EQ                                = '=';
    private static final byte   HT                                = '\t';
    @SuppressWarnings("unused")
    private static final byte[] CRLF                              = "\r\n".getBytes();
    private static final byte[] EQCRLF                            = "=\r\n".getBytes();

    private static final byte   hexmap[]                          = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * QuotedPrintable Encodes data from InputStream and writes to OutputStream.
     * 
     * @param input
     *            : InputStream that supplies the data to be encoded.
     * @param output
     *            : OutputStream that accepts the encoded data.
     * @return Number of bytes written.
     * @exception MimeException
     *                : If an encoding error occurs.
     * @exception IOException
     *                : If an I/O error occurs.
     */
    public static long encodeQP(InputStream input, ByteArrayOutputStream output) throws XDSMimeException, IOException
    {
        byte current = (byte) 0, previous = (byte) 0;
        int read, linelen = 0, written = 0, lastspace = 0, nullCount = 0;
        byte l_bufenc[] = new byte[80];

        while (true)
        {
            read = input.read();

            if (read == -1)
            {
                if (linelen > 0)
                {
                    output.write(l_bufenc, 0, linelen);
                    // output.write (CRLF);
                    // written += 2;
                }

                return (written);
            }

            if (linelen > 74)
            {
                output.write(l_bufenc, 0, linelen);
                output.write(EQCRLF);
                written += 3;
                linelen = 0;
                previous = (byte) 0;
            }
            current = (byte) read;

            if (current == 0x00)
            {
                nullCount++;
                previous = current;
                lastspace = 0;
                continue;
            }
            else if (nullCount > 0)
            {
                // write out all the nulls first and fall through to process current char.
                for (int idx = 1; idx <= nullCount; idx++)
                {
                    byte tmp = 0x00;
                    l_bufenc[linelen++] = EQ;
                    l_bufenc[linelen++] = (byte) hexmap[(tmp >>> 4) & 0xF];
                    l_bufenc[linelen++] = (byte) hexmap[(tmp & 0xF)];
                    // l_bufenc [linelen++] = (byte)0x00;
                    // l_bufenc [linelen++] = (byte)0x00;
                    written += 3;

                    if (linelen > 74)
                    {
                        output.write(l_bufenc, 0, linelen);
                        output.write(EQCRLF);
                        written += 3;
                        linelen = 0;
                    }
                }

                previous = (byte) 0;
                nullCount = 0;
            }

            if ((current > ' ') && (current < 0x7F) && (current != '=') && (current != '(') && (current != ')') && (current != ';') && (current != ':'))
            {
                // Printable chars
                // output.write ((byte) current);
                l_bufenc[linelen++] = (byte) current;
                // linelen += 1;
                written += 1;
                lastspace = 0;
                previous = current;
            }
            else if ((current == ' ') || (current == HT))
            {
                // output.write ((byte) current);
                l_bufenc[linelen++] = (byte) current;
                written += 1;
                lastspace = 1;
                previous = current;
            }
            else if ((current == LF) && (previous == CR))
            {
                // handled this already. Ignore.
                previous = (byte) 0;
            }
            else if (current == LF)
            {
                if ((lastspace == 1) || ((previous == '.') && (linelen == 1)))
                {
                    l_bufenc[linelen++] = EQ;
                    l_bufenc[linelen++] = CR;
                    l_bufenc[linelen++] = LF;
                    written += 3;
                }

                l_bufenc[linelen++] = EQ;
                l_bufenc[linelen++] = '0';
                l_bufenc[linelen++] = 'A';
                lastspace = 0;
                written += 3;

                output.write(l_bufenc, 0, linelen);
                previous = (byte) 0;
                linelen = 0;
            }
            else if ((current == CR))// || (current == LF))
            {
                // Need to emit a soft line break if last char was SPACE/HT or
                // if we have a period on a line by itself.
                if ((lastspace == 1) || ((previous == '.') && (linelen == 1)))
                {
                    l_bufenc[linelen++] = EQ;
                    l_bufenc[linelen++] = CR;
                    l_bufenc[linelen++] = LF;
                    written += 3;
                }

                l_bufenc[linelen++] = CR;
                l_bufenc[linelen++] = LF;
                lastspace = 0;
                written += 2;
                output.write(l_bufenc, 0, linelen);
                previous = (byte) 0;
                linelen = 0;
                // output.write (CRLF);
                // previous = current;
            }
            else if ((current < ' ') || (current == '=') || (current >= 0x7F) || (current == '(') || (current == ')') || (current == ';') || (current == ':'))
            {
                // Special Chars
                // output.write ((byte) '=');
                // output.write ((byte) hexmap [(current >>> 4)]);
                // output.write ((byte) hexmap [(current & 0xF)]);
                l_bufenc[linelen++] = EQ;

                l_bufenc[linelen++] = (byte) hexmap[(current >>> 4) & 0xF];
                l_bufenc[linelen++] = (byte) hexmap[(current & 0xF)];
                lastspace = 0;
                // linelen += 3;
                written += 3;
                previous = current;
            }
            else
            {
                // output.write ((byte) current);
                l_bufenc[linelen++] = (byte) current;
                lastspace = 0;
                // linelen += 1;
                written += 1;
                previous = current;
            }
        } // while

    } // encodeQP

    public static String encodeQP(String in) throws XDSMimeException
    {
        ByteArrayInputStream input = new ByteArrayInputStream(in.getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try
        {
            encodeQP(input, output);

        }
        catch (IOException e)
        {
            XDSDebug.XDS_DEBUG_EXCEPTION(e.toString());
        }
        String ret = output.toString();
        return ret;
    }

    /**
     * QuotedPrintable Decodes data from InputStream and writes to OutputStream
     * 
     * @param input
     *            : InputStream that supplies the data to be decoded.
     * @param output
     *            : OutputStream that accepts the decoded data.
     * @exception XDSMimeException
     *                : if a decoding error occurs.
     * @exception IOException
     *                : if io error occurs.
     */
    public static void decodeQP(InputStream input, OutputStream output) throws XDSMimeException
    {
        byte inputBuffer[];

        try
        {
            inputBuffer = new byte[input.available() + 1];
            input.read(inputBuffer);
            output.write(decodeQP(inputBuffer));
        }
        catch (IOException e)
        {
            throw new XDSMimeException(e.getMessage());
        }
        catch (Exception e)
        {
            throw new XDSMimeException(e.getMessage());
        }

        return;
    }

    /**
     * String version. QP decodes data from the input string.
     * 
     * @param str
     *            : the QP-encoded string.
     * @return the decoded string
     */
    public static String decodeQP(String str) throws XDSMimeException
    {
        String decodedString = null;

        if (TextUtils.isEmpty(str))
            return null;

        try
        {
            decodedString = new String(decodeQP(str.getBytes()));
        }
        catch (Exception e)
        {
            throw new XDSMimeException(e.getMessage());
        }

        return decodedString;
    }

    /**
     * byte[] version. QP decodes input bytes.
     * 
     * @param bytesIn
     *            : QP-encoded bytes.
     * @return the decoded bytes
     */

    protected static byte[] decodeQP(byte[] bytesIn) throws XDSMimeException
    {
        return decodeQP(bytesIn, bytesIn.length);

    }

    /**
     * byte[] version. QP decodes input bytes of given length.
     * 
     * @param bytesIn
     *            : QP-encoded bytes.
     * @param len
     *            : length of QP-encoded bytes.
     * @return the decoded bytes
     * @exception ParseException
     *                : If a '=' is not followed by a valid 2-digit hex number or '\r\n'.
     */
    protected static byte[] decodeQP(byte[] bytesIn, int len) throws XDSMimeException
    {
        if (bytesIn == null)
            throw new XDSMimeException(ERROR_BAD_PARAMETER);

        byte res[] = new byte[len + 1];
        byte src[] = bytesIn;
        byte nl[] = System.getProperty("line.separator").getBytes();
        // "\r\n".getBytes(); //System.getProperty("line.separator").getBytes();

        int last = 0, j = 0;

        for (int i = 0; i < len;)
        {
            byte ch = src[i++];

            if (ch == '=')
            {
                if (src[i] == '\n' || src[i] == '\r')
                { // Rule #5
                    i++;

                    if (src[i - 1] == '\r' && src[i] == '\n')
                        i++;
                }
                else
                // Rule #1
                {
                    byte repl;
                    int hi = Character.digit((char) src[i], 16), lo = Character.digit((char) src[i + 1], 16);

                    if ((hi | lo) < 0)
                        throw new XDSMimeException(new String(src, i - 1, 3) + INVALID_CODE);
                    else
                    {
                        repl = (byte) (hi << 4 | lo);
                        i += 2;
                    }

                    res[j++] = repl;

                }

                last = j;
            }
            else if (ch == '\n' || ch == '\r') // Rule #4
            {
                if (src[i - 1] == '\r' && src[i] == '\n')
                    i++;

                for (int idx = 0; idx < nl.length; idx++)
                    res[last++] = nl[idx];

                j = last;
            }
            else
            // Rule #1, #2
            {
                res[j++] = ch;

                if (ch != ' ' && ch != '\t') // Rule #3
                    last = j;
            }
        }

        byte res2[] = new byte[j];

        System.arraycopy(res, 0, res2, 0, j);

        // for(int k=0;k<j;k++)
        // {
        // res2[k]=res[k];
        // }

        return res2;
    }
}