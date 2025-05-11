import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import O2PlibSS.gui.*;

/** 
 * Collection of utility functions used by various other classes. 
 * <PRE>
 * <U><B>List of Methods</B></U>
 * clearDebugNameValuePairs() - clear DEBUG (name,value) list
 * setDebugNameValue() - set DEBUG (name,value) pair.
 * testDebugValue - test if str is indexOf() value if name is debug(name,value).
 * testDebugValue - test DEBUG value is indexOf(str) if name is debug(name,value).
 * logMsg() - print a message
 * setReportTextArea() - assign report text area in popup report GUI window
 * setReportStatusLineTextField() - assign report status line GUI text field
 * setReportStatusLineLabel() - assign report status line label in GUI
 * appendPRmsg() - append message to  report text area in GUI window
 * updateReportStatusLine() - set status line message if text field in GUI
 * dateStr() - return the current date stamp
 * timeStr() - return the current time stamp
 * getCurTimeStr() - return current time string in the format of HHMMSS.
 * mapCRLF2space() - map CRLFs in the string to spaces
 * mapCRLForCR2LF() - map all CRLF or CR(not LF) to LF
 * mapComma2Tab() - map "," to "\t" in the string
 * mapIllegalChars() - map illegal characters to '_'
 * mapSpaceToMinus() - map " " to "-"
 * removeQuotes() - remove '\"' characters from string
 * isEmptyArray() - test if array consists of empty data elements
 * isNumber() - test if a string is a number (i.e., Double)
 * cvf2s() - convert float to string with precision  # of digits
 * cvd2s() - convert double to string with precision  # of digits
 * cvs2d() - convert String to double
 * cvs2f() - convert String to float
 * cvs2i() - convert String to int
 * cvs2long() - convert String to long
 * cvb2s() - convert boolean String
 * cvs2l() - convert String to long
 * cvs2b() - convert String to boolean 
 * cvs2ArrayNullFill() - cvt arg list "1,4,3,6,..."  to "," fill nulls.
 * cvs2Array() - cvt arg list "1,4,3,6,..."  to "," - delim String[]. 
 * rightSizeArray() - extend or shrink an array to requiredSize.
 * cloneString() - clone a copy of a String array
 * lookupDataIdx() - lookup index of element if exists in a strData[].
 * isListInAnotherList() - test if all listA in listB order independent.
 * mapAllkeywords() - map data from 'keyword' to 'toString' lists.
 * rmvRtnChars() - remove return chars. Map '\r' or "\r\n" to '\n' chars. 
 * rmvSpecifiedChar() - remove specified character from string.
 * rmvEnclosingWhitespace() - remove enclosing whitespace from string
 * trimArrayEnclWhitespace() - trim (clean) enclosing white space in 1D array.
 * removeEmptyStringsFromArray() - remove nulls and empty Strings in 1D array.
 * getCleanArgList() - convert string arg list delimited by "", "," or "\n" to array
 * replaceSpaceWithCommaSpaceDelim() - replace " "-delimited list with ", " list 
 * replaceSubstrInString() - recursively replace oldToken w/newToken in str
 * changeWorkingDirectory() - change the working directory
 * cvDeltaTime2str() - convert a delta epoch time msec to a string
 * off_timer() - Compute the time banner String 
 * on_timer() - Get the current time (in milliseconds)
 * sleepMsec() - sleep for mSec.
 * appendElementToArray() - Append String element to String array.  
 * startsWithIgnoreCase() - test if str starts with startStr ignoring case
 * endsWithIgnoreCase() - test if str ends with endsStr ignoring case
 * indexOfIgnoreCase() - indexOf subStr of str ignoring case.
 * cvtHTMLcolorNameToHex() - map HTML color name to HEX value string
 * updateJarFile() - update .jar file from URL into program install area.
 * copyFile() - binary copy of one file or URL to a local file
 * readBytesFromURL() - read binary data from URL into byte[] array.
 * deleteLocalFile() - delete local file.
 * </PRE>
 * <P> 
 * This code is available at the HTMLtools project on SourceForge at
 * <A HREF="http://htmltools.sourceforge.net/">http://htmltools.sourceforge.net/</A>
 * under the "Common Public License Version 1.0" 
 * <A HREF="http://www.opensource.org/licenses/cpl1.0.php">
 * http://www.opensource.org/licenses/cpl1.0.php</A>.<P></P>
 * <P>
 * It was derived and refactored from the open source
 * MAExplorer (http://maexplorer.sourceforge.net/), and
 * Open2Dprot (http://Open2Dprot.sourceforge.net/) Table modules.
 * <P>
 * $Date: 2009/11/26 11:45:56 $ $Revision: 1.38 $
 * <BR> 
 * Copyright 2008, 2009 by Peter Lemkin 
 * E-Mail: lemkin@users.sourceforge.net 
 * <A HREF="http://lemkingroup.com/">http://lemkingroup.com/</A>
 */
public class UtilCM {

    /** Default value of int for cvs2i() calls */
    public static int defaultInt = 0;

    /** Default value of float for cvs2f() calls */
    public static float defaultFloat = 0.0F;

    /** Local logging flag. If set, then logMsg() output is also
   * printed to System.out */
    public static boolean loggingFlag;

    /** logging String if loggingFlag enabled */
    public static volatile String loggingStr;

    /** Report text area will output appendPRmsg if not null.
   * This is set with setReportTextArea().
   */
    public static volatile JTextArea rptTextArea = null;

    /** Optional Big Report PopupTextViewer text area will output appendPRmsg 
   * if not null. This is set with setBigReportTextViewer().
   */
    public static volatile PopupTextViewer ptViewer = null;

    /** Report text field will output updateReportStatusLine if not null.
   * This is set with setReportStatusLineTextField().
   */
    public static volatile JTextField rptStatusLineTextField = null;

    /** Report Label will output updateReportStatusLine if not null.
   * This is set with setReportStatusLineLabel().
   */
    public static volatile JLabel rptStatusLineLabel = null;

    /** Maximum number of debug(name,value) pairs */
    public static final int MAX_DEBUGS = 10;

    /** Current number of debug(name,value) pairs */
    public static int nDebugPairs = 0;

    /** Name list for Debug facility of (name,value) pairs */
    public static String debugName[] = null;

    /** Value list for Debug facility of (name,value) pairs */
    public static String debugValue[] = null;

    /**
   * UtilCM() - constructor for setup utility pkg for additional
   * UtilCM instances. NOTE: you should call the logging version
   * of the Constructor first or make a call to setLogMsgs that
   * otherwise.
   * @param loggingFlag
   */
    public UtilCM() {
        if (debugName == null) clearDebugNameValuePairs();
    }

    /**
   * UtilCM() - constructor for setup utility pkg
   * @param loggingFlag
   */
    public UtilCM(boolean loggingFlag) {
        UtilCM.loggingFlag = loggingFlag;
        UtilCM.loggingStr = "";
        if (debugName == null) clearDebugNameValuePairs();
    }

    /**
   * clearDebugNameValuePairs() - clear DEBUG (name,value) list
   */
    public static void clearDebugNameValuePairs() {
        debugName = new String[MAX_DEBUGS];
        debugValue = new String[MAX_DEBUGS];
        nDebugPairs = 0;
    }

    /**
   * setDebugNameValue() - set DEBUG (name,value) pair.
   * @param name of the debug pair.
   * @param value to associate with debug name pair.
   * @return true if successful
   * @see #getDebugValue
   */
    public static boolean setDebugNameValue(String name, String value) {
        int n = -1;
        for (int i = 0; i < nDebugPairs; i++) if (name.equals(debugName[i])) {
            n = i;
            break;
        }
        if (n == -1) {
            if (nDebugPairs < MAX_DEBUGS) {
                n = nDebugPairs;
                nDebugPairs++;
            } else return (false);
        }
        debugName[n] = name;
        debugValue[n] = value;
        return (true);
    }

    /**
   * getDebugValue - get value if name is a debug(name,value), else "".
   * or return false it it is not in the debugName[] list.
   * @param name - to test
   * @return return debug-pair value, else "" if not in debugName[] list.
   * @see #setDebugNameValue
   */
    public static String getDebugValue(String name) {
        for (int i = 0; i < nDebugPairs; i++) if (name.equals(debugName[i])) return (debugValue[i]);
        return ("");
    }

    /**
   * testDebugValue - test if testValue is indexOf() value if name is debug(name,value).
   * @param dbugName - debug name in Debug(name,value) DB to test
   * @param testValue - to test if testValue is indexOf() the corresponding 
   *                 dbugName in the Debug (name,value) list.
   * @return return true, if value in debug-pair value, else false
   *                 if not in debugName[] list.
   * @see #setDebugNameValue
   */
    public static boolean testDebugValue(String dbugName, String testValue) {
        for (int i = 0; i < nDebugPairs; i++) if (dbugName.equals(debugName[i])) {
            String dbugVal = debugValue[i];
            boolean flag = (testValue.indexOf(dbugVal) != -1);
            return (flag);
        }
        return (false);
    }

    /**
   * setLogMsgs() - enable/disable logging
   * @param loggingFlag
   */
    public static void setLogMsgs(boolean loggingFlag) {
        UtilCM.loggingFlag = loggingFlag;
        UtilCM.loggingStr = "";
    }

    /**
   * getLogMsgs() - return the logging string
   * @return loggingStr
   */
    public static String getLogMsgs() {
        return (UtilCM.loggingStr);
    }

    /**
   * logMsg() - log the message.
   * @param msg
   */
    public static void logMsg(String msg) {
        if (msg == null) return;
        System.out.print(msg);
        if (loggingFlag) loggingStr += msg;
        if (rptTextArea != null) appendPRmsg(msg);
    }

    /**
   * setReportTextArea() - assign report text area in popup report GUI window
   * @param rptTxtArea is text area to write appendPRmsg() calls
   *          if this is null, then it will not append output to this
   *          text area but rather send it to stdout.
   */
    public static void setReportTextArea(JTextArea rptTxtArea) {
        rptTextArea = rptTxtArea;
    }

    /**
   * setBigReportTextViewer() - assign Big report text viewer GUI window
   * @param pTxtViewer is PopupTextViewer that contains a text area to write 
   *          appendPRmsg() calls if this is null, then it will not append 
   *          output to it's text area.
   */
    public static void setBigReportTextViewer(PopupTextViewer pTxtViewer) {
        ptViewer = pTxtViewer;
    }

    /**
   * setReportStatusLineTextField() - assign report status line GUI text field
   * in popup report window.
   * @param rptStatusLineTxtField is text field to write 
   *       updateReportStatusLine() calls if this is null, then it will 
   *       not output this text field but rather send it to stdout.
   */
    public static void setReportStatusLineTextField(JTextField rptStatusLineTxtField) {
        rptStatusLineTextField = rptStatusLineTxtField;
    }

    /**
   * setReportStatusLineLabel() - assign report status line label in GUI
   * in popup report window.
   * @param rptStatusLineLabel is label to write 
   *       updateReportStatusLine() calls if this is null, then it will 
   *       not output this text field but rather send it to stdout.
   */
    public static void setReportStatusLineLabel(JLabel rptStatusLineLbl) {
        rptStatusLineLabel = rptStatusLineLbl;
    }

    /**
   * appendPRmsg() - append message to  report text area in GUI window 
   * if it exists. Otherwise it send it to stdout. If the
   * dbugConsoleFlag is set, it also sends it to stdout.
   * Scroll to the bottom of the window each time append data.
   * @param msg to display
   */
    public static void appendPRmsg(String msg) {
        if (rptTextArea == null || msg == null) return;
        String rptStr = "";
        int rptStrLth = 0;
        if (rptTextArea != null) {
            rptTextArea.append(msg);
            rptStr = rptTextArea.getText();
            rptStrLth = rptStr.length();
            rptTextArea.setCaretPosition(rptStrLth);
        }
        if (ptViewer != null) {
            ptViewer.appendMsg(msg);
        }
        if (rptTextArea == null) logMsg(msg);
    }

    /**
   * updateReportStatusLine() - set status line message if text field in GUI
   * and/or Label was assigned.
   * @param msg to display
   * @see #setReportStatusLineTextField
   * @see #setReportStatusLineLabel
   */
    public static void updateReportStatusLine(String msg) {
        if (rptStatusLineTextField != null && msg != null) rptStatusLineTextField.setText(msg);
        if (rptStatusLineLabel != null && msg != null) rptStatusLineLabel.setText(msg);
    }

    /**
   * dateStr() - return a new Date string of the current day and time
   * @return date string
   */
    public static String dateStr() {
        Date dateObj = new Date();
        String date = dateObj.toString();
        return (date);
    }

    /**
   * timeStr() - return a new daytime HH:MM:SS string of the current time
   * @return time of day string
   */
    public static String timeStr() {
        Calendar cal = Calendar.getInstance();
        int hrs = cal.get(Calendar.HOUR_OF_DAY), mins = cal.get(Calendar.MINUTE), secs = cal.get(Calendar.SECOND);
        String dayTime = hrs + ":" + mins + ":" + secs;
        return (dayTime);
    }

    /**
   * getCurTimeStr() - return current time string in the format of HHMMSS.
   * This string is sortable.
   * Note: Adds "0" to first single digit if value is 1-9.
   */
    public static String getCurTimeStr() {
        GregorianCalendar cal = new GregorianCalendar();
        int hh = cal.get(Calendar.HOUR_OF_DAY), min = cal.get(Calendar.MINUTE), ss = cal.get(Calendar.SECOND);
        String sH, sMin, sS;
        Integer i;
        if (hh < 10) {
            i = new Integer(hh);
            sH = "0" + i.toString();
        } else {
            i = new Integer(hh);
            sH = i.toString();
        }
        if (min < 10) {
            i = new Integer(min);
            sMin = "0" + i.toString();
        } else {
            i = new Integer(min);
            sMin = i.toString();
        }
        if (ss < 10) {
            i = new Integer(ss);
            sS = "0" + i.toString();
        } else {
            i = new Integer(ss);
            sS = i.toString();
        }
        String date = sH + ":" + sMin + ":" + sS;
        return (date);
    }

    /**
   * mapCRLF2space() - map "\r\n" or "\n\r" or "\r" or '\n' to
   * a single " " in the string.
   * @param s input string to map
   * @return mapped string
   */
    public static String mapCRLF2space(String s) {
        if (s == null) return (null);
        String sR = "";
        char ch, ch2 = 0, sBuf[] = s.toCharArray();
        int sSize = s.length(), sCtr = 0;
        while (sCtr < sSize) {
            ch = sBuf[sCtr++];
            if (sCtr < sSize) ch2 = sBuf[sCtr]; else ch2 = 0;
            if ((ch == '\r' && ch2 == '\n') || (ch2 == '\r' && ch == '\n')) {
                ch = ' ';
                sCtr++;
            } else if (ch == '\r' || ch == '\n') ch = ' ';
            sR += ("" + ch);
        }
        return (sR);
    }

    /**
   * mapCRLForCR2LF() - map all CRLF or CR<not LF> to LF   
   * @param rawData raw data representing string table to analyze
   * @return mapped string
   */
    public static String mapCRLForCR2LF(String rawData) {
        if (rawData == null || rawData.length() == 0) return (rawData);
        char ch = 0, chP1 = 0, inputBuf[] = rawData.toCharArray();
        int xStart = 0, xOut = 0, bufSize = inputBuf.length;
        char outBuf[] = new char[bufSize];
        while (xStart < bufSize) {
            ch = inputBuf[xStart];
            if (ch == '\r' && ((xStart - 1) < bufSize)) {
                chP1 = inputBuf[xStart + 1];
                if (chP1 == '\n') {
                    xStart++;
                }
                xStart++;
                outBuf[xOut++] = '\n';
            } else {
                xStart++;
                outBuf[xOut++] = ch;
            }
        }
        String sOut = new String(outBuf, 0, xOut);
        return (sOut);
    }

    /**
   * mapComma2Tab() - map "," to "\t" in the string
   * @param s input string to map
   * @return mapped string
   */
    public static String mapComma2Tab(String s) {
        if (s == null) return (null);
        String sR = "";
        char ch, sBuf[] = s.toCharArray();
        int sSize = s.length(), sCtr = 0;
        while (sCtr < sSize) {
            ch = sBuf[sCtr++];
            if (ch == ',') ch = '\t';
            sR += ("" + ch);
        }
        return (sR);
    }

    /**
   * mapIllegalChars() - map illegal characters to '_'
   * i.e. ' ', '\t', ':', ';', '\"', ',', '\'' '@', '*', '=' to '_'
   * @param str to map
   * @return String mapped
   */
    static String mapIllegalChars(String str) {
        String sR = str;
        char ch, cBuf[] = new char[str.length()];
        for (int i = str.length() - 1; i >= 0; i--) {
            ch = str.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == ':' || ch == ';' || ch == '\"' || ch == ',' || ch == '@' || ch == '*' || ch == '=' || ch == '\'') ch = '_';
            cBuf[i] = ch;
        }
        sR = new String(cBuf);
        return (sR);
    }

    /**
   * mapSpaceToMinus() - map " " to "-"
   * @param str to map
   * @return mapped string
   */
    public static String mapSpaceToMinus(String str) {
        if (str == null) return (null);
        String sR = "";
        char ch, sBuf[] = str.toCharArray();
        int sSize = str.length(), sCtr = 0;
        while (sCtr < sSize) {
            ch = sBuf[sCtr++];
            if (ch == ' ') ch = '-';
            sR += ("" + ch);
        }
        return (sR);
    }

    /**
   * removeQuotes() - remove '\"' characters from string
   * @param line to check and edit out quotes
   */
    public static String removeQuotes(String line) {
        if (line.indexOf('\"') == -1) return (line);
        int sSize = line.length(), sCtr = 0, oCnt = 0;
        char ch, sInBuf[] = line.toCharArray(), sOutBuf[] = new char[sSize];
        while (sCtr < sSize) {
            ch = sInBuf[sCtr++];
            if (ch != '\"') sOutBuf[oCnt++] = ch;
        }
        line = new String(sOutBuf, 0, oCnt);
        return (line);
    }

    /**
   * isEmptyArray() - test if array consists of empty data elements
   * @param sList is array of data
   * @return true if the sList is empty or null, false if at least 
   *              1 non-empty element
   */
    public static boolean isEmptyArray(String sList[]) {
        if (sList == null) return (true);
        int lth = sList.length;
        for (int i = 0; i < lth; i++) if (sList[i] != null && sList[i].length() > 0) return (false);
        return (true);
    }

    /**
   * isNumber() - test if a string is a number (i.e., Double)
   * @param str is the string to test if it is a number
   * @return value of string
   */
    public static boolean isNumber(String str) {
        double d;
        try {
            Double D = new Double(str);
            d = D.doubleValue();
        } catch (NumberFormatException e) {
            return (false);
        }
        return (true);
    }

    /**
   * cvf2s() - convert float to string with precision  # of digits
   * If precision > 0 then limit # of digits in fraction
   * @param v is the value to convert
   * @param precision is the # of digits to display
   * @return string approximating "%0.pd"
   */
    public static String cvf2s(float v, int precision) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(precision);
        nf.setGroupingUsed(false);
        String s = nf.format(v);
        return (s);
    }

    /**
   * cvd2s() - convert double to string with precision  # of digits
   * If precision > 0 then limit # of digits in fraction
   * @param v is the value to convert
   * @param precision is the # of digits to display
   * @return string approximating "%0.pd"
   */
    public static String cvd2s(double v, int precision) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(precision);
        nf.setGroupingUsed(false);
        String s = nf.format(v);
        return (s);
    }

    /**
   * cvs2d() - convert String to double
   * @param str is the string to convert to a number
   * @return value of string
   */
    public static double cvs2d(String str) {
        double d;
        try {
            Double D = new Double(str);
            d = D.doubleValue();
        } catch (NumberFormatException e) {
            d = (double) defaultFloat;
        }
        return (d);
    }

    /**
   * cvs2f() - convert String to float
   * @param str is the string to convert to a number
   * @return value of string
   */
    public static float cvs2f(String str) {
        float f;
        try {
            Float F = new Float(str);
            f = F.floatValue();
        } catch (NumberFormatException e) {
            f = defaultFloat;
        }
        return (f);
    }

    /**
   * cvs2f() - convert String to float
   * @param str is the string to convert to a number
   * @param defVal is the explicit default value
   * @return value of string
   */
    public static float cvs2f(String str, float defVal) {
        float f;
        try {
            Float F = new Float(str);
            f = F.floatValue();
        } catch (NumberFormatException e) {
            f = defVal;
        }
        return (f);
    }

    /**
   * cvs2i() - convert String to int
   * @param str is the string to convert to a number
   * @return value of string
   */
    public static int cvs2i(String str) {
        int i;
        try {
            i = java.lang.Integer.parseInt(str);
        } catch (NumberFormatException e) {
            i = defaultInt;
        }
        return (i);
    }

    /**
   * cvs2i() - convert String to int
   * @param str is the string to convert to a number
   * @param defVal is the explicit default value
   * @return value of string
   */
    public static int cvs2i(String str, int defVal) {
        int i;
        try {
            i = java.lang.Integer.parseInt(str);
        } catch (NumberFormatException e) {
            i = defVal;
        }
        return (i);
    }

    /**
   * cvs2long() - convert String to long
   * @param str is the string to convert to a long number
   * @return value of string
   */
    public static long cvs2long(String str) {
        long i;
        try {
            i = java.lang.Long.parseLong(str);
        } catch (NumberFormatException e) {
            i = defaultInt;
        }
        return (i);
    }

    /**
   * cvb2s() - convert boolean String
   * @param b is boolean input to convert
   * @return output string as either "true" or "false"
   */
    public static String cvb2s(boolean b) {
        if (b) return ("true"); else return ("false");
    }

    /**
   * cvs2l() - convert String to long
   * @param str containing integer to convert to long
   * @return converted number else defaultLong if illegal number.
   */
    public static long cvs2l(String str, long defaultLong) {
        if (str == null) return (defaultLong);
        long lV = defaultLong;
        if (str != null) {
            try {
                Long L = new Long(str);
                lV = L.longValue();
            } catch (NumberFormatException e) {
                lV = defaultLong;
            }
            return (lV);
        }
        return (lV);
    }

    /**
   * cvs2b() - convert String to boolean
   * @param str containing either "true" or "false"
   * @return converted boolean else false if illegal boolean name.
   */
    public static boolean cvs2b(String str, boolean defaultBool) {
        if (str == null) return (defaultBool);
        boolean b = defaultBool;
        if (str.equals("TRUE") || str.equals("true") || str.equals("T") || str.equals("t") || str.equals("1")) b = true;
        return (b);
    }

    /**
   * cvs2ArrayNullFill() - cvt arg list "1,4,3,6,..."  to "," fill nulls.
   * Also, if the delimiter is not "\r" remove all "\r"s, and if 
   * the delimiter is not "\n" remove all "\n"s. 
   * Ignore delimiters within "..." quotes, and don't include the '\"'
   * character in the token.
   * If the nullFillStr is not null, then replace all null strings
   * with the nullFillStr.
   * @param str string containing a list of Strings
   * @param delimChr delimiter to be used
   * @param nullFillStr is the string to replace all null strings
   *          if not null.
   * @return array of Strings else null if problem
   */
    public static String[] cvs2ArrayNullFill(String str, String delimChr, String nullFillStr) {
        String tokArray[] = cvs2Array(str, delimChr);
        if (tokArray == null || nullFillStr == null) return (tokArray);
        int nTokens = tokArray.length;
        for (int i = 0; i < nTokens; i++) if (tokArray[i] == null) tokArray[i] = nullFillStr;
        return (tokArray);
    }

    /**
   * cvs2Array() - cvt arg list "1,4,3,6,..."  to "," - delim String[].
   * Also, if the delimiter is not "\r" remove all "\r"s, and if 
   * the delimiter is not "\n" remove all "\n"s. 
   * Ignore delimiters within "..." quotes, and don't include the '\"'
   * character in the token.
   * @param str string containing a list of Strings
   * @param delimChr delimiter to be used
   * @return array of Strings else null if problem
   */
    public static String[] cvs2Array(String str, String delimChr) {
        if (str == null || delimChr == null) return (null);
        if (!delimChr.equals("\r")) str = rmvSpecifiedChar(str, '\r');
        if (!delimChr.equals("\n")) str = rmvSpecifiedChar(str, '\n');
        int delimCnt = 0, count = 0, strLen = str.length();
        char delim = delimChr.charAt(0), searchArray[] = str.toCharArray();
        boolean insideQuoteFlag = false;
        while (count < strLen) {
            if (searchArray[count++] == delim) delimCnt++;
        }
        delimCnt++;
        String token, tokArray[] = new String[delimCnt];
        char ch, lineBuf[] = str.toCharArray(), tokBuf[] = new char[500];
        int tokBufSize = tokBuf.length, bufSize = strLen, bufCtr = 0;
        int nTokens = 0;
        for (int c = 0; c < delimCnt; c++) {
            int lastNonSpaceTokCtr = 0, tokCtr = 0;
            while (bufCtr < bufSize && (lineBuf[bufCtr] != delim || insideQuoteFlag)) {
                ch = lineBuf[bufCtr++];
                if (ch == '\"') insideQuoteFlag = !insideQuoteFlag; else {
                    if (tokCtr >= (tokBufSize - 1)) {
                        int newTokBufSize = (int) (2.0 * tokBufSize);
                        char newTokBuf[] = new char[newTokBufSize];
                        for (int k = 0; k < tokCtr; k++) newTokBuf[k] = tokBuf[k];
                        tokBuf = newTokBuf;
                        tokBufSize = newTokBufSize;
                    }
                    tokBuf[tokCtr++] = ch;
                }
                lastNonSpaceTokCtr = tokCtr;
            }
            tokBuf[tokCtr] = '\0';
            token = new String(tokBuf, 0, tokCtr);
            token = token.substring(0, lastNonSpaceTokCtr);
            tokArray[nTokens++] = token;
            if (bufCtr < bufSize && lineBuf[bufCtr] == delim) bufCtr++;
        }
        if (tokArray.length > nTokens) {
            String newTokArray[] = new String[nTokens];
            for (int i = 0; i < nTokens; i++) newTokArray[i] = tokArray[i];
            tokArray = newTokArray;
        }
        return (tokArray);
    }

    /** 
   * rightSizeArray() - extend or shrink an array to requiredSize.
   * If it is less than requiredSize and right fill it with fillStr values. 
   * If there is no change or the array is larger than the requiredSize, 
   * then  return the original array. If it is extended, return a new 
   * array. If the args are bad, return null.
   * @param strArray - string array
   * @param requiredSize - required size of the array
   * @param fillStr - fillValue if extending an array for new slots
   * @return extended String array if succeed, null if error.
   */
    public static String[] rightSizeArray(String strArray[], int requiredSize, String fillStr) {
        if (strArray == null || requiredSize <= 0) return (null);
        if (strArray.length == requiredSize) return (strArray);
        String sRtn[] = new String[requiredSize];
        for (int i = 0; i < requiredSize; i++) sRtn[i] = (i < strArray.length) ? strArray[i] : fillStr;
        return (sRtn);
    }

    /** 
   * cloneString() - clone a copy of a String array
   * @param strArray - string array to clone
   * @return cloned String array if succeed, null if error.
   */
    public static String[] cloneString(String strArray[]) {
        if (strArray == null) return (null);
        int lth = strArray.length;
        String sRtn[] = new String[lth];
        for (int i = 0; i < lth; i++) sRtn[i] = strArray[i];
        return (sRtn);
    }

    /**
   * lookupDataIdx() - lookup index of element if exists in a strData[].
   * Ignore case in the search.
   * @param field to lookup in the strData[]
   * @param strData is array to search
   * @param strDataLth is how far to search in the strData[]. If LEQ 0,
   *        assume it is strData.length
   * @return index if found, else if it does not exist return -1
   */
    public static int lookupDataIdx(String field, String strData[], int strDataLth) {
        if (field == null || strData == null) return (-1);
        if (strDataLth <= 0) strDataLth = strData.length;
        int idx = -1;
        for (int i = 0; i < strDataLth; i++) if (field.equalsIgnoreCase(strData[i])) {
            idx = i;
            break;
        }
        return (idx);
    }

    /** 
   * isListInAnotherList() - test if all listA in listB order independent.
   * @param listA - list that must be completely in listB
   * @param lthA - size of listA, else use listA.length if 0
   * @param listB - list to test against
   * @param lthB - size of listB, else use listB.length if 0
   * @return true if list A is completely in list B, false if not true
   *             or there is any error
   */
    public static boolean isListInAnotherList(String listA[], int lthA, String listB[], int lthB) {
        if (listA == null || listB == null) return (false);
        if (lthA <= 0) lthA = listA.length;
        if (lthB <= 0) lthB = listB.length;
        boolean flag = true;
        if (lthA > 0) {
            for (int i = 0; i < lthA; i++) {
                String strA = listA[i];
                if (lookupDataIdx(strA, listB, lthB) == -1) {
                    flag = false;
                    break;
                }
            }
        }
        return (flag);
    }

    /** 
   * mapAllkeywords() - map data from 'keyword' to 'toString' lists.
   * Do this for all instances in the  mapping list.
   * @param str input string containing possible keywords.
   * @param keyList is list of keywords to look for when map string
   * @param toStrList is list of replacement strings when map string
   * @param nMaps is size of the keyList[] and toStrList[] data
   * @return true if succeed
   */
    public static String mapAllkeywords(String str, String keyList[], String toStrList[], int nMaps) {
        if (str == null || nMaps == 0) return (str);
        String sPreface = null, sKeyword = null, sRest = null, sToString = null, sOld = "", sR = str;
        int keywordLth = 0, prefaceIdx = -1, restIdx = -1;
        while (!sOld.equals(sR)) {
            sOld = sR;
            for (int i = 0; i < nMaps; i++) {
                sKeyword = keyList[i];
                prefaceIdx = sR.indexOf(sKeyword);
                if (prefaceIdx == -1) continue;
                keywordLth = sKeyword.length();
                restIdx = prefaceIdx + keywordLth;
                if (restIdx >= sR.length()) sRest = ""; else sRest = sR.substring(restIdx);
                sPreface = sR.substring(0, prefaceIdx);
                sToString = toStrList[i];
                sR = sPreface + sToString + sRest;
            }
        }
        return (sR);
    }

    /**
   * rmvRtnChars() - remove return chars. Map '\r' or "\r\n" to '\n' chars.
   * @param String str to process
   * @return String with '\r' removed.
   */
    public static final String rmvRtnChars(String str) {
        if (str == null) return (null);
        int lthOut = 0, lthIn = str.length(), lastIdx = lthIn - 1;
        char ch, chLA, cOut[] = new char[lthIn], cIn[] = str.toCharArray();
        int i = 0;
        while (i < lthIn) {
            ch = cIn[i++];
            if (ch != '\r') cOut[lthOut++] = ch; else {
                chLA = (i == lastIdx) ? 0 : cIn[i + 1];
                if (chLA == '\n') i += 1;
                cOut[lthOut++] = '\n';
            }
        }
        String sR = new String(cOut, 0, lthOut);
        return (sR);
    }

    /**
   * rmvSpecifiedChar() - remove specified character from string.
   * @param str to process
   * @param rmvChar to remove
   * @return String with all instances of the specified character removed.
   */
    public static final String rmvSpecifiedChar(String str, char rmvChar) {
        if (str == null) return (null);
        if (rmvChar == '\0') return (str);
        int lthOut = 0, lthIn = str.length();
        char ch, cOut[] = new char[lthIn], cIn[] = str.toCharArray();
        int i = 0;
        while (i < lthIn) {
            ch = cIn[i++];
            if (ch != rmvChar) cOut[lthOut++] = ch;
        }
        String sR = new String(cOut, 0, lthOut);
        return (sR);
    }

    /**
   * rmvEnclosingWhitespace() - remove enclosing whitespace (spaces
   * proceeding and following the non-space string. If there is only
   * spaces, leave one. This counts space as white space not tabs
   * or CR or LF.
   * [TODO] could use trim() and save a lot of time...
   * @param str to process
   * @return String with all enclosing whitespace removed.
   */
    public static final String rmvEnclosingWhitespace(String str) {
        if (str == null || str.equals("") || str.equals(" ")) return (str);
        int lthIn = str.length(), leadingFirstNonBlankIdx = -1, endingFirstNonBlankIdx = -1;
        char cIn[] = str.toCharArray();
        for (int i = 0; i < lthIn; i++) {
            if (cIn[i] == ' ') continue;
            leadingFirstNonBlankIdx = i;
            break;
        }
        for (int i = lthIn - 1; i > -1; i--) {
            if (cIn[i] == ' ') continue;
            endingFirstNonBlankIdx = i + 1;
            break;
        }
        String sR = str;
        if (leadingFirstNonBlankIdx != -1 && endingFirstNonBlankIdx != -1) sR = str.substring(leadingFirstNonBlankIdx, endingFirstNonBlankIdx);
        return (sR);
    }

    /**
   * trimArrayEnclWhitespace() - trim (clean) enclosing white space in 1D array.
   * It goes element by element to try to remove white space. If an element
   * is cleaned, it replaces the element string, otherwise the element
   * string is not replaced.
   * @param rowData - row of data. Trim all non-null elements of the array
   * @return the row with non-null elements trimmed.
   */
    public static final String[] trimArrayEnclWhitespace(String rowData[]) {
        if (rowData != null) {
            int lth = rowData.length;
            for (int c = 0; c < lth; c++) {
                if (rowData[c] != null) rowData[c] = rowData[c].trim();
            }
        }
        return (rowData);
    }

    /**
    * removeEmptyStringsFromArray() - remove nulls and empty Strings in 1D array.
    * It removes "" (empty String) elements and nulls from an array.
    * @param rowData - row of data.
    * @return the row with non-null elements trimmed.
    */
    public static final String[] removeEmptyStringsFromArray(String rowData[]) {
        if (rowData == null) return (null);
        int lth = rowData.length;
        String sTmp, tmpList[] = new String[rowData.length];
        int nFound = 0;
        for (int c = 0; c < lth; c++) if (rowData[c] != null) {
            sTmp = rowData[c];
            if (sTmp == null) continue;
            sTmp = sTmp.trim();
            if (sTmp.equals("")) continue;
            tmpList[nFound++] = sTmp;
        }
        String trimmedData[] = new String[nFound];
        for (int c = 0; c < nFound; c++) trimmedData[c] = tmpList[c];
        return (trimmedData);
    }

    /**
    * getCleanArgList() - convert string arg list delimited by "", "," or "\n" to array
    * Remove blank args.
    * @param strArg to process
    * @return String array if succeed with empty args removed.
    */
    public static final String[] getCleanArgList(String strArg) {
        String str = UtilCM.mapCRLF2space(strArg), sCommaList = UtilCM.replaceSubstrInString(str, " ", ","), sArgList[] = cvs2Array(sCommaList, ","), sR[] = removeEmptyStringsFromArray(sArgList);
        return (sR);
    }

    /**
    * getCleanSpaceList() - cvt str list delim by "  ", ",", "\t", "\n" to " " delim str
    * Standardize to space delimited row of symbols
    * map commas, tabs, "\n" and multiple spaces to single space
    * @param strArg to process
    * @return String  if succeed with empty args removed.
    */
    public static final String getCleanSpaceList(String strArg) {
        String str = UtilCM.mapCRLF2space(strArg), str1 = UtilCM.replaceSubstrInString(str, ",", " ");
        str = UtilCM.replaceSubstrInString(str1, "\t", " ");
        String sR = str;
        str = UtilCM.replaceSubstrInString(str, "  ", " ");
        while (sR.length() != str.length()) {
            str = UtilCM.replaceSubstrInString(str1, "  ", " ");
            sR = str;
        }
        return (sR);
    }

    /**
   * replaceSpaceWithCommaSpaceDelim() - replace " "-delim list with ", " 
   * list in the string str. It does it in a two stage process using
   * the recursive replaceSubstrInString() by first mapping spaces to
   * "@#$*" and then again to change that to ", ".
   * @param str to process
   * @return String " "-delimited list with ", "-delim. list, 
   *         else null if fail.
   * @see #replaceSubstrInString
   */
    public static final String replaceSpaceWithCommaSpaceDelim(String str) {
        String sTmp = UtilCM.replaceSubstrInString(str, " ", "@#$*"), sR = UtilCM.replaceSubstrInString(sTmp, "@#$*", ", ");
        return (sR);
    }

    /**
   * replaceSubstrInString() - recursively replace oldToken with newToken in 
   * the string str. Call it recursively to get all of the instances.
   * @param str to process
   * @param oldToken to be replaced
   * @param newToken to replace it with
   * @return String with replaced token, else null if fail.
   */
    public static final String replaceSubstrInString(String str, String oldToken, String newToken) {
        if (str == null || oldToken == null || newToken == null) return (str);
        String sR = str, frontOfStr = null, restOfStr = null;
        if (oldToken != null && oldToken.length() > 0) {
            int idxOldToken = str.indexOf(oldToken);
            if (idxOldToken != -1) {
                int idxRestOfStr = idxOldToken + oldToken.length();
                frontOfStr = str.substring(0, idxOldToken);
                restOfStr = str.substring(idxRestOfStr);
                sR = frontOfStr + newToken + restOfStr;
                sR = replaceSubstrInString(sR, oldToken, newToken);
            }
        }
        return (sR);
    }

    /**
   * changeWorkingDirectory() - change the working directory
   * @param newDir to change the user.dir to.
   * @return the new conanocalPath of the directory if succeed, 
   *        else null
   */
    public static String changeWorkingDirectory(String newDir) {
        try {
            String oldDir = System.getProperty("user.dir");
            System.setProperty("user.dir", newDir);
            File cur = new File(".");
            String newDirCanonical = cur.getCanonicalPath();
            String curDir = System.getProperty("user.dir");
            return (newDirCanonical);
        } catch (Exception e) {
            return (null);
        }
    }

    /**
   * cvDeltaTime2str() - convert a delta epoch time msec to a string 
   *     "00:01:30 (H:M:S) or 90.0 seconds"
   * @param deltaTimeMsec is the runtime (end-start) in msec.
   * @param fullCvtFlag to generate a full conversion with H:M:S
   *                     "00:01:30 (H:M:S) or 90.0 seconds"
   *             else generate 
   *                     "90.0 seconds"
   * @returns program run time in H:M:S and 1/10 seconds.
   */
    public static String cvDeltaTime2str(long deltaTimeMsec, boolean fullCvtFlag) {
        int run = (int) (deltaTimeMsec / 1000), Rhours = run / 3600, Rminutes = (run - (Rhours * 3600)) / 60, Rseconds = (run - (Rhours * 3600) - (Rminutes * 60));
        float runMsec = (float) deltaTimeMsec, totRseconds = runMsec / 1000.0F;
        String secR = cvf2s(totRseconds, 1) + " seconds", sR = secR;
        if (fullCvtFlag) sR = Rhours + ":" + Rminutes + ":" + Rseconds + " (H:M:S) or " + secR;
        return (sR);
    }

    /**
   * off_timer() - Compute the time banner String assuming that a
   * on_timer(cpuTime,runTime) was done previously.
   * EXAMPLE:
   *     "00:01:30 (H:M:S) or 90.0 seconds"
   * @param startT is the starting times runtime
   * @returns program run time (endTime-startTime) in H:M:S and 1/10
   *          seconds
   */
    public static String off_timer(long startT) {
        long endTime = on_timer(), deltaTimeMsec = (endTime - startT);
        String sR = "Run time =" + cvDeltaTime2str(deltaTimeMsec, true) + "\n";
        return (sR);
    }

    /**
   * on_timer() - Get the current time (in milliseconds)
   * @return runtime
   */
    public static long on_timer() {
        Date dateObj = new Date();
        long time = dateObj.getTime();
        return (time);
    }

    /**
   * sleepMsec() - sleep for mSec.
   * @param mSec
   */
    public static synchronized void sleepMsec(int mSec) {
        try {
            Thread.sleep(mSec);
        } catch (InterruptedException e) {
        }
    }

    /**
   * appendElementToArray() - Append String element to String array.
   * If the array is null, create the array, else extend it 1 element.
   * @param sArray - is array to append element to.
   * @param sElement to append
   * @return extended array.
   */
    public static String[] appendElementToArray(String sArray[], String sElement) {
        int n;
        if (sArray == null) {
            sArray = new String[1];
            n = 1;
        } else {
            n = sArray.length;
            String tmp[] = new String[n + 1];
            for (int i = 0; i < n; i++) tmp[i] = sArray[i];
            sArray = tmp;
            n++;
        }
        sArray[n - 1] = sElement;
        return (sArray);
    }

    /**
   * startsWithIgnoreCase() - test if str starts with startStr ignoring case
   * @param str - string to search
   * @param startStr - substring to see if str starts with this startStr
   * @return true if it starts with the substring ignoring case.
   */
    public static boolean startsWithIgnoreCase(String str, String startStr) {
        String strUC = str.toUpperCase(), startStrUC = startStr.toUpperCase();
        return (strUC.startsWith(startStrUC));
    }

    /**
    * endsWithIgnoreCase() - test if str ends with endsStr ignoring case
    * @param str - string to search
    * @param endsStr - substring to see if str starts with this endsStr
    * @return true if it starts with the substring ignoring case.
    */
    public static boolean endsWithIgnoreCase(String str, String endsStr) {
        String strUC = str.toUpperCase(), endsStrUC = endsStr.toUpperCase();
        return (strUC.endsWith(endsStrUC));
    }

    /**
     * indexOfIgnoreCase() - indexOf subStr of str ignoring case.
     * @param str - string to search
     * @param subStr - substring to see if str starts with this endsStr
     * @return indexOf subStr of str ignoring case.
     */
    public static int indexOfIgnoreCase(String str, String subStr) {
        String strUC = str.toUpperCase(), subStrUC = subStr.toUpperCase();
        return (strUC.indexOf(subStrUC));
    }

    /** 
      * cvtHTMLcolorNameToHex() - map HTML color name to HEX value string
      * Names allowed are the 16 colors in version 5 HTML standard.
      * @param cName color name
      * @return hex string equivalent name if found, else just return cName.
      */
    public static String cvtHTMLcolorNameToHex(String cName) {
        if (cName.equalsIgnoreCase("black")) return ("#000000"); else if (cName.equalsIgnoreCase("sliver")) return ("#C0C0C0"); else if (cName.equalsIgnoreCase("gray")) return ("#808080"); else if (cName.equalsIgnoreCase("white")) return ("#FFFFFF"); else if (cName.equalsIgnoreCase("maroon")) return ("#800000"); else if (cName.equalsIgnoreCase("red")) return ("#FF0000"); else if (cName.equalsIgnoreCase("purple")) return ("#800080"); else if (cName.equalsIgnoreCase("fuscia")) return ("#FF00FF"); else if (cName.equalsIgnoreCase("green")) return ("#008000"); else if (cName.equalsIgnoreCase("lime")) return ("#00FF00"); else if (cName.equalsIgnoreCase("olive")) return ("#808000"); else if (cName.equalsIgnoreCase("yellow")) return ("#FFFF00"); else if (cName.equalsIgnoreCase("navy")) return ("#000080"); else if (cName.equalsIgnoreCase("blue")) return ("#0000FF"); else if (cName.equalsIgnoreCase("teal")) return ("#008080"); else if (cName.equalsIgnoreCase("aqua")) return ("#00FFFF");
        return ("cName");
    }

    /**
     * updateJarFile() - update .jar file from URL into program install area.
     *<PRE>
     * [1] Define directory for .jar path and other file and URL names.
     * [2] Backup the old {pgmName}.jar as {pgmName}.jar.bkup
     * [3] Open the url: from urlJarFile and read the file from
     *     the Web into local file {pgmName}+".jar.tmp"
     * [4] Move the {pgmName}+".jar.tmp" file into {pgmName}+".jar" in the
     *     program directory
     *</PRE>
     * @param pgmName - base name of the program (no extension)
     * @param urlJarFile - full URL for file on the server
     * @param localJarFilePath - full path for the target file
     * @return true if succeed
     * @see #copyFile
     * @see #deleteLocalFile
     */
    public static boolean updateJarFile(String pgmName, String urlJarFile, String localJarFilePath) {
        String fileSep = System.getProperty("file.separator"), userDir = System.getProperty("user.dir") + fileSep, localJarFile = userDir + pgmName + ".jar", localJarFileBkup = userDir + pgmName + ".jar.bkup", localJarFileTmp = userDir + pgmName + ".jar.tmp";
        deleteLocalFile(localJarFileBkup);
        boolean ok = copyFile(localJarFile, localJarFileBkup, null, 0);
        if (!ok) return (false);
        String updateMsg = "Updating your " + pgmName + ".jar file from " + urlJarFile + " server.\n";
        File f = new File(localJarFileBkup);
        int estInputFileLth = (f != null) ? (int) f.length() : 0;
        if (!copyFile(urlJarFile, localJarFileTmp, updateMsg, estInputFileLth)) return (false);
        if (!deleteLocalFile(localJarFile)) return (false);
        if (!copyFile(localJarFileTmp, localJarFile, null, 0)) return (false);
        return (true);
    }

    /**
     * copyFile() - binary copy of one file or URL to a local file
     * @param srcName is either a full path local file name or
     *        a http:// prefixed URL string of the source file.
     * @param dstName is the full path of the local destination file name
     * @param optUpdateMsg (opt) will display message in showMsg() and
     *        increasing ... in showMsg2(). One '.' for every 10K bytes read.
     *        This only is used when reading a URL. Set to null if not used.
     * @param optEstInputFileLth is the estimate size of the input file if 
     *        known else 0. Used in progress bar.
     * @return true if succeed.
     */
    public static boolean copyFile(String srcName, String dstName, String optUpdateMsg, int optEstInputFileLth) {
        try {
            FileOutputStream dstFOS = new FileOutputStream(new File(dstName));
            FileInputStream srcFIS = null;
            int bufSize = 100000, nBytesRead = 0, nBytesWritten = 0;
            byte buf[] = new byte[bufSize];
            boolean isURL = (srcName.startsWith("http://") || srcName.startsWith("file://"));
            if (isURL) {
                if (optUpdateMsg != null) logMsg(optUpdateMsg);
                String sDots = "";
                URL url = new URL(srcName);
                InputStream urlIS = url.openStream();
                int nTotBytesRead = 0;
                while (true) {
                    if (optUpdateMsg != null) {
                        sDots += ".";
                        String sPct = (optEstInputFileLth > 0) ? ((int) ((100 * nTotBytesRead) / optEstInputFileLth)) + "% " : "", sProgress = "Copying " + sPct + sDots + "\n";
                        logMsg(sProgress);
                    }
                    nBytesRead = urlIS.read(buf);
                    nTotBytesRead += nBytesRead;
                    if (nBytesRead == -1) break; else {
                        dstFOS.write(buf, 0, nBytesRead);
                        nBytesWritten += nBytesRead;
                    }
                }
                dstFOS.close();
                if (optUpdateMsg != null) {
                    logMsg("\n");
                }
            } else {
                srcFIS = new FileInputStream(new File(srcName));
                while (true) {
                    nBytesRead = srcFIS.read(buf);
                    if (nBytesRead == -1) break; else {
                        dstFOS.write(buf, 0, nBytesRead);
                        nBytesWritten += nBytesRead;
                    }
                }
                srcFIS.close();
                dstFOS.close();
            }
        } catch (Exception e1) {
            return (false);
        }
        return (true);
    }

    /**
     * readBytesFromURL() - read binary data from URL into byte[] array.
     * @param srcName is either a full path local file name or
     *        a http:// prefixed URL string of the source file.
     * @param optUpdateMsg (opt) will display message in showMsg() and
     *        increasing ... in showMsg2(). One '.' for every 10K bytes read.
     *        This only is used when reading a URL. Set to null if not used.
     * @return a byte[] if succeed, else null.
     */
    public static byte[] readBytesFromURL(String srcName, String optUpdateMsg) {
        if (!srcName.startsWith("http://") && !srcName.startsWith("file://")) return (null);
        int bufSize = 20000, nBytesRead = 0, nBytesWritten = 0, oByteSize = bufSize;
        byte buf[] = null, oBuf[] = null;
        try {
            buf = new byte[bufSize];
            oBuf = new byte[bufSize];
            if (optUpdateMsg != null) logMsg(optUpdateMsg);
            String sDots = "";
            URL url = new URL(srcName);
            InputStream urlIS = url.openStream();
            logMsg("Reading " + sDots);
            while (true) {
                if (optUpdateMsg != null) {
                    logMsg(".");
                }
                nBytesRead = urlIS.read(buf);
                if (nBytesRead == -1) break; else {
                    if (nBytesRead + nBytesWritten >= oByteSize) {
                        byte tmp[] = new byte[oByteSize + bufSize];
                        for (int i = 0; i < nBytesWritten; i++) tmp[i] = oBuf[i];
                        oBuf = tmp;
                        oByteSize += bufSize;
                    }
                    for (int i = 0; i < nBytesRead; i++) oBuf[nBytesWritten++] = buf[i];
                }
            }
            byte tmp[] = new byte[nBytesWritten];
            for (int i = 0; i < nBytesWritten; i++) tmp[i] = oBuf[i];
            oBuf = tmp;
            if (optUpdateMsg != null) {
                logMsg("\n");
            }
        } catch (Exception e) {
            logMsg("Problem can't readBytesFromURL() e=" + e + "\n");
            return (null);
        }
        return (oBuf);
    }

    /**
     * deleteLocalFile() - delete local file.
     * @param fileName to delete
     * @return false if failed..
     */
    public static boolean deleteLocalFile(String fileName) {
        try {
            File srcF = new File(fileName);
            if (srcF.exists()) srcF.delete();
        } catch (Exception e) {
            return (false);
        }
        return (true);
    }
}
