package ucar.nc2.internal.grid;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.projection.FlatEarth;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

/** Test {@link GridCS} */
public class TestGridCS {

  @Test
  public void testBasics() {
    NetcdfDataset ncd = NetcdfDataset.builder().build();
    ArrayList<CoordinateAxis> axes = new ArrayList<>();
    ArrayList<CoordinateTransform> transforms = new ArrayList<>();

    VariableDS.Builder<?> xBuilder = VariableDS.builder().setName("xname").setDataType(DataType.FLOAT)
        .setUnits("xunits").setDesc("xdesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(xBuilder).setAxisType(AxisType.GeoX).build(makeDummyGroup()));

    VariableDS.Builder<?> yBuilder = VariableDS.builder().setName("yname").setDataType(DataType.FLOAT)
        .setUnits("yunits").setDesc("ydesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(yBuilder).setAxisType(AxisType.GeoY).build(makeDummyGroup()));

    ProjectionCT projct = new ProjectionCT("horiz", "auth", new FlatEarth());
    transforms.add(projct);

    CoordinateSystem.Builder<?> builder = CoordinateSystem.builder().setCoordAxesNames("xname yname")
        .addCoordinateTransformByName("horiz").addCoordinateTransformByName("vert");
    CoordinateSystem coordSys = builder.build(ncd, axes, transforms);

  }
}
