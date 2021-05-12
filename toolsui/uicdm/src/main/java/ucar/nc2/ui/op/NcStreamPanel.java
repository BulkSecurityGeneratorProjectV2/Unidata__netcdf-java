/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.op;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.stream.NcStreamIosp;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/** Show internal structure of ncstream files. */
public class NcStreamPanel extends JPanel {
  private final PreferencesExt prefs;

  private final BeanTable<MessBean> messTable;
  private final JSplitPane split;

  private TextHistoryPane infoTA;
  private RandomAccessFile raf;
  private NetcdfFile ncfile;
  private NcStreamIosp iosp;

  public NcStreamPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    messTable = new BeanTable<>(MessBean.class, (PreferencesExt) prefs.node("NcStreamPanel"), false);
    messTable.addListSelectionListener(e -> {
      MessBean bean = messTable.getSelectedBean();
      if (bean == null) {
        return;
      }
      infoTA.setText(bean.getDesc());
    });
    varPopup = new PopupMenu(messTable.getJTable(), "Options");
    varPopup.addAction("Show deflate", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessBean bean = messTable.getSelectedBean();
        if (bean == null) {
          return;
        }
        infoTA.setText(bean.m.showDeflate());
      }
    });

    infoTA = new TextHistoryPane();
    TextHistoryPane infoPopup2 = new TextHistoryPane();
    IndependentWindow infoWindow2 =
        new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoPopup2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    TextHistoryPane infoPopup3 = new TextHistoryPane();
    IndependentWindow infoWindow3 =
        new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoPopup3);
    infoWindow3.setBounds((Rectangle) prefs.getBean("InfoWindowBounds3", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messTable, infoTA);
    split.setDividerLocation(prefs.getInt("splitPos", 800));

    add(split, BorderLayout.CENTER);
  }

  public void save() {
    messTable.saveState(false);
    if (split != null) {
      prefs.putInt("splitPos", split.getDividerLocation());
    }
  }

  public void closeOpenFiles() throws IOException {
    if (ncfile != null) {
      ncfile.close();
    }
    ncfile = null;
    raf = null;
    iosp = null;
  }

  public void showInfo(Formatter f) {
    if (ncfile == null) {
      return;
    }
    try {
      f.format("%s%n", raf.getLocation());
      f.format(" file length = %d%n", raf.length());
      f.format(" version = %d%n", iosp.getVersion());
    } catch (IOException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    }
    f.format("%n%s", ncfile.toString()); // CDL
  }

  public void setNcStreamFile(String filename) throws IOException {
    closeOpenFiles();

    List<MessBean> messages = new ArrayList<>();
    Group.Builder root = Group.builder();
    iosp = new NcStreamIosp();
    try {
      raf = new RandomAccessFile(filename, "r");
      List<NcStreamIosp.NcsMess> ncm = new ArrayList<>();
      iosp.openDebugNew(raf, root, ncm);
      for (NcStreamIosp.NcsMess m : ncm) {
        messages.add(new MessBean(m));
      }
      ncfile = NetcdfFile.builder().setRootGroup(root).setLocation(filename).build();
    } finally {
      if (raf != null) {
        raf.close();
      }
    }
    messTable.setBeans(messages);
  }

  public static class MessBean {
    private NcStreamIosp.NcsMess m;

    MessBean() {}

    MessBean(NcStreamIosp.NcsMess m) {
      this.m = m;
    }

    public String getObjClass() {
      return m.what.getClass().toString();
    }

    public String getDesc() {
      return m.what.toString();
    }

    public int getSize() {
      return m.len;
    }

    public int getNelems() {
      return m.nelems;
    }

    public String getDataType() {
      return (m.dataType == null) ? "" : m.dataType.toString();
    }

    public String getVarname() {
      return m.varName;
    }

    public long getFilePos() {
      return m.filePos;
    }
  }
}
