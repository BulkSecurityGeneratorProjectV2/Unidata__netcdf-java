/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import opendap.dap.parsers.ParseException;

/**
 * Default handler for OPeNDAP directory requests. This class is used
 * by AbstractServlet. This code exists as a separate class in order to alleviate
 * code bloat in the AbstractServlet class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class GetDirHandler {

  private static final boolean _Debug = false;
  private String separator = "/";


  /**
   * ************************************************************************
   * Default handler for OPeNDAP directory requests. Returns an html document
   * with a list of all datasets on this server with links to their
   * DDS, DAS, Information, and HTML responses.
   *
   * @param rs The request state object for this client request.
   * @see ReqState
   */
  public void sendDIR(ReqState rs) throws opendap.dap.DAP2Exception, ParseException {

    if (_Debug)
      System.out.println("sendDIR request = " + rs.getRequest());


    try {

      PrintWriter pw =
          new PrintWriter(new OutputStreamWriter(rs.getResponse().getOutputStream(), StandardCharsets.UTF_8));

      // ignore String ddxCacheDir = rs.getDDXCache(rs.getRootPath());
      String ddsCacheDir = rs.getDDSCache(rs.getRootPath());

      String thisServer = rs.getRequest().getRequestURL().toString();
      pw.println("<html>");
      pw.println("<head>");
      pw.println("<title>OPeNDAP Directory</title>");
      pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html\">");
      pw.println("</head>");

      pw.println("<body bgcolor=\"#FFFFFF\">");


      pw.println("<h1>OPeNDAP Directory for:</h1>");
      pw.println("<h2>" + thisServer + "</h2>");

      // ignore printDIR(pw, ddxCacheDir, "DDX", thisServer);

      printDIR(pw, ddsCacheDir, "DDS", thisServer);
      pw.println("<hr>");
      pw.println("</html>");
      pw.flush();

    } catch (FileNotFoundException fnfe) {
      System.out.println("OUCH! FileNotFoundException: " + fnfe.getMessage());
      fnfe.printStackTrace(System.out);
    } catch (IOException ioe) {
      System.out.println("OUCH! IOException: " + ioe.getMessage());
      ioe.printStackTrace(System.out);
    }


  }


  private void printDIR(PrintWriter pw, String dirName, String dirType, String thisServer) {

    pw.println("<hr>");
    pw.println("<h3>" + dirType + "</h3>");

    File dir = new File(dirName);

    if (dir.exists()) {

      if (dir.isDirectory()) {

        if (_Debug)
          System.out.println("lastIndexOf(" + separator + "): " + thisServer.lastIndexOf(separator));
        if (_Debug)
          System.out.println("length: " + thisServer.length());

        if (thisServer.lastIndexOf(separator) != (thisServer.length() - 1)) {
          if (_Debug)
            System.out.println("Server URL does not end with: " + separator);
          thisServer += separator;
        } else {
          if (_Debug)
            System.out.println("Server URL ends with: " + separator);
        }


        File fList[] = dir.listFiles();

        pw.println("<table border=\"0\">");

        for (int i = 0; i < fList.length; i++) {
          if (fList[i].isFile()) {

            pw.println("<tr>");

            pw.print("    <td>");
            pw.print("<div align='right'>");
            pw.print("<b>" + fList[i].getName() + ":</b> ");
            pw.print("</div>");
            pw.println("</td>");

            /*
             * ignore
             * pw.print("    <td>");
             * pw.print("<div align='center'>");
             * pw.print("<a href='" +
             * thisServer +
             * fList[i].getName() +
             * ".ddx'> DDX </a>");
             * pw.print("</div>");
             * pw.println("</td>");
             */

            pw.print("    <td>");
            pw.print("<div align='center'>");
            pw.print("<a href='" + thisServer + fList[i].getName() + ".dds'> DDS </a>");
            pw.print("</div>");
            pw.println("</td>");

            pw.print("    <td>");
            pw.print("<div align='center'>");
            pw.print("<a href='" + thisServer + fList[i].getName() + ".das'> DAS </a>");
            pw.print("</div>");
            pw.println("</td>");

            pw.print("    <td>");
            pw.print("<div align='center'>");
            pw.print("<a href='" + thisServer + fList[i].getName() + ".info'> Information </a>");
            pw.print("</div>");
            pw.println("</td>");

            pw.print("    <td>");
            pw.print("<div align='center'>");
            pw.print("<a href='" + thisServer + fList[i].getName() + ".html'> HTML Data Request Form </a>");
            pw.print("</div>");
            pw.println("</td>");

            pw.println("</tr>");
          }
        }
        pw.println("</table>");

      } else {
        pw.println("<h3>");
        pw.println("Specified " + dirType + " cache:<br>");
        pw.println("<i>" + dirName + "</i><br>");
        pw.println("is not a directory!");
        pw.println("</h3>");
      }
    } else {
      pw.println("<h4>");
      pw.println("Cannot Find " + dirType + " Directory:<br>");
      pw.println("<i>" + dirName + "</i><br>");
      pw.println("</h4>");
    }


  }


}

