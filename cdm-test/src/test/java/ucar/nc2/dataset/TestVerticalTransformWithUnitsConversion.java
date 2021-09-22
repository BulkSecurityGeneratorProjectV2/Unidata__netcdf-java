/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.assertArrayEquals;

/**
 * 
 * Sometimes all the vars required in the vertical transformation may not have coherent units.
 * This parameterized unit test tests those transformations that make unit checks:
 * - AtmosSigma
 * - HybridSigma (with P param and with AP param)
 * 
 * @author mhermida
 *
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestVerticalTransformWithUnitsConversion {
  private final String sameUnitsFile;
  private final LatLonPoint point;
  private final String var;

  public TestVerticalTransformWithUnitsConversion(String sameUnitsFile, String diffUnitsFile, LatLonPoint point,
      String var) {
    this.sameUnitsFile = sameUnitsFile;
    this.point = point;
    this.var = var;
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][] {
        {TestDir.cdmUnitTestDir + "transforms/idv_sigma.ncml", "/share/testdata/cdmUnitTest/transforms/idv_sigma.nc",
            LatLonPoint.create(52.85, 27.56), "VNK201302"}, // AtmosSigma
        {TestDir.cdmUnitTestDir + "transforms/HybridSigmaPressure.nc",
            "/share/testdata/cdmUnitTest/transforms/HybridSigmaPressure.ncml", LatLonPoint.create(40.019, -105.293),
            "T"}, // HybridSigma with P
        {TestDir.cdmUnitTestDir + "transforms/HIRLAMhybrid.ncml",
            "/share/testdata/cdmUnitTest/transforms/HIRLAMhybrid_hPa.ncml", LatLonPoint.create(42.86, -8.55),
            "Relative_humidity_hybrid"} // HybridSigma with AP
    };

    return Arrays.asList(data);
  }


  @Test
  public void shouldGetSameVerticalProfile() throws IOException, InvalidRangeException {
    System.out.printf("Open %s%n", sameUnitsFile);
    DatasetUrl durl = DatasetUrl.findDatasetUrl(sameUnitsFile);
    NetcdfDataset dsGood = NetcdfDatasets.acquireDataset(durl, true, null);
    GridDataset gdsGood = new GridDataset(dsGood);

    GeoGrid gridGood = gdsGood.findGridByName(var);
    Projection proj = gridGood.getProjection();
    ProjectionPoint pp = proj.latLonToProj(point);

    double[] dataGood = getVertTransformationForPoint(pp, 0, gridGood);

    NetcdfDataset dsDiff = NetcdfDatasets.acquireDataset(durl, true, null);
    GridDataset gdsDiff = new GridDataset(dsDiff);

    GeoGrid gridDiff = gdsDiff.findGridByName(var);
    proj = gridDiff.getProjection();
    pp = proj.latLonToProj(point);

    double[] dataDiff = getVertTransformationForPoint(pp, 0, gridDiff);

    assertArrayEquals(dataGood, dataDiff, 0.00001);

  }

  private double[] getVertTransformationForPoint(ProjectionPoint point, int timeIndex, GeoGrid grid)
      throws IOException, InvalidRangeException {

    VerticalTransform vt = grid.getCoordinateSystem().getVerticalTransform();
    // System.out.println(vt.isTimeDependent());
    int[] pointIndices = new int[] {0, 0};

    grid.getCoordinateSystem().findXYindexFromCoord(point.getX(), point.getY(), pointIndices);

    ArrayDouble.D1 dataArr = vt.getCoordinateArray1D(timeIndex, pointIndices[0], pointIndices[1]);

    return (double[]) dataArr.copyTo1DJavaArray();
  }

}
