/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ma2;

/**
 * Creates a StructureDataIterator by wrapping a section of a ArrayStructure.
 * 
 * @deprecated do not use. LOOK move to BUFR if needed.
 */
@Deprecated
public class SequenceIterator implements StructureDataIterator {
  private final int start;
  private final int size;
  private int count;
  private final ArrayStructure abb;

  public SequenceIterator(int start, int size, ArrayStructure abb) {
    this.start = start;
    this.size = size;
    this.abb = abb;
    this.count = 0;
  }

  @Override
  public boolean hasNext() {
    return (count < size);
  }

  @Override
  public StructureData next() {
    StructureData result = abb.getStructureData(start + count);
    count++;
    return result;
  }

  @Override
  public StructureDataIterator reset() {
    count = 0;
    return this;
  }

  @Override
  public int getCurrentRecno() {
    return count;
  }

}
