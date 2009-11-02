/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.standard;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Formatter;

import ucar.nc2.ft.FeatureDatasetPoint;

/**
 * Helper class to convert a  TableConfig to and from XML
 *
 * @author caron
 * @since Aug 18, 2009
 */
public class PointConfigXML {
  private TableConfig tc;
  private String analyserClass;

  public static void writeConfigXML(FeatureDatasetPoint pfd, java.util.Formatter f) {
    if (!(pfd instanceof PointDatasetStandardFactory.PointDatasetStandard)) {
      f.format("%s not instance of PointDatasetStandard%n", pfd.getLocation());
      return;
    }
    PointDatasetStandardFactory.PointDatasetStandard spfd = (PointDatasetStandardFactory.PointDatasetStandard) pfd;
    TableAnalyzer analyser = spfd.getTableAnalyzer();
    TableConfig tc = analyser.getTableConfig();
    if (tc == null) {
      f.format("%s has no TableConfig%n", pfd.getLocation());
      return;
    }

    PointConfigXML xml = new PointConfigXML(tc, analyser.getClass().getName());
    try {
      xml.writeConfigXML(f);
    } catch (IOException e) {
      f.format("%s error writing=%s%n", pfd.getLocation(), e.getMessage());
      return;
    }
  }

  PointConfigXML(TableConfig tc, String analyserClass) {
    this.tc = tc;
    this.analyserClass = analyserClass;
  }

  public void writeConfigXML(java.util.Formatter sf) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    sf.format("%s", fmt.outputString(makeDocument()));
  }

  /**
   * Create an XML document from this info
   *
   * @return netcdfDatasetInfo XML document
   */
  public Document makeDocument() {
    Element rootElem = new Element("pointConfig");
    Document doc = new Document(rootElem);
    if (analyserClass != null)
      rootElem.addContent( new Element("analyser").setAttribute("class", analyserClass));
    if (tc.featureType != null)
      rootElem.setAttribute("featureType", tc.featureType.toString());

    rootElem.addContent(writeTable(tc));

    return doc;
  }

  private Element writeTable(TableConfig config) {
    Element tableElem = new Element("table");

    //if (config.name != null)
    //  tableElem.setAttribute("name", config.name);
    if (config.type != null)
      tableElem.setAttribute("type", config.type.toString());

    switch (config.type) {
      case ArrayStructure:
        tableElem.setAttribute("dimension", config.dim.getName());
        break;

      case Construct:
        break;

      case Contiguous:
        if (config.start != null)
          tableElem.setAttribute("start", config.start);
        tableElem.setAttribute("numRecords", config.numRecords);
        break;

      case LinkedList:
        tableElem.setAttribute("start", config.start);
        tableElem.setAttribute("next", config.next);
        break;

      case MultidimInner:
      case MultidimInnerPsuedo:
        tableElem.setAttribute("dim0", config.outer.getName());
        tableElem.setAttribute("dim1", config.inner.getName());
        break;

      case MultidimInner3D:
        break;

      case MultidimInnerPsuedo3D:
        break;

      case MultidimStructure:
        tableElem.setAttribute("structName", config.structName);
        break;

      case NestedStructure:
        tableElem.setAttribute("structName", config.structName);
        break;

      case ParentId:
      case ParentIndex:
        tableElem.setAttribute("parentIndex", config.parentIndex);
        break;

      case Singleton:
        break;

      case Structure:
        tableElem.setAttribute("subtype", config.structureType.toString());
        switch (config.structureType) {
          case Structure:
            tableElem.setAttribute("structName", config.structName);
            break;
          case PsuedoStructure:
            tableElem.setAttribute("dim", config.dim.getName());
            break;
          case PsuedoStructure2D:
            tableElem.setAttribute("dim0", config.dim.getName());
            tableElem.setAttribute("dim1", config.outer.getName());
            break;
        }
        break;

      case Top:
        tableElem.setAttribute("structName", config.structName);
        break;

    }

    List<String> varNames = config.vars == null ? new ArrayList<String>() : new ArrayList<String>(config.vars);
    addCoordinates(tableElem, config, varNames);
    if (config.vars != null) {
      for (String col : varNames)
        tableElem.addContent(new Element("variable").addContent(col));
    }

    if (config.extraJoin != null) {
      for (Join j : config.extraJoin) {
        if (j instanceof JoinArray)
          tableElem.addContent(writeJoinArray((JoinArray) j));
        else if (j instanceof JoinMuiltdimStructure)
          tableElem.addContent(writeJoinMuiltdimStructure((JoinMuiltdimStructure) j));
        else if (j instanceof JoinParentIndex)
          tableElem.addContent(writeJoinParentIndex((JoinParentIndex) j));
      }
    }

    if (config.children != null) {
      for (TableConfig child : config.children)
        tableElem.addContent(writeTable(child));
    }
    return tableElem;
  }

  private void addCoordinates(Element tableElem, TableConfig table, List<String> varNames) {
    addCoord(tableElem, table.lat, "lat", varNames);
    addCoord(tableElem, table.lon, "lon", varNames);
    addCoord(tableElem, table.elev, "elev", varNames);
    addCoord(tableElem, table.time, "time", varNames);
    addCoord(tableElem, table.timeNominal, "timeNominal", varNames);
    addCoord(tableElem, table.stnId, "stnId", varNames);
    addCoord(tableElem, table.stnDesc, "stnDesc", varNames);
    addCoord(tableElem, table.stnNpts, "stnNpts", varNames);
    addCoord(tableElem, table.stnWmoId, "stnWmoId", varNames);
    addCoord(tableElem, table.stnAlt, "stnAlt", varNames);
    addCoord(tableElem, table.limit, "limit", varNames);
    addCoord(tableElem, table.feature_id, "featureId", varNames);
    addCoord(tableElem, table.missingVar, "isMissingVar", varNames);
  }

  private void addCoord(Element tableElem, String name, String type, List<String> varNames) {
    if (name != null) {
      Element elem = new Element("coordinate").setAttribute("type", type);
      elem.addContent(name);
      tableElem.addContent(elem);
      varNames.remove(name);
    }
  }

  private Element writeJoinArray(JoinArray join) {
    Element joinElem = new Element("extraJoin");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.type != null)
      joinElem.setAttribute("type", join.type.toString());
    if (join.v != null)
      joinElem.addContent(new Element("variable").addContent(join.v.getName()));
    joinElem.addContent(new Element("param").setAttribute("value", Integer.toString(join.param)));
    return joinElem;
  }

  private Element writeJoinMuiltdimStructure(JoinMuiltdimStructure join) {
    Element joinElem = new Element("extraJoin");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.parentStructure != null)
      joinElem.addContent(new Element("parentStructure").addContent(join.parentStructure.getName()));
    joinElem.addContent(new Element("dimLength").setAttribute("value", Integer.toString(join.dimLength)));
    return joinElem;
  }

  private Element writeJoinParentIndex(JoinParentIndex join) {
    Element joinElem = new Element("extraJoin");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.parentStructure != null)
      joinElem.addContent(new Element("parentStructure").addContent(join.parentStructure.getName()));
    if (join.parentIndex != null)
      joinElem.addContent(new Element("parentIndex").addContent(join.parentIndex));
    return joinElem;
  }
}