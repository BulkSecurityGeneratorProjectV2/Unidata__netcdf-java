/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.bufr.writer;

import ucar.nc2.*;
import ucar.nc2.iosp.netcdf3.N3iosp;
import ucar.nc2.iosp.bufr.BufrIosp2;
import ucar.ma2.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 21, 2008
 */
public class WriteT41_ncRect {
  private static boolean debug = true;

  WriteT41_ncRect(NetcdfFile bufr, String fileOutName, boolean fill) throws IOException, InvalidRangeException {

    try (NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(fileOutName, fill)) {
      if (debug) {
        System.out.println("FileWriter write " + bufr.getLocation() + " to " + fileOutName);
      }

      // global attributes
      List<Attribute> glist = bufr.getGlobalAttributes();
      for (Attribute att : glist) {
        String useName = N3iosp.makeValidNetcdfObjectName(att.getShortName());
        Attribute useAtt;
        if (att.isArray())
          useAtt = ncfile.addGroupAttribute(null, new Attribute(useName, att.getValues()));
        else if (att.isString())
          useAtt = ncfile.addGlobalAttribute(useName, att.getStringValue());
        else
          useAtt = ncfile.addGlobalAttribute(useName, att.getNumericValue());
        if (debug)
          System.out.println("add gatt= " + useAtt);
      }

      // global dimensions
      Dimension recordDim = null;
      for (Dimension oldD : bufr.getDimensions()) {
        String useName = N3iosp.makeValidNetcdfObjectName(oldD.getShortName());
        boolean isRecord = useName.equals("record");
        Dimension newD =
            ncfile.addDimension(null, useName, isRecord ? 0 : oldD.getLength(), isRecord, oldD.isVariableLength());
        if (isRecord)
          recordDim = newD;
        if (debug)
          System.out.println("add dim= " + newD);
      }

      // Variables
      Structure recordStruct = (Structure) bufr.findVariable(BufrIosp2.obsRecordName);
      for (Variable oldVar : recordStruct.getVariables()) {
        if (oldVar.getDataType() == DataType.SEQUENCE)
          continue;

        String varName = N3iosp.makeValidNetcdfObjectName(oldVar.getShortName());
        DataType newType = oldVar.getDataType();

        List<Dimension> newDims = new ArrayList<>();
        newDims.add(recordDim);
        for (Dimension dim : oldVar.getDimensions()) {
          newDims.add(ncfile.addDimension(oldVar.getShortName() + "_strlen", dim.getLength()));
        }

        Variable newVar = ncfile.addVariable(null, varName, newType, newDims);
        if (debug)
          System.out.println("add var= " + newVar);

        // attributes
        for (Attribute att : oldVar.attributes()) {
          String useName = N3iosp.makeValidNetcdfObjectName(att.getShortName());
          if (att.isArray())
            newVar.addAttribute(new Attribute(useName, att.getValues()));
          else if (att.isString())
            ncfile.addVariableAttribute(varName, useName, att.getStringValue());
          else
            ncfile.addVariableAttribute(varName, useName, att.getNumericValue());
        }
      }

      int max_seq = countSeq(recordStruct);
      Dimension seqD = ncfile.addDimension("level", max_seq);

      for (Variable v : recordStruct.getVariables()) {
        if (v.getDataType() != DataType.SEQUENCE)
          continue;

        Structure seq = (Structure) v;
        for (Variable seqVar : seq.getVariables()) {
          String varName = N3iosp.makeValidNetcdfObjectName(seqVar.getShortName());
          DataType newType = seqVar.getDataType();

          List<Dimension> newDims = new ArrayList<>();
          newDims.add(recordDim);
          newDims.add(seqD);
          for (Dimension dim : seqVar.getDimensions()) {
            newDims.add(ncfile.addDimension(seqVar.getShortName() + "_strlen", dim.getLength()));
          }

          Variable newVar = ncfile.addVariable(null, varName, newType, newDims);
          if (debug)
            System.out.println("add var= " + newVar);

          // attributes
          for (Attribute att : seqVar.attributes()) {
            String useName = N3iosp.makeValidNetcdfObjectName(att.getShortName());
            if (att.isArray())
              newVar.addAttribute(new Attribute(useName, att.getValues()));
            else if (att.isString())
              ncfile.addVariableAttribute(varName, useName, att.getStringValue());
            else
              ncfile.addVariableAttribute(varName, useName, att.getNumericValue());
          }
        }
      }

      // create the file
      ncfile.create();
      if (debug)
        System.out.println("File Out= " + ncfile);

      // boolean ok = (Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

      double total = copyVarData(bufr, ncfile, recordStruct);
      ncfile.flush();
      if (debug)
        System.out.println("FileWriter done total bytes = " + total);
    }
  }

  private int countSeq(Structure recordStruct) throws IOException {
    int total = 0;
    int count = 0;
    int max = 0;

    try (StructureDataIterator iter = recordStruct.getStructureIterator()) {
      while (iter.hasNext()) {
        StructureData sdata = iter.next();
        ArraySequence seq1 = sdata.getArraySequence("seq1");
        int n = seq1.getStructureDataCount();
        total += n;
        count++;
        max = Math.max(max, n);
      }
    }
    if (count > 0 && max > 0) {
      double avg = (double) total / count;
      int wasted = count * max - total;
      double wp = (double) wasted / (count * max);
      System.out.println(" Max = " + max + " avg = " + avg + " wasted = " + wasted + " %= " + wp);
    } else {
      System.out.println(" T41_ncFlat - countSeq called on empty recordStruct" + " max = " + max + " count = " + count);
    }
    return max;
  }

  private double copyVarData(NetcdfFile bufr, NetcdfFileWriter ncfile, Structure recordStruct)
      throws IOException, InvalidRangeException {
    int nrecs = (int) recordStruct.getSize();
    int sdataSize = recordStruct.getElementSize();

    double total = 0;
    double totalRecordBytes = 0;
    for (int count = 0; count < nrecs; count++) {

      StructureData recordData = recordStruct.readStructure(count);
      for (StructureMembers.Member m : recordData.getMembers()) {

        if (m.getDataType() == DataType.SEQUENCE) {
          int countLevel = 0;
          ArraySequence seq1 = recordData.getArraySequence(m);
          try (StructureDataIterator iter = seq1.getStructureDataIterator()) {
            while (iter.hasNext()) {
              StructureData seqData = iter.next();
              for (StructureMembers.Member seqm : seqData.getMembers()) {
                Array data = seqData.getArray(seqm);
                int[] shape = data.getShape();
                int[] newShape = new int[data.getRank() + 2];
                newShape[0] = 1;
                newShape[1] = 1;
                System.arraycopy(shape, 0, newShape, 1, data.getRank());

                int[] origin = new int[data.getRank() + 2];
                origin[0] = count;
                origin[1] = countLevel;

                if (debug && (count == 0) && (countLevel == 0))
                  System.out.println("write to = " + seqm.getName());
                ncfile.write(seqm.getName(), origin, data.reshape(newShape));
              }
              countLevel++;
            }
          }
        } else {

          Array data = recordData.getArray(m);
          int[] shape = data.getShape();
          int[] newShape = new int[data.getRank() + 1];
          newShape[0] = 1;
          System.arraycopy(shape, 0, newShape, 1, data.getRank());

          int[] origin = new int[data.getRank() + 1];
          origin[0] = count;

          if (debug && (count == 0))
            System.out.println("write to = " + m.getName());
          ncfile.write(m.getName(), origin, data.reshape(newShape));
        }
      }
      totalRecordBytes += sdataSize;
    }

    total += totalRecordBytes;
    totalRecordBytes /= 1000 * 1000;
    if (debug)
      System.out.println("write record var; total = " + totalRecordBytes + " Mbytes # recs=" + nrecs);

    return total;
  }
}
